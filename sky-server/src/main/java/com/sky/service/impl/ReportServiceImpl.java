package com.sky.service.impl;

// ... (import 语句保持不变) ...

import com.sky.entity.Orders;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        List<SumAmountVO> sumAmountVOList = orderMapper.sumAmountByCheckoutTime(beginDate, endDatePlusOne, Orders.COMPLETED);

        Map<LocalDate, BigDecimal> turnoverMap = sumAmountVOList.stream()
                .collect(Collectors.toMap(SumAmountVO::getDate, SumAmountVO::getTurnover));

        // 步骤二：数据处理 - 填充并补全
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
     * @param begin 统计的开始日期
     * @param end 统计的结束日期
     * @return UserReportVO 用户统计结果
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        // 步骤一：准备数据 - 查询并转换
        // ===================================
        List<CountUserVO> sumAmountVOList = userMapper.countUserByCreateTime(begin, end.plusDays(1));
        //查询beginDate之前的用户数
        LocalDateTime beginDateTime = begin.atStartOfDay();
        Long totalUserCount = userMapper.countUserBeforeDate(beginDateTime);

        Map<LocalDate,Long> countUserMap = sumAmountVOList.stream()
                .collect(Collectors.toMap(CountUserVO::getDate, CountUserVO::getNewUserCount));

        // 步骤二：数据处理 - 填充并补全
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
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO ordersStatistics(LocalDate begin, LocalDate end) {

        List<CountOrdersVO> completedList = orderMapper
                .countOrdersByCheckoutTime(begin, end.plusDays(1), Orders.COMPLETED);
        List<CountOrdersVO> allStatusList = orderMapper
                .countOrdersByCheckoutTime(begin, end.plusDays(1), null);
        Map<LocalDate,Long> completedMap = completedList.stream()
                .collect(Collectors.toMap(CountOrdersVO::getDate, CountOrdersVO::getOrdersCount));
        Map<LocalDate,Long> allStatusMap = allStatusList.stream()
                .collect(Collectors.toMap(CountOrdersVO::getDate, CountOrdersVO::getOrdersCount));

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
     * @return SalesTop10ReportVO 销量排名前10的VO
     */
    @Override
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        List<SalesTop10VO> salesTop10VOList = orderDetailMapper.salesTop10(begin, end.plusDays(1));

        List<String> nameList = salesTop10VOList.stream()
                .map(SalesTop10VO::getName)
                .collect(Collectors.toList());
        String nameListStr = StringUtils.join(nameList, ",");

        List<Long> numberList = salesTop10VOList.stream()
                .map(SalesTop10VO::getTotalNumber)
                .collect(Collectors.toList());
        String numberListStr = StringUtils.join(numberList, ",");

        // 4. 将两个格式化好的字符串封装到最终的VO对象中
        return SalesTop10ReportVO.builder()
                .nameList(nameListStr)
                .numberList(numberListStr)
                .build();

    }
}