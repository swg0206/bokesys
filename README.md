# 基于 Spring Boot 的博客系统（bokesys）

《软件项目开发与实践2》第 19 组 —— 《概要设计》的代码实现。系统按文档第 3 章的
五层体系结构组织（表现层 / 控制层 / 业务逻辑层 / 数据持久层 / 域模型层），
覆盖文档中的 9 张数据表、24 个接口与 Fun_01 ~ Fun_16 全部功能点。

---

## 一、技术栈

| 层次 | 选型 | 版本 |
|---|---|---|
| 框架 | Spring Boot | 4.1.1 |
| 内嵌容器 | Tomcat | 11.0.24（由 Boot 管理） |
| 表现层 | Thymeleaf + Bootstrap 5 | 3.1.5 |
| 持久层 | MyBatis | 3.5.19（starter 4.1.0） |
| 分页 | PageHelper | 6.1.1（starter 4.1.1） |
| 数据库（运行） | H2 内存库（MySQL 兼容模式） | 2.4.240 |
| 数据库（交付对照） | MySQL 8.0 | 8.0.x |
| 构建 | Maven | 3.9+ |
| JDK | 17（在 JDK 24 上以 `--release 17` 编译） | 17+ |

> **与设计文档的一处刻意差异**：文档写的是 Spring Boot 2.7.x。
> 本工程沿用脚手架既有的 **4.1.1**，原因是本机运行环境为 JDK 24，
> 2.7.x 在新版 JDK 上无法正常启动，而降到 JDK 17 会与脚手架现状冲突。
> 文档描述的架构（B/S + MVC + 五层、MyBatis、Thymeleaf、MySQL 8.0、PageHelper）
> 全部保留，差异仅在框架大版本号。

---

## 二、快速开始

```bash
cd "E:/软件设计与重构2/Boke/bokesys"

# 方式一：直接运行
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar target/bokesys-0.0.1-SNAPSHOT.jar
```

启动后访问：

| 入口 | 地址 |
|---|---|
| 前台首页 | http://localhost:8080/ |
| 后台登录 | http://localhost:8080/admin/login |
| 后台仪表盘 | http://localhost:8080/admin/dashboard |
| H2 控制台 | http://localhost:8080/h2-console |

H2 控制台的连接信息（与 `application.yml` 保持一致）：

```
JDBC URL : jdbc:h2:mem:bokesys;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1
User     : sa
Password : （空）
```

### 默认账号

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `123456` | 登录后台；连续 5 次输错锁定 60 秒 |
| 注册用户 | `user` | `123456` | 登录后可发表评论 |

账号由 `config/DataInitializer` 在首次启动时创建 —— 密码必须走 PBKDF2 加盐哈希，
把哈希字面量硬编码进 SQL 极易与 Java 实现对不齐，因此改由启动期用同一份代码现算。

内存库重启即清空，每次启动都会重新建表并灌入种子数据。

---

## 三、接口清单（对应文档 3.6）

### 前台

| 编号 | 方法 | URL | 说明 | 功能点 |
|---|---|---|---|---|
| API-01 | GET | `/` | 首页，按发布时间倒序分页 | Fun_01 |
| API-02 | GET | `/article/{id}` | 文章详情，浏览量自增 | Fun_01 |
| API-03 | GET | `/search?keyword=&pageNo=` | 关键词搜索（标题 + 正文模糊匹配） | Fun_02 |
| API-04 | GET | `/category/{categoryId}` | 分类导航 | Fun_03 |
| API-05 | GET | `/tag/{tagId}` | 标签筛选 | Fun_04 |
| API-06 | GET | `/api/comments?articleId=` | 该文章下已审核评论（树形） | Fun_05 |
| API-07 | POST | `/api/comments` | 提交评论，落为待审核 | Fun_05 |
| API-08 | GET | `/api/links` | 前台友情链接 | Fun_06 |
| — | GET | `/archives` | 文章归档（导航需要，接口清单未单列） | — |

### 后台（均被 `LoginInterceptor` 拦截）

