CREATE TABLE IF NOT EXISTS memory_conflict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fact_id BIGINT,
    `key` VARCHAR(100) NOT NULL,
    old_value TEXT NOT NULL,
    new_value TEXT NOT NULL,
    conflict_score INT DEFAULT 0,
    conflict_type VARCHAR(20) DEFAULT 'CONTRADICTION',
    review_status VARCHAR(20) DEFAULT 'PENDING',
    review_result VARCHAR(20),
    ai_analysis TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME
);

CREATE TABLE IF NOT EXISTS memory_fact_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fact_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    `key` VARCHAR(100) NOT NULL,
    `value` TEXT NOT NULL,
    version INT DEFAULT 1,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    change_reason VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memory_reference (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    fact_id BIGINT NOT NULL,
    `key` VARCHAR(100) NOT NULL,
    referenced_content TEXT,
    ai_response TEXT,
    importance_gain INT DEFAULT 1,
    referenced_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
