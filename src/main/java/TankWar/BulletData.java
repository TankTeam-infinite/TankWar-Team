package TankWar;

import java.io.Serializable;

public class BulletData implements Serializable {
    int x, y, direction;
    boolean isFromTankA;
    boolean active;

    public BulletData(Bullet bullet) {
        this.x = bullet.getX();
        this.y = bullet.getY();
        this.direction = bullet.getDirection();
        this.isFromTankA = bullet.isFormTankA();
        this.active = bullet.isActive();
    }

    public Bullet toBullet() {
        Bullet bullet = new Bullet(x, y, direction, isFromTankA);
        bullet.setActive(active);
        return bullet;
    }
}
