-- OAuth 2.0 授权服务器基础表结构。
-- 使用标准 SQL 与字符串主键，避免各数据库 IDENTITY/AUTO_INCREMENT 方言差异；
-- 时间与 TTL 使用 NUMERIC(19) 存储 Unix 秒，兼容 H2/PostgreSQL/MySQL/MariaDB/SQLite/SQL Server/Oracle。

CREATE TABLE oauth_client (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    client_id VARCHAR(128) NOT NULL UNIQUE,
    client_secret VARCHAR(256) NOT NULL,
    client_name VARCHAR(128) NOT NULL,
    redirect_uri VARCHAR(2048),
    grant_types VARCHAR(512),
    scope VARCHAR(512),
    token_endpoint_auth_method VARCHAR(256),
    require_pkce VARCHAR(8) DEFAULT 'false',
    access_token_ttl NUMERIC(19),
    refresh_token_ttl NUMERIC(19),
    enabled VARCHAR(8) DEFAULT 'true',
    create_time NUMERIC(19) NOT NULL
);

CREATE TABLE oauth_user (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    nickname VARCHAR(128),
    email VARCHAR(256),
    rolename VARCHAR(128),
    enabled VARCHAR(8) DEFAULT 'true',
    create_time NUMERIC(19) NOT NULL
);

CREATE TABLE oauth_auth_code (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    code VARCHAR(128) NOT NULL UNIQUE,
    client_id VARCHAR(128) NOT NULL,
    username VARCHAR(128) NOT NULL,
    redirect_uri VARCHAR(2048),
    scope VARCHAR(512),
    code_challenge VARCHAR(128),
    code_challenge_method VARCHAR(16),
    nonce VARCHAR(256),
    auth_time NUMERIC(19),
    expires_at NUMERIC(19) NOT NULL,
    used INTEGER DEFAULT 0,
    create_time NUMERIC(19) NOT NULL
);

CREATE TABLE oauth_token (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    access_token VARCHAR(128) NOT NULL UNIQUE,
    refresh_token VARCHAR(128),
    client_id VARCHAR(128) NOT NULL,
    username VARCHAR(128),
    token_type VARCHAR(32),
    scope VARCHAR(512),
    expires_at NUMERIC(19) NOT NULL,
    refresh_expires_at NUMERIC(19),
    create_time NUMERIC(19) NOT NULL
);
