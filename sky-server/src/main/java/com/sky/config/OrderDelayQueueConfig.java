package com.sky.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderDelayQueueConfig {
    // --- 延时队列（死信队列）相关配置 ---

    // 1. 业务交换机 (普通交换机)
    public static final String ORDER_EXCHANGE_NORMAL = "order.exchange.normal";
    // 2. 业务队列 (普通队列)，消息会在这里过期
    public static final String ORDER_QUEUE_NORMAL = "order.queue.normal";
    // 3. 业务队列的路由键
    public static final String ORDER_ROUTING_KEY_NORMAL = "order.routing.normal";

    // 4. 死信交换机 (DLX)
    public static final String ORDER_EXCHANGE_DLX = "order.exchange.dlx";
    // 5. 死信队列 (DLX 队列)，消费者监听这个队列
    public static final String ORDER_QUEUE_DLX = "order.queue.dlx";
    // 6. 死信队列的路由键
    public static final String ORDER_ROUTING_KEY_DLX = "order.routing.dlx";

    // 订单超时时间，例如30分钟 (1800 * 1000毫秒)
    public static final long ORDER_TTL = 1 * 60 * 1000;

    // === 声明业务交换机和队列 ===
    @Bean
    public DirectExchange orderNormalExchange() {
        return new DirectExchange(ORDER_EXCHANGE_NORMAL);
    }

    @Bean
    public Queue orderNormalQueue() {
        Map<String, Object> args = new HashMap<>();
        // 关键：设置消息过期后转发到的死信交换机
        args.put("x-dead-letter-exchange", ORDER_EXCHANGE_DLX);
        // 关键：设置消息过期后转发时使用的路由键
        args.put("x-dead-letter-routing-key", ORDER_ROUTING_KEY_DLX);
        // (可选) 为整个队列设置统一的TTL，但我们将在发送消息时单独设置
        // args.put("x-message-ttl", ORDER_TTL);
        return new Queue(ORDER_QUEUE_NORMAL, true, false, false, args);
    }

    @Bean
    public Binding orderNormalBinding() {
        return BindingBuilder.bind(orderNormalQueue()).to(orderNormalExchange()).with(ORDER_ROUTING_KEY_NORMAL);
    }


    // === 声明死信交换机和队列 ===
    @Bean
    public DirectExchange orderDlxExchange() {
        return new DirectExchange(ORDER_EXCHANGE_DLX);
    }

    @Bean
    public Queue orderDlxQueue() {
        // 这是一个普通的队列，用于接收死信
        return new Queue(ORDER_QUEUE_DLX);
    }

    @Bean
    public Binding orderDlxBinding() {
        return BindingBuilder.bind(orderDlxQueue()).to(orderDlxExchange()).with(ORDER_ROUTING_KEY_DLX);
    }
}
