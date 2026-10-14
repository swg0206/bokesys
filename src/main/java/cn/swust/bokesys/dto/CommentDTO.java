package cn.swust.bokesys.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 评论提交入参，对应《概要设计》3.5 的 CommentDTO{articleId, parentId, content}
 * 与接口 API-07。
 */
public class CommentDTO implements Serializable {

    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    /** 父评论 ID，回复场景传入；顶层评论为空 */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 500, message = "评论内容长度需在 1~500 个字符之间")
    private String content;

    /** 昵称：未登录用户可留空，此时取注册用户的昵称 */
    @Size(max = 50, message = "昵称不能超过 50 个字符")
    private String nickname;

    @Size(max = 100, message = "邮箱不能超过 100 个字符")
    private String email;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
