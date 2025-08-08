package com.sky.task;


import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;


    /**
     * 处理超时未付款订单
     * 每隔一分钟执行一次
     */
    //@Scheduled(cron = "0 * * * * ?")
    public void handleTimeoutOrder(){
        log.info("处理支付超时订单...");
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        //查询超时订单ID列表
        List<Long> orderIdList = orderMapper.getByStatusAndOrderTime(Orders.PENDING_PAYMENT,time);
        if(orderIdList != null && !orderIdList.isEmpty()){
            for (Long orderId : orderIdList) {
                Orders orders = new Orders();
                orders.setId(orderId);
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，系统自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    //处理超时订单，每天凌晨1点执行一次
    @Scheduled(cron = "0 0 1 * * ?")
    //测试，每隔5秒执行一次
    //@Scheduled(cron = "0/5 * * * * ?")
    public void handleTimeoutOrderDaily() {
        log.info("处理配送中订单...");
        LocalDateTime time = LocalDateTime.now().minusMinutes(60);
        //查询超时订单ID列表
        List<Long> orderIdList = orderMapper.getByStatusAndOrderTime(Orders.DELIVERY_IN_PROGRESS,time);
        if(orderIdList != null && !orderIdList.isEmpty()){
            for (Long orderId : orderIdList) {
                Orders orders = new Orders();
                orders.setId(orderId);
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }

    }
}

