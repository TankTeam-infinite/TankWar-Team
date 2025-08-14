package TankWar;

import javax.swing.*;
import java.awt.*;

public class NetworkSetupDialog extends JDialog {//初始化界面, 指明了Host和Client所在的机器
    private boolean isHost = false;
    private String serverIP = "localhost";

    public NetworkSetupDialog(JFrame parent) {
        super(parent, "Network Setup", true);// modal 指模态对话框: 阻塞其他窗口输入直到对话框关闭
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(4, 1));//GridLayout以网格形式排列容器中的组件,单元格大小全部相同,组件大小由布局决定,无法主动设置

        JLabel titleLabel = new JLabel("Select Game Mode", SwingConstants.CENTER);////
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));//设置标题字体(粗细,大小)
        add(titleLabel);

        JButton hostButton = new JButton("Host Game");
        hostButton.addActionListener(e -> {
            isHost = true;
            setVisible(false);
        });

        JButton joinButton = new JButton("Join Game");
        joinButton.addActionListener(e -> {
            isHost = false;
            serverIP = JOptionPane.showInputDialog(this, "请键入服务端IP地址: ", "localhost");
            if (serverIP == null || serverIP.trim().isEmpty()) {//.trim 去除了字符两端空白字符(空格,Tab,\n等)
                serverIP = "localhost";
            }
            setVisible(false);
        });

        add(hostButton);
        add(joinButton);
        JLabel noteLabel = new JLabel("<html><center>Host: Start a new game<br>Join: Connect to existing game</center></html>", SwingConstants.CENTER);////
        noteLabel.setFont(new Font("Arial",Font.PLAIN,12));////
        add(noteLabel);
    }

    public boolean isHost() {
        return isHost;
    }

    public String getServerIP(){
        return serverIP;
    }
}
