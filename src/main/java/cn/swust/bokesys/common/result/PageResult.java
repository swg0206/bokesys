package cn.swust.bokesys.common.result;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果封装，对应《概要设计》3.6：
 * {@code { "total": 120, "pageNo": 1, "pageSize": 10, "list": [...] }}
 *
 * <p>同时供 Thymeleaf 模板直接取用（页码计算所需的 pages / hasPrevious / hasNext
 * 都以 getter 形式暴露，避免模板里写复杂表达式）。
 */
public class PageResult<T> implements Serializable {

    private long total;
    private int pageNo;
    private int pageSize;
    private List<T> list = new ArrayList<>();

    public PageResult() {
    }

    public PageResult(long total, int pageNo, int pageSize, List<T> list) {
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.list = list == null ? new ArrayList<>() : list;
    }

    public static <T> PageResult<T> of(long total, int pageNo, int pageSize, List<T> list) {
        return new PageResult<>(total, pageNo, pageSize, list);
    }

    public static <T> PageResult<T> empty(int pageNo, int pageSize) {
        return new PageResult<>(0, pageNo, pageSize, new ArrayList<>());
    }

    /** 总页数，至少为 1，便于模板直接渲染分页条 */
    public int getPages() {
        if (pageSize <= 0) {
            return 1;
        }
        int pages = (int) ((total + pageSize - 1) / pageSize);
        return Math.max(pages, 1);
    }

    public boolean isHasPrevious() {
        return pageNo > 1;
    }

    public boolean isHasNext() {
        return pageNo < getPages();
    }

    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
