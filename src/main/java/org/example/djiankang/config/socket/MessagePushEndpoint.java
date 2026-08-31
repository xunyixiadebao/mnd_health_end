package org.example.djiankang.config.socket;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.example.djiankang.config.satoken.StpCustomerUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 消息推送端点
 *
 * <p>功能：建立 WebSocket 长连接，实现服务端主动向客户端推送消息
 *
 * <p>连接地址：ws://域名/websocket/push/message
 *
 * <p>使用流程：
 * <ol>
 *   <li>客户端建立 WebSocket 连接</li>
 *   <li>连接成功后，客户端需发送注册消息（包含身份和令牌）</li>
 *   <li>服务端绑定 userId 和 Session，后续可通过 sendInfo() 主动推送</li>
 * </ol>
 */
@Slf4j
@ServerEndpoint(value = "/websocket/push/message")
@Component
public class MessagePushEndpoint {

    /*
        key存储的是用户的id，value是这个用户对应的Socket连接。
        假设业务端的id=1的用户访问，则key是 customer_1
        假设mis端的id=1的用户访问，则key是 user_1
        每一个用户都有自己专属的Socket连接（也就是Session对象）
        并且Socket连接永不共享，一个用户对应一个。
        customer_1==>对应自己的Socket连接。
        user_1==>对应自己的Socket连接。
        一个Socket连接不能让张三用了之后，李四又用，这是不可能的。
     */
    private static final ConcurrentHashMap<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 会话超时时间：30分钟
     */
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 用户身份：C端客户
     */
    private static final String IDENTITY_CUSTOMER = "customer";

    /**
     * 操作类型：注册
     */
    private static final String OPT_REGISTER = "register";

    /**
     * 操作类型：心跳
     */
    private static final String OPT_PING = "ping";

    /**
     * WebSocket 连接建立时自动调用
     *
     * @param session 当前会话对象
     */
    @OnOpen
    public void onOpen(Session session) {
        // 设置会话超时时间，防止长时间空闲连接
        session.setMaxIdleTimeout(SESSION_TIMEOUT);
        log.info("WebSocket 连接建立，sessionId: {}", session.getId());
    }

    /**
     * 接收客户端消息时自动调用
     *
     * <p>作用：将用户的id和对应的socket绑定到 SESSION_MAP 中（注册）
     *
     * <p>客户端发送消息格式示例：
     * <pre>
     * ws.send(JSON.stringify({
     *     "opt": "register",      // 操作类型：register / ping
     *     "identity": "customer", // 用户身份：customer / user
     *     "token": "eyJhbGciOiJ..." // 认证令牌
     * }));
     * </pre>
     *
     * @param message 客户端发送的消息内容
     * @param session 当前会话对象
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        JSONObject json = JSONUtil.parseObj(message);
        String opt = json.getStr("opt");

        // 心跳消息，无需处理
        if (OPT_PING.equals(opt)) {
            return;
        }

        // 只处理注册消息
        if (!OPT_REGISTER.equals(opt)) {
            log.warn("未知操作类型: {}", opt);
            return;
        }

        // 获取用户的身份和令牌
        String identity = json.getStr("identity");
        String token = json.getStr("token");

        // 根据身份生成 userId 作为存储 key
        String userId = resolveUserId(identity, token);
        if (userId == null) {
            log.warn("用户身份解析失败，identity: {}", identity);
            return;
        }

        // 将 userId 存储到 Session 的自定义属性中，便于 OnClose 时清理
        Map<String, Object> properties = session.getUserProperties();
        properties.put("userId", userId);

        // 注册到 SESSION_MAP
        // 同一个用户重复连接时，新的 Session 会覆盖旧的
        Session oldSession = SESSION_MAP.put(userId, session);
        if (oldSession != null && oldSession.isOpen()) {
            // 如果旧连接还开着，关掉它（防止同一用户多个连接）
            try {
                oldSession.close();
                log.info("关闭用户 {} 的旧连接", userId);
            } catch (Exception e) {
                log.warn("关闭用户 {} 的旧连接异常", userId, e);
            }
        }
        log.info("用户 {} 注册成功，当前在线连接数：{}", userId, SESSION_MAP.size());
    }

    /**
     * 根据身份和令牌解析用户ID
     *
     * @param identity 用户身份（customer / user）
     * @param token    认证令牌
     * @return 格式化的用户ID，如 "customer_1" / "user_1"，解析失败返回 null
     */
    private String resolveUserId(String identity, String token) {
        try {
            if (IDENTITY_CUSTOMER.equals(identity)) {
                Object loginId = StpCustomerUtil.getLoginIdByToken(token);
                return "customer_" + loginId;
            } else {
                Object loginId = StpUtil.getLoginIdByToken(token);
                return "user_" + loginId;
            }
        } catch (Exception e) {
            log.warn("Token 解析失败，identity: {}", identity, e);
            return null;
        }
    }

    /**
     * WebSocket 连接关闭时自动调用
     *
     * @param session 当前会话对象
     */
    @OnClose
    public void onClose(Session session) {
        Map<String, Object> properties = session.getUserProperties();
        Object userIdObj = properties.get("userId");

        if (userIdObj != null) {
            String userId = userIdObj.toString();
            // 确保只移除当前会话，避免移除其他新会话
            SESSION_MAP.remove(userId, session);
            log.info("用户 {} 连接已关闭，当前在线连接数：{}", userId, SESSION_MAP.size());
        } else {
            log.info("会话关闭，sessionId: {}", session.getId());
        }
    }

    /**
     * WebSocket 发生错误时自动调用
     *
     * @param session 当前会话对象
     * @param error   错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 异常，sessionId: {}", session.getId(), error);
        // 错误时也清理连接
        onClose(session);
    }

    /**
     * 服务端主动向客户端推送消息
     *
     * <p>使用示例：
     * <pre>
     * MessagePushEndpoint.sendInfo("支付成功", "customer_1");
     * </pre>
     *
     * @param message 要发送的消息内容（JSON 字符串）
     * @param userId  目标用户ID（格式：customer_xxx / user_xxx）
     */
    public static void sendInfo(String message, String userId) {
        if (StrUtil.isBlank(userId)) {
            log.warn("发送消息失败：userId 为空");
            return;
        }

        Session session = SESSION_MAP.get(userId);
        if (session == null) {
            log.warn("发送消息失败：用户 {} 不在线", userId);
            return;
        }

        if (!session.isOpen()) {
            // 会话已关闭，清理无效连接
            SESSION_MAP.remove(userId, session);
            log.warn("发送消息失败：用户 {} 的会话已关闭", userId);
            return;
        }

        try {
            // 服务端向客户端发送消息的核心代码
            // session：代表一个用户的WebSocket连接
            // getBasicRemote()：获取消息发送接口
            // sendText(message)：发送文本消息到客户端
            session.getBasicRemote().sendText(message);
            log.debug("消息发送成功，userId: {}", userId);
        } catch (Exception e) {
            log.error("发送消息异常，userId: {}", userId, e);
            SESSION_MAP.remove(userId, session);
        }
    }

    /**
     * 获取当前在线连接数（用于监控）
     *
     * @return 在线连接数
     */
    public static int getOnlineCount() {
        return SESSION_MAP.size();
    }

    /**
     * 检查用户是否在线
     *
     * @param userId 用户ID
     * @return true-在线，false-不在线
     */
    public static boolean isOnline(String userId) {
        if (StrUtil.isBlank(userId)) {
            return false;
        }
        Session session = SESSION_MAP.get(userId);
        return session != null && session.isOpen();
    }
}