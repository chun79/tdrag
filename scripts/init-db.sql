-- 初始化RAG数据库脚本
-- 创建必要的扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- 创建文档表
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id VARCHAR(255) UNIQUE NOT NULL,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    category VARCHAR(50) DEFAULT 'general',
    upload_user_id UUID REFERENCES users(id),
    upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    chunks_count INTEGER DEFAULT 0,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建聊天历史表
CREATE TABLE IF NOT EXISTS chat_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL,
    user_id UUID REFERENCES users(id),
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    model_used VARCHAR(50),
    complexity VARCHAR(20),
    response_time INTEGER, -- 响应时间(毫秒)
    token_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
);

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    config_key VARCHAR(100) UNIQUE NOT NULL,
    config_value TEXT,
    description TEXT,
    config_type VARCHAR(20) DEFAULT 'STRING',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建模型使用统计表
CREATE TABLE IF NOT EXISTS model_usage_stats (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_name VARCHAR(50) NOT NULL,
    usage_date DATE NOT NULL,
    request_count INTEGER DEFAULT 0,
    total_tokens INTEGER DEFAULT 0,
    avg_response_time FLOAT DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    error_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(model_name, usage_date),
    INDEX idx_model_date (model_name, usage_date)
);

-- 插入默认用户
INSERT INTO users (username, email, password_hash, role) 
VALUES 
    ('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFGjwvKtLID/h8PmsPTWNl.', 'ADMIN'),
    ('demo', 'demo@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iYqiSfFGjwvKtLID/h8PmsPTWNl.', 'USER')
ON CONFLICT (username) DO NOTHING;

-- 插入默认系统配置
INSERT INTO system_config (config_key, config_value, description, config_type) 
VALUES 
    ('RAG_CHUNK_SIZE', '1000', 'RAG文档分块大小', 'INTEGER'),
    ('RAG_CHUNK_OVERLAP', '200', 'RAG文档分块重叠大小', 'INTEGER'),
    ('RAG_SIMILARITY_THRESHOLD', '0.7', 'RAG相似度阈值', 'FLOAT'),
    ('RAG_MAX_CONTEXT_LENGTH', '4000', 'RAG最大上下文长度', 'INTEGER'),
    ('CACHE_TTL', '3600', '缓存过期时间(秒)', 'INTEGER'),
    ('MAX_FILE_SIZE', '52428800', '最大文件上传大小(字节)', 'LONG'),
    ('ALLOWED_FILE_TYPES', 'pdf,txt,md,docx,doc', '允许的文件类型', 'STRING')
ON CONFLICT (config_key) DO NOTHING;

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为表添加更新时间触发器
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_config_updated_at BEFORE UPDATE ON system_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_model_usage_stats_updated_at BEFORE UPDATE ON model_usage_stats
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 创建性能分析视图
CREATE OR REPLACE VIEW model_performance_view AS
SELECT 
    model_name,
    DATE_TRUNC('day', created_at) as date,
    COUNT(*) as total_requests,
    AVG(response_time) as avg_response_time,
    SUM(CASE WHEN answer IS NOT NULL THEN 1 ELSE 0 END) as success_count,
    COUNT(*) - SUM(CASE WHEN answer IS NOT NULL THEN 1 ELSE 0 END) as error_count
FROM chat_history 
GROUP BY model_name, DATE_TRUNC('day', created_at)
ORDER BY date DESC;

-- 创建用户活跃度视图
CREATE OR REPLACE VIEW user_activity_view AS
SELECT 
    u.username,
    COUNT(ch.id) as total_questions,
    MAX(ch.created_at) as last_activity,
    DATE_TRUNC('day', ch.created_at) as activity_date
FROM users u
LEFT JOIN chat_history ch ON u.id = ch.user_id
GROUP BY u.id, u.username, DATE_TRUNC('day', ch.created_at)
ORDER BY activity_date DESC;

-- 打印初始化完成信息
DO $$
BEGIN
    RAISE NOTICE 'RAG数据库初始化完成!';
    RAISE NOTICE '默认用户: admin/admin123, demo/demo123';
    RAISE NOTICE '请在应用中修改默认密码';
END
$$; 