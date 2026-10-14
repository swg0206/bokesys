package cn.swust.bokesys.service;

import cn.swust.bokesys.po.Category;

import java.util.List;

/**
 * 分类业务接口，对应《概要设计》3.3 业务逻辑层包结构表。
 *
 * <p>控制层只依赖本接口，实现类位于 {@code service.impl} 包下。
 */
public interface CategoryService {

    /** 全部分类（含各分类文章数，含草稿），后台管理用 */
    List<Category> listAllWithCount();

    /** 全部分类（仅统计已发布文章），前台侧栏导航用 */
    List<Category> listPublishedWithCount();

    Category getById(Long id);

    void create(Category category);

    void update(Category category);

    /**
     * 删除分类。
     *
     * <p>业务规则：删除前必须校验该分类下是否存在文章，
     * 存在则抛出业务异常提示先转移或解除关联。
     */
    void delete(Long id);

    long countAll();
}
