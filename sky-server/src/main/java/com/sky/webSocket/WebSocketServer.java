package com.sky.webSocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {

    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        //System.out.println("客户端：" + sid + "建立连接");
        log.info("客户端：{}建立连接", sid);
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自客户端：" + sid + "的信息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid 连接的唯一标识符
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("连接断开:" + sid);
        sessionMap.remove(sid);
    }

    /**
     * 群发
     *
     * @param message 要发送的消息
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("向客户端发送消息失败: {}", e.getMessage());
            }
        }
    }

    public void sendToAdmin(Integer type,Long orderId,String orderNumber){
        String messageType;
        if (type == 1) {
            messageType = "来单提醒";
        } else if (type == 2) {
            messageType = "客户催单";
        } else if (type == 3) {
            // TODO 该类型前端未实现
            messageType = "订单超时未支付已取消";
        } else {
            messageType = "未知类型: " + type; // 处理未知情况
        }
        log.info("发送消息到管理员，类型：{}，订单ID：{}，订单号：{}", messageType, orderId, orderNumber);

        Map map = new HashMap<>();
        map.put("type", type);
        map.put("orderId", orderId);
        map.put("content", "订单号"+ orderNumber);
        String jsonMessage = JSON.toJSONString(map);
        sendToAllClient(jsonMessage);


    }

}
