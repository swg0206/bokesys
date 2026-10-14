package cn.swust.bokesys.po;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论持久化对象，对应数据表 {@code t_comment}（《概要设计》5.2.7）。
 *
 * <p>通过 {@code parent_id} 自关联实现回复（评论楼）：
 * parent_id 为空表示顶层评论，否则指向被回复的评论。
 */
public class Comment implements Serializable {

    private Long id;
    /** 所属文章 ID */
    private Long articleId;
    /** 父评论 ID，为空表示顶层评论 */
    private Long parentId;
    /** 评论人 ID（注册用户） */
    private Long userId;
    /** 评论时展示的昵称（冗余存储） */
    private String nickname;
    private String email;
    /** 评论内容，长度 1~500 字符 */
    private String content;
    /** 状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
