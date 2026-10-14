package cn.swust.bokesys.controller.base;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 所有控制器的基类，对应《概要设计》3.2：
 * 提供统一返回体与公共模型数据封装。
 *
 * <p>这里只放「前台公共区块」的数据装配与少量请求辅助方法；
 * 后台需要的登录管理员信息由 {@link AdminBaseController} 追加。
 */
public abstract class BaseController {

    /** 侧栏「热门标签」展示的标签个数 */
    private static final int HOT_TAG_LIMIT = 15;

    protected final ConfigService configService;
    protected final CategoryService categoryService;
    protected final TagService tagService;
    protected final LinkService linkService;

    protected BaseController(ConfigService configService,
                             CategoryService categoryService,
                             TagService tagService,
                             LinkService linkService) {
        this.configService = configService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.linkService = linkService;
    }

    /**
     * 填充前台每个页面都要用的公共区块数据。
     *
     * <p>对应《概要设计》6.1 的右侧功能侧边栏：
     * 「关于站点」「文章分类」「热门标签」「友情链接」四个区块，
     * 以及页脚需要读取的站点名称与备案信息（来自 t_config，对应 Fun_14）。
     */
    protected void fillFrontCommonModel(Model model) {
        model.addAttribute("site", configService.loadAllAsMap());
        model.addAttribute("sidebarCategories", categoryService.listPublishedWithCount());
        model.addAttribute("sidebarTags", tagService.listHot(HOT_TAG_LIMIT));
        model.addAttribute("sidebarLinks", linkService.listVisible());
    }

    /** 是否为异步请求，决定异常与返回值走 JSON 还是页面 */
    protected boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /** 当前登录的注册用户，未登录返回 null */
    protected User currentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(SessionConstant.LOGIN_USER);
        return user instanceof User ? (User) user : null;
    }

    /**
     * 后台登录限流用的客户端标识：账号 + 来源 IP。
     * 对应《概要设计》6.2.2「连续 5 次失败后锁定 60 秒」。
     */
    protected String clientKey(HttpServletRequest request, String username) {
        String ip = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            int comma = forwarded.indexOf(',');
            ip = (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return (username == null ? "" : username.trim()) + "@" + ip;
    }

    /** 解析页码参数，非法值一律回落到第 1 页 */
    protected int pageNo(Integer pageNo) {
        return (pageNo == null || pageNo < 1) ? 1 : pageNo;
    }

    /**
     * 拼接「不含 pageNo 的分页链接前缀」，交给分页片段自行追加页码。
     *
     * <p>传入成对的 (键, 值)，值为 null 或空串的会被跳过，
     * 键值都做 URL 编码 —— 否则标题里的空格、中文或 & 会把链接拼坏。
     *
     * @param path      列表页路径，例如 {@code /admin/articles}
     * @param keyValues 成对的查询参数，例如 {@code "title", title, "status", status}
     */
    protected String pageBase(String path, Object... keyValues) {
        StringBuilder sb = new StringBuilder(path).append('?');
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key == null || value == null) {
                continue;
            }
            String text = String.valueOf(value);
            if (text.isEmpty()) {
                continue;
            }
            sb.append(URLEncoder.encode(String.valueOf(key), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(text, StandardCharsets.UTF_8))
                    .append('&');
        }
        return sb.toString();
    }
}
