import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Image;
import javax.swing.ImageIcon;

// Entry point
public class SnakeGame {

    public static void main(String[] args) {
        // Set up the GUI on the event dispatch thread
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // initialize and add the welcome screen panel
            WelcomePanel welcomePanel = new WelcomePanel();
            frame.add(welcomePanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // start the game when the button is clicked
            welcomePanel.getStartButton().addActionListener(e -> {
                frame.remove(welcomePanel);
                GamePanel gamePanel = new GamePanel();
                frame.add(gamePanel);
                frame.revalidate();
                gamePanel.requestFocusInWindow(); // Request focus for the new GamePanel
            });
        });
    }
}

// WelcomePanel class for the welcome screen of the game
class WelcomePanel extends JPanel {
    private JButton startButton;

    WelcomePanel() {
        // Set up the welcome screen layout and components
        this.setPreferredSize(new Dimension(GamePanel.SCREEN_WIDTH, GamePanel.SCREEN_HEIGHT));
        this.setBackground(Color.black);

        JLabel titleLabel = new JLabel("Snake Game");
        titleLabel.setFont(new Font("Cambria", Font.BOLD, 50));
        titleLabel.setForeground(Color.GREEN);

        JTextArea rulesTextArea = new JTextArea();
        rulesTextArea.append("Welcome to Snake Game!\n\n");
        rulesTextArea.append("Rules:\n");
        rulesTextArea.append("- Eat apples to gain higher scores.\n");
        rulesTextArea.append("- Red power-up for 10s gives you double points.\n");
        rulesTextArea.append("- Green power-up makes you go through yourself for 10s.\n");
        rulesTextArea.append("- Blue power-up speeds you up for 5s.\n");

        rulesTextArea.setEditable(false);
        rulesTextArea.setOpaque(false);
        rulesTextArea.setForeground(Color.white);
        rulesTextArea.setFont(new Font("Cambria", Font.PLAIN, 20));

        startButton = new JButton("Start Game");
        startButton.setBackground(Color.BLACK);
        startButton.setForeground(Color.WHITE);
        startButton.setFont(new Font("Cambria", Font.BOLD, 20));

        // Center the components using GridBagLayout
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.black);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(titleLabel, gbc);
        gbc.gridy = 1;
        centerPanel.add(rulesTextArea, gbc);
        gbc.gridy = 2;
        centerPanel.add(startButton, gbc);

        this.setLayout(new BorderLayout());
        this.add(centerPanel, BorderLayout.CENTER);

        this.setFocusable(true);
    }

    // Getter for the 'Start Game' button
    public JButton getStartButton() {
        return startButton;
    }
}

// GamePanel class represents the main gameplay panel
class GamePanel extends JPanel implements ActionListener {

    // Constants defining the game dimensions and properties
    public static final int SCREEN_WIDTH = 1300;
    public static final int SCREEN_HEIGHT = 750;
    public static final int UNIT_SIZE = 50;
    public static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
    public static final int DELAY = 175;

    // Variables for various game states and components
    private boolean goThroughSelfActive = false;
    private boolean doublePointsActive = false;
    private Timer doublePointsTimer;
    private int[] x = new int[GAME_UNITS];
    private int[] y = new int[GAME_UNITS];
    private int bodyParts = 6;
    private int applesEaten;
    private int appleX;
    private int appleY;
    private int lastPowerUpType = -1;
    private Direction direction = Direction.RIGHT;
    private boolean running = false;
    private Timer timer;
    private Random random;
    private int highestScore;
    private ArrayList<PowerUp> powerUps = new ArrayList<>();
    private JButton restartButton;
    private JButton exitButton;
    private Timer speedUpTimer;
    private Timer goThroughSelfTimer;
    private ArrayList<String> powerUpTexts = new ArrayList<>();
    private Image appleImage = loadAppleImage(); // Load the apple image

    // Constructor for the GamePanel
    GamePanel() {
        // Initialization of game-related variables and components
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.black);
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        startGame(); // Start the game logic
        highestScore = 0;
        appleImage = loadAppleImage(); // Load the apple image

