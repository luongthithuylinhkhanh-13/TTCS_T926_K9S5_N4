# Nền tảng điều hành thi công công trình

Nền tảng hỗ trợ quản lý và điều hành quá trình thi công công trình.

## Chức năng chính

- Đăng nhập và phân quyền
- Quản lý người dùng
- Quản lý dự án
- Quản lý công việc
- Quản lý tiến độ
- Quản lý giao việc
- Nhật ký công trường
- Quản lý nghiệm thu
- Quản lý thanh toán
- Quản lý dự toán và thực chi
- Quản lý vật tư
- Báo cáo
- Thông báo

## Cấu trúc dự án

```
nen-tang-dieu-hanh-thi-cong/
├── backend/          # Spring Boot Backend API
├── frontend/         # Giao diện người dùng (Phase 2+)
├── database/         # Database scripts & seeds
├── tests/            # Kiểm thử tích hợp & E2E
├── docs/             # Tài liệu dự án
├── infra/            # Hạ tầng (Docker, monitoring, ...)
└── scripts/          # Script hỗ trợ triển khai
```

## Công nghệ

| Layer    | Công nghệ                              |
| -------- | -------------------------------------- |
| Backend  | Java 25 · Spring Boot · Maven          |
| Database | PostgreSQL 14+                         | 
| Migration| Liquibase                              |
| Container| Docker / Docker Compose                |
| CI/CD    | GitHub Actions                         |
| Frontend | TBD                                    |

---

## Phase 1: Backend + Database Setup

### 1. Công cụ cần cài

**Bắt buộc:**

| Công cụ        | Phiên bản  | Link                                          |
| -------------- | ---------- | --------------------------------------------- |
| Java JDK       | 25 (LTS)   | https://www.oracle.com/java/technologies/downloads/ hoặc https://adoptium.net/ |
| PostgreSQL     | 14+        | https://www.postgresql.org/download/          |
| Git            | Bất kỳ     | https://git-scm.com/                          |

**Không bắt buộc (Phase 1):**
- Maven: không cần cài global vì dự án dùng Maven Wrapper (`./mvnw`).
- Docker: chưa sử dụng ở Phase 1.

**Kiểm tra cài đặt:**

```bash
java -version     # Phải là 25.x.x
psql --version    # Phải là 14+
git --version
```

---

### 2. Cài Java 25

Tải JDK 25 từ Oracle hoặc Adoptium (khuyến nghị). Sau khi cài, xác nhận:

```bash
java -version
# java version "25.x.x" ...
```

Đảm bảo biến môi trường `JAVA_HOME` trỏ đến JDK 25.

---

### 3. PostgreSQL setup

#### 3.1 Cài PostgreSQL

Tải và cài PostgreSQL 14+ từ https://www.postgresql.org/download/

#### 3.2 Khởi động PostgreSQL service

```bash
# Linux / macOS
sudo systemctl start postgresql   # hoặc: brew services start postgresql

# Windows
# Mở Services → tìm "postgresql" → Start
```

#### 3.3 Kết nối PostgreSQL

```bash
psql -U postgres
```

#### 3.4 Tạo database

```sql
CREATE DATABASE construction_management;
-- Xác nhận:
\l
-- Thoát:
\q
```

---

### 4. Environment configuration

#### 4.1 Tạo file `.env`

```bash
cp .env.example .env
```

> **CẢNH BÁO:** `.env` không được commit vào Git. File này đã được thêm vào `.gitignore`.

#### 4.2 Chỉnh sửa `.env`

Mở `.env` và điền giá trị thực tế:

```env
APP_ENV=development
APP_PORT=8080

DB_HOST=localhost
DB_PORT=5432
DB_NAME=construction_management
DB_USERNAME=postgres
DB_PASSWORD=your_actual_password_here
```

> Không để `DB_PASSWORD` trống nếu PostgreSQL yêu cầu password.

#### 4.3 Load environment variables

Spring Boot không tự đọc `.env`. Cần export trước khi chạy:

**Linux / macOS:**

```bash
export $(grep -v '^#' .env | xargs)
```

**Windows (PowerShell):**

```powershell
Get-Content .env | Where-Object { $_ -notmatch '^#' -and $_ -ne '' } |
  ForEach-Object { $k,$v = $_ -split '=', 2; [System.Environment]::SetEnvironmentVariable($k, $v, 'Process') }
```

**Hoặc cấu hình trực tiếp trong IDE:**
- IntelliJ IDEA: `Run → Edit Configurations → Environment variables`
- VS Code: `.vscode/launch.json` → `"env"` section

---

### 5. Build project

```bash
cd backend

# Linux / macOS
./mvnw clean compile

# Windows
mvnw.cmd clean compile
```

Build thành công khi thấy: `BUILD SUCCESS`

---

### 6. Chạy Backend

Đảm bảo PostgreSQL đang chạy và `.env` đã được load, sau đó:

