-- User表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    status TINYINT DEFAULT 1 COMMENT '1-正常,0-禁用'
);

-- 分类表
CREATE TABLE IF NOT EXISTS t_category (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    icon VARCHAR(255),
    color VARCHAR(20),
    type TINYINT NOT NULL COMMENT '1-支出,2-收入',
    parent_id INT,
    is_active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 账单表
CREATE TABLE IF NOT EXISTS t_bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    category_id INT NOT NULL,
    remark VARCHAR(255),
    bill_time DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status TINYINT DEFAULT 1 COMMENT '1-正常,0-已删除',
    type TINYINT DEFAULT 1 COMMENT '1-支出,2-收入'
);

-- 分类规则表
CREATE TABLE IF NOT EXISTS t_category_rule (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    time_start TIME,
    time_end TIME,
    priority TINYINT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 预算表
CREATE TABLE IF NOT EXISTS t_budget (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    category_id INT,
    period_type VARCHAR(20) NOT NULL DEFAULT 'monthly' COMMENT 'monthly-月度, weekly-周度',
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT DEFAULT 1 COMMENT '1-启用,0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 账户表
CREATE TABLE IF NOT EXISTS t_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'cash' COMMENT 'cash-现金, bank-银行卡, alipay-支付宝, wechat-微信, credit-信用卡',
    balance DECIMAL(12, 2) DEFAULT 0,
    initial_balance DECIMAL(12, 2) DEFAULT 0,
    icon VARCHAR(10),
    color VARCHAR(20),
    sort_order INT DEFAULT 0,
    is_default TINYINT DEFAULT 0 COMMENT '1-默认,0-非默认',
    remark VARCHAR(255),
    status TINYINT DEFAULT 1 COMMENT '1-正常,0-禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 账户变动记录表
CREATE TABLE IF NOT EXISTS t_account_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    bill_id BIGINT,
    amount DECIMAL(12, 2) NOT NULL COMMENT '变动金额',
    before_balance DECIMAL(12, 2) NOT NULL,
    after_balance DECIMAL(12, 2) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'income-收入, expense-支出, transfer-转账, adjust-调整',
    remark VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_bill_user_time ON t_bill(user_id, bill_time);
CREATE INDEX idx_bill_user_status ON t_bill(user_id, status);
CREATE INDEX idx_category_rule_keyword ON t_category_rule(keyword);
CREATE INDEX idx_category_rule_user ON t_category_rule(user_id);
CREATE INDEX idx_budget_user_period ON t_budget(user_id, period_type, start_time, end_time);
CREATE INDEX idx_account_user ON t_account(user_id);
CREATE INDEX idx_account_record_account ON t_account_record(account_id);

-- 为已存在的数据库添加 type 字段（如不需要可删除此行）
-- ALTER TABLE t_bill ADD COLUMN type TINYINT DEFAULT 1 COMMENT '1-支出,2-收入';
