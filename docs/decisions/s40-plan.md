# Implementation Plan: Epic S-40 — Siêu dữ liệu chụp ảnh nhật ký địa điểm

> **Tài liệu đặc tả kỹ thuật triển khai cho 3 Subtask:**
> - **T-68:** Chụp/tải ảnh hiện trường lên hệ thống.
> - **T-69:** Tự động ghi nhận ngày giờ của ảnh.
> - **T-70:** Hiển thị thời điểm chụp trong thông tin ảnh.

---

## 1. Tóm tắt hiện trạng và các quyết định kỹ thuật đã có

### 1.1. Hiện trạng codebase sau kiểm tra thực tế
Sau khi kiểm tra toàn diện mã nguồn (`git log`, diffs giữa nhánh hiện tại và `origin/main`, các module frontend và backend):
1. **Kiến trúc lưu trữ và Upload 2 giai đoạn (S-23):**
   - Đã có đầy đủ cơ chế upload hai giai đoạn trong `diaryUploadService.js` và `diaryApi.js`: Giai đoạn 1 nén hiển thị (< 1 MiB), Giai đoạn 2 tải ảnh gốc (< 20 MiB).
   - Backend `DiaryController.java` và `DiaryService.java` cung cấp 2 endpoint:
     - `POST /api/projects/{projectId}/diary-entries/{entryId}/images/display`
     - `POST /api/projects/{projectId}/diary-entries/{entryId}/images/original`
   - Bảng cơ sở dữ liệu `diary_images` (migration `V16_0__create_diary_tables.xml`) đã có sẵn các cột cần thiết:
     - `captured_at` (`TIMESTAMP WITH TIME ZONE`, nullable)
     - `latitude` (`DOUBLE PRECISION`, nullable)
     - `longitude` (`DOUBLE PRECISION`, nullable)
     - `display_status`, `original_status`, `display_attempt_id`, `original_attempt_id`.
   - Entity `DiaryImage.java` đã ánh xạ trường `capturedAt` (`java.time.OffsetDateTime`).

2. **Cơ chế trích xuất EXIF và kiểm tra Metadata (T-74, T-75, T-76 đã merge vào `origin/main`):**
   - **Frontend:**
     - `imageMetadataExtractor.js`: Đã có parser EXIF đọc nhị phân qua DataView (`parseExifFromBuffer`, `parseExifDateString`), lấy ngày giờ từ thẻ Exif IFD `0x9003` (DateTimeOriginal) / `0x9004` (DateTimeDigitized) hoặc IFD0 `0x0132` (DateTime).
     - `validateImageMetadata`: Đã có các kiểm tra định dạng MIME, kích thước file, thời gian chụp (`capturedAt`), phát hiện thời gian tương lai (> now + 5m) hoặc quá cũ (< năm 2000).
     - `determineImageMetadataStatus`: Trả về 3 trạng thái chuẩn (`VALID`, `MISSING`, `INVALID`) kèm danh sách lý do cảnh báo (`reasons`).
   - **Backend:**
     - `ExifMetadataExtractor.java`: Trích xuất nhị phân phía server trực tiếp từ bytes tải lên, đọc tag EXIF ngày giờ và tọa độ GPS, chuyển đổi sang `OffsetDateTime` theo múi giờ hệ thống (ZoneId.systemDefault()).
     - `DiaryController.java`: Tiếp nhận tham số query/form `capturedAt` (ISO-8601 string) từ client, chuyển đổi an toàn qua `parseOffsetDateTime`.
     - `DiaryService.java`: Ưu tiên `capturedAt` truyền từ client (hoặc trích xuất từ file gốc khi client gửi file), truyền qua `ImageMetadataValidator` và lưu vào `DiaryImage`.
     - `DiaryImageResponse.java`: Trả về trường `capturedAt`, `metadataStatus`, và `metadataReasons`.

