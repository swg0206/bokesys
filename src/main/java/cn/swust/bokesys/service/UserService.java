package cn.swust.bokesys.service;

import cn.swust.bokesys.dto.UserRegisterDTO;
import cn.swust.bokesys.po.User;

/**
 * 注册用户业务接口，对应《概要设计》3.3 与接口 API-23 / API-24。
 */
public interface UserService {

    /** 登录校验，成功返回用户对象，失败抛出业务异常 */
    User login(String username, String password);

    /** 注册：密码加盐哈希后落库，账号唯一 */
    void register(UserRegisterDTO dto);

    User getById(Long id);

    void updateProfile(User user);
}