```bash
cd backend

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Backend khởi động thành công khi log hiển thị:

```
Started Application in X.XXX seconds (process running for X.XXX)
```

Backend chạy tại: `http://localhost:8080`

---

### 7. Liquibase Migration

#### 7.1 Chạy Migration UP (tự động khi start)

Khi backend khởi động, Liquibase tự động chạy các migration chưa được áp dụng.
Xác nhận trong log:

```
liquibase : Successfully acquired change log lock
liquibase : Running Changeset: ...V1_0__init_infrastructure_check.xml::V1.0-infrastructure-check::...
liquibase : ChangeSet ... ran successfully
liquibase : Successfully released change log lock
```

#### 7.2 Chạy Migration UP thủ công

```bash
cd backend

# Linux / macOS
./mvnw liquibase:update \
  -DDB_URL="jdbc:postgresql://localhost:5432/construction_management" \
  -DDB_USERNAME=postgres \
  -DDB_PASSWORD="$DB_PASSWORD"

# Windows (PowerShell)
mvnw.cmd liquibase:update -DDB_URL="jdbc:postgresql://localhost:5432/construction_management" -DDB_USERNAME=postgres "-DDB_PASSWORD=$env:DB_PASSWORD"
```

#### 7.3 Kiểm tra Migration đã áp dụng

```bash
psql -U postgres -d construction_management -c "\dt"
# Phải thấy: schema_version, databasechangelog, databasechangeloglock
```

```bash
psql -U postgres -d construction_management -c "SELECT * FROM schema_version;"
# Phải thấy: 1 row với version_tag = 'phase1-infrastructure'
```

---

### 8. Liquibase Rollback

#### 8.1 Rollback 1 changeset gần nhất

```bash
cd backend

# Linux / macOS
./mvnw liquibase:rollback \
  -Dliquibase.rollbackCount=1 \
  -DDB_URL="jdbc:postgresql://localhost:5432/construction_management" \
  -DDB_USERNAME=postgres \
  -DDB_PASSWORD="$DB_PASSWORD"

# Windows (PowerShell)
mvnw.cmd liquibase:rollback -Dliquibase.rollbackCount=1 -DDB_URL="jdbc:postgresql://localhost:5432/construction_management" -DDB_USERNAME=postgres "-DDB_PASSWORD=$env:DB_PASSWORD"
```

#### 8.2 Xác nhận Rollback

```bash
psql -U postgres -d construction_management -c "\dt"
# Bảng schema_version không còn tồn tại
```

---

### 9. Kiểm tra kết nối Database

Trong khi backend đang chạy, kiểm tra log HikariCP:

```
HikariPool-ConstructionPlatform - Start completed.
```

Hoặc kiểm tra qua psql:

```bash
psql -U postgres -d construction_management -c "SELECT NOW();"
```

---

### 10. Kiểm tra Security (không commit secret)

```bash
git status
# .env không được xuất hiện trong danh sách

git diff HEAD -- .env.example
# Chỉ thấy placeholder, không có password thực

grep -r "DB_PASSWORD" backend/src/main/resources/
# Phải thấy ${DB_PASSWORD:} — không phải giá trị thực
```

---

## Git workflow

```
main
  |
develop
  |
feature/*
  |
Pull Request
  |
Code Review
  |
develop
  |
main
```

## Bảo mật

- **Không bao giờ** commit `.env`, password, API key, hoặc token vào Git.
- `.env.example` chỉ chứa placeholder (giá trị rỗng hoặc mô tả).
- Sử dụng environment variables cho tất cả credentials.
- Kiểm tra `git status` trước mỗi commit.


##  Docker Setup

### 1. Công cụ cần cài

* Docker Desktop
* Docker Compose

Kiểm tra:

```bash
docker --version
docker compose version
```

### 2. Khởi động hệ thống

Từ thư mục root của project:

```bash
docker compose build
docker compose up -d
```

Kiểm tra trạng thái các service:

```bash
docker compose ps
```

PostgreSQL phải ở trạng thái `healthy` và Backend phải ở trạng thái `running`.

### 3. Kiểm tra log

Kiểm tra Backend:

```bash
docker compose logs backend
```

Kiểm tra PostgreSQL:

```bash
docker compose logs postgres
```

Backend khởi động thành công khi log hiển thị:

```text
Started Application
```

### 4. Kiểm tra Database và Migration

Kết nối vào PostgreSQL container:

```bash
docker exec -it construction-postgres psql -U postgres -d construction_management
```

Kiểm tra các bảng:

```sql
\dt
```

Kiểm tra migration:

```sql
SELECT * FROM schema_version;
```

Phải thấy record với:

```text
version_tag = phase1-infrastructure
```

### 5. Dừng hệ thống

```bash
docker compose down
```

> Không sử dụng `docker compose down -v` nếu muốn giữ dữ liệu PostgreSQL.

### 6. Chạy lại hệ thống

```bash
docker compose up -d
```

Docker Compose sẽ khởi động PostgreSQL trước. Backend chỉ khởi động sau khi PostgreSQL đạt trạng thái `healthy`.