3. **Giao diện hiển thị (T-70):**
   - `DiaryImageList.jsx`:
     - Hiện đang hiển thị ngày chụp dạng rút gọn: `{img.capturedAt ? \`📅 ${img.capturedAt.slice(0, 10)}\` : 'Chưa có ngày'}`.
     - Cần hoàn thiện định dạng ngày giờ chụp tiếng Việt rõ ràng, dễ đọc (ví dụ: `DD/MM/YYYY HH:mm` hoặc `HH:mm DD/MM/YYYY`), hiển thị nhãn chuẩn khi không có thời gian chụp (ví dụ: "Chưa có thời gian chụp", không bị lỗi `Invalid Date` hoặc thời gian giả).
   - `DiaryImageLightbox.jsx`:
     - Đang hiển thị: `{currentImage.capturedAt && (<Text ...>📅 {currentImage.capturedAt.slice(0, 16).replace('T', ' ')}</Text>)}`.
     - Cần bổ sung hiển thị rõ ràng nhãn "Thời điểm chụp: Chưa xác định" khi `capturedAt` null/trống, và định dạng chuẩn ngày giờ tiếng Việt thân thiện người dùng khi có giá trị.
   - `ImageUploader.jsx`:
     - Đang hiển thị ở hàng đợi: `Chụp: {item.metadata.capturedAt ? item.metadata.capturedAt.slice(0, 16).replace('T', ' ') : 'Chưa có'}`.
     - Tự động gán fallback `file.lastModified` trong `createQueueItem`.

### 1.2. Các điểm cần điều chỉnh/tinh chỉnh cho S-40
1. **Phân biệt rạch ròi thời điểm chụp (`capturedAt`) vs thời điểm tạo/upload (`createdAt`):**
   - Theo đặc tả T-69: *Ảnh không có thời gian chụp vẫn được tải lên nếu nghiệp vụ cho phép; lưu `capturedAt = null`, không tự thay bằng giờ upload.*
   - Trong `imageMetadataExtractor.js` (và `imageQueueUtils.js`): Hàm `createQueueItem` hiện đang gán `defaultCapturedAt` lấy từ `file.lastModified` hoặc `new Date()`. Nếu ảnh chụp từ camera hoặc ảnh không có EXIF, việc tự gán thời gian hiện tại (`new Date()`) vào `capturedAt` làm mất đi tính nguyên bản (không phản ánh đúng EXIF ảnh chụp và vi phạm nguyên tắc "lưu `capturedAt = null`, không tự thay bằng giờ upload").
   - Cần chuẩn hóa: Chỉ lấy `capturedAt` từ EXIF của ảnh. Nếu ảnh không có EXIF hoặc tệp không có EXIF hợp lệ, để `capturedAt = null`. Không tự tạo thời gian giả.
2. **Định dạng hiển thị thời điểm chụp (T-70):**
   - Cung cấp hàm định dạng ngày giờ chụp chuẩn hóa `formatCapturedAt(dateStr)` tái sử dụng được ở `DiaryImageList` và `DiaryImageLightbox`:
     - Nếu có giá trị hợp lệ: hiển thị dạng `HH:mm DD/MM/YYYY` (ví dụ `14:30 18/10/2026`).
     - Nếu không có giá trị: hiển thị rõ ràng `Chưa có thông tin thời điểm chụp` (hoặc `Không xác định`), phân biệt rõ với thời gian tạo bản ghi nhật ký.
     - Đảm bảo an toàn không bao giờ xuất hiện `Invalid Date` hoặc `NaN/NaN/NaN`.
3. **Môi trường build Backend Java 21 vs 25:**
   - Trong file `backend/pom.xml`, một commit trước đó (`461de38`) đã sửa `<java.version>25</java.version>` và compiler plugin `<release>25</release>`, trong khi môi trường máy tính thực tế chạy Java 21 LTS (`21.0.12.1 LTS`). Cần đảm bảo pom.xml cấu hình Java 21 tương thích với quy ước dự án trong `AGENTS.md` ("Backend: Java 21 + Spring Boot") để các lệnh test và build backend thực thi được trơn tru.

---

## 2. Acceptance Criteria

