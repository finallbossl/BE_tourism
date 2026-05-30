-- ============================================================================
-- AI-POWERED QUY NHON TOURISM MANAGEMENT SYSTEM
-- DATABASE INITIALIZATION SCRIPT (POSTGRESQL)
-- VERSION: 1.2 (Added Wishlists & Reported Reviews)
-- ============================================================================

-- Kích hoạt extension sinh mã UUID ngẫu nhiên
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Hủy các bảng cũ nếu đã tồn tại để tránh xung đột
DROP TABLE IF EXISTS ai_dynamic_pricing_logs CASCADE;
DROP TABLE IF EXISTS ai_travel_plans CASCADE;
DROP TABLE IF EXISTS wishlists CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS tour_schedules CASCADE;
DROP TABLE IF EXISTS tours CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS otp_sessions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- ============================================================================
-- 1. MODULE: USER & AUTHENTICATION
-- ============================================================================

-- Bảng: users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(15),
    role VARCHAR(20) NOT NULL,
    loyalty_points INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loyalty_points CHECK (loyalty_points >= 0),
    CONSTRAINT chk_user_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_STAFF', 'ROLE_GUIDE', 'ROLE_CUSTOMER'))
);

-- Bảng: otp_sessions
CREATE TABLE otp_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    purpose VARCHAR(20) NOT NULL,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('REGISTER', 'LOGIN', 'BOOKING_VERIFY'))
);

-- ============================================================================
-- 2. MODULE: TOUR & SCHEDULE MANAGEMENT
-- ============================================================================

-- Bảng: categories
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    slug VARCHAR(120) NOT NULL UNIQUE,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Bảng: tours
CREATE TABLE tours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    base_price NUMERIC(12,2) NOT NULL,
    duration_days INT NOT NULL,
    duration_nights INT NOT NULL,
    cover_image VARCHAR(255),
    images_gallery TEXT[],
    created_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tours_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tours_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_base_price CHECK (base_price > 0),
    CONSTRAINT chk_duration_days CHECK (duration_days > 0),
    CONSTRAINT chk_duration_nights CHECK (duration_nights >= 0)
);

-- Bảng: tour_schedules
CREATE TABLE tour_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id UUID NOT NULL,
    guide_id UUID,
    start_date TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date TIMESTAMP WITH TIME ZONE NOT NULL,
    max_slots INT NOT NULL,
    available_slots INT NOT NULL,
    current_price NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_tour FOREIGN KEY (tour_id) REFERENCES tours(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_guide FOREIGN KEY (guide_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_date_range CHECK (end_date > start_date),
    CONSTRAINT chk_max_slots CHECK (max_slots > 0),
    CONSTRAINT chk_available_slots CHECK (available_slots >= 0 AND available_slots <= max_slots),
    CONSTRAINT chk_schedule_status CHECK (status IN ('AVAILABLE', 'FULL', 'DEPARTED', 'CANCELLED'))
);

-- ============================================================================
-- 3. MODULE: BOOKING & TRANSACTION PAYMENT
-- ============================================================================

-- Bảng: bookings
CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    schedule_id UUID NOT NULL,
    quantity_adults INT NOT NULL DEFAULT 1,
    quantity_children INT NOT NULL DEFAULT 0,
    total_price NUMERIC(12,2) NOT NULL,
    points_used INT DEFAULT 0,
    status VARCHAR(30) DEFAULT 'PENDING_PAYMENT',
    check_in_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_schedule FOREIGN KEY (schedule_id) REFERENCES tour_schedules(id) ON DELETE RESTRICT,
    CONSTRAINT chk_qty_adults CHECK (quantity_adults >= 1),
    CONSTRAINT chk_qty_children CHECK (quantity_children >= 0),
    CONSTRAINT chk_points_used CHECK (points_used >= 0),
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'COMPLETED'))
);

-- Bảng: payments
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE,
    vnp_txn_ref VARCHAR(100) NOT NULL UNIQUE,
    vnp_transaction_no VARCHAR(100),
    payment_gateway VARCHAR(20) DEFAULT 'VNPAY',
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

