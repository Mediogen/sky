package com.sky.service.impl;

// ... (import 语句保持不变) ...

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService; // 假设这个服务已经实现了相关的业务逻辑

    /**
     * 营业额统计
     * @param beginDate 统计的开始日期
     * @param endDate   统计的结束日期
     * @return TurnoverReportVO 营业额统计结果
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate beginDate, LocalDate endDate) {
        // ========== 核心逻辑开始 ==========

        // 步骤一：准备数据 - 查询并转换
        // ===================================
        LocalDate endDatePlusOne = endDate.plusDays(1);
        List<SumAmount> sumAmountList = orderMapper.sumAmountByCheckoutTime(beginDate, endDatePlusOne, Orders.COMPLETED);

        Map<LocalDate, BigDecimal> turnoverMap = sumAmountList.stream()
                .collect(Collectors.toMap(SumAmount::getDate, SumAmount::getTurnover));

        // 步骤二：数据处理
        // ===================================

        // 1. 创建两个空列表
        List<LocalDate> continuousDateList = new ArrayList<>();
        List<BigDecimal> continuousTurnoverList = new ArrayList<>();

        // 2. 从开始日期循环到结束日期
        LocalDate currentDate = beginDate;
        while (!currentDate.isAfter(endDate)) {
            // a. 将当前日期加入到连续日期列表中
            continuousDateList.add(currentDate);

            // b. 从Map中获取营业额，默认值为 BigDecimal.ZERO
            BigDecimal turnover = turnoverMap.getOrDefault(currentDate, BigDecimal.ZERO);

            // c. 将获取到的 BigDecimal 对象加入到列表中
            continuousTurnoverList.add(turnover);

            // d. 日期增加一天
            currentDate = currentDate.plusDays(1);
        }

        // 步骤三：封装结果 - 格式化并返回
        // ===================================

        // 1. 将日期列表转换为逗号分隔的字符串
        String dateListStr = StringUtils.join(continuousDateList, ",");

        // 2. 将BigDecimal列表转换为逗号分隔的字符串
        //    StringUtils.join会自动调用每个BigDecimal对象的toString()方法，所以这里能正常工作
        String turnoverListStr = StringUtils.join(continuousTurnoverList, ",");

        // 3. 使用Builder模式创建并返回最终的VO对象
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    /**
     * 用户统计
     * @param begin 统计的开始日期
     * @param end 统计的结束日期
     * @return UserReportVO 用户统计结果
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 步骤一：准备数据 - 查询并转换
        // ===================================
        List<CountUser> sumAmountVOList = userMapper.countUserByCreateTime(begin, end.plusDays(1));
        //查询beginDate之前的用户数
        LocalDateTime beginDateTime = begin.atStartOfDay();
        Long totalUserCount = userMapper.countUserBeforeDate(beginDateTime);

        Map<LocalDate,Long> countUserMap = sumAmountVOList.stream()
                .collect(Collectors.toMap(CountUser::getDate, CountUser::getNewUserCount));

        // 步骤二：数据处理
        // ===================================

        // 1. 创建3个空列表
        List<LocalDate> dateList = new ArrayList<>();
        List<Long> newUserList = new ArrayList<>();
        List<Long> totalUserList = new ArrayList<>();

        // 2. 从开始日期循环到结束日期
        LocalDate currentDate = begin;
        while (!currentDate.isAfter(end)) {
            // a. 将当前日期加入到连续日期列表中
            dateList.add(currentDate);

            // b. 从Map中获取每日用户数，默认值为 0L
            Long userCount  = countUserMap.getOrDefault(currentDate, 0L);
            totalUserCount += userCount;

            // c. 将获取到的每日用户数加入到列表中
            newUserList.add(userCount);

            totalUserList.add(totalUserCount);

            // d. 日期增加一天
            currentDate = currentDate.plusDays(1);
        }

        // 步骤三：封装结果 - 格式化并返回
        // ===================================

        // 1. 将日期列表转换为逗号分隔的字符串
        String dateListStr = StringUtils.join(dateList, ",");

        // 2. 将BigDecimal列表转换为逗号分隔的字符串
        //    StringUtils.join会自动调用每个BigDecimal对象的toString()方法，所以这里能正常工作
        String turnoverListStr = StringUtils.join(newUserList, ",");

        String totalUserListStr = StringUtils.join(totalUserList, ",");

        // 3. 使用Builder模式创建并返回最终的VO对象
        return UserReportVO.builder()
                .dateList(dateListStr)
                .totalUserList(totalUserListStr)
                .newUserList(turnoverListStr)
                .build();


    }

    /**
     * 订单统计
     * @param begin 统计的开始日期
     * @param end 统计的结束日期
     * @return OrderReportVO 订单统计结果
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {

        List<CountOrders> completedList = orderMapper
                .countOrdersByCheckoutTime(begin, end.plusDays(1), Orders.COMPLETED);
        List<CountOrders> allStatusList = orderMapper
                .countOrdersByCheckoutTime(begin, end.plusDays(1), null);
        Map<LocalDate,Long> completedMap = completedList.stream()
                .collect(Collectors.toMap(CountOrders::getDate, CountOrders::getOrdersCount));
        Map<LocalDate,Long> allStatusMap = allStatusList.stream()
                .collect(Collectors.toMap(CountOrders::getDate, CountOrders::getOrdersCount));

        List<LocalDate> dateList = new ArrayList<>();
        List<Long> validOrderCountList = new ArrayList<>();
        List<Long> orderCountList = new ArrayList<>();
        //有效订单数
        Long validOrderCount = 0L;
        //订单总数
        Long totalOrderCount = 0L;

        LocalDate currentDate = begin;

        while (!currentDate.isAfter(end)) {
            // a. 将当前日期加入到连续日期列表中
            dateList.add(currentDate);
            // b. 从Map中获取每日有效订单数，默认值为 0L
            Long validOrderCountToday = completedMap.getOrDefault(currentDate, 0L);
            validOrderCount += validOrderCountToday;
            validOrderCountList.add(validOrderCountToday);
            // c. 从Map中获取每日订单数，默认值为 0L
            Long orderCountToday = allStatusMap.getOrDefault(currentDate, 0L);
            totalOrderCount += orderCountToday;
            orderCountList.add(orderCountToday);

            // d. 日期增加一天
            currentDate = currentDate.plusDays(1);
        }
        // 计算订单完成率
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (double) validOrderCount / totalOrderCount;

        // 将三个列表转换为逗号分隔的字符串
        String dateListStr = StringUtils.join(dateList, ",");
        String validOrderCountListStr = StringUtils.join(validOrderCountList, ",");
        String orderCountListStr = StringUtils.join(orderCountList, ",");

        return OrderReportVO.builder()
                .dateList(dateListStr)
                .validOrderCountList(validOrderCountListStr)
                .orderCountList(orderCountListStr)
                .totalOrderCount(totalOrderCount.intValue())
                .validOrderCount(validOrderCount.intValue())
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名前10的菜品或套餐
     * @return SalesTop10ReportVO 销量排名前10的VO
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        List<SalesTop10> salesTop10List = orderDetailMapper.salesTop10(begin, end.plusDays(1));

        List<String> nameList = salesTop10List.stream()
                .map(SalesTop10::getName)
                .collect(Collectors.toList());
        String nameListStr = StringUtils.join(nameList, ",");

        List<Long> numberList = salesTop10List.stream()
                .map(SalesTop10::getTotalNumber)
                .collect(Collectors.toList());
        String numberListStr = StringUtils.join(numberList, ",");

        // 4. 将两个格式化好的字符串封装到最终的VO对象中
        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();

    }

    /**
     * 导出Excel报表
     * @param response HttpServletResponse
     */
    @Override
    public void exportExcel(HttpServletResponse response) {
        //查数据库，获取营业额数据
        BusinessDataVO total=  workspaceService.getBusinessData(LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(1));
        //读取模版
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        if (in == null) {
            throw new RuntimeException("模版文件不存在");
        }
        //创建Excel工作簿
        try {
            Workbook workbook = new XSSFWorkbook(in);
            //填充total数据
            XSSFSheet sheet1 = (XSSFSheet) workbook.getSheet("sheet1");
            sheet1.getRow(1).getCell(1).setCellValue(LocalDate.now().minusDays(30)+"至"+LocalDate.now().minusDays(1));
            XSSFRow row3 = sheet1.getRow(3);
            row3.getCell(2).setCellValue(total.getTurnover());
            row3.getCell(4).setCellValue(total.getOrderCompletionRate());
            row3.getCell(6).setCellValue(total.getNewUsers());
            XSSFRow row4 = sheet1.getRow(4);
            row4.getCell(2).setCellValue(total.getValidOrderCount());
            row4.getCell(4).setCellValue(total.getUnitPrice());
            //填充前30天的营业额数据
            for(int i = 0; i < 30; i++) {
                LocalDate date = LocalDate.now().minusDays(30 - i);
                BusinessDataVO businessData = workspaceService.getBusinessData(date, date);
                XSSFRow row = sheet1.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            ServletOutputStream out = response.getOutputStream();
            workbook.write(out);
            out.close();
            workbook.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }

}