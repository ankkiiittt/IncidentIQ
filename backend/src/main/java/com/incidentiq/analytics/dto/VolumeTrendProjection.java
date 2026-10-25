package com.incidentiq.analytics.dto;

public interface VolumeTrendProjection {
    String getTimestamp();
    long getP1();
    long getP2();
    long getP3();
    long getP4();
    long getTotal();
}
