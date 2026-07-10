package com.example.acordehub.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.acordehub.R;
import com.example.acordehub.databinding.ActivityPremiumBinding;
import com.example.acordehub.subscription.PlanConfig;
import com.example.acordehub.subscription.PlanLimits;
import com.example.acordehub.subscription.UserPlan;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PremiumActivity extends AppCompatActivity {

    private ActivityPremiumBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private UserPlan currentPlan = UserPlan.FREE;
    private String subscriptionStatus = PlanLimits.STATUS_INACTIVE;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPremiumBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        loadCurrentPlan();
    }

    private void loadCurrentPlan() {
        String uid = getCurrentUid();
        if (uid == null) {
            Toast.makeText(this, "Inicia sesion para ver planes", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setLoading(true);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    currentPlan = UserPlan.fromId(snapshot.getString("plan"));
                    String status = snapshot.getString("subscriptionStatus");
                    subscriptionStatus = status == null || status.trim().isEmpty()
                            ? PlanLimits.STATUS_INACTIVE
                            : status;
                    setLoading(false);
                    renderPlans();
                })
                .addOnFailureListener(e -> {
                    currentPlan = UserPlan.FREE;
                    subscriptionStatus = PlanLimits.STATUS_INACTIVE;
                    setLoading(false);
                    renderPlans();
                });
    }

    private void renderPlans() {
        binding.plansContainer.removeAllViews();
        PlanConfig currentConfig = PlanLimits.get(currentPlan);
        binding.tvCurrentPlan.setText("Plan actual: " + currentConfig.getDisplayName()
                + " · " + subscriptionStatus);

        for (PlanConfig plan : PlanLimits.allPlans()) {
            binding.plansContainer.addView(createPlanCard(plan));
        }
    }

    private View createPlanCard(PlanConfig plan) {
        boolean isCurrent = plan.getPlan() == currentPlan;
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);
        card.setCardBackgroundColor(getColor(R.color.surface_card));
        card.setRadius(dp(16));
        card.setCardElevation(dp(1));
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(getColor(isCurrent ? R.color.orange_primary : R.color.outline_soft));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView title = text(plan.getDisplayName(), 20, R.color.black_primary, true);
        content.addView(title);

        TextView price = text(plan.getFormattedMonthlyPrice() + " / mes", 15, R.color.orange_dark, true);
        price.setPadding(0, dp(4), 0, 0);
        content.addView(price);

        TextView description = text(plan.getDescription(), 13, R.color.black_soft, false);
        description.setPadding(0, dp(8), 0, 0);
        content.addView(description);

        TextView limits = text(plan.getFileLimitsSummary(), 12, R.color.gray_hint, false);
        limits.setPadding(0, dp(10), 0, 0);
        content.addView(limits);

        TextView features = text(buildFeatures(plan), 13, R.color.black_soft, false);
        features.setPadding(0, dp(10), 0, 0);
        content.addView(features);

        MaterialButton button = new MaterialButton(this);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48));
        buttonParams.setMargins(0, dp(14), 0, 0);
        button.setLayoutParams(buttonParams);
        button.setCornerRadius(dp(12));
        button.setText(isCurrent ? "Plan actual" : (plan.getMonthlyPrice() == 0 ? "Usar Free" : "Suscribirme"));
        button.setTextColor(getColor(isCurrent || plan.getMonthlyPrice() == 0 ? R.color.black_primary : R.color.white));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getColor(isCurrent || plan.getMonthlyPrice() == 0 ? R.color.orange_light : R.color.black_primary)));
        button.setEnabled(!isCurrent);
        button.setOnClickListener(v -> selectPlan(plan));
        content.addView(button);

        card.addView(content);
        return card;
    }

    private String buildFeatures(PlanConfig plan) {
        StringBuilder builder = new StringBuilder();
        if (plan.hasAdvancedFilters()) builder.append("• Filtros avanzados\n");
        if (plan.hasFeaturedProfile()) builder.append("• Mayor visibilidad / perfil destacado\n");
        if (plan.hasProfileStatistics()) builder.append("• Estadisticas del perfil\n");
        if (plan.hasProducerPortfolio()) builder.append("• Portfolio profesional de productor\n");
        if (builder.length() == 0) builder.append("• Funciones basicas de AcordeHub");
        return builder.toString().trim();
    }

    private void selectPlan(PlanConfig plan) {
        if (plan.getPlan() == currentPlan) {
            Toast.makeText(this, "Ya estás utilizando este plan.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (plan.getMonthlyPrice() == 0) {
            updateFreePlan();
            return;
        }

        startPaymentFlow(plan);
    }

    private void updateFreePlan() {
        String uid = getCurrentUid();
        if (uid == null) return;

        setLoading(true);
        Map<String, Object> updates = new HashMap<>();
        updates.put("plan", UserPlan.FREE.getId());
        updates.put("subscriptionStatus", PlanLimits.STATUS_INACTIVE);
        updates.put("subscriptionProvider", "");
        updates.put("subscriptionId", "");
        updates.put("subscriptionStartedAt", null);
        updates.put("subscriptionExpiresAt", null);
        updates.put("autoRenew", false);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Tu suscripción fue actualizada correctamente.", Toast.LENGTH_SHORT).show();
                    loadCurrentPlan();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "No pudimos actualizar tu suscripcion", Toast.LENGTH_SHORT).show();
                });
    }

    private void startPaymentFlow(PlanConfig plan) {
        Toast.makeText(this,
                "Proveedor de pago pendiente de configurar para " + plan.getDisplayName(),
                Toast.LENGTH_LONG).show();
        // Cuando se conecte Stripe, MercadoPago u otro proveedor, confirmar el pago
        // y luego actualizar plan/subscriptionStatus desde este punto.
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(getColor(color));
        if (bold) textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return textView;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String getCurrentUid() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
