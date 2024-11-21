package Tanks;
import java.lang.Math;
import processing.core.PImage;

public class Tank {
    String name;
    int[] color;
    int x_coord; // x-coordinate of the tank
    float y_coord; // y-coordinate of the tank
    int size = 16;
    final float MIN_ANGLE = (float)-Math.PI/2;
    final float MAX_ANGLE = (float)Math.PI/2;
    final float MAX_VELOCITY = 540; // power = 100
    final float MIN_VELOCITY = 60;  // power = 0
    // Default 
    float turretLength = 15;
    int fuel = 250;
    int power = 50;
    float aimAngle = 0;
    int hp = 100;
    int score = 0;
    int parachute = App.INITIAL_PARACHUTES; // 1
    boolean bigBalls = false; // Name of powerup xD
    boolean isAlive = true;
    boolean exploded = false;
    boolean outOfMap = false;
    boolean takenFallDmg = false;
    boolean inAir = false;
    boolean using_parachute = false;
    // Explosion after falling out of the map   
    float outOfMapTime;
    int outOfMapRadius = 30;
    // Explosion after hp reduces to 0
    float deathTime;
    int deathRadius = 15;
    float explosionTime = 0.2f;
    // rainbow color when powerup is activated
    int hue = 0; 
    // load parachute image
    PImage bigPara;

    public Tank(String name, int x) {
        this.x_coord = x;
        this.name = name;
        this.aimAngle = 0;
    }

    public int getScore() {
        return this.score;
    }

    public int getX() {
        return this.x_coord;
    }

    public void setX(int x) {
        this.x_coord = x;
    }

    public float getY() {
        return this.y_coord;
    }

    public void setY(int y) {
        this.y_coord = y;
    }
    public void setCol(int[] color) {
        this.color = color;
    }
    public void setRandomCol() {
        int[] rgb_index = new int[3];
        rgb_index[0] = App.random.nextInt(256);
        rgb_index[1] = App.random.nextInt(256);
        rgb_index[2] = App.random.nextInt(256);
        this.color = rgb_index;
    }
    public void addPow(App app) {
        power = App.constrain(power + 1, 0, hp);
    }
    public void reducePow(App app) {
        power = App.constrain(power - 1, 0, hp);
    }

    public void aim(App app, float angle) {
        aimAngle = App.constrain(aimAngle + angle, MIN_ANGLE, MAX_ANGLE);
    }
    public Projectile shoot() {
        float speed = Math.round(MIN_VELOCITY + power * (MAX_VELOCITY-MIN_VELOCITY)* 0.01f); // 60 + 0.48 * power (60 to 540 px/s)
        return new Projectile(this, x_coord, y_coord, speed * 0.7f, aimAngle);
    }
    // Calculate fall damage to be added to shooter's score
    public int fall(App app) { 
        int dmg = 0;
        // When the tank is in the air
        if (y_coord < app.cur_lev.height_1d[x_coord] - 1) {
            inAir = true;
            if (!takenFallDmg) {
                dmg = (int) (app.cur_lev.height_1d[x_coord] - 1 - y_coord);
                takenFallDmg = true;
                if (parachute == 0) {
                    if (dmg > hp) {
                        dmg = hp;
                    }
                    hp -= dmg;
                }
                return dmg;
            }
            if (parachute > 0) {
                using_parachute = true; 
                y_coord += 2;
            } else {
                using_parachute = false;
                y_coord += 4;
            }
        } else { // After tank has landed
            inAir = false;
            if (takenFallDmg) {
                if (using_parachute) {
                    parachute -= 1;
                    dmg = 0;
                    using_parachute = false;
                    takenFallDmg = false; 
                }
                if (dmg > hp) {
                    dmg = hp;
                }
                takenFallDmg = false;
                hp -= dmg;
            }
        }
        return dmg;   
    }
    public void update(App app) { 
        // Limiting score, parachute, hp
        hp = App.constrain(hp, 0, 100);
        power = App.constrain(power, 0, hp);
        parachute = App.constrain(parachute, 0, (int) Double.POSITIVE_INFINITY); 
        if (!isAlive) {
            return;
        }
        if (hp <= 0) {
            deathTime = app.millis();
            exploded = true; // blows up with radius 15
            inAir = false;
            isAlive = false;
        }
        // falling out of the map
        if (y_coord >= 640) { 
            outOfMapTime = app.millis();
            outOfMap = true; // blows up with radius 30
            inAir = false;
            isAlive = false;
        }
        // check falling state
        fall(app);
        if (outOfMap) {
            y_coord = 640; // bottom of the map
        }   
    }
    /* 
     *  Drawing explosions for both death scenarios
     */
    public void drawExplosion(App app, float startTime, float explosionRadius) {
        float elapsedTime = (app.millis() - startTime)/1000; // in seconds
        if (elapsedTime <= explosionTime) {
            // Calculating the radius of the inner explosions in ratio to bomb radius
            float redRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius);
            float orangeRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius * 0.5f);
            float yellowRadius = App.map(elapsedTime, 0, explosionTime, 0, explosionRadius * 0.2f);
            app.fill(255, 0, 0); 
            app.ellipse(x_coord, y_coord, 2 * redRadius, 2 * redRadius);

            app.fill(255, 128, 0); 
            app.ellipse(x_coord, y_coord, 2 * orangeRadius, 2 * orangeRadius);

            app.fill(255, 255, 0); 
            app.ellipse(x_coord, y_coord, 2 * yellowRadius, 2 * yellowRadius);
        }
    }
    public void draw(App app) {
        if (using_parachute && inAir && isAlive) { 
            app.image(app.bigPara, x_coord - 32, y_coord - 64);
        }
        if (exploded) {
            drawExplosion(app, deathTime, deathRadius);
            }
        if (outOfMap) {
            drawExplosion(app, outOfMapTime, outOfMapRadius);
        }
        if (isAlive) {
            hue = (hue + 5) % 360;
            app.pushMatrix(); 
                // Change color of turret when powerup is activated
                if (bigBalls) {
                    app.strokeWeight(2);
                    app.stroke(hue % 360, 128, 255);
                } else {
                    app.noStroke();
                }
                app.translate(x_coord, y_coord);
                // Draw turret with angle
                app.fill(0);
                app.rotate(aimAngle);
                app.rect(-size/8, -size * 1.15f, size * 0.25f, 15);
                app.rotate(-aimAngle);
                app.noStroke();
                // Draw tank body
                app.fill(color[0], color[1], color[2]);
                app.rect(-size/2, 0, size, size / 4); 
                app.ellipse(0, 0, size * 0.8f, size * 0.5f);
            app.popMatrix();
        }
    }
}
