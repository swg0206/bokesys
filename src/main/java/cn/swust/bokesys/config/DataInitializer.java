package cn.swust.bokesys.config;

import cn.swust.bokesys.common.constant.UserStatus;
import cn.swust.bokesys.common.util.PasswordUtil;
import cn.swust.bokesys.mapper.AdminMapper;
import cn.swust.bokesys.mapper.UserMapper;
import cn.swust.bokesys.po.Admin;
import cn.swust.bokesys.po.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动期初始化账号数据。
 *
 * <p>为什么不把账号直接写进 data.sql：密码必须是 PBKDF2 加盐哈希，
 * 而盐是随机生成的 —— 硬编码哈希字面量既不可读，也极易与 Java 实现
 * 的参数（迭代次数、盐长度）对不齐，导致「脚本能跑但登录不上」这种难查的问题。
 * 因此改由这里用与运行期完全相同的 {@link PasswordUtil} 现算哈希。
 *
 * <p>执行时机：{@code ApplicationRunner} 在 Spring 上下文刷新完成后运行，
 * 晚于 {@code spring.sql.init} 执行的 schema.sql / data.sql，所以此时表已建好。
 * 只在表为空时插入，重复启动不会覆盖管理员的改密结果。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** 默认管理员账号 */
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    /** 默认演示用户账号，用于演示「注册用户发表评论」这条链路 */
    private static final String DEMO_USER_USERNAME = "user";
    private static final String DEMO_USER_PASSWORD = "123456";

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;

    public DataInitializer(AdminMapper adminMapper, UserMapper userMapper) {
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initDemoUser();
    }

    private void initAdmin() {
        if (adminMapper.countAll() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Admin admin = new Admin();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(PasswordUtil.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setNickname("站点管理员");
        admin.setEmail("admin@example.com");
        admin.setCreateTime(now);
        admin.setUpdateTime(now);
        adminMapper.insert(admin);
        log.info("已初始化默认管理员账号：{} / {}（请在个人信息页及时修改密码）",
                DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    private void initDemoUser() {
        if (userMapper.countAll() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(DEMO_USER_USERNAME);
        user.setPassword(PasswordUtil.encode(DEMO_USER_PASSWORD));
        user.setNickname("演示用户");
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
        log.info("已初始化演示注册用户：{} / {}（用于演示发表评论）",
                DEMO_USER_USERNAME, DEMO_USER_PASSWORD);
    }
}
