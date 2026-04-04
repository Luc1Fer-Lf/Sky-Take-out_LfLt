package com.sky.service;

import com.sky.vo.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    UserReportVO userStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    OrderReportVO orderReport(LocalDate begin, LocalDate end);

    /**
     * 查询top10
     * @param begin
     * @param end
     * @return
     */
    SalesTop10ReportVO top10Sales(LocalDate begin, LocalDate end);

    /**
     * 导出营业数据
     * @param response
     */
    void exportBusinessData(HttpServletResponse response);
}
