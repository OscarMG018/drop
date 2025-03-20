package io.github.rain.oscar.medina.galvez;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class Main implements ApplicationListener {
    Texture backgroundTexture;
    Texture bucketTexture;
    Texture dropTexture;

    SpriteBatch spriteBatch;
    FitViewport viewport;
    BitmapFont font;

    Sprite bucketSprite;
    Vector2 touchPos;
    Array<Sprite> dropSprites;

    float dropTimer;
    Rectangle bucketRectangle;
    Rectangle dropRectangle;
    int score;
    int missedDrops;
    boolean gameOver;

    private final int MAX_MISSED_DROPS = 3;
    private float DROP_SPAWN_TIME = 1f;
    private float DROP_FALL_SPEED = 3.0f;

    private boolean wasTouched = false;

    @Override
    public void create() {
        backgroundTexture = new Texture("background.png");
        bucketTexture = new Texture("bucket.png");
        dropTexture = new Texture("drop.png");

        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(8, 5);
        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        for (TextureRegion region : font.getRegions()) {
            region.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        bucketSprite = new Sprite(bucketTexture);
        bucketSprite.setSize(1, 1);
        touchPos = new Vector2();
        dropSprites = new Array<>();

        bucketRectangle = new Rectangle();
        dropRectangle = new Rectangle();
        score = 0;
        missedDrops = 0;
        gameOver = false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        input();
        update();
        draw();
    }

    private void input() {
        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                resetGame();
            }
            if (Gdx.input.isTouched()) {
                if (!wasTouched) {
                    resetGame();
                }
                wasTouched = true;
            } else {
                wasTouched = false;
            }
            return;
        }

        if (Gdx.input.isTouched()) {
            wasTouched = true;
        } else {
            wasTouched = false;
        }

        float speed = 4f;
        float delta = Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            bucketSprite.translateX(speed * delta);
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            bucketSprite.translateX(-speed * delta);
        }

        if (Gdx.input.isTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);
            bucketSprite.setCenterX(touchPos.x);
        }
    }

    private void update() {
        if (gameOver) {
            return;
        }

        float worldWidth = viewport.getWorldWidth();
        float bucketWidth = bucketSprite.getWidth();
        float bucketHeight = bucketSprite.getHeight();

        bucketSprite.setX(MathUtils.clamp(bucketSprite.getX(), 0, worldWidth - bucketWidth));

        float delta = Gdx.graphics.getDeltaTime();

        bucketRectangle.set(bucketSprite.getX(), bucketSprite.getY(), bucketWidth, bucketHeight);

        for (int i = dropSprites.size - 1; i >= 0; i--) {
            Sprite dropSprite = dropSprites.get(i);
            float dropWidth = dropSprite.getWidth();
            float dropHeight = dropSprite.getHeight();
            dropSprite.translateY(-DROP_FALL_SPEED * delta);
            dropRectangle.set(dropSprite.getX(), dropSprite.getY(), dropWidth, dropHeight);
            if (dropSprite.getY() < -dropHeight) {
                dropSprites.removeIndex(i);
                if (!Color.RED.equals(dropSprite.getColor()))
                    missedDrops++;
            }
            else if (bucketRectangle.overlaps(dropRectangle)) {
                dropSprites.removeIndex(i);
                if (Color.RED.equals(dropSprite.getColor()))
                    missedDrops++;
                else
                    score++;
            }
        }
        dropTimer += delta;
        if (dropTimer > DROP_SPAWN_TIME) {
            dropTimer = 0;
            createDroplet();
            if (DROP_SPAWN_TIME > 0.2f)
                DROP_SPAWN_TIME *= 0.95f;
            DROP_FALL_SPEED *= 1.01f;
        }
        if (missedDrops >= MAX_MISSED_DROPS) {
            gameOver = true;
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        bucketSprite.draw(spriteBatch);
        for (Sprite dropSprite : dropSprites) {
            dropSprite.draw(spriteBatch);
        }
        font.setColor(Color.WHITE);
        font.getData().setScale(0.03f);
        font.draw(spriteBatch, "Score: " + score, 0.2f, worldHeight - 0.2f);
        font.draw(spriteBatch, "Missed: " + missedDrops + "/" + MAX_MISSED_DROPS, worldWidth - 2.5f, worldHeight - 0.2f);
        if (gameOver) {
            font.getData().setScale(0.05f);
            font.setColor(Color.RED);
            font.draw(spriteBatch, "GAME OVER", worldWidth / 2 - 2f, worldHeight / 2 + 1f);
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, "Final Score: " + score, worldWidth / 2 - 2f, worldHeight / 2);
            font.draw(spriteBatch, "Press SPACE or tap", worldWidth / 2 - 3.2f, worldHeight / 2 - 1f);
        }
        spriteBatch.end();
    }

    private void createDroplet() {
        float dropWidth = 0.8f;
        float dropHeight = 0.8f;
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        Sprite dropSprite = new Sprite(dropTexture);
        if (score >= 10 && score % 5 == 0){
            dropSprite.setColor(Color.RED);
        }
        dropSprite.setSize(dropWidth, dropHeight);
        dropSprite.setX(MathUtils.random(0f, worldWidth - dropWidth));
        dropSprite.setY(worldHeight);
        dropSprites.add(dropSprite);
    }

    private void resetGame() {
        DROP_SPAWN_TIME = 1f;
        DROP_FALL_SPEED = 3f;
        dropSprites.clear();
        score = 0;
        missedDrops = 0;
        gameOver = false;
        float worldWidth = viewport.getWorldWidth();
        bucketSprite.setPosition(worldWidth / 2 - bucketSprite.getWidth() / 2, 0);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        bucketTexture.dispose();
        dropTexture.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
