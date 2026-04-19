-- V3__seed_prompt_suggestions.sql
-- TDD 5.3 — Seed initial demo Prompt Suggestions data

INSERT INTO prompt_suggestions (owner_id, text, sort_order, enabled)
VALUES
    (1, 'What kinds of projects do you work on? Any open-source work?', 0, TRUE),
    (1, 'What is your tech stack? What are your strongest areas?', 1, TRUE),
    (1, 'Are you open to freelance work or collaboration?', 2, TRUE)
ON CONFLICT DO NOTHING;
