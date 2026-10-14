package cn.swust.bokesys.service.impl;

import cn.swust.bokesys.common.constant.UserStatus;
import cn.swust.bokesys.common.exception.BusinessException;
import cn.swust.bokesys.common.util.PasswordUtil;
import cn.swust.bokesys.dto.UserRegisterDTO;
import cn.swust.bokesys.mapper.UserMapper;
import cn.swust.bokesys.po.User;
import cn.swust.bokesys.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 注册用户业务实现。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User login(String username, String password) {
        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {
            throw new BusinessException("请输入用户名和密码");
        }
        User user = userMapper.selectByUsername(username.trim());
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            // 不区分「账号不存在」与「密码错误」，避免账号枚举
            throw new BusinessException("用户名或密码错误");
        }
        if (UserStatus.DISABLED.matches(user.getStatus())) {
            throw new BusinessException("该账号已被禁用，请联系管理员");
        }
        return user;
    }

    @Override
    @Transactional
    public void register(UserRegisterDTO dto) {
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isEmpty()) {
            throw new BusinessException("用户名不能为空");
        }
        if (!dto.isPasswordConfirmed()) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException("用户名「" + username + "」已被注册");
        }

        User user = new User();
        user.setUsername(username);
        // 绝不落明文：PBKDF2 加盐哈希，每个用户一份独立随机盐
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().trim().isEmpty()
                ? username : dto.getNickname().trim());
        user.setEmail(dto.getEmail() == null ? null : dto.getEmail().trim());
        user.setStatus(UserStatus.NORMAL.getCode());

        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        userMapper.insert(user);
    }

    @Override
    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional
    public void updateProfile(User user) {
        if (user.getId() == null) {
            throw new BusinessException("缺少用户 ID");
        }
        if (userMapper.selectById(user.getId()) == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updateProfile(user);
    }
}
