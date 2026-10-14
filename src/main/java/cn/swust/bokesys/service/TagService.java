package cn.swust.bokesys.service;

import cn.swust.bokesys.po.Tag;

import java.util.List;

/**
 * 标签业务接口，对应《概要设计》3.3。
 */
public interface TagService {

    /** 全部标签，附带引用文章数 */
    List<Tag> listAllWithCount();

    /** 热门标签，按引用文章数倒序取前 N，供首页标签云 */
    List<Tag> listHot(int limit);

    List<Tag> listByArticleId(Long articleId);

    Tag getById(Long id);

    void create(Tag tag);

    void update(Tag tag);

    /** 删除标签：先清理文章标签关联，再删除标签本体 */
    void delete(Long id);

    long countAll();
}
