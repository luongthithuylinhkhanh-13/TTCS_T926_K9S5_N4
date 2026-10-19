package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("S-35 / NTDHTCT-219: Khai báo khối lượng, đơn vị tính và đơn giá WbsItem")
class WbsVolumeUnitPriceTest {

    private final WbsItemRepository wbsItemRepository = mock(WbsItemRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    private final CpmEngine cpmEngine = new CpmEngine();

    private final WbsService service = new WbsService(
            wbsItemRepository,
            projectRepository,
            projectMemberRepository,
            cpmEngine
    );

    private final UUID projectId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(projectRepository.findById(projectId))
                .thenReturn(Optional.of(new Project("TEST", "Test project")));
        when(wbsItemRepository.existsByProjectIdAndWbsCode(projectId, "1.1"))
                .thenReturn(false);
        when(wbsItemRepository.save(any(WbsItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("T-81: Lưu thành công công việc với khối lượng, đơn vị tính và đơn giá hợp lệ")
    void createsTaskWithValidVolumeUnitAndPrice() {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Đổ bê tông móng");
        item.setType("task");
        item.setDuration(5);
        item.setQuantity(150.5);
        item.setUnit("m3");
        item.setUnitPrice(2500000.0);

        WbsItem created = service.create(projectId, item);

        assertThat(created.getQuantity()).isEqualTo(150.5);
        assertThat(created.getTotalVolume()).isEqualTo(150.5);
        assertThat(created.getUnit()).isEqualTo("m3");
        assertThat(created.getUnitPrice()).isEqualTo(2500000.0);
        assertThat(created.getTotalPrice()).isEqualTo(376250000.0);
    }

    @Test
    @DisplayName("T-81: Cập nhật thành công khối lượng, đơn vị tính và đơn giá")
    void updatesTaskVolumeAndUnitPrice() {
        UUID itemId = UUID.randomUUID();
        WbsItem existing = new WbsItem();
        existing.setProjectId(projectId);
        existing.setWbsCode("1.1");
        existing.setName("Đào đất");
        existing.setType("task");
        existing.setDuration(3);
        existing.setVersion(1L);

        when(wbsItemRepository.findById(itemId)).thenReturn(Optional.of(existing));

        WbsItem updateRequest = new WbsItem();
        updateRequest.setName("Đào đất móng sâu");
        updateRequest.setType("task");
        updateRequest.setDuration(4);
        updateRequest.setVersion(1L);
        updateRequest.setQuantity(200.0);
        updateRequest.setUnit("m3");
        updateRequest.setUnitPrice(80000.0);

        WbsItem updated = service.update(projectId, itemId, updateRequest);

        assertThat(updated.getQuantity()).isEqualTo(200.0);
        assertThat(updated.getUnit()).isEqualTo("m3");
        assertThat(updated.getUnitPrice()).isEqualTo(80000.0);
        assertThat(updated.getTotalPrice()).isEqualTo(16000000.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, -10.0, -100.0})
    @DisplayName("T-82: Từ chối khối lượng công việc âm")
    void rejectsNegativeVolume(double negativeVolume) {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Gia công cốt thép");
        item.setDuration(2);
        item.setQuantity(negativeVolume);
        item.setUnit("tấn");

        assertThatThrownBy(() -> service.create(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Khối lượng công việc không được âm");

        verify(wbsItemRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -50000.0})
    @DisplayName("T-82: Từ chối đơn giá âm")
    void rejectsNegativeUnitPrice(double negativePrice) {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Lắp dựng cốp pha");
        item.setDuration(2);
        item.setQuantity(50.0);
        item.setUnit("m2");
        item.setUnitPrice(negativePrice);

        assertThatThrownBy(() -> service.create(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Đơn giá không được âm");

        verify(wbsItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-82: Bắt buộc đơn vị tính khi có khai báo khối lượng > 0")
    void requiresUnitWhenVolumeIsSpecified() {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Xây tường gạch");
        item.setDuration(3);
        item.setQuantity(120.0);
        item.setUnit("   "); // Trống

        assertThatThrownBy(() -> service.create(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng nhập đơn vị tính khi đã khai báo khối lượng");

        verify(wbsItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("T-82: Từ chối đơn vị tính dài quá 50 ký tự")
    void rejectsUnitExceeding50Characters() {
        WbsItem item = new WbsItem();
        item.setWbsCode("1.1");
        item.setName("Xây tường gạch");
        item.setDuration(3);
        item.setQuantity(10.0);
        item.setUnit("a".repeat(51));

        assertThatThrownBy(() -> service.create(projectId, item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Đơn vị tính không được vượt quá 50 ký tự");

        verify(wbsItemRepository, never()).save(any());
    }
}
