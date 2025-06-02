-- 插入测试用户
INSERT INTO users (id, username, email, password_hash, role, created_at, updated_at, is_active) 
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'testuser',
    'test@example.com',
    '$2a$10$dXJ3SW6G7P6.E92wHPkYa.4qQFXTX9QueU/yk2XlMwQxQV6dxqpue', -- password: secret
    'USER',
    NOW(),
    NOW(),
    true
) ON CONFLICT (id) DO NOTHING; 