package TankWar;

import java.awt.*;
import java.io.Serializable;

public class Bullet implements Serializable {
    private int x, y;
    private final int speed;
    private final int direction;
    private final int size = 6;
    private final boolean formTankA;
    private boolean active = true;

    public Bullet(int x, int y, int direction, boolean formTankA) {
        this.y = y;
        this.x = x;
        this.speed = 10;//子弹速度
        this.direction = direction;
        this.formTankA = formTankA;
    }

    public void move() {
        switch (direction) {
            case 0:
                x -= speed;
                break;
            case 1:
                y -= speed;
                break;
            case 2:
                x += speed;
                break;
            case 3:
                y += speed;
                break;
        }
    }

    public void draw(Graphics2D g2d) {
        if (active) {
            g2d.setColor(Color.black);
            g2d.fillOval(x, y, size, size);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isFormTankA() {
        return formTankA;
    }

}
