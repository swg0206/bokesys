package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.mapper.LinkMapper;
import cn.swust.bokesys.po.Link;
import cn.swust.bokesys.service.LinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 友情链接业务实现，对应《概要设计》4.2.4「站点运营模块」中的友链管理。
 */
@Service
public class LinkServiceImpl implements LinkService {

    private final LinkMapper linkMapper;

    public LinkServiceImpl(LinkMapper linkMapper) {
        this.linkMapper = linkMapper;
    }

    @Override
    public List<Link> listVisible() {
        return linkMapper.selectVisible();
    }

    @Override
    public List<Link> listAll() {
        return linkMapper.selectAll();
    }

    @Override
    public Link getById(Long id) {
        return linkMapper.selectById(id);
    }

    @Override
    @Transactional
    public void create(Link link) {
        validate(link);
        if (link.getSortOrder() == null) {
            link.setSortOrder(0);
        }
        if (link.getStatus() == null) {
            link.setStatus(1);
        }
        link.setCreateTime(LocalDateTime.now());
        linkMapper.insert(link);
    }

    @Override
    @Transactional
    public void update(Link link) {
        if (link.getId() == null) {
            throw new BusinessException("缺少友链 ID");
        }
        if (linkMapper.selectById(link.getId()) == null) {
            throw new BusinessException("友链不存在或已被删除");
        }
        validate(link);
        linkMapper.update(link);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (linkMapper.selectById(id) == null) {
            throw new BusinessException("友链不存在或已被删除");
        }
        linkMapper.deleteById(id);
    }

    @Override
    public long countAll() {
        return linkMapper.countAll();
    }

    private void validate(Link link) {
        if (link.getName() == null || link.getName().trim().isEmpty()) {
            throw new BusinessException("链接名称不能为空");
        }
        if (link.getUrl() == null || link.getUrl().trim().isEmpty()) {
            throw new BusinessException("链接地址不能为空");
        }
        link.setName(link.getName().trim());
        link.setUrl(link.getUrl().trim());
    }
}
