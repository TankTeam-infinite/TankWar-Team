package TankWar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GamePanel extends JPanel implements KeyListener {//GamePanel类是游戏的核心控制器，负责管理游戏循环、输入处理和游戏状态更新。
    private TankA tankA;
    private TankB tankB;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Timer gameTimer;
    private final Random ran = new Random();
    private final BattleMaps map;
    private final scorePanel sPanel;
    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private boolean gameOver = false;
    private String winner = "";

    //网络相关变量
    private final boolean isHost;
    private final String serverIP;
    private ServerSocket serverSocket;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Thread networkThread;

    //聊天回馈
    private ChatCallback chatCallback;

    public boolean isHost(){//////////////////////////////////////////////////////
        return isHost;
    }

    public GamePanel(boolean isHost, String serverIP) {
        this.isHost = isHost;
        this.serverIP = serverIP;

        map = new BattleMaps();
        sPanel = new scorePanel();

        // 生成坦克A/B的合法位置
        tankA = generatePositionA(45, 35);
        tankB = generatePositionB(45, 35);

        //从初始化网络
        initNetwork();

        // 初始化游戏定时器（每16ms≈60FPS）
        //使用游戏循环（Timer）来定期处理按键状态，更新坦克位置。
        gameTimer = new Timer(3, e -> {
            processInput();// 处理输入
            updateGame();// 更新游戏状态
            SwingUtilities.invokeLater(this::repaint);// 请求重绘
        });
        gameTimer.start();

        setFocusable(true);
        addKeyListener(this);
    }

    public void setChatCallback(ChatCallback callback) {
        this.chatCallback = callback;
    }

    private void initNetwork() {//初始化CS架构
        try {
            if (isHost) {
                //作为主机
                serverSocket = new ServerSocket(8881);
                System.out.println("等待客户端连接中...");
                socket = serverSocket.accept();////
                System.out.println("客户端已连接!");
            } else {
                //作为客户端
                System.out.println("正在连接服务器: " + serverIP);
                socket = new Socket(serverIP, 8881);
                System.out.println("已连接到服务器!");
            }

            //创建输入输出流
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            //启动网络线程
            networkThread = new Thread(this::receiveNetworkData);
            networkThread.start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "网络异常:  " + e.getMessage(),
                    "连接失败!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void receiveNetworkData() {//用于接收网络信息
        try {
            while (true) {
                NetworkMessage message = (NetworkMessage) in.readObject();
                handleNetworkMessage(message);
            }
        } catch (Exception e) {
            if (!socket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {//管理网络信息
        switch (message.type) {

            case TANK_UPDATE:
                //更新对方坦克
                if (isHost) {
                    tankB = (TankB) message.data;
                } else {
                    tankA = (TankA) message.data;
                }
                break;

            case BULLET_UPDATE:
                //更新对方的子弹
                Bullet bullet = (Bullet) message.data;
                bullets.add(bullet);
                break;
            case CHAT_MESSAGE:
                //更新聊天信息
                if (chatCallback != null) {
                    chatCallback.onMessageReceived((String) message.data);
                }
                break;
            case GAME_STATE:
                //更新游戏状态
                GameState state = (GameState) message.data;
                tankA = state.tankA;
                tankB = state.tankB;
                bullets.clear();
                bullets.addAll(state.bullets);
                break;
        }
    }

    private void sendNetworkMessage(NetworkMessage message) {//发送网络信息
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private TankA generatePositionA(int width, int height) {
//        Random ran = new Random();
        Rectangle tempRect;
        int x, y;
        do {
            x = 120 + ran.nextInt(900);
            y = 60 + ran.nextInt(750);
            tempRect = new Rectangle(x, y, width, height);
        } while (map.isCollidingWithWall(tempRect)); // 确保不生成在墙上
        return new TankA(x, y); // 或 TankB
    }

    private TankB generatePositionB(int width, int height) {
//        Random ran = new Random();
        Rectangle tempRect;
        int x, y;
        do {
            x = 120 + ran.nextInt(900);
            y = 60 + ran.nextInt(750);
            tempRect = new Rectangle(x, y, width, height);
        } while (map.isCollidingWithWall(tempRect)); // 确保不生成在墙上
        return new TankB(x, y); // 或 TankB
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());

        //添加子弹发射功能
        if (e.getKeyCode() == KeyEvent.VK_Q) {
            Bullet bullet = createBullet(tankA, true);
            bullets.add(bullet);

            //发送子弹信息
            sendNetworkMessage(new NetworkMessage(MessageType.BULLET_UPDATE, bullet));
        } else if (e.getKeyCode() == KeyEvent.VK_SLASH) {
            Bullet bullet = createBullet(tankB, false);
            bullets.add(bullet);

            //发送子弹信息
            sendNetworkMessage(new NetworkMessage(MessageType.BULLET_UPDATE, bullet));
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            //打开聊天输入框
            if (chatCallback != null) {
                chatCallback.requestChatFocus();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            pressedKeys.clear();// 在显示对话框前清除按键状态
        } else {
            pressedKeys.remove(e.getKeyCode());
        }// 处理平滑停止（可选）
        processInput();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void processInput() {
        if (gameOver) {
            return;
        }

        if (isHost) {//根据谁是主客来处理输入
            processTankInput(tankA, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S);
        } else {
            processTankInput(tankB, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN);
        }

        //发送坦克更新
        if (isHost) {
            sendNetworkMessage(new NetworkMessage(MessageType.TANK_UPDATE, tankA));
        } else {
            sendNetworkMessage(new NetworkMessage(MessageType.TANK_UPDATE, tankB));
        }
    }

    private void processTankInput(MoveObjects tank, int leftKey, int rightKey, int upKey, int downKey) {
        tank.setSpeedX(0);
        tank.setSpeedY(0);

        if (pressedKeys.contains(leftKey)) {
            tank.setDirection(0);
            tank.setSpeedX(-4);
        }
        if (pressedKeys.contains(rightKey)) {
            tank.setDirection(2);
            tank.setSpeedX(+4);
        }
        if (pressedKeys.contains(upKey)) {
            tank.setDirection(1);
            tank.setSpeedY(-4);
        }
        if (pressedKeys.contains(downKey)) {
            tank.setDirection(3);
            tank.setSpeedY(+4);
        }

    }

    private void handleTankMovement(MoveObjects tank) {
        //保存移动前的位置
        int oldX = tank.getX();
        int oldY = tank.getY();
        //移动坦克
        tank.move();
        // 获取移动后的碰撞区域
        Rectangle newBounds = tank.getBounds();

        //检测是否会与墙体/敌方坦克发生碰撞
        if (map.isCollidingWithWall(newBounds)) {//碰撞后回退位置并重置速度
            tank.setX(oldX);
            tank.setY(oldY);
            tank.setSpeedX(0);
            tank.setSpeedY(0);
        } else if (tankA.getBounds().intersects(tankB.getBounds())) {
            tank.setX(oldX);
            tank.setY(oldY);
            tank.setSpeedX(0);
            tank.setSpeedY(0);
        }
    }

    private Bullet createBullet(MoveObjects tank, boolean fromTankA) {
        int tankHeadX;
        int tankHeadY;
        if (tank.getDirection() == 0 || tank.getDirection() == 2) {
            tankHeadX = tank.getX() + (tank.getWidth() / 2);
            tankHeadY = tank.getY() + (tank.getHeight() / 2) - 2;
        } else {
            tankHeadX = tank.getX() + (tank.getHeight() / 2) - 2;
            tankHeadY = tank.getY() + (tank.getWidth() / 2);
        }
        return new Bullet(tankHeadX, tankHeadY, tank.getDirection(), fromTankA);
    }

    @Override
    protected void paintComponent(Graphics g) {//自动启用Swing双缓冲，避免闪烁
        super.paintComponent(g);// 清空背景，清除前一帧画面 确保每次绘制都是全新的画面，避免画面残留
        //底层原理：默认会使用组件的背景色填充整个区域
        Graphics2D g2d = (Graphics2D) g.create();//创建图形上下文副本
        map.paintMap(g2d);
        tankA.drawTankA(g2d);
        tankB.drawTankB(g2d);

        //绘制所有子弹
        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }

        sPanel.drawTankPicture(g2d);

        //绘制玩家标识
        g2d.setColor(Color.black);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        if (isHost) {
            g2d.drawString("You (Host) - TankA ", 50, 30);
            g2d.drawString("ENEMY - TankB", 50, 60);
        } else {
            g2d.drawString("You (Client) - TankB ", 50, 30);
            g2d.drawString("ENEMY - TankA", 50, 60);
        }

        //绘制控制说明
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("-按下回车键以开启对话-", 50, 100);
        g2d.dispose();//保证图形状态隔离///
    }

    private void updateGame() {
        if (gameOver) {
            return;
        }

        //if (isHost) {//主机处理所有游戏逻辑
            handleTankMovement(tankA);
            handleTankMovement(tankB);

            //更新子弹位置
            for (Bullet bullet : new ArrayList<>(bullets)) {
                bullet.move();
                //检测子弹与墙壁的碰撞
                if (map.isCollidingWithWall(bullet.getBounds())) {
                    bullet.setActive(false);
                }
                //检测子弹与坦克碰撞
                if (bullet.isActive()) {
                    if (bullet.isFormTankA() && bullet.getBounds().intersects(tankB.getBounds())) {
                        gameOver = true;
                        winner = "-TankA-";
                        bullet.setActive(false);
                        showGameOver();
                    } else if (!bullet.isFormTankA() && bullet.getBounds().intersects(tankA.getBounds())) {
                        gameOver = true;
                        winner = "-TankB-";
                        bullet.setActive(false);
                        showGameOver();
                    }
                }
            }
            bullets.removeIf(bullet -> !bullet.isActive());//移除不活跃的子弹

            //发送完整的游戏状态给客户端
            sendNetworkMessage(new NetworkMessage(MessageType.GAME_STATE, new GameState(tankA, tankB, new ArrayList<>(bullets))));

        //}
    }

    private void resetGame() {
        // 重置游戏前再次确保清除按键状态
        pressedKeys.clear();
        tankA = generatePositionA(45, 35);
        tankB = generatePositionB(45, 35);
        bullets.clear();
        winner = "";
        gameOver = false;
        requestFocusInWindow();
    }

    private void showGameOver() {
        pressedKeys.clear();// 在显示对话框前清除按键状态

        SwingUtilities.invokeLater(() -> {
            int option = JOptionPane.showConfirmDialog(
                    this, "  " + winner + "   Wins!!!\n WANT PLAY AGAIN?", "--Game Over--", JOptionPane.YES_NO_OPTION
            );

            if (option == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                System.exit(0);
            }
        });
    }

    //发送消息
    public void sendChatMessage(String message) {
        sendNetworkMessage(new NetworkMessage(MessageType.CHAT_MESSAGE, message));
    }

    //关闭网络连接
    public void closeNetwork(){
        try {
            if (socket != null) {
                socket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
/*

注意：

（1）Swing的绘图应该在事件分派线程（EDT）中进行，使用多线程可能导致不可预测的行为。因此，应该将所有绘
 图逻辑放在一个主循环中，使用Swing的Timer来定期触发重绘，而不是在独立的线程中使用while循环和
 Thread.sleep

（2）KeyListener的keyPressed事件机制设计为单次触发模式，无法跟踪组合按键状态
当同时按下多个键时，操作系统会快速交替触发多个keyPressed事件，但无法保持持续状态

（3）Swing使用被动绘制机制，应重写paintComponent()方法getGraphics()获取的是临
时图形上下文，无法持久化
*/

