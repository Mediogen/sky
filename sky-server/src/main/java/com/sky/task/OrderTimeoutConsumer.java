package com.sky.task;

import com.sky.config.OrderDelayQueueConfig;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 消费者监听订单超时队列
@Component
@Slf4j
public class OrderTimeoutConsumer {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @RabbitListener(queues = OrderDelayQueueConfig.ORDER_QUEUE_DLX)
    public void handleTimeoutOrder(String orderNumber) {
        System.out.println("接收到超时订单消息，订单号: " + orderNumber);

        if (orderNumber == null) {
            log.warn("收到空的订单消息，已忽略");
            return;
        }
        // 1. 根据订单号查询订单的最新状态
        Orders order = orderMapper.getByNumber(orderNumber);

        if (order == null) {
            log.warn("根据订单号未找到订单，可能已被删除，订单号: {}", orderNumber);
            return;
        }

        // 如果订单状态不是“待付款”，说明用户已经支付或手动取消了，无需处理
        if (!order.getStatus().equals(Orders.PENDING_PAYMENT)) {
            log.info("订单状态已改变（已支付或已取消），无需处理超时，订单号: {}", orderNumber);
            return;
        }
        // 3. 确认订单超时，执行取消订单的业务逻辑
        log.info("订单超时，准备取消订单，订单号: {}", orderNumber);

        // 更新订单状态为“已取消”
        Orders updatedOrder = Orders.builder()
                .id(order.getId())
                .status(Orders.CANCELLED) // 假设你有“已取消”的状态
                .cancelReason("订单超时，自动取消")
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(updatedOrder);

        // 通知商家端
        webSocketServer.sendToAdmin(3,order.getId(),order.getNumber());

    }
}
