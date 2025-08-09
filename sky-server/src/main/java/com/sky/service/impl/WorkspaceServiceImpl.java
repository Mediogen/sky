package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    /**
     * 查询今日运营数据
     * @return BusinessDataVO 今日运营数据
     */
    @Override
    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        // ========== 1. 初始化累加变量 ==========
        BigDecimal totalTurnover = BigDecimal.ZERO; // 使用BigDecimal累加营业额，更精确
        Long totalValidOrderCount = 0L;           // 累加有效订单数
        Long totalNewUsers = 0L;                  // 累加新增用户数
        Long totalOrderCount = 0L;                // 累加总订单数

        // ========== 2. 调用已有的Mapper方法，查询【整个时间段】的聚合数据 ==========

        // 准备时间参数（左闭右开区间）
        LocalDate endDatePlusOne = end.plusDays(1);

        // a. 获取时间段内，【每日】的营业额列表
        List<SumAmountVO> turnoverList = orderMapper.sumAmountByCheckoutTime(begin, endDatePlusOne, Orders.COMPLETED);

        // b. 获取时间段内，【每日】的有效订单数列表
        List<CountOrdersVO> validOrderList = orderMapper.countOrdersByCheckoutTime(begin, endDatePlusOne, Orders.COMPLETED);

        // c. 获取时间段内，【每日】的总订单数列表
        List<CountOrdersVO> allOrderList = orderMapper.countOrdersByCheckoutTime(begin, endDatePlusOne, null);

        // d. 获取时间段内，【每日】的新增用户数列表
        List<CountUserVO> newUserList = userMapper.countUserByCreateTime(begin, endDatePlusOne);


        // ========== 3. 在内存中对查询结果进行【累加】处理 ==========

        // a. 累加总营业额
        if (turnoverList != null && !turnoverList.isEmpty()) {
            // 使用Stream API对BigDecimal进行求和
            totalTurnover = turnoverList.stream()
                    .map(SumAmountVO::getTurnover)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // b. 累加总有效订单数
        if (validOrderList != null && !validOrderList.isEmpty()) {
            // 使用Stream API对Integer进行求和
            totalValidOrderCount = validOrderList.stream()
                    .mapToLong(CountOrdersVO::getOrdersCount)
                    .sum();
        }

        // c. 累加总订单数
        if (allOrderList != null && !allOrderList.isEmpty()) {
            totalOrderCount = allOrderList.stream()
                    .mapToLong(CountOrdersVO::getOrdersCount)
                    .sum();
        }

        // d. 累加总新增用户数
        if (newUserList != null && !newUserList.isEmpty()) {
            totalNewUsers = newUserList.stream()
                    .mapToLong(CountUserVO::getNewUserCount)
                    .sum();
        }


        // ========== 4. 基于累加结果，进行最终计算 ==========

        // a. 计算订单完成率
        Double orderCompletionRate = (totalOrderCount == 0) ? 0.0 : totalValidOrderCount.doubleValue() / totalOrderCount;

        // b. 计算平均客单价
        Double unitPrice = (totalValidOrderCount == 0) ? 0.0 : totalTurnover.doubleValue() / totalValidOrderCount;


        // ========== 5. 封装并返回最终的BusinessDataVO ==========

        return BusinessDataVO.builder()
                .turnover(totalTurnover.doubleValue()) // 按需转换为double
                .validOrderCount(totalValidOrderCount.intValue())
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(totalNewUsers.intValue())
                .build();
    }



    /**
     * 查询订单管理数据
     * 对各种状态的订单进行统计
     * @return OrderOverViewVO 订单概览数据
     */
    @Override
    public OrderOverViewVO overviewOrders() {
        OrderOverViewVO orderOverViewVO = new OrderOverViewVO(0,0,0,0,0);
        LocalDate today = LocalDate.now();
        List<SqlOrderStatisticsVO> statistics = orderMapper.statistics(today, today.plusDays(1));

        Integer allOrders = 0;
        if (statistics != null && !statistics.isEmpty()) {
            for (SqlOrderStatisticsVO statistic : statistics) {
                Integer status = statistic.getStatus();
                Integer count = statistic.getCount();
                //使用if-else判断来设置不同状态的订单数量
                if (status.equals(Orders.TO_BE_CONFIRMED)) {
                    orderOverViewVO.setWaitingOrders(count);
                } else if (status.equals(Orders.CONFIRMED)) {
                    orderOverViewVO.setDeliveredOrders(count);
                } else if (status.equals(Orders.COMPLETED)) {
                    orderOverViewVO.setCompletedOrders(count);
                } else if (status.equals(Orders.CANCELLED)) {
                    orderOverViewVO.setCancelledOrders(count);
                }
                // 统计所有订单数量
                allOrders += count;
            }
        }

        orderOverViewVO.setAllOrders(allOrders);

        return orderOverViewVO;
    }

    /**
     * 查询菜品总览
     * @return DishOverViewVO 菜品概览数据
     */
    @Override
    public DishOverViewVO overviewDishes() {
        Integer discontinued = dishMapper.dishCountByStatus(0);
        Integer sold =  dishMapper.dishCountByStatus(1);
        if (discontinued == null) {
            discontinued = 0;
        }
        if (sold == null) {
            sold = 0;
        }
        return DishOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }

    /**
     * 查询套餐总览
     * @return SetmealOverViewVO 套餐概览数据
     */
    @Override
    public SetmealOverViewVO overviewSetmeals() {
        Integer discontinued = setmealMapper.setmealCountByStatus(0);
        Integer sold = setmealMapper.setmealCountByStatus(1);
        if (discontinued == null) {
            discontinued = 0;
        }
        if (sold == null) {
            sold = 0;
        }
        return SetmealOverViewVO.builder()
                .discontinued(discontinued)
                .sold(sold)
                .build();
    }
}
