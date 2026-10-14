package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.constant.ArticleStatus;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.common.util.PageUtil;
import cn.swust.bokesys.common.util.TextUtil;
import cn.swust.bokesys.dto.ArticleDTO;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.ArticleTagMapper;
import cn.swust.bokesys.mapper.CommentMapper;
import cn.swust.bokesys.mapper.TagMapper;
import cn.swust.bokesys.po.Article;
import cn.swust.bokesys.po.ArticleTag;
import cn.swust.bokesys.service.ArticleService;
import cn.swust.bokesys.vo.ArticleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文章业务实现，是《概要设计》4.2.3「内容管理模块」的核心。
 */
@Service
public class ArticleServiceImpl implements ArticleService {

    /** 未填写摘要时自动截取的正文字数 */
    private static final int AUTO_SUMMARY_LENGTH = 150;

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper,
                              ArticleTagMapper articleTagMapper,
                              TagMapper tagMapper,
                              CommentMapper commentMapper) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public PageResult<ArticleVO> pageFront(ArticleQueryDTO query) {
        PageUtil.startPage(query.getPageNo(), query.getPageSize());
        // startPage 之后必须紧跟这条查询，中间不能插入其它数据库调用
        List<ArticleVO> list = articleMapper.selectFrontPage(query);
        return PageUtil.toResult(list);
    }

    @Override
    public PageResult<ArticleVO> pageAdmin(ArticleQueryDTO query) {
        PageUtil.startPage(query.getPageNo(), query.getPageSize());
        List<ArticleVO> list = articleMapper.selectAdminPage(query);
        return PageUtil.toResult(list);
    }

    @Override
    public PageResult<ArticleVO> pageByStatus(Integer status, Integer pageNo, Integer pageSize) {
        ArticleQueryDTO query = new ArticleQueryDTO();
        query.setStatus(status);
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        return pageAdmin(query);
    }

    @Override
    public ArticleVO getVoById(Long id) {
        ArticleVO vo = articleMapper.selectVoById(id);
        if (vo == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        return vo;
    }

    /**
     * 阅读量自增。
     *
     * <p>属性为草稿时不允许通过前台访问，因此这里额外挡一道，
     * 避免用猜 ID 的方式预览未发布的草稿。
     */
    @Override
    @Transactional
    public ArticleVO viewDetail(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        if (!ArticleStatus.PUBLISHED.matches(article.getStatus())) {
            throw new BusinessException("该文章尚未发布");
        }
        articleMapper.incrementViewCount(id);
        return getVoById(id);
    }

    @Override
    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    /**
     * 新建文章。
     *
     * <p>事务边界对应《概要设计》3.3：「文章发布时需要同时写入文章表、
     * 更新分类计数表并写入文章-标签关联表，任一环节失败则整体回滚」。
     *
     * <p>关于「更新分类计数」：文档 5.2.4 的分类表字段清单里并没有计数字段，
     * 为保持脚本与文档一致，本项目把分类文章数做成 LEFT JOIN 派生查询
     * （见 CategoryMapper.selectAllWithCount），因此无需在此维护计数，
     * 也就不存在计数漂移。事务内实际保证的是「文章 + 标签关联」的原子性。
     */
    @Override
    @Transactional
    public Long create(ArticleDTO dto) {
        validate(dto);
        Article article = new Article();
        article.setTitle(dto.getTitle().trim());
        article.setContent(dto.getContent());
        article.setSummary(resolveSummary(dto));
        article.setCoverImage(dto.getCoverImage());
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus() == null
                ? ArticleStatus.DRAFT.getCode() : dto.getStatus());

        LocalDateTime now = LocalDateTime.now();
        article.setCreateTime(now);
        article.setUpdateTime(now);

        // useGeneratedKeys 会把自增主键回填到 article.id
        articleMapper.insert(article);
        replaceTags(article.getId(), dto.getTagIds());
        return article.getId();
    }

    @Override
    @Transactional
    public void update(ArticleDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少文章 ID");
        }
        Article existing = articleMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        validate(dto);

        Article article = new Article();
        article.setId(dto.getId());
        article.setTitle(dto.getTitle().trim());
        article.setContent(dto.getContent());
        article.setSummary(resolveSummary(dto));
        article.setCoverImage(dto.getCoverImage());
        article.setCategoryId(dto.getCategoryId());
        article.setStatus(dto.getStatus() == null
                ? existing.getStatus() : dto.getStatus());
        article.setUpdateTime(LocalDateTime.now());

        articleMapper.update(article);
        // 标签采用「先清后插」整体替换，避免逐条 diff 带来的复杂度
        replaceTags(dto.getId(), dto.getTagIds());
    }

    /**
     * 删除文章。
     *
     * <p>t_comment 与 t_article_tag 都对 t_article 建有外键，
     * 因此必须先删子表再删主表，全部在同一事务内完成。
     * 对应《概要设计》6.2.4 的提示语「删除后该文章及其评论将不可恢复」。
     *
     * <p>删评论这一步不能直接一条 DELETE 了事：t_comment 的 parent_id 是自关联外键，
     * 一条语句删整篇文章的评论时，数据库会在删掉父评论的瞬间发现子评论仍引用它而报错。
     * 所以先把这些评论的 parent_id 统一置空，再整体删除。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Article existing = articleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("文章不存在或已被删除");
        }
        commentMapper.clearParentIdByArticleId(id);
        commentMapper.deleteByArticleId(id);
        articleTagMapper.deleteByArticleId(id);
        articleMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请先选择要删除的文章");
        }
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    public Article getPrev(Long id) {
        Article current = articleMapper.selectById(id);
        if (current == null || current.getCreateTime() == null) {
            return null;
        }
        return articleMapper.selectPrev(id, current.getCreateTime());
    }

    @Override
    public Article getNext(Long id) {
        Article current = articleMapper.selectById(id);
        if (current == null || current.getCreateTime() == null) {
            return null;
        }
        return articleMapper.selectNext(id, current.getCreateTime());
    }

    @Override
    public List<Article> listRecent(int limit) {
        return articleMapper.selectRecent(limit <= 0 ? 5 : limit);
    }

    @Override
    public long countAll() {
        return articleMapper.countAll();
    }

    @Override
    public long countByStatus(Integer status) {
        return articleMapper.countByStatus(status);
    }

    @Override
    public long sumViewCount() {
        return articleMapper.sumViewCount();
    }

    // ── 内部工具 ────────────────────────────────────────────────────────

    private void validate(ArticleDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("文章标题不能为空");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new BusinessException("文章正文不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BusinessException("请选择文章分类");
        }
    }

    /** 摘要留空时，自动从正文截取一段，保证列表页不会出现空白摘要 */
    private String resolveSummary(ArticleDTO dto) {
        if (dto.getSummary() != null && !dto.getSummary().trim().isEmpty()) {
            return dto.getSummary().trim();
        }
        return TextUtil.summarize(dto.getContent(), AUTO_SUMMARY_LENGTH);
    }

    /**
     * 整体替换文章的标签关联。
     *
     * <p>先去重再插入：(article_id, tag_id) 上有唯一约束，
     * 若前端重复提交同一个标签会直接撞约束报错。
     */
    private void replaceTags(Long articleId, List<Long> tagIds) {
        articleTagMapper.deleteByArticleId(articleId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<ArticleTag> relations = new ArrayList<>();
        List<Long> seen = new ArrayList<>();
        for (Long tagId : tagIds) {
            if (tagId == null || seen.contains(tagId)) {
                continue;
            }
            // 顺带过滤掉已被删除的标签，避免外键报错
            if (tagMapper.selectById(tagId) == null) {
                continue;
            }
            seen.add(tagId);
            relations.add(new ArticleTag(articleId, tagId));
        }
        if (!relations.isEmpty()) {
            articleTagMapper.batchInsert(relations);
        }
    }
}
