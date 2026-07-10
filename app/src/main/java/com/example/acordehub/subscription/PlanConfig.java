package com.example.acordehub.subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlanConfig {
    private final UserPlan plan;
    private final String id;
    private final String displayName;
    private final double monthlyPrice;
    private final String currency;
    private final String description;
    private final List<String> allowedFileExtensions;
    private final int maxFileSizeMB;
    private final int maxFilesPerProject;
    private final int maxActiveProjects;
    private final boolean hasAdvancedFilters;
    private final boolean hasProfileStatistics;
    private final boolean hasFeaturedProfile;
    private final boolean hasProducerPortfolio;

    public PlanConfig(UserPlan plan, String displayName, double monthlyPrice, String currency,
                      String description, List<String> allowedFileExtensions, int maxFileSizeMB,
                      int maxFilesPerProject, int maxActiveProjects, boolean hasAdvancedFilters,
                      boolean hasProfileStatistics, boolean hasFeaturedProfile,
                      boolean hasProducerPortfolio) {
        this.plan = plan;
        this.id = plan.getId();
        this.displayName = displayName;
        this.monthlyPrice = monthlyPrice;
        this.currency = currency;
        this.description = description;
        this.allowedFileExtensions = Collections.unmodifiableList(new ArrayList<>(allowedFileExtensions));
        this.maxFileSizeMB = maxFileSizeMB;
        this.maxFilesPerProject = maxFilesPerProject;
        this.maxActiveProjects = maxActiveProjects;
        this.hasAdvancedFilters = hasAdvancedFilters;
        this.hasProfileStatistics = hasProfileStatistics;
        this.hasFeaturedProfile = hasFeaturedProfile;
        this.hasProducerPortfolio = hasProducerPortfolio;
    }

    public UserPlan getPlan() { return plan; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public double getMonthlyPrice() { return monthlyPrice; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public List<String> getAllowedFileExtensions() { return allowedFileExtensions; }
    public int getMaxFileSizeMB() { return maxFileSizeMB; }
    public int getMaxFilesPerProject() { return maxFilesPerProject; }
    public int getMaxActiveProjects() { return maxActiveProjects; }
    public boolean hasAdvancedFilters() { return hasAdvancedFilters; }
    public boolean hasProfileStatistics() { return hasProfileStatistics; }
    public boolean hasFeaturedProfile() { return hasFeaturedProfile; }
    public boolean hasProducerPortfolio() { return hasProducerPortfolio; }

    public boolean allowsExtension(String extension) {
        return extension != null && allowedFileExtensions.contains(extension.toLowerCase());
    }

    public String getFormattedMonthlyPrice() {
        if (monthlyPrice == 0) return currency + " 0";
        return currency + " " + String.format(java.util.Locale.US, "%.2f", monthlyPrice);
    }

    public String getFileLimitsSummary() {
        return "Archivos " + allowedFileExtensions.toString().toUpperCase()
                + " · " + maxFileSizeMB + " MB · " + maxFilesPerProject + " por proyecto";
    }

    static List<String> extensions(String... values) {
        return Arrays.asList(values);
    }
}
