package cn.swust.bokesys;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.constant.CommentStatus;
import cn.swust.bokesys.common.constant.UserStatus;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.util.PasswordUtil;
import cn.swust.bokesys.dto.ArticleDTO;
import cn.swust.bokesys.dto.CommentDTO;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.ArticleTagMapper;
import cn.swust.bokesys.mapper.UserMapper;
import cn.swust.bokesys.po.Article;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.AdminService;
import cn.swust.bokesys.service.ArticleService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.CommentService;
import cn.swust.bokesys.service.DashboardService;
import cn.swust.bokesys.service.UserService;
import cn.swust.bokesys.vo.CommentVO;
import cn.swust.bokesys.vo.DashboardVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业务逻辑层测试：逐条验证《概要设计》第 2、3、4 章里明确的业务规则。
 *
 * <p>整个类标注 {@code @Transactional}，每个测试方法跑完自动回滚，
 * 因此用例之间不会互相污染共享的内存库。
 */
@SpringBootTest
@Transactional
@DisplayName("业务逻辑层规则测试")
class ServiceLayerTests {

    @Autowired
    private CommentService commentService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private ArticleTagMapper articleTagMapper;

    // ── 评论：先审核后展示 ──────────────────────────────────────────────

    @Test
    @DisplayName("提交的评论一律为待审核，前台查不到")
    void submittedCommentIsPendingAndInvisible() {
        User user = userMapper.selectByUsername("user");
        assertNotNull(user, "演示用户应已由 DataInitializer 初始化");

        int before = approvedCount(2L);

        CommentDTO dto = new CommentDTO();
        dto.setArticleId(2L);
        dto.setContent("这是一条待审核的测试评论");
        commentService.submit(dto, user);

        assertEquals(before, approvedCount(2L),
                "刚提交的评论处于待审核状态，前台必须查不到");
        assertEquals(1, commentService.countPending() - 3,
                "待审核评论数应增加 1（种子数据原本 3 条）");
    }

