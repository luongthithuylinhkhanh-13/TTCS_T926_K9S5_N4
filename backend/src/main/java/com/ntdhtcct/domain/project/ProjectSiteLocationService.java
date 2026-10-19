package com.ntdhtcct.domain.project;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.project.dto.SiteLocationResponse;
import com.ntdhtcct.domain.project.dto.UpdateSiteLocationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectSiteLocationService {

    private static final String[] VIEW_ROLES = {
            "ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"
    };

    private static final String[] UPDATE_ROLES = {
            "ADMIN", "PROJECT_MANAGER"
    };

    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    public ProjectSiteLocationService(
            ProjectRepository projectRepository,
            AuthorizationService authorizationService
    ) {
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public SiteLocationResponse getSiteLocation(UUID projectId, UUID userId) {
        if (userId != null) {
            authorizationService.checkProjectAccess(projectId, userId, VIEW_ROLES);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PROJECT_NOT_FOUND",
                        "Không tìm thấy dự án"
                ));

        return SiteLocationResponse.fromProject(project);
    }

    @Transactional
    public SiteLocationResponse updateSiteLocation(
            UUID projectId,
            UpdateSiteLocationRequest request,
            UUID userId
    ) {
        if (userId != null) {
            authorizationService.checkProjectAccess(projectId, userId, UPDATE_ROLES);
        }

        if (request == null) {
            throw new BadRequestException("INVALID_REQUEST", "Dữ liệu yêu cầu không được để trống");
        }

        if (request.latitude() == null || request.longitude() == null || request.radiusMeters() == null) {
            throw new BadRequestException(
                    "INCOMPLETE_SITE_LOCATION",
                    "Cấu hình vị trí công trường phải cung cấp đầy đủ vĩ độ, kinh độ và bán kính"
            );
        }

        if (Double.isNaN(request.latitude()) || Double.isInfinite(request.latitude())
                || request.latitude() < -90.0 || request.latitude() > 90.0) {
            throw new BadRequestException(
                    "INVALID_LATITUDE",
                    "Vĩ độ phải nằm trong khoảng từ -90 đến 90 độ"
            );
        }

        if (Double.isNaN(request.longitude()) || Double.isInfinite(request.longitude())
                || request.longitude() < -180.0 || request.longitude() > 180.0) {
            throw new BadRequestException(
                    "INVALID_LONGITUDE",
                    "Kinh độ phải nằm trong khoảng từ -180 đến 180 độ"
            );
        }

        if (Double.isNaN(request.radiusMeters()) || Double.isInfinite(request.radiusMeters())
                || request.radiusMeters() <= 0.0) {
            throw new BadRequestException(
                    "INVALID_RADIUS",
                    "Bán kính công trường phải lớn hơn 0 mét"
            );
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PROJECT_NOT_FOUND",
                        "Không tìm thấy dự án"
                ));

        project.setSiteLatitude(request.latitude());
        project.setSiteLongitude(request.longitude());
        project.setSiteRadiusMeters(request.radiusMeters());

        Project saved = projectRepository.save(project);
        return SiteLocationResponse.fromProject(saved);
    }
}
