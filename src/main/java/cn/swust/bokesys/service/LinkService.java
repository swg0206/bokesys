package cn.swust.bokesys.service;

import cn.swust.bokesys.po.Link;

import java.util.List;

/**
 * 友情链接业务接口，对应《概要设计》3.3 与接口 API-08 / API-20。
 */
public interface LinkService {

    /** 前台可见友链（status=1，按排序号升序） */
    List<Link> listVisible();

    /** 后台全部友链 */
    List<Link> listAll();

    Link getById(Long id);

    void create(Link link);

    void update(Link link);

    void delete(Long id);

    long countAll();
}
