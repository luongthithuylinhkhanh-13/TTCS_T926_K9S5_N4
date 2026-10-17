# Đặc Tả Kiểm Thử Ghi Nhận Chi Phí Thực Tế Gắn Với Hạng Mục (Module S-47)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-237 (S-47)** và các Subtasks:
- **NTDHTCT-306 (T-94)**: Xây dựng chức năng nhập chi phí thực tế.
- **NTDHTCT-307 (T-95)**: Liên kết chi phí với hạng mục tương ứng.
- **NTDHTCT-308 (T-96)**: Lưu và hiển thị lịch sử chi phí.
Thuộc Epic: **NTDHTCT-227 (E-09 - Quản lý Dự toán & Thực chi)**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-306** | T-94 – Xây dựng chức năng nhập chi phí thực tế | `TC-COST-01` | Nhập chi phí thực tế với đầy đủ thông tin hợp lệ | Lưu thành công vào CSDL (`actual_costs`), API trả về HTTP 201 Created | **Passed** |
| **NTDHTCT-306** | T-94 – Xây dựng chức năng nhập chi phí thực tế | `TC-COST-02` | Kiểm tra từ chối khi số tiền thực chi âm hoặc bằng 0 (`amount <= 0`) | Báo lỗi rõ ràng: *"Số tiền chi phí phải lớn hơn 0"*, API trả về HTTP 400 Bad Request | **Passed** |
| **NTDHTCT-306** | T-94 – Xây dựng chức năng nhập chi phí thực tế | `TC-COST-03` | Kiểm tra bắt buộc nhập tên khoản chi và ngày phát sinh chi phí | Báo lỗi: *"Tên chi phí không được để trống"*, *"Ngày phát sinh chi phí không được để trống"* | **Passed** |
| **NTDHTCT-307** | T-95 – Liên kết chi phí với hạng mục tương ứng | `TC-COST-04` | Liên kết khoản chi phí thực tế với một hạng mục WBS cụ thể của dự án | Bản ghi lưu đúng `wbs_item_id`, truy vấn theo hạng mục trả về chính xác khoản chi | **Passed** |
| **NTDHTCT-307** | T-95 – Liên kết chi phí với hạng mục tương ứng | `TC-COST-05` | Ngăn chặn việc liên kết khoản chi với hạng mục thuộc dự án khác | Từ chối và trả về lỗi: *"Hạng mục công việc không thuộc dự án này"* | **Passed** |
| **NTDHTCT-308** | T-96 – Lưu và hiển thị lịch sử chi phí | `TC-COST-06` | Truy vấn và hiển thị danh sách lịch sử chi phí thực tế của toàn dự án | Hiển thị bảng lịch sử sắp xếp theo ngày phát sinh mới nhất, đầy đủ phân loại, số tiền và người nhập | **Passed** |
| **NTDHTCT-308** | T-96 – Lưu và hiển thị lịch sử chi phí | `TC-COST-07` | Lọc lịch sử chi phí theo hạng mục WBS, theo loại chi phí và theo khoảng ngày | Dữ liệu được lọc chính xác theo các tiêu chí đã chọn | **Passed** |
| **NTDHTCT-308** | T-96 – Lưu và hiển thị lịch sử chi phí | `TC-COST-08` | Tính toán chính xác tổng chi phí thực tế lũy kế của dự án và của từng hạng mục | Tổng số tiền chi phí phản ánh đúng tổng các khoản chi phát sinh theo thời gian thực | **Passed** |

---

## 2. Chi Tiết Kịch Bản Kiểm Thử Nghiệp Vụ

### Kịch bản 1: Nhập chi phí thực tế và liên kết với hạng mục (T-94, T-95)
- **Thao tác**: Người dùng mở modal **"Nhập chi phí thực tế"** từ trang Chi phí hoặc từ Drawer chi tiết công việc trên WBS.
- **Dữ liệu nhập**:
  - Tên khoản chi: `Mua thép Pomina D20 phục vụ đài cọc móng`
  - Số tiền thực chi: `85,000,000 VNĐ`
  - Phân loại: `VẬT TƯ`
  - Ngày phát sinh: `17/10/2026`
  - Hạng mục liên kết: `[1.1] Thi công cọc và móng`
  - Ghi chú: `Hóa đơn GTGT số HD-00918`
- **Kết quả**:
  - Khoản chi được ghi nhận thành công.
  - Khi xem chi tiết hạng mục `[1.1]`, mục "Chi phí thực tế" lập tức hiển thị khoản chi 85.000.000 đ và cộng dồn vào tổng chi phí thực tế của hạng mục.

### Kịch bản 2: Tra cứu lịch sử và thống kê chi phí thực tế (T-96)
- **Thao tác**: Người dùng truy cập trang **"Chi phí thực tế"** trên thanh menu Sidebar.
- **Kết quả hiển thị**:
  - Thẻ thống kê KPI: Tổng chi phí thực tế lũy kế toàn dự án, Số lượng khoản chi đã nhập, Khoản chi gần nhất.
  - Bảng lịch sử chi tiết: Cột ngày, tên khoản chi, hạng mục liên kết, loại chi phí, số tiền định dạng VNĐ và người nhập.
  - Hỗ trợ lọc nhanh theo từng hạng mục WBS hoặc theo loại chi phí (Vật tư, Nhân công, Máy móc...).
