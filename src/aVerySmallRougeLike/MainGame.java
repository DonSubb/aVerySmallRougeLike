package aVerySmallRougeLike;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//DonSubb
//Oct 28 2026
public class MainGame extends JPanel implements ActionListener, KeyListener, MouseListener {
    
    // Game States
    enum GameState { MENU, CHAR_SELECT, PLAYING, LEVEL_UP, GAME_OVER }
    private GameState currentState = GameState.MENU;

    // Window dimensions
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    // Game loop timer
    private Timer timer;
    private Random random = new Random();

    // Input tracking
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    // Game Entities
    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<UpgradeCard> currentChoices = new ArrayList<>();

    // Wave / Spawn logic
    private int frameCount = 0;
    private int spawnRate = 120;
    private int waveNumber = 1;
    private int enemiesKilledThisWave = 0;
    private int enemiesTotalThisWave = 20;

    // UI and Animation Images
    private Image uiPanelImg;
    private BufferedImage[] bookFrames = new BufferedImage[12];
    private int currentBookFrame = 0;
    private int bookAnimTimer = 0;
    private boolean bookOpen = false;
    
    // Text Spritesheet
    private BufferedImage textSheet;
    private final String CHARSET = "ABCDEFGHIJKLMNOPQRST  VUWXYZ  1234567890.,:?!()+-="; 

    // Archer Spritesheet
    private BufferedImage[][] archerSprites;

