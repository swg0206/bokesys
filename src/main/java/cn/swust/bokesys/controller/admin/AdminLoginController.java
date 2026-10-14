package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.common.util.CaptchaUtil;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.po.Admin;
import cn.swust.bokesys.service.AdminService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

/**
 * 后台登录，对应 API-09 {@code POST /admin/login} 与 API-10 {@code GET /admin/logout}
 * （Fun_07 管理员登录）。
 *
 * <p>界面细节对应《概要设计》6.2.2：验证码可点击刷新、输入框失焦校验、
 * 登录失败在表单顶部给出统一错误提示且不指明具体哪一项错误、连续 5 次失败锁定 60 秒。
 */
@Controller
public class AdminLoginController extends BaseController {

    private final AdminService adminService;

    public AdminLoginController(ConfigService configService,
                                CategoryService categoryService,
                                TagService tagService,
                                LinkService linkService,
                                AdminService adminService) {
        super(configService, categoryService, tagService, linkService);
        this.adminService = adminService;
    }

    @GetMapping("/admin/login")
    public String loginPage(Model model) {
        return "admin/login";
    }

    /**
     * 生成图形验证码。
     *
     * <p>答案写入 Session 供提交时比对；每次请求都刷新，因此点击图片即可换一张。
     * 该路径在 WebMvcConfig 中被明确放行，否则会被登录拦截器挡掉。
     */
    @GetMapping(value = "/admin/captcha", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] captcha(HttpSession session, HttpServletResponse response) throws IOException {
        CaptchaUtil.Captcha captcha = CaptchaUtil.generate();
        session.setAttribute(SessionConstant.CAPTCHA_CODE, captcha.code());
        // 验证码不能被缓存，否则点击刷新拿到的还是同一张图
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        return CaptchaUtil.toPngBytes(captcha.image());
    }

    @PostMapping("/admin/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "captcha", required = false) String captcha,
                        HttpServletRequest request,
                        HttpSession session,
                        RedirectAttributes ra) {
        // 验证码校验：比对后立即从 Session 移除，保证一次一用
        Object expected = session.getAttribute(SessionConstant.CAPTCHA_CODE);
        session.removeAttribute(SessionConstant.CAPTCHA_CODE);
        if (expected == null) {
            ra.addFlashAttribute("error", "验证码已过期，请点击图片刷新后重试");
            return "redirect:/admin/login";
        }
        if (captcha == null || !expected.toString().equalsIgnoreCase(captcha.trim())) {
            ra.addFlashAttribute("error", "验证码不正确");
            ra.addFlashAttribute("username", username);
            return "redirect:/admin/login";
        }

        try {
            Admin admin = adminService.login(username, password, clientKey(request, username));
            session.setAttribute(SessionConstant.LOGIN_ADMIN, admin);
            return "redirect:/admin/dashboard";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("username", username);
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/admin/logout")
    public String logout(HttpSession session, RedirectAttributes ra) {
        session.removeAttribute(SessionConstant.LOGIN_ADMIN);
        ra.addFlashAttribute("message", "已安全退出");
        return "redirect:/admin/login";
    }

    /** 后台首页跳转到仪表盘，省掉手输路径 */
    @GetMapping("/admin")
    public String index() {
        return "redirect:/admin/dashboard";
    }
}
