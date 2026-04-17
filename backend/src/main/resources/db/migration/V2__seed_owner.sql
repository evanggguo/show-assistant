-- V2__seed_owner.sql
-- TDD 5.3 — 初始化示例 Owner 数据

INSERT INTO owners (name, tagline, avatar_url, contact, config)
VALUES (
    '示例用户',
    '全栈开发者 & 独立产品人',
    NULL,
    '{"email": "demo@dossier.app", "github": "https://github.com/demo"}',
    '{"theme": "default", "language": "zh-CN"}'
)
ON CONFLICT DO NOTHING;
