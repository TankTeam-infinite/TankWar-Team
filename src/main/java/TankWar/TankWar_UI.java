package TankWar;

import javax.swing.*;
import java.awt.*;

public class TankWar_UI {
    public void initUI() {
        JFrame jf = new JFrame();
        jf.setSize(1300, 910);
        jf.setTitle("---Tank War (OnLine)---");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setResizable(false);
        jf.setLocationRelativeTo(null);

        //初始化CS双端机器,网络设置对话框
        NetworkSetupDialog setupDialog = new NetworkSetupDialog(jf);
        setupDialog.setVisible(true);

        //创建游戏面板
        GamePanel gamePanel = new GamePanel(setupDialog.isHost(),setupDialog.getServerIP());
        gamePanel.setBackground(Color.WHITE);

        //创建分数面板
        ScorePanel scorePanel = new ScorePanel();
        scorePanel.setBackground(Color.WHITE);

        //创建聊天面板
        ChatPanel chatPanel = new ChatPanel(gamePanel);

        //创建中控大面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(gamePanel, BorderLayout.CENTER);
        centerPanel.add(chatPanel,BorderLayout.EAST);

        //JFrame布局
        jf.setLayout(new BorderLayout());//划分为东西南北中
        jf.add(scorePanel, BorderLayout.SOUTH);
        jf.add(centerPanel,BorderLayout.CENTER);
        jf.setVisible(true);

        gamePanel.requestFocus();//游戏区获取焦点
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TankWar_UI tankWarUi = new TankWar_UI();
            tankWarUi.initUI();
        });
    }
}