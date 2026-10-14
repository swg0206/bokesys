-- ============================================================================
--  博客系统 H2 建表脚本（运行用）
--  对应《概要设计》5.2 数据库逻辑结构，共 9 张表。
--
--  与 db/mysql/bokesys_mysql.sql 的差异（仅为适配 H2）：
--    1. 不写显示宽度（BIGINT(20) -> BIGINT），H2 2.x 对整数精度校验更严
--    2. 去掉 t_article 的 FULLTEXT 全文索引，H2 不支持该语法（已实测报 42001-240）
--       —— 关键词检索改用 LIKE 模糊匹配，见 ArticleMapper.xml
--    3. 不写 ENGINE=InnoDB / DEFAULT CHARSET=utf8mb4 等存储引擎子句
--  其余字段、主键、外键、唯一约束、索引与 MySQL 版保持一致。
-- ============================================================================

-- ── 清理旧表 ────────────────────────────────────────────────────────────
-- 内存库配了 DB_CLOSE_DELAY=-1，会在整个 JVM 生命周期内存活。
-- 因此同一个 JVM 内启动第二个 Spring 上下文时（例如测试里带了
-- @AutoConfigureMockMvc 的配置与默认配置不同，会各建一个上下文），
-- 建表语句会撞上「表已存在」。先按外键反序删干净，脚本就可以反复执行。
DROP TABLE IF EXISTS t_comment;
DROP TABLE IF EXISTS t_article_tag;
DROP TABLE IF EXISTS t_article;
DROP TABLE IF EXISTS t_tag;
DROP TABLE IF EXISTS t_category;
DROP TABLE IF EXISTS t_user;
DROP TABLE IF EXISTS t_admin;
DROP TABLE IF EXISTS t_link;
DROP TABLE IF EXISTS t_config;

-- ── 5.2.1 管理员表 ──────────────────────────────────────────────────────
CREATE TABLE t_admin (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    nickname        VARCHAR(50),
    avatar          VARCHAR(255),
    email           VARCHAR(100),
    last_login_time DATETIME,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_username UNIQUE (username)
);

-- ── 5.2.2 注册用户表 ────────────────────────────────────────────────────
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    email       VARCHAR(100),
    avatar      VARCHAR(255),
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);
CREATE INDEX idx_user_status ON t_user (status);

-- ── 5.2.4 分类表 ────────────────────────────────────────────────────────
-- 先建 t_category，供 t_article 外键引用
CREATE TABLE t_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order  INT         NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_category_name UNIQUE (name)
);
CREATE INDEX idx_category_sort_order ON t_category (sort_order);

-- ── 5.2.3 文章表 ────────────────────────────────────────────────────────
CREATE TABLE t_article (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    title         VARCHAR(200) NOT NULL,
    summary       VARCHAR(500),
    content       LONGTEXT     NOT NULL,
    cover_image   VARCHAR(255),
    category_id   BIGINT       NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0,
    view_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES t_category (id)
);
CREATE INDEX idx_article_status_create ON t_article (status, create_time);
CREATE INDEX idx_article_category_id ON t_article (category_id);

-- ── 5.2.5 标签表 ────────────────────────────────────────────────────────
CREATE TABLE t_tag (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_tag_name UNIQUE (name)
);

-- ── 5.2.6 文章标签关联表 ────────────────────────────────────────────────
CREATE TABLE t_article_tag (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_article_tag UNIQUE (article_id, tag_id),
    CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES t_article (id),
    CONSTRAINT fk_article_tag_tag FOREIGN KEY (tag_id) REFERENCES t_tag (id)
);
CREATE INDEX idx_article_tag_tag_id ON t_article_tag (tag_id);

-- ── 5.2.7 评论表 ────────────────────────────────────────────────────────
CREATE TABLE t_comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    article_id  BIGINT       NOT NULL,
    parent_id   BIGINT,
    user_id     BIGINT,
    nickname    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100),
    content     VARCHAR(500) NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES t_article (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES t_comment (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES t_user (id)
);
CREATE INDEX idx_comment_article_status ON t_comment (article_id, status);
CREATE INDEX idx_comment_status ON t_comment (status);
CREATE INDEX idx_comment_create_time ON t_comment (create_time);

-- ── 5.2.8 友情链接表 ────────────────────────────────────────────────────
CREATE TABLE t_link (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    url         VARCHAR(255) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_link_sort_status ON t_link (sort_order, status);

-- ── 5.2.9 系统配置表 ────────────────────────────────────────────────────
-- 注意：列名用 config_key / config_value，未使用 H2 保留字 key / value
CREATE TABLE t_config (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    config_key   VARCHAR(50) NOT NULL,
    config_value VARCHAR(500),
    remark       VARCHAR(200),
    update_time  DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_config_key UNIQUE (config_key)
);
