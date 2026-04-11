# 数据库表设计 UML

```mermaid
erDiagram

    owners {
        bigserial id PK
        varchar(100) name "NOT NULL"
        varchar(255) tagline
        varchar(500) avatar_url
        jsonb contact "默认 {}"
        jsonb config "默认 {}"
        timestamptz created_at
    }

    client_users {
        bigserial id PK
        varchar(50) sso_provider "NOT NULL google/github"
        varchar(255) sso_id "NOT NULL"
        varchar(100) nickname
        varchar(500) avatar_url
        timestamptz created_at
    }

    conversations {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        bigint user_id FK "NULL = 游客"
        varchar(50) source "默认 web"
        timestamptz created_at
        timestamptz updated_at
    }

    messages {
        bigserial id PK
        bigint conversation_id FK "NOT NULL"
        varchar(20) role "user / assistant / system"
        text content "NOT NULL"
        timestamptz created_at
    }

    dynamic_suggestions {
        bigserial id PK
        bigint message_id FK "NOT NULL"
        varchar(500) text "NOT NULL"
        int sort_order "默认 0"
    }

    prompt_suggestions {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(500) text "NOT NULL"
        int sort_order "默认 0"
        boolean enabled "默认 true"
    }

    knowledge_entries {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(50) type "skill/experience/project/education/service/other"
        varchar(255) title
        text content "NOT NULL"
        vector_1536 embedding "Phase 3 向量检索"
        bigint source_doc FK "可为 NULL"
        timestamptz created_at
    }

    documents {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(255) filename "NOT NULL"
        varchar(50) file_type
        bigint file_size
        varchar(500) file_path
        varchar(50) status "pending/processing/done/failed"
        timestamptz created_at
    }

    owners ||--o{ conversations : "拥有"
    owners ||--o{ prompt_suggestions : "配置"
    owners ||--o{ knowledge_entries : "维护"
    owners ||--o{ documents : "上传"

    client_users ||--o{ conversations : "发起"

    conversations ||--o{ messages : "包含"

    messages ||--o{ dynamic_suggestions : "生成"

    documents ||--o{ knowledge_entries : "提取自"
```

---

## 表关系说明

| 关系 | 类型 | 说明 |
|------|------|------|
| owners → conversations | 1 : N | 一个 Owner 下有多个对话会话 |
| owners → prompt_suggestions | 1 : N | Owner 在管理端配置首屏初始提示词 |
| owners → knowledge_entries | 1 : N | Owner 维护的知识库条目 |
| owners → documents | 1 : N | Owner 上传的原始文件 |
| client_users → conversations | 1 : N | 已登录用户发起的对话（游客时 user_id = NULL）|
| conversations → messages | 1 : N | 一个会话包含多条消息 |
| messages → dynamic_suggestions | 1 : N | 每条 assistant 消息生成 2~4 条动态提示词 |
| documents → knowledge_entries | 1 : N | 一个文件可提取多条知识条目（source_doc 可为 NULL，表示手动录入） |

---

## 关键索引

| 索引名 | 表 | 列 | 类型 | 用途 |
|--------|----|----|------|------|
| `idx_knowledge_entries_embedding` | knowledge_entries | embedding | HNSW (cosine) | Phase 3 向量相似度检索 |
| `idx_messages_conversation_id` | messages | conversation_id, created_at | B-tree | 按会话加载历史消息 |
| `idx_knowledge_entries_owner_id` | knowledge_entries | owner_id | B-tree | 按 Owner 筛选知识条目 |
| `idx_prompt_suggestions_owner_id` | prompt_suggestions | owner_id, sort_order | B-tree | 首屏提示词排序查询 |
| `idx_conversations_owner_id` | conversations | owner_id | B-tree | 管理端查看会话列表 |
| UNIQUE (sso_provider, sso_id) | client_users | — | Unique | 防止 SSO 用户重复注册 |
