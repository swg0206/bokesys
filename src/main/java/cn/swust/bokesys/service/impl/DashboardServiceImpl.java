package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.constant.CommentStatus;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.CategoryMapper;
import cn.swust.bokesys.mapper.CommentMapper;
import cn.swust.bokesys.mapper.TagMapper;
import cn.swust.bokesys.service.DashboardService;
import cn.swust.bokesys.vo.DashboardVO;
import cn.swust.bokesys.vo.TrendPointVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘业务实现，对应《概要设计》4.2.4「站点运营模块」的仪表盘部分。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    /** 趋势图统计的天数 */
    private static final int TREND_DAYS = 7;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public DashboardServiceImpl(ArticleMapper articleMapper,
                                CommentMapper commentMapper,
                                CategoryMapper categoryMapper,
                                TagMapper tagMapper) {
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public DashboardVO stats() {
        DashboardVO vo = new DashboardVO();
        vo.setArticleTotal(articleMapper.countAll());
        vo.setPublishedTotal(articleMapper.countByStatus(ArticleStatus.PUBLISHED.getCode()));
        vo.setDraftTotal(articleMapper.countByStatus(ArticleStatus.DRAFT.getCode()));
        vo.setCommentTotal(commentMapper.countAll());
        vo.setPendingCommentTotal(commentMapper.countByStatus(CommentStatus.PENDING.getCode()));
        vo.setCategoryTotal(categoryMapper.countAll());
        vo.setTagTotal(tagMapper.countAll());
        vo.setViewTotal(articleMapper.sumViewCount());

        vo.setRecentArticles(articleMapper.selectRecent(5));
        vo.setTrend(buildTrend());
        vo.setCategoryStats(articleMapper.selectCategoryStats());
        return vo;
    }

    /**
     * 近 7 天文章发布趋势。
     *
     * <p>数据库只返回「有文章的日期」，这里在 Java 侧把缺失的日期补成 0，
     * 否则趋势图会出现断档。日期格式化也放在 Java 侧 ——
     * H2 没有 MySQL 的 DATE_FORMAT 函数，放这里才能两库通用。
     */
    private List<TrendPointVO> buildTrend() {
        LocalDate start = LocalDate.now().minusDays(TREND_DAYS - 1L);
        List<TrendPointVO> raw = articleMapper.selectTrendSince(start.atStartOfDay());

        Map<String, Integer> byDate = new HashMap<>();
        for (TrendPointVO point : raw) {
            if (point.getStatDate() != null) {
                byDate.put(point.getStatDate(), point.getArticleCount());
            }
        }

        List<TrendPointVO> filled = new ArrayList<>(TREND_DAYS);
        for (int i = 0; i < TREND_DAYS; i++) {
            String day = start.plusDays(i).format(DAY);
            filled.add(new TrendPointVO(day, byDate.getOrDefault(day, 0)));
        }
        return filled;
    }
}
