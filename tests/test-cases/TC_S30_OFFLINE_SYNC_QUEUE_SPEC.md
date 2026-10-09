# Đặc Tả Kiểm Thử Hàng Đợi Đồng Bộ Ngoại Tuyến (Module S-30: Offline Sync Queue)

Tài liệu kiểm thử và minh chứng hoàn thành tương ứng với Story **NTDHTCT-214 (S-30)** và các Subtasks:
- **NTDHTCT-255 (T-70)**: Tạo hàng đợi cho dữ liệu chưa đồng bộ.
- **NTDHTCT-256 (T-71)**: Tự động gửi dữ liệu lên máy chủ khi kết nối mạng trở lại.
Thuộc Epic: **NTDHTCT-212 (E-05 - Nhật ký công trường)**.

---

## 1. Danh Sách Ma Trận Task & Test Case

| Mã Task | Tên Task | Mã Test Case | Mục Tiêu Kiểm Thử | Kết Quả Mong Đợi | Trạng Thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **NTDHTCT-255** | T-70 – Tạo hàng đợi cho dữ liệu chưa đồng bộ | `TC-SYNC-01` | Kiểm tra cơ chế `enqueue` lưu trữ bản ghi nhật ký vào LocalStorage khi mất kết nối mạng | Bản ghi được lưu trữ an toàn, sinh `clientSyncId`, trạng thái `PENDING` | **Passed** |
| **NTDHTCT-255** | T-70 – Tạo hàng đợi cho dữ liệu chưa đồng bộ | `TC-SYNC-02` | Kiểm tra khả năng lọc `getPendingItems` các bản ghi cần gửi, bỏ qua các bản ghi vượt quá max retries | Lấy chính xác danh sách các mục chờ đồng bộ | **Passed** |
| **NTDHTCT-255** | T-70 – Tạo hàng đợi cho dữ liệu chưa đồng bộ | `TC-SYNC-03` | Kiểm tra thống kê `getStats` tổng số lượng, số lượng chờ gửi, đang gửi và thất bại | Thông tin hiển thị chính xác trên StatusBar và Modal | **Passed** |
| **NTDHTCT-256** | T-71 – Tự động gửi dữ liệu lên máy chủ khi có mạng | `TC-SYNC-04` | Khi phát hiện sự kiện `online`, tự động kích hoạt `AutoSyncService` gửi dữ liệu lên API | Dữ liệu được gửi tuần tự lên máy chủ, cập nhật trạng thái `SYNCED` | **Passed** |
| **NTDHTCT-256** | T-71 – Tự động gửi dữ liệu lên máy chủ khi có mạng | `TC-SYNC-05` | Cơ chế **Idempotency**: Gửi lại cùng `clientSyncId` do mạng chập chờn không tạo bản ghi trùng | Máy chủ trả về HTTP 200 `ALREADY_EXISTS`, không nhân bản dữ liệu | **Passed** |
| **NTDHTCT-256** | T-71 – Tự động gửi dữ liệu lên máy chủ khi có mạng | `TC-SYNC-06` | Kiểm tra API Batch Sync (`POST /api/projects/{projectId}/site-diaries/batch-sync`) | Máy chủ nhận và xử lý hàng loạt nhiều bản ghi từ hàng đợi | **Passed** |
| **NTDHTCT-256** | T-71 – Tự động gửi dữ liệu lên máy chủ khi có mạng | `TC-SYNC-07` | Xử lý lỗi và cơ chế thử lại (Retry & Exponential Backoff) khi máy chủ tạm ngắt kết nối | Tăng `retryCount`, ghi nhận `lastError`, tiếp tục thử lại lần sau | **Passed** |
| **NTDHTCT-214** | S-30 – Hàng đợi đồng bộ tự gửi khi có mạng | `TC-SYNC-08` | Bảo toàn mốc thời gian ngoại tuyến `offlineCreatedAt` và liên kết dự án `projectId` | Thời gian ghi nhận tại hiện trường được giữ nguyên sau khi đồng bộ | **Passed** |

---

## 2. Chi Tiết Các Kịch Bản Test Chính

### Kịch bản 1: Lưu trữ dữ liệu vào hàng đợi khi mất mạng (T-70)
- **Môi trường**: Trình duyệt mất kết nối Internet (`navigator.onLine = false` hoặc kích hoạt chế độ Giả lập ngoại tuyến).
- **Thao tác**: Kỹ sư điền biểu mẫu Nhật ký công trường và nhấn **"Lưu nhật ký"**.
- **Kết quả thực tế**:
  - Dữ liệu không bị mất.
  - Tự động sinh `clientSyncId` UUID duy nhất.
  - Lưu trữ bền vững vào LocalStorage với key `ntdhtcct_offline_sync_queue_v1`.
  - Hiển thị thông báo: *"Đã lưu vào hàng đợi ngoại tuyến (T-70). Dữ liệu sẽ tự động gửi khi có mạng!"*.
  - Badge số lượng trên thanh `SyncQueueStatusBar` tăng lên.

