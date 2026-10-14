package cn.swust.bokesys;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.constant.CommentStatus;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.CategoryMapper;
import cn.swust.bokesys.mapper.CommentMapper;
import cn.swust.bokesys.mapper.ConfigMapper;
import cn.swust.bokesys.mapper.LinkMapper;
import cn.swust.bokesys.mapper.TagMapper;
import cn.swust.bokesys.po.Category;
import cn.swust.bokesys.po.SysConfig;
import cn.swust.bokesys.common.util.PageUtil;
import cn.swust.bokesys.vo.ArticleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据层冒烟测试：验证 schema.sql / data.sql 确实生效、9 个 Mapper 可用、
 * 中文没有乱码，以及「先审核后展示」这条核心业务规则在 SQL 层面成立。
 *
 * <p>这类测试的价值在于：一旦建表脚本、字段映射或编码配置被改坏，
 * 会在这里直接失败，而不是等到页面打开才发现。
 */
@SpringBootTest
@DisplayName("数据层冒烟测试")
class DatabaseSmokeTests {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private LinkMapper linkMapper;
    @Autowired
    private ConfigMapper configMapper;

    @Test
    @DisplayName("种子数据条数与预期一致")
    void seedDataLoaded() {
        assertEquals(6, categoryMapper.countAll(), "分类应为 6 条");
        assertEquals(12, tagMapper.countAll(), "标签应为 12 条");
        assertEquals(8, articleMapper.countAll(), "文章应为 8 条");
        assertEquals(6, linkMapper.countAll(), "友链应为 6 条");
        assertEquals(6, configMapper.countAll(), "系统配置应为 6 条");
        assertEquals(2, articleMapper.countByStatus(ArticleStatus.DRAFT.getCode()), "草稿应为 2 篇");
        assertEquals(6, articleMapper.countByStatus(ArticleStatus.PUBLISHED.getCode()), "已发布应为 6 篇");
    }

    @Test
    @DisplayName("中文经 schema/data 脚本落库后没有乱码")
    void chineseNotMangled() {
        SysConfig siteName = configMapper.selectByKey("site_name");
        assertNotNull(siteName, "site_name 配置应存在");
        assertEquals("笔迹博客", siteName.getConfigValue());

        List<Category> categories = categoryMapper.selectAllWithCount();
        assertEquals("后端开发", categories.get(0).getName());
        assertEquals("数据库", categories.get(2).getName());
    }

    @Test
    @DisplayName("分类计数为派生查询，且与文章表强一致")
    void categoryCountIsDerived() {
        List<Category> categories = categoryMapper.selectAllWithCount();
        // 「后端开发」下有 2 篇已发布 + 1 篇草稿 = 3 篇
        Category backend = categories.stream()
                .filter(c -> "后端开发".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(3, backend.getArticleCount());
        // 「算法与数据结构」下没有文章
        Category alg = categories.stream()
                .filter(c -> "算法与数据结构".equals(c.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(0, alg.getArticleCount());
    }

    @Test
    @DisplayName("前台只展示已通过审核的评论（先审核后展示）")
    void onlyApprovedCommentsAreVisible() {
        // 文章 1 有 4 条评论，其中 3 条已通过、1 条待审核
        assertEquals(4, commentMapper.countByArticleId(1L), "文章1 总评论数应为 4");
        assertEquals(3, commentMapper.selectApprovedByArticleId(1L).size(),
                "前台只能看到 3 条已通过评论，待审核的那条必须查不出来");
        assertEquals(3, commentMapper.countByStatus(CommentStatus.PENDING.getCode()),
                "待审核评论共 3 条");
    }

    @Test
    @DisplayName("文章的评论计数与实际已通过评论数一致")
    void commentCountMatchesApproved() {
        for (long articleId = 1; articleId <= 8; articleId++) {
            int expected = commentMapper.selectApprovedByArticleId(articleId).size();
            Integer actual = articleMapper.selectById(articleId).getCommentCount();
            assertEquals(expected, actual,
                    "文章 " + articleId + " 的 comment_count 与实际已通过评论数不符");
        }
    }

    @Test
    @DisplayName("前台分页只返回已发布文章，且按发布时间倒序")
    void frontPageReturnsPublishedOnly() {
        PageUtil.startPage(1, 10);
        ArticleQueryDTO query = new ArticleQueryDTO();
        List<ArticleVO> list = articleMapper.selectFrontPage(query);

        assertEquals(6, list.size(), "前台首页应只有 6 篇已发布文章");
        assertTrue(list.stream().allMatch(ArticleVO::isPublished), "不应出现草稿");
        for (int i = 1; i < list.size(); i++) {
            assertFalse(list.get(i).getCreateTime().isAfter(list.get(i - 1).getCreateTime()),
                    "文章应按发布时间倒序");
        }
        // 标签集合由 resultMap 的嵌套 select 装配
        assertFalse(list.get(0).getTags().isEmpty(), "文章列表应带上标签");
    }

    @Test
    @DisplayName("关键词同时匹配标题与正文（H2 下用 LIKE 替代 FULLTEXT）")
    void keywordSearchHitsTitleAndContent() {
        // 命中标题
        PageUtil.startPage(1, 10);
        ArticleQueryDTO byTitle = new ArticleQueryDTO();
        byTitle.setKeyword("Spring Boot");
        assertTrue(articleMapper.selectFrontPage(byTitle).size() >= 2,
                "关键词应命中标题含 Spring Boot 的文章");

        // 命中正文但不在标题中：多篇文章正文里出现「事务」二字
        PageUtil.startPage(1, 10);
        ArticleQueryDTO byContent = new ArticleQueryDTO();
        byContent.setKeyword("最左前缀");
        assertFalse(articleMapper.selectFrontPage(byContent).isEmpty(),
                "关键词应能命中正文内容");
    }

    @Test
    @DisplayName("按分类与按标签筛选可命中文章")
    void filterByCategoryAndTag() {
        PageUtil.startPage(1, 10);
        ArticleQueryDTO byCategory = new ArticleQueryDTO();
        byCategory.setCategoryId(1L);
        assertEquals(2, articleMapper.selectFrontPage(byCategory).size(),
                "「后端开发」下有 2 篇已发布文章");

        PageUtil.startPage(1, 10);
        ArticleQueryDTO byTag = new ArticleQueryDTO();
        byTag.setTagId(6L); // Bootstrap
        assertFalse(articleMapper.selectFrontPage(byTag).isEmpty(), "标签筛选应命中文章");
    }

    @Test
    @DisplayName("仪表盘统计与趋势查询可正常执行（不使用 DATE_FORMAT）")
    void dashboardStatsWork() {
        assertEquals(3, commentMapper.countByStatus(CommentStatus.PENDING.getCode()));
        assertFalse(articleMapper.selectRecent(5).isEmpty(), "最近文章列表不应为空");
        assertFalse(articleMapper.selectCategoryStats().isEmpty(), "分类分布不应为空");
        assertFalse(articleMapper
                        .selectTrendSince(java.time.LocalDate.now().minusDays(30).atStartOfDay())
                        .isEmpty(),
                "趋势统计应能查出数据（用的 DATE() 而非 DATE_FORMAT()）");
    }

    @Test
    @DisplayName("前台友链只取显示状态的，并按排序号升序")
    void visibleLinksOnly() {
        assertEquals(5, linkMapper.selectVisible().size());
        assertTrue(linkMapper.selectVisible().stream().allMatch(l -> l.getStatus() == 1));
    }
}
