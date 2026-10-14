package cn.swust.bokesys;

import cn.swust.bokesys.common.constant.SessionConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web 层测试：走真实的 DispatcherServlet + 拦截器 + Thymeleaf 渲染管线。
 *
 * <p>这里最大的价值是「模板真的能渲染出来」—— Thymeleaf 的表达式错误只有
 * 真正渲染到那一页才会暴露，光靠编译和 Service 测试是发现不了的。
 * 因此每个页面都至少被请求一次，并对关键内容做断言。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Web 层（控制器 + 模板渲染）测试")
class WebLayerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private cn.swust.bokesys.mapper.ArticleMapper articleMapper;

    // ── 前台页面渲染 ────────────────────────────────────────────────────

    @Test
    @DisplayName("首页渲染成功，含站点名与文章卡片")
    void indexRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("笔迹博客")))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("Spring Boot 约定优于配置的实践总结")))
                // 侧边栏四个区块
                .andExpect(content().string(org.hamcrest.Matchers.containsString("关于站点")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章分类")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("热门标签")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("友情链接")))
                // 页脚备案信息取自 t_config
                .andExpect(content().string(org.hamcrest.Matchers.containsString("蜀ICP备2026000000号")));
    }

    @Test
    @DisplayName("文章详情页渲染成功，且浏览量自增")
    void articleDetailRendersAndIncrementsView() throws Exception {
        MvcResult first = mockMvc.perform(get("/article/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("Spring Boot 约定优于配置的实践总结")))
                // 评论区：已通过审核的评论可见
                .andExpect(content().string(org.hamcrest.Matchers.containsString("小明")))
                // 待审核的评论不出现在前台
                .andExpect(content().string(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.containsString("profile 的优先级具体是怎么排序的"))))
                .andReturn();

        String firstHtml = first.getResponse().getContentAsString();

        MvcResult second = mockMvc.perform(get("/article/1"))
                .andExpect(status().isOk())
                .andReturn();
        String secondHtml = second.getResponse().getContentAsString();

        // 浏览量是在渲染之前自增的，因此第一次访问展示的就是 328 + 1 = 329，
        // 第二次访问展示 330，两次都不应再出现种子里那个初始值 328
        assertTrue(firstHtml.contains("329"),
                "首次访问展示的应是自增后的浏览量 329");
        assertFalse(firstHtml.contains("328"),
                "首次访问不应再展示初始浏览量 328（自增发生在读取之前）");
        assertTrue(secondHtml.contains("330"),
                "第二次访问展示的应是 330");

        // 再从数据库直读一次，避免字符串断言被页面里的其它数字误导
        assertEquals(330, articleMapper.selectById(1L).getViewCount(),
                "两次访问后库存的浏览量应为 328 + 2");
    }

    @Test
    @DisplayName("搜索、分类、标签、归档页均可渲染")
    void listPagesRender() throws Exception {
        mockMvc.perform(get("/search").param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("关键词")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring")));

        mockMvc.perform(get("/search").param("keyword", "不存在的词xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("没有找到匹配的文章")));

        mockMvc.perform(get("/category/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("后端开发")));

        mockMvc.perform(get("/tag/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring Boot")));

        mockMvc.perform(get("/archives"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章归档")));
    }

    @Test
    @DisplayName("用户登录与注册页可渲染")
    void authPagesRender() throws Exception {
        mockMvc.perform(get("/user/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("用户登录")));

        mockMvc.perform(get("/user/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("用户注册")));
    }

    @Test
    @DisplayName("不存在的文章给出业务错误页，而不是 500 堆栈")
    void missingArticleShowsBusinessError() throws Exception {
        mockMvc.perform(get("/article/99999"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章不存在或已被删除")));
    }

    // ── 前台 JSON 接口 ──────────────────────────────────────────────────

    @Test
    @DisplayName("友链接口返回统一 Result 结构")
    void linksApiReturnsResult() throws Exception {
        mockMvc.perform(get("/api/links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                // 只返回 status=1 的 5 条
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("Spring 官方文档"));
    }

    @Test
    @DisplayName("评论接口只返回已通过审核的评论，并且是树形结构")
    void commentsApiReturnsApprovedTree() throws Exception {
        mockMvc.perform(get("/api/comments").param("articleId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                // 文章 1 有 3 条已通过评论，其中 1 条是回复 → 根节点 2 个
                .andExpect(jsonPath("$.data.length()").value(2))
                // 第一条根评论下挂着 1 条回复
                .andExpect(jsonPath("$.data[0].children.length()").value(1))
                .andExpect(jsonPath("$.data[0].children[0].replyToNickname").value("小明"));
    }

    // ── 后台：登录拦截 ──────────────────────────────────────────────────

    @Test
    @DisplayName("未登录访问后台一律重定向到登录页")
    void adminRequiresLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/admin/articles"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));

        mockMvc.perform(get("/admin/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @DisplayName("登录页与验证码接口本身不被拦截")
    void loginPageAndCaptchaAreExcluded() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("后台管理系统")));

        mockMvc.perform(get("/admin/captcha"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/png"));
    }

    @Test
    @DisplayName("验证码错误时登录失败")
    void loginWithWrongCaptchaFails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/admin/captcha").session(session)).andExpect(status().isOk());

        mockMvc.perform(post("/admin/login").session(session)
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("captcha", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .flash().attribute("error", "验证码不正确"));
    }

    @Test
    @DisplayName("验证码正确 + 密码正确时可登录并进入仪表盘")
    void loginWithCorrectCredentialsSucceeds() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 先取验证码，答案在 Session 里
        mockMvc.perform(get("/admin/captcha").session(session)).andExpect(status().isOk());
        Object code = session.getAttribute(SessionConstant.CAPTCHA_CODE);
        assertNotNull(code, "验证码答案应写入 Session");

        mockMvc.perform(post("/admin/login").session(session)
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("captcha", code.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        assertNotNull(session.getAttribute(SessionConstant.LOGIN_ADMIN), "登录后 Session 应有管理员");
    }

    // ── 后台：登录后的页面渲染 ──────────────────────────────────────────

    @Test
    @DisplayName("登录后仪表盘与各管理页均可渲染")
    void adminPagesRenderAfterLogin() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("仪表盘")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章总数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("评论总数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("分类数")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("标签数")))
                // 趋势图与最近文章
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章发布趋势")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("最近发布")))
                // 待审核提醒条（种子数据有 3 条待审核）
                .andExpect(content().string(org.hamcrest.Matchers.containsString("条评论等待审核")));

        mockMvc.perform(get("/admin/articles").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("博客管理")))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("Spring Boot 约定优于配置的实践总结")))
                // 草稿也要出现在后台列表里
                .andExpect(content().string(org.hamcrest.Matchers.containsString("重构笔记")));

        mockMvc.perform(get("/admin/articles/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("文章标题")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("存草稿")));

        mockMvc.perform(get("/admin/articles/1/edit").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("编辑文章")));

        mockMvc.perform(get("/admin/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("分类管理")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("后端开发")));

        mockMvc.perform(get("/admin/tags").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("标签管理")));

        mockMvc.perform(get("/admin/comments").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("评论管理")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("待审核")));

        mockMvc.perform(get("/admin/links").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("友情链接")));

        mockMvc.perform(get("/admin/config").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("站点名称")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("备案信息")));

        mockMvc.perform(get("/admin/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("个人信息")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("修改密码")));
    }

    // ── 后台：写操作 ────────────────────────────────────────────────────

    @Test
    @DisplayName("发布文章：POST 后返回 Result，文章出现在后台列表")
    void createArticleThroughHttp() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/articles").session(session)
                        .param("title", "通过 HTTP 发布的新文章")
                        .param("content", "正文内容，用于验证发布链路。")
                        .param("categoryId", "1")
                        .param("tagIds", "1")
                        .param("tagIds", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("发布成功"))
                .andExpect(jsonPath("$.data").isNumber());

        mockMvc.perform(get("/admin/articles").session(session))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("通过 HTTP 发布的新文章")));

        // 已发布的文章应出现在前台首页
        mockMvc.perform(get("/"))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("通过 HTTP 发布的新文章")));
    }

    @Test
    @DisplayName("存草稿：POST /admin/articles/draft 后不进前台")
    void createDraftThroughHttp() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/articles/draft").session(session)
                        .param("title", "通过 HTTP 保存的草稿")
                        .param("content", "草稿正文")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("草稿已保存"));

        mockMvc.perform(get("/admin/articles").session(session))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("通过 HTTP 保存的草稿")));

        // 草稿不该出现在前台
        mockMvc.perform(get("/"))
                .andExpect(content().string(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.containsString("通过 HTTP 保存的草稿"))));
    }

    @Test
    @DisplayName("标题为空时发布被拒，返回可读的参数校验提示")
    void createArticleValidatesTitle() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 后台的写操作由 fetch 发起，会带 X-Requested-With，异常才走 JSON 分支
        mockMvc.perform(post("/admin/articles").session(session)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("title", "")
                        .param("content", "有正文但没标题")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章标题不能为空"));
    }

    @Test
    @DisplayName("删除有文章的分类被拒，提示先转移或解除关联")
    void deleteCategoryWithArticlesRejected() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/admin/categories/1").session(session)
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该分类下存在 3 篇文章，请先转移或解除关联"));
    }

    @Test
    @DisplayName("评论审核通过后前台可见，且文章评论数增加")
    void approveCommentThroughHttp() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 审核前，前台看不到待审核的评论 4
        mockMvc.perform(get("/article/1"))
                .andExpect(content().string(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.containsString("profile 的优先级具体是怎么排序的"))));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/admin/comments/4/approve").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 审核后前台可见
        mockMvc.perform(get("/article/1"))
                .andExpect(content().string(org.hamcrest.Matchers
                        .containsString("profile 的优先级具体是怎么排序的")));
    }

    @Test
    @DisplayName("未登录的异步写操作返回 401 JSON，而不是重定向到登录页")
    void ajaxWriteWithoutLoginReturns401() throws Exception {
        mockMvc.perform(post("/admin/articles")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .param("title", "未登录发布")
                        .param("content", "内容")
                        .param("categoryId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ── 用户侧交互 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("登录用户可以提交评论，提交后为待审核")
    void submitCommentAsUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/user/login").session(session)
                        .param("username", "user")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/api/comments").session(session)
                        .param("articleId", "2")
                        .param("content", "这是一条来自 HTTP 测试的评论")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("评论提交成功，等待管理员审核通过后展示"));

        // 待审核状态：前台查不到
        mockMvc.perform(get("/api/comments").param("articleId", "2"))
                .andExpect(jsonPath("$.data[?(@.content == '这是一条来自 HTTP 测试的评论')]")
                        .isEmpty());
    }

    @Test
    @DisplayName("未登录提交评论返回 401，提示先登录")
    void submitCommentWithoutLoginRejected() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .param("articleId", "2")
                        .param("content", "游客的评论")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录后再发表评论"));
    }

    @Test
    @DisplayName("命中敏感词的评论被拒")
    void submitSensitiveCommentRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/user/login").session(session)
                .param("username", "user").param("password", "123456"));

        mockMvc.perform(post("/api/comments").session(session)
                        .param("articleId", "2")
                        .param("content", "这里有赌博内容")
                        .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("违规词")));
    }

    // ── 辅助 ────────────────────────────────────────────────────────────

    /** 走一遍带验证码的真实登录流程，返回已登录的会话 */
    private MockHttpSession loginAsAdmin() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/admin/captcha").session(session)).andExpect(status().isOk());
        String code = String.valueOf(session.getAttribute(SessionConstant.CAPTCHA_CODE));
        MvcResult result = mockMvc.perform(post("/admin/login").session(session)
                        .param("username", "admin")
                        .param("password", "123456")
                        .param("captcha", code))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertEquals("/admin/dashboard", result.getResponse().getRedirectedUrl(),
                "登录应成功并跳转仪表盘");
        return session;
    }
}
