package TankWar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ChatPanel extends JPanel {
    private final JTextArea chatArea;
    private final JTextField inputField;

    public ChatPanel(GamePanel gamePanel) {
        setPreferredSize(new Dimension(250, 0));//东西组件宽度自定义高度随父组件填满
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("聊天"));

        //聊天消息显示区
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(218, 212, 212));
        chatArea.setLineWrap(true);//用于控制文本自动换行,当文本到达组件右边界时自动转到下一行
        chatArea.setWrapStyleWord(true);//与上方法配合使用, 用于检测文本换行时单词是否保持完整, 及在单词边界换行（不会切断单词）
        JScrollPane scrollPane = new JScrollPane(chatArea);

        //输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("发送");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        //设置聊天消息处理器
        gamePanel.setChatCallback(new ChatCallback() {
            @Override
            public void onMessageReceived(String message) {//接受并显示消息的方法
                chatArea.append(message + "\n");//将指定文本追加到 JTextArea 的末尾
                chatArea.setCaretPosition(chatArea.getDocument().getLength());//根据文档中当前包含的字符总数设置文本插入符（光标）的位置
                //即获取文本区域当前内容的长度并将光标移动到文本的末尾
            }

            @Override
            public void requestChatFocus() {
                inputField.requestFocus();
            }
        });

        //发送消息的按钮事件处理
        ActionListener sendAction = e -> {
            String message = inputField.getText().trim();//表示输入的文本不包括(空格, Tab, 回车)
            if (!message.isEmpty()) {
                String formattedMessage = (gamePanel.isHost() ? "[Host]: " : "[Client]: ") + message;
                chatArea.append(formattedMessage + "\n");
                gamePanel.sendChatMessage(formattedMessage);
                inputField.setText("");//清空输入框
            }
            gamePanel.requestFocus();
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);// JTextField 的内置行为
        // 当获得焦点的 JTextField 检测到回车键时：
        // 1. 自动触发所有注册的 ActionListener
        // 2. 执行 sendAction 逻辑
    }
}
