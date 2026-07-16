const {onRequest} = require("firebase-functions/v2/https");
const {onDocumentCreated, onDocumentUpdated} = require("firebase-functions/v2/firestore");
const {defineSecret} = require("firebase-functions/params");
const {setGlobalOptions} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({region: "us-central1"});

const mercadoPagoAccessToken = defineSecret("MERCADO_PAGO_ACCESS_TOKEN");
const spotifyClientId = defineSecret("SPOTIFY_CLIENT_ID");
const spotifyClientSecret = defineSecret("SPOTIFY_CLIENT_SECRET");

let spotifyAccessToken = "";
let spotifyAccessTokenExpiresAt = 0;

const PLAN_CONFIG = {
  plus: {
    displayName: "Plus",
    amount: 3990,
    currency: "ARS",
    description: "AcordeHub Plus mensual",
  },
  pro: {
    displayName: "Pro",
    amount: 7990,
    currency: "ARS",
    description: "AcordeHub Pro mensual",
  },
  producer: {
    displayName: "Producer",
    amount: 9990,
    currency: "ARS",
    description: "AcordeHub Producer mensual",
  },
};

const PROJECT_LIMITS = {
  free: {maxActiveProjects: 1},
  plus: {maxActiveProjects: 5},
  pro: {maxActiveProjects: Number.MAX_SAFE_INTEGER},
  producer: {maxActiveProjects: Number.MAX_SAFE_INTEGER},
};

exports.notifyConnectionRequestCreated = onDocumentCreated(
    {document: "connectionRequests/{requestId}", region: "southamerica-east1"},
    async (event) => {
      const request = event.data?.data();
      if (!request?.targetUid) return;
      await sendUserNotification(request.targetUid, {
        type: "connection_request",
        entityId: event.params.requestId,
        title: "Nueva solicitud de conexión",
        body: `${request.requesterName || "Un músico"} quiere conectar con vos`,
      });
    },
);

exports.notifyConnectionRequestUpdated = onDocumentUpdated(
    {document: "connectionRequests/{requestId}", region: "southamerica-east1"},
    async (event) => {
      const before = event.data?.before.data();
      const after = event.data?.after.data();
      if (!after?.requesterUid || before?.status === after.status ||
          !["accepted", "rejected"].includes(after.status)) return;
      await sendUserNotification(after.requesterUid, {
        type: "connection_response",
        entityId: event.params.requestId,
        title: after.status === "accepted" ? "Solicitud aceptada" : "Solicitud rechazada",
        body: after.status === "accepted"
          ? `${after.targetName || "El usuario"} aceptó tu conexión`
          : "Tu solicitud de conexión fue rechazada",
      });
    },
);

exports.notifyProjectJoinRequestCreated = onDocumentCreated(
    {
      document: "projects/{projectId}/joinRequests/{requesterUid}",
      region: "southamerica-east1",
    },
    async (event) => {
      const request = event.data?.data();
      if (!request?.ownerUid) return;
      await sendUserNotification(request.ownerUid, {
        type: "project_join_request",
        entityId: `${event.params.projectId}_${event.params.requesterUid}`,
        title: "Nueva solicitud para tu proyecto",
        body: `${request.requesterName || "Un músico"} quiere unirse a ${request.projectTitle || "tu proyecto"}`,
      });
    },
);

exports.notifyProjectJoinRequestUpdated = onDocumentUpdated(
    {
      document: "projects/{projectId}/joinRequests/{requesterUid}",
      region: "southamerica-east1",
    },
    async (event) => {
      const before = event.data?.before.data();
      const after = event.data?.after.data();
      if (!after?.requesterUid || before?.status === after.status ||
          !["accepted", "rejected"].includes(after.status)) return;
      await sendUserNotification(after.requesterUid, {
        type: "project_join_response",
        entityId: `${event.params.projectId}_${event.params.requesterUid}`,
        title: after.status === "accepted" ? "Te aceptaron en el proyecto" : "Solicitud rechazada",
        body: after.status === "accepted"
          ? `Ya podés conversar sobre ${after.projectTitle || "el proyecto"}`
          : `No aceptaron tu solicitud para ${after.projectTitle || "el proyecto"}`,
      });
    },
);

