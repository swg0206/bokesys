package cn.swust.bokesys.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论展示对象，对应《概要设计》3.5：
 * 评论展示对象，含回复者昵称与子评论集合。
 *
 * <p>{@link #children} 用于把扁平列表组装成评论楼（树）。组装在 Service 层一次完成，
 * 严禁在模板或递归方法里反复查库。
 */
public class CommentVO implements Serializable {

    private Long id;
    private Long articleId;
    /** 所属文章标题，后台评论列表展示用 */
    private String articleTitle;
    private Long parentId;
    private Long userId;
    /** 评论人昵称 */
    private String nickname;
    private String email;
    private String content;
    /** 状态：0 待审核 / 1 已通过 / 2 已拒绝 */
    private Integer status;
    private LocalDateTime createTime;

    /** 被回复者的昵称，用于「回复 @xxx」的展示 */
    private String replyToNickname;

    /** 子评论（回复），构成评论楼 */
    private List<CommentVO> children = new ArrayList<>();

    /** 该评论是否被回复过 */
    public boolean isHasChildren() {
        return children != null && !children.isEmpty();
    }

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

    public String getArticleTitle() {
        return articleTitle;
    }

    public void setArticleTitle(String articleTitle) {
        this.articleTitle = articleTitle;
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

    public String getReplyToNickname() {
        return replyToNickname;
    }

    public void setReplyToNickname(String replyToNickname) {
        this.replyToNickname = replyToNickname;
    }

    public List<CommentVO> getChildren() {
        return children;
    }

    public void setChildren(List<CommentVO> children) {
        this.children = children == null ? new ArrayList<>() : children;
    }
}
