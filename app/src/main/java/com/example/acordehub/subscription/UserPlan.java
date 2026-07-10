package com.example.acordehub.subscription;

public enum UserPlan {
    FREE("free"),
    PLUS("plus"),
    PRO("pro"),
    PRODUCER("producer");

    private final String id;

    UserPlan(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static UserPlan fromId(String id) {
        if (id == null || id.trim().isEmpty()) return FREE;
        for (UserPlan plan : values()) {
            if (plan.id.equalsIgnoreCase(id.trim())) return plan;
        }
        return FREE;
    }
}
