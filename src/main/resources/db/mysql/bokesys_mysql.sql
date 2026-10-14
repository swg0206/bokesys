-- ============================================================================
--  博客系统 MySQL 8.0 建库脚本（交付对照版）
--
--  用途：与《概要设计》第 5 章「数据库设计」逐字对照。
--        运行中的应用默认使用 H2 内存库（db/h2/schema.sql），
--        本脚本是把同一套逻辑结构落到真实 MySQL 8.0 上的版本。
--
--  与 H2 运行版的差异（这些差异正是 H2 无法承载的部分）：
--    1. 完整的存储引擎与字符集声明：ENGINE=InnoDB + utf8mb4
--    2. t_article 上保留文档 5.2.10 要求的 FULLTEXT 全文索引
--       —— H2 不支持该语法（实测报 42001-240），运行版只能退化为 LIKE
--    3. 保留字段显示宽度（bigint(20) / int(11) / tinyint(4)），文档表格里就是这么写的
--    4. 中文字段用 ngram 解析器，否则 MySQL 默认分词器对中文几乎无效
--
--  使用方式：
--    mysql -u root -p < bokesys_mysql.sql
--  然后修改 application.yml：
--    spring.datasource.url: jdbc:mysql://localhost:3306/bokesys?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
--    spring.datasource.username / password 改成你的账号
--    spring.sql.init.mode: never      # 真实库不要每次启动都重建，改为手工执行本脚本
--
--  注意：本脚本不插入管理员与注册用户账号。密码必须是 PBKDF2 加盐哈希，
--        硬编码哈希字面量极易与 Java 实现对不齐，统一由启动期的
--        DataInitializer 现算入库（首次启动会自动创建 admin/123456 与 user/123456）。
--        因此下面的 t_comment.user_id 全部为 NULL。
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bokesys
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE bokesys;

SET NAMES utf8mb4;

-- 反序清理，保证脚本可重复执行
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
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    username        VARCHAR(50)  NOT NULL COMMENT '管理员登录账号，唯一',
    password        VARCHAR(100) NOT NULL COMMENT '密码（加盐哈希后的密文）',
    nickname        VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    avatar          VARCHAR(255)          DEFAULT NULL COMMENT '头像图片访问路径',
    email           VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    last_login_time DATETIME              DEFAULT NULL COMMENT '最近一次登录时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '管理员表';

-- ── 5.2.2 注册用户表 ────────────────────────────────────────────────────
CREATE TABLE t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号，唯一',
    password    VARCHAR(100) NOT NULL COMMENT '密码（加盐哈希后的密文）',
    nickname    VARCHAR(50)           DEFAULT NULL COMMENT '昵称，评论展示时优先使用',
    email       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    avatar      VARCHAR(255)          DEFAULT NULL COMMENT '头像图片访问路径',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常 / 0 禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '注册用户表';

