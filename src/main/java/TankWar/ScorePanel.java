package TankWar;

import javax.swing.*;
import java.awt.*;

public class ScorePanel extends JPanel {
    private final ImageIcon[] imgPic = new ImageIcon[2];

    public void drawTankPicture(Graphics2D g2d) {
        imgPic[0] = new ImageIcon("Photo\\TankB.png");
        imgPic[1] = new ImageIcon("Photo\\TankA.png");
        g2d.drawImage(imgPic[0].getImage(), 230, 810, 100, 60, null);
        g2d.drawImage(imgPic[1].getImage(), 830, 810, 100, 60, null);

        //绘制玩家标签
        g2d.setColor(Color.black);
        g2d.setFont(new Font("Arial",Font.BOLD,20));
        g2d.drawString("Player A:",140,850);
        g2d.drawString("Player B:",740,850);
    }
}