### T-68 — Chụp/tải ảnh hiện trường
- [ ] Người dùng có thể chọn hoặc kéo thả một hoặc nhiều ảnh hiện trường (JPEG, PNG, WebP) từ thiết bị vào khu vực upload nhật ký.
- [ ] Giữ nguyên cơ chế upload hai giai đoạn (giai đoạn 1 nén hiển thị < 1 MiB, giai đoạn 2 tải ảnh gốc nguyên bản <= 20 MiB).
- [ ] Chặn và thông báo lỗi rõ ràng đối với các tệp không đúng định dạng (không phải ảnh), tệp rỗng (0 byte) hoặc tệp vượt quá giới hạn 20 MiB.
- [ ] Giữ nguyên các cơ chế retry độc lập từng giai đoạn, phân quyền dự án (`ADMIN`, `PROJECT_MANAGER`, `SITE_ENGINEER`) và kiểm tra khóa sổ nhật ký theo ngày.

### T-69 — Tự động ghi nhận ngày giờ của ảnh
- [ ] Hệ thống tự động đọc và trích xuất ngày giờ chụp từ siêu dữ liệu EXIF của ảnh (các tag `DateTimeOriginal`, `DateTimeDigitized`, `DateTime`).
- [ ] Thời điểm chụp (`capturedAt`) được truyền nhất quán từ frontend lên backend và lưu vào trường `captured_at` của bảng `diary_images`.
- [ ] Nếu ảnh gốc có EXIF hợp lệ, backend đọc chính xác và ưu tiên dữ liệu ngày giờ chụp trích xuất được.
- [ ] Nếu ảnh không chứa EXIF ngày giờ chụp: hệ thống lưu `capturedAt = null`, tuyệt đối không tự ý gán thời gian hiện tại (`new Date()`) hoặc thời điểm upload vào trường `capturedAt`.
- [ ] Thời điểm chụp phải hoàn toàn độc lập và phân biệt với thời điểm upload (`createdAt`).
- [ ] Nếu thời gian chụp không hợp lệ (ở tương lai vượt quá sai lệch 5 phút hoặc trước năm 2000), hệ thống ghi nhận trạng thái metadata không hợp lệ theo đúng quy tắc kiểm tra hiện có.

### T-70 — Hiển thị thời điểm chụp trong thông tin ảnh
- [ ] Danh sách ảnh (`DiaryImageList`) và chi tiết xem ảnh (`DiaryImageLightbox`) hiển thị rõ ràng thời điểm chụp đọc từ cơ sở dữ liệu (`capturedAt`).
- [ ] Định dạng ngày giờ hiển thị thân thiện với người dùng Việt Nam (dạng `HH:mm DD/MM/YYYY`).
- [ ] Khi ảnh không có `capturedAt` (`null`), hiển thị thông báo rõ ràng "Chưa có thông tin ngày giờ chụp" hoặc "Không xác định", tuyệt đối không hiển thị `Invalid Date`, chuỗi trống khó hiểu hoặc thời gian giả.
- [ ] Giao diện phân biệt rõ ràng giữa thời điểm chụp ảnh và thời điểm đăng/tạo bản ghi nhật ký.

---

## 3. Danh sách file và thay đổi chi tiết

### Giai đoạn 1 (T-68): Upload ảnh hiện trường
*Mục tiêu:* Đảm bảo luồng chọn/chụp ảnh, xác thực file và tải lên 2 giai đoạn hoạt động ổn định, cấu hình backend sẵn sàng chạy trên Java 21 theo AGENTS.md.

1. **`backend/pom.xml`**
   - *Lý do:* Khôi phục cấu hình Java version về 21 (theo quy ước `AGENTS.md`: "Backend: Java 21 + Spring Boot") để test suite và build backend thực thi được với JDK 21 của môi trường.
   - *Thay đổi:* Đặt `<java.version>21</java.version>`, `<maven.compiler.source>21</maven.compiler.source>`, `<maven.compiler.target>21</maven.compiler.target>` và `<release>21</release>`.
