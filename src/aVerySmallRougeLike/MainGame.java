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

    private static final int TILE = 64;                 
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
    private static final int SLOT_GAP = -1;

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
    
    private int[] slotOffsets = { 20, 60, 100, 140, 180, 220, 260, 300, 340, 380 }; 
    private int customSlotWidth = 35;

    // =====================================================================
    //  ENUMS
    // =====================================================================
    enum GameState { MENU, CHAR_SELECT, JOURNAL, PLAYING, PAUSED, LEVEL_UP, GAME_OVER }

    enum Behavior { CHASE, WEAVE, RANGED }

    enum MagicType { FIRE, EARTH, AIR, WATER, NONE }

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
        EPIC(12, new Color(150, 40, 190)),
        LEGENDARY(4, new Color(255, 215, 0)); 

        final int weight;
        final Color color;
        Rarity(int weight, Color color) { this.weight = weight; this.color = color; }
    }

    enum Card {
        SHARP_EDGE("SHARP EDGE", Rarity.COMMON, 8, "Damage +20%.", MagicType.NONE, -1, -1),
        LONG_SHOT("LONG SHOT", Rarity.COMMON, 6, "Attack range +15%.", MagicType.NONE, -1, -1),
        QUICK_DRAW("QUICK DRAW", Rarity.COMMON, 6, "Attack speed +12%.", MagicType.NONE, -1, -1),
        SWIFT_FEET("SWIFT FEET", Rarity.COMMON, 5, "Move speed +8%.", MagicType.NONE, -1, -1),
        VITALITY("VITALITY", Rarity.COMMON, 99, "Max HP +20 and heal 20.", MagicType.NONE, -1, -1),
        MENDING("MENDING", Rarity.COMMON, 99, "Heal 40% of your max HP.", MagicType.NONE, -1, -1),
        FLEET_SHOT("FLEET SHOT", Rarity.COMMON, 5, "Projectile speed +20%.", MagicType.NONE, -1, -1),
        SCHOLAR("SCHOLAR", Rarity.COMMON, 5, "XP gained +25%.", MagicType.NONE, -1, -1),
        MAGNETISM("MAGNETISM", Rarity.COMMON, 5, "XP pickup range +40%.", MagicType.NONE, -1, -1),
        IRON_SKIN("IRON SKIN", Rarity.RARE, 5, "Damage taken -8%.", MagicType.NONE, -1, -1),
        CRITICAL("CRITICAL", Rarity.RARE, 5, "Crit chance +10%. Crits deal double damage.", MagicType.NONE, -1, -1),
        LEECH("LEECH", Rarity.RARE, 4, "Heal 1 HP for every kill.", MagicType.NONE, -1, -1),
        REGEN("REGEN", Rarity.RARE, 4, "Regenerate 0.6 HP per second.", MagicType.NONE, -1, -1),
        FORTUNE("FORTUNE", Rarity.RARE, 4, "Chests appear 25% faster.", MagicType.NONE, -1, -1),
        TWIN_SHOT("TWIN SHOT", Rarity.EPIC, 3, "Fire one extra projectile.", MagicType.NONE, -1, -1),
        PIERCING("PIERCING", Rarity.EPIC, 3, "Shots pierce one more enemy.", MagicType.NONE, -1, -1),
        BLAST("BLAST", Rarity.EPIC, 3, "Shots explode and hurt nearby enemies.", MagicType.NONE, -1, -1),
        GLASS_CANNON("GLASS CANNON", Rarity.EPIC, 1, "Damage +60% but max HP -20%.", MagicType.NONE, -1, -1),
        
        // --- FIRE MAGIC (Col 0) ---
        BLAZING_TRAIL("BLAZING TRAIL", Rarity.LEGENDARY, 1, "Leave a permanent trail of fire behind you as you walk.", MagicType.FIRE, 0, 0),
        SEARING_GLARE("SEARING GLARE", Rarity.LEGENDARY, 1, "Any enemy you are currently facing directly takes continuous burn damage.", MagicType.FIRE, 1, 0),
        METEOR_STRIKE("METEOR STRIKE", Rarity.LEGENDARY, 1, "Every time you dash, a meteor crashes onto your starting location.", MagicType.FIRE, 2, 0),
        CAUTERIZE("CAUTERIZE", Rarity.LEGENDARY, 1, "Whenever you take damage, you release a blast of fire. Can heal if it kills.", MagicType.FIRE, 3, 0),
        FERAL_HEAT("FERAL HEAT", Rarity.LEGENDARY, 1, "Lower health boosts speed. At 10% health, attacks shoot fiery slashes.", MagicType.FIRE, 4, 0),
        
        // --- EARTH MAGIC (Col 1) ---
        MOMENTUM_MASS("MOMENTUM MASS", Rarity.LEGENDARY, 1, "The longer you move without stopping, the larger you get. Crush small enemies.", MagicType.EARTH, 0, 1),
        ROOTED_STANCE("ROOTED STANCE", Rarity.LEGENDARY, 1, "Stand still for 2s to gain massive armor and double damage.", MagicType.EARTH, 1, 1),
        TREMOR_STEP("TREMOR STEP", Rarity.LEGENDARY, 1, "Critical hits fracture the ground, slowing and damaging enemies.", MagicType.EARTH, 2, 1),
        OVERGROWTH("OVERGROWTH", Rarity.LEGENDARY, 1, "Health drops permanently increase your size and attack range.", MagicType.EARTH, 3, 1),
        LUCKY_RICOCHET("LUCKY RICOCHET", Rarity.LEGENDARY, 1, "Projectiles bounce off walls. Lower health = more bounces.", MagicType.EARTH, 4, 1),
        
        // --- AIR MAGIC (Col 2) ---
        CUMULONIMBUS("CUMULONIMBUS", Rarity.LEGENDARY, 1, "Absorb a hit and burst into a thick fog that blinds enemies.", MagicType.AIR, 0, 2),
        AFTERIMAGE("AFTERIMAGE", Rarity.LEGENDARY, 1, "Dashing leaves behind an explosive decoy clone.", MagicType.AIR, 1, 2),
        STATIC_CHARGE("STATIC CHARGE", Rarity.LEGENDARY, 1, "Moving builds static. Fully charged attacks release chain lightning.", MagicType.AIR, 2, 2),
        GALE_FORCE("GALE FORCE", Rarity.LEGENDARY, 1, "Aura of wind slows incoming enemy projectiles by 50%.", MagicType.AIR, 3, 2),
        FEATHERWEIGHT("FEATHERWEIGHT", Rarity.LEGENDARY, 1, "Hover above traps and spikes. Dash travels twice as far.", MagicType.AIR, 4, 2),
        
        // --- WATER MAGIC (Col 3) ---
        CRASHING_TIDE("CRASHING TIDE", Rarity.LEGENDARY, 1, "Killing an enemy unleashes a wave that pushes other enemies back.", MagicType.WATER, 0, 3),
        FLUID_FORM("FLUID FORM", Rarity.LEGENDARY, 1, "Taking damage teleports you back and leaves a slowing puddle.", MagicType.WATER, 1, 3),
        PIERCING_CHILL("PIERCING CHILL", Rarity.LEGENDARY, 1, "Attacks pierce. Enemies hit behind the first target are frozen for 3s.", MagicType.WATER, 2, 3),
        GLACIAL_TRAIL("GLACIAL TRAIL", Rarity.LEGENDARY, 1, "Leave a path of slick ice. Skate on your own ice for 3x speed.", MagicType.WATER, 3, 3),
        VORTEX_AURA("VORTEX AURA", Rarity.LEGENDARY, 1, "Whirlpool pulls loot and enemies. Stuns in melee range.", MagicType.WATER, 4, 3);

        final String label, description;
        final Rarity rarity;
        final int maxLevel;
        final MagicType magicType;
        final int iconRow, iconCol;
        Card(String label, Rarity rarity, int maxLevel, String description, MagicType magicType, int iconRow, int iconCol) {
            this.label = label; this.rarity = rarity; this.maxLevel = maxLevel; 
            this.description = description; this.magicType = magicType;
            this.iconRow = iconRow; this.iconCol = iconCol;
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

    private boolean upPressed, downPressed, leftPressed, rightPressed, spacePressed, escHeld;
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
    
    private final List<Wall> walls = new ArrayList<>(); 
    private final List<SpikeTrap> traps = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<Decoy> decoys = new ArrayList<>();
    private final List<Meteor> meteors = new ArrayList<>();

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
    private BufferedImage verySmallTextSheet;
    private static final int VERY_SMALL_GLYPH_W = 6;
    private static final int VERY_SMALL_GLYPH_H = 6;
    private BufferedImage cSelectImg;
    
    private final Map<HeroClass, BufferedImage[][]> heroSprites = new EnumMap<>(HeroClass.class);
    private BufferedImage[][] enemySprites;            
    private final BufferedImage[][] starSprites = new BufferedImage[5][];
    private final BufferedImage[] buttonFrames = new BufferedImage[2];
    
    private BufferedImage[] chest1Frames;
    private BufferedImage[] chest2Frames;
    private BufferedImage actionPanelImg;
    
    private BufferedImage[] itemIcons = new BufferedImage[9];
    private BufferedImage[] floorTiles = new BufferedImage[18];
    private BufferedImage[] wallSprites = new BufferedImage[9]; 
    private BufferedImage[] spikeFrames;
    
    private BufferedImage[][] magicIcons;
    private BufferedImage[] magicLogos;
    private BufferedImage[] magicNames;
    
    private double hotbarScale = 2.5;       
    private int hotbarBottomMargin = - 40;    
    private int hotbarXOffset = 0;          
    
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
        
        verySmallTextSheet = loadImage("VerySmallText.png");
        cSelectImg = loadImage("CSelect.png");
        
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
        if (new File("Chest1_D.png").exists()) chest1Frames = flatten(loadSheet("Chest1_D.png", 4, 1, false));
        if (new File("Chest2_D.png").exists()) chest2Frames = flatten(loadSheet("Chest2_D.png", 4, 1, false));
        if (new File("Action_panel.png").exists()) actionPanelImg = loadImage("Action_panel.png");
        
        if (new File("Spikes.png").exists()) spikeFrames = flatten(loadSheet("Spikes.png", 6, 1, false)); 
        if (new File("Icons.png").exists()) magicIcons = loadSheet("Icons.png", 4, 5, true);
        if (new File("MagicName.png").exists()) magicNames = flatten(loadSheet("MagicName.png", 1, 4, false));
        if (new File("MagicLogo.png").exists()) magicLogos = flatten(loadSheet("MagicLogo.png", 2, 2, false));
        
        BufferedImage itemSheet = loadImage("ItemIcon.png");
        if (itemSheet != null) {
            for (int i = 0; i < 9; i++) {
                int col = i % 3;
                int row = i / 3;
                itemIcons[i] = itemSheet.getSubimage(col * 15, row * 15, 15, 15);
            }
        }
        
        BufferedImage tileSheet = loadImage("TileSet.png");
        if (tileSheet != null) {
            int idx = 0;
            for (int y = 0; y < tileSheet.getHeight(); y += 16) {
                for (int x = 0; x < tileSheet.getWidth(); x += 16) {
                    if (idx < 18 && x + 16 <= tileSheet.getWidth() && y + 16 <= tileSheet.getHeight()) {
                        floorTiles[idx++] = tileSheet.getSubimage(x, y, 16, 16);
                    }
                }
            }
        }

        BufferedImage wallSheet = loadImage("Walls.png");
        if (wallSheet == null) {
            wallSheet = loadImage("Walls.png");
        }
        
        if (wallSheet != null) {
            int wCols = wallSheet.getWidth() / 50; 
            int wRows = wallSheet.getHeight() / 50;
            int idx = 0;
            for (int y = 0; y < wRows; y++) {
                for (int x = 0; x < wCols; x++) {
                    if (idx < 9 && (x * 50) + 50 <= wallSheet.getWidth() && (y * 50) + 50 <= wallSheet.getHeight()) {
                        wallSprites[idx++] = wallSheet.getSubimage(x * 50, y * 50, 50, 50); 
                    }
                }
            }
        }
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

        backButtons.add(new UIButton(15, 0, 150, 100, "BACK", true, () -> setState(GameState.MENU)));

        int cardW = 350, cardH = 340, cardGap = -80;
        int cx0 = (WIDTH - (HEROES.length * cardW + (HEROES.length - 1) * cardGap)) / 2;
        for (int i = 0; i < HEROES.length; i++) heroCards[i] = new Rectangle(cx0 + i * (cardW + cardGap), 180, cardW, cardH);
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
    
    private boolean collidesWithWall(double nx, double ny, double entityRadius) {
        // Shift the collision center down slightly so the player can visually walk "behind" roofs
        double depthOffsetY = 5.0; 
        double checkY = ny + depthOffsetY;
        double r = entityRadius * 0.7; // Diagonal corner offset

        // We check 9 points around the entity to make sure no part of the circle overlaps a solid pixel
        double[][] testPoints = {
            {nx, checkY},                               // Center
            {nx - entityRadius, checkY},                // Left
            {nx + entityRadius, checkY},                // Right
            {nx, checkY - entityRadius},                // Top
            {nx, checkY + entityRadius},                // Bottom
            {nx - r, checkY - r}, {nx + r, checkY - r}, // Top-Left, Top-Right
            {nx - r, checkY + r}, {nx + r, checkY + r}  // Bottom-Left, Bottom-Right
        };

        for (Wall w : walls) {
            double wallLeft = w.x - 75;
            double wallTop = w.y - 75;
            
            // Broad phase: Check if the entity is anywhere near this 150x150 wall
            if (nx + entityRadius < wallLeft || nx - entityRadius > wallLeft + 150 ||
                checkY + entityRadius < wallTop || checkY - entityRadius > wallTop + 150) {
                continue;
            }

            int type = Math.min(w.type, 8); 
            BufferedImage img = wallSprites[type];
            if (img == null) continue;

            // Narrow phase: Pixel-perfect collision check
            for (double[] pt : testPoints) {
                double px = pt[0];
                double py = pt[1];

                // If this point is inside the wall's screen bounds...
                if (px >= wallLeft && px < wallLeft + 150 && py >= wallTop && py < wallTop + 150) {
                    
                    // Convert screen coordinates back down to the 50x50 image pixel scale
                    int imgX = (int) ((px - wallLeft) / 3.0);
                    int imgY = (int) ((py - wallTop) / 3.0);

                    // Clamp limits to prevent ArrayOutOfBounds exceptions on the very edges
                    imgX = Math.max(0, Math.min(49, imgX));
                    imgY = Math.max(0, Math.min(49, imgY));

                    // Get the pixel color and extract the Alpha (transparency) channel
                    int pixel = img.getRGB(imgX, imgY);
                    int alpha = (pixel >> 24) & 0xff;

                    // If the pixel is mostly solid (alpha > 128), it's a collision!
                    if (alpha > 128) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        
        // Momentum Mass Logic
        if (p.hasMomentumMass) {
            if (moving) {
                p.momentumCharge = Math.min(100, p.momentumCharge + 0.2);
            } else {
                p.momentumCharge = 0;
            }
            p.radiusMult = 1.0 + (p.momentumCharge / 100.0) * 0.5; // Max 50% larger
        }

        // Rooted Stance Logic
        if (p.hasRootedStance) {
            if (!moving) p.stationaryTime++;
            else p.stationaryTime = 0;
        }

        // Static Charge Logic
        if (p.hasStaticCharge && moving) {
            p.staticCharge = Math.min(100, p.staticCharge + 0.5);
        }

        // Blazing Trail Logic
        if (p.hasBlazingTrail && moving && frameCount % 5 == 0) {
            hazards.add(new Hazard(p.x, p.y, 20, 180, 0, p.damage * 0.5));
        }

        // Glacial Trail Logic
        if (p.hasGlacialTrail && moving && frameCount % 6 == 0) {
            hazards.add(new Hazard(p.x, p.y, 25, 240, 3, 0));
        }

        // Ice Skating check
        boolean onIce = false;
        if (p.hasGlacialTrail) {
            for (Hazard h : hazards) {
                if (h.type == 3 && distSq(p.x, p.y, h.x, h.y) < 25*25) { onIce = true; break; }
            }
        }

        // Dash Handling
        if (p.dashTime > 0) {
            p.dashTime--;
            dx = p.dashDx;
            dy = p.dashDy;
        } else {
            if (p.dashCooldown > 0) p.dashCooldown--;
            
            if (moving) {
                double len = Math.hypot(dx, dy);
                double currentSpeed = p.moveSpeed * (onIce ? 3.0 : 1.0);
                if (p.hasFeralHeat) {
                    currentSpeed += (1.0 - (p.hp / p.maxHp)) * 2.0; // Up to +2.0 speed at 0 HP
                }
                dx = dx / len * currentSpeed;
                dy = dy / len * currentSpeed;
                
                // Trigger Dash
                if (spacePressed && p.dashCooldown <= 0) {
                    p.dashTime = p.hasFeatherweight ? 24 : 12;         
                    p.dashCooldown = 60;     
                    p.dashDx = dx * 3.5;     
                    p.dashDy = dy * 3.5;
                    dx = p.dashDx;
                    dy = p.dashDy;

                    if (p.hasMeteorStrike) {
                        meteors.add(new Meteor(p.x, p.y, 30, p.damage * 3));
                    }
                    if (p.hasAfterimage) {
                        decoys.add(new Decoy(p.x, p.y, 180, p.damage * 2));
                    }
                }
            }
        }

        // Move if not colliding (and not rooted)
        boolean isRooted = p.hasRootedStance && p.stationaryTime > 120;
        if ((dx != 0 || dy != 0) && !isRooted) {
            if (!collidesWithWall(p.x + dx, p.y, 12 * p.radiusMult)) p.x += dx;
            if (!collidesWithWall(p.x, p.y + dy, 12 * p.radiusMult)) p.y += dy;
        }
        
        if (mouseHeld) p.facingRight = mouseX >= HALF_W;
        else if (dx < 0) p.facingRight = false;
        else if (dx > 0) p.facingRight = true;
        updateCamera();

        // Aura and Area Effects
        if (p.hasVortexAura) {
            for (Gem g : gems) {
                if (distSq(g.x, g.y, p.x, p.y) < 250*250) g.pulled = true;
            }
            for (Enemy en : enemies) {
                double dist = distSq(en.x, en.y, p.x, p.y);
                if (dist < 250*250 && dist > 40*40) {
                    en.x += (p.x - en.x) * 0.01;
                    en.y += (p.y - en.y) * 0.01;
                } else if (dist <= 40*40) {
                    en.stunTimer = 60; 
                }
            }
        }

        if (p.hasSearingGlare) {
            double pAngle = Math.atan2(mouseY - HALF_H, mouseX - HALF_W);
            for (Enemy en : enemies) {
                double eAngle = Math.atan2(en.y - p.y, en.x - p.x);
                double diff = Math.abs(pAngle - eAngle);
                if (diff > Math.PI) diff = 2 * Math.PI - diff;
                if (diff < 0.4 && distSq(p.x, p.y, en.x, en.y) < 300*300) {
                    damageEnemy(en, p.damage * 0.1, false);
                }
            }
        }

        // Cloud Shield Cooldown
        if (p.cloudCooldown > 0) p.cloudCooldown--;

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
        else if (moving || p.dashTime > 0) newState = 1;
        else newState = 0;
        if (p.attackAnimTimer > 0) p.attackAnimTimer--;
        if (newState != p.animState) {
            p.animState = newState;
            p.animFrame = 0;
            p.animTimer = 0;
        }
        tickPlayerAnimation();

        updateProjectiles();
        updateTraps();
        updateHazards();
        updateMeteors();
        updateDecoys();
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

    private void updateHazards() {
        for (int i = hazards.size() - 1; i >= 0; i--) {
            Hazard h = hazards.get(i);
            h.life--;
            if (h.life <= 0) { hazards.remove(i); continue; }
            
            // Damage check for Fire (0) and Tremor (1)
            if ((h.type == 0 || h.type == 1) && frameCount % 15 == 0) {
                for (Enemy en : enemies) {
                    if (distSq(en.x, en.y, h.x, h.y) < h.radius*h.radius) damageEnemy(en, h.damage, false);
                }
            }
        }
    }

    private void updateMeteors() {
        for (int i = meteors.size() - 1; i >= 0; i--) {
            Meteor m = meteors.get(i);
            m.delay--;
            if (m.delay <= 0) {
                explode(m.x, m.y, 80, m.damage, null);
                meteors.remove(i);
            }
        }
    }

    private void updateDecoys() {
        for (int i = decoys.size() - 1; i >= 0; i--) {
            Decoy d = decoys.get(i);
            d.life--;
            if (d.life <= 0) {
                explode(d.x, d.y, 60, d.damage, null);
                decoys.remove(i);
            }
        }
    }
    
    private void updateTraps() {
        Player p = player;
        for (SpikeTrap t : traps) {
            if (t.state == 0) { 
                boolean triggered = false;
                if (!p.hasFeatherweight && distSq(p.x, p.y, t.x, t.y) < 35 * 35) triggered = true;
                if (!triggered) {
                    for (Enemy en : enemies) {
                        if (distSq(en.x, en.y, t.x, t.y) < 35 * 35) {
                            triggered = true; break;
                        }
                    }
                }
                if (triggered) {
                    t.state = 1; t.animTimer = 0; t.frame = 0; t.dealtDamage = false;
                }
            } else if (t.state == 1) { 
                t.animTimer++;
                if (t.animTimer > 5) { 
                    t.animTimer = 0;
                    t.frame++;
                    
                    if (t.frame == 3 && !t.dealtDamage) {
                        t.dealtDamage = true;
                        if (!p.hasFeatherweight && distSq(p.x, p.y, t.x, t.y) < 35 * 35) hurtPlayer(20);
                        for (Enemy en : enemies) {
                            if (distSq(en.x, en.y, t.x, t.y) < 35 * 35) {
                                damageEnemy(en, 25, false);
                            }
                        }
                    }
                    
                    if (spikeFrames != null && t.frame >= spikeFrames.length) {
                        t.state = 2; t.frame = 0; t.animTimer = 0;
                    } else if (spikeFrames == null && t.frame >= 6) {
                        t.state = 2; t.frame = 0; t.animTimer = 0;
                    }
                }
            } else if (t.state == 2) { 
                t.animTimer++;
                if (t.animTimer > 60) t.state = 0;
            }
        }
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
        double damageMult = (rageTimer > 0 ? 1.5 : 1.0);
        if (p.hasRootedStance && p.stationaryTime > 120) damageMult *= 2.0;

        double base = p.damage * damageMult;
        int n = p.projectileCount;
        
        // Feral Heat bonus projectiles at low HP
        if (p.hasFeralHeat && p.hp / p.maxHp <= 0.10) n += 2;

        boolean isChainLightning = false;
        if (p.hasStaticCharge && p.staticCharge >= 100) {
            isChainLightning = true;
            p.staticCharge = 0;
        }

        for (int i = 0; i < n; i++) {
            double a = angle + (i - (n - 1) / 2.0) * 0.14;
            boolean crit = random.nextDouble() < p.critChance;
            
            int bounces = 0;
            if (p.hasLuckyRicochet) {
                bounces = 1 + (int)((1.0 - (p.hp / p.maxHp)) * 4); // Up to 5 bounces
            }

            Projectile pr = new Projectile(p.x, p.y, a, crit ? base * 2 : base, p.projSpeed, p.attackRange,
                    p.pierce, crit, false, p.hero.color);
            pr.radius = p.hero.projRadius;
            pr.bounces = bounces;
            pr.isChainLightning = isChainLightning;
            projectiles.add(pr);
        }
        
        double cdMult = (rageTimer > 0 ? 0.6 : 1.0);
        if (p.hasFeralHeat) cdMult *= Math.max(0.3, (p.hp / p.maxHp)); // Faster at lower HP

        double cd = p.attackCooldown * cdMult;
        p.cooldownTimer = (int) Math.max(4, Math.round(cd));
        p.attackAnimTimer = 15;
    }

    private void updateProjectiles() {
        Player p = player;
        for (int i = 0, n = projectiles.size(); i < n; i++) {
            Projectile pr = projectiles.get(i);
            pr.update();
            if (!pr.active) continue;

            if (p.hasGaleForce && pr.hostile) {
                if (distSq(p.x, p.y, pr.x, pr.y) < 150*150) {
                    pr.vx *= 0.95; 
                    pr.vy *= 0.95;
                }
            }

            if (collidesWithWall(pr.x, pr.y, pr.radius)) {
                if (pr.bounces > 0) {
                    pr.bounces--;
                    pr.vx = -pr.vx; // Simplistic bounce
                    pr.vy = -pr.vy;
                } else {
                    pr.active = false;
                    continue;
                }
            }

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
                    
                    if (pr.isChainLightning) {
                        explode(en.x, en.y, 150, pr.damage, null); // Simulate chain hit
                        pr.active = false;
                        break;
                    }

                    damageEnemy(en, pr.damage, pr.crit);
                    if (p.blastRadius > 0) explode(pr.x, pr.y, p.blastRadius, pr.damage * 0.5, en);
                    
                    if (pr.crit && p.hasTremorStep) {
                        hazards.add(new Hazard(en.x, en.y, 30, 240, 1, p.damage * 0.3));
                    }

                    if (pr.pierce > 0) {
                        if (p.hasPiercingChill && pr.hitList != null && !pr.hitList.isEmpty()) {
                            en.freezeTimer = 180; // 3 seconds freeze
                        }
                        pr.pierce--;
                        pr.markHit(en);
                        if (pr.bounces > 0) {
                            // Find nearest enemy to bounce to
                            pr.bounces--;
                        }
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
            
            boolean locallyFrozen = frozen || en.freezeTimer > 0;
            if (en.freezeTimer > 0) en.freezeTimer--;
            
            if (en.stunTimer > 0) {
                en.stunTimer--;
                locallyFrozen = true;
            }

            // Hazard effects
            boolean onPuddle = false;
            for (Hazard h : hazards) {
                if (h.type == 2 && distSq(en.x, en.y, h.x, h.y) < h.radius*h.radius) onPuddle = true;
                if (h.type == 3 && distSq(en.x, en.y, h.x, h.y) < h.radius*h.radius) en.stunTimer = Math.max(en.stunTimer, 2); // Ice slips
            }

            if (!locallyFrozen) en.update(p, onPuddle, decoys);

            double d2 = distSq(en.x, en.y, p.x, p.y);
            if (d2 > 1200.0 * 1200.0) { 
                placeOnSpawnRing(en);
                continue;
            }
            if (!locallyFrozen) {
                // Check player collision
                if (p.hasMomentumMass && p.momentumCharge >= 90 && en.size <= 24) {
                    if (d2 < (en.size/2.0 + 15*p.radiusMult)*(en.size/2.0 + 15*p.radiusMult)) {
                        damageEnemy(en, 9999, false); // Crush
                    }
                } else {
                    double r = en.size / 2.0 + 10 * p.radiusMult;
                    if (d2 < r * r) hurtPlayer(en.damage);
                }
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
            
            if (c.opening) {
                c.animTimer++;
                if (c.animTimer > 8) { 
                    c.animTimer = 0;
                    c.frame++;
                    
                    if (c.frame == 2 && !c.itemGiven) {
                        openChest(c);
                        c.itemGiven = true;
                    }
                    
                    if (c.frame >= 4) {
                        chests.remove(i);
                    }
                }
            } else {
                double d2 = distSq(c.x, c.y, p.x, p.y);
                if (d2 < 30 * 30) {
                    c.opening = true; 
                } else if (d2 > 1600.0 * 1600.0) {
                    chests.remove(i);
                }
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
        
        if (player.hasCrashingTide) {
            rings.add(new Ring(en.x, en.y, 80, new Color(50, 100, 255)));
            for (Enemy other : enemies) {
                if (other == en) continue;
                if (distSq(en.x, en.y, other.x, other.y) < 80*80) {
                    double ang = Math.atan2(other.y - en.y, other.x - en.x);
                    other.x += Math.cos(ang) * 40;
                    other.y += Math.sin(ang) * 40;
                }
            }
        }

        if (en.type == EnemyType.SLIME) {
            Enemy m1 = new Enemy(en.x - 12, en.y, EnemyType.MINI_SLIME, wave);
            Enemy m2 = new Enemy(en.x + 12, en.y, EnemyType.MINI_SLIME, wave);
            if (collidesWithWall(m1.x, m1.y, m1.size/2.0)) m1.x = en.x;
            if (collidesWithWall(m2.x, m2.y, m2.size/2.0)) m2.x = en.x;
            pendingSpawns.add(m1);
            pendingSpawns.add(m2);
        }
        
        if (en.type.weight > 0) {
            if (++enemiesKilledThisWave >= enemiesTotalThisWave) {
                wave++;
                enemiesKilledThisWave = 0;
                enemiesTotalThisWave += 10;
            }
        }
    }

    private void hurtPlayer(double dmg) {
        Player p = player;
        if (currentState != GameState.PLAYING || p.invincibilityTimer > 0) return;
        
        if (p.hasCumulonimbus && p.cloudCooldown <= 0) {
            p.cloudCooldown = 900; // 15 seconds
            p.invincibilityTimer = 300; // 5 seconds reposition
            rings.add(new Ring(p.x, p.y, 200, Color.WHITE));
            showToast("CLOUD SHIELD PROCCED!");
            return;
        }

        double armorMult = 1 - p.armor;
        if (p.hasRootedStance && p.stationaryTime > 120) armorMult *= 0.2; // Massive armor
        
        double actual = dmg * armorMult;
        p.hp -= actual;
        p.invincibilityTimer = 60;
        floatTexts.add(new FloatText(p.x, p.y - 30, "-" + (int) Math.ceil(actual), Color.RED));
        
        if (p.hasCauterize) {
            explode(p.x, p.y, 100, p.damage * 2, null);
            // Simplification: just heal a flat amount instead of tracking exact kill
            heal(actual * 0.5, false); 
        }

        if (p.hasFluidForm) {
            hazards.add(new Hazard(p.x, p.y, 40, 240, 2, 0)); // Slowing puddle
            p.x -= Math.cos(Math.atan2(mouseY - HALF_H, mouseX - HALF_W)) * 80; // teleport back
            p.y -= Math.sin(Math.atan2(mouseY - HALF_H, mouseX - HALF_W)) * 80;
        }

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

        if (p.hasOvergrowth && gained >= 1) {
            p.radiusMult += 0.02;
            p.attackRange *= 1.02;
        }
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
        upPressed = downPressed = leftPressed = rightPressed = spacePressed = false;
        setState(GameState.LEVEL_UP);
    }

    private void generateUpgrades() {
        currentChoices.clear();
        List<Card> pool = new ArrayList<>();
        Player p = player;
        
        int typesInvested = 0;
        boolean isDualTypeLocked = false;
        
        for (MagicType type : MagicType.values()) {
            if (type != MagicType.NONE && p.magicCounts[type.ordinal()] > 0) {
                typesInvested++;
                if (p.magicCounts[type.ordinal()] >= 3) {
                    p.lockedMonoType = type; 
                }
            }
        }
        
        if (typesInvested >= 2) isDualTypeLocked = true; 

        for (Card c : CARDS) {
            if (p.cardLevels[c.ordinal()] >= c.maxLevel) continue;
            
            if (c.magicType != MagicType.NONE) {
                if (p.lockedMonoType != null && c.magicType != p.lockedMonoType) continue;
                if (isDualTypeLocked) {
                    if (p.magicCounts[c.magicType.ordinal()] == 0) continue; 
                    if (p.magicCounts[c.magicType.ordinal()] >= 2) continue; 
                }
            }
            pool.add(c);
        }

        for (int pick = 0; pick < 2 && !pool.isEmpty(); pick++) {
            int total = 0;
            int[] dynamicWeights = new int[pool.size()];
            for (int i = 0; i < pool.size(); i++) {
                Card c = pool.get(i);
                int weight = c.rarity.weight;
                
                if (c.magicType != MagicType.NONE && p.magicCounts[c.magicType.ordinal()] > 0) {
                    weight += (p.magicCounts[c.magicType.ordinal()] * 5);
                }
                
                dynamicWeights[i] = weight;
                total += weight;
            }

            int roll = random.nextInt(total);
            int acc = 0;
            for (int i = 0; i < pool.size(); i++) {
                acc += dynamicWeights[i];
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
        
        if (c.magicType != MagicType.NONE) {
            p.magicCounts[c.magicType.ordinal()]++;
        }
        
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
            
            // FIRE
            case BLAZING_TRAIL: p.hasBlazingTrail = true; break;
            case SEARING_GLARE: p.hasSearingGlare = true; break;
            case METEOR_STRIKE: p.hasMeteorStrike = true; break;
            case CAUTERIZE: p.hasCauterize = true; break;
            case FERAL_HEAT: p.hasFeralHeat = true; break;
            // EARTH
            case MOMENTUM_MASS: p.hasMomentumMass = true; break;
            case ROOTED_STANCE: p.hasRootedStance = true; break;
            case TREMOR_STEP: p.hasTremorStep = true; break;
            case OVERGROWTH: p.hasOvergrowth = true; break;
            case LUCKY_RICOCHET: p.hasLuckyRicochet = true; break;
            // AIR
            case CUMULONIMBUS: p.hasCumulonimbus = true; break;
            case AFTERIMAGE: p.hasAfterimage = true; break;
            case STATIC_CHARGE: p.hasStaticCharge = true; break;
            case GALE_FORCE: p.hasGaleForce = true; break;
            case FEATHERWEIGHT: p.hasFeatherweight = true; break;
            // WATER
            case CRASHING_TIDE: p.hasCrashingTide = true; break;
            case FLUID_FORM: p.hasFluidForm = true; break;
            case PIERCING_CHILL: p.hasPiercingChill = true; break;
            case GLACIAL_TRAIL: p.hasGlacialTrail = true; break;
            case VORTEX_AURA: p.hasVortexAura = true; break;
        }
        if (discovered.add(c)) saveJournal();
    }

    private void openChest(Chest c) {
        if (random.nextDouble() < 0.05) {
            Item it = rollItem();
            player.specialItem = it;
            player.specialItemCount++;
            if (!it.consumable) applyPassive(it); 
            showToast("FOUND BOOSTED " + it.label + "!");
        } else {
            Item it = rollItem();
            player.itemCounts[it.ordinal()]++;
            if (!it.consumable) applyPassive(it);
            showToast("FOUND " + it.label + "!");
        }
        
        rings.add(new Ring(c.x, c.y, 60, new Color(255, 215, 0)));
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            addGem(c.x + Math.cos(a) * 25, c.y + Math.sin(a) * 25, 3);
        }
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
        if (slot >= 0 && slot < 9) {
            useItem(ITEMS[slot], false);
        } else if (slot == 9 && player.specialItem != null) {
            useItem(player.specialItem, true);
        }
    }

    private void useItem(Item it, boolean isSpecial) {
        Player p = player;
        if (!it.consumable) return;
        if (!isSpecial && p.itemCounts[it.ordinal()] <= 0) return;
        if (isSpecial && p.specialItemCount <= 0) return;
        
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
        
        if (isSpecial) {
            p.specialItemCount--;
            if (p.specialItemCount <= 0) p.specialItem = null;
        } else {
            p.itemCounts[it.ordinal()]--;
        }
        showToast((isSpecial ? "USED BOOSTED " : "USED ") + it.label);
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
        boolean valid = false;
        while (!valid) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 520 + random.nextDouble() * 80; 
            en.x = player.x + Math.cos(a) * r;
            en.y = player.y + Math.sin(a) * r;
            
            if (!collidesWithWall(en.x, en.y, en.size / 2.0)) {
                valid = true;
            }
        }
    }

    private void spawnChest() {
        double cx = 0, cy = 0;
        boolean valid = false;
        while (!valid) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 350 + random.nextDouble() * 300;
            cx = player.x + Math.cos(a) * r;
            cy = player.y + Math.sin(a) * r;
            valid = true;
            
            if (collidesWithWall(cx, cy, 20)) {
                valid = false;
            }
        }
        int type = random.nextInt(2); 
        chests.add(new Chest(cx, cy, type));
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
        hazards.clear();
        decoys.clear();
        meteors.clear();
        
        frameCount = 0;
        spawnRate = 120;
        spawnTimer = 60;
        wave = 1;
        enemiesKilledThisWave = 0;
        enemiesTotalThisWave = 20;
        totalKills = 0;
        rageTimer = freezeTimer = 0;
        toastTimer = 0;
        upPressed = downPressed = leftPressed = rightPressed = spacePressed = false;
        mouseHeld = false;
        chestTimer = nextChestDelay();
        
        walls.clear();
        traps.clear();
        for (int i = 0; i < 350; i++) {
            double wx = -4000 + random.nextDouble() * 8000;
            double wy = -4000 + random.nextDouble() * 8000;
            if (Math.abs(wx) < 150 && Math.abs(wy) < 150) continue;
            
            boolean overlap = false;
            for (Wall w : walls) {
                if (Math.hypot(w.x - wx, w.y - wy) < 200) { overlap = true; break; }
            }
            if (overlap) continue;
            
            if (random.nextDouble() < 0.15) {
                traps.add(new SpikeTrap(wx, wy));
            } else {
                walls.add(new Wall(wx, wy, random.nextInt(8)));
            }
        }
        
        updateCamera();
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
    
    private int getTileIndex(int tx, int ty) {
        int hash = tx * 73856093 ^ ty * 19349663;
        hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
        hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
        hash = hash ^ (hash >>> 16);
        return Math.abs(hash) % 18;
    }

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
    
    private void drawVerySmallCustomText(Graphics2D g, String text, int x, int y) {
        if (verySmallTextSheet == null) return;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int idx = c < 128 ? GLYPH_INDEX[c] : -1;
            if (idx != -1) {
                int sx = (idx % 6) * VERY_SMALL_GLYPH_W;
                int sy = (idx / 6) * VERY_SMALL_GLYPH_H;
                g.drawImage(verySmallTextSheet, x, y, x + VERY_SMALL_GLYPH_W, y + VERY_SMALL_GLYPH_H,
                        sx, sy, sx + VERY_SMALL_GLYPH_W, sy + VERY_SMALL_GLYPH_H, null);
            }
            x += VERY_SMALL_GLYPH_W;
        }
    }

    private int verySmallTextWidth(String text) {
        return text.length() * VERY_SMALL_GLYPH_W;
    }
    
    private int drawWrappedVerySmallCentered(Graphics2D g, String text, int cx, int y, int maxW, int lineH) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (verySmallTextWidth(test) > maxW && line.length() > 0) {
                drawVerySmallCustomText(g, line.toString(), cx - verySmallTextWidth(line.toString()) / 2, y);
                y += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            drawVerySmallCustomText(g, line.toString(), cx - verySmallTextWidth(line.toString()) / 2, y);
            y += lineH;
        }
        return y;
    }

    private void drawCenteredString(Graphics2D g, String text, int y, Color c, int size) {
        g.setColor(c);
        g.setFont(getBoldFont(size));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (WIDTH - fm.stringWidth(text)) / 2, y);
    }

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
        String hint = "WASD move   |   Hold left click to shoot   |   1-0 use items   |   Space dash   |   Esc pause";
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
        
        if (titleImg != null) {
            int imgW = 800; 
            int imgH = 80;  
            g.drawImage(titleImg, HALF_W - imgW / 2, 75, imgW, imgH, null);
            drawCustomTextCentered(g, "SELECT YOUR CLASS", HALF_W, 100, 1);
        } else {
            drawLabelBoxCentered(g, "SELECT YOUR CLASS", HALF_W, 25, 1);
        }
        
        for (UIButton b : backButtons) b.draw(g);

        for (int i = 0; i < HEROES.length; i++) {
            HeroClass h = HEROES[i];
            Rectangle r = heroCards[i];
            boolean hover = r.contains(mouseX, mouseY);
            int cx = r.x + r.width / 2;

            if (cSelectImg != null) {
                g.drawImage(cSelectImg, r.x, r.y, r.width, r.height, null);
                if (hover) {
                    g.setColor(new Color(255, 255, 255, 40));
                    g.fillRect(r.x, r.y, r.width, r.height);
                    g.setColor(h.color);
                    g.setStroke(THICK);
                    g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
                    g.setStroke(THIN);
                }
            } else {
                g.setColor(hover ? PANEL_HOVER : PANEL_FILL);
                g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
                g.setColor(hover ? h.color : BORDER);
                g.setStroke(THICK);
                g.drawRoundRect(r.x, r.y, r.width, r.height, 16, 16);
                g.setStroke(THIN);
            }

            drawHeroIcon(g, h, cx - 56, r.y + 14, 112, hover ? uiTick / 6 : 0);
            drawCustomTextCentered(g, h.label, cx, r.y + 140, 1);

            String[] stats = {
                "HP: " + (int) h.maxHp,
                "DAMAGE: " + (int) h.damage,
                "RANGE: " + (int) h.range,
                "SPEED: " + String.format("%.1f", h.speed),
                "ATK SPD: " + String.format("%.1f", 60.0 / h.cooldown)
            };
            for (int s = 0; s < stats.length; s++) {
                drawSmallCustomText(g, stats[s], r.x + 100, r.y + 172 + s * 16);
            }

            drawWrappedVerySmallCentered(g, h.description, cx, r.y + 270, r.width - 100, 10);
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
                
                String tag = c.rarity.name() + (c.maxLevel >= 99 ? "" : "  MAX LV " + c.maxLevel);
                int tagWidth = verySmallTextWidth(tag);
                drawVerySmallCustomText(g, tag, x + panelW - 10 - tagWidth, y + 8);
                
                drawVerySmallCustomText(g, c.description, x + 10, y + panelH - 12);
            } else {
                drawSmallCustomText(g, "???", x + 10, y + 6);
                drawVerySmallCustomText(g, "PICK THIS CARD IN A RUN TO DISCOVER IT", x + 10, y + panelH - 12);
            }
        }
        
        String discText = "DISCOVERED " + discovered.size() + " OF " + CARDS.length; 
        
        if (titleImg != null) {
            int botImgW = 500; 
            int botImgH = 60;
            int botY = HEIGHT - 55;
            
            g.drawImage(titleImg, HALF_W - botImgW / 2, botY, botImgW, botImgH, null);
            
            int textW = smallTextWidth(discText);
            drawSmallCustomText(g, discText, HALF_W - textW / 2, botY + 22);
        } else {
            drawSmallLabelBox(g, discText, HALF_W - smallTextWidth(discText) / 2 - 8, HEIGHT - 50);
        }
    }

    private void drawWorld(Graphics2D g) {
        AffineTransform saved = g.getTransform();
        g.translate(-camX, -camY);

        int startTileX = Math.floorDiv(camX, TILE) - 1;
        int startTileY = Math.floorDiv(camY, TILE) - 1;
        int endTileX = Math.floorDiv(camX + WIDTH, TILE) + 1;
        int endTileY = Math.floorDiv(camY + HEIGHT, TILE) + 1;

        for (int tx = startTileX; tx <= endTileX; tx++) {
            for (int ty = startTileY; ty <= endTileY; ty++) {
                int tileIdx = getTileIndex(tx, ty);
                if (floorTiles[tileIdx] != null) {
                    g.drawImage(floorTiles[tileIdx], tx * TILE, ty * TILE, TILE, TILE, null);
                }
            }
        }
        
        // Draw Hazards
        for (Hazard h : hazards) {
            if (!onScreen(h.x, h.y, (int)h.radius)) continue;
            if (h.type == 0) { // Fire
                g.setColor(new Color(255, 100, 0, 150));
                g.fillOval((int)(h.x - h.radius), (int)(h.y - h.radius), (int)h.radius*2, (int)h.radius*2);
            } else if (h.type == 1) { // Tremor
                g.setColor(new Color(139, 69, 19, 180));
                g.drawOval((int)(h.x - h.radius), (int)(h.y - h.radius), (int)h.radius*2, (int)h.radius*2);
            } else if (h.type == 2) { // Puddle
                g.setColor(new Color(0, 50, 200, 120));
                g.fillOval((int)(h.x - h.radius), (int)(h.y - h.radius/2), (int)h.radius*2, (int)h.radius);
            } else if (h.type == 3) { // Ice
                g.setColor(new Color(150, 220, 255, 120));
                g.fillOval((int)(h.x - h.radius), (int)(h.y - h.radius), (int)h.radius*2, (int)h.radius*2);
            }
        }

        // Draw Decoys
        for (Decoy d : decoys) {
            if (!onScreen(d.x, d.y, 30)) continue;
            g.setColor(new Color(150, 150, 255, 120));
            g.fillOval((int)d.x - 20, (int)d.y - 20, 40, 40);
        }

        for (Wall w : walls) {
            if (!onScreen(w.x, w.y, 100)) continue;
            int type = Math.min(w.type, 8);
            if (wallSprites[type] != null) {
                g.drawImage(wallSprites[type], (int)w.x - 75, (int)w.y - 75, 150, 150, null);
            } else {
                g.setColor(Color.GRAY);
                g.fillRect((int)w.x - 75, (int)w.y - 75, 150, 150);
            }
        }
        
        for (SpikeTrap t : traps) {
            if (!onScreen(t.x, t.y, 50)) continue;
            if (spikeFrames != null && t.frame < spikeFrames.length && spikeFrames[t.frame] != null) {
                g.drawImage(spikeFrames[t.frame], (int)t.x - 25, (int)t.y - 25, 50, 50, null);
            } else {
                g.setColor(Color.DARK_GRAY);
                g.fillRect((int)t.x - 20, (int)t.y - 20, 40, 40);
                if (t.state == 1 && t.frame >= 2) {
                    g.setColor(Color.RED);
                    g.fillOval((int)t.x - 15, (int)t.y - 15, 30, 30);
                }
            }
        }

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
            if (pr.isChainLightning) g.setColor(Color.YELLOW);
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

        // Draw Meteors
        for (Meteor m : meteors) {
            if (onScreen(m.x, m.y, 50)) {
                g.setColor(new Color(255, 100, 0, 100));
                g.fillOval((int)m.x - 30, (int)m.y - 30, 60, 60);
                g.setColor(Color.ORANGE);
                g.fillOval((int)m.x - 10, (int)m.y - 10 - m.delay*2, 20, 20); // Falling effect
            }
        }

        g.setTransform(saved);
        drawChestArrows(g);
    }

    private void drawPlayer(Graphics2D g) {
        Player p = player;
        if (p.invincibilityTimer > 0 && (p.invincibilityTimer / 5) % 2 != 0) return; 

        int size = (int)(48 * p.radiusMult);
        int px = (int) Math.round(p.x) - size / 2;
        int py = (int) Math.round(p.y) - size / 2;
        
        g.setColor(SHADOW);
        g.fillOval(px + 8, py + size - 15, size - 16, 10); 

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
        
        if (p.hasRootedStance && p.stationaryTime > 120) {
            g.setColor(new Color(100, 255, 100, 80));
            g.fillOval(px - 10, py + size - 20, size + 20, 20);
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
        g.setColor(en.hitFlash > 0 ? Color.WHITE : (freezeTimer > 0 || en.freezeTimer > 0) ? FROZEN : en.type.color);
        g.fillOval(ex - half, ey - half, en.size, en.size);
        g.setColor(OUTLINE);
        g.drawOval(ex - half, ey - half, en.size, en.size);
    }

    private void drawChest(Graphics2D g, Chest c) {
        BufferedImage[] frames = (c.type == 0) ? chest1Frames : chest2Frames;
        if (frames != null && c.frame < frames.length && frames[c.frame] != null) {
            BufferedImage img = frames[c.frame];
            int w = img.getWidth() * 2; 
            int h = img.getHeight() * 2;
            g.drawImage(img, (int)c.x - w / 2, (int)c.y - h / 2 + 10, w, h, null);
        } else {
            int x = (int) c.x, y = (int) c.y;
            g.setColor(c.type == 0 ? new Color(120, 75, 35) : new Color(150, 90, 45));
            g.fillRoundRect(x - 14, y - 10, 28, 20, 6, 6);
        }
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
        g.fillRect(barX, uiY + 17, maxBarWidth, 8);
        g.setColor(Color.RED);
        g.fillRect(barX, uiY + 17, (int) (maxBarWidth * hpPercent), 8);

        g.setColor(new Color(0, 0, 100));
        g.fillRect(barX, uiY + 30, maxBarWidth, 8);
        g.setColor(new Color(50, 150, 255));
        g.fillRect(barX, uiY + 30, (int) (maxBarWidth * xpPercent), 8);

        g.setColor(new Color(0, 100, 0));
        g.fillRect(barX, uiY + 45, 100, 8);
        g.setColor(Color.GREEN);
        g.fillRect(barX, uiY + 45, (int) (100 * wavePercent), 8);

        int panelH = 70;
        if (uiPanelImg != null) {
            g.drawImage(uiPanelImg, uiX, uiY, null);
            panelH = Math.max(panelH, uiPanelImg.getHeight(null));
        }

        int ty = uiY + panelH + 6;
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

    private int getHotbarY() {
        if (actionPanelImg != null) {
            return HEIGHT - (int)(actionPanelImg.getHeight() * hotbarScale) - hotbarBottomMargin;
        }
        return HEIGHT - 56; 
    }

    private int getHotbarX() {
        if (actionPanelImg != null) {
            return (WIDTH - (int)(actionPanelImg.getWidth() * hotbarScale)) / 2 + hotbarXOffset;
        }
        return (WIDTH - (10 * SLOT + (10 - 1) * SLOT_GAP)) / 2; 
    }

    private int hotbarSlotAt(int mx, int my) {
        int hy = getHotbarY();
        int hx = getHotbarX();
        if (actionPanelImg != null) {
            int scaledW = (int)(actionPanelImg.getWidth() * hotbarScale);
            int scaledH = (int)(actionPanelImg.getHeight() * hotbarScale);
            if (my < hy || my > hy + scaledH || mx < hx || mx > hx + scaledW) return -1;
            for (int i = 0; i < 10; i++) {
                int slotLeft = hx + slotOffsets[i];
                if (mx >= slotLeft && mx <= slotLeft + customSlotWidth) return i;
            }
            return -1;
        } else {
            if (my < hy || my > hy + SLOT) return -1;
            int rel = mx - hx;
            if (rel < 0) return -1;
            int i = rel / (SLOT + SLOT_GAP);
            if (i >= 10 || rel % (SLOT + SLOT_GAP) > SLOT) return -1;
            return i;
        }
    }

    private void drawHotbar(Graphics2D g) {
        int hx = getHotbarX() - 10;
        int hy = getHotbarY();
        int hover = hotbarSlotAt(mouseX, mouseY);
        
        if (actionPanelImg != null) {
            int scaledW = (int)(actionPanelImg.getWidth() * hotbarScale);
            int scaledH = (int)(actionPanelImg.getHeight() * hotbarScale);
            g.drawImage(actionPanelImg, hx, hy, scaledW, scaledH, null);
            
            for (int i = 0; i < 10; i++) {
                int slotX = hx + slotOffsets[i];
                int slotCX = slotX + (customSlotWidth / 2);
                int slotCY = hy + scaledH / 2;
                
                if (i == hover) {
                    g.setColor(new Color(255, 200, 0, 80));
                    g.fillRect(slotX, hy + 40, customSlotWidth, scaledH - 80);
                }
                
                String slotNum = (i == 9) ? "0" : String.valueOf(i + 1);
                drawSmallCustomText(g, slotNum, slotX + 6, hy + 6);
                int iconSize = (int)(24 * (hotbarScale / 2.0)); 
                
                if (i < 9) {
                    int count = player.itemCounts[i];
                    if (count > 0) {
                        if (itemIcons[i] != null) g.drawImage(itemIcons[i], slotCX - iconSize/2, slotCY - iconSize/2 + 5, iconSize, iconSize, null);
                        else { g.setColor(ITEMS[i].color); g.fillRoundRect(slotCX - iconSize/2 + 5, slotCY - iconSize/2 + 5, iconSize, iconSize, 8, 8); }
                        drawSmallCustomText(g, String.valueOf(count), slotX + customSlotWidth - smallTextWidth(String.valueOf(count)) - 6, hy + scaledH - 14);
                    }
                } else {
                    if (player.specialItemCount > 0 && player.specialItem != null) {
                        int sIdx = player.specialItem.ordinal();
                        if (itemIcons[sIdx] != null) g.drawImage(itemIcons[sIdx], slotCX - iconSize/2 + 5, slotCY - iconSize/2 + 5, iconSize, iconSize, null);
                        else { g.setColor(player.specialItem.color); g.fillRoundRect(slotCX - iconSize/2 + 5, slotCY - iconSize/2 + 5, iconSize, iconSize, 8, 8); }
                        drawSmallCustomText(g, String.valueOf(player.specialItemCount), slotX + customSlotWidth - smallTextWidth(String.valueOf(player.specialItemCount)) - 6, hy + scaledH - 14);
                    }
                }
            }
        } else {
            // standard drawing logic...
        }

        if (hover >= 0) {
            Item it = null;
            if (hover < 9 && player.itemCounts[hover] > 0) it = ITEMS[hover];
            else if (hover == 9 && player.specialItemCount > 0) it = player.specialItem;
            
            if (it != null) {
                String tip = (hover == 9 ? "BOOSTED " : "") + it.label + ": " + it.description;
                g.setFont(FONT_BODY);
                FontMetrics fm = g.getFontMetrics();
                int w = fm.stringWidth(tip) + 16;
                int x = Math.max(5, Math.min(WIDTH - w - 5, mouseX - w / 2));
                g.setColor(PANEL_FILL);
                g.fillRoundRect(x, hy - 30, w, 22, 8, 8);
                g.setColor(BORDER);
                g.drawRoundRect(x, hy - 30, w, 22, 8, 8);
                g.setColor(TEXT_DARK);
                g.drawString(tip, x + 8, hy - 15);
            }
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

        int bookX = HALF_W - 325 ;
        int bookY = HALF_H - 340;
        BufferedImage frame = bookFrames != null ? bookFrames[currentBookFrame] : null;
        if (frame != null) {
            g.drawImage(frame, bookX, bookY, 600, 500, null);
        } else {
            g.setColor(Color.GRAY);
            g.fillRect(bookX, bookY, 400, 300);
        }
        if (!bookOpen) return;

        int cardW = 240;
        int cardH = 300;
        int cardY = bookY + 160;

        for (int i = 0; i < currentChoices.size(); i++) {
            Card c = currentChoices.get(i);
            int cx = bookX + (i == 0 ? 170 : 430); 
            int cardX = cx - cardW / 2;
            
            boolean hover = mouseX >= cardX && mouseX < cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

            if (cSelectImg != null) {
                g.drawImage(cSelectImg, cardX, cardY, cardW, cardH, null);
                if (hover) {
                    g.setColor(new Color(255, 255, 255, 40));
                    g.fillRect(cardX, cardY, cardW, cardH);
                    g.setColor(c.rarity.color);
                    g.setStroke(THICK);
                    g.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
                    g.setStroke(THIN);
                }
            } else {
                g.setColor(hover ? PANEL_HOVER : PANEL_FILL);
                g.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);
                g.setColor(hover ? c.rarity.color : BORDER);
                g.setStroke(THICK);
                g.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
                g.setStroke(THIN);
            }

            if (c.magicType != MagicType.NONE && magicLogos != null && magicNames != null && magicIcons != null) {
                // Determine indices based on MagicType
                int logoIdx = 0;
                int nameIdx = 0;
                if (c.magicType == MagicType.FIRE) { logoIdx = 0; nameIdx = 0; }
                if (c.magicType == MagicType.WATER) { logoIdx = 1; nameIdx = 2; }
                if (c.magicType == MagicType.EARTH) { logoIdx = 2; nameIdx = 3; }
                if (c.magicType == MagicType.AIR) { logoIdx = 3; nameIdx = 1; }

                // 1. Draw Logo (Top)
                BufferedImage centerLogo = magicLogos[logoIdx];
                if (centerLogo != null) {
                    // Scales 125x75 to 250x150, fitting cleanly inside the 260px card width
                    int lw = centerLogo.getWidth() * 2;
                    int lh = centerLogo.getHeight() * 2;
                    g.drawImage(centerLogo, cx - lw/2, cardY - 30, lw, lh, null);
                }

                // 2. Draw Skill Icon (Below logo)
                BufferedImage iconImg = magicIcons[c.iconRow][c.iconCol];
                if (iconImg != null) {
                    int iw = iconImg.getWidth() * 2;
                    int ih = iconImg.getHeight() * 2;
                    g.drawImage(iconImg, cx - iw/2, cardY + 100, iw, ih, null); 
                }
                
                // 3. Draw Description (Below icon)
                int descY = cardY + 175; 
                drawWrappedVerySmallCentered(g, c.description, cx, descY, cardW - 20, 10);

                // 4. Draw Magic Name Text (Bottom)
                BufferedImage nameImg = magicNames[nameIdx];
                if (nameImg != null) {
                    int nw = nameImg.getWidth() * 2;
                    int nh = nameImg.getHeight() * 2;
                    g.drawImage(nameImg, cx - nw/2, cardY + 225, nw, nh, null);
                }

            } else {
                int nameY = cardY + 30;
                int nameW = smallTextWidth(c.label);
                drawSmallCustomText(g, c.label, cx - nameW / 2, nameY);

                int rarityY = cardY + 55;
                String rar = c.rarity.name();
                int rarW = verySmallTextWidth(rar);
                drawVerySmallCustomText(g, rar, cx - rarW / 2, rarityY);

                int descY = cardY + 90;
                drawWrappedVerySmallCentered(g, c.description, cx, descY, cardW - 30, 10);
            }

            int levelY = cardY + 260;
            int next = player.cardLevels[c.ordinal()] + 1;
            String lvl = next == 1 ? "NEW!" : "LEVEL " + next;
            int lvlW = smallTextWidth(lvl);
            drawSmallCustomText(g, lvl, cx - lvlW / 2, levelY);
        }

        g.setFont(FONT_BODY);
        g.setColor(Color.LIGHT_GRAY);
        String hint = "Click a card or press 1 / 2";
        g.drawString(hint, HALF_W - g.getFontMetrics().stringWidth(hint) / 2, bookY + 550);
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
                    int cardW = 160, cardH = 240, cardY = bookY + 30;
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
            case KeyEvent.VK_SPACE: spacePressed = true; break;
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
                } else if (key == KeyEvent.VK_0) {
                    if (currentState == GameState.PLAYING) useItem(9); 
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
            case KeyEvent.VK_SPACE: spacePressed = false; break;
            case KeyEvent.VK_ESCAPE: case KeyEvent.VK_P: escHeld = false; break;
        }
    }

    @Override public void keyTyped(KeyEvent e) {}

    class Wall {
        final double x, y;
        final int type;
        Wall(double x, double y, int type) { this.x = x; this.y = y; this.type = type; }
    }

    class SpikeTrap {
        double x, y;
        int state = 0; 
        int animTimer = 0;
        int frame = 0;
        boolean dealtDamage = false;
        SpikeTrap(double x, double y) { this.x = x; this.y = y; }
    }

    class Hazard {
        double x, y, radius, damage;
        int life, type; // 0=Fire, 1=Tremor, 2=Puddle, 3=Ice
        Hazard(double x, double y, double radius, int life, int type, double damage) {
            this.x = x; this.y = y; this.radius = radius; this.life = life; this.type = type; this.damage = damage;
        }
    }
    
    class Decoy {
        double x, y, damage;
        int life;
        Decoy(double x, double y, int life, double damage) { this.x = x; this.y = y; this.life = life; this.damage = damage; }
    }

    class Meteor {
        double x, y, damage;
        int delay;
        Meteor(double x, double y, int delay, double damage) { this.x = x; this.y = y; this.delay = delay; this.damage = damage; }
    }

    class Player {
        final HeroClass hero;
        double x = 0, y = 0;
        double hp, maxHp, damage, attackRange, attackCooldown, moveSpeed, projSpeed;
        double xp = 0, xpToNextLevel = 10, xpMultiplier = 1.0;
        double critChance, armor, regen, leech, blastRadius, chestSpeed;
        double pickupRadius = 70;
        int projectileCount = 1, pierce = 0;
        int level = 1, cooldownTimer = 0, invincibilityTimer = 0;
        
        int dashTime = 0, dashCooldown = 0;
        double dashDx = 0, dashDy = 0;
        
        final int[] cardLevels = new int[CARDS.length];
        final int[] itemCounts = new int[ITEMS.length];
        final int[] magicCounts = new int[MagicType.values().length]; 
        MagicType lockedMonoType = null;

        // Buffs and States for Magic Cards
        boolean hasBlazingTrail, hasSearingGlare, hasMeteorStrike, hasCauterize, hasFeralHeat;
        boolean hasMomentumMass, hasRootedStance, hasTremorStep, hasOvergrowth, hasLuckyRicochet;
        boolean hasCumulonimbus, hasAfterimage, hasStaticCharge, hasGaleForce, hasFeatherweight;
        boolean hasCrashingTide, hasFluidForm, hasPiercingChill, hasGlacialTrail, hasVortexAura;
        
        double radiusMult = 1.0;
        double momentumCharge = 0;
        int stationaryTime = 0;
        double staticCharge = 0;
        int cloudCooldown = 0;

        Item specialItem = null;
        int specialItemCount = 0;

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
        int freezeTimer = 0, stunTimer = 0;

        Enemy(double x, double y, EnemyType type, int wave) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.size = type.size;
            this.speed = type.speed;
            this.hp = type.hp * (1 + 0.15 * (wave - 1));          
            this.damage = type.damage * (1 + 0.08 * (wave - 1));
        }

        void update(Player p, boolean onPuddle, List<Decoy> decoys) {
            tick++;
            
            // Decoy target prioritization
            double targetX = p.x;
            double targetY = p.y;
            if (!decoys.isEmpty()) {
                double closest = Double.MAX_VALUE;
                for (Decoy d : decoys) {
                    double dist = distSq(x, y, d.x, d.y);
                    if (dist < closest) { closest = dist; targetX = d.x; targetY = d.y; }
                }
            }

            double dx = targetX - x, dy = targetY - y;
            double dist = Math.hypot(dx, dy);
            if (dist < 0.001) return;
            double ux = dx / dist, uy = dy / dist;

            double moveX = 0, moveY = 0;
            double currentSpeed = speed * (onPuddle ? 0.3 : 1.0); // Slowed on puddles
            
            switch (type.behavior) {
                case WEAVE: {
                    double wob = Math.sin(tick * 0.15) * currentSpeed * 0.9;
                    moveX = ux * currentSpeed - uy * wob;
                    moveY = uy * currentSpeed + ux * wob;
                    break;
                }
                case RANGED:
                    if (dist > 280) { moveX = ux * currentSpeed; moveY = uy * currentSpeed; }
                    else if (dist < 190) { moveX = -ux * currentSpeed; moveY = -uy * currentSpeed; }
                    else { moveX = -uy * currentSpeed * 0.6; moveY = ux * currentSpeed * 0.6; }
                    if (--shootTimer <= 0 && dist < 480) {
                        shootTimer = 120;
                        projectiles.add(new Projectile(x, y, Math.atan2(dy, dx), damage, 4.5, 600, 0, false, true, ENEMY_SHOT));
                    }
                    break;
                default:
                    moveX = ux * currentSpeed;
                    moveY = uy * currentSpeed;
            }
            
            if (!collidesWithWall(x + moveX, y, size / 2.0)) {
                x += moveX;
            }
            if (!collidesWithWall(x, y + moveY, size / 2.0)) {
                y += moveY;
            }
        }
    }

    class Projectile {
        double x, y, px, py, vx, vy, damage, speed, remaining;
        int pierce, radius = 4, bounces = 0;
        boolean crit, hostile, active = true;
        boolean isChainLightning = false;
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
        final int type;
        boolean opening = false;
        boolean itemGiven = false;
        int frame = 0;
        int animTimer = 0;
        Chest(double x, double y, int type) { this.x = x; this.y = y; this.type = type; }
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
                    drawCustomTextCentered(g, label, cx, bounds.y + 13 + (int) (bounds.height * frac) - GLYPH_H, 1);
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