package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 管理员表 {@code t_admin} 的数据访问接口。
 *
 * <p>对应《概要设计》3.4 数据持久层包结构表。
 */
@Mapper
public interface AdminMapper {

    /** 按登录账号查询（登录校验用） */
    Admin selectByUsername(@Param("username") String username);

    Admin selectById(@Param("id") Long id);

    int insert(Admin admin);

    /** 登录成功后回写最近登录时间 */
    int updateLastLoginTime(@Param("id") Long id, @Param("lastLoginTime") LocalDateTime lastLoginTime);

    /** 更新个人信息（昵称、头像、邮箱） */
    int updateProfile(Admin admin);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    long countAll();
}
