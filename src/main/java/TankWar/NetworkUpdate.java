package TankWar;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class NetworkUpdate implements Serializable {//用于网络更新的对象
    public int sequence;//序列号
    public long timestamp;//时间戳
    public TankA tankA;
    public TankB tankB;
    public List<Bullet> bullets;
    public Set<Integer> keysPressed;

    public NetworkUpdate() {

    }
}

