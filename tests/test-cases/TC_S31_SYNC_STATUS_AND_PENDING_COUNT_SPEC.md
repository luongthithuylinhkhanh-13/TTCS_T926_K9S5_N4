# Đặc Tả Kiểm Thử Trạng Thái Chưa Đồng Bộ Và Số Mục Chờ Đồng Bộ (Module S-31: Sync Status & Pending Count)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-215 (S-31: Trạng thái chưa đồng bộ và số mục)** và các Subtasks:
- **NTDHTCT-257 (T-72)**: Hiển thị trạng thái chưa đồng bộ của nhật ký.
- **NTDHTCT-258 (T-73)**: Hiển thị số lượng mục đang chờ đồng bộ.
Thuộc Epic: **NTDHTCT-212 (E-05 - Nhật ký công trường)**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-257** | T-72 – Hiển thị trạng thái chưa đồng bộ của nhật ký | `TC-S31-01` | Kiểm tra bản ghi nhật ký tạo ngoại tuyến có trạng thái "Chưa đồng bộ" và cờ `_isPendingSync = true` | Hiển thị Tag màu cam `Chưa đồng bộ`, tooltip ghi rõ thời gian lưu ngoại tuyến | **Passed** |
| **NTDHTCT-257** | T-72 – Hiển thị trạng thái chưa đồng bộ của nhật ký | `TC-S31-02` | Cập nhật và phân biệt chi tiết các trạng thái: `SYNCING` (Đang đồng bộ) và `FAILED` (Lỗi đồng bộ) | Tag chuyển sang xanh dương đang gửi hoặc đỏ báo lỗi kèm `lastError` và `retryCount` | **Passed** |
| **NTDHTCT-257** | T-72 – Hiển thị trạng thái chưa đồng bộ của nhật ký | `TC-S31-03` | Hàm `getDiarySyncStatus` xác định chính xác trạng thái qua `clientSyncId` (`PENDING`, `SYNCING`, `FAILED`, `SYNCED`) | Trả về đúng mã trạng thái đồng bộ của bản ghi | **Passed** |
| **NTDHTCT-258** | T-73 – Hiển thị số lượng mục đang chờ đồng bộ | `TC-S31-04` | Tính toán chính xác số lượng mục đang chờ đồng bộ (`pendingCount`) theo từng dự án và toàn cục | Đếm chính xác số lượng mục `PENDING`, `SYNCING`, `FAILED` trong hàng đợi | **Passed** |
| **NTDHTCT-258** | T-73 – Hiển thị số lượng mục đang chờ đồng bộ | `TC-S31-05` | Số lượng mục chờ đồng bộ tự động giảm chính xác sau khi đồng bộ thành công hoặc xóa bỏ | Số lượng đếm giảm tức thì và badge trạng thái cập nhật theo thời gian thực | **Passed** |
| **NTDHTCT-258** | T-73 – Hiển thị số lượng mục đang chờ đồng bộ | `TC-S31-06` | Phân loại bộ lọc (Filter) giữa "Tất cả", "Chưa đồng bộ" và "Đã đồng bộ" trên giao diện danh sách nhật ký | Lọc chính xác danh sách theo trạng thái và hiển thị số lượng tương ứng | **Passed** |
| **NTDHTCT-215** | S-31 – Backend API Sync Status | `TC-S31-07` | API `GET /api/projects/{projectId}/site-diaries/sync-status` trả về thống kê tổng số, số đã đồng bộ và mốc thời gian | Trả về HTTP 200 OK kèm `totalDiaries`, `syncedCount`, `pendingCount`, `lastSyncedAt` | **Passed** |

---

## 2. Chi Tiết Kịch Bản Kiểm Thử Giao Diện & Hành Vi (UI / UX)

### Kịch bản 1: Hiển thị trạng thái chưa đồng bộ của nhật ký (T-72)
- **Môi trường**: Thiết bị không có mạng hoặc mạng chập chờn khi kỹ sư nhập nhật ký công trường.
- **Thao tác**: Người dùng tạo nhật ký tại hiện trường khi offline.
- **Kết quả hiển thị**:
  1. Trong bảng Lịch sử nhật ký:
     - Cột **Trạng thái đồng bộ** hiển thị Tag cảnh báo màu vàng/cam: `<Tag color="warning">Chưa đồng bộ</Tag>`.
     - Hover chuột hiển thị tooltip: *"Bản ghi được lưu trên thiết bị lúc HH:mm DD/MM/YYYY. Đang chờ gửi lên máy chủ."*
  2. Trong Modal Chi tiết nhật ký:
     - Hộp thông báo màu vàng nổi bật ở đầu trang:
       - Trạng thái: **Chưa đồng bộ (Ngoại tuyến)**.
       - Hướng dẫn: *"Bản ghi này đang được lưu trên thiết bị và chưa được tải lên máy chủ. Hệ thống sẽ tự động đồng bộ khi có kết nối mạng."*
       - Thời điểm tạo ngoại tuyến được hiển thị chính xác.

