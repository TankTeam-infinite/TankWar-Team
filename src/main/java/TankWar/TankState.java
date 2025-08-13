package TankWar;

import java.io.Serializable;

//坦克状态容器
public class TankState implements Serializable {
    int tankAX, tankAY, tankADir;
    int tankBX, tankBY, tankBDir;
    long timestamp;
    int sequence;

    public TankState(TankA tankA, TankB tankB, int sequence) {
        this.tankAX = tankA.getX();
        this.tankAY = tankA.getY();
        this.tankADir = tankA.getDirection();
        this.tankBX = tankB.getX();
        this.tankBY = tankB.getY();
        this.tankBDir = tankB.getDirection();
        this.timestamp = System.currentTimeMillis();
        this.sequence = sequence;
    }

    public void applyTo(TankA tankA, TankB tankB) {
        tankA.setX(tankAX);
        tankB.setX(tankBX);
        tankB.setY(tankBY);
        tankA.setY(tankAY);
        tankA.setDirection(tankADir);
        tankB.setDirection(tankBDir);
    }
}