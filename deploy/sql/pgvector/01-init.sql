CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 向量表由 Spring AI（PgVectorStore initializeSchema=true）在首次启动时自动创建
