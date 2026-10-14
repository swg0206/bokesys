package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.util.PasswordUtil;
import cn.swust.bokesys.mapper.AdminMapper;
import cn.swust.bokesys.po.Admin;
import cn.swust.bokesys.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理员业务实现。
 *
 * <p>登录失败锁定策略对应《概要设计》6.2.2：
 * 「连续 5 次失败后锁定 60 秒」。由于 t_admin 表没有状态字段，
 * 锁定状态保存在内存中（按 账号+IP 计数），重启即清空 —— 对单机部署足够，
 * 且不会给数据表引入文档之外的字段。
 */
@Service
public class AdminServiceImpl implements AdminService {

    /** 触发锁定的连续失败次数 */
    private static final int MAX_FAILURES = 5;
    /** 锁定时长（秒） */
    private static final long LOCK_SECONDS = 60;

    private final AdminMapper adminMapper;

    /** clientKey -> 失败记录 */
    private final Map<String, FailureRecord> failures = new ConcurrentHashMap<>();

    public AdminServiceImpl(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @Override
    public Admin login(String username, String password, String clientKey) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new BusinessException("请输入用户名和密码");
        }
        String key = clientKey == null ? username.trim() : clientKey;

        long now = System.currentTimeMillis();
        FailureRecord record = failures.get(key);
        if (record != null && record.lockUntil > now) {
            long remain = (record.lockUntil - now) / 1000 + 1;
            throw new BusinessException("登录失败次数过多，请 " + remain + " 秒后重试");
        }

        Admin admin = adminMapper.selectByUsername(username.trim());
        if (admin == null || !PasswordUtil.matches(password, admin.getPassword())) {
            recordFailure(key, now);
            // 不区分「账号不存在」与「密码错误」，避免账号枚举
            throw new BusinessException("用户名或密码错误");
        }

        failures.remove(key);
        adminMapper.updateLastLoginTime(admin.getId(), LocalDateTime.now());
        admin.setLastLoginTime(LocalDateTime.now());
        return admin;
    }

    @Override
    public void clearFailures(String clientKey) {
        if (clientKey != null) {
            failures.remove(clientKey);
        }
    }

    @Override
    public Admin getById(Long id) {
        return adminMapper.selectById(id);
    }

    @Override
    @Transactional
    public void updateProfile(Admin admin) {
        if (admin.getId() == null) {
            throw new BusinessException("缺少管理员 ID");
        }
        if (adminMapper.selectById(admin.getId()) == null) {
            throw new BusinessException("管理员不存在");
        }
        adminMapper.updateProfile(admin);
    }

    @Override
    @Transactional
    public void changePassword(Long adminId, String oldPassword,
                               String newPassword, String confirmPassword) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        if (!PasswordUtil.matches(oldPassword, admin.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException("新密码长度不能少于 6 位");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        adminMapper.updatePassword(adminId, PasswordUtil.encode(newPassword));
    }

    private void recordFailure(String key, long now) {
        FailureRecord record = failures.computeIfAbsent(key, k -> new FailureRecord());
        synchronized (record) {
            record.count++;
            if (record.count >= MAX_FAILURES) {
                record.lockUntil = now + LOCK_SECONDS * 1000;
                record.count = 0;
            }
        }
    }

    /** 某个客户端的失败计数与锁定截止时间 */
    private static final class FailureRecord {
        private int count;
        private long lockUntil;
    }
}
