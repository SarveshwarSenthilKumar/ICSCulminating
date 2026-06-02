import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class RacingGame extends JFrame {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int ROAD_WIDTH = 400;
    private static final int LANE_WIDTH = ROAD_WIDTH / 3;
    private static final int CAR_WIDTH = 50;
    private static final int CAR_HEIGHT = 80;
    
    private GamePanel gamePanel;
    private PlayerCar playerCar;
    private ArrayList<TrafficCar> trafficCars;
    private Random random;
    
    private int score;
    private int highScore;
    private int gameSpeed;
    private boolean gameOver;
    private boolean gameStarted;
    private boolean paused;
    
    private Timer gameTimer;
    private Timer trafficTimer;
    
    public RacingGame() {
        setTitle("Hill Climb Racing");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        
        random = new Random();
        trafficCars = new ArrayList<>();
        score = 0;
        highScore = 0;
        gameSpeed = 5;
        gameOver = false;
        gameStarted = false;
        paused = false;
        
        playerCar = new PlayerCar(WINDOW_WIDTH / 2 - CAR_WIDTH / 2, WINDOW_HEIGHT - CAR_HEIGHT - 20);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        setupControls();
        
        gameTimer = new Timer(20, e -> updateGame());
        trafficTimer = new Timer(2000, e -> spawnTrafficCar());
        
        setVisible(true);
    }
    
    private void setupControls() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!gameStarted) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        startGame();
                    }
                    return;
                }
                
                if (gameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        resetGame();
                    }
                    return;
                }
                
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    playerCar.moveLeft();
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerCar.moveRight();
                } else if (e.getKeyCode() == KeyEvent.VK_P) {
                    togglePause();
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    gameSpeed = Math.min(gameSpeed + 1, 15);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    gameSpeed = Math.max(gameSpeed - 1, 3);
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    playerCar.stopMoving();
                }
            }
        });
    }
    
    private void startGame() {
        gameStarted = true;
        gameOver = false;
        gameTimer.start();
        trafficTimer.start();
    }
    
    private void resetGame() {
        score = 0;
        gameSpeed = 5;
        gameOver = false;
        trafficCars.clear();
        playerCar.resetPosition();
        gameTimer.start();
        trafficTimer.start();
    }
    
    private void togglePause() {
        paused = !paused;
        if (paused) {
            gameTimer.stop();
            trafficTimer.stop();
        } else {
            gameTimer.start();
            trafficTimer.start();
        }
    }
    
    private void updateGame() {
        if (gameOver || paused) return;
        
        playerCar.update();
        
        // Update traffic cars
        for (int i = trafficCars.size() - 1; i >= 0; i--) {
            TrafficCar car = trafficCars.get(i);
            car.update(gameSpeed);
            
            // Check collision
            if (checkCollision(playerCar, car)) {
                gameOver = true;
                gameTimer.stop();
                trafficTimer.stop();
                if (score > highScore) {
                    highScore = score;
                }
            }
            
            // Remove cars that have gone off screen
            if (car.getY() > WINDOW_HEIGHT) {
                trafficCars.remove(i);
                score += 10;
                
                // Increase difficulty
                if (score % 100 == 0) {
                    gameSpeed = Math.min(gameSpeed + 1, 15);
                    trafficTimer.setDelay(Math.max(trafficTimer.getDelay() - 100, 800));
                }
            }
        }
        
        gamePanel.repaint();
    }
    
    private void spawnTrafficCar() {
        if (gameOver || paused) return;
        
        int lane = random.nextInt(3);
        int x = WINDOW_WIDTH / 2 - ROAD_WIDTH / 2 + lane * LANE_WIDTH + (LANE_WIDTH - CAR_WIDTH) / 2;
        
        // Check if lane is clear
        for (TrafficCar car : trafficCars) {
            if (car.getY() < CAR_HEIGHT + 50 && Math.abs(car.getX() - x) < LANE_WIDTH) {
                return; // Lane not clear
            }
        }
        
        trafficCars.add(new TrafficCar(x, -CAR_HEIGHT));
    }
    
    private boolean checkCollision(PlayerCar player, TrafficCar traffic) {
        Rectangle playerRect = new Rectangle(player.getX(), player.getY(), CAR_WIDTH, CAR_HEIGHT);
        Rectangle trafficRect = new Rectangle(traffic.getX(), traffic.getY(), CAR_WIDTH, CAR_HEIGHT);
        return playerRect.intersects(trafficRect);
    }
    
    private class GamePanel extends JPanel {
        private int roadOffset = 0;
        
        public GamePanel() {
            setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
            setFocusable(true);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw background
            g2d.setColor(new Color(34, 139, 34));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            // Draw grass
            g2d.setColor(new Color(50, 205, 50));
            g2d.fillRect(0, 0, WINDOW_WIDTH / 2 - ROAD_WIDTH / 2, WINDOW_HEIGHT);
            g2d.fillRect(WINDOW_WIDTH / 2 + ROAD_WIDTH / 2, 0, WINDOW_WIDTH / 2 - ROAD_WIDTH / 2, WINDOW_HEIGHT);
            
            // Draw road
            g2d.setColor(new Color(80, 80, 80));
            g2d.fillRect(WINDOW_WIDTH / 2 - ROAD_WIDTH / 2, 0, ROAD_WIDTH, WINDOW_HEIGHT);
            
            // Draw road borders
            g2d.setColor(Color.WHITE);
            g2d.fillRect(WINDOW_WIDTH / 2 - ROAD_WIDTH / 2 - 5, 0, 5, WINDOW_HEIGHT);
            g2d.fillRect(WINDOW_WIDTH / 2 + ROAD_WIDTH / 2, 0, 5, WINDOW_HEIGHT);
            
            // Draw lane markings
            if (!paused) {
                roadOffset = (roadOffset + gameSpeed) % 40;
            }
            g2d.setColor(Color.WHITE);
            for (int i = 0; i < WINDOW_HEIGHT + 40; i += 40) {
                int y = i + roadOffset - 40;
                if (y >= 0 && y < WINDOW_HEIGHT) {
                    g2d.fillRect(WINDOW_WIDTH / 2 - LANE_WIDTH / 2 - 2, y, 4, 20);
                    g2d.fillRect(WINDOW_WIDTH / 2 + LANE_WIDTH / 2 - 2, y, 4, 20);
                }
            }
            
            // Draw traffic cars
            for (TrafficCar car : trafficCars) {
                drawCar(g2d, car.getX(), car.getY(), car.getColor(), false);
            }
            
            // Draw player car
            drawCar(g2d, playerCar.getX(), playerCar.getY(), Color.RED, true);
            
            // Draw HUD
            drawHUD(g2d);
            
            // Draw start screen
            if (!gameStarted) {
                drawStartScreen(g2d);
            }
            
            // Draw game over screen
            if (gameOver) {
                drawGameOverScreen(g2d);
            }
            
            // Draw pause screen
            if (paused) {
                drawPauseScreen(g2d);
            }
        }
        
        private void drawCar(Graphics2D g2d, int x, int y, Color color, boolean isPlayer) {
            // Car body
            g2d.setColor(color);
            g2d.fillRect(x, y, CAR_WIDTH, CAR_HEIGHT);
            
            // Car roof
            g2d.setColor(color.darker());
            g2d.fillRect(x + 5, y + 15, CAR_WIDTH - 10, CAR_HEIGHT - 35);
            
            // Windshield
            g2d.setColor(new Color(135, 206, 250));
            if (isPlayer) {
                g2d.fillRect(x + 8, y + 20, CAR_WIDTH - 16, 15);
            } else {
                g2d.fillRect(x + 8, y + CAR_HEIGHT - 35, CAR_WIDTH - 16, 15);
            }
            
            // Wheels
            g2d.setColor(Color.BLACK);
            g2d.fillRect(x - 5, y + 10, 8, 15);
            g2d.fillRect(x + CAR_WIDTH - 3, y + 10, 8, 15);
            g2d.fillRect(x - 5, y + CAR_HEIGHT - 25, 8, 15);
            g2d.fillRect(x + CAR_WIDTH - 3, y + CAR_HEIGHT - 25, 8, 15);
            
            // Headlights/taillights
            if (isPlayer) {
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(x + 5, y + CAR_HEIGHT - 5, 10, 5);
                g2d.fillRect(x + CAR_WIDTH - 15, y + CAR_HEIGHT - 5, 10, 5);
            } else {
                g2d.setColor(Color.RED);
                g2d.fillRect(x + 5, y + CAR_HEIGHT - 5, 10, 5);
                g2d.fillRect(x + CAR_WIDTH - 15, y + CAR_HEIGHT - 5, 10, 5);
            }
        }
        
        private void drawHUD(Graphics2D g2d) {
            // Score panel
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(10, 10, 200, 80);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            g2d.drawString("Score: " + score, 20, 35);
            g2d.drawString("High Score: " + highScore, 20, 55);
            g2d.drawString("Speed: " + gameSpeed, 20, 75);
            
            // Controls hint
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.drawString("← → : Move | P : Pause", 10, WINDOW_HEIGHT - 10);
        }
        
        private void drawStartScreen(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String title = "HILL CLIMB RACING";
            g2d.drawString(title, (WINDOW_WIDTH - fm.stringWidth(title)) / 2, WINDOW_HEIGHT / 3);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2d.getFontMetrics();
            String instruction = "Press ENTER to Start";
            g2d.drawString(instruction, (WINDOW_WIDTH - fm.stringWidth(instruction)) / 2, WINDOW_HEIGHT / 2);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            String controls = "Controls: ← → to move, ↑ ↓ to change speed, P to pause";
            fm = g2d.getFontMetrics();
            g2d.drawString(controls, (WINDOW_WIDTH - fm.stringWidth(controls)) / 2, WINDOW_HEIGHT / 2 + 50);
        }
        
        private void drawGameOverScreen(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String gameOver = "GAME OVER";
            g2d.drawString(gameOver, (WINDOW_WIDTH - fm.stringWidth(gameOver)) / 2, WINDOW_HEIGHT / 3);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            fm = g2d.getFontMetrics();
            String scoreText = "Score: " + score;
            g2d.drawString(scoreText, (WINDOW_WIDTH - fm.stringWidth(scoreText)) / 2, WINDOW_HEIGHT / 2);
            
            String highScoreText = "High Score: " + highScore;
            g2d.drawString(highScoreText, (WINDOW_WIDTH - fm.stringWidth(highScoreText)) / 2, WINDOW_HEIGHT / 2 + 40);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2d.getFontMetrics();
            String restart = "Press ENTER to Restart";
            g2d.drawString(restart, (WINDOW_WIDTH - fm.stringWidth(restart)) / 2, WINDOW_HEIGHT / 2 + 100);
        }
        
        private void drawPauseScreen(Graphics2D g2d) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String pause = "PAUSED";
            g2d.drawString(pause, (WINDOW_WIDTH - fm.stringWidth(pause)) / 2, WINDOW_HEIGHT / 2);
            
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            fm = g2d.getFontMetrics();
            String resume = "Press P to Resume";
            g2d.drawString(resume, (WINDOW_WIDTH - fm.stringWidth(resume)) / 2, WINDOW_HEIGHT / 2 + 50);
        }
    }
    
    private class PlayerCar {
        private int x;
        private int y;
        private int speed;
        private boolean movingLeft;
        private boolean movingRight;
        
        public PlayerCar(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 8;
            this.movingLeft = false;
            this.movingRight = false;
        }
        
        public void update() {
            if (movingLeft) {
                x -= speed;
                // Keep on road
                if (x < WINDOW_WIDTH / 2 - ROAD_WIDTH / 2) {
                    x = WINDOW_WIDTH / 2 - ROAD_WIDTH / 2;
                }
            }
            if (movingRight) {
                x += speed;
                // Keep on road
                if (x > WINDOW_WIDTH / 2 + ROAD_WIDTH / 2 - CAR_WIDTH) {
                    x = WINDOW_WIDTH / 2 + ROAD_WIDTH / 2 - CAR_WIDTH;
                }
            }
        }
        
        public void moveLeft() {
            movingLeft = true;
        }
        
        public void moveRight() {
            movingRight = true;
        }
        
        public void stopMoving() {
            movingLeft = false;
            movingRight = false;
        }
        
        public void resetPosition() {
            x = WINDOW_WIDTH / 2 - CAR_WIDTH / 2;
            movingLeft = false;
            movingRight = false;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
    }
    
    private class TrafficCar {
        private int x;
        private int y;
        private Color color;
        private int speed;
        
        public TrafficCar(int x, int y) {
            this.x = x;
            this.y = y;
            this.speed = 3 + random.nextInt(3);
            this.color = getRandomColor();
        }
        
        public void update(int gameSpeed) {
            y += gameSpeed - speed;
            if (y < -CAR_HEIGHT) {
                y = -CAR_HEIGHT;
            }
        }
        
        private Color getRandomColor() {
            Color[] colors = {
                Color.BLUE, Color.GREEN, Color.ORANGE, 
                Color.PINK, Color.CYAN, Color.MAGENTA,
                new Color(128, 0, 128), new Color(255, 140, 0)
            };
            return colors[random.nextInt(colors.length)];
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public Color getColor() {
            return color;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RacingGame();
        });
    }
}