| 编号 | 方法 | URL | 说明 | 功能点 |
|---|---|---|---|---|
| API-09 | POST | `/admin/login` | 管理员登录（含图形验证码） | Fun_07 |
| API-10 | GET | `/admin/logout` | 退出登录 | Fun_07 |
| — | GET | `/admin/captcha` | 图形验证码 PNG | Fun_07 |
| API-11 | GET | `/admin/dashboard` | 仪表盘统计概览 | Fun_08 |
| API-12 | GET | `/admin/articles` | 文章分页列表（标题/分类/状态筛选） | Fun_09 |
| API-13 | POST | `/admin/articles` | 保存并发布 | Fun_09 |
| API-14 | PUT | `/admin/articles/{id}` | 更新文章 | Fun_09 |
| API-15 | DELETE | `/admin/articles/{id}` | 删除文章 | Fun_09 |
| API-16 | POST | `/admin/articles/draft` | 保存为草稿 | Fun_09 |
| — | POST | `/admin/articles/batch-delete` | 批量删除 | Fun_09 |
| API-17 | GET/POST/PUT/DELETE | `/admin/categories[/{id}]` | 分类增删改查 | Fun_10 |
| API-18 | GET/PUT/DELETE | `/admin/comments[/{id}]` | 评论查看 / 审核 / 删除 | Fun_11 |
| API-19 | GET/POST/PUT/DELETE | `/admin/tags[/{id}]` | 标签增删改查 | Fun_12 |
| API-20 | GET/POST/PUT/DELETE | `/admin/links[/{id}]` | 友情链接增删改查 | Fun_13 |
| API-21 | GET/PUT | `/admin/config` | 系统参数配置 | Fun_14 |
| API-22 | GET/PUT | `/admin/profile` | 管理员个人信息 | Fun_15 |
| — | POST | `/admin/profile/upload` | 图片上传（封面 / 头像） | Fun_15 |

### 用户

| 编号 | 方法 | URL | 说明 | 功能点 |
|---|---|---|---|---|
| API-23 | POST | `/user/register` | 注册用户注册 | Fun_16 |
| API-24 | POST | `/user/login` | 注册用户登录 | Fun_16 |
| — | GET | `/user/logout` | 退出登录 | Fun_16 |

**返回约定**：`GET` 返回服务端渲染的 HTML 页面；增删改与 `/api/**` 返回统一返回体
`Result<T>` —— `{ "code": 200, "message": "操作成功", "data": ... }`，
分页数据为 `PageResult<T>` —— `{ "total", "pageNo", "pageSize", "list" }`，
与文档 3.6 的内部接口约定一致。

---

## 四、数据库

### 运行库（H2 内存库）

- `src/main/resources/db/h2/schema.sql` —— 9 张表 + 全部索引
- `src/main/resources/db/h2/data.sql` —— 种子数据（6 分类 / 12 标签 / 8 文章 / 11 评论 / 6 友链 / 6 配置）

### 交付对照库（MySQL 8.0）

- `src/main/resources/db/mysql/bokesys_mysql.sql` —— 与文档 5.2 逐字段对照的版本

与运行版的差异，正是 H2 无法承载的部分：

| 项 | H2 运行版 | MySQL 交付版 |
|---|---|---|
| 存储引擎 / 字符集 | 不声明 | `ENGINE=InnoDB` + `utf8mb4` |
| 关键字字段 | 显示宽度省略（`BIGINT`） | 保留（`bigint(20)`） |
| 全文索引 | **不支持**，已去掉 | `FULLTEXT (title, content) WITH PARSER ngram` |
| 关键词检索 | `LIKE CONCAT('%',?,'%')` | 同样用 LIKE（行为一致），索引另作优化 |

> H2 是否支持 FULLTEXT 是实测结论：在 MySQL 兼容模式下执行
> `CREATE FULLTEXT INDEX ...` 会直接抛 `JdbcSQLSyntaxErrorException`（错误码 42001-240）。
> 因此关键词搜索统一用 `LIKE` 实现，两库行为一致。

### 切换到真实 MySQL

```bash
mysql -u root -p < src/main/resources/db/mysql/bokesys_mysql.sql
```

然后修改 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bokesys?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: 你的密码
  sql:
    init:
      mode: never      # 真实库不要每次启动都重建表
pagehelper:
  helper-dialect: mysql
