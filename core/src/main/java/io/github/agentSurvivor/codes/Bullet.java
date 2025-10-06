package io.github.agentSurvivor.codes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
    // ====== Config do sprite/anim ======
    private static final String  SHEET_PATH   = "sprite_player/hit.png"; // seu sprite
    private static final int     COLS         = 5;   // 5 quadros na horizontal
    private static final int     ROWS         = 1;
    private static final float   FRAME_SEC    = 0.07f; // velocidade da animação
    private static final float   DRAW_SCALE   = 1.5f;  // tamanho na tela
    private static final float   BASE_ANGLE   = 0f;    // 0° se o sprite aponta p/ direita (ajuste se necessário)

    // Compartilhados entre todas as balas
    private static Texture                     sSheet;
    private static Animation<TextureRegion>    sAnim;

    // ====== Estado ======
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public float r = 4f;             // raio para colisão simples
    private float life = 3.0f;       // duração
    private float animTime = 0f;
    private float rotationDeg = 0f;

    public Bullet(Vector2 start, Vector2 velocity) {
        ensureLoaded();
        this.pos.set(start);
        this.vel.set(velocity);
        this.rotationDeg = vel.angleDeg() + BASE_ANGLE;
    }

    public void update(float dt) {
        pos.mulAdd(vel, dt);
        life -= dt;
        animTime += dt;
        rotationDeg = vel.angleDeg() + BASE_ANGLE;
    }

    public boolean isExpired() { return life <= 0f; }

    public boolean isOffscreen(float w, float h, float margin) {
        return pos.x < -margin || pos.x > w + margin || pos.y < -margin || pos.y > h + margin;
    }

    // ====== Render ======
    public void render(SpriteBatch batch, float dtIgnored) {
        ensureLoaded();
        TextureRegion frame = sAnim.getKeyFrame(animTime, true);

        float w = frame.getRegionWidth()  * DRAW_SCALE;
        float h = frame.getRegionHeight() * DRAW_SCALE;

        // desenha rotacionando ao redor do centro
        batch.draw(
            frame,
            pos.x - w * 0.5f, pos.y - h * 0.5f,
            w * 0.5f, h * 0.5f,     // origem
            w, h,
            1f, 1f,
            rotationDeg
        );
    }

    /** Fallback antigo (se ainda houver algum draw com ShapeRenderer). */
    public void render(ShapeRenderer shapes) {
        shapes.circle(pos.x, pos.y, r, 12);
    }

    // ====== Recursos ======
    private static void ensureLoaded() {
        if (sAnim != null) return;

        sSheet = new Texture(Gdx.files.internal(SHEET_PATH));
        sSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int fw = sSheet.getWidth() / COLS;
        int fh = sSheet.getHeight() / ROWS;
        TextureRegion[][] grid = TextureRegion.split(sSheet, fw, fh);

        TextureRegion[] frames = new TextureRegion[COLS * ROWS];
        int idx = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                frames[idx++] = grid[r][c];

        sAnim = new Animation<>(FRAME_SEC, frames);
        sAnim.setPlayMode(Animation.PlayMode.LOOP);
    }

    /** Chame no GameScreen.dispose(). */
    public static void disposeShared() {
        if (sSheet != null) {
            sSheet.dispose();
            sSheet = null;
            sAnim = null;
        }
    }
}
