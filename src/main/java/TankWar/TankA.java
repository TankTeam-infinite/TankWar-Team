package TankWar;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

public class TankA extends MoveObjects implements Serializable {
    private final ImageIcon[] imgArr = new ImageIcon[4];

    public TankA(int x, int y) {
        super(x, y);
        setWidth(45);
        setHeight(35);
        setDirection(1);

        for (int i = 0; i < 4; i++) {
            imgArr[i] = new ImageIcon("Photo\\TankA" + i + ".png");
        }
    }

    public void drawTankA(Graphics2D g2d) {
        switch (getDirection()) {
            case (0)://左
                g2d.drawImage(imgArr[0].getImage(), getX(), getY(), getWidth(), getHeight(), null);
                break;
            case (1)://上
                g2d.drawImage(imgArr[1].getImage(), getX(), getY(), getHeight(), getWidth(), null);
                break;
            case (2)://右
                g2d.drawImage(imgArr[2].getImage(), getX(), getY(), getWidth(), getHeight(), null);
                break;
            case (3)://下
                g2d.drawImage(imgArr[3].getImage(), getX(), getY(), getHeight(), getWidth(), null);
                break;
        }
    }

    public Rectangle getBounds() {//获取边界方方法
        return new Rectangle(getX(), getY(), getWidth(), getHeight());
    }

    public void copyFrom(TankA source) {
        if (source == null) {
            return;
        }
        this.setX(source.getX());
        this.setY(source.getY());
        this.setDirection(source.getDirection());
    }

}