```

---

## 五、工程结构

```
src/main/java/cn/swust/bokesys/
├── BokesysApplication.java          启动类
├── common/                          通用层
│   ├── result/                      Result<T>、PageResult<T> 统一返回体
│   ├── constant/                    ArticleStatus / CommentStatus / UserStatus / SessionConstant
│   ├── exception/BusinessException  业务异常（携带错误码与可读提示）
│   └── util/                        PasswordUtil(PBKDF2 加盐) / CaptchaUtil / SensitiveWordUtil
│                                    TextUtil / PageUtil
├── po/                              9 个持久化对象，与 9 张表一一对应
├── dto/                             ArticleDTO / ArticleQueryDTO / CommentDTO / UserRegisterDTO
├── vo/                              ArticleVO / CommentVO / DashboardVO / TrendPointVO / CategoryStatVO
├── mapper/                          9 个 MyBatis 接口
├── service/ + service/impl/         9 个业务接口与实现（业务规则与事务边界）
├── controller/
│   ├── base/                        BaseController / AdminBaseController
│   ├── front/                       Index / Article / Comment / Link / User
│   └── admin/                       AdminLogin / Dashboard / Blog / Category / Tag
│                                    CommentAdmin / LinkAdmin / Config / Profile
├── interceptor/LoginInterceptor     拦截 /admin/**，未登录重定向登录页
├── handler/GlobalExceptionHandler   全局异常：异步走 JSON，页面走错误页
└── config/                          WebMvcConfig（拦截器 + 上传目录映射）、DataInitializer

src/main/resources/
├── application.yml                  数据源 / MyBatis / PageHelper / 站点配置
├── mapper/*.xml                     9 个 SQL 映射文件
├── db/h2/ + db/mysql/               两套建库脚本
├── templates/                       Thymeleaf 模板
│   ├── front/ + front/fragments/    前台 8 个页面 + 7 个公共碎片
│   ├── admin/ + admin/fragments/    后台 10 个页面 + 3 个公共碎片
│   └── error/                       业务错误 / 404 / 500
└── static/                          css / js / img
```

---

## 六、已落实的关键业务规则

| 规则 | 位置 | 验证用例 |
|---|---|---|
| 评论提交后一律「待审核」，前台查不到 | `CommentServiceImpl.submit` | `ServiceLayerTests` / `WebLayerTests` |
| 评论审核通过才回写文章评论数（同一事务、幂等） | `CommentServiceImpl.approve` | 同上 |
| 评论敏感词过滤，命中即拒绝 | `SensitiveWordUtil` | `ServiceLayerTests` |
| 删除分类前校验占用，提示转移或解除关联 | `CategoryServiceImpl.delete` | `ServiceLayerTests` / `WebLayerTests` |
| 发布文章时文章与标签关联在同一事务内写入 | `ArticleServiceImpl.create` | `ServiceLayerTests` |
| 密码 PBKDF2 加盐哈希，绝不落明文 | `PasswordUtil` | `ServiceLayerTests` |
| 未登录拦截 `/admin/**`；异步请求返 401 JSON | `LoginInterceptor` | `WebLayerTests` |
| 连续 5 次登录失败锁定 60 秒 | `AdminServiceImpl` | `ServiceLayerTests` |
| 阅读量自增 | `ArticleServiceImpl.viewDetail` | `ServiceLayerTests` / `WebLayerTests` |
| 评论楼按 parentId 组装成树 | `CommentServiceImpl.listApprovedTree` | `ServiceLayerTests` |

### 关于「分类计数」的一处文档自相矛盾

文档 3.3 要求「文章发布时同步维护分类的文章计数」，但 5.2.4 的 `t_category`
字段清单里**并没有计数字段**。本工程按字段清单实现：分类下的文章数量由
`CategoryMapper.selectAllWithCount` 用 `LEFT JOIN` 派生查询得到
（`CategoryServiceImpl` 的注释里也记了这一点）。

这样做的代价是每次列表多一次 JOIN，收益是**不存在计数漂移**，
且交付脚本与文档表结构完全一致。

---

## 七、测试

```bash
mvn test
```

共 57 个用例，分四层：

| 测试类 | 用例数 | 覆盖内容 |
|---|---|---|
| `BokesysApplicationTests` | 1 | 上下文加载 |
| `DatabaseSmokeTests` | 10 | 建表灌数是否生效、中文是否乱码、Mapper 可用、先审核后展示在 SQL 层成立 |
| `ServiceLayerTests` | 25 | 业务规则（事务、幂等、敏感词、级联删除、分页规范化、密码哈希、登录锁定） |
| `WebLayerTests` | 21 | 真实 DispatcherServlet + 拦截器 + **Thymeleaf 模板渲染**，以及登录、发布、审核、删除保护等 HTTP 链路 |

`WebLayerTests` 的价值在于：Thymeleaf 的表达式错误只有真正渲染到那一页才会暴露，
编译和 Service 测试都发现不了；因此每个页面都至少被请求一次并断言关键内容。

---

## 八、已知简化与边界

1. **文章正文按纯文本渲染**（`th:text` + `white-space: pre-wrap`），
   不解析 Markdown、不渲染富文本。这样可以从根本上避免 `th:utext` 带来的 XSS。
   文档 4.2.3 提到的「支持 Markdown 格式编辑」在当前实现中是「以 Markdown 源文存储与展示」。
2. **文章详情页的右侧目录高亮未实现**（文档 6.2.1 提到），属于纯前端增强项。
3. **登录失败锁定状态保存在内存**，重启即清空。因为 `t_admin` 表没有状态字段，
   落库会引入文档之外的字段；单机部署下内存计数足够。
4. **MySQL 交付脚本未经真实 MySQL 执行验证** —— 运行链路全程使用 H2，
   该脚本按文档 5.2 逐字段编写，作为交付对照物。
5. 后台的写操作用 `fetch` 提交（对应文档 6.2.4 描述的局部刷新）。
   HTML 表单本身只能发 GET/POST，表单场景靠 `_method` 隐藏域配合
   `spring.mvc.hiddenmethod.filter.enabled` 转换，该开关已在 `application.yml` 中打开。
