package com.alliance.placement.service;

import com.alliance.placement.domain.PlacementStatus;

import java.util.Map;

public record PlacementReport(int studentCount, int companyCount, int applicationCount,
                              Map<PlacementStatus, Long> studentsByStatus) {
    public PlacementReport { studentsByStatus = Map.copyOf(studentsByStatus); }
}