### Kịch bản 2: Tự động gửi dữ liệu khi có mạng trở lại (T-71)
- **Sự kiện**: Thiết bị khôi phục kết nối mạng (bắn sự kiện `window.addEventListener('online')`).
- **Xử lý**:
  - `AutoSyncService` phát hiện sự kiện mạng, chuyển trạng thái `SYNCING`.
  - Quét danh sách các mục `PENDING` trong hàng đợi.
  - Gửi request `POST /api/projects/{projectId}/site-diaries/sync` lên máy chủ.
  - Khi nhận HTTP 201 Created: Gọi `syncQueueService.markSuccess()`, xóa bản ghi khỏi hàng đợi.
  - Phát thông báo Toast: *"Đã tự động đồng bộ thành công X mục dữ liệu lên máy chủ (T-71)"*.

### Kịch bản 3: Cơ chế Idempotent chống gửi trùng lặp (S-30 Backend)
- **Kịch bản**: Client gửi dữ liệu nhưng kết nối mạng bị rớt trước khi nhận response, Client gửi lại cùng một bản ghi (có cùng `clientSyncId`).
- **Endpoint**: `POST /api/projects/{projectId}/site-diaries/sync`
- **Body**:
  ```json
  {
    "clientSyncId": "sync-duplicate-check-UUID",
    "diaryDate": "2026-10-09",
    "workSummary": "Đổ bê tông sàn tầng 5 khu A",
    "workerCount": 45
  }
  ```
- **Kết quả**:
  - Lần 1: HTTP 201 Created, `status: "CREATED"`.
  - Lần 2: HTTP 200 OK, `status: "ALREADY_EXISTS"`, `serverDiaryId` trùng với bản ghi cũ.
  - Cơ sở dữ liệu: Chỉ tồn tại duy nhất 1 bản ghi trong bảng `site_diaries`.

### Kịch bản 4: Đồng bộ hàng loạt (Batch Sync)
- **Endpoint**: `POST /api/projects/{projectId}/site-diaries/batch-sync`
- **Body**:
  ```json
  {
    "items": [
      {
        "clientSyncId": "sync-01",
        "diaryDate": "2026-10-08",
        "workSummary": "Đào đất móng"
      },
      {
        "clientSyncId": "sync-02",
        "diaryDate": "2026-10-09",
        "workSummary": "Đổ bê tông lót móng"
      }
    ]
  }
  ```
- **Kết quả trả về**:
  ```json
  {
    "totalProcessed": 2,
    "createdCount": 2,
    "duplicateCount": 0,
    "failedCount": 0
  }
  ```

---

## 3. Bằng Chứng Thực Thi Kiểm Thử (Execution Evidence)

### Bộ kiểm thử Backend (Spring Boot & JUnit 5):
- File kiểm thử: `SiteDiarySyncIntegrationTest.java` & `SiteDiarySyncServiceTest.java`.
- Kết quả chạy tự động:
  ```text
  [INFO] Running com.ntdhtcct.domain.sitediary.SiteDiarySyncIntegrationTest
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
  [INFO] Running com.ntdhtcct.domain.sitediary.SiteDiarySyncServiceTest
  [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
  [INFO] -------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] Total time: 8.420 s
  ```

### Bộ kiểm thử Frontend (Node.js Test Runner):
- File kiểm thử: `frontend/tests/syncQueue.test.js`.
- Kết quả chạy tự động:
  ```text
  ✔ TC-SYNC-01: generateUUID tạo chuỗi UUID hợp lệ
  ✔ TC-SYNC-02: T-70 - Enqueue thêm bản ghi vào hàng đợi khi mất mạng
  ✔ TC-SYNC-03: T-70 - getPendingItems chỉ lấy các bản ghi PENDING hoặc FAILED chưa quá số lần retry
  ✔ TC-SYNC-04: T-71 - markSuccess xóa bản ghi khỏi hàng đợi sau khi gửi máy chủ thành công
  ✔ TC-SYNC-05: T-71 - markFailed cập nhật trạng thái FAILED và ghi nhận chi tiết lỗi
  ✔ TC-SYNC-06: T-70 - getStats tính toán đúng số lượng trạng thái hàng đợi
  ℹ tests 6
  ℹ suites 0
  ℹ pass 6
  ℹ fail 0
  ```

---

## 4. Tổng Kết
Toàn bộ các yêu cầu của Story **S-30 (NTDHTCT-214)** và 2 subtasks **T-70 (NTDHTCT-255)**, **T-71 (NTDHTCT-256)** đã được hiện thực hóa trọn vẹn cả phía Client (Frontend) và Server (Backend), bao gồm:
1. Module hàng đợi ngoại tuyến LocalStorage bền vững (`SyncQueueService`).
2. Bộ lắng nghe kết nối mạng và tự động gửi dữ liệu (`AutoSyncService`).
3. Giao diện người dùng hiển thị trạng thái mạng, badge hàng đợi (`SyncQueueStatusBar`, `SyncQueueModal`, `SiteDiaryPage`).
4. API máy chủ tiếp nhận đồng bộ đơn lẻ và hàng loạt có bảo đảm tính **Idempotent**.
5. Cơ sở dữ liệu và migration Liquibase `V16_0__create_site_diaries_and_sync_queue.xml`.
6. Bộ test tự động 100% Passed.
