package com.ntdhtcct.domain.project;

import com.ntdhtcct.auth.service.AuthorizationService;
import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.project.dto.SiteLocationResponse;
import com.ntdhtcct.domain.project.dto.UpdateSiteLocationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProjectSiteLocationTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthorizationService authorizationService;

    private ProjectSiteLocationService service;

    private final UUID projectId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private Project project;

    @BeforeEach
    void setUp() {
        service = new ProjectSiteLocationService(projectRepository, authorizationService);
        project = new Project("PRJ-001", "Dự án Tòa nhà Sunrise");
    }

    @Test
    @DisplayName("T-71: Đọc vị trí công trường khi đã cấu hình đầy đủ")
    void getSiteLocation_Success_WhenConfigured() {
        project.setSiteLatitude(21.028511);
        project.setSiteLongitude(105.854212);
        project.setSiteRadiusMeters(300.0);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        SiteLocationResponse response = service.getSiteLocation(projectId, userId);

        assertThat(response).isNotNull();
        assertThat(response.configured()).isTrue();
        assertThat(response.latitude()).isEqualTo(21.028511);
        assertThat(response.longitude()).isEqualTo(105.854212);
        assertThat(response.radiusMeters()).isEqualTo(300.0);
        assertThat(response.projectCode()).isEqualTo("PRJ-001");
        verify(authorizationService).checkProjectAccess(eq(projectId), eq(userId), any());
    }

    @Test
    @DisplayName("T-71: Đọc vị trí công trường khi dự án chưa cấu hình (mặc định null)")
    void getSiteLocation_ReturnsUnconfigured_WhenNotSet() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        SiteLocationResponse response = service.getSiteLocation(projectId, userId);

        assertThat(response).isNotNull();
        assertThat(response.configured()).isFalse();
        assertThat(response.latitude()).isNull();
        assertThat(response.longitude()).isNull();
        assertThat(response.radiusMeters()).isNull();
    }

    @Test
    @DisplayName("T-71: Ném ResourceNotFoundException khi không tìm thấy dự án")
    void getSiteLocation_ThrowsException_WhenProjectNotFound() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSiteLocation(projectId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy dự án");
    }

    @Test
    @DisplayName("T-71: Cập nhật vị trí công trường thành công với tọa độ và bán kính hợp lệ")
    void updateSiteLocation_Success_WhenValid() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSiteLocationRequest request = new UpdateSiteLocationRequest(21.0285, 105.8542, 500.0);
        SiteLocationResponse response = service.updateSiteLocation(projectId, request, userId);

        assertThat(response.configured()).isTrue();
        assertThat(response.latitude()).isEqualTo(21.0285);
        assertThat(response.longitude()).isEqualTo(105.8542);
        assertThat(response.radiusMeters()).isEqualTo(500.0);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(projectCaptor.capture());
        Project saved = projectCaptor.getValue();
        assertThat(saved.getSiteLatitude()).isEqualTo(21.0285);
        assertThat(saved.getSiteLongitude()).isEqualTo(105.8542);
        assertThat(saved.getSiteRadiusMeters()).isEqualTo(500.0);

        verify(authorizationService).checkProjectAccess(eq(projectId), eq(userId), eq(new String[]{"ADMIN", "PROJECT_MANAGER"}));
    }

    @Test
    @DisplayName("T-71: Từ chối khi vĩ độ ngoài phạm vi [-90, 90]")
    void updateSiteLocation_Fails_WhenLatitudeOutOfRange() {
        UpdateSiteLocationRequest reqTooHigh = new UpdateSiteLocationRequest(95.0, 105.0, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqTooHigh, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Vĩ độ phải nằm trong khoảng từ -90 đến 90 độ");

        UpdateSiteLocationRequest reqTooLow = new UpdateSiteLocationRequest(-91.0, 105.0, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqTooLow, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Vĩ độ phải nằm trong khoảng từ -90 đến 90 độ");
    }

    @Test
    @DisplayName("T-71: Từ chối khi kinh độ ngoài phạm vi [-180, 180]")
    void updateSiteLocation_Fails_WhenLongitudeOutOfRange() {
        UpdateSiteLocationRequest reqTooHigh = new UpdateSiteLocationRequest(21.0, 185.0, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqTooHigh, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Kinh độ phải nằm trong khoảng từ -180 đến 180 độ");

        UpdateSiteLocationRequest reqTooLow = new UpdateSiteLocationRequest(21.0, -185.0, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqTooLow, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Kinh độ phải nằm trong khoảng từ -180 đến 180 độ");
    }

    @Test
    @DisplayName("T-71: Từ chối khi bán kính nhỏ hơn hoặc bằng 0")
    void updateSiteLocation_Fails_WhenRadiusZeroOrNegative() {
        UpdateSiteLocationRequest reqZero = new UpdateSiteLocationRequest(21.0, 105.0, 0.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqZero, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bán kính công trường phải lớn hơn 0 mét");

        UpdateSiteLocationRequest reqNeg = new UpdateSiteLocationRequest(21.0, 105.0, -50.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqNeg, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Bán kính công trường phải lớn hơn 0 mét");
    }

    @Test
    @DisplayName("T-71: Từ chối khi cấu hình một phần (thiếu lat, lon hoặc radius)")
    void updateSiteLocation_Fails_WhenFieldIsNull() {
        UpdateSiteLocationRequest reqMissingLat = new UpdateSiteLocationRequest(null, 105.0, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqMissingLat, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("phải cung cấp đầy đủ");

        UpdateSiteLocationRequest reqMissingLon = new UpdateSiteLocationRequest(21.0, null, 200.0);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqMissingLon, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("phải cung cấp đầy đủ");

        UpdateSiteLocationRequest reqMissingRadius = new UpdateSiteLocationRequest(21.0, 105.0, null);
        assertThatThrownBy(() -> service.updateSiteLocation(projectId, reqMissingRadius, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("phải cung cấp đầy đủ");
    }
}
