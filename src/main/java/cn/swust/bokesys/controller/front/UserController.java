package cn.swust.bokesys.controller.front;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.BaseController;
import cn.swust.bokesys.dto.UserRegisterDTO;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import cn.swust.bokesys.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 注册用户接口，对应 API-23 {@code POST /user/register} 与 API-24 {@code POST /user/login}
 * （Fun_16）。
 *
 * <p>登录/注册采用「表单 POST + 重定向」的传统流程，配合页面内联的错误提示；
 * 这样即使浏览器禁用了 JS 也能完成注册登录。
 */
@Controller
@RequestMapping("/user")
public class UserController extends BaseController {

    private final UserService userService;

    public UserController(ConfigService configService,
                          CategoryService categoryService,
                          TagService tagService,
                          LinkService linkService,
                          UserService userService) {
        super(configService, categoryService, tagService, linkService);
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect,
                            Model model) {
        model.addAttribute("redirect", redirect);
        model.addAttribute("pageTitle", "用户登录");
        fillFrontCommonModel(model);
        return "front/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "redirect", required = false) String redirect,
                        HttpSession session,
                        RedirectAttributes ra) {
        try {
            User user = userService.login(username, password);
            session.setAttribute(SessionConstant.LOGIN_USER, user);
            return "redirect:" + safeRedirect(redirect);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("username", username);
            return "redirect:/user/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "用户注册");
        fillFrontCommonModel(model);
        return "front/register";
    }

    @PostMapping("/register")
    public String register(@Valid UserRegisterDTO dto, HttpSession session, RedirectAttributes ra) {
        try {
            userService.register(dto);
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addFlashAttribute("username", dto.getUsername());
            ra.addFlashAttribute("nickname", dto.getNickname());
            ra.addFlashAttribute("email", dto.getEmail());
            return "redirect:/user/register";
        }
        // 注册成功后直接登录，省掉一次手动输入
        session.setAttribute(SessionConstant.LOGIN_USER, userService.login(
                dto.getUsername().trim(), dto.getPassword()));
        ra.addFlashAttribute("message", "注册成功，已自动登录");
        return "redirect:/";
    }

    /**
     * 退出登录。
     *
     * <p>只清理注册用户身份，不影响同一浏览器里已登录的后台管理员会话。
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SessionConstant.LOGIN_USER);
        return "redirect:/";
    }

    /**
     * 当前登录状态，供页面用 fetch 判断是否展示评论输入框。
     */
    @GetMapping("/status")
    @ResponseBody
    public Result<User> status(HttpSession session) {
        return Result.ok(currentUser(session));
    }

    /** 只允许跳回站内路径，避免被构造成开放重定向 */
    private String safeRedirect(String redirect) {
        if (redirect == null || redirect.isEmpty() || !redirect.startsWith("/")
                || redirect.startsWith("//")) {
            return "/";
        }
        return redirect;
    }
}
