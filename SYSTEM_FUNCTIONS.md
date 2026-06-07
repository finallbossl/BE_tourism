# 📊 BẢNG THỐNG KÊ CHI TIẾT CÁC CHỨC NĂNG HỆ THỐNG
## AI-Powered Quy Nhon Tourism Management System

Tài liệu này tổng hợp toàn bộ các chức năng đã được hiện thực hóa trong mã nguồn của hệ thống quản lý du lịch Quy Nhơn Travel.

---

## 📌 Phân nhóm Chức năng Hệ thống

### 1. Phân hệ Xác thực & Quản lý Người dùng (User & Authentication)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Đăng ký / Đăng nhập OTP** | Khách hàng nhập email, hệ thống gửi mã OTP 6 chữ số qua Email. Xác thực OTP thành công để đăng nhập mà không cần mật khẩu. | Public / Customer | `users`, `otp_sessions` | Spring Mail, Redis (lưu OTP) |
| **Xác thực JWT Token** | Sinh mã Access Token ngắn hạn và Refresh Token dài hạn. Refresh Token được lưu trữ trong HttpOnly Cookie để chống tấn công XSS. | Hệ thống tự động | `users` | Spring Security, OAuth2, JWT |
| **Xoay vòng Refresh Token** | API tự động đọc HttpOnly Cookie để cấp mới Access Token mà không bắt người dùng đăng nhập lại. | Public | `users` | JWT Decoder / Encoder |
| **Đăng xuất (Logout)** | Xóa bỏ Refresh Token trên trình duyệt bằng cách ghi đè Cookie với `Max-Age = 0` và thu hồi phiên. | Customer / Admin / Staff | `users` | Spring Security Logout Handler |
| **Quản lý Hồ sơ cá nhân** | Tra cứu thông tin cá nhân, cập nhật số điện thoại, họ tên và quản lý vai trò người dùng. | Theo phiên đăng nhập | `users` | Spring Data JPA |

### 2. Phân hệ Quản lý Tour & Danh mục (Tours & Categories)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Quản lý Danh mục Tour** | CRUD danh mục tour (Tên, mô tả, slug). Hỗ trợ xóa mềm (`is_deleted`). | Admin / Manager | `categories` | JPA Repository |
| **Quản lý Tour chi tiết** | CRUD tour du lịch: Tên, mô tả chi tiết, giá gốc (`base_price`), số ngày đêm, ảnh đại diện, bộ sưu tập ảnh. | Admin / Manager | `tours` | JPA Specification Executor |
| **Tối ưu hóa SEO URL** | Tự động tạo chuỗi URL thân thiện (slug) từ tiêu đề tour để phục vụ SEO. | Hệ thống tự động | `tours` | Slugify logic, Index trên DB |

### 3. Phân hệ Quản lý Lịch trình & Giá bán (Tour Schedules)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Thiết lập Lịch trình đi** | Khởi tạo lịch khởi hành của từng Tour: Hướng dẫn viên phụ trách, ngày đi/ngày về, số lượng chỗ tối đa (`max_slots`). | Admin / Manager | `tour_schedules`, `users` | JPA Query |
| **Trạng thái chỗ ngồi** | Tự động tính toán số chỗ còn trống (`available_slots`) và cập nhật trạng thái (AVAILABLE, FULL, DEPARTED, CANCELLED). | Khách hàng / Staff | `tour_schedules` | Transactional Lock |

### 4. Phân hệ Đặt Tour & Thanh toán trực tuyến (Bookings & Payments)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Đặt tour du lịch** | Chọn lịch khởi hành, số lượng vé người lớn/trẻ em, tính toán giá tiền tự động. | Customer | `bookings`, `tour_schedules` | Transactional, JPA |
| **Quy đổi điểm thưởng** | Kiểm tra điểm thưởng tích lũy, tự động tính giá tiền được chiết khấu theo tỷ lệ `1 điểm = 1,000 VND`. | Customer | `bookings`, `users` | Business logic validation |
| **Cổng thanh toán VNPay** | Tạo liên kết thanh toán VNPay Sandbox với mã bảo mật checksum (SHA512) để khách hàng quét mã. | Customer | `payments`, `bookings` | VNPay Gateway Integration |
| **Xử lý Webhook (IPN)** | Lắng nghe phản hồi từ VNPay để đối soát chữ ký số, cập nhật trạng thái đặt chỗ sang `PAID`, thanh toán sang `SUCCESS`. | Public (VNPay IPN) | `bookings`, `payments`, `users` | SHA512 Verification, Transactional |