exports.notifyChatMessageCreated = onDocumentCreated(
    {document: "chats/{chatId}/messages/{messageId}", region: "southamerica-east1"},
    async (event) => {
      const message = event.data?.data();
      if (!message?.senderId) return;
      const chatSnapshot = await admin.firestore().collection("chats")
          .doc(event.params.chatId).get();
      if (!chatSnapshot.exists) return;
      const chat = chatSnapshot.data();
      const recipients = (chat.participantIds || []).filter((uid) => uid !== message.senderId);
      const senderName = chat.participantNames?.[message.senderId] || "Nuevo mensaje";
      await Promise.all(recipients.map((uid) => sendUserNotification(uid, {
        type: "chat_message",
        entityId: event.params.messageId,
        chatId: event.params.chatId,
        chatTitle: senderName,
        title: senderName,
        body: truncateNotificationText(message.text || "Te envió un mensaje"),
      })));
    },
);

exports.createProject = onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({error: "method_not_allowed"});
    return;
  }

  try {
    const decodedToken = await verifyFirebaseToken(req);
    const input = validateProjectInput(req.body, decodedToken.uid);
    const db = admin.firestore();
    const userRef = db.collection("users").doc(decodedToken.uid);
    const projectRef = db.collection("projects").doc(input.projectId);

    await db.runTransaction(async (transaction) => {
      const userSnapshot = await transaction.get(userRef);
      if (!userSnapshot.exists) throw publicError(404, "user_not_found");

      const user = userSnapshot.data();
      const planId = getEffectivePlanId(user);
      const activeProjects = db.collection("projects")
          .where("ownerUid", "==", decodedToken.uid)
          .where("status", "==", "active");
      const activeSnapshot = await transaction.get(activeProjects);
      if (activeSnapshot.size >= PROJECT_LIMITS[planId].maxActiveProjects) {
        throw publicError(409, "active_project_limit_reached");
      }

      const existingProject = await transaction.get(projectRef);
      if (existingProject.exists) throw publicError(409, "project_already_exists");

      transaction.create(projectRef, {
        ownerUid: decodedToken.uid,
        ownerName: cleanString(user.name, 120) || decodedToken.name || "Usuario",
        title: input.title,
        description: input.description,
        genre: input.genre,
        imageUri: input.imageUrl,
        demoUri: input.demoUrl,
        status: "active",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    res.status(201).json({projectId: input.projectId});
  } catch (error) {
    console.error("createProject failed", error);
    res.status(error.statusCode || 500).json({
      error: error.publicCode || "project_creation_error",
    });
  }
});

exports.updateProject = onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({error: "method_not_allowed"});
    return;
  }

  try {
    const decodedToken = await verifyFirebaseToken(req);
    const input = validateProjectInput(req.body, decodedToken.uid);
    const projectRef = admin.firestore().collection("projects").doc(input.projectId);

    await admin.firestore().runTransaction(async (transaction) => {
      const snapshot = await transaction.get(projectRef);
      if (!snapshot.exists) throw publicError(404, "project_not_found");
      if (snapshot.data().ownerUid !== decodedToken.uid) {
        throw publicError(403, "not_project_owner");
      }
      transaction.update(projectRef, {
        title: input.title,
        description: input.description,
        genre: input.genre,
        imageUri: input.imageUrl,
        demoUri: input.demoUrl,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    res.status(200).json({projectId: input.projectId});
  } catch (error) {
    console.error("updateProject failed", error);
    res.status(error.statusCode || 500).json({
      error: error.publicCode || "project_update_error",
    });
  }
});

exports.deleteProject = onRequest(async (req, res) => {
  setCorsHeaders(res);
  if (req.method === "OPTIONS") {
    res.status(204).send("");
    return;
  }
  if (req.method !== "POST") {
    res.status(405).json({error: "method_not_allowed"});
    return;
  }

  try {
    const decodedToken = await verifyFirebaseToken(req);
    const projectId = cleanString(req.body?.projectId, 40);
    if (!projectId || !/^[A-Za-z0-9]{20}$/.test(projectId)) {
      throw publicError(400, "invalid_project_id");
    }

    const projectRef = admin.firestore().collection("projects").doc(projectId);
    const snapshot = await projectRef.get();
    if (!snapshot.exists) throw publicError(404, "project_not_found");
    if (snapshot.data().ownerUid !== decodedToken.uid) {
      throw publicError(403, "not_project_owner");
    }

    await admin.firestore().recursiveDelete(projectRef);
    try {
      await admin.storage().bucket().deleteFiles({
        prefix: `project_uploads/${decodedToken.uid}/${projectId}/`,
      });
    } catch (storageError) {
      console.error("Project deleted but asset cleanup failed", storageError);
    }

    res.status(200).json({projectId});
  } catch (error) {
    console.error("deleteProject failed", error);
    res.status(error.statusCode || 500).json({
      error: error.publicCode || "project_delete_error",
    });
  }
});

exports.createMercadoPagoPreference = onRequest(
    {secrets: [mercadoPagoAccessToken]},
    async (req, res) => {
      setCorsHeaders(res);
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }
      if (req.method !== "POST") {
        res.status(405).json({error: "method_not_allowed"});
        return;
      }

      try {
        const decodedToken = await verifyFirebaseToken(req);
        const plan = PLAN_CONFIG[String(req.body?.planId || "").toLowerCase()];
        if (!plan) {
          res.status(400).json({error: "invalid_plan"});
          return;
        }

        const userSnapshot = await admin.firestore()
            .collection("users")
            .doc(decodedToken.uid)
            .get();
        const user = userSnapshot.data() || {};
        const paymentRef = admin.firestore().collection("paymentPreferences").doc();
        const publicBaseUrl = getPublicFunctionsBaseUrl();

        await paymentRef.set({
          uid: decodedToken.uid,
          userEmail: decodedToken.email || user.email || null,
          planId: req.body.planId,
          provider: "mercadopago",
          amount: plan.amount,
          currency: plan.currency,
          status: "pending",
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        const requestedBackUrl = typeof req.body?.backUrl === "string" ? req.body.backUrl : "";
        const backUrl = requestedBackUrl.startsWith("http://") || requestedBackUrl.startsWith("https://")
          ? requestedBackUrl
          : "acordehub://premium?status=approved";

        const preferenceBody = {
          reason: plan.description,
          payer_email: decodedToken.email || user.email,
          external_reference: paymentRef.id,
          back_url: backUrl,
          notification_url: `${publicBaseUrl}/mercadoPagoWebhook?source_news=webhooks`,
          auto_recurring: {
            frequency: 1,
            frequency_type: "months",
            transaction_amount: plan.amount,
            currency_id: plan.currency,
          },
        };

        if (!preferenceBody.payer_email) {
          res.status(400).json({error: "missing_user_email"});
          return;
        }

        const preference = await mercadoPagoRequest(
            "/preapproval",
            "POST",
            preferenceBody,
        );

        await paymentRef.set({
          preferenceId: preference.id || null,
          preapprovalId: preference.id || null,
          checkoutUrl: preference.init_point || null,
          sandboxCheckoutUrl: preference.sandbox_init_point || null,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});

        res.status(200).json({
          paymentPreferenceId: paymentRef.id,
          preferenceId: preference.id,
          checkoutUrl: preference.init_point,
          sandboxCheckoutUrl: preference.sandbox_init_point,
        });
      } catch (error) {
        console.error("createMercadoPagoPreference failed", error);
        res.status(error.statusCode || 500).json({
          error: error.publicCode || "payment_preference_error",
        });
      }
    },
);

exports.syncMercadoPagoSubscription = onRequest(
    {secrets: [mercadoPagoAccessToken]},
    async (req, res) => {
      setCorsHeaders(res);
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }
      if (req.method !== "POST") {
        res.status(405).json({error: "method_not_allowed"});
        return;
      }

      try {
        const decodedToken = await verifyFirebaseToken(req);
        const paymentPreferenceId = cleanString(
            req.body?.paymentPreferenceId,
            80,
        );
        if (!paymentPreferenceId) {
          throw publicError(400, "invalid_payment_preference");
        }

        const paymentRef = admin.firestore()
            .collection("paymentPreferences")
            .doc(paymentPreferenceId);
        const snapshot = await paymentRef.get();
        if (!snapshot.exists || snapshot.data().uid !== decodedToken.uid) {
          throw publicError(404, "payment_preference_not_found");
        }

        const paymentRecord = snapshot.data();
        if (!paymentRecord.preapprovalId) {
          throw publicError(409, "payment_preference_pending");
        }

        const subscription = await mercadoPagoRequest(
            `/preapproval/${encodeURIComponent(paymentRecord.preapprovalId)}`,
            "GET",
        );
        if (String(subscription.external_reference) !== paymentPreferenceId) {
          throw publicError(409, "payment_reference_mismatch");
        }

        const result = await applySubscriptionState(
            paymentRef,
            paymentRecord,
            subscription,
        );
        res.status(200).json(result);
      } catch (error) {
        console.error("syncMercadoPagoSubscription failed", error);
        res.status(error.statusCode || 500).json({
          error: error.publicCode || "payment_sync_error",
        });
      }
    },
);

exports.cancelSubscription = onRequest(
    {secrets: [mercadoPagoAccessToken]},
    async (req, res) => {
      setCorsHeaders(res);
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }
      if (req.method !== "POST") {
        res.status(405).json({error: "method_not_allowed"});
        return;
      }

      try {
        const decodedToken = await verifyFirebaseToken(req);
        const userRef = admin.firestore().collection("users").doc(decodedToken.uid);
        const snapshot = await userRef.get();
        if (!snapshot.exists) throw publicError(404, "user_not_found");

        const user = snapshot.data();
        if (user.subscriptionProvider === "mercadopago" && user.subscriptionId) {
          await mercadoPagoRequest(
              `/preapproval/${encodeURIComponent(user.subscriptionId)}`,
              "PUT",
              {status: "canceled"},
          );
        }

        await userRef.set({
          plan: "free",
          subscriptionStatus: "cancelled",
          autoRenew: false,
          subscriptionExpiresAt: admin.firestore.FieldValue.delete(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});
        res.status(200).json({plan: "free", status: "cancelled"});
      } catch (error) {
        console.error("cancelSubscription failed", error);
        res.status(error.statusCode || 500).json({
          error: error.publicCode || "subscription_cancellation_error",
        });
      }
    },
);

exports.searchSpotifyArtists = onRequest(
    {secrets: [spotifyClientId, spotifyClientSecret]},
    async (req, res) => {
      setCorsHeaders(res);
      if (req.method === "OPTIONS") {
        res.status(204).send("");
        return;
      }
      if (req.method !== "POST") {
        res.status(405).json({error: "method_not_allowed"});
        return;
      }

      try {
        await verifyFirebaseToken(req);
        const query = cleanString(req.body?.query, 100);
        if (query.length < 2) {
          res.status(400).json({error: "invalid_search_query"});
          return;
        }

        const token = await getSpotifyAccessToken();
        const searchParams = new URLSearchParams({
          q: query,
          type: "artist",
          market: "AR",
          limit: "10",
        });
        const response = await fetch(`https://api.spotify.com/v1/search?${searchParams}`, {
          headers: {Authorization: `Bearer ${token}`},
        });
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) throw publicError(502, "spotify_search_error");

        const artists = (payload.artists?.items || []).map((artist) => ({
          id: artist.id,
          name: artist.name,
          imageUrl: artist.images?.[0]?.url || "",
        }));
        res.status(200).json({artists});
      } catch (error) {
        console.error("searchSpotifyArtists failed", error);
        res.status(error.statusCode || 500).json({
          error: error.publicCode || "spotify_search_error",
        });
      }
    },
);

exports.mercadoPagoWebhook = onRequest(
    {secrets: [mercadoPagoAccessToken]},
    async (req, res) => {
      if (req.method !== "POST") {
        res.status(405).json({error: "method_not_allowed"});
        return;
      }

      try {
        const type = req.body?.type || req.query?.type || req.query?.topic;
        const paymentId = req.body?.data?.id || req.query?.["data.id"] || req.query?.id;
        if (!paymentId) {
          res.status(200).json({received: true});
          return;
        }

        let paymentPreferenceId = "";
        let subscription = null;
        let paymentStatus = null;
        let paymentStatusDetail = null;
        let webhookDetails = {};

        if (type === "subscription_preapproval" || type === "preapproval") {
          subscription = await mercadoPagoRequest(
              `/preapproval/${encodeURIComponent(paymentId)}`,
              "GET",
          );
          paymentPreferenceId = String(subscription.external_reference || "");
          paymentStatus = subscription.status || null;
          webhookDetails = {mercadoPagoPreapprovalId: String(paymentId)};
        } else if (type === "subscription_authorized_payment") {
          const authorizedPayment = await mercadoPagoRequest(
              `/authorized_payments/${encodeURIComponent(paymentId)}`,
              "GET",
          );
          paymentPreferenceId = String(authorizedPayment.external_reference || "");
          paymentStatus = authorizedPayment.payment?.status ||
            authorizedPayment.status || null;
          paymentStatusDetail = authorizedPayment.payment?.status_detail || null;
          webhookDetails = {
            mercadoPagoAuthorizedPaymentId: String(paymentId),
            mercadoPagoPaymentId: authorizedPayment.payment?.id ?
              String(authorizedPayment.payment.id) : null,
          };
          if (authorizedPayment.preapproval_id) {
            subscription = await mercadoPagoRequest(
                `/preapproval/${encodeURIComponent(authorizedPayment.preapproval_id)}`,
                "GET",
            );
          }
        } else {
          const payment = await mercadoPagoRequest(
              `/v1/payments/${encodeURIComponent(paymentId)}`,
              "GET",
          );
          paymentPreferenceId = String(payment.external_reference || "");
          paymentStatus = payment.status || null;
          paymentStatusDetail = payment.status_detail || null;
          webhookDetails = {mercadoPagoPaymentId: String(paymentId)};
        }

        if (!paymentPreferenceId) {
          res.status(200).json({received: true});
          return;
        }

        const paymentRef = admin.firestore()
            .collection("paymentPreferences")
            .doc(paymentPreferenceId);
        const paymentSnapshot = await paymentRef.get();
        if (!paymentSnapshot.exists) {
          res.status(200).json({received: true});
          return;
        }

        await paymentRef.set({
          ...webhookDetails,
          mercadoPagoStatus: paymentStatus,
          mercadoPagoStatusDetail: paymentStatusDetail,
          status: isSuccessfulMercadoPagoStatus(paymentStatus) ?
            "approved" : paymentStatus || "updated",
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});

        if (subscription) {
          await applySubscriptionState(
              paymentRef,
              paymentSnapshot.data(),
              subscription,
              paymentStatus,
          );
        }

        res.status(200).json({received: true});
      } catch (error) {
        console.error("mercadoPagoWebhook failed", error);
        res.status(500).json({error: "webhook_error"});
      }
    },
);

async function applySubscriptionState(
    paymentRef,
    paymentRecord,
    subscription,
    latestPaymentStatus = null,
) {
  const planId = paymentRecord.planId;
  const subscriptionStatus = String(subscription.status || "pending");
  const active = subscriptionStatus === "authorized" &&
    (!latestPaymentStatus || isSuccessfulMercadoPagoStatus(latestPaymentStatus));
  const canceled = subscriptionStatus === "cancelled" ||
    subscriptionStatus === "canceled";

  await paymentRef.set({
    mercadoPagoPreapprovalId: String(subscription.id),
    mercadoPagoStatus: subscriptionStatus,
    status: active ? "approved" : subscriptionStatus,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  if (active && PLAN_CONFIG[planId]) {
    const expiresAt = new Date();
    expiresAt.setUTCMonth(expiresAt.getUTCMonth() + 1);
    await admin.firestore().collection("users").doc(paymentRecord.uid).set({
      plan: planId,
      subscriptionStatus: "active",
      subscriptionProvider: "mercadopago",
      subscriptionId: String(subscription.id),
      subscriptionStartedAt: admin.firestore.FieldValue.serverTimestamp(),
      subscriptionExpiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
      autoRenew: true,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    return {plan: planId, status: "active"};
  }

  if (canceled) {
    const userRef = admin.firestore().collection("users").doc(paymentRecord.uid);
    const userSnapshot = await userRef.get();
    if (userSnapshot.data()?.subscriptionId === String(subscription.id)) {
      await userRef.set({
        plan: "free",
        subscriptionStatus: "cancelled",
        autoRenew: false,
        subscriptionExpiresAt: admin.firestore.FieldValue.delete(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});
    }
  }

  return {plan: planId, status: subscriptionStatus};
}

async function verifyFirebaseToken(req) {
  const authorization = req.get("Authorization") || "";
  const match = authorization.match(/^Bearer (.+)$/);
  if (!match) {
    const error = new Error("Missing bearer token");
    error.statusCode = 401;
    error.publicCode = "unauthenticated";
    throw error;
  }
  return admin.auth().verifyIdToken(match[1]);
}

async function mercadoPagoRequest(path, method, body) {
  const response = await fetch(`https://api.mercadopago.com${path}`, {
    method,
    headers: {
      "Authorization": `Bearer ${mercadoPagoAccessToken.value()}`,
      "Content-Type": "application/json",
      "Accept": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(`Mercado Pago ${method} ${path} failed`);
    error.statusCode = response.status >= 400 && response.status < 500 ? 502 : 500;
    error.payload = payload;
    throw error;
  }
  return payload;
}

async function getSpotifyAccessToken() {
  if (spotifyAccessToken && spotifyAccessTokenExpiresAt > Date.now() + 60000) {
    return spotifyAccessToken;
  }

  const credentials = Buffer.from(
      `${spotifyClientId.value()}:${spotifyClientSecret.value()}`,
  ).toString("base64");
  const response = await fetch("https://accounts.spotify.com/api/token", {
    method: "POST",
    headers: {
      "Authorization": `Basic ${credentials}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: "grant_type=client_credentials",
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || !payload.access_token) {
    throw publicError(502, "spotify_auth_error");
  }

  spotifyAccessToken = payload.access_token;
  spotifyAccessTokenExpiresAt = Date.now() + Number(payload.expires_in || 3600) * 1000;
  return spotifyAccessToken;
}

function getPublicFunctionsBaseUrl() {
  if (process.env.FUNCTIONS_PUBLIC_BASE_URL) {
    return process.env.FUNCTIONS_PUBLIC_BASE_URL.replace(/\/$/, "");
  }
  const projectId = process.env.GCLOUD_PROJECT || process.env.GCP_PROJECT;
  return `https://us-central1-${projectId}.cloudfunctions.net`;
}

function setCorsHeaders(res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
}

function isSuccessfulMercadoPagoStatus(status) {
  return status === "approved" || status === "authorized";
}

function validateProjectInput(body, uid) {
  const projectId = cleanString(body?.projectId, 40);
  const title = cleanString(body?.title, 120);
  const description = cleanString(body?.description, 2000);
  const genre = cleanString(body?.genre, 80);
  const imageUrl = cleanString(body?.imageUrl, 3000) || "";
  const demoUrl = cleanString(body?.demoUrl, 3000);

  if (!projectId || !/^[A-Za-z0-9]{20}$/.test(projectId)) {
    throw publicError(400, "invalid_project_id");
  }
  if (!title || !description || !genre || !demoUrl) {
    throw publicError(400, "invalid_project_data");
  }
  if (!isOwnedProjectAsset(demoUrl, uid, projectId, "demo") ||
      (imageUrl && !isOwnedProjectAsset(imageUrl, uid, projectId, "cover"))) {
    throw publicError(400, "invalid_project_asset");
  }
  return {projectId, title, description, genre, imageUrl, demoUrl};
}

function isOwnedProjectAsset(url, uid, projectId, kind) {
  try {
    const parsed = new URL(url);
    const allowedHost = parsed.hostname === "firebasestorage.googleapis.com" ||
      parsed.hostname === "storage.googleapis.com";
    const decodedUrl = decodeURIComponent(url);
    const expectedPath = `/project_uploads/${uid}/${projectId}/${kind}.`;
    return allowedHost && decodedUrl.includes(expectedPath);
  } catch (error) {
    return false;
  }
}

function cleanString(value, maxLength) {
  if (typeof value !== "string") return "";
  const cleaned = value.trim();
  return cleaned.length > 0 && cleaned.length <= maxLength ? cleaned : "";
}

function getEffectivePlanId(user) {
  const expiresAt = user.subscriptionExpiresAt;
  const expiresInFuture = expiresAt && typeof expiresAt.toMillis === "function" &&
    expiresAt.toMillis() > Date.now();
  if (user.subscriptionStatus === "active" && expiresInFuture &&
      Object.prototype.hasOwnProperty.call(PROJECT_LIMITS, user.plan)) {
    return user.plan;
  }
  return "free";
}

function publicError(statusCode, publicCode) {
  const error = new Error(publicCode);
  error.statusCode = statusCode;
  error.publicCode = publicCode;
  return error;
}

async function sendUserNotification(uid, payload) {
  const tokenRef = admin.firestore().collection("notificationTokens").doc(uid);
  const snapshot = await tokenRef.get();
  if (!snapshot.exists) return;
  const tokens = [...new Set((snapshot.data().tokens || [])
      .filter((token) => typeof token === "string" && token.length > 0))];
  if (tokens.length === 0) return;

  const invalidTokens = [];
  for (let offset = 0; offset < tokens.length; offset += 500) {
    const batchTokens = tokens.slice(offset, offset + 500);
    const response = await admin.messaging().sendEachForMulticast({
      tokens: batchTokens,
      data: Object.fromEntries(Object.entries(payload)
          .map(([key, value]) => [key, String(value)])),
      android: {priority: "high"},
    });
    response.responses.forEach((result, index) => {
      const code = result.error?.code;
      if (code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token") {
        invalidTokens.push(batchTokens[index]);
      }
    });
  }

  if (invalidTokens.length > 0) {
    await tokenRef.update({
      tokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
    });
  }
}

function truncateNotificationText(text) {
  const normalized = String(text).trim();
  return normalized.length <= 160 ? normalized : `${normalized.slice(0, 157)}...`;
}
