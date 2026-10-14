package cn.swust.bokesys.service;

import cn.swust.bokesys.common.result.PageResult;
import cn.swust.bokesys.dto.ArticleDTO;
import cn.swust.bokesys.dto.ArticleQueryDTO;
import cn.swust.bokesys.po.Article;
import cn.swust.bokesys.vo.ArticleVO;

import java.util.List;

/**
 * 文章业务接口，对应《概要设计》3.3：
 * 文章发布、编辑、删除、草稿保存、分页查询、关键词模糊搜索、阅读量自增；
 * 发布时维护分类计数与标签关联。
 */
public interface ArticleService {

    /** 前台文章分页：只含已发布文章 */
    PageResult<ArticleVO> pageFront(ArticleQueryDTO query);

    /** 后台文章分页：不限状态，支持多条件筛选 */
    PageResult<ArticleVO> pageAdmin(ArticleQueryDTO query);

    /** 按 ID 取展示对象（含分类名与标签） */
    ArticleVO getVoById(Long id);

    /**
     * 文章详情：阅读量自增后返回展示对象。
     * 对应接口 API-02「浏览量自动加一」。
     */
    ArticleVO viewDetail(Long id);

    Article getById(Long id);

    /** 上一篇已发布文章，没有则返回 null（详情页的「上一篇」链接） */
    Article getPrev(Long id);

    /** 下一篇已发布文章，没有则返回 null */
    Article getNext(Long id);

    /** 新建文章（发布或存草稿），返回新文章 ID */
    Long create(ArticleDTO dto);

    /** 编辑文章，并整体替换其标签关联 */
    void update(ArticleDTO dto);

    /** 删除文章：连带清理其评论与标签关联 */
    void delete(Long id);

    void deleteBatch(List<Long> ids);

    /** 草稿箱 / 已发布筛选 */
    PageResult<ArticleVO> pageByStatus(Integer status, Integer pageNo, Integer pageSize);

    List<Article> listRecent(int limit);

    long countAll();

    long countByStatus(Integer status);

    long sumViewCount();
}