2. **`frontend/src/components/diary/ImageUploader.jsx` & `frontend/src/services/diaryUploadService.js`**
   - *Lý do:* Kiểm tra tính toàn vẹn của luồng tải ảnh, xác thực dung lượng, phần mở rộng, mime type và retry.
   - *Thay đổi:* Rà soát các thông báo lỗi tiếng Việt, giữ nguyên logic upload hai giai đoạn hiện có.

### Giai đoạn 2 (T-69): Tự động ghi nhận ngày giờ của ảnh
*Mục tiêu:* Đảm bảo nguồn trích xuất `capturedAt` xuất phát trung thực từ EXIF của ảnh, không tạo ngày giờ giả mạo nếu ảnh không có metadata.

1. **`frontend/src/utils/imageQueueUtils.js`**
   - *Lý do:* Trong `createQueueItem`, `defaultCapturedAt` đang tự động fallback về `new Date().toISOString()`, khiến ảnh không có EXIF bị gắn thời gian upload/tạo queue làm `capturedAt`.
   - *Thay đổi:* Khi không có `initialMetadata?.capturedAt`, để `capturedAt: null`. Đảm bảo ảnh không có EXIF được phản ánh trung thực là `capturedAt = null`.
2. **`frontend/src/utils/imageMetadataExtractor.js`**
   - *Lý do:* Trong `extractImageMetadata`, fallback `file.lastModified` có thể làm giả mạo thời gian chụp đối với ảnh không có EXIF. Cần chỉ định rõ nguồn gốc `exif` và chỉ đặt `capturedAt` khi thực sự đọc được từ EXIF.
   - *Thay đổi:* Điều chỉnh `extractImageMetadata` để chỉ trích xuất `capturedAt` từ EXIF. Không dùng `lastModified` để giả mạo thời điểm chụp ảnh hiện trường.
3. **`backend/src/main/java/com/ntdhtcct/domain/diary/DiaryService.java` & `DiaryController.java`**
   - *Lý do:* Kiểm tra bảo đảm backend ưu tiên đọc `capturedAt` từ file EXIF nếu client không gửi hoặc client gửi null, và lưu đúng `null` vào database nếu cả client và file nhị phân đều không có EXIF date.
   - *Thay đổi:* Đảm bảo logic `getOrCreateImageRecord` không ghi đè thời gian upload vào `capturedAt`.

### Giai đoạn 3 (T-70): Hiển thị thời điểm chụp
*Mục tiêu:* Hoàn thiện giao diện hiển thị thông tin thời gian chụp cho ảnh trên cả lưới thumbnail (`DiaryImageList`) và màn hình xem chi tiết (`DiaryImageLightbox`).

1. **`frontend/src/utils/dateTimeUtils.js` (hoặc helper trực tiếp trong `imageMetadataExtractor.js`)**
   - *Lý do:* Chuẩn hóa định dạng hiển thị thời gian chụp ảnh theo phong cách ngày giờ tiếng Việt, xử lý an toàn giá trị `null`, rỗng, hoặc chuỗi ngày giờ không hợp lệ.
   - *Thay đổi:* Thêm hàm tiện ích `formatCapturedAt(dateStr)`:
     ```javascript
     export function formatCapturedAt(dateStr) {
       if (!dateStr) return 'Chưa có thông tin thời điểm chụp';
       const date = new Date(dateStr);
       if (isNaN(date.getTime())) return 'Thời điểm chụp không hợp lệ';
       const day = String(date.getDate()).padStart(2, '0');
       const month = String(date.getMonth() + 1).padStart(2, '0');
       const year = date.getFullYear();
       const hours = String(date.getHours()).padStart(2, '0');
       const minutes = String(date.getMinutes()).padStart(2, '0');
       return `${hours}:${minutes} ${day}/${month}/${year}`;
     }
     ```
