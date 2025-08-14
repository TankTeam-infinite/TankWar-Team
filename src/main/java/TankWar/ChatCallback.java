package TankWar;

public interface ChatCallback {//聊天消息处理器
    void onMessageReceived(String message);
    void requestChatFocus();
}
