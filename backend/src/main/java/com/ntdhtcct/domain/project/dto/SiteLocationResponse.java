package com.ntdhtcct.domain.project.dto;

import com.ntdhtcct.domain.project.Project;

import java.util.UUID;

public record SiteLocationResponse(
        UUID projectId,
        String projectCode,
        String projectName,
        Double latitude,
        Double longitude,
        Double radiusMeters,
        boolean configured
) {
    public static SiteLocationResponse fromProject(Project project) {
        boolean configured = project.getSiteLatitude() != null
                && project.getSiteLongitude() != null
                && project.getSiteRadiusMeters() != null
                && project.getSiteRadiusMeters() > 0;
        return new SiteLocationResponse(
                project.getId(),
                project.getCode(),
                project.getName(),
                project.getSiteLatitude(),
                project.getSiteLongitude(),
                project.getSiteRadiusMeters(),
                configured
        );
    }
}
