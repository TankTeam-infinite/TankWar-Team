package TankWar;

import java.io.Serializable;

public class InitialPosition implements Serializable {
    final int tankAX, tankAY;
    final int tankBX, tankBY;

    public InitialPosition(TankA a, TankB b) {
        this.tankAX = a.getX();
        this.tankAY = a.getY();
        this.tankBX = b.getX();
        this.tankBY = b.getY();
    }
}
