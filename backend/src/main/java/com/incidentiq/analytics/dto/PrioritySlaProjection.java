package com.incidentiq.analytics.dto;

public interface PrioritySlaProjection {
    String getPriority();
    long getMetCount();
    long getBreachedCount();
}
