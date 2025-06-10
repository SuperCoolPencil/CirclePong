import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class EnhancedCirclePong extends JPanel implements Runnable {

    // Game Constants
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 900;
    public static final int GAME_AREA_RADIUS = 350;
    private static final int PADDLE_WIDTH = 15;
    private static final int PADDLE_LENGTH = 70;
    private static final int BALL_DIAMETER = 15;
    private static final double PADDLE_MOVEMENT_SPEED = 0.045;
    private static final double INITIAL_BALL_SPEED = 4.0;

    // Game Components
    private Ball ball;
    private Paddle leftPaddle;
    private Paddle rightPaddle;
    private AiController leftAi;
    private AiController rightAi;
    private final List<Particle> particles = new ArrayList<>();

    // Game State
    private int leftPlayerScore = 0;
    private int rightPlayerScore = 0;
    private GameMode activeGameMode = GameMode.HUMAN_VS_AI;
    private final AtomicBoolean isGameRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    // Player Input
    private final boolean[] keyStates = new boolean[256];

    public EnhancedCirclePong() {
        setupWindow();
        initializeGameComponents();
        startGameLoop();
    }

    private void setupWindow() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(new Color(10, 10, 20));
        setFocusable(true);
        addKeyListener(new KeyInputAdapter());
    }

    private void initializeGameComponents() {
        Point gameCenter = new Point(WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
        ball = new Ball(gameCenter, INITIAL_BALL_SPEED, BALL_DIAMETER);

        leftPaddle = new Paddle(gameCenter, GAME_AREA_RADIUS, PADDLE_LENGTH, PADDLE_WIDTH, PADDLE_MOVEMENT_SPEED, new Color(0, 200, 255), 1, Math.PI);
        rightPaddle = new Paddle(gameCenter, GAME_AREA_RADIUS, PADDLE_LENGTH, PADDLE_WIDTH, PADDLE_MOVEMENT_SPEED, new Color(255, 80, 120), 2, 0);

        // Use new constructor: (responsiveness, accuracy, ...)
        leftAi = new AiController(0.1, 0.9, gameCenter, GAME_AREA_RADIUS);
        rightAi = new AiController(0.1, 0.9, gameCenter, GAME_AREA_RADIUS);
    }

    // NEW METHOD for difficulty adjustment
    private void adjustAIDifficulty(double responsivenessChange, double accuracyChange) {
        double newAccuracy = Math.max(0.2, Math.min(1.0, rightAi.getAccuracy() + accuracyChange));
        double newResponsiveness = Math.max(0.04, Math.min(0.25, rightAi.getResponsiveness() + responsivenessChange));

        leftAi.setAccuracy(newAccuracy);
        rightAi.setAccuracy(newAccuracy);
        leftAi.setResponsiveness(newResponsiveness);
        rightAi.setResponsiveness(newResponsiveness);
    }

    private void startGameLoop() {
        isGameRunning.set(true);
        new Thread(this).start();
    }

    @Override
    public void run() {
        final long frameTime = 1000 / 60; // 60 FPS
        while (isGameRunning.get()) {
            long startTime = System.currentTimeMillis();

            if (!isPaused.get()) {
                updateGameState();
            }
            repaint();

            long elapsedTime = System.currentTimeMillis() - startTime;
            long sleepTime = frameTime - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateGameState() {
        handlePlayerInput();
        updateAi();
        ball.update();
        handleCollisions();
        updateParticles();
    }

    private void handlePlayerInput() {
        if (activeGameMode == GameMode.TWO_HUMAN || activeGameMode == GameMode.HUMAN_VS_AI) {
            if (keyStates[KeyEvent.VK_W]) leftPaddle.move(-1);
            if (keyStates[KeyEvent.VK_S]) leftPaddle.move(1);
        }
        if (activeGameMode == GameMode.TWO_HUMAN) {
            if (keyStates[KeyEvent.VK_UP]) rightPaddle.move(-1);
            if (keyStates[KeyEvent.VK_DOWN]) rightPaddle.move(1);
        }
    }

    private void updateAi() {
        if (activeGameMode == GameMode.HUMAN_VS_AI) {
            rightAi.updatePaddle(rightPaddle, ball);
        } else if (activeGameMode == GameMode.TWO_AI || activeGameMode == GameMode.AI_SOLO) {
            leftAi.updatePaddle(leftPaddle, ball);
            if (activeGameMode != GameMode.AI_SOLO) {
                rightAi.updatePaddle(rightPaddle, ball);
            }
        }
    }

    private void handleCollisions() {
        double ballDistance = ball.getDistanceFromCenter();
        if (ballDistance >= GAME_AREA_RADIUS - ball.getSize() / 2.0) {
            double ballAngle = ball.getAngleFromCenter();
            boolean collisionOccurred = false;

            Paddle paddleToCheck = ball.getX() < getWidth() / 2.0 ? leftPaddle : rightPaddle;
            if (activeGameMode != GameMode.AI_SOLO && paddleToCheck == leftPaddle && paddleToCheck.isAngleWithinPaddle(ballAngle)) {
                ball.handlePaddleCollision(paddleToCheck.getAngle());
                createCollisionParticles(ball.getX(), ball.getY());
                collisionOccurred = true;
            } else if (paddleToCheck == rightPaddle && paddleToCheck.isAngleWithinPaddle(ballAngle)) {
                ball.handlePaddleCollision(paddleToCheck.getAngle());
                createCollisionParticles(ball.getX(), ball.getY());
                collisionOccurred = true;
            }

            if (!collisionOccurred) {
                if (ball.getX() < getWidth() / 2.0) rightPlayerScore++;
                else leftPlayerScore++;
                ball.reset();
            }
        }
    }

    private void createCollisionParticles(double x, double y) {
        for (int i = 0; i < 20; i++) {
            particles.add(new Particle(x, y));
        }
    }

    private void updateParticles() {
        particles.removeIf(Particle::isFaded);
        particles.forEach(Particle::update);
    }

    private void resetGame() {
        leftPlayerScore = 0;
        rightPlayerScore = 0;
        ball.reset();
        isPaused.set(false);
    }

    private void changeGameMode(GameMode newMode) {
        activeGameMode = newMode;
        resetGame();
        initializeGameComponents();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGameArena(g2d);
        drawGameElements(g2d);
        drawUserInterface(g2d);
    }

    private void drawGameArena(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        g2d.setColor(new Color(30, 30, 40));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(centerX - GAME_AREA_RADIUS, centerY - GAME_AREA_RADIUS, GAME_AREA_RADIUS * 2, GAME_AREA_RADIUS * 2);

        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
        g2d.drawLine(centerX, centerY - GAME_AREA_RADIUS, centerX, centerY + GAME_AREA_RADIUS);
    }

    private void drawGameElements(Graphics2D g2d) {
        particles.forEach(p -> p.draw(g2d));
        if (activeGameMode != GameMode.AI_SOLO) leftPaddle.draw(g2d);
        rightPaddle.draw(g2d);
        ball.draw(g2d, rightAi.predictBallInterceptAngle(ball));
    }

    private void drawUserInterface(Graphics2D g2d) {
        // ... (score and mode drawing is the same) ...
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
        g2d.setColor(Color.WHITE);

        String leftScoreText = String.format("%s: %d", activeGameMode.getLeftPlayerName(), leftPlayerScore);
        String rightScoreText = String.format("%s: %d", activeGameMode.getRightPlayerName(), rightPlayerScore);

        if (activeGameMode != GameMode.AI_SOLO) {
            g2d.drawString(leftScoreText, 30, 40);
        }
        g2d.drawString(rightScoreText, 30, 80);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("Mode: " + activeGameMode.getDisplayName(), getWidth() - 220, 40);

        // Display new AI stats
        g2d.drawString(String.format("AI Responsiveness: %.2f", rightAi.getResponsiveness()), getWidth() - 220, 65);
        g2d.drawString(String.format("AI Accuracy: %.2f", rightAi.getAccuracy()), getWidth() - 220, 90);
        g2d.drawString("-/+: Adjust Difficulty", getWidth() - 220, 115);

        if (isPaused.get()) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 50));
            g2d.setColor(new Color(255, 255, 255, 200));
            String pauseText = "PAUSED";
            FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(pauseText, (getWidth() - metrics.stringWidth(pauseText)) / 2, getHeight() / 2);
        }
    }

    private class KeyInputAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            keyStates[e.getKeyCode()] = true;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_SPACE:
                    isPaused.set(!isPaused.get());
                    break;
                case KeyEvent.VK_R:
                    resetGame();
                    break;
                case KeyEvent.VK_1:
                    changeGameMode(GameMode.HUMAN_VS_AI);
                    break;
                case KeyEvent.VK_2:
                    changeGameMode(GameMode.TWO_HUMAN);
                    break;
                case KeyEvent.VK_3:
                    changeGameMode(GameMode.TWO_AI);
                    break;
                case KeyEvent.VK_4:
                    changeGameMode(GameMode.AI_SOLO);
                    break;
                case KeyEvent.VK_MINUS:
                    adjustAIDifficulty(-0.01, -0.05); // Decrease responsiveness and accuracy
                    break;
                case KeyEvent.VK_EQUALS: // Often used for plus sign
                case KeyEvent.VK_PLUS:
                    adjustAIDifficulty(0.01, 0.05); // Increase responsiveness and accuracy
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyStates[e.getKeyCode()] = false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced Circle Pong");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new EnhancedCirclePong(), BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