### 5. Phân hệ Thông báo & Vé điện tử (Notifications & Email)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Gửi Vé xác nhận Asynchronous**| Xử lý bất đồng bộ gửi Email hóa đơn HTML sau khi thanh toán thành công, giúp tối ưu hóa thời gian phản hồi IPN của VNPay. | Hệ thống tự động | `bookings`, `users` | Spring `@Async`, JavaMailSender |
| **Tích hợp Vé QR Code** | Tự động tạo link mã QR chứa ID đặt vé nhúng vào email để hướng dẫn viên quét check-in tại bến tàu. | Customer | `bookings` | Dynamic QR Code Generator API |

### 6. Phân hệ Đánh giá & Phân tích cảm xúc (Reviews & Sentiments)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Đăng đánh giá dịch vụ** | Khách hàng đánh giá rating (1-5 sao) và bình luận. Chỉ cho phép đánh giá khi đã đặt tour thành công và thanh toán `PAID`. | Customer | `reviews`, `bookings` | JPA validation |
| **Phân tích cảm xúc AI** | Chạy ngầm phân tích văn bản đánh giá thông qua Gemini để dán nhãn cảm xúc (`POSITIVE`, `NEUTRAL`, `NEGATIVE`). | Hệ thống tự động | `reviews` | Gemini API, Spring `@Async` |
| **Báo cáo & Kiểm duyệt** | Người dùng báo cáo đánh giá tiêu cực hoặc vi phạm (`is_reported`). Admin duyệt ẩn/hiển thị. | Admin / Customer | `reviews` | Role-based Security |

### 7. Phân hệ Khách hàng thân thiết & Wishlist (Loyalty & Wishlists)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Tra cứu Điểm tích lũy** | Hiển thị số điểm thưởng hiện có của khách hàng. | Customer | `users` | JPA Mapping |
| **Lịch sử biến động điểm** | Trích xuất lịch sử cộng điểm (từ thanh toán thành công) và trừ điểm (từ giảm giá khi đặt vé) của khách hàng. | Customer | `bookings`, `payments` | Dynamic History Builder |
| **Yêu thích Tour (Wishlist)** | Toggles lưu hoặc bỏ lưu tour vào danh sách yêu thích cá nhân. | Customer | `wishlists`, `tours` | Unique Constraint handling |

### 8. Phân hệ Trí tuệ nhân tạo nâng cao (AI Services)
| Tên chức năng | Mô tả chi tiết | Phân quyền truy cập | Bảng CSDL liên quan | Công nghệ backend áp dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Lập lịch trình du lịch AI** | Tự động xây dựng lịch trình chi tiết (Ăn sáng, tham quan Kỳ Co, Eo Gió, v.v.) dựa trên ngân sách, số người, số ngày và sở thích cá nhân. | Customer | `ai_travel_plans` | Gemini API, Redis Caching |
| **Dự phòng Cascading Fallback**| Tự động tuần tự chuyển tiếp sang các dòng model dự phòng (`gemini-3.1-flash-lite` -> `gemini-2.5-flash-lite` -> `gemma-4-26b-a4b-it` -> `gemini-2.5-flash`) nếu bị quá tải hoặc lỗi mạng. | Hệ thống tự động | Cấu hình YAML | RestTemplate, Fallback list |
| **Bộ trích xuất JSON thô** | Tự động bóc tách và định dạng lại chuỗi JSON thô từ phản hồi văn bản của các model dòng Gemma. | Hệ thống tự động | Cấu hình dịch vụ | Regex / Substring Parser |
| **Trợ lý ảo RAG Chatbot** | Nhận diện câu hỏi của người dùng, phân tích vector (embedding) để tìm kiếm các tour du lịch liên quan trong database, nạp ngữ cảnh giúp AI trả lời cực kỳ chính xác. | Public | `tours` | **Gemini Embedding 1**, Cosine Similarity |
| **AI Định giá động** | Bộ lập lịch tự động quét các tour chưa khởi hành, phân tích tỉ lệ lấp đầy chỗ trống (`occupancy`) và số ngày cận kề để tự động đề xuất giá bán mới. | Hệ thống (Scheduler) | `ai_dynamic_pricing_logs`, `tour_schedules` | Gemini API, Spring Cron Scheduler |
