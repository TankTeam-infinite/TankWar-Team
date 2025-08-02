package TankWar;

import java.awt.*;
import java.io.Serializable;

public class MoveObjects implements Serializable {
    private int width;
    private int height;
    private int speedX;
    private int speedY;
    private int x, y;
    private int direction;

    public MoveObjects(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        x += speedX;
        y += speedY;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public void setSpeedY(int speedY) {
        this.speedY = speedY;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getSpeedY() {
        return speedY;
    }

    public int getSpeedX() {
        return speedX;
    }

}
