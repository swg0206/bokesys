package cn.swust.bokesys.common.util;

import cn.swust.bokesys.common.result.PageResult;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 分页工具：规范化页码参数，并把 PageHelper 的分页结果转换为 {@link PageResult}。
 *
 * <p>注意 PageHelper 的用法约束：{@link PageHelper#startPage} 之后必须**紧跟**
 * 那条需要分页的 Mapper 查询，中间不能插入其它数据库操作，否则分页会作用到错误语句上。
 */
public final class PageUtil {

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NO = 1;
    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;
    /** 单页上限，防止前端传入超大 pageSize 拖垮数据库 */
    public static final int MAX_PAGE_SIZE = 100;

    private PageUtil() {
    }

    /** 页码规范化：小于 1 或为空一律回到第 1 页 */
    public static int normalizePageNo(Integer pageNo) {
        return (pageNo == null || pageNo < 1) ? DEFAULT_PAGE_NO : pageNo;
    }

    /** 每页条数规范化：限制在 1..100 之间 */
    public static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 调用 PageHelper 开启分页，返回规范化后的页码。
     */
    public static int startPage(Integer pageNo, Integer pageSize) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        PageHelper.startPage(no, size);
        return no;
    }

    /**
     * 把一条已经分页的查询结果转换为统一的分页返回体。
     *
     * @param list PageHelper 拦截后返回的列表（内部实现为 com.github.pagehelper.Page）
     */
    public static <T> PageResult<T> toResult(List<T> list) {
        PageInfo<T> info = new PageInfo<>(list);
        return PageResult.of(info.getTotal(), info.getPageNum(), info.getPageSize(), info.getList());
    }
}
