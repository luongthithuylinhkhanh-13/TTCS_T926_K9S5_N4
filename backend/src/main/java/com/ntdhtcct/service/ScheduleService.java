package com.ntdhtcct.service;

import com.ntdhtcct.dto.*;

import java.util.List;

/**
 * T-18 & T-19 (NTDHTCT-144, NTDHTCT-145): Service quản lý công việc, quan hệ phụ thuộc và tính toán tiến độ CPM (ES/EF).
 */
public interface ScheduleService {

    /**
     * Tạo công việc mới cho dự án.
     */
    TaskResponse createTask(Long projectId, CreateTaskRequest request);

    /**
     * Lấy danh sách công việc của dự án.
     */
    List<TaskResponse> getTasks(Long projectId);

    /**
     * Tạo mối quan hệ phụ thuộc giữa 2 công việc (T-18: FS, SS, FF, SF).
     */
    TaskDependencyResponse createDependency(Long projectId, CreateDependencyRequest request);

    /**
     * Lấy danh sách các quan hệ phụ thuộc trong dự án.
     */
    List<TaskDependencyResponse> getDependencies(Long projectId);

    /**
     * T-19: Thực hiện tính toán duyệt tiến (Forward Pass) tính ES và EF cho toàn bộ công việc trong dự án.
     *
     * @param projectId ID của dự án
     * @param saveToDb  Nếu true, cập nhật các giá trị ES và EF tính được vào Database
     * @return Kết quả tính toán tiến độ
     */
    ScheduleCalculationResponse calculateForwardPass(Long projectId, boolean saveToDb);

    /**
     * Lấy kết quả tiến độ hiện tại của dự án.
     */
    ScheduleCalculationResponse getSchedule(Long projectId);
}
