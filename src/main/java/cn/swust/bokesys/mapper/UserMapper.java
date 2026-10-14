package cn.swust.bokesys.mapper;

import cn.swust.bokesys.po.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 注册用户表 {@code t_user} 的数据访问接口。
 */
@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 启用 / 禁用账号（t_user.status：1 正常 / 0 禁用） */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    long countAll();
}
