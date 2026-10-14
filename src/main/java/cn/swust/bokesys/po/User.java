package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 注册用户持久化对象，对应数据表 {@code t_user}（《概要设计》5.2.2）。
 */
public class User implements Serializable {

    private Long id;
    /** 登录账号，唯一 */
    private String username;
    /** 密码（加盐哈希后的密文） */
    private String password;
    /** 昵称，评论展示时优先使用 */
    private String nickname;
    private String email;
    private String avatar;
    /** 状态：1 正常 / 0 禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /** 评论展示用的昵称：优先昵称，其次账号 */
    public String getDisplayName() {
        return (nickname == null || nickname.isEmpty()) ? username : nickname;
    }
}
