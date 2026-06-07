# 🌴 AI-Powered Quy Nhon Tourism Management System

Một hệ thống quản lý du lịch Quy Nhơn hiện đại, tích hợp Trí tuệ nhân tạo (AI) giúp tối ưu hóa lịch trình, phân tích cảm xúc đánh giá, tự động định giá động và cung cấp trợ lý ảo trả lời ngữ nghĩa RAG thông minh.

---

## 🚀 Tính năng nổi bật

### 1. Phân hệ Quản lý & Nghiệp vụ Du lịch
* **Xác thực & Người dùng (JWT/OTP)**: Đăng nhập không mật khẩu qua mã OTP gửi qua Email. Quản lý phiên làm việc bằng HttpOnly Cookie chứa JWT Token, cơ chế xoay vòng Refresh Token bảo mật.
* **Tour & Lịch trình (Schedules)**: Quản lý danh mục tour, lịch trình xuất phát chi tiết, số lượng chỗ trống (`available_slots`), điều phối hướng dẫn viên du lịch.
* **Đặt tour & Thanh toán VNPay**: Đặt chỗ thông minh (phân loại người lớn/trẻ em), tích hợp cổng thanh toán **VNPay Sandbox** bảo mật cao với cơ chế kiểm tra đối soát chữ ký số (IPN).
* **Vé QR & Email tự động**: Sau khi thanh toán thành công, hệ thống gửi email xác nhận dạng HTML cao cấp kèm mã **QR Code** để check-in tự động khi lên tàu/cano.
* **Chương trình Khách hàng thân thiết (Loyalty)**: Tích lũy điểm khi đi tour (`1 point = 1,000 VND`), quy đổi điểm thưởng trực tiếp để giảm trừ chi phí cho các lần đặt tour tiếp theo.
* **Wishlist**: Khách hàng lưu trữ các tour yêu thích để theo dõi.

### 2. Dịch vụ AI thông minh (Tích hợp Google Gemini API)
* **Lập lịch trình thông minh (AI Travel Planner)**: Lập lịch trình 3 ngày 2 đêm chi tiết theo ngân sách và sở thích của khách hàng, trả về chuỗi JSON nghiêm ngặt và lưu cache trên Redis.
* **Cơ chế Dự phòng Cascading Fallback**: Hệ thống tự động chuyển tiếp cuộc gọi AI theo chuỗi ưu tiên khi gặp giới hạn tần suất (Rate Limit):
  $$\text{gemini-3.1-flash-lite} \rightarrow \text{gemini-2.5-flash-lite} \rightarrow \text{gemma-4-26b-a4b-it} \rightarrow \text{gemini-2.5-flash}$$
* **Hệ thống xử lý JSON thông minh (`extractJson`)**: Tự động bóc tách và làm sạch mã JSON thô từ phản hồi văn bản của các mô hình Gemma, tránh lỗi phân tích cấu trúc dữ liệu.
* **Trợ lý ảo Hỏi đáp (RAG Chatbot)**: 
  * Sử dụng model **Gemini Embedding 1** (`gemini-embedding-001`) để chuyển đổi câu hỏi của khách hàng thành vector 3072 chiều.
  * Tìm kiếm ngữ nghĩa bằng **Độ tương đồng Cosine (Cosine Similarity)** với kho dữ liệu tour nội bộ, trích xuất tour khớp nhất nhúng vào ngữ cảnh hệ thống (System Prompt) gửi cho LLM.
* **Phân tích Cảm xúc Đánh giá (Sentiment Analysis)**: Tự động chạy ngầm bất đồng bộ (`@Async`) quét và dán nhãn bình luận (`POSITIVE`, `NEUTRAL`, `NEGATIVE`).
* **Tính giá động bằng AI (Dynamic Pricing)**: Bộ lập lịch định kỳ tự động phân tích thời gian cận ngày đi, số chỗ trống còn lại và giá gốc để đề xuất giá bán tối ưu nhất.

---

## 🛠️ Công nghệ sử dụng

* **Backend**: Spring Boot 4.0.6, Java 24, Spring Data JPA, Spring Security OAuth2 (JWT), Spring Cache (Redis).
* **Cơ sở dữ liệu**: PostgreSQL, Redis (Lưu trữ session, OTP session và cache dữ liệu AI).
* **Tích hợp bên thứ ba**: Google Generative Language API (Gemini), VNPay Payment Gateway, Google SMTP Server.
* **Build tool**: Maven.
* **Container**: Docker & Docker Compose.

---

## ⚙️ Hướng dẫn cài đặt & Cấu hình

### 1. Chuẩn bị môi trường
* **JDK 24** cài đặt trên hệ thống.
* **Docker & Docker Compose** (để chạy nhanh PostgreSQL và Redis).

### 2. Cấu hình Tham số Hệ thống
Tạo hoặc chỉnh sửa cấu hình kết nối tại file [application.yaml](file:///l:/tourism/src/main/resources/application.yaml):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tourism
    username: your_postgres_user
    password: your_postgres_password
  data:
    redis:
      host: localhost
      port: 6379
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password

app:
  jwt:
    secret: your_jwt_sha256_secret_key_minimum_32_bytes
  gemini:
    api-key: your_gemini_api_studio_key
  vnpay:
    tmn-code: your_vnpay_terminal_code
    hash-secret: your_vnpay_hash_secret
```

### 3. Khởi tạo Cơ sở Dữ liệu
Sử dụng script [db_schema.sql](file:///l:/tourism/db_schema.sql) để tạo các bảng, khóa ngoại, chỉ mục hiệu năng cao (Performance Index) và Trigger kiểm toán tự động trên PostgreSQL.

---

## 🏃 Vận hành ứng dụng

### Chạy các container bổ trợ (PostgreSQL & Redis)
```bash
docker-compose up -d
```

### Build & Chạy ứng dụng Spring Boot
Sử dụng Maven Wrapper kèm sẵn trong dự án:

**Trên Windows:**
```cmd
mvnw.cmd spring-boot:run
```

**Trên Linux/MacOS:**
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### Chạy các ca kiểm thử (Unit / Integration Tests)
```bash
mvnw.cmd test
```

---

## 📂 Cấu trúc dự án chính
* `src/main/java/com/quynhontravel/tourism/common`: Chứa cấu hình bảo mật, xử lý ngoại lệ toàn cục và cấu hình Jackson.
* `src/main/java/com/quynhontravel/tourism/integration/gemini`: Bộ kết nối API Gemini và xử lý cơ chế dự phòng failover.
* `src/main/java/com/quynhontravel/tourism/modules`: Các module nghiệp vụ lõi (User, Tour, Booking, Payment, Loyalty, Wishlist, Review, AI Services).
