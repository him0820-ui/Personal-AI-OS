SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS chat_session;
DROP TABLE IF EXISTS memory_reference;
DROP TABLE IF EXISTS memory_fact_history;
DROP TABLE IF EXISTS memory_conflict;
DROP TABLE IF EXISTS memory_todo;
DROP TABLE IF EXISTS memory_goal;
DROP TABLE IF EXISTS memory_timeline;
DROP TABLE IF EXISTS memory_fact;
DROP TABLE IF EXISTS memory_attribute;
DROP TABLE IF EXISTS memory_summary;
DROP TABLE IF EXISTS user;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memory_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    summary TEXT NOT NULL,
    fact_count INT DEFAULT 0,
    goal_count INT DEFAULT 0,
    todo_count INT DEFAULT 0,
    timeline_count INT DEFAULT 0,
    tags VARCHAR(500),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS memory_attribute (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    entity VARCHAR(200) NOT NULL,
    attribute VARCHAR(100) NOT NULL,
    `value` TEXT NOT NULL,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    access_count INT DEFAULT 0,
    last_access_time DATETIME,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_category_entity_attr (user_id, category, entity, attribute),
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS memory_fact (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    `key` VARCHAR(100) NOT NULL,
    `value` TEXT NOT NULL,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    access_count INT DEFAULT 0,
    last_access_time DATETIME,
    memory_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_key (user_id, `key`)
);

CREATE TABLE IF NOT EXISTS memory_timeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    `timestamp` DATETIME NOT NULL,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    access_count INT DEFAULT 0,
    last_access_time DATETIME,
    memory_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memory_goal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    progress INT DEFAULT 0,
    priority INT DEFAULT 0,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    deadline DATE,
    access_count INT DEFAULT 0,
    last_access_time DATETIME,
    memory_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS memory_todo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    priority INT DEFAULT 0,
    importance INT DEFAULT 50,
    confidence INT DEFAULT 0,
    source_quote TEXT,
    due_date DATE,
    access_count INT DEFAULT 0,
    last_access_time DATETIME,
    memory_type VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) DEFAULT '新会话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    think TEXT,
    sender VARCHAR(20) NOT NULL,
    `type` VARCHAR(50) DEFAULT 'TEXT',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (session_id) REFERENCES chat_session(id)
);

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
    reviewed_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (fact_id) REFERENCES memory_fact(id)
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
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (fact_id) REFERENCES memory_fact(id)
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
    referenced_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (fact_id) REFERENCES memory_fact(id),
    FOREIGN KEY (session_id) REFERENCES chat_session(id)
);

SET FOREIGN_KEY_CHECKS=1;