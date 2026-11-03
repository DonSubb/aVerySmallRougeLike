package aVerySmallRougeLike;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainGame extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;

    // =====================================================================
    //  CONSTANTS
    // =====================================================================
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    private static final int HALF_W = WIDTH / 2;
    private static final int HALF_H = HEIGHT / 2;

    private static final int TILE = 256;                 
    private static final int MAX_ENEMIES = 250;          
    private static final int MAX_GEMS = 300;
    private static final int BUTTON_PRESS_FRAMES = 20;   
    private static final double BTN_ASPECT = 250.0 / 300.0; 

    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!?$";
    private static final int GLYPH_W = 25;
    private static final int GLYPH_H = 25;
    private static final int[] GLYPH_INDEX = new int[128];
    static {
        Arrays.fill(GLYPH_INDEX, -1);
        for (int i = 0; i < CHARSET.length(); i++) {
            char c = CHARSET.charAt(i);
            if (c != ' ') GLYPH_INDEX[c] = i;
        }
    }

    private static final int SLOT = 44;
    private static final int SLOT_GAP = 6;
    private static final int HOTBAR_Y = HEIGHT - 56;

    private static final HeroClass[] HEROES = HeroClass.values();
    private static final EnemyType[] ENEMY_TYPES = EnemyType.values();
    private static final Card[] CARDS = Card.values();
    private static final Item[] ITEMS = Item.values();

    private static final Color OVERLAY = new Color(0, 0, 0, 150);
    private static final Color PANEL_FILL = new Color(235, 228, 205);
    private static final Color PANEL_HOVER = new Color(255, 250, 230);
    private static final Color SLOT_FILL = new Color(235, 228, 205, 235);
    private static final Color BORDER = new Color(60, 45, 30);
    private static final Color SHADOW = new Color(0, 0, 0, 70);
    private static final Color FROZEN = new Color(150, 220, 255);
    private static final Color OUTLINE = new Color(20, 20, 20, 160);
    private static final Color ENEMY_SHOT = new Color(230, 110, 30);
    private static final Color CRIT_COLOR = new Color(255, 140, 0);
    private static final Color BLAST_COLOR = new Color(255, 160, 40);
    private static final Color TEXT_DARK = new Color(50, 35, 20);
    private static final Color[] GEM_COLORS = { new Color(90, 200, 255), new Color(90, 230, 120), new Color(255, 215, 0) };
    private static final Stroke THIN = new BasicStroke(1f);
    private static final Stroke THICK = new BasicStroke(3f);
    private static final Font FONT_BODY = new Font("Arial", Font.PLAIN, 12);
    private static final Font FONT_BODY_BOLD = new Font("Arial", Font.BOLD, 12);
    private static final Font FONT_FLOAT = new Font("Arial", Font.BOLD, 13);
    private final Map<Integer, Font> boldFonts = new HashMap<>();

    // =====================================================================
    //  ENUMS
    // =====================================================================
    enum GameState { MENU, CHAR_SELECT, JOURNAL, PLAYING, PAUSED, LEVEL_UP, GAME_OVER }

    enum Behavior { CHASE, WEAVE, RANGED }

    enum EnemyType {
        RAT(15, 5, 2.0, 2, 16, new Color(139, 69, 19), 1, 10, Behavior.CHASE),
        GOBLIN(30, 10, 1.2, 5, 24, new Color(0, 100, 0), 1, 10, Behavior.CHASE),
        BAT(10, 4, 2.8, 3, 14, new Color(110, 60, 160), 2, 6, Behavior.WEAVE),
        SLIME(40, 8, 0.9, 6, 26, new Color(90, 200, 90), 3, 6, Behavior.CHASE),
        SKELETON(25, 8, 1.0, 8, 22, new Color(225, 225, 205), 4, 5, Behavior.RANGED),
        OGRE(150, 25, 0.7, 25, 40, new Color(160, 110, 60), 6, 2, Behavior.CHASE),
        MINI_SLIME(12, 5, 1.4, 2, 14, new Color(130, 230, 130), 0, 0, Behavior.CHASE);

        final double hp, damage, speed;
        final int xp, size, unlockWave, weight;
        final Color color;
        final Behavior behavior;

        EnemyType(double hp, double damage, double speed, int xp, int size, Color color,
                  int unlockWave, int weight, Behavior behavior) {
            this.hp = hp; this.damage = damage; this.speed = speed; this.xp = xp; this.size = size;
            this.color = color; this.unlockWave = unlockWave; this.weight = weight; this.behavior = behavior;
        }
    }

    enum HeroClass {
        WIZARD("Wizard", "CharacterW.png", new Color(60, 90, 255), 100, 10, 300, 60, 3.0, 6.0, 5,
                "Balanced caster that fires magic bolts."),
        ARCHER("Archer", "CharacterA.png", new Color(50, 180, 70), 80, 5, 400, 30, 4.0, 10.0, 3,
                "Fast and agile. Shoots quickly from far away."),
        SWORDSMAN("Swordsman", "CharacterS.png", new Color(210, 50, 50), 150, 15, 100, 45, 3.5, 15.0, 8,
                "Tough melee fighter. Hits hard but has short reach.");

        final String label, sheet, description;
        final Color color;
        final double maxHp, damage, range, cooldown, speed, projSpeed;
        final int projRadius;

        HeroClass(String label, String sheet, Color color, double maxHp, double damage, double range,
                  double cooldown, double speed, double projSpeed, int projRadius, String description) {
            this.label = label; this.sheet = sheet; this.color = color; this.maxHp = maxHp; this.damage = damage;
            this.range = range; this.cooldown = cooldown; this.speed = speed; this.projSpeed = projSpeed;
            this.projRadius = projRadius; this.description = description;
        }
    }

    enum Rarity {
        COMMON(50, new Color(70, 70, 70)),
        RARE(30, new Color(30, 90, 200)),
        EPIC(12, new Color(150, 40, 190));

        final int weight;
        final Color color;
        Rarity(int weight, Color color) { this.weight = weight; this.color = color; }
    }

    enum Card {
        SHARP_EDGE("SHARP EDGE", Rarity.COMMON, 8, "Damage +20%."),
        LONG_SHOT("LONG SHOT", Rarity.COMMON, 6, "Attack range +15%."),
        QUICK_DRAW("QUICK DRAW", Rarity.COMMON, 6, "Attack speed +12%."),
        SWIFT_FEET("SWIFT FEET", Rarity.COMMON, 5, "Move speed +8%."),
        VITALITY("VITALITY", Rarity.COMMON, 99, "Max HP +20 and heal 20."),
        MENDING("MENDING", Rarity.COMMON, 99, "Heal 40% of your max HP."),
        FLEET_SHOT("FLEET SHOT", Rarity.COMMON, 5, "Projectile speed +20%."),
        SCHOLAR("SCHOLAR", Rarity.COMMON, 5, "XP gained +25%."),
        MAGNETISM("MAGNETISM", Rarity.COMMON, 5, "XP pickup range +40%."),
        IRON_SKIN("IRON SKIN", Rarity.RARE, 5, "Damage taken -8%."),
        CRITICAL("CRITICAL", Rarity.RARE, 5, "Crit chance +10%. Crits deal double damage."),
        LEECH("LEECH", Rarity.RARE, 4, "Heal 1 HP for every kill."),
        REGEN("REGEN", Rarity.RARE, 4, "Regenerate 0.6 HP per second."),
        FORTUNE("FORTUNE", Rarity.RARE, 4, "Chests appear 25% faster."),
        TWIN_SHOT("TWIN SHOT", Rarity.EPIC, 3, "Fire one extra projectile."),
        PIERCING("PIERCING", Rarity.EPIC, 3, "Shots pierce one more enemy."),
        BLAST("BLAST", Rarity.EPIC, 3, "Shots explode and hurt nearby enemies."),
        GLASS_CANNON("GLASS CANNON", Rarity.EPIC, 1, "Damage +60% but max HP -20%.");

        final String label, description;
        final Rarity rarity;
        final int maxLevel;
        Card(String label, Rarity rarity, int maxLevel, String description) {
            this.label = label; this.rarity = rarity; this.maxLevel = maxLevel; this.description = description;
        }
    }

    enum Item {
        POTION("POTION", true, 25, new Color(220, 60, 70), "Heals 40% of your max HP."),
        BOMB("BOMB", true, 16, new Color(70, 70, 70), "Blasts every nearby enemy."),
        FREEZE_ORB("FREEZE ORB", true, 10, new Color(90, 190, 240), "Freezes all enemies for 4 seconds."),
        RAGE_TONIC("RAGE TONIC", true, 10, new Color(230, 120, 30), "+50% damage and faster attacks for 10 seconds."),
        MAGNET_ORB("MAGNET ORB", true, 14, new Color(150, 90, 220), "Pulls in every XP gem."),
        BOOTS("BOOTS", false, 8, new Color(150, 100, 50), "Passive: move speed +0.35."),
        HEART_CHARM("HEART CHARM", false, 8, new Color(240, 90, 150), "Passive: max HP +15."),
        CLOVER("CLOVER", false, 8, new Color(60, 170, 80), "Passive: crit chance +4%."),
        IRON_RING("IRON RING", false, 8, new Color(130, 130, 150), "Passive: damage taken -4%.");

        final String label, description;
        final boolean consumable;
        final int weight;
        final Color color;
        Item(String label, boolean consumable, int weight, Color color, String description) {
            this.label = label; this.consumable = consumable; this.weight = weight;
            this.color = color; this.description = description;
        }
    }

    // =====================================================================
    //  FIELDS
    // =====================================================================
    private GameState currentState = GameState.MENU;
    private int stateTimer = 0;
    private int uiTick = 0;

    private final Timer timer;
    private final Random random = new Random();

    private boolean upPressed, downPressed, leftPressed, rightPressed, escHeld;
    private boolean mouseHeld;
    private int mouseX = HALF_W, mouseY = HALF_H;

    private Player player;
    private int camX, camY;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Enemy> pendingSpawns = new ArrayList<>();
    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final List<Chest> chests = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private final List<Card> currentChoices = new ArrayList<>();

    private int frameCount = 0;
    private int spawnRate = 120;
    private int spawnTimer = 60;
    private int wave = 1;
    private int enemiesKilledThisWave = 0;
    private int enemiesTotalThisWave = 20;
    private int totalKills = 0;
    private int chestTimer = 0;

    private int rageTimer = 0;
    private int freezeTimer = 0;
    private String bannerText = "";
    private int bannerTimer = 0;
    private String toastText = "";
    private int toastTimer = 0;

    private static final Path JOURNAL_FILE = Paths.get("journal.dat");
    private final Set<Card> discovered = EnumSet.noneOf(Card.class);

    private Image uiPanelImg;
    private BufferedImage titleImg;
    private BufferedImage[] bookFrames;
    private int currentBookFrame = 0;
    private int bookAnimTimer = 0;
    private boolean bookOpen = false;
    private BufferedImage textSheet;
    private BufferedImage smallTextSheet;
    private int smallGlyphW;
    private int smallGlyphH;
    
    private final Map<HeroClass, BufferedImage[][]> heroSprites = new EnumMap<>(HeroClass.class);
    private BufferedImage[][] enemySprites;            
    private final BufferedImage[][] starSprites = new BufferedImage[5][];
    private final BufferedImage[] buttonFrames = new BufferedImage[2];
    private final BufferedImage groundTile = createGroundTile();
    private final Ellipse2D portraitClip = new Ellipse2D.Float(20, 20, 55, 55);

    private final List<Star> menuStars = new ArrayList<>();
    private final List<UIButton> menuButtons = new ArrayList<>();
    private final List<UIButton> pauseButtons = new ArrayList<>();
    private final List<UIButton> backButtons = new ArrayList<>();
    private final Rectangle[] heroCards = new Rectangle[HEROES.length];

    // =====================================================================
    //  SETUP
    // =====================================================================
    public MainGame() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                if (currentState == GameState.PLAYING) setState(GameState.PAUSED);
                mouseHeld = false;
            }
        });

        loadAssets();
        loadJournal();
        buildUi();

        timer = new Timer(16, this); 
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Simple Rougelike");
            MainGame game = new MainGame();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }

    private void loadAssets() {
        uiPanelImg = loadImage("character_panel.png");
        titleImg = loadImage("Title.png");
        textSheet = loadImage("Text.png");
        
        smallTextSheet = loadImage("SmallText.png");
        if (smallTextSheet != null) {
            smallGlyphW = smallTextSheet.getWidth() / 6; 
            smallGlyphH = smallTextSheet.getHeight() / 7; 
        }
        
        bookFrames = flatten(loadSheet("Open_book.png", 4, 3, true));

        for (HeroClass h : HEROES) heroSprites.put(h, loadSheet(h.sheet, 8, 5, true));

        String[] starFiles = { "Star1.png", "Star2.png", "Star3.png", "Star4.png", "Star5.png" };
        for (int i = 0; i < starFiles.length; i++) starSprites[i] = flatten(loadSheet(starFiles[i], 3, 3, true));

        BufferedImage btnSheet = loadImage("Button.png");
        if (btnSheet != null) {
            int frameH = btnSheet.getHeight() / 2;
            buttonFrames[0] = btnSheet.getSubimage(0, 0, btnSheet.getWidth(), frameH);
            buttonFrames[1] = btnSheet.getSubimage(0, frameH, btnSheet.getWidth(), frameH);
        }

        if (new File("Enemies.png").exists()) enemySprites = loadSheet("Enemies.png", 4, ENEMY_TYPES.length, true);
    }

    private BufferedImage loadImage(String file) {
        try {
            return ImageIO.read(new File(file));
        } catch (Exception e) {
            System.out.println("Error: Could not load " + file + ".");
            return null;
        }
    }

    private BufferedImage[][] loadSheet(String file, int cols, int rows, boolean report) {
        BufferedImage sheet = loadImage(file);
        if (sheet == null) return null;
        int fw = sheet.getWidth() / cols;
        int fh = sheet.getHeight() / rows;
        BufferedImage[][] out = new BufferedImage[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                out[y][x] = sheet.getSubimage(x * fw, y * fh, fw, fh);
        return out;
    }

    private BufferedImage[] flatten(BufferedImage[][] grid) {
        if (grid == null) return null;
        List<BufferedImage> list = new ArrayList<>();
        for (BufferedImage[] row : grid) list.addAll(Arrays.asList(row));
        return list.toArray(new BufferedImage[0]);
    }

    private BufferedImage createGroundTile() {
        BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_RGB);
        Random r = new Random(7);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(38, 52, 44));
        g.fillRect(0, 0, TILE, TILE);
        Color[] shades = { new Color(46, 62, 52), new Color(33, 46, 39), new Color(52, 70, 57) };
        for (int i = 0; i < 900; i++) {
            g.setColor(shades[r.nextInt(shades.length)]);
            g.fillRect(r.nextInt(TILE), r.nextInt(TILE), 2, 2);
        }
        g.setColor(new Color(70, 76, 72));
        for (int i = 0; i < 14; i++) g.fillOval(r.nextInt(TILE - 8), r.nextInt(TILE - 8), 5 + r.nextInt(4), 3 + r.nextInt(3));
        g.dispose();
        return img;
    }

    private void buildUi() {
        int bw = 220;
        int bh = (int) (bw * BTN_ASPECT);
        int gap = 20;

        int x0 = (WIDTH - (3 * bw + 2 * gap)) / 2;
        int by = HEIGHT / 2 - 10;
        menuButtons.add(new UIButton(x0, by, bw, bh, "START", true, () -> setState(GameState.CHAR_SELECT)));
        menuButtons.add(new UIButton(x0 + bw + gap, by, bw, bh, "JOURNAL", true, () -> setState(GameState.JOURNAL)));
        menuButtons.add(new UIButton(x0 + 2 * (bw + gap), by, bw, bh, "QUIT", true, () -> System.exit(0)));

        int px = (WIDTH - (2 * bw + gap)) / 2;
        int py = HEIGHT / 2 - 40;
        pauseButtons.add(new UIButton(px, py, bw, bh, "RESUME", true, () -> setState(GameState.PLAYING)));
        pauseButtons.add(new UIButton(px + bw + gap, py, bw, bh, "MENU", true, () -> setState(GameState.MENU)));

        backButtons.add(new UIButton(15, 15, 110, 40, "BACK", true, () -> setState(GameState.MENU)));

        int cardW = 210, cardH = 340, cardGap = 30;
        int cx0 = (WIDTH - (HEROES.length * cardW + (HEROES.length - 1) * cardGap)) / 2;
        for (int i = 0; i < HEROES.length; i++) heroCards[i] = new Rectangle(cx0 + i * (cardW + cardGap), 130, cardW, cardH);
    }

    private void setState(GameState s) {
        currentState = s;
        stateTimer = 0;
        setCursor(Cursor.getPredefinedCursor(s == GameState.PLAYING ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void loadJournal() {
        try {
            if (Files.exists(JOURNAL_FILE)) {
                for (String line : Files.readAllLines(JOURNAL_FILE)) {
                    try { discovered.add(Card.valueOf(line.trim())); } catch (IllegalArgumentException ignored) { }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read journal.dat");
        }
    }

    private void saveJournal() {
        List<String> lines = new ArrayList<>();
        for (Card c : discovered) lines.add(c.name());
        try {
            Files.write(JOURNAL_FILE, lines);
        } catch (IOException e) {
            System.out.println("Could not write journal.dat");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        uiTick++;
        switch (currentState) {
            case MENU:
            case CHAR_SELECT:
            case JOURNAL:
                updateStars();
                tickButtons();
                break;
            case PAUSED:
                tickButtons();
                break;
            case PLAYING:
                updateGame();
                break;
            case LEVEL_UP:
                updateBook();
                break;
            case GAME_OVER:
                stateTimer++;
                tickPlayerAnimation();
                break;
        }
        repaint();
    }

    private List<UIButton> buttonsForState() {
        switch (currentState) {
            case MENU: return menuButtons;
            case PAUSED: return pauseButtons;
            case CHAR_SELECT:
            case JOURNAL: return backButtons;
            default: return java.util.Collections.emptyList();
        }
    }

    private void tickButtons() {
        for (UIButton b : buttonsForState()) b.tick();
    }

    private boolean pressButtons(int mx, int my) {
        List<UIButton> list = buttonsForState();
        for (UIButton b : list) if (b.pressed) return true;
        for (UIButton b : list) {
            if (b.bounds.contains(mx, my)) { b.press(); return true; }
        }
        return false;
    }

    private void updateStars() {
        if (random.nextInt(100) < 15) menuStars.add(new Star(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(5)));
        for (int i = menuStars.size() - 1; i >= 0; i--) {
            Star s = menuStars.get(i);
            s.update();
            if (!s.active) menuStars.remove(i);
        }
    }

    private void updateBook() {
        if (bookOpen || bookFrames == null) { bookOpen = true; return; }
        if (++bookAnimTimer > 4) {
            bookAnimTimer = 0;
            if (++currentBookFrame >= bookFrames.length - 1) {
                currentBookFrame = bookFrames.length - 1;
                bookOpen = true;
            }
        }
    }

    private void updateCamera() {
        camX = (int) Math.round(player.x) - HALF_W;
        camY = (int) Math.round(player.y) - HALF_H;
    }

    private void updateGame() {
        frameCount++;
        final Player p = player;

        double dx = 0, dy = 0;
        if (upPressed) dy -= 1;
        if (downPressed) dy += 1;
        if (leftPressed) dx -= 1;
        if (rightPressed) dx += 1;
        boolean moving = dx != 0 || dy != 0;
        if (moving) {
            double len = Math.hypot(dx, dy);
            dx = dx / len * p.moveSpeed;
            dy = dy / len * p.moveSpeed;
            p.x += dx;
            p.y += dy;
        }
        if (mouseHeld) p.facingRight = mouseX >= HALF_W;
        else if (dx < 0) p.facingRight = false;
        else if (dx > 0) p.facingRight = true;
        updateCamera();

        if (rageTimer > 0) rageTimer--;
        if (freezeTimer > 0) freezeTimer--;
        if (bannerTimer > 0) bannerTimer--;
        if (toastTimer > 0) toastTimer--;
        if (p.invincibilityTimer > 0) p.invincibilityTimer--;
        if (p.regen > 0 && p.hp < p.maxHp) p.hp = Math.min(p.maxHp, p.hp + p.regen / 60.0);

        p.cooldownTimer--;
        if (mouseHeld && p.cooldownTimer <= 0) fire();

        int newState;
        if (p.invincibilityTimer > 40) newState = 3;
        else if (p.attackAnimTimer > 0) newState = 2;
        else if (moving) newState = 1;
        else newState = 0;
        if (p.attackAnimTimer > 0) p.attackAnimTimer--;
        if (newState != p.animState) {
            p.animState = newState;
            p.animFrame = 0;
            p.animTimer = 0;
        }
        tickPlayerAnimation();

        updateProjectiles();
        updateEnemies();
        updateGems();
        updateChests();

        rings.removeIf(r -> --r.life <= 0);
        for (int i = floatTexts.size() - 1; i >= 0; i--) {
            FloatText f = floatTexts.get(i);
            f.y -= 0.6;
            if (--f.life <= 0) floatTexts.remove(i);
        }

        if (--spawnTimer <= 0) {
            spawnTimer = spawnRate;
            if (spawnRate > 30) spawnRate--;
            int count = 1 + (wave - 1) / 3;
            for (int i = 0; i < count && enemies.size() < MAX_ENEMIES; i++) spawnEnemy(pickEnemyType());
        }
        if (--chestTimer <= 0) {
            chestTimer = nextChestDelay();
            if (chests.size() < 3) spawnChest();
        }

        if (currentState == GameState.PLAYING && p.xp >= p.xpToNextLevel) startLevelUp();
    }

    private void tickPlayerAnimation() {
        player.animTimer++;
        if (player.animTimer > 6) {
            player.animTimer = 0;
            player.animFrame++;
        }
    }

    private void fire() {
        Player p = player;
        double angle = Math.atan2(mouseY - HALF_H, mouseX - HALF_W);
        double base = p.damage * (rageTimer > 0 ? 1.5 : 1.0);
        int n = p.projectileCount;
        for (int i = 0; i < n; i++) {
            double a = angle + (i - (n - 1) / 2.0) * 0.14;
            boolean crit = random.nextDouble() < p.critChance;
            Projectile pr = new Projectile(p.x, p.y, a, crit ? base * 2 : base, p.projSpeed, p.attackRange,
                    p.pierce, crit, false, p.hero.color);
            pr.radius = p.hero.projRadius;
            projectiles.add(pr);
        }
        double cd = p.attackCooldown * (rageTimer > 0 ? 0.6 : 1.0);
        p.cooldownTimer = (int) Math.max(4, Math.round(cd));
        p.attackAnimTimer = 15;
    }

    private void updateProjectiles() {
        Player p = player;
        for (int i = 0, n = projectiles.size(); i < n; i++) {
            Projectile pr = projectiles.get(i);
            pr.update();
            if (!pr.active) continue;

            if (pr.hostile) {
                if (distSq(pr.x, pr.y, p.x, p.y) < 14 * 14) {
                    hurtPlayer(pr.damage);
                    pr.active = false;
                }
                continue;
            }
            for (int j = 0, m = enemies.size(); j < m; j++) {
                Enemy en = enemies.get(j);
                if (en.hp <= 0 || pr.alreadyHit(en)) continue;
                double r = en.size / 2.0 + pr.radius;
                if (segDistSq(en.x, en.y, pr.px, pr.py, pr.x, pr.y) < r * r) {
                    damageEnemy(en, pr.damage, pr.crit);
                    if (p.blastRadius > 0) explode(pr.x, pr.y, p.blastRadius, pr.damage * 0.5, en);
                    if (pr.pierce > 0) {
                        pr.pierce--;
                        pr.markHit(en);
                    } else {
                        pr.active = false;
                    }
                    break;
                }
            }
        }
        projectiles.removeIf(pr -> !pr.active);
    }

    private void updateEnemies() {
        Player p = player;
        boolean frozen = freezeTimer > 0;
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy en = enemies.get(i);
            if (en.hp <= 0) {
                enemies.remove(i);
                onEnemyKilled(en);
                continue;
            }
            if (en.hitFlash > 0) en.hitFlash--;
            if (!frozen) en.update(p);

            double d2 = distSq(en.x, en.y, p.x, p.y);
            if (d2 > 1200.0 * 1200.0) { 
                placeOnSpawnRing(en);
                continue;
            }
            if (!frozen) {
                double r = en.size / 2.0 + 10;
                if (d2 < r * r) hurtPlayer(en.damage);
            }
        }
        if (!pendingSpawns.isEmpty()) {
            enemies.addAll(pendingSpawns);
            pendingSpawns.clear();
        }
    }

    private void updateGems() {
        Player p = player;
        double rad2 = p.pickupRadius * p.pickupRadius;
        for (int i = gems.size() - 1; i >= 0; i--) {
            Gem gem = gems.get(i);
            double d2 = distSq(gem.x, gem.y, p.x, p.y);
            if (!gem.pulled && d2 < rad2) gem.pulled = true;
            if (gem.pulled) {
                gem.speed = Math.min(10, gem.speed + 0.5);
                double d = Math.sqrt(d2);
                if (d < 12) {
                    gainXP(gem.value);
                    gems.remove(i);
                    continue;
                }
                gem.x += (p.x - gem.x) / d * gem.speed;
                gem.y += (p.y - gem.y) / d * gem.speed;
            } else if (d2 > 2000.0 * 2000.0) {
                gems.remove(i);
            }
        }
    }

    private void updateChests() {
        Player p = player;
        for (int i = chests.size() - 1; i >= 0; i--) {
            Chest c = chests.get(i);
            double d2 = distSq(c.x, c.y, p.x, p.y);
            if (d2 < 30 * 30) {
                chests.remove(i);
                openChest(c);
            } else if (d2 > 1600.0 * 1600.0) {
                chests.remove(i);
            }
        }
    }

    private void damageEnemy(Enemy en, double dmg, boolean crit) {
        en.hp -= dmg;
        en.hitFlash = 4;
        if (crit) floatTexts.add(new FloatText(en.x, en.y - en.size / 2.0, String.valueOf((int) dmg), CRIT_COLOR));
    }

    private void explode(double x, double y, double radius, double dmg, Enemy skip) {
        rings.add(new Ring(x, y, radius, BLAST_COLOR));
        for (int i = 0, n = enemies.size(); i < n; i++) {
            Enemy en = enemies.get(i);
            if (en == skip || en.hp <= 0) continue;
            double r = radius + en.size / 2.0;
            if (distSq(x, y, en.x, en.y) < r * r) damageEnemy(en, dmg, false);
        }
    }

    private void onEnemyKilled(Enemy en) {
        totalKills++;
        addGem(en.x, en.y, en.type.xp);
        if (player.leech > 0) heal(player.leech, false);
        if (en.type == EnemyType.SLIME) {
            pendingSpawns.add(new Enemy(en.x - 12, en.y, EnemyType.MINI_SLIME, wave));
            pendingSpawns.add(new Enemy(en.x + 12, en.y, EnemyType.MINI_SLIME, wave));
        }
        if (en.type.weight > 0) {
            if (++enemiesKilledThisWave >= enemiesTotalThisWave) {
                wave++;
                enemiesKilledThisWave = 0;
                enemiesTotalThisWave += 10;
                showBanner("WAVE " + wave);
            }
        }
    }

    private void hurtPlayer(double dmg) {
        Player p = player;
        if (currentState != GameState.PLAYING || p.invincibilityTimer > 0) return;
        double actual = dmg * (1 - p.armor);
        p.hp -= actual;
        p.invincibilityTimer = 60;
        floatTexts.add(new FloatText(p.x, p.y - 30, "-" + (int) Math.ceil(actual), Color.RED));
        if (p.hp <= 0) {
            p.hp = 0;
            p.animState = 4; 
            p.animFrame = 0;
            p.animTimer = 0;
            p.invincibilityTimer = 0;
            mouseHeld = false;
            setState(GameState.GAME_OVER);
        }
    }

    private void heal(double amount, boolean showText) {
        Player p = player;
        double before = p.hp;
        p.hp = Math.min(p.maxHp, p.hp + amount);
        double gained = p.hp - before;
        if (showText && gained >= 1) floatTexts.add(new FloatText(p.x, p.y - 30, "+" + (int) gained, new Color(80, 220, 100)));
    }

    private void gainXP(double amount) {
        player.xp += amount * player.xpMultiplier;
    }

    private void addGem(double x, double y, int value) {
        if (gems.size() >= MAX_GEMS) { 
            gems.get(random.nextInt(gems.size())).value += value;
            return;
        }
        gems.add(new Gem(x, y, value));
    }

    private void startLevelUp() {
        Player p = player;
        p.level++;
        p.xp -= p.xpToNextLevel;
        p.xpToNextLevel = Math.round(p.xpToNextLevel * 1.3 + 2);

        generateUpgrades();
        currentBookFrame = 0;
        bookAnimTimer = 0;
        bookOpen = false;
        upPressed = downPressed = leftPressed = rightPressed = false;
        setState(GameState.LEVEL_UP);
    }

    private void generateUpgrades() {
        currentChoices.clear();
        List<Card> pool = new ArrayList<>();
        for (Card c : CARDS) if (player.cardLevels[c.ordinal()] < c.maxLevel) pool.add(c);

        for (int pick = 0; pick < 2 && !pool.isEmpty(); pick++) {
            int total = 0;
            for (Card c : pool) total += c.rarity.weight;
            int roll = random.nextInt(total);
            int acc = 0;
            for (int i = 0; i < pool.size(); i++) {
                acc += pool.get(i).rarity.weight;
                if (roll < acc) {
                    currentChoices.add(pool.remove(i));
                    break;
                }
            }
        }
    }

    private void chooseCard(int index) {
        if (index < 0 || index >= currentChoices.size()) return;
        applyCard(currentChoices.get(index));
        setState(GameState.PLAYING);
    }

    private void applyCard(Card c) {
        Player p = player;
        int lvl = ++p.cardLevels[c.ordinal()];
        switch (c) {
            case SHARP_EDGE: p.damage *= 1.2; break;
            case LONG_SHOT: p.attackRange *= 1.15; break;
            case QUICK_DRAW: p.attackCooldown = Math.max(5, p.attackCooldown * 0.88); break;
            case SWIFT_FEET: p.moveSpeed *= 1.08; break;
            case VITALITY: p.maxHp += 20; p.hp = Math.min(p.maxHp, p.hp + 20); break;
            case MENDING: heal(p.maxHp * 0.4, true); break;
            case FLEET_SHOT: p.projSpeed *= 1.2; break;
            case SCHOLAR: p.xpMultiplier += 0.25; break;
            case MAGNETISM: p.pickupRadius *= 1.4; break;
            case IRON_SKIN: p.armor = Math.min(0.6, p.armor + 0.08); break;
            case CRITICAL: p.critChance += 0.10; break;
            case LEECH: p.leech += 1; break;
            case REGEN: p.regen += 0.6; break;
            case FORTUNE: p.chestSpeed += 0.25; break;
            case TWIN_SHOT: p.projectileCount++; break;
            case PIERCING: p.pierce++; break;
            case BLAST: p.blastRadius = 30 + 20 * lvl; break;
            case GLASS_CANNON: p.damage *= 1.6; p.maxHp *= 0.8; p.hp = Math.min(p.hp, p.maxHp); break;
        }
        if (discovered.add(c)) saveJournal();
    }

    private void openChest(Chest c) {
        Item it = rollItem();
        player.itemCounts[it.ordinal()]++;
        if (!it.consumable) applyPassive(it);
        rings.add(new Ring(c.x, c.y, 60, new Color(255, 215, 0)));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            addGem(c.x + Math.cos(a) * 25, c.y + Math.sin(a) * 25, 3);
        }
        showToast("FOUND " + it.label + "!");
    }

    private Item rollItem() {
        int total = 0;
        for (Item it : ITEMS) total += it.weight;
        int roll = random.nextInt(total);
        int acc = 0;
        for (Item it : ITEMS) {
            acc += it.weight;
            if (roll < acc) return it;
        }
        return Item.POTION;
    }

    private void applyPassive(Item it) {
        Player p = player;
        switch (it) {
            case BOOTS: p.moveSpeed += 0.35; break;
            case HEART_CHARM: p.maxHp += 15; p.hp = Math.min(p.maxHp, p.hp + 15); break;
            case CLOVER: p.critChance += 0.04; break;
            case IRON_RING: p.armor = Math.min(0.6, p.armor + 0.04); break;
            default: break;
        }
    }

    private void useItem(int slot) {
        if (slot >= 0 && slot < ITEMS.length) useItem(ITEMS[slot]);
    }

    private void useItem(Item it) {
        Player p = player;
        if (!it.consumable || p.itemCounts[it.ordinal()] <= 0) return;
        switch (it) {
            case POTION:
                if (p.hp >= p.maxHp) return; 
                heal(p.maxHp * 0.4, true);
                break;
            case BOMB: {
                double radius = 220;
                double dmg = 60 + 6 * p.level;
                rings.add(new Ring(p.x, p.y, radius, new Color(255, 90, 40)));
                for (Enemy en : enemies) {
                    double r = radius + en.size / 2.0;
                    if (distSq(p.x, p.y, en.x, en.y) < r * r) damageEnemy(en, dmg, false);
                }
                break;
            }
            case FREEZE_ORB: freezeTimer = 240; break;
            case RAGE_TONIC: rageTimer = 600; break;
            case MAGNET_ORB: for (Gem g : gems) g.pulled = true; break;
            default: return;
        }
        p.itemCounts[it.ordinal()]--;
        showToast("USED " + it.label);
    }

    private EnemyType pickEnemyType() {
        int total = 0;
        for (EnemyType t : ENEMY_TYPES) if (t.weight > 0 && t.unlockWave <= wave) total += t.weight;
        int roll = random.nextInt(total);
        int acc = 0;
        for (EnemyType t : ENEMY_TYPES) {
            if (t.weight <= 0 || t.unlockWave > wave) continue;
            acc += t.weight;
            if (roll < acc) return t;
        }
        return EnemyType.RAT;
    }

    private void spawnEnemy(EnemyType type) {
        Enemy en = new Enemy(0, 0, type, wave);
        placeOnSpawnRing(en);
        enemies.add(en);
    }

    private void placeOnSpawnRing(Enemy en) {
        double a = random.nextDouble() * Math.PI * 2;
        double r = 520 + random.nextDouble() * 80; 
        en.x = player.x + Math.cos(a) * r;
        en.y = player.y + Math.sin(a) * r;
    }

    private void spawnChest() {
        double a = random.nextDouble() * Math.PI * 2;
        double r = 350 + random.nextDouble() * 300;
        chests.add(new Chest(player.x + Math.cos(a) * r, player.y + Math.sin(a) * r));
    }

    private int nextChestDelay() {
        double base = 900 + random.nextInt(600);
        return (int) (base / (1 + (player == null ? 0 : player.chestSpeed)));
    }

    private void showBanner(String text) { bannerText = text; bannerTimer = 150; }
    private void showToast(String text) { toastText = text; toastTimer = 150; }

    private void startGame(HeroClass hero) {
        player = new Player(hero);
        enemies.clear();
        pendingSpawns.clear();
        projectiles.clear();
        gems.clear();
        chests.clear();
        rings.clear();
        floatTexts.clear();
        frameCount = 0;
        spawnRate = 120;
        spawnTimer = 60;
        wave = 1;
        enemiesKilledThisWave = 0;
        enemiesTotalThisWave = 20;
        totalKills = 0;
        rageTimer = freezeTimer = 0;
        toastTimer = 0;
        upPressed = downPressed = leftPressed = rightPressed = false;
        mouseHeld = false;
        chestTimer = nextChestDelay();
        updateCamera();
        showBanner("WAVE 1");
        setState(GameState.PLAYING);
    }

    private static double distSq(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        return dx * dx + dy * dy;
    }

    private static double segDistSq(double px, double py, double ax, double ay, double bx, double by) {
        double abx = bx - ax, aby = by - ay;
        double len2 = abx * abx + aby * aby;
        double t = len2 == 0 ? 0 : ((px - ax) * abx + (py - ay) * aby) / len2;
        t = Math.max(0, Math.min(1, t));
        return distSq(ax + abx * t, ay + aby * t, px, py);
    }

    private boolean onScreen(double wx, double wy, int margin) {
        double sx = wx - camX, sy = wy - camY;
        return sx > -margin && sx < WIDTH + margin && sy > -margin && sy < HEIGHT + margin;
    }

    // =====================================================================
    //  TEXT HELPERS
    // =====================================================================
    private Font getBoldFont(int size) {
        return boldFonts.computeIfAbsent(size, s -> new Font("Arial", Font.BOLD, s));
    }

    private void drawCustomText(Graphics2D g, String text, int x, int y, int scale) {
        if (textSheet == null) {
            g.setColor(Color.BLACK);
            g.setFont(getBoldFont(12 * scale));
            g.drawString(text, x, y + 10 * scale);
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int idx = c < 128 ? GLYPH_INDEX[c] : -1;
            if (idx != -1) {
                int sx = (idx % 6) * GLYPH_W;
                int sy = (idx / 6) * GLYPH_H;
                g.drawImage(textSheet, x, y, x + GLYPH_W * scale, y + GLYPH_H * scale,
                        sx, sy, sx + GLYPH_W, sy + GLYPH_H, null);
            }
            x += GLYPH_W * scale;
        }
    }

    private int customTextWidth(String text, int scale) {
        return text.length() * GLYPH_W * scale;
    }

    private void drawCustomTextCentered(Graphics2D g, String text, int centerX, int y, int scale) {
        drawCustomText(g, text, centerX - customTextWidth(text, scale) / 2, y, scale);
    }

    private int labelBoxWidth(String text, int scale) {
        return customTextWidth(text, scale) + 16;
    }

    private void drawLabelBox(Graphics2D g, String text, int x, int y, int scale) {
        int w = labelBoxWidth(text, scale);
        int h = GLYPH_H * scale + 10;
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 8, 8);
        drawCustomText(g, text, x + 8, y + 5, scale);
    }

    private void drawLabelBoxCentered(Graphics2D g, String text, int cx, int y, int scale) {
        drawLabelBox(g, text, cx - labelBoxWidth(text, scale) / 2, y, scale);
    }

    private void drawSmallCustomText(Graphics2D g, String text, int x, int y) {
        if (smallTextSheet == null) return;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int idx = c < 128 ? GLYPH_INDEX[c] : -1;
            if (idx != -1) {
                int sx = (idx % 6) * smallGlyphW;
                int sy = (idx / 6) * smallGlyphH;
                g.drawImage(smallTextSheet, x, y, x + smallGlyphW, y + smallGlyphH,
                        sx, sy, sx + smallGlyphW, sy + smallGlyphH, null);
            }
            x += smallGlyphW;
        }
    }

    private int smallTextWidth(String text) {
        if (smallTextSheet == null) return text.length() * 8;
        return text.length() * smallGlyphW;
    }

    private void drawSmallLabelBox(Graphics2D g, String text, int x, int y) {
        int w = smallTextWidth(text) + 16;
        int h = smallGlyphH + 10;
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(BORDER);
        g.drawRoundRect(x, y, w, h, 8, 8);
        drawSmallCustomText(g, text, x + 8, y + 5);
    }

    private void drawCenteredString(Graphics2D g, String text, int y, Color c, int size) {
        g.setColor(c);
        g.setFont(getBoldFont(size));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, y);
    }

    private int drawWrappedCentered(Graphics2D g, String text, int cx, int y, int maxW, int lineH) {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), cx - fm.stringWidth(line.toString()) / 2, y);
                y += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            g.drawString(line.toString(), cx - fm.stringWidth(line.toString()) / 2, y);
            y += lineH;
        }
        return y;
    }

    // =====================================================================
    //  DRAWING
    // =====================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        switch (currentState) {
            case MENU: drawMenu(g2d); break;
            case CHAR_SELECT: drawCharSelect(g2d); break;
            case JOURNAL: drawJournal(g2d); break;
            case PLAYING:
                drawWorld(g2d);
                drawHud(g2d);
                drawReticle(g2d);
                break;
            case PAUSED:
                drawWorld(g2d);
                drawHud(g2d);
                drawPause(g2d);
                break;
            case LEVEL_UP:
                drawWorld(g2d);
                drawHud(g2d);
                drawLevelUp(g2d);
                break;
            case GAME_OVER:
                drawWorld(g2d);
                drawGameOver(g2d);
                break;
        }
    }

    private void drawStars(Graphics2D g) {
        for (Star s : menuStars) {
            BufferedImage[] frames = starSprites[s.type];
            if (frames != null && s.frameIndex < frames.length && frames[s.frameIndex] != null) {
                int size = 48;
                g.drawImage(frames[s.frameIndex], s.x - size / 2, s.y - size / 2, size, size, null);
            }
        }
    }

    private void drawMenu(Graphics2D g) {
        drawStars(g);
        
        int titleY = HEIGHT / 3 - 15;
        if (titleImg != null) {
            int imgW = 750; 
            int imgH = 150;
            g.drawImage(titleImg, HALF_W - imgW / 2, titleY - 38, imgW, imgH, null);
            drawCustomTextCentered(g, "SIMPLE ROUGELIKE", HALF_W, titleY + 20, 1);
        } else {
            drawLabelBoxCentered(g, "SIMPLE ROUGELIKE", HALF_W, titleY, 1);
        }
        for (UIButton b : menuButtons) b.draw(g);

        g.setFont(FONT_BODY);
        g.setColor(Color.LIGHT_GRAY);
        String hint = "WASD move   |   Hold left click to shoot   |   1-9 use items   |   Esc pause";
        g.drawString(hint, HALF_W - g.getFontMetrics().stringWidth(hint) / 2, HEIGHT - 20);
    }

    private void drawHeroIcon(Graphics2D g, HeroClass h, int x, int y, int size, int tick) {
        BufferedImage[][] sp = heroSprites.get(h);
        if (sp != null) {
            g.drawImage(sp[4][2 + Math.floorMod(tick, 4)], x, y, size, size, null); 
        } else {
            g.setColor(h.color);
            g.fillOval(x, y, size, size);
        }
    }

    private void drawCharSelect(Graphics2D g) {
        drawStars(g);
        drawLabelBoxCentered(g, "SELECT YOUR CLASS", HALF_W, 25, 3);
        for (UIButton b : backButtons) b.draw(g);

        for (int i = 0; i < HEROES.length; i++) {
            HeroClass h = HEROES[i];
            Rectangle r = heroCards[i];
            boolean hover = r.contains(mouseX, mouseY);
            int cx = r.x + r.width / 2;

            g.setColor(hover ? PANEL_HOVER : PANEL_FILL);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            g.setColor(hover ? h.color : BORDER);
            g.setStroke(THICK);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
            g.setStroke(THIN);

            drawHeroIcon(g, h, cx - 56, r.y + 14, 112, hover ? uiTick / 6 : 0);
            drawCustomTextCentered(g, h.label, cx, r.y + 140, 2);

            String[] stats = {
                "HP: " + (int) h.maxHp,
                "DAMAGE: " + (int) h.damage,
                "RANGE: " + (int) h.range,
                "SPEED: " + String.format("%.1f", h.speed),
                "ATK SPD: " + String.format("%.1f", 60.0 / h.cooldown)
            };
            for (int s = 0; s < stats.length; s++) drawSmallCustomText(g, stats[s], r.x + 30, r.y + 172 + s * 16);

            g.setFont(FONT_BODY);
            g.setColor(TEXT_DARK);
            drawWrappedCentered(g, h.description, cx, r.y + 270, r.width - 30, 15);
        }
    }

    private void drawJournal(Graphics2D g) {
        drawStars(g);
        
        if (titleImg != null) {
            int imgW = 400; 
            int imgH = 80;  
            g.drawImage(titleImg, HALF_W - imgW / 2, 10, imgW, imgH, null);
            drawCustomTextCentered(g, "JOURNAL", HALF_W, 35, 1);
        } else {
            drawLabelBoxCentered(g, "JOURNAL", HALF_W, 15, 3);
        }
        
        for (UIButton b : backButtons) b.draw(g);

        int rows = (CARDS.length + 1) / 2;
        int top = 85;
        int rowH = Math.min(62, (HEIGHT - top - 45) / rows);
        int panelW = 375, panelH = rowH - 6;
        int x0 = (WIDTH - (2 * panelW + 10)) / 2;

        for (int i = 0; i < CARDS.length; i++) {
            Card c = CARDS[i];
            boolean found = discovered.contains(c);
            int x = x0 + (i % 2) * (panelW + 10);
            int y = top + (i / 2) * rowH;

            g.setColor(found ? PANEL_FILL : new Color(150, 150, 150));
            g.fillRoundRect(x, y, panelW, panelH, 10, 10);
            g.setColor(found ? c.rarity.color : Color.DARK_GRAY);
            g.drawRoundRect(x, y, panelW, panelH, 10, 10);

            if (found) {
                drawSmallCustomText(g, c.label, x + 10, y + 6);
                g.setFont(FONT_BODY_BOLD);
                g.setColor(c.rarity.color);
                String tag = c.rarity.name() + (c.maxLevel >= 99 ? "" : "  MAX LV " + c.maxLevel);
                g.drawString(tag, x + panelW - 10 - g.getFontMetrics().stringWidth(tag), y + 17);
                g.setFont(FONT_BODY);
                g.setColor(TEXT_DARK);
                g.drawString(c.description, x + 10, y + panelH - 8);
            } else {
                drawSmallCustomText(g, "???", x + 10, y + 6);
                g.setFont(FONT_BODY);
                g.setColor(Color.DARK_GRAY);
                g.drawString("Pick this card in a run to discover it.", x + 10, y + panelH - 8);
            }
        }
        drawLabelBoxCentered(g, "DISCOVERED: " + discovered.size() + "/" + CARDS.length, HALF_W, HEIGHT - 38, 1);
    }

    private void drawWorld(Graphics2D g) {
        int startX = -Math.floorMod(camX, TILE);
        int startY = -Math.floorMod(camY, TILE);
        for (int x = startX; x < WIDTH; x += TILE)
            for (int y = startY; y < HEIGHT; y += TILE)
                g.drawImage(groundTile, x, y, null);

        AffineTransform saved = g.getTransform();
        g.translate(-camX, -camY);

        for (Chest c : chests) if (onScreen(c.x, c.y, 40)) drawChest(g, c);

        for (Gem gem : gems) {
            if (!onScreen(gem.x, gem.y, 20)) continue;
            g.setColor(GEM_COLORS[gem.value >= 10 ? 2 : gem.value >= 4 ? 1 : 0]);
            g.fillOval((int) gem.x - 4, (int) gem.y - 4, 8, 8);
            g.setColor(Color.WHITE);
            g.fillRect((int) gem.x - 1, (int) gem.y - 2, 2, 2);
        }

        for (Enemy en : enemies) if (onScreen(en.x, en.y, 60)) drawEnemy(g, en);

        drawPlayer(g);

        for (Projectile pr : projectiles) {
            if (!onScreen(pr.x, pr.y, 20)) continue;
            g.setColor(pr.crit ? CRIT_COLOR : pr.color);
            g.fillOval((int) pr.x - pr.radius, (int) pr.y - pr.radius, pr.radius * 2, pr.radius * 2);
        }

        Composite oldComp = g.getComposite();
        for (Ring r : rings) {
            float a = Math.max(0f, (float) r.life / r.maxLife);
            int rad = (int) (r.radius * (0.3 + 0.7 * (1 - a)));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(r.color);
            g.setStroke(THICK);
            g.drawOval((int) r.x - rad, (int) r.y - rad, rad * 2, rad * 2);
        }
        g.setComposite(oldComp);
        g.setStroke(THIN);

        g.setFont(FONT_FLOAT);
        for (FloatText f : floatTexts) {
            g.setColor(Color.BLACK);
            g.drawString(f.text, (int) f.x + 1, (int) f.y + 1);
            g.setColor(f.color);
            g.drawString(f.text, (int) f.x, (int) f.y);
        }

        g.setTransform(saved);
        drawChestArrows(g);
    }

    private void drawPlayer(Graphics2D g) {
        Player p = player;
        if (p.invincibilityTimer > 0 && (p.invincibilityTimer / 5) % 2 != 0) return; 

        int size = 48;
        int px = (int) Math.round(p.x) - size / 2;
        int py = (int) Math.round(p.y) - size / 2;
        g.setColor(SHADOW);
        g.fillOval(px + 8, py + size - 10, size - 16, 10);

        BufferedImage[][] sp = heroSprites.get(p.hero);
        if (sp == null) { 
            g.setColor(p.hero.color);
            g.fillOval((int) p.x - 15, (int) p.y - 15, 30, 30);
            return;
        }
        BufferedImage img = null;
        switch (p.animState) {
            case 0: img = sp[4][2 + (p.animFrame % 4)]; break;          
            case 1: img = sp[0][1 + (p.animFrame % 6)]; break;          
            case 2: img = sp[2][2 + (p.animFrame % 4)]; break;          
            case 3: img = sp[3][3 + (p.animFrame % 2)]; break;          
            case 4: img = sp[1][Math.min(7, p.animFrame)]; break;       
        }
        if (img == null) return;
        if (p.facingRight) g.drawImage(img, px, py, size, size, null);
        else g.drawImage(img, px + size, py, -size, size, null);
    }

    private void drawEnemy(Graphics2D g, Enemy en) {
        int half = en.size / 2;
        int ex = (int) en.x, ey = (int) en.y;

        if (enemySprites != null && en.type.ordinal() < enemySprites.length && enemySprites[en.type.ordinal()] != null) {
            BufferedImage[] row = enemySprites[en.type.ordinal()];
            BufferedImage img = row[(frameCount / 8) % row.length];
            int ds = en.size * 2;
            if (player.x >= en.x) g.drawImage(img, ex - ds / 2, ey - ds / 2, ds, ds, null);
            else g.drawImage(img, ex + ds / 2, ey - ds / 2, -ds, ds, null);
            return;
        }

        g.setColor(SHADOW);
        g.fillOval(ex - half, ey + half - 4, en.size, 8);
        g.setColor(en.hitFlash > 0 ? Color.WHITE : freezeTimer > 0 ? FROZEN : en.type.color);
        g.fillOval(ex - half, ey - half, en.size, en.size);
        g.setColor(OUTLINE);
        g.drawOval(ex - half, ey - half, en.size, en.size);
    }

    private void drawChest(Graphics2D g, Chest c) {
        int x = (int) c.x, y = (int) c.y;
        g.setColor(SHADOW);
        g.fillOval(x - 16, y + 6, 32, 8);
        g.setColor(new Color(120, 75, 35));
        g.fillRoundRect(x - 14, y - 10, 28, 20, 6, 6);
        g.setColor(new Color(90, 55, 25));
        g.fillRoundRect(x - 14, y - 14, 28, 9, 8, 8);
        g.setColor(new Color(240, 200, 60));
        g.fillRect(x - 3, y - 14, 6, 24);
        g.setColor(BORDER);
        g.drawRoundRect(x - 14, y - 14, 28, 24, 6, 6);
    }

    private void drawChestArrows(Graphics2D g) {
        Player p = player;
        for (Chest c : chests) {
            if (onScreen(c.x, c.y, -10)) continue; 
            double ang = Math.atan2(c.y - p.y, c.x - p.x);
            double cos = Math.cos(ang), sin = Math.sin(ang);
            double tx = Math.abs(cos) > 1e-6 ? (HALF_W - 25) / Math.abs(cos) : Double.MAX_VALUE;
            double ty = Math.abs(sin) > 1e-6 ? (HALF_H - 25) / Math.abs(sin) : Double.MAX_VALUE;
            double t = Math.min(tx, ty);

            AffineTransform saved = g.getTransform();
            g.translate(HALF_W + cos * t, HALF_H + sin * t);
            g.rotate(ang);
            g.setColor(new Color(240, 200, 60));
            g.fillPolygon(new int[] { 10, -8, -8 }, new int[] { 0, -7, 7 }, 3);
            g.setColor(BORDER);
            g.drawPolygon(new int[] { 10, -8, -8 }, new int[] { 0, -7, 7 }, 3);
            g.setTransform(saved);
        }
    }

    private void drawReticle(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setStroke(THIN);
        g.drawOval(mouseX - 8, mouseY - 8, 16, 16);
        g.drawLine(mouseX - 12, mouseY, mouseX - 4, mouseY);
        g.drawLine(mouseX + 4, mouseY, mouseX + 12, mouseY);
        g.drawLine(mouseX, mouseY - 12, mouseX, mouseY - 4);
        g.drawLine(mouseX, mouseY + 4, mouseX, mouseY + 12);
    }

    private void drawHud(Graphics2D g) {
        Player p = player;
        int uiX = 10, uiY = 10;

        Shape oldClip = g.getClip();
        g.setClip(portraitClip);
        g.setColor(p.hero.color);
        g.fillRect(uiX + 10, uiY + 10, 55, 55);
        drawHeroIcon(g, p.hero, uiX + 1, uiY + 3, 72, uiTick / 8);
        g.setClip(oldClip);

        double hpPercent = Math.max(0, p.hp / p.maxHp);
        double xpPercent = Math.min(1.0, p.xp / p.xpToNextLevel);
        double wavePercent = Math.max(0, (double) (enemiesTotalThisWave - enemiesKilledThisWave) / enemiesTotalThisWave);
        int barX = uiX + 65;
        int maxBarWidth = 120;

        g.setColor(new Color(100, 0, 0));
        g.fillRect(barX, uiY + 15, maxBarWidth, 8);
        g.setColor(Color.RED);
        g.fillRect(barX, uiY + 15, (int) (maxBarWidth * hpPercent), 8);

        g.setColor(new Color(0, 0, 100));
        g.fillRect(barX, uiY + 30, maxBarWidth, 8);
        g.setColor(new Color(50, 150, 255));
        g.fillRect(barX, uiY + 30, (int) (maxBarWidth * xpPercent), 8);

        g.setColor(new Color(0, 100, 0));
        g.fillRect(barX, uiY + 45, maxBarWidth, 8);
        g.setColor(Color.GREEN);
        g.fillRect(barX, uiY + 45, (int) (maxBarWidth * wavePercent), 8);

        int panelH = 70;
        if (uiPanelImg != null) {
            g.drawImage(uiPanelImg, uiX, uiY, null);
            panelH = Math.max(panelH, uiPanelImg.getHeight(null));
        } else {
            g.setColor(Color.RED);
            g.drawString("Missing character_panel.png!", uiX, uiY + 20);
        }

        int ty = uiY + panelH + 6;
        drawSmallLabelBox(g, "LV " + p.level + "  WAVE " + wave, uiX, ty);
        if (rageTimer > 0) { ty += 24; drawSmallLabelBox(g, "RAGE " + (rageTimer / 60 + 1) + "S", uiX, ty); }
        if (freezeTimer > 0) { ty += 24; drawSmallLabelBox(g, "FREEZE " + (freezeTimer / 60 + 1) + "S", uiX, ty); }

        int secs = frameCount / 60;
        String time = String.format("TIME %02d:%02d", secs / 60, secs % 60);
        String kills = "KILLS: " + totalKills;
        drawSmallLabelBox(g, time, WIDTH - 10 - (smallTextWidth(time) + 16), 10);
        drawSmallLabelBox(g, kills, WIDTH - 10 - (smallTextWidth(kills) + 16), 34);

        if (bannerTimer > 0) drawLabelBoxCentered(g, bannerText, HALF_W, 70, 3);
        if (toastTimer > 0) drawLabelBoxCentered(g, toastText, HALF_W, 125, 1);

        drawHotbar(g);
    }

    private int hotbarX() {
        return (WIDTH - (ITEMS.length * SLOT + (ITEMS.length - 1) * SLOT_GAP)) / 2;
    }

    private int hotbarSlotAt(int mx, int my) {
        if (my < HOTBAR_Y || my > HOTBAR_Y + SLOT) return -1;
        int rel = mx - hotbarX();
        if (rel < 0) return -1;
        int i = rel / (SLOT + SLOT_GAP);
        if (i >= ITEMS.length || rel % (SLOT + SLOT_GAP) > SLOT) return -1;
        return i;
    }

    private void drawHotbar(Graphics2D g) {
        int x0 = hotbarX();
        int hover = hotbarSlotAt(mouseX, mouseY);
        for (int i = 0; i < ITEMS.length; i++) {
            Item it = ITEMS[i];
            int count = player.itemCounts[i];
            int x = x0 + i * (SLOT + SLOT_GAP);

            g.setColor(SLOT_FILL);
            g.fillRoundRect(x, HOTBAR_Y, SLOT, SLOT, 8, 8);
            g.setColor(i == hover ? Color.ORANGE : (it.consumable ? BORDER : new Color(190, 150, 40)));
            g.setStroke(i == hover ? THICK : THIN);
            g.drawRoundRect(x, HOTBAR_Y, SLOT, SLOT, 8, 8);
            g.setStroke(THIN);

            drawSmallCustomText(g, String.valueOf(i + 1), x + 4, HOTBAR_Y + 3);
            if (count > 0) {
                g.setColor(it.color);
                g.fillRoundRect(x + 9, HOTBAR_Y + 9, 26, 26, 8, 8);
                g.setFont(getBoldFont(16));
                g.setColor(Color.WHITE);
                String letter = it.label.substring(0, 1);
                g.drawString(letter, x + 22 - g.getFontMetrics().stringWidth(letter) / 2, HOTBAR_Y + 28);
                String c = String.valueOf(count);
                drawSmallCustomText(g, c, x + SLOT - 4 - smallTextWidth(c), HOTBAR_Y + SLOT - 15);
            }
        }

        if (hover >= 0 && player.itemCounts[hover] > 0) {
            Item it = ITEMS[hover];
            String tip = it.label + ": " + it.description;
            g.setFont(FONT_BODY);
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(tip) + 16;
            int x = Math.max(5, Math.min(WIDTH - w - 5, mouseX - w / 2));
            g.setColor(PANEL_FILL);
            g.fillRoundRect(x, HOTBAR_Y - 30, w, 22, 8, 8);
            g.setColor(BORDER);
            g.drawRoundRect(x, HOTBAR_Y - 30, w, 22, 8, 8);
            g.setColor(TEXT_DARK);
            g.drawString(tip, x + 8, HOTBAR_Y - 15);
        }
    }

    private void drawPause(Graphics2D g) {
        g.setColor(OVERLAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawLabelBoxCentered(g, "PAUSED", HALF_W, 130, 4);
        for (UIButton b : pauseButtons) b.draw(g);
        g.setFont(FONT_BODY);
        g.setColor(Color.LIGHT_GRAY);
        String hint = "Esc or P to resume";
        g.drawString(hint, HALF_W - g.getFontMetrics().stringWidth(hint) / 2, HEIGHT - 40);
    }

    private void drawLevelUp(Graphics2D g) {
        g.setColor(OVERLAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawCenteredString(g, "LEVEL UP!", 100, Color.YELLOW, 40);

        int bookX = HALF_W - 200;
        int bookY = HALF_H - 150;
        BufferedImage frame = bookFrames != null ? bookFrames[currentBookFrame] : null;
        if (frame != null) {
            g.drawImage(frame, bookX, bookY, 400, 300, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(bookX, bookY, 400, 300);
        }
        if (!bookOpen) return;

        for (int i = 0; i < currentChoices.size(); i++) {
            int px = bookX + i * 200;
            if (mouseX >= px && mouseX < px + 200 && mouseY >= bookY && mouseY <= bookY + 300) {
                g.setColor(new Color(255, 255, 255, 50));
                g.fillRect(px + 10, bookY + 10, 180, 280);
            }
        }

        int nameY = bookY + 95, rarityY = bookY + 128, descY = bookY + 152, levelY = bookY + 235;
        for (int i = 0; i < currentChoices.size(); i++) {
            Card c = currentChoices.get(i);
            int cx = bookX + (i == 0 ? 110 : 310);

            drawCustomTextCentered(g, c.label, cx, nameY, 1);

            g.setFont(FONT_BODY_BOLD);
            g.setColor(c.rarity.color);
            String rar = c.rarity.name();
            g.drawString(rar, cx - g.getFontMetrics().stringWidth(rar) / 2, rarityY);

            g.setFont(FONT_BODY);
            g.setColor(TEXT_DARK);
            drawWrappedCentered(g, c.description, cx, descY, 140, 15);

            int next = player.cardLevels[c.ordinal()] + 1;
            String lvl = next == 1 ? "New!" : "Level " + next;
            g.setFont(FONT_BODY_BOLD);
            g.drawString(lvl, cx - g.getFontMetrics().stringWidth(lvl) / 2, levelY);
        }

        g.setFont(FONT_BODY);
        g.setColor(Color.LIGHT_GRAY);
        String hint = "Click a page or press 1 / 2";
        g.drawString(hint, HALF_W - g.getFontMetrics().stringWidth(hint) / 2, bookY + 335);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(OVERLAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawCenteredString(g, "GAME OVER", 150, Color.RED, 50);

        int secs = frameCount / 60;
        String[] lines = {
            "CLASS: " + player.hero.label,
            "LEVEL: " + player.level,
            "WAVE: " + wave,
            "KILLS: " + totalKills,
            String.format("TIME: %02d:%02d", secs / 60, secs % 60)
        };
        for (int i = 0; i < lines.length; i++) drawLabelBoxCentered(g, lines[i], HALF_W, 200 + i * 40, 2);

        if (stateTimer > 45) drawCenteredString(g, "Click anywhere to return to the menu", HEIGHT - 60, Color.WHITE, 20);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        int mx = e.getX(), my = e.getY();
        mouseX = mx;
        mouseY = my;

        switch (currentState) {
            case MENU:
            case JOURNAL:
            case PAUSED:
                pressButtons(mx, my);
                break;
            case CHAR_SELECT:
                if (!pressButtons(mx, my)) {
                    for (int i = 0; i < HEROES.length; i++) {
                        if (heroCards[i].contains(mx, my)) { startGame(HEROES[i]); break; }
                    }
                }
                break;
            case PLAYING: {
                int slot = hotbarSlotAt(mx, my);
                if (slot >= 0) useItem(slot);
                else mouseHeld = true;
                break;
            }
            case LEVEL_UP:
                if (bookOpen) {
                    int bookX = HALF_W - 200;
                    int bookY = HALF_H - 150;
                    if (my >= bookY && my <= bookY + 300) {
                        if (mx >= bookX && mx < bookX + 200) chooseCard(0);
                        else if (mx >= bookX + 200 && mx <= bookX + 400) chooseCard(1);
                    }
                }
                break;
            case GAME_OVER:
                if (stateTimer > 45) setState(GameState.MENU);
                break;
        }
    }

    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) mouseHeld = false;
    }
    @Override public void mouseMoved(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_W: case KeyEvent.VK_UP: upPressed = true; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN: downPressed = true; break;
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT: leftPressed = true; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: rightPressed = true; break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_P:
                if (!escHeld) {
                    escHeld = true;
                    handleEscape(key == KeyEvent.VK_ESCAPE);
                }
                break;
            default:
                if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_9) {
                    int n = key - KeyEvent.VK_1;
                    if (currentState == GameState.PLAYING) useItem(n);
                    else if (currentState == GameState.LEVEL_UP && bookOpen) chooseCard(n);
                }
        }
    }

    private void handleEscape(boolean isEsc) {
        switch (currentState) {
            case PLAYING: setState(GameState.PAUSED); break;
            case PAUSED: setState(GameState.PLAYING); break;
            case CHAR_SELECT:
            case JOURNAL:
                if (isEsc) setState(GameState.MENU);
                break;
            default: break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: case KeyEvent.VK_UP: upPressed = false; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN: downPressed = false; break;
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT: leftPressed = false; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: rightPressed = false; break;
            case KeyEvent.VK_ESCAPE: case KeyEvent.VK_P: escHeld = false; break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    class Player {
        final HeroClass hero;
        double x = 0, y = 0;
        double hp, maxHp, damage, attackRange, attackCooldown, moveSpeed, projSpeed;
        double xp = 0, xpToNextLevel = 10, xpMultiplier = 1.0;
        double critChance, armor, regen, leech, blastRadius, chestSpeed;
        double pickupRadius = 70;
        int projectileCount = 1, pierce = 0;
        int level = 1, cooldownTimer = 0, invincibilityTimer = 0;
        final int[] cardLevels = new int[CARDS.length];
        final int[] itemCounts = new int[ITEMS.length];

        boolean facingRight = true;
        int animState = 0, animFrame = 0, animTimer = 0, attackAnimTimer = 0;

        Player(HeroClass hero) {
            this.hero = hero;
            maxHp = hp = hero.maxHp;
            damage = hero.damage;
            attackRange = hero.range;
            attackCooldown = hero.cooldown;
            moveSpeed = hero.speed;
            projSpeed = hero.projSpeed;
        }
    }

    class Enemy {
        final EnemyType type;
        double x, y, hp, damage, speed;
        final int size;
        int hitFlash = 0, tick = random.nextInt(100), shootTimer = 60 + random.nextInt(60);

        Enemy(double x, double y, EnemyType type, int wave) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.size = type.size;
            this.speed = type.speed;
            this.hp = type.hp * (1 + 0.15 * (wave - 1));          
            this.damage = type.damage * (1 + 0.08 * (wave - 1));
        }

        void update(Player p) {
            tick++;
            double dx = p.x - x, dy = p.y - y;
            double dist = Math.hypot(dx, dy);
            if (dist < 0.001) return;
            double ux = dx / dist, uy = dy / dist;

            switch (type.behavior) {
                case WEAVE: {
                    double wob = Math.sin(tick * 0.15) * speed * 0.9;
                    x += ux * speed - uy * wob;
                    y += uy * speed + ux * wob;
                    break;
                }
                case RANGED:
                    if (dist > 280) { x += ux * speed; y += uy * speed; }
                    else if (dist < 190) { x -= ux * speed; y -= uy * speed; }
                    else { x += -uy * speed * 0.6; y += ux * speed * 0.6; }
                    if (--shootTimer <= 0 && dist < 480) {
                        shootTimer = 120;
                        projectiles.add(new Projectile(x, y, Math.atan2(dy, dx), damage, 4.5, 600, 0, false, true, ENEMY_SHOT));
                    }
                    break;
                default:
                    x += ux * speed;
                    y += uy * speed;
            }
        }
    }

    class Projectile {
        double x, y, px, py, vx, vy, damage, speed, remaining;
        int pierce, radius = 4;
        boolean crit, hostile, active = true;
        final Color color;
        private List<Enemy> hitList;

        Projectile(double x, double y, double angle, double damage, double speed, double range,
                   int pierce, boolean crit, boolean hostile, Color color) {
            this.x = px = x;
            this.y = py = y;
            this.damage = damage;
            this.speed = speed;
            this.remaining = range;
            this.pierce = pierce;
            this.crit = crit;
            this.hostile = hostile;
            this.color = color;
            this.vx = Math.cos(angle) * speed;
            this.vy = Math.sin(angle) * speed;
            if (hostile) radius = 5;
        }

        void update() {
            px = x;
            py = y;
            x += vx;
            y += vy;
            remaining -= speed;
            if (remaining <= 0) active = false;
        }

        boolean alreadyHit(Enemy e) { return hitList != null && hitList.contains(e); }
        void markHit(Enemy e) {
            if (hitList == null) hitList = new ArrayList<>(4);
            hitList.add(e);
        }
    }

    class Gem {
        double x, y, speed = 1;
        int value;
        boolean pulled;
        Gem(double x, double y, int value) { this.x = x; this.y = y; this.value = value; }
    }

    class Chest {
        final double x, y;
        Chest(double x, double y) { this.x = x; this.y = y; }
    }

    class Ring {
        final double x, y, radius;
        final Color color;
        final int maxLife = 25;
        int life = 25;
        Ring(double x, double y, double radius, Color color) { this.x = x; this.y = y; this.radius = radius; this.color = color; }
    }

    class FloatText {
        double x, y;
        final String text;
        final Color color;
        int life = 40;
        FloatText(double x, double y, String text, Color color) { this.x = x; this.y = y; this.text = text; this.color = color; }
    }

    class Star {
        int x, y, type;
        int frameIndex = 0, tickCounter = 0;
        boolean active = true;

        Star(int x, int y, int type) { this.x = x; this.y = y; this.type = type; }

        void update() {
            if (++tickCounter >= 10) { 
                tickCounter = 0;
                if (++frameIndex >= 9) active = false; 
            }
        }
    }

    class UIButton {
        final Rectangle bounds;
        final String label;
        final boolean sprite;
        final Runnable action;
        boolean pressed;
        int timer;

        UIButton(int x, int y, int w, int h, String label, boolean sprite, Runnable action) {
            this.bounds = new Rectangle(x, y, w, h);
            this.label = label;
            this.sprite = sprite;
            this.action = action;
        }

        void press() {
            if (!pressed) { pressed = true; timer = 0; }
        }

        void tick() {
            if (pressed && ++timer >= BUTTON_PRESS_FRAMES) {
                pressed = false;
                timer = 0;
                action.run();
            }
        }

        void draw(Graphics2D g) {
            int cx = bounds.x + bounds.width / 2;
            if (sprite && buttonFrames[0] != null && buttonFrames[1] != null) {
                g.drawImage(buttonFrames[pressed ? 1 : 0], bounds.x, bounds.y, bounds.width, bounds.height, null);
                double frac = pressed ? 0.50 : 0.40; 
                
                if (label.equals("BACK")) {
                    drawCustomTextCentered(g, label, cx, bounds.y + 35 + (int) (bounds.height * frac) - GLYPH_H, 1);
                } else {
                    drawCustomTextCentered(g, label, cx, bounds.y + 20 + (int) (bounds.height * frac) - GLYPH_H, 1);
                }
            } else {
                boolean hover = !pressed && bounds.contains(mouseX, mouseY);
                g.setColor(pressed ? Color.GRAY : hover ? Color.WHITE : Color.LIGHT_GRAY);
                g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
                g.setColor(BORDER);
                g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
                drawCustomTextCentered(g, label, cx, bounds.y + (bounds.height - GLYPH_H * 2) / 2 + (pressed ? 2 : 0), 1);
            }
        }
    }
}