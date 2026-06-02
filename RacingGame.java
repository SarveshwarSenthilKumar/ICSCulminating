import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class RacingGame extends JPanel implements ActionListener, KeyListener {

    final int WIDTH = 800;
    final int HEIGHT = 700;

    Timer timer;

    PlayerCar player;

    ArrayList<TrafficCar> traffic = new ArrayList<>();

    Random rand = new Random();

    boolean left;
    boolean right;
    boolean up;
    boolean down;

    int roadOffset = 0;

    int score = 0;
    int speed = 8;

    boolean gameOver = false;

    public RacingGame() {

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.GRAY);

        player = new PlayerCar(370, 550);

        addKeyListener(this);
        setFocusable(true);

        for (int i = 0; i < 6; i++) {
            spawnCar(-i * 150);
        }

        timer = new Timer(16, this);
        timer.start();
    }

    public void spawnCar(int y) {

        int lane = rand.nextInt(3);

        int x;

        if (lane == 0)
            x = 250;
        else if (lane == 1)
            x = 370;
        else
            x = 490;

        traffic.add(new TrafficCar(x, y));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (!gameOver) {

            updateGame();

        }

        repaint();
    }

    private void updateGame() {

        score++;

        if (score % 500 == 0) {
            speed++;
        }

        roadOffset += speed;

        if (roadOffset >= 40)
            roadOffset = 0;

        player.update();

        for (TrafficCar car : traffic) {

            car.y += speed;

            if (car.y > HEIGHT + 100) {

                car.y = -200;

                int lane = rand.nextInt(3);

                if (lane == 0)
                    car.x = 250;
                else if (lane == 1)
                    car.x = 370;
                else
                    car.x = 490;
            }

            if (player.getBounds().intersects(car.getBounds())) {
                gameOver = true;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        drawRoad(g2);

        for (TrafficCar car : traffic) {
            car.draw(g2);
        }

        player.draw(g2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("Score: " + score, 20, 40);

        g2.drawString("Speed: " + speed, 20, 80);

        if (gameOver) {

            g2.setColor(Color.RED);
            g2.setFont(new Font("Arial", Font.BOLD, 50));

            g2.drawString("GAME OVER", 240, 300);

            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("Press R to Restart", 290, 360);
        }
    }

    private void drawRoad(Graphics2D g2) {

        g2.setColor(new Color(50, 50, 50));
        g2.fillRect(200, 0, 400, HEIGHT);

        g2.setColor(Color.WHITE);

        for (int y = -40; y < HEIGHT; y += 80) {

            g2.fillRect(395, y + roadOffset, 10, 40);
        }

        g2.setColor(Color.YELLOW);

        g2.fillRect(200, 0, 10, HEIGHT);
        g2.fillRect(590, 0, 10, HEIGHT);
    }

    private void restartGame() {

        player.x = 370;
        player.y = 550;

        score = 0;
        speed = 8;

        traffic.clear();

        for (int i = 0; i < 6; i++) {
            spawnCar(-i * 150);
        }

        gameOver = false;
    }

    @Override
    public void keyPressed(KeyEvent e) {

        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT)
            left = true;

        if (key == KeyEvent.VK_RIGHT)
            right = true;

        if (key == KeyEvent.VK_UP)
            up = true;

        if (key == KeyEvent.VK_DOWN)
            down = true;

        if (key == KeyEvent.VK_R && gameOver) {
            restartGame();
        }

        player.left = left;
        player.right = right;
        player.up = up;
        player.down = down;
    }

    @Override
    public void keyReleased(KeyEvent e) {

        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT)
            left = false;

        if (key == KeyEvent.VK_RIGHT)
            right = false;

        if (key == KeyEvent.VK_UP)
            up = false;

        if (key == KeyEvent.VK_DOWN)
            down = false;

        player.left = left;
        player.right = right;
        player.up = up;
        player.down = down;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("Java Swing Racing Game");

        RacingGame game = new RacingGame();

        frame.add(game);

        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}

class PlayerCar {

    int x;
    int y;

    int width = 60;
    int height = 100;

    boolean left;
    boolean right;
    boolean up;
    boolean down;

    int speed = 7;

    public PlayerCar(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update() {

        if (left)
            x -= speed;

        if (right)
            x += speed;

        if (up)
            y -= speed;

        if (down)
            y += speed;

        if (x < 210)
            x = 210;

        if (x > 530)
            x = 530;

        if (y < 0)
            y = 0;

        if (y > 600)
            y = 600;
    }

    public void draw(Graphics2D g2) {

        g2.setColor(Color.BLUE);

        g2.fillRoundRect(x, y, width, height, 15, 15);

        g2.setColor(Color.BLACK);

        g2.fillRect(x + 8, y + 15, 15, 25);
        g2.fillRect(x + 37, y + 15, 15, 25);

        g2.fillRect(x + 8, y + 65, 15, 25);
        g2.fillRect(x + 37, y + 65, 15, 25);

        g2.setColor(Color.CYAN);

        g2.fillRect(x + 10, y + 10, 40, 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}

class TrafficCar {

    int x;
    int y;

    int width = 60;
    int height = 100;

    Color color;

    Random rand = new Random();

    public TrafficCar(int x, int y) {

        this.x = x;
        this.y = y;

        color = new Color(
                rand.nextInt(255),
                rand.nextInt(255),
                rand.nextInt(255));
    }

    public void draw(Graphics2D g2) {

        g2.setColor(color);

        g2.fillRoundRect(x, y, width, height, 15, 15);

        g2.setColor(Color.BLACK);

        g2.fillRect(x + 8, y + 15, 15, 25);
        g2.fillRect(x + 37, y + 15, 15, 25);

        g2.fillRect(x + 8, y + 65, 15, 25);
        g2.fillRect(x + 37, y + 65, 15, 25);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }
}