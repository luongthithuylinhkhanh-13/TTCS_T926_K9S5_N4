# Đặc Tả Kiểm Thử Phân Quyền & Thành Viên Dự Án (Module T-04: Authorization & RBAC)

Tài liệu kiểm thử tương ứng với các thẻ công việc (Jira Tasks) từ **NTDHTCT-60** đến **NTDHTCT-71**.

---

## 1. Danh sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-60** | T-04.1 - Thiết kế bảng Roles | `TC-AUTH-01` | Kiểm tra migration và khởi tạo danh mục Role chuẩn (ADMIN, PROJECT_MANAGER, SITE_ENGINEER, WORKER, VIEWER) | Các Role được lưu trữ chính xác với mã code duy nhất | **Passed** |
| **NTDHTCT-61** | T-04.2 - Thiết kế bảng Project Members | `TC-AUTH-02` | Kiểm tra cấu trúc bảng `project_members`, các khoá ngoại và ràng buộc duy nhất (Unique: `project_id, user_id`) | Bảng tạo thành công, khoá ngoại và index hoạt động đúng | **Passed** |
| **NTDHTCT-62** | T-04.3 - Liên kết User với Project | `TC-AUTH-03` | Kiểm tra quan hệ N-N giữa User và Project thông qua ProjectMember | Dữ liệu liên kết nhất quán, xoá project cascade xoá member | **Passed** |
| **NTDHTCT-63** | T-04.4 - Gán Role cho User trong Project | `TC-AUTH-04` | Quản lý dự án thực hiện gán vai trò, cập nhật vai trò, xoá thành viên khỏi dự án | Trả về HTTP 201 Created khi thêm mới, HTTP 200 khi cập nhật/xoá | **Passed** |
| **NTDHTCT-64** | T-04.5 - Tạo Middleware Authorization | `TC-AUTH-05` | Interceptor can thiệp các request vào `/api/projects/**`, trích xuất User ID và Project ID | Request không hợp lệ bị chặn ngay từ lớp tiền xử lý | **Passed** |
| **NTDHTCT-65** | T-04.6 - Kiểm tra User có thuộc Project hay không | `TC-AUTH-06` | User không có bản ghi trong `project_members` gửi request đến dự án | Trả về HTTP 403 Forbidden với mã lỗi `PROJECT_ACCESS_DENIED` | **Passed** |
| **NTDHTCT-66** | T-04.7 - Kiểm tra Role của User | `TC-AUTH-07` | User thuộc dự án nhưng có role không đủ quyền (ví dụ WORKER truy cập nhật ký kỹ sư) | Trả về HTTP 403 Forbidden với mã lỗi `INSUFFICIENT_PROJECT_ROLE` | **Passed** |
| **NTDHTCT-67** | T-04.8 - Cấu hình quyền theo từng Route | `TC-AUTH-08` | Sử dụng `@RequireProjectRole` và `RoutePermissionConfig` để thiết lập quyền cho từng endpoint | Chỉ các role được chỉ định trong cấu hình mới có thể truy cập | **Passed** |
| **NTDHTCT-68** | T-04.9 - Thiết lập cơ chế Default Deny | `TC-AUTH-09` | Tuyến đường thuộc `/api/projects/**` không có cấu hình quyền tường minh | Hệ thống tự động từ chối mặc định với HTTP 403 `DEFAULT_DENY` | **Passed** |
| **NTDHTCT-69** | T-04.10 - Xử lý lỗi 403 khi không đủ quyền | `TC-AUTH-10` | Khi bị từ chối truy cập, hệ thống trả về cấu trúc JSON lỗi chuẩn mực | HTTP 403, JSON gồm `timestamp`, `status`, `error`, `code`, `message`, `path` | **Passed** |
| **NTDHTCT-70** | T-04.11 - Kiểm tra truy cập Project không thuộc thành viên | `TC-AUTH-11` | Thử truy cập Project mà user không tham gia, hoặc Project không tồn tại | Chặn 403 Forbidden hoặc báo 404 Not Found, không lộ dữ liệu nội bộ | **Passed** |
| **NTDHTCT-71** | T-04.12 - Viết Test Case cho Authorization | `TC-AUTH-12` | Xây dựng bộ Integration Test tự động hoá trong `AuthorizationIntegrationTest.java` | 100% test case pass tự động trong pipeline CI/CD | **Passed** |

---

## 2. Chi Tiết Các Kịch Bản Test Chính

### Kịch bản 1: Thêm thành viên và gán quyền (T-04.4)
- **Endpoint**: `POST /api/projects/{projectId}/members`
- **Header**: `X-User-Id: 1` (Manager)
- **Body**:
  ```json
  {
    "userId": 4,
    "roleCode": "VIEWER"
  }
  ```
- **Kết quả**: HTTP 201 Created, `success: true`.

### Kịch bản 2: Người ngoài truy cập dự án (T-04.6 & T-04.11)
- **Endpoint**: `GET /api/projects/{projectId}/members`
- **Header**: `X-User-Id: 99` (Không thuộc dự án)
- **Kết quả**: HTTP 403 Forbidden.
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "code": "PROJECT_ACCESS_DENIED",
    "message": "Bạn không phải là thành viên của dự án này [ID=1]. Quyền truy cập bị từ chối."
  }
  ```

### Kịch bản 3: Sai quyền hạn vai trò (T-04.7 & T-04.10)
- **Endpoint**: `POST /api/projects/{projectId}/members`
- **Header**: `X-User-Id: 3` (Role = WORKER)
- **Kết quả**: HTTP 403 Forbidden.
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "code": "INSUFFICIENT_PROJECT_ROLE",
    "message": "Vai trò của bạn [WORKER] không có quyền thực hiện hành động này. Yêu cầu một trong các quyền: [ADMIN, PROJECT_MANAGER]"
  }
  ```

### Kịch bản 4: Kích hoạt cơ chế Default Deny (T-04.9)
- **Endpoint**: `GET /api/projects/{projectId}/unconfigured-secure-endpoint`
- **Header**: `X-User-Id: 1` (Manager)
- **Kết quả**: HTTP 403 Forbidden.
  ```json
  {
    "status": 403,
    "error": "Forbidden",
    "code": "DEFAULT_DENY",
    "message": "Cơ chế bảo vệ Default Deny: Tuyến đường [...] chưa được cấp phép truy cập công khai. Truy cập bị từ chối mặc định."
  }
  ```