    public MainGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);

        // Load the provided UI Panel Image
        try {
            uiPanelImg = ImageIO.read(new File("character_panel.png"));
        } catch (Exception e) {
            System.out.println("Error: Could not load character_panel.png.");
        }

        // Load the Open Book Spritesheet
        try {
            BufferedImage sheet = ImageIO.read(new File("Open_book.png"));
            int cols = 4;
            int rows = 3;
            int frameW = sheet.getWidth() / cols;
            int frameH = sheet.getHeight() / rows;
            
            int count = 0;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    if (count < 12) {
                        bookFrames[count] = sheet.getSubimage(x * frameW, y * frameH, frameW, frameH);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Could not load Open_book.png.");
        }

        // Load the Text Spritesheet
        try {
            textSheet = ImageIO.read(new File("Text.png"));
        } catch (Exception e) {
            System.out.println("Error: Could not load Text1.png.");
        }

        // Load the Archer Spritesheet
        try {
            BufferedImage archerSheet = ImageIO.read(new File("CharacterA.png"));
            int cols = 8;
            int rows = 5;
            int frameW = archerSheet.getWidth() / cols;
            int frameH = archerSheet.getHeight() / rows;
            
            archerSprites = new BufferedImage[rows][cols];
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    archerSprites[y][x] = archerSheet.getSubimage(x * frameW, y * frameH, frameW, frameH);
                }
            }
        } catch (Exception e) {
            System.out.println("Error: Could not load CharacterA.png.");
        }

        // Run the game at ~60 FPS
        timer = new Timer(16, this);
        timer.start();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Roguelike");
        MainGame game = new MainGame();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }

    // --- GAME LOOP ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentState == GameState.PLAYING) {
            updateGame();
        } 
        else if (currentState == GameState.LEVEL_UP && !bookOpen) {
            bookAnimTimer++;
            if (bookAnimTimer > 4) { 
                bookAnimTimer = 0;
                currentBookFrame++;
                if (currentBookFrame >= 11) {
                    currentBookFrame = 11;
                    bookOpen = true; 
                }
            }
        }
        repaint();
    }

    private void updateGame() {
        frameCount++;

        double dx = 0, dy = 0;
        if (upPressed) dy -= player.moveSpeed;
        if (downPressed) dy += player.moveSpeed;
        if (leftPressed) dx -= player.moveSpeed;
        if (rightPressed) dx += player.moveSpeed;
        
        // Face correct direction horizontally
        if (dx < 0) player.facingRight = false;
        else if (dx > 0) player.facingRight = true;

        if (dx != 0 && dy != 0) {
            dx *= 0.7071;
            dy *= 0.7071;
        }
        
        player.x += dx;
        player.y += dy;
        
        player.x = Math.max(0, Math.min(WIDTH, player.x));
        player.y = Math.max(0, Math.min(HEIGHT, player.y));

        player.cooldownTimer--;
        if (player.cooldownTimer <= 0) {
            Enemy target = getClosestEnemy();
            if (target != null) {
                double dist = getDistance(player.x, player.y, target.x, target.y);
                if (dist <= player.attackRange) {
                    projectiles.add(new Projectile(player.x, player.y, target, player.damage, player.projSpeed));
                    player.cooldownTimer = player.attackCooldown;
                    player.attackAnimTimer = 15; // Show attack animation for 15 frames
                }
            }
        }

        // Setup Player Animation States
        if (player.invincibilityTimer > 40) {
            player.animState = 3; // Hit
        } else if (player.attackAnimTimer > 0) {
            player.animState = 2; // Attack
            player.attackAnimTimer--;
        } else if (dx != 0 || dy != 0) {
            player.animState = 1; // Run
        } else {
            player.animState = 0; // Idle
        }

        // Tick Player Animation Frames
        player.animTimer++;
        if (player.animTimer > 6) { // Speed of animation 
            player.animTimer = 0;
            player.animFrame++;
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update();
            if (!p.active) {
                projectiles.remove(i);
            } else {
                for (Enemy enemy : enemies) {
                    if (getDistance(p.x, p.y, enemy.x, enemy.y) < 15) { 
                        enemy.hp -= p.damage;
                        p.active = false;
                        break;
                    }
                }
            }
        }

        if (player.invincibilityTimer > 0) {
            player.invincibilityTimer--;
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy enemy = enemies.get(i);
            if (enemy.hp <= 0) {
                enemies.remove(i);
                gainXP(enemy.xpReward);
                
                enemiesKilledThisWave++;
                if (enemiesKilledThisWave >= enemiesTotalThisWave) {
                    waveNumber++;
                    enemiesTotalThisWave += 10; 
                    enemiesKilledThisWave = 0;
                }
                continue;
            }
            enemy.update(player.x, player.y);

            if (getDistance(player.x, player.y, enemy.x, enemy.y) < 20) {
                if (player.invincibilityTimer <= 0) {
                    player.hp -= enemy.damage;
                    player.invincibilityTimer = 60; 
                    if (player.hp <= 0) {
                        player.animState = 4; // Dead
                        currentState = GameState.GAME_OVER;
                    }
                }
            }
        }

        if (frameCount % spawnRate == 0) {
            spawnEnemy();
            if (spawnRate > 30) spawnRate--; 
        }
    }

    private void spawnEnemy() {
        double ex = random.nextBoolean() ? -50 : WIDTH + 50;
        double ey = random.nextDouble() * HEIGHT;
        
        if (random.nextBoolean()) { 
            double temp = ex;
            ex = ey;
            ey = temp;
        }

        if (random.nextBoolean()) {
            enemies.add(new Enemy(ex, ey, "Rat"));
        } else {
            enemies.add(new Enemy(ex, ey, "Goblin"));
        }
    }

    private Enemy getClosestEnemy() {
        Enemy closest = null;
        double minDist = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            double dist = getDistance(player.x, player.y, e.x, e.y);
            if (dist < minDist) {
                minDist = dist;
                closest = e;
            }
        }
        return closest;
    }

    private double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private void gainXP(int amount) {
        player.xp += amount * player.xpMultiplier;
        if (player.xp >= player.xpToNextLevel) {
            player.level++;
            player.xp -= player.xpToNextLevel;
            player.xpToNextLevel = (int)(player.xpToNextLevel * 1.5);
            
            generateUpgrades();
            
            currentBookFrame = 0;
            bookAnimTimer = 0;
            bookOpen = false;
            
            currentState = GameState.LEVEL_UP;
            upPressed = downPressed = leftPressed = rightPressed = false; 
        }
    }

    private void generateUpgrades() {
        currentChoices.clear();
        String[] possibleUpgrades = {
            "Damage Up", "Range Up", "Fast Fire", "Move Speed",
            "Max HP Up", "Heal 50%", "Proj Speed", "XP Boost",
            "Double Atk", "Tank Mode"
        };
        
        while (currentChoices.size() < 2) {
            String choice = possibleUpgrades[random.nextInt(possibleUpgrades.length)];
            boolean exists = false;
            for(UpgradeCard card : currentChoices) {
                if(card.name.equals(choice)) exists = true;
            }
            if(!exists) {
                currentChoices.add(new UpgradeCard(choice));
            }
        }
    }

    // --- CUSTOM TEXT DRAWING ---
    private void drawCustomTextCentered(Graphics2D g, String text, int centerX, int y, int scale) {
        if (textSheet == null) {
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, centerX - (fm.stringWidth(text) / 2), y);
            return;
        }

        text = text.toUpperCase();
        int cellW = 10;
        int cellH = 12;
        int stepX = 10; 
        int totalWidth = text.length() * stepX * scale;
        int currentX = centerX - (totalWidth / 2);

        for (char c : text.toCharArray()) {
            if (c == ' ') {
                currentX += stepX * scale;
                continue;
            }
            int index = CHARSET.indexOf(c);
            if (index != -1) {
                int col = index % 10;
                int row = index / 10;
                int srcX = col * cellW;
                int srcY = row * cellH;
                
                g.drawImage(textSheet, 
                    currentX, y, currentX + cellW * scale, y + cellH * scale,
                    srcX, srcY, srcX + cellW, srcY + cellH, 
                    null);
            }
            currentX += stepX * scale;
        }
    }

    // --- DRAWING ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == GameState.MENU) {
            drawCenteredString(g2d, "SIMPLE ROGUELIKE", HEIGHT / 3, Color.WHITE, 40);
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(WIDTH/2 - 100, HEIGHT/2, 200, 50);
            drawCenteredString(g2d, "START", HEIGHT/2 + 35, Color.BLACK, 24);
        } 
        else if (currentState == GameState.CHAR_SELECT) {
            drawCenteredString(g2d, "SELECT YOUR CLASS", 100, Color.WHITE, 30);
            
            g2d.setColor(Color.BLUE);
            g2d.fillOval(150, 250, 50, 50);
            g2d.setColor(Color.WHITE);
            g2d.drawString("WIZARD", 150, 320);

            g2d.setColor(Color.GREEN);
            g2d.fillOval(375, 250, 50, 50);
            g2d.setColor(Color.WHITE);
            g2d.drawString("ARCHER", 375, 320);

            g2d.setColor(Color.RED);
            g2d.fillOval(600, 250, 50, 50);
            g2d.setColor(Color.WHITE);
            g2d.drawString("SWORDSMAN", 585, 320);
        } 
        else if (currentState == GameState.PLAYING) {
            // Draw Player if not flashing from damage
            if (player.invincibilityTimer <= 0 || (player.invincibilityTimer / 5) % 2 == 0) {
                
                if (player.type.equals("Archer") && archerSprites != null) {
                    BufferedImage img = null;
                    switch(player.animState) {
                        case 0: img = archerSprites[4][2 + (player.animFrame % 4)]; break; // Idle
                        case 1: img = archerSprites[0][1 + (player.animFrame % 6)]; break; // Run
                        case 2: img = archerSprites[2][2 + (player.animFrame % 4)]; break; // Attack
                        case 3: img = archerSprites[3][3 + (player.animFrame % 2)]; break; // Hit
                        case 4: img = archerSprites[1][Math.min(7, player.animFrame)]; break; // Dead
                    }
                    
                    if (img != null) {
                        int drawSize = 48; // Scale the sprite up slightly
                        int px = (int)player.x - drawSize / 2;
                        int py = (int)player.y - drawSize / 2;
                        
                        if (player.facingRight) {
                            g2d.drawImage(img, px, py, drawSize, drawSize, null);
                        } else {
                            // Draw flipped horizontally
                            g2d.drawImage(img, px + drawSize, py, -drawSize, drawSize, null);
                        }
                    }
                } else {
                    // Fallback dot for Wizard and Swordsman
                    g2d.setColor(player.color);
                    g2d.fillOval((int)player.x - 15, (int)player.y - 15, 30, 30);
                }
            }

            for (Enemy enemy : enemies) {
                g2d.setColor(enemy.color);
                g2d.fillOval((int)enemy.x - enemy.size/2, (int)enemy.y - enemy.size/2, enemy.size, enemy.size);
            }

            g2d.setColor(Color.YELLOW);
            for (Projectile p : projectiles) {
                g2d.fillOval((int)p.x - 4, (int)p.y - 4, 8, 8);
            }

            int uiX = 10;
            int uiY = 10;
            
            g2d.setColor(player.color); 
            g2d.fillOval(uiX + 10, uiY + 10, 55, 55); 

            double hpPercent = Math.max(0, player.hp / player.maxHp);
            double xpPercent = Math.min(1.0, (double)player.xp / player.xpToNextLevel);
            int enemiesRemaining = enemiesTotalThisWave - enemiesKilledThisWave;
            double wavePercent = Math.max(0, (double)enemiesRemaining / enemiesTotalThisWave);

            int barStartX = uiX + 65;
            int barStartXG = uiX + 50;
            int maxBarWidth = 120; 

            g2d.setColor(new Color(100, 0, 0));
            g2d.fillRect(barStartX, uiY + 15, maxBarWidth, 8);
            g2d.setColor(Color.RED);
            g2d.fillRect(barStartX, uiY + 15, (int)(maxBarWidth * hpPercent), 8);

            g2d.setColor(new Color(0, 0, 100));
            g2d.fillRect(barStartX, uiY + 30, maxBarWidth, 8);
            g2d.setColor(new Color(50, 150, 255)); 
            g2d.fillRect(barStartX, uiY + 30, (int)(maxBarWidth * xpPercent), 8);

            g2d.setColor(new Color(0, 100, 0));
            g2d.fillRect(barStartXG, uiY + 45, maxBarWidth, 8);
            g2d.setColor(Color.GREEN);
            g2d.fillRect(barStartXG, uiY + 45, (int)(maxBarWidth * wavePercent), 8);

            if (uiPanelImg != null) {
                g2d.drawImage(uiPanelImg, uiX, uiY, null);
            } else {
                g2d.setColor(Color.RED);
                g2d.drawString("Missing character_panel.png!", uiX, uiY + 20);
            }
        }
        else if (currentState == GameState.LEVEL_UP) {
            drawCenteredString(g2d, "LEVEL UP!", 100, Color.YELLOW, 40);
            
            int bookX = WIDTH / 2 - 200;
            int bookY = HEIGHT / 2 - 150;
            
            if (bookFrames[currentBookFrame] != null) {
                g2d.drawImage(bookFrames[currentBookFrame], bookX, bookY, 400, 300, null);
            } else {
                g2d.setColor(Color.GRAY);
                g2d.fillRect(bookX, bookY, 400, 300);
            }
            
            if (bookOpen) {
                int leftPageCenterX = bookX + 110;
                int rightPageCenterX = bookX + 310;
                int textY = bookY + 140;

                drawCustomTextCentered(g2d, currentChoices.get(0).name, leftPageCenterX, textY, 1);
                drawCustomTextCentered(g2d, currentChoices.get(1).name, rightPageCenterX, textY, 1);
            }
        }
        else if (currentState == GameState.GAME_OVER) {
            drawCenteredString(g2d, "GAME OVER", HEIGHT / 2 - 50, Color.RED, 50);
            drawCenteredString(g2d, "Click anywhere to restart", HEIGHT / 2 + 50, Color.WHITE, 20);
        }
    }

    private void drawCenteredString(Graphics2D g, String text, int y, Color c, int size) {
        g.setColor(c);
        g.setFont(new Font("Arial", Font.BOLD, size));
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    // --- INPUT HANDLING ---
    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        if (currentState == GameState.MENU) {
            if (mx >= WIDTH/2 - 100 && mx <= WIDTH/2 + 100 && my >= HEIGHT/2 && my <= HEIGHT/2 + 50) {
                currentState = GameState.CHAR_SELECT;
            }
        } 
        else if (currentState == GameState.CHAR_SELECT) {
            if (mx >= 150 && mx <= 200 && my >= 250 && my <= 300) startGame("Wizard");
            if (mx >= 375 && mx <= 425 && my >= 250 && my <= 300) startGame("Archer");
            if (mx >= 600 && mx <= 650 && my >= 250 && my <= 300) startGame("Swordsman");
        }
        else if (currentState == GameState.LEVEL_UP && bookOpen) {
            int bookX = WIDTH / 2 - 200;
            int bookY = HEIGHT / 2 - 150;
            
            if (mx >= bookX && mx <= bookX + 200 && my >= bookY && my <= bookY + 300) {
                applyUpgrade(currentChoices.get(0).name);
                currentState = GameState.PLAYING;
            }
            else if (mx >= bookX + 200 && mx <= bookX + 400 && my >= bookY && my <= bookY + 300) {
                applyUpgrade(currentChoices.get(1).name);
                currentState = GameState.PLAYING;
            }
        }
        else if (currentState == GameState.GAME_OVER) {
            currentState = GameState.MENU;
        }
    }

    private void startGame(String characterClass) {
        player = new Player(characterClass);
        enemies.clear();
        projectiles.clear();
        frameCount = 0;
        spawnRate = 120;
        waveNumber = 1;
        enemiesKilledThisWave = 0;
        enemiesTotalThisWave = 20;
        currentState = GameState.PLAYING;
    }

    private void applyUpgrade(String name) {
        switch (name) {
            case "Damage Up": player.damage += 5; break;
            case "Range Up": player.attackRange += 50; break;
            case "Fast Fire": player.attackCooldown = Math.max(5, (int)(player.attackCooldown * 0.8)); break;
            case "Move Speed": player.moveSpeed += 0.5; break;
            case "Max HP Up": player.maxHp += 20; player.hp += 20; break;
            case "Heal 50%": player.hp = Math.min(player.maxHp, player.hp + (player.maxHp / 2)); break;
            case "Proj Speed": player.projSpeed += 3; break;
            case "XP Boost": player.xpMultiplier += 0.5; break;
            case "Double Atk": player.damage += 2; break;
            case "Tank Mode": player.maxHp += 50; player.hp += 50; player.moveSpeed -= 0.5; break;
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) upPressed = true;
        if (key == KeyEvent.VK_S) downPressed = true;
        if (key == KeyEvent.VK_A) leftPressed = true;
        if (key == KeyEvent.VK_D) rightPressed = true;
    }

    @Override public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_W) upPressed = false;
        if (key == KeyEvent.VK_S) downPressed = false;
        if (key == KeyEvent.VK_A) leftPressed = false;
        if (key == KeyEvent.VK_D) rightPressed = false;
    }
    
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // --- INNER CLASSES ---

    class Player {
        String type;
        double x, y, hp, maxHp, damage, attackRange, moveSpeed, projSpeed;
        int attackCooldown, cooldownTimer, level = 1, xp = 0, xpToNextLevel = 10, invincibilityTimer = 0; 
        double xpMultiplier = 1.0;
        Color color;
        
        // Animation Variables
        boolean facingRight = true;
        int animState = 0; 
        int animFrame = 0;
        int animTimer = 0;
        int attackAnimTimer = 0;

        public Player(String type) {
            this.type = type;
            this.x = WIDTH / 2.0; this.y = HEIGHT / 2.0;
            if (type.equals("Wizard")) {
                color = Color.BLUE; maxHp = 100; hp = 100; damage = 10;
                attackRange = 300; attackCooldown = 60; moveSpeed = 3.0; projSpeed = 6.0;
            } else if (type.equals("Archer")) {
                color = Color.GREEN; maxHp = 80; hp = 80; damage = 5;
                attackRange = 400; attackCooldown = 30; moveSpeed = 4.0; projSpeed = 10.0;
            } else if (type.equals("Swordsman")) {
                color = Color.RED; maxHp = 150; hp = 150; damage = 15;
                attackRange = 100; attackCooldown = 45; moveSpeed = 3.5; projSpeed = 15.0; 
            }
        }
    }

    class Enemy {
        double x, y, hp, damage, speed;
        int xpReward, size;
        Color color;

        public Enemy(double x, double y, String type) {
            this.x = x; this.y = y;
            if (type.equals("Rat")) {
                color = new Color(139, 69, 19); hp = 15; damage = 5; speed = 2.0; xpReward = 2; size = 16;
            } else if (type.equals("Goblin")) {
                color = new Color(0, 100, 0); hp = 30; damage = 10; speed = 1.2; xpReward = 5; size = 24;
            }
        }

        public void update(double playerX, double playerY) {
            double angle = Math.atan2(playerY - y, playerX - x);
            x += Math.cos(angle) * speed;
            y += Math.sin(angle) * speed;
        }
    }

    class Projectile {
        double x, y, vx, vy, damage;
        boolean active = true;

        public Projectile(double startX, double startY, Enemy target, double damage, double speed) {
            this.x = startX; this.y = startY; this.damage = damage;
            double angle = Math.atan2(target.y - startY, target.x - startX);
            this.vx = Math.cos(angle) * speed; this.vy = Math.sin(angle) * speed;
        }

        public void update() {
            x += vx; y += vy;
            if (x < 0 || x > WIDTH || y < 0 || y > HEIGHT) active = false;
        }
    }

    class UpgradeCard {
        String name;
        public UpgradeCard(String name) { this.name = name; }
    }
}