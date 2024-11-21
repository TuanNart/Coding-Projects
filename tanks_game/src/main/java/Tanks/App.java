package Tanks;

import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.event.KeyEvent;

import java.util.*;

public class App extends PApplet {

    public static final int CELLSIZE = 32; 
    public static final int CELLHEIGHT = 32;

    public static final int CELLAVG = 32;
    public static final int TOPBAR = 0;
    public static int WIDTH = 864; //CELLSIZE*BOARD_WIDTH;
    public static int HEIGHT = 640; //BOARD_HEIGHT*CELLSIZE+TOPBAR;
    public static final int BOARD_WIDTH = WIDTH/CELLSIZE;
    public static final int BOARD_HEIGHT = 20;

    public static final int INITIAL_PARACHUTES = 1;

    public static final int FPS = 30;

    public String configPath;

    public static Random random = new Random();
	// For loading terrains
    public static int numCols = 28; 
    public int randomNumber;
    // For loading and storing levels
    JSONArray levels;
    ArrayList<Level> all_levels;
    // Index and elements of current level 
    public int level_index = 0;
    Level cur_lev;
    private PImage bg;
    private Integer[] rgb;
    private PImage tree; 
    // For features that require holding a key
    HashSet<Integer> heldKeys = new HashSet<Integer>();
    // Active tanks & scores
    JSONObject players;
    ArrayList<Tank> removable_tanks; 
    ArrayList<Tank> scores;
    ArrayList<Tank> ordered;
    // Current player
    Tank activeTank;
    float startArrow;
    // All active projectiles displayed
    ArrayList<Projectile> activeProj = new ArrayList<Projectile>();
    // HUD/GUI
    int wind;
    PImage fuel;
    PImage bigPara;
    PImage para;
    PImage leftwind;
    PImage rightwind; 
    String playerList;
    String scoreList;
    boolean endGame;
    float displayScoreboard;
    // Draw loop
    long previousMilli = millis();
    // for custom rainbow color at the end of the game!!!1!!1
    int hueValue = 0; 

    public App() {
        this.configPath = "config.json";
    }
    /**
     * Initialise the setting of the window size.
     */
	@Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    /**
     * Load all resources such as images. Initialise the elements such as the player and map elements.
     */
    
    public PImage imageLoader(String obj) {
        return loadImage(this.getClass().getResource(obj).getPath().toLowerCase(Locale.ROOT).replace("%20", " "));
    }

    /**
     * Loading up all elements of the current level
    */

    public void setLevel() {
        // Transfer score of current tanks to a separate space
        if (level_index > 0) {
            for (Tank tank: scores) {
                int index = scores.indexOf(tank);
                tank.score = cur_lev.tanks.get(index).score;
            }
            // Reset players (tanks) and projectiles for next level.
            cur_lev.resetLevel();
        }
        cur_lev = all_levels.get(level_index);
        bg = cur_lev.getBackground(this);
        rgb = cur_lev.getForeground();
        tree = cur_lev.getTree(this);
        cur_lev.loadTerrain();
        cur_lev.makePlayerTurns();
        // Determine player who goes first aplhabetically
        activeTank = cur_lev.nextPlayer(this); 
        startArrow = cur_lev.startArrow;
        // Queue with the order of player turns
        removable_tanks = new ArrayList<>(cur_lev.tanks);
        activeProj.clear();
        // Tree and wind randomizer
        wind = random.nextInt(71) - 35;
        randomNumber = random.nextInt(31) - 15; 
        // Initializing scores for the first level ONLY
        if (level_index == 0) {
            scores = new ArrayList<Tank>(cur_lev.tanks.size());
            for (Tank tank: cur_lev.tanks) {
                scores.add(tank);
            }
        }
        // Maintaining the scores from previous level and adding to a new set of tanks.
        cur_lev.addScores(this);  
    }
    
