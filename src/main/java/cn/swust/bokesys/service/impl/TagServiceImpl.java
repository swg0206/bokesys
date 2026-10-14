package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.mapper.ArticleTagMapper;
import cn.swust.bokesys.mapper.TagMapper;
import cn.swust.bokesys.po.Tag;
import cn.swust.bokesys.service.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签业务实现，对应《概要设计》4.2.3 中「标签维护」部分。
 */
@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    public TagServiceImpl(TagMapper tagMapper, ArticleTagMapper articleTagMapper) {
        this.tagMapper = tagMapper;
        this.articleTagMapper = articleTagMapper;
    }

    @Override
    public List<Tag> listAllWithCount() {
        return tagMapper.selectAllWithCount();
    }

    @Override
    public List<Tag> listHot(int limit) {
        return tagMapper.selectHotTags(limit <= 0 ? 10 : limit);
    }

    @Override
    public List<Tag> listByArticleId(Long articleId) {
        return tagMapper.selectByArticleId(articleId);
    }

    @Override
    public Tag getById(Long id) {
        return tagMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(Tag tag) {
        String name = tag.getName() == null ? "" : tag.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("标签名称不能为空");
        }
        if (tagMapper.selectByName(name) != null) {
            throw new BusinessException("标签「" + name + "」已存在");
        }
        tag.setName(name);
        tag.setCreateTime(LocalDateTime.now());
        tagMapper.insert(tag);
    }

    @Override
    @Transactional
    public void update(Tag tag) {
        if (tag.getId() == null) {
            throw new BusinessException("缺少标签 ID");
        }
        Tag existing = tagMapper.selectById(tag.getId());
        if (existing == null) {
            throw new BusinessException("标签不存在或已被删除");
        }
        String name = tag.getName() == null ? "" : tag.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("标签名称不能为空");
        }
        Tag sameName = tagMapper.selectByName(name);
        if (sameName != null && !sameName.getId().equals(tag.getId())) {
            throw new BusinessException("标签「" + name + "」已存在");
        }
        tag.setName(name);
        tagMapper.update(tag);
    }

    /**
     * 删除标签。
     *
     * <p>关联表 t_article_tag 对 tag_id 有外键约束，因此必须先清理关联再删除本体，
     * 两步放在同一事务里，避免删除到一半留下脏关联。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Tag existing = tagMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("标签不存在或已被删除");
        }
        articleTagMapper.deleteByTagId(id);
        tagMapper.deleteById(id);
    }

    @Override
    public long countAll() {
        return tagMapper.countAll();
    }
}