### Kịch bản 2: Hiển thị số lượng mục đang chờ đồng bộ (T-73)
- **Giao diện trang Nhật ký (`SiteDiaryPage`)**:
  1. **Thanh thẻ tóm tắt (Summary Cards)**:
     - Thẻ **Chờ đồng bộ (T-73)** hiển thị số lượng lớn nổi bật (ví dụ: `3 mục`) với Badge trạng thái cảnh báo màu cam.
     - Thẻ **Tổng số nhật ký** và thẻ **Đã đồng bộ** hiển thị số lượng tương ứng.
  2. **Banner cảnh báo trực quan**:
     - Khi `pendingCount > 0`: Xuất hiện Alert thông báo:
       *"Đang có {pendingCount} mục nhật ký chờ đồng bộ lên máy chủ"*.
     - Cung cấp nút thao tác nhanh: **"Đồng bộ ngay ({pendingCount} mục)"** (khi có mạng) và nút **"Xem hàng đợi ({pendingCount})"**.
  3. **Thanh điều hướng chung (`SyncQueueStatusBar`)**:
     - Tooltip hiển thị: *"Số lượng mục đang chờ đồng bộ: {pendingCount} mục"*.
     - Badge màu cam/đỏ cập nhật số lượng chờ gửi.
     - Nút đồng bộ hiển thị số lượng: *"Đồng bộ ngay ({pendingCount})"*.

### Kịch bản 3: Lọc danh sách theo trạng thái đồng bộ
- **Thao tác**: Kỹ sư chọn bộ lọc *"Chưa đồng bộ (3)"* trong ô `Select`.
- **Kết quả**:
  - Bảng chỉ hiển thị 3 bản ghi chưa đồng bộ.
  - Khi chọn *"Đã đồng bộ (15)"*, bảng hiển thị 15 bản ghi đã lưu trên server.
  - Khi chọn *"Tất cả (18)"*, hiển thị toàn bộ 18 bản ghi.

---

## 3. Bằng Chứng Thực Thi Kiểm Thử Tự Động (Execution Evidence)

### Bộ kiểm thử Frontend (Node.js Test Runner):
- File: `frontend/tests/syncQueue.test.js`
- Lệnh chạy: `node --test frontend/tests/syncQueue.test.js`
- Kết quả chạy tự động:
  ```text
  ✔ TC-SYNC-01: generateUUID tạo chuỗi UUID hợp lệ
  ✔ TC-SYNC-02: T-70 - Enqueue thêm bản ghi vào hàng đợi khi mất mạng
  ✔ TC-SYNC-03: T-70 - getPendingItems chỉ lấy các bản ghi PENDING hoặc FAILED chưa quá số lần retry
  ✔ TC-SYNC-04: T-71 - markSuccess xóa bản ghi khỏi hàng đợi sau khi gửi máy chủ thành công
  ✔ TC-SYNC-05: T-71 - markFailed cập nhật trạng thái FAILED và ghi nhận chi tiết lỗi
  ✔ TC-SYNC-06: T-70 - getStats tính toán đúng số lượng trạng thái hàng đợi
  ✔ TC-S29-01: Nhật ký tạo khi offline được lưu trên thiết bị với trạng thái chờ đồng bộ
  ✔ TC-S29-02: Không báo lưu thành công nếu thiết bị không ghi được localStorage
  ✔ TC-S34: Ghi 20 nhật ký khi mất mạng và đồng bộ đầy đủ khi kết nối trở lại
  ✔ TC-S31-01: T-72 - Bản ghi ngoại tuyến có trạng thái "Chưa đồng bộ" và _isPendingSync = true
  ✔ TC-S31-02: T-72 - Cập nhật trạng thái "Đang đồng bộ" và "Lỗi đồng bộ" kèm chi tiết lỗi
  ✔ TC-S31-03: T-72 - Kiểm tra hàm getDiarySyncStatus xác định đúng trạng thái
  ✔ TC-S31-04: T-73 - Tính toán chính xác số lượng mục đang chờ đồng bộ (pendingCount)
  ✔ TC-S31-05: T-73 - Số lượng mục chờ đồng bộ tự động giảm chính xác sau khi đồng bộ thành công hoặc xóa
  ✔ TC-S31-06: T-72 & T-73 - Phân loại bộ lọc (Filter) giữa Chưa đồng bộ và Đã đồng bộ
  ℹ tests 15
  ℹ suites 0
  ℹ pass 15
  ℹ fail 0
  ```

### Bộ kiểm thử Backend (JUnit 5 & Spring Boot):
- File: `SiteDiarySyncServiceTest.java`
- Lệnh chạy: `mvn test -Dtest=SiteDiarySyncServiceTest`
- Kết quả chạy tự động:
  ```text
  [INFO] Running com.ntdhtcct.domain.sitediary.SiteDiarySyncServiceTest
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  ```

### Kiểm thử Biên Dịch Production (Frontend Build):
- Lệnh chạy: `npm run build`
- Kết quả:
  ```text
  ✓ 3233 modules transformed.
  ✓ built in 42.22s
  dist/index.html                                       1.00 kB
  dist/assets/index-CYobAM36.css                       35.85 kB
  dist/assets/index-VMcnQNpY.js                     1,613.96 kB
  ```

---

## 4. Kết Luận
Toàn bộ các tiêu chí nghiệm thu của Story **S-31 (NTDHTCT-215)** và 2 Subtasks **T-72 (NTDHTCT-257)**, **T-73 (NTDHTCT-258)** đã hoàn thành 100%, bảo đảm chất lượng code, giao diện trực quan và bộ kiểm thử tự động vượt qua tất cả các bài test.
