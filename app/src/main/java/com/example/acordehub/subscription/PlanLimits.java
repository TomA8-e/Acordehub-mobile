package com.example.acordehub.subscription;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PlanLimits {
    public static final String STATUS_INACTIVE = "inactive";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_PAST_DUE = "past_due";
    public static final String STATUS_CANCELLED = "cancelled";

    private static final PlanConfig FREE = new PlanConfig(
            UserPlan.FREE,
            "Free",
            0,
            "USD",
            "Perfil musical basico y perfil de productor basico, sin funciones profesionales.",
            PlanConfig.extensions("mp3"),
            10,
            3,
            1,
            false,
            false,
            false,
            false
    );

    private static final PlanConfig PLUS = new PlanConfig(
            UserPlan.PLUS,
            "Plus",
            3.99,
            "USD",
            "MP3 y WAV, filtros avanzados, mas proyectos activos y mayor visibilidad.",
            PlanConfig.extensions("mp3", "wav"),
            100,
            10,
            5,
            true,
            false,
            true,
            false
    );

    private static final PlanConfig PRO = new PlanConfig(
            UserPlan.PRO,
            "Pro",
            7.99,
            "USD",
            "Formatos profesionales, mayor almacenamiento, perfil destacado y estadisticas.",
            PlanConfig.extensions("mp3", "wav", "stem", "stems", "zip"),
            250,
            25,
            Integer.MAX_VALUE,
            true,
            true,
            true,
            false
    );

    private static final PlanConfig PRODUCER = new PlanConfig(
            UserPlan.PRODUCER,
            "Producer",
            9.99,
            "USD",
            "Portfolio profesional ampliado, servicios, DAW, equipamiento y tarifas visibles.",
            PlanConfig.extensions("mp3", "wav", "stem", "stems", "zip"),
            500,
            30,
            Integer.MAX_VALUE,
            true,
            true,
            true,
            true
    );

    private static final List<PlanConfig> PLANS = Collections.unmodifiableList(
            Arrays.asList(FREE, PLUS, PRO, PRODUCER)
    );

    private PlanLimits() {}

    public static List<PlanConfig> allPlans() {
        return PLANS;
    }

    public static PlanConfig get(UserPlan plan) {
        if (plan == null) return FREE;
        switch (plan) {
            case PLUS:
                return PLUS;
            case PRO:
                return PRO;
            case PRODUCER:
                return PRODUCER;
            case FREE:
            default:
                return FREE;
        }
    }

    public static PlanConfig get(String planId) {
        return get(UserPlan.fromId(planId));
    }

    public static boolean isProfessionalFormat(String extension) {
        return "stem".equals(extension) || "stems".equals(extension) || "zip".equals(extension);
    }
}