// Represents the game ball
class Ball {
    private double x, y, velX, velY, speed;
    private final int size;
    private final Point center;
    private final Random random = new Random();

    public Ball(Point center, double speed, int size) {
        this.center = center;
        this.speed = speed;
        this.size = size;
        reset();
    }

    public void reset() {
        x = center.x;
        y = center.y;
        double angle = random.nextDouble() * 2 * Math.PI;
        velX = Math.cos(angle) * speed;
        velY = Math.sin(angle) * speed;
    }

    public void update() {
        x += velX;
        y += velY;
    }

    public void draw(Graphics2D g2d, double predictedAngle) {
        // Draw ghost ball
        if (predictedAngle != -1) {
            g2d.setColor(new Color(255, 255, 255, 60));
            double ghostX = center.x + Math.cos(predictedAngle) * (EnhancedCirclePong.GAME_AREA_RADIUS - size / 2.0);
            double ghostY = center.y + Math.sin(predictedAngle) * (EnhancedCirclePong.GAME_AREA_RADIUS - size / 2.0);
            g2d.fillOval((int) (ghostX - size / 2.0), (int) (ghostY - size / 2.0), size, size);
        }

        g2d.setColor(Color.WHITE);
        g2d.fillOval((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.drawOval((int) (x - size / 2.0), (int) (y - size / 2.0), size, size);
    }

    public void handlePaddleCollision(double paddleAngle) {
        double normalX = Math.cos(paddleAngle);
        double normalY = Math.sin(paddleAngle);
        double dotProduct = velX * normalX + velY * normalY;

        velX -= 2 * dotProduct * normalX;
        velY -= 2 * dotProduct * normalY;

        velX += (random.nextDouble() - 0.5) * 0.2;
        velY += (random.nextDouble() - 0.5) * 0.2;

        double currentSpeed = Math.sqrt(velX * velX + velY * velY);
        velX = (velX / currentSpeed) * speed;
        velY = (velY / currentSpeed) * speed;

        // Ensure ball is outside the paddle
        double distFromCenter = getDistanceFromCenter();
        if (distFromCenter > EnhancedCirclePong.GAME_AREA_RADIUS - size) {
            x = center.x + (x - center.x) * (EnhancedCirclePong.GAME_AREA_RADIUS - size) / distFromCenter;
            y = center.y + (y - center.y) * (EnhancedCirclePong.GAME_AREA_RADIUS - size) / distFromCenter;
        }
    }

    public double getDistanceFromCenter() {
        return Math.hypot(x - center.x, y - center.y);
    }

    public double getAngleFromCenter() {
        return Math.atan2(y - center.y, x - center.x);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public int getSize() { return size; }
}

// Represents a player's paddle
class Paddle {
    private double angle;
    private final Point center;
    private final int radius, length, width;
    private final double speed; // Max speed
    private final Color color;
    private final int playerId;

    // ... (Constructor and other methods remain the same) ...
    public Paddle(Point center, int radius, int length, int width, double speed, Color color, int playerId, double initialAngle) {
        this.center = center;
        this.radius = radius;
        this.length = length;
        this.width = width;
        this.speed = speed;
        this.color = color;
        this.playerId = playerId;
        this.angle = initialAngle;
    }


    public void move(int direction) {
        double newAngle = angle + direction * speed;
        if (isValidMove(newAngle)) {
            angle = newAngle;
            normalizeAngle();
        }
    }

    /**
     * Moves the paddle by a specific angle amount. Used by the AI for smooth movement.
     * @param angleDelta The amount to move the angle by.
     */
    public void moveBy(double angleDelta) {
        // Clamp the movement to the paddle's maximum speed to prevent teleporting
        double maxMove = this.speed * 1.5; // Allow AI to move slightly faster than players
        angleDelta = Math.max(-maxMove, Math.min(maxMove, angleDelta));

        double newAngle = angle + angleDelta;

        if (isValidMove(newAngle)) {
            angle = newAngle;
            normalizeAngle();
        }
    }

    private boolean isValidMove(double testAngle) {
        // The solo AI (player 0) has no movement restrictions
        if (playerId == 0) return true;

        double normalized = normalizeAngleStatically(testAngle);
        if (playerId == 1) { // Player 1 (left side): π/2 to 3π/2
            return normalized >= Math.PI / 2 && normalized <= 3 * Math.PI / 2;
        } else { // Player 2 (right side): 3π/2 to π/2
            return normalized >= 3 * Math.PI / 2 || normalized <= Math.PI / 2;
        }
    }

    private void normalizeAngle() {
        angle = normalizeAngleStatically(angle);
    }

    private static double normalizeAngleStatically(double ang) {
        while (ang < 0) ang += 2 * Math.PI;
        while (ang >= 2 * Math.PI) ang -= 2 * Math.PI;
        return ang;
    }

    // ... (draw, isAngleWithinPaddle, and getters remain the same) ...
    public void draw(Graphics2D g2d) {
        double paddleX = center.x + Math.cos(angle) * radius;
        double paddleY = center.y + Math.sin(angle) * radius;
        double perpendicularAngle = angle + Math.PI / 2;

        int x1 = (int) (paddleX + Math.cos(perpendicularAngle) * length / 2);
        int y1 = (int) (paddleY + Math.sin(perpendicularAngle) * length / 2);
        int x2 = (int) (paddleX - Math.cos(perpendicularAngle) * length / 2);
        int y2 = (int) (paddleY - Math.sin(perpendicularAngle) * length / 2);

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);

        // Breathing effect
        float alpha = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() * 0.002);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 150)));
        g2d.setStroke(new BasicStroke(width + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    public boolean isAngleWithinPaddle(double ballAngle) {
        double paddleArc = (double) length / radius;
        double normalizedBallAngle = normalizeAngleStatically(ballAngle);
        double normalizedPaddleAngle = normalizeAngleStatically(angle);

        double diff = Math.abs(normalizedBallAngle - normalizedPaddleAngle);
        if (diff > Math.PI) diff = 2 * Math.PI - diff;
        return diff <= paddleArc / 2;
    }

    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
}

// Controls an AI paddle
class AiController {
    private double responsiveness; // How quickly the AI reacts (higher is faster)
    private double accuracy;       // How precise the AI is (higher is more accurate)
    private final Point center;
    private final int radius;

    // A stable error offset to prevent jitter from random calculations each frame
    private double currentInaccuracyOffset = 0.0;
    private int framesUntilNextInaccuracyCheck = 0;

    /**
     * @param responsiveness A value typically from 0.05 (slow) to 0.2 (very responsive).
     * @param accuracy A value from 0.0 (wildly inaccurate) to 1.0 (perfect).
     */
    public AiController(double responsiveness, double accuracy, Point center, int radius) {
        this.responsiveness = responsiveness;
        this.accuracy = accuracy;
        this.center = center;
        this.radius = radius;
    }

    /**
     * Updates the paddle's position using smooth, proportional movement.
     */
    public void updatePaddle(Paddle paddle, Ball ball) {
        double predictedAngle = predictBallInterceptAngle(ball);

        if (predictedAngle != -1) {
            // -- Stable Inaccuracy Logic --
            // Only recalculate the AI's "mistake" periodically, not every frame.
            framesUntilNextInaccuracyCheck--;
            if (framesUntilNextInaccuracyCheck <= 0) {
                if (Math.random() > this.accuracy) {
                    // The magnitude of the error depends on the AI's accuracy level.
                    double errorMagnitude = (1.0 - this.accuracy) * 0.6;
                    this.currentInaccuracyOffset = (Math.random() - 0.5) * errorMagnitude;
                } else {
                    this.currentInaccuracyOffset = 0.0; // Perfect accuracy on this check
                }
                // Decide when to check for another "mistake" again.
                framesUntilNextInaccuracyCheck = 10 + (int)(Math.random() * 15); // ~150-400ms
            }

            // Apply the stable error to the target
            double targetAngle = predictedAngle + this.currentInaccuracyOffset;

            // -- Proportional Movement Logic --
            // Calculate the shortest distance between current and target angle
            double angleDifference = targetAngle - paddle.getAngle();
            while (angleDifference > Math.PI) angleDifference -= 2 * Math.PI;
            while (angleDifference < -Math.PI) angleDifference += 2 * Math.PI;

            // The amount to move is a fraction of the remaining distance.
            // This creates smooth acceleration and deceleration.
            double moveDelta = angleDifference * this.responsiveness;

            paddle.moveBy(moveDelta);
        }
    }

    public double predictBallInterceptAngle(Ball ball) {
        double currentX = ball.getX();
        double currentY = ball.getY();
        double currentVelX = ball.getVelX();
        double currentVelY = ball.getVelY();

        // Simulate ball's path for up to 120 frames into the future
        for (int i = 0; i < 120; i++) {
            currentX += currentVelX;
            currentY += currentVelY;
            double dist = Math.hypot(currentX - center.x, currentY - center.y);

            // If the predicted position hits the game boundary, return the angle
            if (dist >= radius - ball.getSize() / 2.0) {
                return Math.atan2(currentY - center.y, currentX - center.x);
            }
        }
        return -1; // Prediction failed
    }

    // Setters and Getters for dynamic difficulty adjustment
    public void setResponsiveness(double responsiveness) { this.responsiveness = responsiveness; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public double getAccuracy() { return accuracy; }
    public double getResponsiveness() { return responsiveness; }
}

// Represents a visual particle effect
class Particle {
    private double x, y, velX, velY;
    private Color color;
    private float alpha = 1.0f;
    private final Random random = new Random();

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = random.nextDouble() * 2 + 1;
        velX = Math.cos(angle) * speed;
        velY = Math.sin(angle) * speed;
        color = new Color(255, 255, 255, 255);
    }

    public void update() {
        x += velX;
        y += velY;
        alpha -= 0.02f;
        if (alpha < 0) alpha = 0;
        color = new Color(1.0f, 1.0f, 1.0f, alpha);
    }

    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int) x, (int) y, 3, 3);
    }

    public boolean isFaded() {
        return alpha <= 0;
    }
}

// Enum for managing game modes
enum GameMode {
    HUMAN_VS_AI("Human vs AI", "Human", "AI"),
    TWO_HUMAN("Two Human", "Player 1", "Player 2"),
    TWO_AI("Two AI", "AI 1", "AI 2"),
    AI_SOLO("AI Solo", "AI", "AI");

    private final String displayName, leftPlayerName, rightPlayerName;

    GameMode(String displayName, String leftPlayerName, String rightPlayerName) {
        this.displayName = displayName;
        this.leftPlayerName = leftPlayerName;
        this.rightPlayerName = rightPlayerName;
    }

    public String getDisplayName() { return displayName; }
    public String getLeftPlayerName() { return leftPlayerName; }
    public String getRightPlayerName() { return rightPlayerName; }
}