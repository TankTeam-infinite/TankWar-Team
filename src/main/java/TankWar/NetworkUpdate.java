package TankWar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NetworkUpdate implements Serializable {
    final TankState state;
    final List<Bullet> bullets;
    final int sequence;

    public NetworkUpdate(TankState state, List<Bullet> bullets, int sequence) {
        this.state = state;
        this.sequence = sequence;
        this.bullets = new ArrayList<>(bullets);
    }
}