	@Override
    public void setup() {
        frameRate(FPS);
        randomNumber = random.nextInt(31) - 15;
        //
        // Load all levels and player colours from the config file.
        //
        JSONObject config = loadJSONObject("config.json");
        levels = config.getJSONArray("levels");
        all_levels = new ArrayList<Level>();
        for (int i = 0; i < levels.size(); i++) {
            try {
                all_levels.add(new Level(this, levels.getJSONObject(i)));
            } catch (Exception e) {}
        }
        players = config.getJSONObject("player_colours");
        // Load all images from resources.
        fuel = imageLoader("fuel.png");
        fuel.resize(24, 24);
        bigPara = imageLoader("parachute.png");
        para = imageLoader("parachute.png");
        para.resize(24, 24);
        leftwind = imageLoader("leftwind.png");
        leftwind.resize(48, 48);
        rightwind = imageLoader("rightwind.png");    
        rightwind.resize(48, 48);   
        // Set up first level.
        setLevel();
    }
    /**
     * Receive key pressed signal from the keyboard. <for held keys as well>
     */
	@Override
    public void keyPressed(KeyEvent event){
        if (key == CODED) {
            heldKeys.add(keyCode);
        } else {
            heldKeys.add(event.getKeyCode());
            //
            // When user presses the spacebar.
            //
            if (key == ' ' && !endGame) { 
                if (removable_tanks.size() <= 1) {
                    if (level_index < all_levels.size() - 1) {
                        // Switch levels
                        level_index += 1;
                        ordered.clear(); 
                        setLevel();
                    } else {
                        // End the game and display final scoreboard
                        endGame = true;
                        displayScoreboard = millis();    
                    }
                } else {
                    if (!activeTank.inAir) {
                        if (activeTank.isAlive) {
                            activeProj.add(activeTank.shoot());
                            if (activeTank.bigBalls = true) {
                                activeTank.bigBalls = false;
                            }
                        }
                        // Change turns after one shot.
                        activeTank = cur_lev.nextPlayer(this);
                        startArrow = cur_lev.startArrow;
                        wind += random.nextInt(10) - 5;
                    }
                    else if (!activeTank.isAlive && !endGame)  {
                        // Skip turn of dead tanks.
                        activeTank = cur_lev.nextPlayer(this);
                        startArrow = cur_lev.startArrow;
                        wind += random.nextInt(10) - 5;
                    }
                }
            }
            // Resets the game
            if (key == 'r' && endGame) {
                endGame = false;
                level_index = 0;
                ordered.clear();
                cur_lev.resetLevel(); // reset last level for new game
                setLevel();
            }
        /* *
         * Powerups bought in-turn.
        */
                // repair kit
            if (key == 'r' && !endGame) {
                if (activeTank.getScore() >= 20 && activeTank.hp < 100) {
                    activeTank.score -= 20;
                    activeTank.hp = constrain(activeTank.hp + 20, 0, 100);
                }
            }   // buy fuel
            if (key == 'f' && !endGame) {
                if (activeTank.getScore() >= 10) {
                    activeTank.score -= 10;
                    activeTank.fuel += 200;
                } 
            }   // buy parachute
            if (key == 'p' && !endGame) {
                if (activeTank.getScore() >= 15) {
                    activeTank.score -= 15;
                    activeTank.parachute += 1;
                } 
            }   // 2x bigger projectile 
            if (key == 'x' && !endGame) {
                if (activeTank.getScore() >= 20 && !activeTank.bigBalls) {
                    activeTank.score -= 20;
                    activeTank.bigBalls = true;
                } 
            }
        }
    }
        /**
         * Receive key released signal from the keyboard. <for held keys as well>
         */

