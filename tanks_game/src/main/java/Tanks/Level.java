package Tanks;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


import processing.core.PImage;
import processing.data.JSONObject;

public class Level {
    // Load elements from config file
    JSONObject lev;
    private String layout_file;
    private String bg;
    private String[] fg;
    private Integer[] rgb;
    private String t;
    private PImage tree;
    //* Reading from textfile *\\
    char[][] terrain;
    int[] height_1d = new int[896];
    int[] tree_coords = new int[896];
    JSONObject players;
    ArrayList<Tank> tanks; //order of config
    LinkedList<Tank> turns; // queue of players in order of turn
    // for indicator arrow
    float startArrow;
    // to initialise a Tank object
    int x;
    int y;

    public Level(App app, JSONObject lev) {
        this.lev = lev;
        this.layout_file = lev.getString("layout");
        this.players = app.loadJSONObject("config.json").getJSONObject("player_colours");
        this.tanks = new ArrayList<>();
    }

    public void resetLevel() {
        this.tanks.clear();
    }

    public PImage getBackground(App app) {
        bg = lev.getString("background");
        return app.imageLoader(bg);
    }
    public Integer[] getForeground() {
        fg = lev.getString("foreground-colour").split(",\\s*");
        rgb = new Integer[3];
        for (int i = 0; i < fg.length; i++) {
            rgb[i] = Integer.parseInt(fg[i]);
        }
        return rgb;
    }
    public PImage getTree(App app) {
        try {
            t = lev.getString("trees");
            tree = app.imageLoader(t);
            tree.resize(App.CELLSIZE, App.CELLSIZE);  
        } catch (Exception e) {
        }
        return tree;
    }


    public void loadTerrain() {

        terrain = new char[App.BOARD_HEIGHT][App.numCols];

        File file = new File(this.layout_file);
    
        try (Scanner scan = new Scanner(file)) {
            int j = 0;
            while (scan.hasNextLine() && (j < App.BOARD_HEIGHT)) {
                String line = scan.nextLine();
                char[] letters = line.toCharArray();
                for (int i = 0; i < Math.min(App.numCols, letters.length); i++) {
                    terrain[j][i] = letters[i];
                }
                j++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace(); 
        }
        // Read players from colours config and add them if in textfile
        for (Object key: players.keys()) {
            String letter = (String) key;
            for (int row = 0; row < App.BOARD_HEIGHT; row++) { // 0 -> 19
                for (int col = 0; col < App.numCols; col++) { // 0 -> 27
                    char tile = terrain[row][col];
                    if (tile == letter.charAt(0)) {
                        tanks.add(new Tank(letter, col * 32));
                    }
                }
            }
        }
        for (int row = 0; row < App.BOARD_HEIGHT; row++) { // 0 -> 19
            for (int col = 0; col < App.numCols; col++) { // 0 -> 27
                char tile = terrain[row][col];
                //terrain
                if (tile == 'X') {
                    for (int i = 0; i < 32;i++) {
                        height_1d[32 * col + i] = row * 32;
                    }
                }
                // trees
                if (tile == 'T') {
                    tree_coords[col*32+1] = row * 32;
                }
            }
        }
        /* 
        * calculate the moving average:
        * make 1d array of size 32*28 = 896 | only 32 * 27 = 864 is displayed
        * terrain[x] -> pixel 32*x -> 32*x + 31
        * height = row, 
        * i <- (i + (i+1) + ... + (i + 31))/32 
        */
        // smoothing the terrain
        for (int i = 0; i < 863;i++) {
            int new_height = 0;
            for (int k = 1; k < 33; k++) {
                new_height += height_1d[i+k];
            } 
            height_1d[i] = new_height/32;
        }
        // twice
        for (int i = 0; i < 863;i++) {
            int new_height = 0;
            for (int k = 1; k < 33; k++) {
                new_height += height_1d[i+k];
            } 
            height_1d[i] = new_height/32;
        }
        //initialize players
        for (Tank tank: tanks) {
            x = tank.getX();
            tank.setY(height_1d[x] - 1);
            String mau = players.getString(tank.name);
            if (mau.equals("random")) {
                tank.setRandomCol();
            } else {
                String[] col = mau.split(",\\s*");
                int[] rgb_index = new int[3];
                rgb_index[0] = Integer.parseInt(col[0]);
                rgb_index[1] = Integer.parseInt(col[1]);
                rgb_index[2] = Integer.parseInt(col[2]);
                tank.setCol(rgb_index);
            }
        }
    }
    // Adding the score from the previous level
    public void addScores(App app) {
        for (Tank tank: app.scores) {
            int index = app.scores.indexOf(tank);
            tanks.get(index).score += tank.score;
        }
    }
    // Initializing the ordered list of players' turns
    public void makePlayerTurns() {
        turns = new LinkedList<Tank>();
        turns.addAll(tanks);
    }

    public Tank nextPlayer(App app) {
        boolean loop = true;
        while (loop) {
            Tank head = turns.remove();
            turns.add(head);
            if (head.isAlive) {
                startArrow = app.millis();
                return head;
            }
        }
        return null; // not gonna happen lol (hopefully)
    }
}