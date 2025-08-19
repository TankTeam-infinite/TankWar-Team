package TankWar;

import java.awt.*;
import java.io.Serializable;

public class Bullet implements Serializable {
    private int x, y;
    private final int speed = 10;
    private final int direction;
    private final int size = 6;
    private final boolean formTankA;
    private boolean active = true;

    public Bullet(int x, int y, int direction, boolean formTankA) {
        this.y = y;
        this.x = x;
        this.direction = direction;
        this.formTankA = formTankA;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getSpeed() {
        return speed;
    }

    public int getDirection() {
        return direction;
    }

    public int getSize() {
        return size;
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