-- ============================================================================
-- 4. MODULE: REVIEWS & WISHLISTS
-- ============================================================================

-- Bảng: reviews
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    tour_id UUID NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    ai_sentiment VARCHAR(20),
    is_reported BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_tour FOREIGN KEY (tour_id) REFERENCES tours(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating_range CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_ai_sentiment CHECK (ai_sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'))
);

-- Bảng: wishlists (Lưu tour yêu thích)
CREATE TABLE wishlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    tour_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlists_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlists_tour FOREIGN KEY (tour_id) REFERENCES tours(id) ON DELETE CASCADE,
    CONSTRAINT uq_customer_tour UNIQUE (customer_id, tour_id)
);

-- ============================================================================
-- 5. MODULE: AI INTERACTIONS & ANALYTICS
-- ============================================================================

-- Bảng: ai_travel_plans
CREATE TABLE ai_travel_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID,
    input_budget NUMERIC(12,2) NOT NULL,
    input_days INT NOT NULL,
    input_guests INT NOT NULL,
    input_preferences TEXT,
    ai_response_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_plans_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_input_budget CHECK (input_budget > 0),
    CONSTRAINT chk_input_days CHECK (input_days > 0),
    CONSTRAINT chk_input_guests CHECK (input_guests > 0)
);

-- Bảng: ai_dynamic_pricing_logs
CREATE TABLE ai_dynamic_pricing_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL,
    old_price NUMERIC(12,2) NOT NULL,
    new_price NUMERIC(12,2) NOT NULL,
    trigger_reason TEXT NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pricing_logs_schedule FOREIGN KEY (schedule_id) REFERENCES tour_schedules(id) ON DELETE CASCADE
);

-- ============================================================================
-- 6. PERFORMANCE INDEXES (TỐI ƯU HÓA TRUY VẤN TỐC ĐỘ CAO)
-- ============================================================================

-- Tối ưu cho trang chủ và các trang tìm kiếm, tối ưu cấu trúc SEO URL (Slug)
CREATE INDEX idx_tours_category_slug ON tours(category_id, slug);

-- Tối ưu hóa truy vấn lịch khởi hành theo thời gian chạy (Lọc tour sắp khởi hành)
CREATE INDEX idx_schedules_date_status ON tour_schedules(start_date, status);

-- Tối ưu cho màn hình tra cứu lịch sử đặt chỗ của khách hàng (Màn hình Dashboard)
CREATE INDEX idx_bookings_customer ON bookings(customer_id);

-- Tối ưu hóa cổng thanh toán khi cổng VNPAY gọi webhook đối soát IPN đồng thời
CREATE INDEX idx_payments_vnp_ref ON payments(vnp_txn_ref);

-- Tối ưu cho việc tải danh sách tour yêu thích (Wishlist) của khách hàng
CREATE INDEX idx_wishlists_customer ON wishlists(customer_id);

-- Chỉ mục nâng cao GIN phục vụ việc phân tích và tìm kiếm từ khóa bên trong cấu trúc JSONB của AI Planner
CREATE INDEX idx_ai_plans_jsonb ON ai_travel_plans USING GIN (ai_response_json);

-- ============================================================================
-- 7. AUTOMATED AUDIT TRIGGERS (TỰ ĐỘNG CẬP NHẬT TRƯỜNG UPDATED_AT)
-- ============================================================================

-- Tạo hàm xử lý tự động cập nhật trường `updated_at`
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Gắn trigger vào bảng users
CREATE TRIGGER update_user_modtime
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- Gắn trigger vào bảng tours
CREATE TRIGGER update_tour_modtime
    BEFORE UPDATE ON tours
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- Gắn trigger vào bảng tour_schedules
CREATE TRIGGER update_schedule_modtime
    BEFORE UPDATE ON tour_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- Gắn trigger vào bảng bookings
CREATE TRIGGER update_booking_modtime
    BEFORE UPDATE ON bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- Gắn trigger vào bảng payments
CREATE TRIGGER update_payment_modtime
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();

-- Gắn trigger vào bảng reviews
CREATE TRIGGER update_review_modtime
    BEFORE UPDATE ON reviews
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();
