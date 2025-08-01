package TankWar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ChatPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField inputField;

    public ChatPanel(GamePanel gamePanel) {
        setPreferredSize(new Dimension(250, 0));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("聊天"));

        //聊天消息显示区
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(240, 240, 240));
        chatArea.setLineWrap(true);////
        chatArea.setWrapStyleWord(true);////
        JScrollPane scrollPane = new JScrollPane(chatArea);

        //输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("发送");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        //设置聊天回调//////////////////////////
        gamePanel.setChatCallback(new ChatCallback() {
            @Override
            public void onMessageReceived(String message) {
                chatArea.append(message + "\n");
                chatArea.setCaretPosition(chatArea.getDocument().getLength());////
            }

            @Override
            public void requestChatFocus() {
                inputField.requestFocus();
            }
        });

        //发送消息的按钮事件处理
        ActionListener sendAction = e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                String formattedMessage = (gamePanel.isHost() ? "[Host]: " : "[Client]: " + message);
                chatArea.append(formattedMessage+"\n");
                gamePanel.sendChatMessage(formattedMessage);
                inputField.setText("");
            }
            gamePanel.requestFocus();
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

    }
}
