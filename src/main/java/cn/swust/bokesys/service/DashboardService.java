package cn.swust.bokesys.service;

import cn.swust.bokesys.vo.DashboardVO;

/**
 * 仪表盘业务接口，对应《概要设计》3.3 与接口 API-11：
 * 汇总文章总数、评论总数、分类数、标签数等统计数据供仪表盘展示。
 */
public interface DashboardService {

    /** 汇总仪表盘所需的全部统计数据 */
    DashboardVO stats();
}
