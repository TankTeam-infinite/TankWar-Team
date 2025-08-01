package TankWar;

public interface ChatCallback {
    void onMessageReceived(String message);
    void requestChatFocus();
}
