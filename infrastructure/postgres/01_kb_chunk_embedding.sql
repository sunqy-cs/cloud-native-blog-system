-- 知识库文档块向量表：用于 RAG 向量检索
-- 与 MySQL 中 knowledge_base、content、knowledge_base_content 对应，此处仅存向量与元数据，不做跨库外键

CREATE TABLE IF NOT EXISTS kb_chunk_embedding (
    id           BIGSERIAL PRIMARY KEY,
    kb_id        BIGINT        NOT NULL,
    content_id   BIGINT        NOT NULL,
    chunk_index  INT           NOT NULL DEFAULT 0,
    content      TEXT          NOT NULL,
    embedding    vector(1024) NOT NULL,
    created_at   TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE kb_chunk_embedding IS '知识库文档块向量表，供 RAG 语义检索';
COMMENT ON COLUMN kb_chunk_embedding.kb_id IS '知识库 ID（对应 MySQL knowledge_base.id）';
COMMENT ON COLUMN kb_chunk_embedding.content_id IS '内容 ID（对应 MySQL content.id）';
COMMENT ON COLUMN kb_chunk_embedding.chunk_index IS '文档内块序号';
COMMENT ON COLUMN kb_chunk_embedding.content IS '块原文';
COMMENT ON COLUMN kb_chunk_embedding.embedding IS '向量，维度与百炼 text-embedding-v4 默认一致';

CREATE INDEX IF NOT EXISTS idx_kb_chunk_kb_id ON kb_chunk_embedding (kb_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_content_id ON kb_chunk_embedding (content_id);
CREATE INDEX IF NOT EXISTS idx_kb_chunk_created_at ON kb_chunk_embedding (created_at);

-- 向量相似度检索索引（余弦距离，语义检索常用）
CREATE INDEX IF NOT EXISTS idx_kb_chunk_embedding_hnsw
    ON kb_chunk_embedding
    USING hnsw (embedding vector_cosine_ops);