	@Override
    public void keyReleased(KeyEvent event){
        if (key == CODED) {
            heldKeys.remove(keyCode);
        } else {
            heldKeys.remove(event.getKeyCode());
        }
    }
    /**
     * Draw all elements in the game by current frame.
     */
	@Override
    public void draw() { 
        long timeMilli = millis();
        float deltaMilli = timeMilli - previousMilli;
        float deltaSec = deltaMilli/1000;
        previousMilli = timeMilli;
         // -------- \\
        // Render map \\
       ///------------\\\
        background(bg);
        // draw terrain
        for (int i = 0; i < 896; i++) {
            stroke(rgb[0], rgb[1], rgb[2]);
            rect(i,cur_lev.height_1d[i], 1, 640);
        }
        // draw trees
        for (int i = 0; i < 896; i++) { 
            if (cur_lev.tree_coords[i] > 0 && cur_lev.tree_coords[i] < 640) {
                int modified = i + randomNumber;
                if (modified < 0) {
                    modified = 0;
                }
                if (modified > 895) {
                    modified = 895;
                }
                image(tree, modified - 16, cur_lev.height_1d[modified] - 31);
            }
        }
        /* *  Animation for tanks and projectiles. * */
        for (Tank tank : cur_lev.tanks) {
            tank.update(this);
            tank.draw(this);  
            if (!tank.isAlive) {
                removable_tanks.remove(tank);
            }
        }
        for (Projectile proj: activeProj) {
            proj.update(this, deltaSec);
            proj.draw(this);
        }
        // Handling player turns
        if (!activeTank.isAlive) {
            activeTank = cur_lev.nextPlayer(this);
        }
        // Handling held keys, controlling active tank
        if (activeTank.isAlive) {
            if (heldKeys.contains(UP)) {
                activeTank.aim(this, -0.0054f*deltaMilli);
            }
            if (heldKeys.contains(DOWN)) {
                activeTank.aim(this, 0.0054f*deltaMilli); 
            }
            if (heldKeys.contains(LEFT)) {
                if ((activeTank.getX() > 0) && (activeTank.fuel > 0) && !activeTank.inAir) {
                    activeTank.fuel -= 1;
                    int newX = activeTank.getX() - 1;
                    activeTank.setX(newX);  
                    activeTank.setY(cur_lev.height_1d[newX]);   
                }
            }
            if (heldKeys.contains(RIGHT)) {
                if (activeTank.getX() < 862 && (activeTank.fuel > 0) && !activeTank.inAir) {
                    activeTank.fuel -= 1;
                    int newX = activeTank.getX() + 1;
                    activeTank.setX(newX);  
                    activeTank.setY(cur_lev.height_1d[newX]);   
                }
            }
            // 'w' key
            if (heldKeys.contains(87)) { 
                activeTank.addPow(this);
            } 
            // 's' key
            if (heldKeys.contains(83)) {
                activeTank.reducePow(this);
            }
        }
        //|---------------------------------|\\
        //|        Display HUD/GUI          |\\
        //|---------------------------------|\\

        // Current player indicators.
        float endArrow = (millis() - startArrow)/1000;
        if (endArrow < 2) {
            stroke(0);
            strokeWeight(3);
            line(activeTank.getX(), activeTank.getY() - 64, activeTank.getX(), activeTank.getY() - 128);
            line(activeTank.getX(), activeTank.getY() - 64, activeTank.getX() - 8, activeTank.getY() - 84);
            line(activeTank.getX(), activeTank.getY() - 64, activeTank.getX() + 8, activeTank.getY() - 84);
        }
        textSize(16);
        fill(0);
        text("Player " + activeTank.name + "'s turn", 88, 32);
        // Display fuel, parachute count.
        image(fuel, 190, 4);
        text(activeTank.fuel, 232, 24);
        image(para, 190, 32);
        text(activeTank.parachute, 232, 52);
        // Display healthbar & power bar.
        text("Health:", 376, 24);
        text("Power:  " + activeTank.power, 390, 52);
        text(activeTank.hp, 612, 24);
        //||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\\
        stroke(0);
        strokeWeight(3);
        fill(0);
        rect(420, 8, 160, 20);
        fill(activeTank.color[0], activeTank.color[1], activeTank.color[2]);
        rect(420, 8, 160 * activeTank.hp/100f, 20);
        //||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\\
        stroke(128);
        strokeWeight(5);
        rect(420, 8, 160 * activeTank.power/100f, 20);
        //||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||\\
        strokeWeight(1);
        stroke(255,0,0);
        line(420 + 160 * activeTank.power/100f, 4, 420 + 160 * activeTank.power/100f, 32);
        // Display wind direction & speed
        fill(0);
        if (wind < 0) {
            image(leftwind, 760, 2);
            text(abs(wind), 830, 30);
        } else {
            image(rightwind, 760, 2);
            text(abs(wind), 830, 30);
        }
        /* 
         * Order the list of players by score for the final scoreboard.
         */
        ordered = new ArrayList<Tank>(cur_lev.tanks);
        Collections.sort(ordered, Comparator.comparing(Tank::getScore).reversed());
        // Display scoreboard in-game.
        if (!endGame) {
            fill(0);
            textAlign(LEFT);
            text("Scores", 720, 66);
            for (int i = 0; i < cur_lev.tanks.size(); i++) {
                Tank tank = cur_lev.tanks.get(i);
                fill(tank.color[0], tank.color[1], tank.color[2]);
                textAlign(LEFT);
                text("Player " + tank.name, 720, 90 + i * 20);
                fill(0);
                textAlign(CENTER);
                text(tank.score, 828, 90 + i * 20);
            }
            stroke(0);
            noFill();
            strokeWeight(5);
            rect(712, 50, 140, 20);
            rect(712, 70, 140, 20 * (cur_lev.tanks.size()) + 8);
        }
        // Display scoreboard when game ends.
        else {
            stroke(0);
            int[] winner_col = ordered.get(0).color;
            strokeWeight(5);
            fill(winner_col[0] * 0.7f, winner_col[1]* 0.7f, winner_col[2]* 0.7f, 150);
            rect(300, 140, 274, 30);
            rect(300, 170, 274, 30 * (cur_lev.tanks.size()) + 16);

            textSize(20);
            fill(0);
            textAlign(LEFT);
            text("Final scores", 320, 164);

            for (int i = 0; i < ordered.size(); i++) {
                if (millis() - displayScoreboard > 700 * i) {
                    Tank tank = ordered.get(i);
                    textAlign(LEFT);
                    if (i == 0) {
                        textSize(25);
                        hueValue = (hueValue + 10) % 360;
                        fill(hueValue, 255, 128);
                        text("Player " + tank.name + " wins!", 348, 108);
                    }
                    fill(tank.color[0], tank.color[1], tank.color[2]);
                    textSize(20);
                    text("Player " + tank.name, 320, 200 + i * 30);
                    fill(0);
                    textAlign(CENTER);
                    text(tank.score, 544, 200 + i * 30);
                }
            }
        } 
    }
    public static void main(String[] args) {
        PApplet.main("Tanks.App");
    }

}
