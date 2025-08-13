package TankWar;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class GamePanel extends JPanel implements KeyListener, Serializable {//GamePanel类是游戏的核心控制器，负责管理游戏循环、输入处理和游戏状态更新。
    //游戏实体
    private TankA tankA;
    private TankB tankB;
    private final CopyOnWriteArrayList<Bullet> bullets = new CopyOnWriteArrayList<>();

    //游戏状态
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Timer gameTimer;
    private final Random ran = new Random();
    private final BattleMaps map;
    private final ScorePanel sPanel;
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
    private final AtomicBoolean running = new AtomicBoolean(true);

    //聊天回馈
    private ChatCallback chatCallback;

    //网络同步相关变量
    private static final int NETWORK_FPS = 30;//降低网络同步频率
    private final Timer networkSendTimer;
    private final Queue<Set<Integer>> inputQueue = new ConcurrentLinkedQueue<>();

    //碰撞检测变量
    private int prevTankAX, prevTankAY;
    private int prevTankBX, prevTankBY;

    public boolean isHost() {
        return isHost;
    }

    private static class GameState implements Serializable {//Serializable 是一个“空接口”，但它是 Java 对象能否被序列化的“通行证”。
        int tankAX, tankAY, tanADir;
        int tankBX, tankBY, tanBDir;
        List<BulletData> bullets;

        public GameState(TankA a, TankB b, List<Bullet> bullets) {
            this.tankAX = a.getX();
            this.tankAY = a.getY();
            this.tanADir = a.getDirection();
            this.tankBX = b.getX();
            this.tankBY = b.getY();
            this.tanBDir = b.getDirection();
            this.bullets = new ArrayList<>();
            for (Bullet bullet : bullets) {
                this.bullets.add(new BulletData(bullet));
            }
        }

        public void apply(GamePanel game) {
            game.tankA.setX(tankAX);
            game.tankA.setY(tankAY);
            game.tankB.setX(tankBX);
            game.tankB.setY(tankBY);
            game.tankA.setDirection(tanADir);
            game.tankB.setDirection(tanBDir);

            //更新子弹
            game.bullets.clear();
            for (BulletData data : bullets) {
                game.bullets.add(data.toBullet());
            }
        }
    }

    public GamePanel(boolean isHost, String serverIP) {
        this.isHost = isHost;
        this.serverIP = serverIP;

        map = new BattleMaps();
        sPanel = new ScorePanel();

        //初始化网络
        initNetwork();

        // 生成坦克A/B的合法位置
        if (isHost) {
            tankA = generatePositionA(45, 35);
            tankB = generatePositionB(45, 35);

            //保存初始位置
            prevTankAX = tankA.getX();
            prevTankAY = tankA.getY();
            prevTankBX = tankB.getX();
            prevTankBY = tankB.getY();
            sendInitialPosition();
        } else {
            // 客户端等待初始位置
            tankA = new TankA(0, 0);
            tankB = new TankB(0, 0);
        }

        // 初始化游戏定时器（每16ms≈60FPS）
        //使用游戏循环（Timer）来定期处理按键状态，更新坦克位置。
        gameTimer = new Timer(16, e -> {
            if (!gameOver) {
                //保存坦克移动前的位置
                prevTankAX = tankA.getX();
                prevTankAY = tankA.getY();
                prevTankBX = tankB.getX();
                prevTankBY = tankB.getY();
                if (isHost) {
                    // 主机：处理所有游戏逻辑
                    processHostLogic();
                } else {
                    //客户端 只处理渲染和输出
                    processClientLogic();
                }
                repaint();// 请求重绘
            }else{
                pressedKeys.clear();
            }
        });
        gameTimer.start();

        // 网络同步循环 (30FPS)
        networkSendTimer = new Timer(1000 / NETWORK_FPS, e -> {
            if (running.get()) {
                sendNetworkUpdate();
            }
        });
        networkSendTimer.start();

        setFocusable(true);
        addKeyListener(this);
    }

    private void sendInitialPosition() {
        try {
            if (isHost && socket != null && !socket.isConnected()) {
                out.writeObject(new InitialPosition(tankA, tankB));
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("发送初始位置失败" + e.getMessage());
        }

    }

    private void sendNetworkUpdate() {
        if (gameOver || !running.get()) {
            return;
        }

        try {
            if (isHost) {
                // 主机：发送完整游戏状态
                if (socket != null && socket.isConnected()) {
                    out.writeObject(new GameState(tankA, tankB, bullets));
                    out.flush();
                }
            } else {
                // 客户端：发送按键输入
                Set<Integer> currentInput = new HashSet<>(pressedKeys);
                if (socket != null && socket.isConnected()) {
                    out.writeObject(currentInput);
                    out.flush();
                }
            }
        } catch (SocketException e) {
            System.err.println("连接已关闭,停止发送");
            running.set(false);
        } catch (Exception e) {
            System.err.println("网络发送错误" + e.getMessage());
        }
    }

    private void initNetwork() {//初始化CS架构
        try {
            if (isHost) {
                //作为主机
                serverSocket = new ServerSocket(8881);
                System.out.println("等待客户端连接中...");
                socket = serverSocket.accept();////
                System.out.println("客户端已连接!");

                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } else {
                //作为客户端
                System.out.println("正在连接服务器: " + serverIP);
                socket = new Socket(serverIP, 8881);
                System.out.println("已连接到服务器!");

                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                // 客户端等待初始位置
                Object initObj = in.readObject();
                if (initObj instanceof InitialPosition) {
                    InitialPosition init = (InitialPosition) initObj;
                    tankA.setX(init.tankAX);
                    tankB.setX(init.tankBX);
                    tankA.setY(init.tankAY);
                    tankB.setY(init.tankBY);

                    //保存初始位置
                    prevTankAX = tankA.getX();
                    prevTankAY = tankA.getY();
                    prevTankBX = tankB.getX();
                    prevTankBY = tankB.getY();

                }
            }

            //启动网络线程
            networkThread = new Thread(this::networkReceiveLoop);
            networkThread.setDaemon(true);///////////////////
            networkThread.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "网络异常:  " + e.getMessage(),
                    "连接失败!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void networkReceiveLoop() {
        try {
            while (running.get()&& !Thread.currentThread().isInterrupted()) {
                try {

                    if (in == null) {
                        break;
                    }
                    Object obj = in.readObject();
                    if (isHost) {
                        // 主机接收客户端输入
                        if (obj instanceof Set) {
                            @SuppressWarnings("unchecked")
                            Set<Integer> input = (Set<Integer>) obj;
                            inputQueue.add(input);
                        } else if (obj instanceof NetworkMessage) {
                            handleNetworkMessage((NetworkMessage) obj);
                        }
                    } else {
                        // 客户端接收游戏状态
                        if (obj instanceof NetworkMessage) {
                            handleNetworkMessage((NetworkMessage) obj);
                        } else if (obj instanceof GameState) {
                            GameState state = (GameState) obj;
                            state.apply(this);
                        }
                    }
                }catch (SocketException e){
                    System.out.println("连接断开: " + e.getMessage());
                    break;
                }catch (EOFException e){
                    System.out.println("连接正常关闭");
                    break;
                }
            }
        } catch (Exception e) {
                e.printStackTrace();
        } finally {
            System.out.println("网络线程退出");
            closeNetwork();
        }
    }

    @SuppressWarnings("unchecked")//忽略转型警告
    private void handleNetworkMessage(NetworkMessage message) {//管理网络信息

        if (message.type == MessageType.PLAYER_FIRE && isHost) {
            // 主机为客户端生成子弹
            Bullet bullet = createBullet(tankB, false);
            synchronized (bullets) {
                bullets.add(bullet);
            }
        } else if (message.type == MessageType.CHAT_MESSAGE && chatCallback != null) {
            chatCallback.onMessageReceived((String) message.data);
        }
    }

    private void sendNetworkMessage(NetworkMessage message) {//发送网络信息
        try {
            if (socket != null && socket.isConnected()) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            System.err.println("发送网络消息失败: " + e.getMessage());
        }
    }

    private TankA generatePositionA(int width, int height) {
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
        Rectangle tempRect;
        int x, y;
        do {
            x = 120 + ran.nextInt(900);
            y = 60 + ran.nextInt(750);
            tempRect = new Rectangle(x, y, width, height);
        } while (map.isCollidingWithWall(tempRect)); // 确保不生成在墙上
        return new TankB(x, y); // 或 TankB
    }

    private void fireBullet(MoveObjects tank, boolean fromTankA) {
        Bullet bullet = createBullet(tank, fromTankA);
        synchronized (bullets) {
            bullets.add(bullet);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());

        if (e.getKeyCode() == KeyEvent.VK_Q && isHost) {
            // 主机本地开火
            fireBullet(tankA, true);
        } else if (e.getKeyCode() == KeyEvent.VK_SLASH && !isHost) {
            // 客户端发送开火请求
            sendNetworkMessage(new NetworkMessage(MessageType.PLAYER_FIRE, null));
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && chatCallback != null) {
            chatCallback.requestChatFocus();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void processHostLogic() {

        //1.处理客户端输入
        processClientInput();

        //2.处理本地坦克移动(TankA)
        processTankInput(tankA, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S);

        //3. 更新坦克位置
        updateTankPosition(tankA);
        updateTankPosition(tankB);

        //4. 更新子弹
        updateBullets();

        if(gameOver){
            pressedKeys.clear();
        }
    }

    private void processClientLogic() {//客户端只收集输入，不执行游戏逻辑
        //处理本地输入
        processTankInput(tankB, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN);

        //更新坦克位置
        updateTankPosition(tankB);

        if(gameOver){
            pressedKeys.clear();
        }
    }

    private void processClientInput() {
        //处理所有排队中的客户端输入
        while (!inputQueue.isEmpty()) {
            Set<Integer> input = inputQueue.poll();
            // 使用专门的按键处理方法
            processTankInput(tankB, input, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN);
        }
    }

    private void processTankInput(MoveObjects tank, int leftKey, int rightKey, int upKey, int downKey) {
        tank.setSpeedX(0);
        tank.setSpeedY(0);

        if (pressedKeys.contains(leftKey)) {
            tank.setDirection(0);
            tank.setSpeedX(-6);
        }
        if (pressedKeys.contains(rightKey)) {
            tank.setDirection(2);
            tank.setSpeedX(6);
        }
        if (pressedKeys.contains(upKey)) {
            tank.setDirection(1);
            tank.setSpeedY(-6);
        }
        if (pressedKeys.contains(downKey)) {
            tank.setDirection(3);
            tank.setSpeedY(6);
        }

    }

    private void processTankInput(MoveObjects tank, Set<Integer> keys,
                                  int leftKey, int rightKey, int upKey, int downKey) {
        if (keys == null) return;

        tank.setSpeedX(0);
        tank.setSpeedY(0);

        if (keys.contains(leftKey)) {
            tank.setDirection(0);
            tank.setSpeedX(-6);
        }
        if (keys.contains(rightKey)) {
            tank.setDirection(2);
            tank.setSpeedX(6);
        }
        if (keys.contains(upKey)) {
            tank.setDirection(1);
            tank.setSpeedY(-6);
        }
        if (keys.contains(downKey)) {
            tank.setDirection(3);
            tank.setSpeedY(6);
        }
    }

    private void updateTankPosition(MoveObjects tank) {
        //移动坦克
        tank.move();

        // 碰撞检测
        Rectangle bounds = tank.getBounds();
        boolean collision = false;

        // 检测墙壁碰撞
        if (map.isCollidingWithWall(bounds)) {
            collision = true;
        }

        // 检测坦克间碰撞
        if (tank == tankA && tankA.getBounds().intersects(tankB.getBounds())) {
            collision = true;
        }
        if (tank == tankB && tankB.getBounds().intersects(tankA.getBounds())) {
            collision = true;
        }

        //处理碰撞
        if (collision) {
            //回退到移动前的位置
            if (tank == tankA) {
                tankA.setX(prevTankAX);
                tankA.setY(prevTankAY);
            } else {
                tankB.setX(prevTankBX);
                tankB.setY(prevTankBY);
            }
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
        Graphics2D g2d = (Graphics2D) g;//创建图形上下文副本

        // 绘制地图
        map.paintMap(g2d);

        // 绘制坦克
        tankA.drawTankA(g2d);
        tankB.drawTankB(g2d);

        //绘制所有子弹
        for (Bullet bullet : bullets) {
            if (bullet.isActive()) {
                bullet.draw(g2d);
            }
        }

        //绘制分数面板
        sPanel.drawTankPicture(g2d);

        // 绘制玩家信息
        drawPlayerInfo(g2d);

        //绘制游戏结束信息
        if (gameOver) {
            showGameOver();
        }
    }

    private void drawPlayerInfo(Graphics2D g2d) {
        //绘制玩家标识
        g2d.setColor(Color.black);

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        if (isHost) {
            g2d.drawString("You (Host) - TankA ", 50, 30);
        } else {
            g2d.drawString("You (Client) - TankB ", 50, 30);
        }

        //绘制控制说明
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("-按下回车键以开启对话-", 50, 100);
        g2d.dispose();//保证图形状态隔离///
    }

    private void updateBullets() {
        if (gameOver) return;

        // 创建副本避免并发修改
        List<Bullet> bulletsCopy = new ArrayList<>(bullets);

        //更新子弹位置
        for (Bullet bullet : bulletsCopy) {
            if (!bullet.isActive()) {
                continue;
            }

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
                    //bullet.setActive(false);
                    showGameOver();
                } else if (!bullet.isFormTankA() && bullet.getBounds().intersects(tankA.getBounds())) {
                    gameOver = true;
                    winner = "-TankB-";
                    //bullet.setActive(false);
                    showGameOver();
                }
            }
        }

        bullets.removeIf(bullet -> !bullet.isActive());//移除不活跃的子弹
    }


    private void resetGame() {
        // 重置游戏前再次确保清除按键状态
        pressedKeys.clear();
        bullets.clear();

        if (isHost) {
            // 主机重新生成位置并发送
            tankA = generatePositionA(45, 35);
            tankB = generatePositionB(45, 35);

            //保存位置
            prevTankAX = tankA.getX();
            prevTankAY = tankA.getY();
            prevTankBX = tankB.getX();
            prevTankBY = tankB.getY();
            sendInitialPosition();
        } else {
            //客户端重置位置
            tankA.setX(prevTankAX);
            tankB.setY(prevTankBY);
            tankA.setY(prevTankAY);
            tankB.setX(prevTankBX);
        }

        gameOver = false;
        winner = "";

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

    public void setChatCallback(ChatCallback callback) {
        this.chatCallback = callback;
    }

    //发送消息
    public void sendChatMessage(String message) {
        sendNetworkMessage(new NetworkMessage(MessageType.CHAT_MESSAGE, message));
    }

    public void closeNetwork() {
        running.set(false);
        networkSendTimer.stop();

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭网络连接失败: " + e.getMessage());
        }

        System.out.println("网络资源已释放");
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

