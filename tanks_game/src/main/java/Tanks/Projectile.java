package Tanks;

import java.util.HashSet;

public class Projectile {
    float x;
    float y;
    float vel;
    float velx;
    float vely;
    float direction;
    float wind = 0;
    int size = 8;
    int[] color;
    boolean dead = false;
    boolean explode = false;
    float startTime; // time of collison
    int explosionRadius = 30;
    float explosionTime = 0.2f;
    Tank shooter; // Tank that shot the projectile
    boolean big; // whether the bullet is supercharged/a powerup shot or not
    // Each terrain should only be affected once
    HashSet<Integer> affectedTerrain = new HashSet<>(); 
    // Each tank should only take damage once
    HashSet<Tank> tanksDamaged = new HashSet<>();

    public Projectile(Tank shooter, float x, float y, float vel, float direction) {
        this.shooter = shooter;
        this.x = x;
        this.y = y;
        this.vel = vel;
        this.direction = direction;
        this.color = shooter.color;
        this.velx = vel * App.sin(direction);
        this.vely = vel * App.cos(direction);
        if (shooter.bigBalls) {
            big = true;
        }
    }
    public void update(App app, float deltaSec) {
        if (dead) {
            big = false;
            return;
        }
        this.velx += app.wind * 0.03f;  // wind
        this.vely -= 3.6; // gravity 
        x += velx * deltaSec;
        y -= vely * deltaSec; 

        int roundedx = Math.round(x);
        int roundedy = Math.round(y);
        // out of bounds
        if (roundedx < -3 | roundedx > App.WIDTH + 3 | roundedy > App.HEIGHT+3) { //out of bounds
            dead = true;
        }
        // collision detection
        if (roundedx >= 0 && roundedx <= 863) {
            if (app.cur_lev.height_1d[roundedx] <= roundedy) {
                startTime = app.millis();
                dead = true;
                explode = true;
            }
        }
        // Projectile goes above and beyond the screen
        if (roundedy < 0) {
            return;
        }
    }
    public void draw(App app) { 
        if (!dead) {
            if (big) { // added outlines for powerup shots
                app.strokeWeight(3);
                app.stroke(0);
            }
            app.fill(color[0], color[1], color[2]);
            app.ellipse(x, y, size, size);
            app.noStroke();
        }
        if (explode) {
            if (big) { 
                explosionRadius = 60;
            }      
            float elapsedTime = (app.millis() - startTime)/1000; // in seconds
            if (elapsedTime <= explosionTime) {
                float redRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius);
                float orangeRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius * 0.5f);
                float yellowRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius * 0.2f);

                app.fill(255, 0, 0); // Red color
                app.ellipse(x, y, 2 * redRadius, 2 * redRadius);

                app.fill(255, 128, 0); // Orange color
                app.ellipse(x, y, 2 * orangeRadius, 2 * orangeRadius);

                app.fill(255, 255, 0); // Yellow color
                app.ellipse(x, y, 2 * yellowRadius, 2 * yellowRadius); 

            int projx = Math.round(x);
            int projy = Math.round(y);
            
            // check affected terrain (loop through ONCE)
            for (int i = projx - explosionRadius; i <= projx + explosionRadius; i++) {
                if (!affectedTerrain.contains(i) && i >= 0 && i <= 863) {
                    int terx = i;
                    int tery = app.cur_lev.height_1d[i];
                    // distance of terrain to center of explosion SQUARED
                    double val = Math.sqrt(Math.pow(terx - projx, 2) + Math.pow(tery - projy, 2));
                    // terrain is outside bomb radius and below
                    if (val > explosionRadius && tery > projy) {}
                    // if terrain is in lower semi-circle of bomb radius
                    if (val <= explosionRadius) {
                        app.cur_lev.height_1d[i] = (int)(Math.sqrt(Math.pow(explosionRadius, 2) - Math.pow(terx - projx, 2)) + projy);
                    }
                    // if terrain is outside radius but above contact circle --> falls down
                    if (val > explosionRadius && tery < projy){
                        int chord = (int)(Math.sqrt(Math.pow(explosionRadius, 2) - Math.pow(terx - projx, 2)));
                        app.cur_lev.height_1d[i] += 2* chord;
                    }
                    affectedTerrain.add(i);
                }
            }  
            // check affected tank(s) ONCE each
            for (Tank tank: app.cur_lev.tanks) {
                if (!tanksDamaged.contains(tank)) {
                    int dist = (int) (Math.sqrt(Math.pow(tank.getX() - projx, 2) + Math.pow(tank.getY() - projy, 2)));
                    int dmg = 0; 
                    if (dist >= 0 && dist < explosionRadius) {
                        if (big) {
                            dmg = 60 * (1 - dist/explosionRadius); //double the range (same damage)
                        } else {
                            dmg = 2 * (explosionRadius - dist); //normal shot
                        }
                        if (dmg > tank.hp) {
                            dmg = tank.hp;
                        }
                        tank.hp -= dmg;
                        if (tank != shooter) {
                            shooter.score += dmg;
                        } 
                        tank.update(app);
                    }
                    tanksDamaged.add(tank);
                }
                if (!tank.equals(shooter) && tank.parachute == 0) {
                    shooter.score += tank.fall(app);
                }  
            }  
            }
            dead = true;
        }
    }


}
