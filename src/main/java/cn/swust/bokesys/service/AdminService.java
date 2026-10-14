package cn.swust.bokesys.service;

import cn.swust.bokesys.po.Admin;

/**
 * 管理员业务接口，对应《概要设计》3.3 与接口 API-09 / API-22。
 */
public interface AdminService {

    /**
     * 管理员登录校验。
     *
     * @param username  登录账号
     * @param password  明文密码
     * @param clientKey 用于限流的客户端标识（账号 + IP），
     *                  对应 6.2.2「连续 5 次失败后锁定 60 秒」
     * @return 校验通过的管理员对象
     */
    Admin login(String username, String password, String clientKey);

    /** 登录成功后清除失败计数 */
    void clearFailures(String clientKey);

    Admin getById(Long id);

    void updateProfile(Admin admin);

    /** 修改密码：先校验原密码，再用新的加盐哈希覆盖 */
    void changePassword(Long adminId, String oldPassword, String newPassword, String confirmPassword);
}
