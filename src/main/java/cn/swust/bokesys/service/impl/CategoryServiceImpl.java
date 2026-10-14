package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.mapper.ArticleMapper;
import cn.swust.bokesys.mapper.CategoryMapper;
import cn.swust.bokesys.po.Category;
import cn.swust.bokesys.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务实现，承载《概要设计》4.2 「分类管理模块」的业务规则。
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    public CategoryServiceImpl(CategoryMapper categoryMapper, ArticleMapper articleMapper) {
        this.categoryMapper = categoryMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    public List<Category> listAllWithCount() {
        return categoryMapper.selectAllWithCount();
    }

    @Override
    public List<Category> listPublishedWithCount() {
        return categoryMapper.selectPublishedWithCount();
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(Category category) {
        String name = trim(category.getName());
        if (name.isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        if (categoryMapper.selectByName(name) != null) {
            throw new BusinessException("分类「" + name + "」已存在");
        }
        category.setName(name);
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        LocalDateTime now = LocalDateTime.now();
        category.setCreateTime(now);
        category.setUpdateTime(now);
        categoryMapper.insert(category);
    }

    @Override
    @Transactional
    public void update(Category category) {
        if (category.getId() == null) {
            throw new BusinessException("缺少分类 ID");
        }
        Category existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new BusinessException("分类不存在或已被删除");
        }
        String name = trim(category.getName());
        if (name.isEmpty()) {
            throw new BusinessException("分类名称不能为空");
        }
        Category sameName = categoryMapper.selectByName(name);
        if (sameName != null && !sameName.getId().equals(category.getId())) {
            throw new BusinessException("分类「" + name + "」已存在");
        }
        category.setName(name);
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.update(category);
    }

    /**
     * 删除分类前先校验占用情况。
     *
     * <p>这是《概要设计》4.2.3 明确要求的规则：
     * 「删除分类时若该分类下存在文章，系统会提示先将文章转移到其他分类或解除关联」。
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Category existing = categoryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("分类不存在或已被删除");
        }
        long articleCount = articleMapper.countByCategoryId(id);
        if (articleCount > 0) {
            throw new BusinessException(
                    "该分类下存在 " + articleCount + " 篇文章，请先转移或解除关联");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public long countAll() {
        return categoryMapper.countAll();
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
