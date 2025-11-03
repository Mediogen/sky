package com.sky.task;


import com.sky.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class WebSocketTask {
    @Autowired
    private WebSocketServer webSocketServer;
    // 定时任务示例
    //@Scheduled(cron = "0/5 * * * * ?")
    public void executeWebSocketTask() {
        log.info("执行WebSocket定时任务");
        // 这里可以添加具体的业务逻辑，比如发送消息到WebSocket客户端等
        webSocketServer.sendToAllClient("服务端发送的消息，当前时间：" + LocalDateTime.now());

    }

}