2. **`frontend/src/components/diary/DiaryImageList.jsx`**
   - *Lý do:* Hiện tại đang hiển thị `📅 ${img.capturedAt.slice(0, 10)}`. Cần hiển thị rõ ràng hơn và xử lý trường hợp không có ngày chụp một cách thân thiện.
   - *Thay đổi:* Sử dụng định dạng `formatCapturedAt` ngắn gọn (hoặc hiển thị tooltip ngày giờ đầy đủ) và nhãn "Chưa có ngày chụp" khi `null`.
3. **`frontend/src/components/diary/DiaryImageLightbox.jsx`**
   - *Lý do:* Lightbox cần cung cấp thông tin chi tiết đầy đủ của ảnh, bao gồm thời điểm chụp (được phân biệt rõ với thời điểm tải lên), phân biệt rõ trường hợp có metadata và trường hợp không có metadata.
   - *Thay đổi:* Hiển thị rõ trường:
     - `Thời điểm chụp: 14:30 18/10/2026` (hoặc `Thời điểm chụp: Chưa có thông tin`).
     - `Thời điểm tải lên: ...` (lấy từ `createdAt` để người dùng phân biệt rõ 2 mốc thời gian).

---

## 4. Các file chỉ cần kiểm tra (không cần sửa nếu đã đạt yêu cầu)

- `backend/src/main/resources/db/migration/v16/V16_0__create_diary_tables.xml`: Đã có cột `captured_at TIMESTAMP WITH TIME ZONE`. Không cần sửa hay tạo thêm migration.
- `backend/src/main/java/com/ntdhtcct/domain/diary/DiaryImage.java`: Đã có trường `capturedAt`, getter/setter đầy đủ.
- `backend/src/main/java/com/ntdhtcct/domain/diary/ExifMetadataExtractor.java`: Bộ giải mã EXIF JPEG nhị phân độc lập đã hoạt động chính xác.
- `backend/src/main/java/com/ntdhtcct/domain/diary/ImageMetadataValidator.java`: Bộ kiểm tra tính bất thường (thời gian tương lai, thời gian trước năm 2000) đã hoàn thiện.
- `frontend/src/services/diaryApi.js`: Các hàm `uploadDisplayImage`, `uploadOriginalImage`, `getDiaryImages` đã truyền và nhận `capturedAt`.

---

## 5. Kế hoạch kiểm thử (Testing Strategy)

### 5.1. Kiểm thử cho T-68 (Upload ảnh)
- **Frontend test:**
  ```powershell
  node --test frontend/tests/imageUploadQueue.test.js frontend/tests/imageQueueUtils.test.js frontend/tests/imageUploaderFlow.test.js frontend/tests/diaryUpload.test.js
  ```
  - Kiểm tra: Xác thực loại tệp, từ chối tệp không phải ảnh, từ chối tệp rỗng, từ chối tệp > 20 MiB, tải lên 2 giai đoạn độc lập.
- **Backend test:**
  ```powershell
  cd backend; .\mvnw.cmd test -Dtest=ImageMetadataValidatorTest; cd ..
  ```
  - Kiểm tra: Xác thực dữ liệu file upload tại backend.

### 5.2. Kiểm thử cho T-69 (Tự động ghi nhận ngày giờ)
- **Frontend test:**
  ```powershell
  node --test frontend/tests/imageMetadataValidation.test.js
  ```
  - Kiểm tra:
    - Ảnh có EXIF hợp lệ -> trích xuất đúng ngày giờ chụp.
    - Ảnh không có EXIF -> trả về `capturedAt = null`, không tự gán ngày giờ hiện tại.
    - Phát hiện bất thường khi ngày giờ chụp ở tương lai hoặc quá cũ (< 2000).
- **Backend test:**
  ```powershell
  cd backend; .\mvnw.cmd test -Dtest=ImageMetadataValidatorTest; cd ..
  ```

### 5.3. Kiểm thử cho T-70 (Hiển thị thời điểm chụp)
- **Frontend test:**
  ```powershell
  node --test frontend/tests/diaryLightbox.test.js
  ```
  - Bổ sung test kiểm tra:
    - Hàm format ngày giờ hiển thị đúng định dạng `HH:mm DD/MM/YYYY`.
    - Xử lý giá trị `null` hiển thị nhãn "Chưa có thông tin thời điểm chụp", không xuất hiện `Invalid Date`.
    - Phân biệt giữa `capturedAt` và `createdAt`.

