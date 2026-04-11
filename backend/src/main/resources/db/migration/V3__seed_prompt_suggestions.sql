-- V3__seed_prompt_suggestions.sql
-- TDD 5.3 — 初始化示例 Prompt Suggestions 数据

INSERT INTO prompt_suggestions (owner_id, text, sort_order, enabled)
VALUES
    (1, '你平时做什么项目？有没有开源作品？', 0, TRUE),
    (1, '你的技术栈是什么？最擅长哪个方向？', 1, TRUE),
    (1, '你有接外包或者合作的意向吗？', 2, TRUE)
ON CONFLICT DO NOTHING;
