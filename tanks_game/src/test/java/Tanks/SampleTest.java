package Tanks;


import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONObject;

import org.junit.jupiter.api.Test;

import com.jogamp.newt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

import java.security.Key;
import java.util.ArrayList;

public class SampleTest {
    // Testing the main class
    @Test
    public void testApp() {
        App app = new App();
        app.loop();
        PApplet.runSketch(new String[] { "App" }, app);
        app.setup();
        app.delay(1000); // to give time to initialise stuff before drawing begins
        
        // level switching
        app.level_index = 1;

        
        //
    }

    @Test
    public void testSettingUpAndLoadingLevels() {
        App app = new App();
        JSONObject config = app.loadJSONObject("config.json");
        app.levels = config.getJSONArray("levels");
        assertNotNull(app.levels);
        // initial setup
        app.setup();
        assertTrue(app.levels.size() > 0);
        //ArrayList<Level> levels = app.all_levels;
        Level dust2 = new Level(app, app.levels.getJSONObject(0));
        Tank dummy = new Tank("A", 69);
        dust2.resetLevel();
        app.cur_lev = dust2;
        
        app.level_index = 1;
        
        app.setLevel();
        assertNotNull(app.cur_lev);
        app.cur_lev.loadTerrain();
        app.cur_lev.makePlayerTurns();

        assertNotNull(app.activeTank);
        assertNotNull(app.startArrow);
        assertNotNull(app.activeProj);
        assertNotNull(app.players);
        assertNotNull(app.ordered);
        app.level_index = 0;
        app.setLevel();
        // test key handling   
        app.activeTank = dummy;     
        app.key = 'r';
        app.keyPressed(null);
        app.key = 'w';
        app.keyPressed(null);
        app.key = 's';
        app.keyPressed(null);
        app.key = 'p';
        app.keyPressed(null);
        app.key = 'f';
        app.keyPressed(null);
        app.key = 'x';
        app.keyPressed(null);
        
        // draw
        app.cur_lev.height_1d = new int[896];
        app.draw();
    }

    /* @Test
    public void testKeyEventsHandling() {
        App app = new App();
        app.setup();
        app.setLevel();
        //app.keyPressed(new KeyEvent(app, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_SPACE, ' '));
        assertTrue(app.endGame);
        assertFalse(app.cur_lev.tanks.isEmpty());
    } */

    @Test
    public void testTank() {
        App app = new App();
        Tank dummy = new Tank("A", 69);
        assertNotNull(dummy);
        // changing power of the tank's next shot
        dummy.addPow(app);
        assertEquals(51, dummy.power);
        dummy.reducePow(app);
        assertEquals(50, dummy.power);

        // adjusting turret angle
        dummy.aim(app, 0.69f);
        // keeping angle in range [-PI/2, PI/2]
        dummy.aim(app, -5);
        dummy.score = 69;
        dummy.setX(10);
        dummy.setY(20);
        dummy.getX();
        dummy.getY();
        dummy.getScore();
        int[] rgb = new int[3];
        dummy.setCol(rgb);
        dummy.setRandomCol();
        dummy.shoot();
        app.cur_lev.height_1d = new int[896];
        app.cur_lev.height_1d[69] = 300;

        dummy.using_parachute = true;
        dummy.draw(app);
        dummy.inAir = true;
        dummy.draw(app);
        dummy.using_parachute = false;
        dummy.draw(app);


        dummy.fall(app);
        dummy.hp = 0;
        dummy.update(app);
        dummy.draw(app);

        dummy.y_coord = 800;
        dummy.update(app);
        dummy.draw(app);

        dummy.bigBalls = true;
        dummy.using_parachute = true;
        dummy.draw(app);
        dummy.inAir = true;
        dummy.draw(app);
        dummy.using_parachute = false;
        dummy.draw(app);

        dummy.fall(app);
   
        dummy.drawExplosion(app, 10, 0.2f);
    }
}
