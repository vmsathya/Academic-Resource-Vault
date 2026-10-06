-- ==========================================================
-- Academic Resource Vault - Database Schema
-- Compatible with SQLite & standard SQL RDBMS
-- ==========================================================

CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT', -- 'STUDENT' or 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subjects (
    subject_code VARCHAR(30) PRIMARY KEY,
    subject_name VARCHAR(150) NOT NULL,
    semester INTEGER NOT NULL,
    semester_type VARCHAR(10) NOT NULL, -- 'ODD' or 'EVEN'
    department VARCHAR(80) DEFAULT 'Computer Science',
    credits INTEGER DEFAULT 3
);

CREATE TABLE IF NOT EXISTS resources (
    resource_id INTEGER PRIMARY KEY AUTOINCREMENT,
    subject_code VARCHAR(30) NOT NULL,
    resource_type VARCHAR(30) NOT NULL, -- 'NOTES', 'PYQ', 'QUESTION_BANK', 'ANSWER_KEY'
    title VARCHAR(200) NOT NULL,
    description TEXT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size VARCHAR(50) DEFAULT '1.2 MB',
    file_extension VARCHAR(20) DEFAULT 'pdf',
    uploaded_by INTEGER NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    downloads_count INTEGER DEFAULT 0,
    FOREIGN KEY (subject_code) REFERENCES subjects(subject_code) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS bookmarks (
    bookmark_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    resource_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, resource_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS download_logs (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    resource_id INTEGER NOT NULL,
    user_id INTEGER,
    ip_address VARCHAR(50),
    download_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resource_id) REFERENCES resources(resource_id) ON DELETE CASCADE
);

-- Indexes for lightning fast lookups
CREATE INDEX IF NOT EXISTS idx_resources_subject ON resources(subject_code);
CREATE INDEX IF NOT EXISTS idx_resources_type ON resources(resource_type);
CREATE INDEX IF NOT EXISTS idx_subjects_semester ON subjects(semester);
CREATE INDEX IF NOT EXISTS idx_subjects_sem_type ON subjects(semester_type);
CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id);