### 5.4. Kiểm thử tích hợp toàn diện S-40
- Chạy toàn bộ các test liên quan đến ảnh nhật ký:
  ```powershell
  node --test frontend/tests/imageMetadataValidation.test.js frontend/tests/diaryLightbox.test.js frontend/tests/diaryUpload.test.js frontend/tests/imageQueueUtils.test.js
  cd backend; .\mvnw.cmd test -Dtest=ImageMetadataValidatorTest; cd ..
  ```

---

## 6. Ranh giới 3 Commit (Commit Boundaries)

Việc triển khai sẽ được thực hiện tuần tự qua 3 commit rõ ràng:

### Commit 1 — T-68: Upload ảnh hiện trường
- **Phạm vi:**
  - Chuẩn hóa cấu hình `backend/pom.xml` về Java 21 để đảm bảo môi trường build và chạy kiểm thử theo quy định của dự án.
  - Đảm bảo luồng chọn ảnh, xác thực file (loại ảnh, kích thước <= 20 MiB), nén hiển thị và tải lên 2 giai đoạn ổn định.
- **Tiêu chí hoàn thành:**
  - Các test `imageUploadQueue.test.js`, `imageQueueUtils.test.js`, `diaryUpload.test.js` chạy thành công 100%.
  - `pom.xml` tương thích JDK 21.

### Commit 2 — T-69: Ghi nhận thời điểm chụp
- **Phạm vi:**
  - Điều chỉnh `createQueueItem` và `extractImageMetadata` để `capturedAt` chỉ lấy từ EXIF. Khi không có EXIF, giữ `capturedAt = null` thay vì tự tạo giờ hiện tại.
  - Đảm bảo luồng gửi dữ liệu từ frontend và backend lưu đúng `captured_at` (hoặc `null`) vào cơ sở dữ liệu.
- **Tiêu chí hoàn thành:**
  - Test suite `imageMetadataValidation.test.js` và `ImageMetadataValidatorTest` chạy thành công.
  - Ảnh không có EXIF được ghi nhận `capturedAt = null` mà không gây lỗi upload.

### Commit 3 — T-70: Hiển thị thời điểm chụp
- **Phạm vi:**
  - Bổ sung định dạng thời gian chuẩn tiếng Việt `HH:mm DD/MM/YYYY` cho thời điểm chụp.
  - Cập nhật hiển thị thời điểm chụp trong `DiaryImageList` và `DiaryImageLightbox`.
  - Hiển thị rõ ràng trạng thái khi thiếu ngày chụp, hiển thị tách biệt thời điểm chụp và thời điểm tải lên.
- **Tiêu chí hoàn thành:**
  - Test suite `diaryLightbox.test.js` và kiểm tra hiển thị thời gian chạy thành công.
  - Toàn bộ flow upload -> trích xuất -> hiển thị hoạt động nhất quán, không có lỗi `Invalid Date`.

---

## 7. Các giả định và câu hỏi mở

- **Giả định về múi giờ:** EXIF của hầu hết máy ảnh/điện thoại ghi ngày giờ địa phương (local time) không kèm offset. Code hiện tại đang ánh xạ thời gian này sang `ZoneId.systemDefault()` (Việt Nam GMT+7), hoàn toàn phù hợp với ngữ cảnh dự án thi công xây dựng tại Việt Nam.
- **Giả định về GPS:** Backlog S-40 có đề cập đến tọa độ, nhưng 3 subtask T-68, T-69, T-70 tập trung vào upload và thời điểm chụp. Mã nguồn đã có sẵn hỗ trợ GPS từ T-74/T-75/T-76 và giữ nguyên trạng, không mở rộng bản đồ hay yêu cầu bắt buộc GPS mới.
- **Không có câu hỏi chặn:** Tất cả kiến trúc, database, backend và frontend đã được định vị chính xác và sẵn sàng triển khai.
