package cn.swust.bokesys.controller.base;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.po.Admin;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 后台控制器基类，对应《概要设计》3.2：
 * 提供当前登录管理员信息的获取。
 *
 * <p>通过 {@code @ModelAttribute} 把当前管理员注入每个后台页面的模型，
 * 供侧边栏顶部的头像与下拉菜单使用；同时提供需要登录态时的取用方法。
 */
public abstract class AdminBaseController extends BaseController {

    protected AdminBaseController(ConfigService configService,
                                  CategoryService categoryService,
                                  TagService tagService,
                                  LinkService linkService) {
        super(configService, categoryService, tagService, linkService);
    }

    /**
     * 把当前登录管理员放进模型，名为 {@code currentAdmin}。
     *
     * <p>能走到这里说明 LoginInterceptor 已放行，正常不会为 null；
     * 但登录页等被排除的路径也可能命中，所以调用方仍需容忍 null。
     */
    @ModelAttribute("currentAdmin")
    public Admin currentAdmin(HttpSession session) {
        Object admin = session == null ? null : session.getAttribute(SessionConstant.LOGIN_ADMIN);
        return admin instanceof Admin ? (Admin) admin : null;
    }

    /** 取当前登录管理员；未登录直接抛 401，用于必须要有身份的写操作 */
    protected Admin requireAdmin(HttpSession session) {
        Object admin = session == null ? null : session.getAttribute(SessionConstant.LOGIN_ADMIN);
        if (admin instanceof Admin) {
            return (Admin) admin;
        }
        throw new BusinessException(Result.CODE_UNAUTHORIZED, "登录状态已失效，请重新登录");
    }
}
