package TankWar;

import java.io.Serializable;

public class NetworkMessage implements Serializable {
    final MessageType type;
    final Object data;

    public NetworkMessage(MessageType type, Object data) {
        this.type = type;
        this.data = data;
    }
}
