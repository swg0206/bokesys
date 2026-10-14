package cn.swust.bokesys.controller.admin;

import cn.swust.bokesys.common.constant.SessionConstant;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.result.Result;
import cn.swust.bokesys.controller.base.AdminBaseController;
import cn.swust.bokesys.po.Admin;
import cn.swust.bokesys.service.AdminService;
import cn.swust.bokesys.service.CategoryService;
import cn.swust.bokesys.service.ConfigService;
import cn.swust.bokesys.service.LinkService;
import cn.swust.bokesys.service.TagService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

/**
 * 个人信息维护，对应 API-22（Fun_15）：管理员用户名、密码、头像、邮箱。
 *
 * <p>另含封面图上传接口 {@code POST /admin/upload} —— 文章编辑器需要，
 * 文档 4.2.3 的输入项里列了「封面图文件」并要求返回可访问的图片 URL。
 */
@Controller
@RequestMapping("/admin/profile")
public class ProfileController extends AdminBaseController {

    /** 允许上传的图片后缀 */
    private static final String[] ALLOWED_EXT = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
    /** 单张图片大小上限：5MB，与 application.yml 的 multipart 限制保持一致 */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final AdminService adminService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public ProfileController(ConfigService configService,
                             CategoryService categoryService,
                             TagService tagService,
                             LinkService linkService,
                             AdminService adminService) {
        super(configService, categoryService, tagService, linkService);
        this.adminService = adminService;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        // 从库里重新读一遍，保证页面显示的是最新资料而不是登录时的快照
        Admin current = requireAdmin(session);
        model.addAttribute("admin", adminService.getById(current.getId()));
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("pageTitle", "个人信息");
        return "admin/profile";
    }

    /** 更新昵称、头像、邮箱 */
    @PutMapping
    @ResponseBody
    public Result<Void> update(@RequestParam(value = "nickname", required = false) String nickname,
                               @RequestParam(value = "email", required = false) String email,
                               @RequestParam(value = "avatar", required = false) String avatar,
                               HttpSession session) {
        Admin current = requireAdmin(session);
        Admin update = new Admin();
        update.setId(current.getId());
        update.setNickname(nickname);
        update.setEmail(email);
        update.setAvatar(avatar);
        adminService.updateProfile(update);

        // 同步刷新 Session 里的快照，否则页面顶栏还显示旧昵称
        session.setAttribute(SessionConstant.LOGIN_ADMIN, adminService.getById(current.getId()));
        return Result.ok("个人信息已更新", null);
    }

    /** 修改密码：校验原密码 + 两次新密码一致 */
    @PutMapping("/password")
    @ResponseBody
    public Result<Void> changePassword(@RequestParam("oldPassword") String oldPassword,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session) {
        Admin current = requireAdmin(session);
        adminService.changePassword(current.getId(), oldPassword, newPassword, confirmPassword);
        return Result.ok("密码已修改，请重新登录", null);
    }

    /**
     * 上传图片，返回可直接引用的访问路径。
     *
     * <p>落盘目录由 {@code file.upload-dir} 指定，并通过 WebMvcConfig 映射到 /uploads/**。
     * 文件名用 UUID 重命名，避免覆盖同名文件，也顺手规避了用户传入路径穿越文件名。
     */
    @PostMapping("/upload")
    @ResponseBody
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (!isAllowed(ext)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename).normalize();
            // 双保险：确保最终路径仍在允许的目录内
            if (!target.startsWith(dir)) {
                throw new BusinessException("非法的文件路径");
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException("图片上传失败：" + e.getMessage(), e);
        }
        return Result.ok("上传成功", "/uploads/" + filename);
    }

    private boolean isAllowed(String ext) {
        for (String allowed : ALLOWED_EXT) {
            if (allowed.equals(ext)) {
                return true;
            }
        }
        return false;
    }
}