        // Set up restart and exit buttons
        restartButton = new JButton("Restart");
        exitButton = new JButton("Exit");
        restartButton.setBounds(SCREEN_WIDTH / 4, SCREEN_HEIGHT / 2 + 300, SCREEN_WIDTH / 4, 60);
        exitButton.setBounds(SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 300, SCREEN_WIDTH / 4, 60);
        restartButton.addActionListener(e -> restartGame());
        exitButton.addActionListener(e -> System.exit(0));
        restartButton.setBackground(Color.BLACK);
        restartButton.setForeground(Color.WHITE);
        restartButton.setFont(new Font("Cambria", Font.BOLD, 20));
        exitButton.setBackground(Color.BLACK);
        exitButton.setForeground(Color.WHITE);
        exitButton.setFont(new Font("Cambria", Font.BOLD, 20));
        restartButton.setFocusPainted(false);
        exitButton.setFocusPainted(false);
        restartButton.setBorder(BorderFactory.createEmptyBorder());
        exitButton.setBorder(BorderFactory.createEmptyBorder());
        this.add(restartButton);
        this.add(exitButton);
        this.setLayout(null);
        restartButton.setVisible(false);
        exitButton.setVisible(false);
        speedUpTimer = new Timer(5000, e -> {
            timer.setDelay(DELAY);
            speedUpTimer.stop();
        });
        goThroughSelfTimer = new Timer(10000, e -> {
            goThroughSelfActive = false;
            goThroughSelfTimer.stop();
        });
    }

    // Load the apple image
    private Image loadAppleImage() {
        return new ImageIcon("images/appleImage.png").getImage();
    }

    // Start the game logic
    public void startGame() {
        newApple();
        running = true;
        timer = new Timer(DELAY, this);
        timer.start();
        generateRandomPowerUp();
    }

    // Custom paintComponent method for drawing the game
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (running) {
            draw(g);
            drawTimers(g);
        } else {
            gameOver(g);
        }
    }

    // draw various game elements (snake, apple, power-ups, score, etc.)
    private void draw(Graphics g) {
        if (running) {
            // Draw the apple image
            g.drawImage(appleImage, appleX, appleY, UNIT_SIZE, UNIT_SIZE, this);

            // Draw the power-ups
            for (PowerUp powerUp : powerUps) {
                drawPowerUp(g, powerUp);
            }

            // Draw the snake
            for (int i = 0; i < bodyParts; i++) {
                if (i == 0) {
                    g.setColor(Color.green);
                    g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                } else {
                    g.setColor(new Color(45, 180, 0));
                    g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                }
            }

            // draw the score and power-up texts
            g.setColor(Color.red);
            g.setFont(new Font("Cambria", Font.BOLD, 40));
            FontMetrics metrics = getFontMetrics(g.getFont());
            g.drawString("Score: " + applesEaten, (SCREEN_WIDTH - metrics.stringWidth("Score: " + applesEaten)) / 2, g.getFont().getSize());

            drawPowerUpTexts(g);
        } else {
            gameOver(g);
        }
    }

    // draw a specific power-up on the screen
    private void drawPowerUp(Graphics g, PowerUp powerUp) {
        int size = UNIT_SIZE;
        int px = powerUp.getX();
        int py = powerUp.getY();

        // draw the power-up image
        g.drawImage(powerUp.getImage(), px, py, size, size, this);
    }

    // draw the countdown timers for active power-ups
    private void drawTimers(Graphics g) {
        if (speedUpTimer.isRunning()) {
            showCountdown("Speed Up: ", 5); // Hardcoded to 5 seconds
        }

        if (goThroughSelfTimer.isRunning()) {
            showCountdown("Go Through Self: ", 10); // Hardcoded to 10 seconds
        }

        if (doublePointsActive) {
            showCountdown("Double XP: ", 10); // Hardcoded to 10 seconds
        }
    }

    // draw the power-up countdown texts on the screen
    private void drawPowerUpTexts(Graphics g) {
        int textY = 50; // Initial Y position for the text

        for (String powerUpText : powerUpTexts) {
            g.setColor(Color.white);
            g.setFont(new Font("Cambria", Font.BOLD, 20));
            g.drawString(powerUpText, 10, textY);
            textY += 25; // Increase Y position for the next text
        }

        powerUpTexts.clear(); // Clear the texts list after drawing
    }

    // Show countdown for a specific power-up
    private void showCountdown(String message, int seconds) {
        String powerUpText = message + seconds + "s";
        powerUpTexts.add(powerUpText);
    }

    // Generate a new apple at a random location
    public void newApple() {
        appleX = random.nextInt((int) (SCREEN_WIDTH / UNIT_SIZE)) * UNIT_SIZE;
        appleY = random.nextInt((int) (SCREEN_HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
    }

    // Generate a random power-up at a random location
    private void generateRandomPowerUp() {
        int randomX = random.nextInt((int) (SCREEN_WIDTH / UNIT_SIZE)) * UNIT_SIZE;
        int randomY = random.nextInt((int) (SCREEN_HEIGHT / UNIT_SIZE)) * UNIT_SIZE;

        // Randomly choose a power-up type based on spawn rates and cooldown
        PowerUpType type;
        do {
            int rand = random.nextInt(100);
            if (rand < 30) {
                type = PowerUpType.SPEED_UP; // 30% chance
            } else if (rand < 60) {
                type = PowerUpType.GO_THROUGH_SELF; // 30% chance
            } else {
                type = PowerUpType.DOUBLE_POINTS; // 40% chance
            }
        } while (type.ordinal() == lastPowerUpType); // Ensure it's not the same type consecutively

        lastPowerUpType = type.ordinal();

        PowerUp powerUp = new PowerUp(randomX, randomY, type);
        powerUps.add(powerUp);
    }

    // Move the snake based on the current direction
    public void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case UP:
                y[0] = y[0] - UNIT_SIZE;
                break;
            case DOWN:
                y[0] = y[0] + UNIT_SIZE;
                break;
            case LEFT:
                x[0] = x[0] - UNIT_SIZE;
                break;
            case RIGHT:
                x[0] = x[0] + UNIT_SIZE;
                break;
        }

        // Wrap around the screen if the snake reaches the screen edges
        if (x[0] >= SCREEN_WIDTH) x[0] = 0;
        if (x[0] < 0) x[0] = SCREEN_WIDTH - UNIT_SIZE;
        if (y[0] >= SCREEN_HEIGHT) y[0] = 0;
        if (y[0] < 0) y[0] = SCREEN_HEIGHT - UNIT_SIZE;
    }

    // Check if the snake has eaten the apple
    public void checkApple() {
        if ((x[0] == appleX) && (y[0] == appleY)) {
            bodyParts++;
            applesEaten++;

            // Check if the current score is higher than the highest score
            if (applesEaten > highestScore) {
                highestScore = applesEaten;
            }

            // If double points are active, increment the score again
            if (doublePointsActive) {
                applesEaten++;
            }

            newApple(); // Generate a new apple
        }
    }

    // Handle collisions with power-ups and update game state accordingly
    private void handlePowerUpCollision(PowerUp powerUp) {
        switch (powerUp.getType()) {
            case SPEED_UP:
                timer.setDelay(DELAY / 2);
                speedUpTimer.restart();
                showCountdown("Speed Up: ", 5); // Hardcoded to 5 seconds
                break;
            case GO_THROUGH_SELF:
                goThroughSelfActive = true;
                goThroughSelfTimer.restart();
                showCountdown("Go Through Self: ", 10); // Hardcoded to 10 seconds
                break;
            case DOUBLE_POINTS:
                if (!doublePointsActive) {
                    doublePointsActive = true;

                    // Start a timer for double points with a specified duration
                    doublePointsTimer = new Timer(10000, e -> {
                        doublePointsActive = false;
                        ((Timer) e.getSource()).stop();
                    });
                    doublePointsTimer.start();
                    showCountdown("Double XP: ", 10); // Hardcoded to 10 seconds
                }
                break;
        }
    }

    // Check for collisions (self-collision, power-up collisions, game over)
    public void checkCollisions() {
        if (!goThroughSelfActive) {
            // Check for self-collision only if 'goThroughSelf' is not active
            for (int i = bodyParts; i > 0; i--) {
                if ((x[0] == x[i]) && (y[0] == y[i])) {
                    running = false;
                    gameOver();
                }
            }
        }

        // Check for collisions with power-ups
        for (PowerUp powerUp : new ArrayList<>(powerUps)) {
            if (x[0] == powerUp.getX() && y[0] == powerUp.getY()) {
                handlePowerUpCollision(powerUp);
                powerUps.remove(powerUp);
                generateRandomPowerUp();
                break;
            }
        }

        // If the game is over, stop the timer and show restart/exit buttons
        if (!running) {
            timer.stop();
            restartButton.setVisible(true);
            exitButton.setVisible(true);

            // Ensure gameOver is called when the game is over
            gameOver();
            revalidate();
        }
    }

    // Display the game over screen
    private void gameOver() {
        repaint();
    }

    // Display the game over screen with specific information
    private void gameOver(Graphics g) {
        g.setColor(Color.red);
        g.setFont(new Font("Cambria", Font.BOLD, 75));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        g.drawString("Game Over", (SCREEN_WIDTH - metrics2.stringWidth("Game Over")) / 2, SCREEN_HEIGHT / 2 + 40);

        g.setFont(new Font("Cambria", Font.BOLD, 40));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        String scoreText = "Score: " + applesEaten;
        String highScoreText = "High Score: " + highestScore;

        g.drawString(scoreText, (SCREEN_WIDTH - metrics1.stringWidth(scoreText)) / 2, SCREEN_HEIGHT / 2 + 120);

        // Update highest score if needed
        if (applesEaten > highestScore) {
            highestScore = applesEaten;
        }

        g.drawString(highScoreText, (SCREEN_WIDTH - metrics1.stringWidth(highScoreText)) / 2, SCREEN_HEIGHT / 2 + 200);
    }


    // Restart the game with initial settings
    private void restartGame() {
        x = new int[GAME_UNITS];
        y = new int[GAME_UNITS];
        bodyParts = 6;
        applesEaten = 0;
        direction = Direction.RIGHT;
        newApple();
        running = true;
        restartButton.setVisible(false);
        exitButton.setVisible(false);
        timer.start();
        repaint();
    }

    // ActionListener implementation for game updates
    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint(); // Trigger repaint to update the game screen
    }

    // KeyAdapter class for handling keyboard input
    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A: // Added support for 'A' key
                    if (direction != Direction.RIGHT) {
                        direction = Direction.LEFT;
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D: // Added support for 'D' key
                    if (direction != Direction.LEFT) {
                        direction = Direction.RIGHT;
                    }
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W: // Added support for 'W' key
                    if (direction != Direction.DOWN) {
                        direction = Direction.UP;
                    }
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S: // Added support for 'S' key
                    if (direction != Direction.UP) {
                        direction = Direction.DOWN;
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    if (!running) {
                        restartGame();
                    }
                    break;
            }
        }
    }
}

