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
    public BusinessDataVO getBusinessData() {
        Double turnover = 0.0;//营业额
        Integer validOrderCount = 0;//有效订单数
        Double orderCompletionRate = 0.0;//订单完成率
        Double unitPrice = 0.0;//平均客单价
        Integer newUsers = 0;//新增用户数

        // 获取今日的日期
        LocalDate today = LocalDate.now();
        // 1. 获取今日营业额
        List<SumAmountVO> list = orderMapper.sumAmountByCheckoutTime(today, today.plusDays(1), Orders.COMPLETED);
        if (list != null && !list.isEmpty()) {
            // 因为我们只查询了一天，所以这个列表最多只会有一个元素。
            // 直接获取第一个元素的turnover即可。
            turnover = list.get(0).getTurnover().doubleValue();
        }
        // 2. 获取今日有效订单数
        List<CountOrdersVO> list2 = orderMapper.countOrdersByCheckoutTime(today, today.plusDays(1),Orders.COMPLETED);
        if (list2 != null && !list2.isEmpty()) {
            validOrderCount = list2.get(0).getOrdersCount().intValue();
        }
        // 3. 获取今日新增用户数
        List<CountUserVO> list3 = userMapper.countUserByCreateTime(today, today.plusDays(1));
        if (list3 != null && !list3.isEmpty()) {
            newUsers = list3.get(0).getNewUserCount().intValue();
        }
        //4. 计算订单完成率
        // 获取今日所有订单
        List<CountOrdersVO> allOrders = orderMapper.countOrdersByCheckoutTime(today, today.plusDays(1), null);
        if (allOrders != null && !allOrders.isEmpty()) {
            int totalOrders = allOrders.get(0).getOrdersCount().intValue();
            if (totalOrders > 0) {
                orderCompletionRate = (double) validOrderCount / totalOrders;
            }
        }
        // 5. 计算平均客单价(其实可以直接用营业额除以有效订单数，我犯唐了)
        List<AvgAmountVO> avgAmountList = orderMapper.avgAmountByCheckoutTime(today, today.plusDays(1), Orders.COMPLETED);
        if (avgAmountList != null && !avgAmountList.isEmpty()) {
            unitPrice = avgAmountList.get(0).getAvgAmount().doubleValue();
        }
        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
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