-- ── 5.2.4 分类表 ────────────────────────────────────────────────────────
CREATE TABLE t_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    name        VARCHAR(50) NOT NULL COMMENT '分类名称，唯一',
    description VARCHAR(200)         DEFAULT NULL COMMENT '分类描述',
    sort_order  INT         NOT NULL DEFAULT 0 COMMENT '排序序号，升序排列',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME             DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_name (name),
    KEY idx_category_sort_order (sort_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '分类表';

-- ── 5.2.3 文章表 ────────────────────────────────────────────────────────
CREATE TABLE t_article (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    title         VARCHAR(200) NOT NULL COMMENT '文章标题',
    summary       VARCHAR(500)          DEFAULT NULL COMMENT '文章摘要，列表页展示',
    content       LONGTEXT     NOT NULL COMMENT '文章正文（Markdown 源文）',
    cover_image   VARCHAR(255)          DEFAULT NULL COMMENT '封面图访问路径',
    category_id   BIGINT       NOT NULL COMMENT '所属分类',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：1 已发布 / 0 草稿',
    view_count    INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    comment_count INT          NOT NULL DEFAULT 0 COMMENT '已审核通过的评论数',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    -- 文档 5.2.10：支撑 Fun_02 关键词搜索。中文必须用 ngram 解析器，
    -- 否则 MySQL 默认分词器按空格切词，对中文基本检索不出来。
    FULLTEXT KEY idx_title_content (title, content) WITH PARSER ngram,
    -- 支撑 Fun_01 首页按发布时间倒序分页展示已发布文章
    KEY idx_status_create (status, create_time),
    -- 支撑 Fun_03 按分类筛选文章
    KEY idx_category_id (category_id),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES t_category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '文章表';

-- ── 5.2.5 标签表 ────────────────────────────────────────────────────────
CREATE TABLE t_tag (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    name        VARCHAR(50) NOT NULL COMMENT '标签名称，唯一',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '标签表';

-- ── 5.2.6 文章标签关联表 ────────────────────────────────────────────────
CREATE TABLE t_article_tag (
    id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    article_id BIGINT NOT NULL COMMENT '文章 ID',
    tag_id     BIGINT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_tag (article_id, tag_id) COMMENT '避免同一篇文章重复关联同一标签',
    KEY idx_tag_id (tag_id) COMMENT '支撑 Fun_04 按标签反查文章',
    CONSTRAINT fk_article_tag_article FOREIGN KEY (article_id) REFERENCES t_article (id),
    CONSTRAINT fk_article_tag_tag FOREIGN KEY (tag_id) REFERENCES t_tag (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '文章标签关联表';

-- ── 5.2.7 评论表 ────────────────────────────────────────────────────────
CREATE TABLE t_comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    article_id  BIGINT       NOT NULL COMMENT '所属文章 ID',
    parent_id   BIGINT                DEFAULT NULL COMMENT '父评论 ID，为空表示顶层评论',
    user_id     BIGINT                DEFAULT NULL COMMENT '评论人 ID（注册用户）',
    nickname    VARCHAR(50)  NOT NULL COMMENT '评论时展示的昵称（冗余存储）',
    email       VARCHAR(100)          DEFAULT NULL COMMENT '评论人邮箱',
    content     VARCHAR(500) NOT NULL COMMENT '评论内容，长度 1~500 字符',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 待审核 / 1 已通过 / 2 已拒绝',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论提交时间',
    PRIMARY KEY (id),
    KEY idx_article_status (article_id, status) COMMENT '支撑 Fun_05 查询某文章下已审核评论',
    KEY idx_status (status) COMMENT '支撑 Fun_11 后台按状态筛选待审核评论',
    KEY idx_comment_create_time (create_time),
    CONSTRAINT fk_comment_article FOREIGN KEY (article_id) REFERENCES t_article (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES t_comment (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES t_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '评论表';

-- ── 5.2.8 友情链接表 ────────────────────────────────────────────────────
CREATE TABLE t_link (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    name        VARCHAR(50)  NOT NULL COMMENT '链接名称',
    url         VARCHAR(255) NOT NULL COMMENT '链接地址',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序序号，升序排列',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 显示 / 0 隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_sort_status (sort_order, status) COMMENT '支撑 Fun_06 前台友链按序展示'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '友情链接表';

-- ── 5.2.9 系统配置表 ────────────────────────────────────────────────────
CREATE TABLE t_config (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    config_key   VARCHAR(50) NOT NULL COMMENT '配置项键名，如 site_name',
    config_value VARCHAR(500)         DEFAULT NULL COMMENT '配置项值',
    remark       VARCHAR(200)         DEFAULT NULL COMMENT '配置项说明',
    update_time  DATETIME             DEFAULT NULL COMMENT '最近更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '系统配置表';

-- ============================================================================
--  种子数据（与 db/h2/data.sql 内容一致）
-- ============================================================================

INSERT INTO t_config (id, config_key, config_value, remark, update_time) VALUES
  (1, 'site_name',        '笔迹博客',                       '站点名称，显示在导航栏与页面标题', '2026-10-01 09:00:00'),
  (2, 'site_description', '记录技术成长与代码之外的生活',    '站点描述，显示在侧边栏「关于站点」', '2026-10-01 09:00:00'),
  (3, 'site_keywords',    'Spring Boot,MyBatis,Java,博客',  '站点关键词，用于 SEO',             '2026-10-01 09:00:00'),
  (4, 'icp_no',           '蜀ICP备2026000000号',            '备案信息，显示在页脚',             '2026-10-01 09:00:00'),
  (5, 'page_size',        '6',                              '前台每页文章数',                   '2026-10-01 09:00:00'),
  (6, 'author_name',      '第19组',                         '博主署名，显示在页脚版权行',        '2026-10-01 09:00:00');

INSERT INTO t_category (id, name, description, sort_order, create_time, update_time) VALUES
  (1, '后端开发',       'Spring、MyBatis 等服务端框架与工程实践', 1, '2026-09-01 09:00:00', NULL),
  (2, '前端开发',       'Thymeleaf、Bootstrap 与浏览器端交互',    2, '2026-09-01 09:00:00', NULL),
  (3, '数据库',         'MySQL 索引、SQL 优化与事务',             3, '2026-09-01 09:00:00', NULL),
  (4, '运维部署',       'Docker、构建与上线流程',                 4, '2026-09-01 09:00:00', NULL),
  (5, '算法与数据结构', '常用算法与数据结构笔记',                 5, '2026-09-01 09:00:00', NULL),
  (6, '随笔',           '代码之外的技术思考',                     6, '2026-09-01 09:00:00', NULL);

INSERT INTO t_tag (id, name, create_time) VALUES
  (1, 'Spring Boot', '2026-09-01 09:00:00'), (2, 'MyBatis', '2026-09-01 09:00:00'),
  (3, 'MySQL', '2026-09-01 09:00:00'),       (4, 'Java', '2026-09-01 09:00:00'),
  (5, 'Thymeleaf', '2026-09-01 09:00:00'),   (6, 'Bootstrap', '2026-09-01 09:00:00'),
  (7, 'Docker', '2026-09-01 09:00:00'),      (8, '设计模式', '2026-09-01 09:00:00'),
  (9, '并发编程', '2026-09-01 09:00:00'),    (10, '性能优化', '2026-09-01 09:00:00'),
  (11, '重构', '2026-09-01 09:00:00'),       (12, '索引', '2026-09-01 09:00:00');

INSERT INTO t_article (id, title, summary, content, cover_image, category_id, status, view_count, comment_count, create_time, update_time) VALUES
  (1, 'Spring Boot 约定优于配置的实践总结',
      '从自动装配到外部化配置，梳理 Spring Boot 让工程结构收敛下来的几个关键机制。',
      '## 一、自动装配解决了什么问题\n\n在传统 Spring 工程里，引入一个中间件往往意味着新增一份 XML 或 JavaConfig，装配逻辑散落在各处。Spring Boot 用 AutoConfiguration.imports 把「条件满足就生效」的装配类集中登记，于是使用方只需要引入依赖。\n\n## 二、外部化配置\n\napplication.yml 中的配置项按 profile 覆盖，优先级从命令行参数、环境变量到配置文件依次降低。\n\n## 三、分层与单一职责\n\n控制层只负责接收请求、校验参数与选择视图；业务逻辑层承载业务规则与事务边界；持久层只做 SQL 与对象映射。\n\n## 四、小结\n\n约定优于配置把重复的装配决策固化成默认值，让开发者只需要关注那些真正不同的部分。',
      NULL, 1, 1, 328, 3, '2026-10-13 09:12:00', NULL),

  (2, 'MyBatis 动态 SQL 与分页的取舍',
      'if、where、foreach 三个标签覆盖了后台列表页的绝大多数筛选场景，配合分页插件即可。',
      '## 动态 SQL 的三个高频标签\n\nif 按条件拼接，注意配合 where 使用；where 自动去掉首个 AND 或 OR；foreach 是批量插入与 IN 查询的必备。\n\n## 分页的两种做法\n\n物理分页由数据库在 LIMIT 层面完成；逻辑分页把全量结果取回内存再截取，文章量一大就会拖垮应用。生产环境一律选物理分页。\n\n## 参数一律用预编译占位符\n\n#{} 会生成 PreparedStatement 的占位符，从根源上避免 SQL 注入。',
      NULL, 1, 1, 256, 2, '2026-10-12 14:05:00', NULL),

  (3, 'MySQL 索引设计：从慢查询到执行计划',
      '联合索引的最左前缀原则、覆盖索引与回表，是排查慢查询时绕不开的三个概念。',
      '## 最左前缀原则\n\n联合索引 (a, b, c) 只有在查询条件从 a 开始连续使用时才能被完整利用。\n\n## 覆盖索引与回表\n\n如果查询所需字段全部包含在索引中，存储引擎无需回表取行数据。\n\n## 选择性与索引代价\n\n索引不是越多越好。每个索引都会拖慢写入，并且占用额外空间。\n\n## 执行计划怎么看\n\n重点关注 type、key、rows 与 Extra 四列。',
      NULL, 3, 1, 412, 1, '2026-10-10 20:30:00', NULL),

  (4, 'Thymeleaf 模板复用：碎片与布局',
      '用 th:replace 与碎片表达式把页头、页脚、侧边栏抽出去，避免每页重复。',
      '## 碎片定义与引用\n\n碎片用 th:fragment 声明，引用时写完整的碎片表达式。3.1 版本之后统一使用 ~{} 形式。\n\n## 传参\n\n碎片可以接收参数，调用方用逗号分隔的键值对传入。\n\n## 时间格式化\n\n#temporals 已经并入核心模块，直接用它格式化 LocalDateTime 即可。\n\n## 关于正文渲染\n\n文章正文使用 th:text 而不是 th:utext，避免用户提交的内容里的标签被当作 HTML 执行。',
      NULL, 2, 1, 189, 1, '2026-10-08 11:20:00', NULL),

  (5, 'Bootstrap 栅格与响应式断点实践',
      '从 900px 与 640px 两个断点出发，说明双栏布局在窄屏下的退化策略。',
      '## 断点的作用\n\n断点不是为了适配具体机型，而是为了让布局在内容开始变得拥挤时换一种排布方式。\n\n## 640px 以下的取舍\n\n窄屏下导航项横向排布会溢出，折叠成汉堡菜单是通行做法。\n\n## 移动优先还是桌面优先\n\n本项目以桌面阅读为主，因此采用桌面优先、向下覆盖的写法。',
      NULL, 2, 1, 97, 0, '2026-10-06 16:45:00', NULL),

  (6, '用 Docker 打包一个 Spring Boot 应用',
      '分层构建、时区与编码，是把 Java 应用塞进容器时最容易忽略的三件事。',
      '## 分层构建\n\n先拷贝 pom.xml 执行依赖下载，再拷贝源码编译，只要 pom 没变，依赖层就能命中缓存。\n\n## 时区\n\n容器默认使用 UTC。应用日志与数据库时间对不上，往往就是没设置 TZ 环境变量所致。\n\n## 编码\n\n镜像里如果没有 UTF-8 的 locale，JVM 会退回到 ASCII 或 latin1，中文会变成问号。',
      NULL, 4, 1, 143, 1, '2026-10-04 10:10:00', NULL),

  (7, '重构笔记：先把重复揪出来（草稿）',
      '重复代码的三种典型形态，以及提取时机的判断标准。',
      '这篇文章还在整理中，先记录几个要点：\n\n- 三行相似尚可容忍，第五次复制粘贴就该提取了\n- 提取时优先关注「变化的方向是否一致」\n- 重构的第一步永远是补上测试',
      NULL, 6, 0, 12, 0, '2026-10-14 08:30:00', NULL),

  (8, '关于并发编程的一点思考（草稿）',
      '从可见性、原子性、有序性三个维度梳理并发问题的根源。',
      '草稿，待补充。\n\n可见性依赖于内存屏障与 volatile；原子性需要锁或 CAS；有序性则与指令重排和 happens-before 规则相关。',
      NULL, 1, 0, 5, 0, '2026-10-14 08:40:00', NULL);

INSERT INTO t_article_tag (id, article_id, tag_id) VALUES
  (1, 1, 1), (2, 1, 4), (3, 1, 8),
  (4, 2, 2), (5, 2, 1), (6, 2, 10),
  (7, 3, 3), (8, 3, 12), (9, 3, 10),
  (10, 4, 5), (11, 4, 6),
  (12, 5, 6),
  (13, 6, 7), (14, 6, 4),
  (15, 7, 11), (16, 7, 8),
  (17, 8, 9), (18, 8, 4);

-- status: 0 待审核 / 1 已通过 / 2 已拒绝
-- 已通过条数与 t_article.comment_count 一致：文章1=3、2=2、3=1、4=1、5=0、6=1
INSERT INTO t_comment (id, article_id, parent_id, user_id, nickname, email, content, status, create_time) VALUES
  (1,  1, NULL, NULL, '小明',    'xiaoming@example.com', '自动装配那一段讲得很清楚，之前一直没弄明白 starter 到底做了什么。', 1, '2026-10-13 10:20:00'),
  (2,  1, NULL, NULL, '阿May',   'amay@example.com',     '分层那部分建议再补充一下 DTO 和 VO 的转换时机。',                   1, '2026-10-13 11:05:00'),
  (3,  1, 1,    NULL, '博主',    NULL,                   '谢谢，starter 本质就是一个带自动装配配置的普通依赖。',            1, '2026-10-13 12:30:00'),
  (4,  1, NULL, NULL, '路人甲',  NULL,                   '想问下 profile 的优先级具体是怎么排序的？',                        0, '2026-10-13 15:40:00'),
  (5,  2, NULL, NULL, '小张',    'xiaozhang@example.com', '预编译占位符那段提醒得好，我们项目里还有 ${} 拼排序字段的写法，得改。', 1, '2026-10-12 16:00:00'),
  (6,  2, NULL, NULL, '老王',    NULL,                   'foreach 批量插入记得控制单次条数，一次几千条会超包大小。',          1, '2026-10-12 18:22:00'),
  (7,  3, NULL, NULL, 'DBA 小陈', 'chen@example.com',    '最左前缀那部分可以再举个反例，跳过首列的情况最容易被忽略。',        1, '2026-10-11 09:15:00'),
  (8,  3, NULL, NULL, '匿名',    NULL,                   '请问 status 这种低基数列到底要不要建索引？',                        0, '2026-10-11 10:00:00'),
  (9,  4, NULL, NULL, '前端小刘', NULL,                  '白名单那段很关键，th:utext 用错就是 XSS。',                        1, '2026-10-09 14:30:00'),
  (10, 6, NULL, NULL, '运维小赵', NULL,                  '时区这个坑我踩过，日志时间全错，排查了一下午。',                    1, '2026-10-05 20:10:00'),
  (11, 5, NULL, NULL, '设计爱好者', NULL,                '窄屏下侧栏下移的思路很实用，比直接隐藏侧栏友好。',                  0, '2026-10-07 13:00:00');

INSERT INTO t_link (id, name, url, sort_order, status, create_time) VALUES
  (1, 'Spring 官方文档',  'https://spring.io/projects/spring-boot', 1, 1, '2026-09-01 09:00:00'),
  (2, 'MyBatis 中文文档', 'https://mybatis.org/mybatis-3/zh/',      2, 1, '2026-09-01 09:00:00'),
  (3, 'MySQL 官方手册',   'https://dev.mysql.com/doc/',             3, 1, '2026-09-01 09:00:00'),
  (4, 'Bootstrap 中文网', 'https://v5.bootcss.com/',                4, 1, '2026-09-01 09:00:00'),
  (5, 'MDN Web 文档',     'https://developer.mozilla.org/zh-CN/',   5, 1, '2026-09-01 09:00:00'),
  (6, '暂未启用的友链',   'https://example.com/hidden',             6, 0, '2026-09-01 09:00:00');
