package TankWar;

import javax.swing.*;
import java.awt.*;

public class NetworkSetupDialog extends JDialog {
    private boolean isHost = false;
    private String serverIP = "localhost";

    public NetworkSetupDialog(JFrame parent) {
        super(parent, "Network Setup", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(4, 1));////

        JLabel titleLabel = new JLabel("Select Game Mode", SwingConstants.CENTER);////
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));////
        add(titleLabel);

        JButton hostButton = new JButton("Host Game");
        hostButton.addActionListener(e -> {
            isHost = true;
            setVisible(false);
        });

        JButton joinButton = new JButton("Join Game");
        joinButton.addActionListener(e -> {
            isHost = false;
            serverIP = JOptionPane.showInputDialog(this, "Enter Server IP: ", "localhost");
            if (serverIP == null || serverIP.trim().isEmpty()) {////
                serverIP = "localhost";
            }
            setVisible(false);
        });

        add(hostButton);
        add(joinButton);
        JLabel noteLabel = new JLabel("<html><center>Host: Start a new game<br>Join: Connect to existing game</center></html>", SwingConstants.CENTER);////
        noteLabel.setFont(new Font("Arial",Font.PLAIN,12));////
        add(noteLabel);
    }

    public boolean isHost() {
        return isHost;
    }

    public String getServerIP(){
        return serverIP;
    }
}
