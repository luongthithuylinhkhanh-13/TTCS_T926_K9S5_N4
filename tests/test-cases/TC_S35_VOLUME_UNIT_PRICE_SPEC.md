# Đặc Tả Kiểm Thử Khai Báo Khối Lượng, Đơn Vị Tính Và Đơn Giá (Module S-35)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-219 (S-35)** và các Subtasks:
- **NTDHTCT-265 (T-80)**: Tạo form khai báo khối lượng, đơn vị tính và đơn giá.
- **NTDHTCT-266 (T-81)**: Lưu và cập nhật thông tin khối lượng công việc.
- **NTDHTCT-267 (T-82)**: Kiểm tra dữ liệu khối lượng, đơn vị tính và đơn giá hợp lệ.
Thuộc Epic: **NTDHTCT-15 (E-03 - Cơ cấu tổ chức và WBS)**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-265** | T-80 – Tạo form khai báo khối lượng, đơn vị tính và đơn giá | `TC-VOL-01` | Tính chính xác thành tiền dự toán tự động trên form: `Khối lượng * Đơn giá` | Hiển thị giá trị ước tính định dạng tiền tệ VNĐ theo thời gian thực | **Passed** |
| **NTDHTCT-265** | T-80 – Tạo form khai báo khối lượng, đơn vị tính và đơn giá | `TC-VOL-02` | Khởi tạo và liên kết các trường `quantity`, `unit`, `unitPrice` trên `WorkFormModal` và `CategoryTaskSection` | Dữ liệu được nạp đầy đủ khi chỉnh sửa và reset sạch khi tạo mới | **Passed** |
| **NTDHTCT-266** | T-81 – Lưu và cập nhật thông tin khối lượng công việc | `TC-VOL-03` | Lưu thành công bản ghi WbsItem có khối lượng, đơn vị tính và đơn giá xuống CSDL | CSDL lưu giữ đúng `total_volume`, `unit`, `unit_price`, API trả về HTTP 200/201 | **Passed** |
| **NTDHTCT-266** | T-81 – Lưu và cập nhật thông tin khối lượng công việc | `TC-VOL-04` | Cập nhật thông tin khối lượng và đơn giá của công việc hiện có | Dữ liệu thay đổi được cập nhật chính xác, recalculate CPM không bị gián đoạn | **Passed** |
| **NTDHTCT-266** | T-81 – Lưu và cập nhật thông tin khối lượng công việc | `TC-VOL-05` | Lưu và hiển thị công việc trong hạng mục (`Task` thuộc Category) qua `CategoryTaskController` | API trả về `TaskResponse` có đầy đủ `quantity`, `unit`, `unitPrice`, `totalPrice` | **Passed** |
| **NTDHTCT-267** | T-82 – Kiểm tra dữ liệu khối lượng, đơn vị tính và đơn giá hợp lệ | `TC-VOL-06` | Kiểm tra từ chối khối lượng âm (`quantity < 0`) ở cả Frontend Form và Backend Service | Báo lỗi rõ ràng: *"Khối lượng công việc không được âm"* | **Passed** |
| **NTDHTCT-267** | T-82 – Kiểm tra dữ liệu khối lượng, đơn vị tính và đơn giá hợp lệ | `TC-VOL-07` | Kiểm tra từ chối đơn giá âm (`unitPrice < 0`) ở cả Frontend Form và Backend Service | Báo lỗi rõ ràng: *"Đơn giá không được âm"* | **Passed** |
| **NTDHTCT-267** | T-82 – Kiểm tra dữ liệu khối lượng, đơn vị tính và đơn giá hợp lệ | `TC-VOL-08` | Bắt buộc phải có đơn vị tính khi đã khai báo khối lượng `> 0` | Báo lỗi: *"Vui lòng nhập đơn vị tính khi đã khai báo khối lượng"* | **Passed** |
| **NTDHTCT-267** | T-82 – Kiểm tra dữ liệu khối lượng, đơn vị tính và đơn giá hợp lệ | `TC-VOL-09` | Giới hạn độ dài đơn vị tính tối đa 50 ký tự | Báo lỗi: *"Đơn vị tính không được vượt quá 50 ký tự"* | **Passed** |

---

## 2. Chi Tiết Kịch Bản Kiểm Thử

### Kịch bản 1: Khai báo đầy đủ trên Giao diện và Kiểm tra Thành tiền (T-80)
- **Thao tác**: Người dùng mở modal **Thêm công việc mới** hoặc **Chỉnh sửa công việc** trên trang WBS.
- **Dữ liệu nhập**:
  - Khối lượng: `150.5`
  - Đơn vị tính: `m3`
  - Đơn giá: `2,500,000`
- **Kết quả hiển thị**:
  - Dòng thành tiền dự toán ước tính lập tức xuất hiện: `376,250,000 VNĐ`.
  - Khi lưu, dữ liệu được gửi trọn vẹn lên API và hiển thị chi tiết trong `WorkDetailDrawer`.

### Kịch bản 2: Kiểm tra bắt lỗi dữ liệu không hợp lệ (T-82)
- **Trường hợp 1 (Khối lượng âm)**: Nhập `-10` -> Form cảnh báo đỏ, không cho submit. Backend trả về HTTP 400 Bad Request nếu request cố tình gửi qua API.
- **Trường hợp 2 (Đơn giá âm)**: Nhập `-500,000` -> Form chặn submit, backend từ chối `Đơn giá không được âm`.
- **Trường hợp 3 (Có khối lượng nhưng bỏ trống ĐVT)**: Nhập khối lượng `50`, xóa trắng ĐVT -> Form yêu cầu *"Vui lòng nhập đơn vị tính khi đã có khối lượng"*.
