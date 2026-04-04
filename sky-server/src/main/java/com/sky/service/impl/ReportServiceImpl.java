package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrdersMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.Servlet;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;
    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //1、准备日期列表数据  根据begin和end插入日期到列表中
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        log.info("营业额统计，日期范围：{}到{}", dateList.get(0), dateList.get(dateList.size()-1));
        log.info("营业额统计，日期列表：{}", dateList);
        //2、准备数量数据
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//获取日期的开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取日期的结束时间
            Double turnover = ordersMapper.sumByStatusAndOrderTime(Orders.COMPLETED, beginTime, endTime);
            //如果为null，则设置为0
            if (turnover == null) {
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }
        log.info("营业额统计，营业额列表：{}", turnoverList);
        //3、封装数据
        return TurnoverReportVO.builder()
                .dateList(String.join(",", java.util.Arrays.asList(dateList.toString())))
                .turnoverList(String.join(",", java.util.Arrays.asList(turnoverList.toString())))
                .build();
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //1、准备日期列表数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        log.info("营业额统计，日期范围：{}到{}", dateList.get(0), dateList.get(dateList.size() - 1));
        log.info("营业额统计，日期列表：{}", dateList);
        //2、准备新增用户数量数据
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//获取日期的开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取日期的结束时间
            Integer newUser = ordersMapper.countByCreateTime(beginTime, endTime);
            //如果为null，则设置为0
            if (newUser == null || newUser == 0) {
                newUser = 0;
            }
            newUserList.add(newUser);
        }
        log.info("用户统计，新增用户列表：{}", newUserList);
        //3、准备总用户数量数据
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取日期的结束时间
            Integer totalUser = ordersMapper.countSumByCreateTime(endTime);
            //如果为null，则设置为0
            if (totalUser == null || totalUser == 0) {
                totalUser = 0;
            }
            totalUserList.add(totalUser);
            log.info("用户统计，总用户列表：{}", totalUserList);
        }
        return UserReportVO.builder()
                .dateList(String.join(",", java.util.Arrays.asList(dateList.toString())))
                .totalUserList(String.join(",", java.util.Arrays.asList(totalUserList.toString())))
                .newUserList(String.join(",", java.util.Arrays.asList(newUserList.toString())))
                .build();
    }

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderReport(LocalDate begin, LocalDate end) {
        //1、准备日期列表数据
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        log.info("订单统计，日期范围：{}到{}", dateList.get(0), dateList.get(dateList.size() - 1));
        log.info("订单统计，日期列表：{}", dateList);
        //2.0 准备两个整形，封装订单总数和有效订单总数
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;
        //2、准备每日订单总数数据
        List<Integer> orderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//获取日期的开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取日期的结束时间
            Integer orderCountNum = ordersMapper.sumOrderCountByOrderTime(beginTime, endTime);
            //如果为null，则设置为0
            if (orderCountNum == null) {
                orderCountNum = 0;
            }
            //订单总数累加
            totalOrderCount += orderCountNum;
            orderCountList.add(orderCountNum);
        }
        //3、准备每日有效订单数量数据
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);//获取日期的开始时间
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);//获取日期的结束时间
            Integer validOrderCountNum = ordersMapper.sumValidOrderCountByOrderTime(Orders.COMPLETED, beginTime, endTime);
            //如果为null，则设置为0
            if (validOrderCountNum == null) {
                validOrderCountNum = 0;
            }
            //有效订单总数累加
            validOrderCount += validOrderCountNum;
            validOrderCountList.add(validOrderCountNum);
        }
        //4、计算订单完成率
        Double orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount; // 订单完成率（有效订单数/订单总数）*100%
        //5、分装数据并返回
        return OrderReportVO.builder()
                .dateList(String.join(",", java.util.Arrays.asList(dateList.toString())))
                .orderCountList(String.join(",", java.util.Arrays.asList(orderCountList.toString())))
                .validOrderCountList(String.join(",", java.util.Arrays.asList(validOrderCountList.toString())))
                .orderCompletionRate(orderCompletionRate)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .build();
    }

    /**
     * 销量排名top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO top10Sales(LocalDate begin, LocalDate end) {
        //0、准备 nameList 和 numberList
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        //1、日期内的订单号列表  且状态为已完成 5  订单号对应的订单明细数据  查询对应的订单明细数据
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);//获取日期的开始时间
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);//获取日期的结束时间
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", Orders.COMPLETED);
        List<Map> list = ordersMapper.sumTop10(map);
        //3、从查询结果中提取商品名称和数量，SQL 已按销量降序排列
        for (Map mapItem : list) {
            nameList.add((String) mapItem.get("name"));
            numberList.add(((java.math.BigDecimal) mapItem.get("sumNum")).intValue());
        }
        log.info("销量排名 top10，商品名称列表：{}", nameList);
        log.info("销量排名 top10，商品数量列表：{}", numberList);
        //5、返回结果
        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    /**
     * 导出Excel报表
     * @param response
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1、查询数据库 获取数据
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));
        //2、根据查询结果，封装Excel数据
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + begin + "至" + end);// 时间
            sheet.getRow(3).getCell(2).setCellValue(businessDataVO.getTurnover()); //营业额
            sheet.getRow(3).getCell(4).setCellValue(businessDataVO.getOrderCompletionRate()); //有效订单数
            sheet.getRow(3).getCell(6).setCellValue(businessDataVO.getNewUsers()); //新增用户数
            sheet.getRow(4).getCell(2).setCellValue(businessDataVO.getValidOrderCount()); //有效订单数
            sheet.getRow(4).getCell(4).setCellValue(businessDataVO.getUnitPrice()); //客单价
            //填充明细数据
            for(int i = 0; i < 30; i++){
                LocalDate date = begin.plusDays(i);
                //获取指定日期的营业额、订单数、订单完成数、新增用户数
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                sheet.getRow(7 + i).getCell(1).setCellValue(date.toString());
                sheet.getRow(7 + i).getCell(2).setCellValue(businessData.getTurnover());
                sheet.getRow(7 + i).getCell(3).setCellValue(businessData.getValidOrderCount());
                sheet.getRow(7 + i).getCell(4).setCellValue(businessData.getOrderCompletionRate());
                sheet.getRow(7 + i).getCell(5).setCellValue(businessData.getUnitPrice());
                sheet.getRow(7 + i).getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3、通过输出流写出Excel数据
            ServletOutputStream out = response.getOutputStream();
            excel.write(out); // 写入Excel数据

            //4、关闭流
            out.close();
            excel.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

}