    @Test
    @DisplayName("游客未登录不能发表评论")
    void anonymousCannotComment() {
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(2L);
        dto.setContent("游客的评论");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> commentService.submit(dto, null));
        assertTrue(ex.getMessage().contains("登录"), "应提示先登录，实际：" + ex.getMessage());
    }

    @Test
    @DisplayName("命中敏感词的评论被拒绝提交")
    void sensitiveCommentRejected() {
        User user = userMapper.selectByUsername("user");
        CommentDTO dto = new CommentDTO();
        dto.setArticleId(2L);
        dto.setContent("这里有赌博相关内容，应当被拦截");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> commentService.submit(dto, user));
        assertTrue(ex.getMessage().contains("违规词"), "应提示命中违规词，实际：" + ex.getMessage());
    }

    @Test
    @DisplayName("审核通过后评论出现在前台，且文章评论数 +1")
    void approveMakesCommentVisibleAndBumpsCount() {
        // 种子数据：文章 1 有 4 条评论，其中 3 条已通过（评论 3 是评论 1 的回复，嵌在树里）
        assertEquals(3, articleMapper.selectById(1L).getCommentCount());
        assertEquals(3, approvedCount(1L));

        commentService.approve(4L);

        assertEquals(4, approvedCount(1L), "审核通过后前台应能看到");
        assertEquals(4, articleMapper.selectById(1L).getCommentCount(),
                "审核通过必须回写文章的评论数");
    }

    @Test
    @DisplayName("重复审核通过不会把评论数加两次（幂等）")
    void approveIsIdempotent() {
        commentService.approve(4L);
        commentService.approve(4L);
        assertEquals(4, articleMapper.selectById(1L).getCommentCount(),
                "重复审核不应重复累加评论数");
    }

    @Test
    @DisplayName("已通过的评论被拒绝时，评论数回退")
    void rejectRollsBackCount() {
        // 评论 1 是文章 1 的已通过评论
        assertEquals(3, articleMapper.selectById(1L).getCommentCount());
        commentService.reject(1L);
        assertEquals(2, articleMapper.selectById(1L).getCommentCount(),
                "把已通过的评论改为拒绝，评论数应减回去");
    }

    @Test
    @DisplayName("评论树按 parentId 组装成评论楼")
    void commentTreeAssembled() {
        List<CommentVO> tree = commentService.listApprovedTree(1L);
        // 种子数据里评论 3 是对评论 1 的回复
        CommentVO root1 = tree.stream()
                .filter(c -> c.getId() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals(1, root1.getChildren().size(), "评论 1 下应挂着 1 条回复");
        assertEquals("博主", root1.getChildren().get(0).getNickname());
        assertEquals("小明", root1.getChildren().get(0).getReplyToNickname(),
                "回复应带上被回复者昵称");
    }

    @Test
    @DisplayName("删除评论会连同其回复一起删除")
    void deleteCommentRemovesReplies() {
        // 评论 1 下挂着评论 3；两者都是已通过状态（共 2 条计入文章 1 的评论数）
        assertEquals(3, articleMapper.selectById(1L).getCommentCount());
        commentService.delete(1L);
        assertEquals(1, articleMapper.selectById(1L).getCommentCount(),
                "父评论与其已通过的回复都应从评论数里扣除");
        assertTrue(flatten(commentService.listApprovedTree(1L)).stream()
                        .noneMatch(c -> c.getId() == 1L || c.getId() == 3L),
                "评论 1 与回复 3 都应消失（包括树里的子节点）");
    }

    // ── 分类：删除前校验占用 ────────────────────────────────────────────

    @Test
    @DisplayName("删除有文章的分类被拒绝，并给出台数与处置建议")
    void deleteCategoryWithArticlesIsRejected() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.delete(1L));
        assertTrue(ex.getMessage().contains("该分类下存在 3 篇文章"),
                "应提示该分类下存在 N 篇文章，实际：" + ex.getMessage());
        assertTrue(ex.getMessage().contains("转移") || ex.getMessage().contains("解除关联"),
                "应给出处置建议，实际：" + ex.getMessage());
        assertNotNull(categoryService.getById(1L), "被拒绝后分类应当仍然存在");
    }

    @Test
    @DisplayName("空分类可以正常删除")
    void emptyCategoryCanBeDeleted() {
        // 分类 5「算法与数据结构」下没有文章
        // 注意：articleCount 是 selectAllWithCount 的派生字段，
        // 单条 selectById 不会填充它，因此这里直接用 Mapper 统计
        assertEquals(0, articleMapper.countByCategoryId(5L), "该分类下应没有文章");
        categoryService.delete(5L);
        assertNull(categoryService.getById(5L), "空分类应被删除");
    }

    // ── 文章：发布事务与标签关联 ────────────────────────────────────────

    @Test
    @DisplayName("发布文章时文章与标签关联在同一事务内写入")
    void publishArticleWritesTagsAtomically() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("事务测试文章");
        dto.setContent("## 正文\n这是一篇用于验证发布事务的测试文章。");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatus.PUBLISHED.getCode());
        dto.setTagIds(List.of(1L, 2L, 3L));

        Long id = articleService.create(dto);
        assertNotNull(id, "应回填自增主键");

        Article saved = articleMapper.selectById(id);
        assertEquals("事务测试文章", saved.getTitle());
        assertEquals(ArticleStatus.PUBLISHED.getCode(), saved.getStatus());
        assertEquals(0, saved.getViewCount());
        assertEquals(0, saved.getCommentCount());
        assertEquals("正文 这是一篇用于验证发布事务的测试文章。", saved.getSummary(),
                "未填摘要时应自动从正文截取");

        assertEquals(3, articleTagMapper.selectTagIdsByArticleId(id).size(),
                "标签关联应与文章同时写入");
    }

    @Test
    @DisplayName("重复的标签 ID 会被去重，不会撞唯一约束")
    void duplicateTagIdsDeduplicated() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("重复标签测试");
        dto.setContent("正文内容");
        dto.setCategoryId(1L);
        dto.setTagIds(List.of(1L, 1L, 2L, 2L));

        Long id = articleService.create(dto);
        assertEquals(2, articleTagMapper.selectTagIdsByArticleId(id).size(),
                "重复标签应被去重为 2 条");
    }

    @Test
    @DisplayName("草稿不出现在前台，出现在后台")
    void draftInvisibleOnFront() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("草稿测试文章");
        dto.setContent("草稿正文");
        dto.setCategoryId(1L);
        dto.setStatus(ArticleStatus.DRAFT.getCode());
        Long id = articleService.create(dto);

        assertThrows(BusinessException.class, () -> articleService.viewDetail(id),
                "草稿不应能通过前台详情接口访问");
    }

    @Test
    @DisplayName("编辑文章会整体替换标签关联")
    void updateReplacesTagSet() {
        ArticleDTO dto = new ArticleDTO();
        dto.setTitle("标签替换测试");
        dto.setContent("正文");
        dto.setCategoryId(1L);
        dto.setTagIds(List.of(1L, 2L, 3L));
        Long id = articleService.create(dto);

        // 自增主键是回填到 Article 实体上的，DTO 需要显式带上 ID 才表示「编辑」
        dto.setId(id);
        dto.setTagIds(List.of(4L));
        articleService.update(dto);

        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(id);
        assertEquals(1, tagIds.size(), "应只剩 1 个标签");
        assertEquals(4L, tagIds.get(0));
    }

    @Test
    @DisplayName("删除文章会连带清理其评论与标签关联")
    void deleteArticleCascades() {
        // 文章 1 有 4 条评论与 3 个标签关联
        articleService.delete(1L);
        assertEquals(null, articleMapper.selectById(1L));
        assertTrue(articleTagMapper.selectTagIdsByArticleId(1L).isEmpty(),
                "标签关联应被清理");
    }

    @Test
    @DisplayName("阅读量自增")
    void viewCountIncrements() {
        int before = articleMapper.selectById(2L).getViewCount();
        articleService.viewDetail(2L);
        assertEquals(before + 1, articleMapper.selectById(2L).getViewCount());
    }

    // ── 账号与密码 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("密码以加盐哈希存储，同一明文两次编码结果不同")
    void passwordIsSaltedHash() {
        String encoded = PasswordUtil.encode("123456");
        assertNotEquals("123456", encoded, "绝不能存明文");
        assertTrue(encoded.startsWith("pbkdf2$"), "应带算法标识，实际：" + encoded);
        assertTrue(PasswordUtil.matches("123456", encoded));
        assertFalse(PasswordUtil.matches("1234567", encoded));

        String again = PasswordUtil.encode("123456");
        assertNotEquals(encoded, again, "两次编码应因随机盐而不同");
    }

    @Test
    @DisplayName("管理员密码落库为哈希而非明文")
    void adminPasswordStoredHashed() {
        assertNotNull(adminService.login("admin", "123456", "test-key"),
                "默认管理员应能登录");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.login("admin", "wrong-password", "test-key-2"));
        assertEquals("用户名或密码错误", ex.getMessage(),
                "不应区分账号不存在与密码错误");
    }

    @Test
    @DisplayName("连续 5 次登录失败后锁定 60 秒")
    void adminLockedAfterFiveFailures() {
        String key = "lock-test-key";
        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class,
                    () -> adminService.login("admin", "bad-password", key));
        }
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminService.login("admin", "123456", key));
        assertTrue(ex.getMessage().contains("秒后重试"),
                "第 6 次即使密码正确也应被锁定，实际：" + ex.getMessage());
    }

    @Test
    @DisplayName("注册用户密码加盐哈希，且用户名不可重复")
    void registerUserAndRejectDuplicate() {
        cn.swust.bokesys.dto.UserRegisterDTO dto = new cn.swust.bokesys.dto.UserRegisterDTO();
        dto.setUsername("newbie");
        dto.setPassword("abc123");
        dto.setConfirmPassword("abc123");
        userService.register(dto);

        User created = userMapper.selectByUsername("newbie");
        assertNotNull(created);
        assertNotEquals("abc123", created.getPassword(), "不应存明文");
        assertTrue(created.getPassword().startsWith("pbkdf2$"));

        assertThrows(BusinessException.class, () -> userService.register(dto),
                "重复用户名应被拒绝");
    }

    @Test
    @DisplayName("两次密码不一致时注册失败")
    void registerRejectsMismatchedPassword() {
        cn.swust.bokesys.dto.UserRegisterDTO dto = new cn.swust.bokesys.dto.UserRegisterDTO();
        dto.setUsername("mismatch");
        dto.setPassword("abc123");
        dto.setConfirmPassword("abc124");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(dto));
        assertTrue(ex.getMessage().contains("不一致"));
    }

    @Test
    @DisplayName("被禁用的账号不能登录")
    void disabledUserCannotLogin() {
        User user = userMapper.selectByUsername("user");
        userMapper.updateStatus(user.getId(), UserStatus.DISABLED.getCode());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("user", "123456"));
        assertTrue(ex.getMessage().contains("禁用"),
                "应提示账号被禁用，实际：" + ex.getMessage());
    }

    // ── 仪表盘 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("仪表盘统计与库中数据一致，趋势补齐 7 天")
    void dashboardStatsConsistent() {
        DashboardVO vo = dashboardService.stats();
        assertEquals(8, vo.getArticleTotal());
        assertEquals(6, vo.getPublishedTotal());
        assertEquals(2, vo.getDraftTotal());
        assertEquals(11, vo.getCommentTotal());
        assertEquals(3, vo.getPendingCommentTotal());
        assertEquals(6, vo.getCategoryTotal());
        assertEquals(12, vo.getTagTotal());
        assertEquals(7, vo.getTrend().size(), "趋势应补齐为 7 天");
        assertTrue(vo.isHasPending(), "有待审核评论时提醒条应显示");
        assertFalse(vo.getCategoryStats().isEmpty());
    }

    @Test
    @DisplayName("分页参数会被规范化，越界页码不会报错")
    void paginationNormalized() {
        cn.swust.bokesys.dto.ArticleQueryDTO query = new cn.swust.bokesys.dto.ArticleQueryDTO();
        query.setPageNo(-5);
        query.setPageSize(1000);
        assertEquals(1, articleService.pageFront(query).getPageNo(),
                "非法页码应回落到第 1 页");
        assertEquals(100, articleService.pageFront(query).getPageSize(),
                "超大 pageSize 应被压到单页上限 100");

        query.setPageNo(1);
        query.setPageSize(3);
        assertEquals(6, articleService.pageFront(query).getTotal());
        assertEquals(3, articleService.pageFront(query).getList().size());
        assertEquals(2, articleService.pageFront(query).getPages());
    }

    @Test
    @DisplayName("空结果集不会抛异常")
    void emptyResultHandled() {
        assertEquals(0, commentService.listApprovedTree(999L).size());
        assertEquals(Collections.emptyList(),
                articleService.pageFront(keywordQuery("不存在的关键词xyz")).getList());
    }

    // ── 测试辅助 ────────────────────────────────────────────────────────

    /**
     * 把评论楼拍平成列表。
     *
     * <p>{@code listApprovedTree} 返回的是「根评论」，回复藏在 children 里，
     * 因此统计总条数必须递归展开，直接取 size() 得到的是根节点个数。
     */
    private List<CommentVO> flatten(List<CommentVO> tree) {
        List<CommentVO> all = new ArrayList<>();
        for (CommentVO node : tree) {
            all.add(node);
            all.addAll(flatten(node.getChildren()));
        }
        return all;
    }

    /** 某篇文章当前在前台可见（已通过审核）的评论总条数 */
    private int approvedCount(Long articleId) {
        return flatten(commentService.listApprovedTree(articleId)).size();
    }

    private cn.swust.bokesys.dto.ArticleQueryDTO keywordQuery(String keyword) {
        cn.swust.bokesys.dto.ArticleQueryDTO q = new cn.swust.bokesys.dto.ArticleQueryDTO();
        q.setKeyword(keyword);
        return q;
    }
}