// Enum for representing the directions of the snake
enum Direction {
    UP, DOWN, LEFT, RIGHT
}

// Class representing a power-up in the game
class PowerUp {
    private int x;
    private int y;
    private PowerUpType type;
    private Image image;

    // Constructor for creating a power-up with specified coordinates and type
    public PowerUp(int x, int y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.image = loadImage(type); // Load the image when creating a PowerUp object
    }

    // Getter method for X coordinate
    public int getX() {
        return x;
    }

    // Getter method for Y coordinate
    public int getY() {
        return y;
    }

    // Method to get the color based on the type if needed
    public Color getColor() {
        switch (type) {
            case SPEED_UP:
                return Color.BLUE;
            case GO_THROUGH_SELF:
                return new Color(0, 100, 0); // Dark Green
            case DOUBLE_POINTS:
                return Color.RED;
            default:
                return Color.BLACK; // Default color
        }
    }

    // Getter method for the power-up type
    public PowerUpType getType() {
        return type;
    }

    // Getter method for the power-up image
    public Image getImage() {
        return image;
    }

    // Load the image for a specific power-up type
    private Image loadImage(PowerUpType type) {
        switch (type) {
            case SPEED_UP:
                return new ImageIcon("images/speedUpImage.png").getImage();
            case GO_THROUGH_SELF:
                return new ImageIcon("images/goThroughSelfImage.png").getImage();
            case DOUBLE_POINTS:
                return new ImageIcon("images/doublePointsImage.png").getImage();
            default:
                return null; // No image for unknown power-up types
        }
    }
}

// Enum for representing the types of power-ups in the game
enum PowerUpType {
    SPEED_UP, GO_THROUGH_SELF, DOUBLE_POINTS
}
