package io.github.agentSurvivor.codes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/** Inimigo com sprite (scorpion) e suporte a BOSS e SUPER BOSS. */
public class Enemy {

    // ===== Sprite base (inimigo comum) =====
    private static final String SHEET_PATH = "sprite_mob/scorpion.png";
    private static final int COLS = 4;
    private static final int ROWS = 1;
    private static final float WALK_FRAME_SEC = 0.12f;

    /** Se o sprite original olha para a ESQUERDA, mantenha true. */
    private static final boolean BASE_FACES_LEFT = true;

    // ===== Escalas visuais =====
    private static final float NORMAL_SCALE = 2.0f;
    private static final float BOSS_SCALE   = 3.0f;

    // ===== Frames do boss registrados externamente (GameScreen) =====
    private static TextureRegion[] BOSS_FRAMES;
    public static void setBossFrames(TextureRegion[] frames) { BOSS_FRAMES = frames; }

    // ===== Gameplay =====
    public final Vector2 pos = new Vector2();
    public float r = 10f;         // raio para colisão
    public float baseSpeed = 60f; // velocidade base (a efetiva vem de fora)
    public int hp = 1;
    public int maxHp = 1;         // <<< NOVO: vida máxima para barra
    public boolean isBoss = false;

    private final Vector2 tmp = new Vector2();

    // ===== Visual =====
    private Texture sheetTex; // só do inimigo comum
    private Animation<TextureRegion> walkAnim;     // comum / também usado no super boss
    private Animation<TextureRegion> bossAnim;     // boss (se frames forem fornecidos)
    private TextureRegion current;
    private float animTime = 0f;
    private boolean facingLeft = false;
    private float visualScale = NORMAL_SCALE;

    // “morte” sem frames dedicados (fade/encolher)
    private boolean dying = false;
    private boolean diedAndFinished = false;
    private float dieTime = 0f;
    private static final float DIE_DURATION = 0.45f;

    // ===== Construtores =====
    public Enemy(Vector2 start) { this(start, 10f, 60f, 1); }

    public Enemy(Vector2 start, float r, float baseSpeed, int hp) {
        this.pos.set(start);
        this.r = r;
        this.baseSpeed = baseSpeed;
        this.hp = hp;
        this.maxHp = hp;             // <<< NOVO
        this.visualScale = NORMAL_SCALE;
        loadCommonSheet();
        buildBossAnimIfPossible();
    }

    private void loadCommonSheet() {
        sheetTex = new Texture(Gdx.files.internal(SHEET_PATH));
        int fw = sheetTex.getWidth() / COLS;
        int fh = sheetTex.getHeight() / ROWS;

        TextureRegion[][] grid = TextureRegion.split(sheetTex, fw, fh);
        Array<TextureRegion> frames = new Array<>(COLS);
        for (int c = 0; c < COLS; c++) frames.add(grid[0][c]);

        walkAnim = new Animation<>(WALK_FRAME_SEC, frames, Animation.PlayMode.LOOP);
        current = frames.first();
    }

    private void buildBossAnimIfPossible() {
        if (BOSS_FRAMES != null && BOSS_FRAMES.length > 0) {
            Array<TextureRegion> frames = new Array<>(BOSS_FRAMES.length);
            for (TextureRegion tr : BOSS_FRAMES) frames.add(tr);
            bossAnim = new Animation<>(WALK_FRAME_SEC, frames, Animation.PlayMode.LOOP);
        } else {
            bossAnim = null;
        }
    }

    /** Fábrica de boss. */
    public static Enemy makeBoss(Vector2 p) {
        Enemy e = new Enemy(p);
        e.isBoss = true;
        e.visualScale = BOSS_SCALE;
        e.hp = 12;
        e.maxHp = e.hp;                                // <<< NOVO
        e.r = 20f * (BOSS_SCALE / NORMAL_SCALE);
        return e;
    }

    /** Fábrica de SUPER BOSS (usa sprite própria). */
    public static Enemy makeSuperBoss(Vector2 p) {
        return makeSuperBoss(p, "sprite_mob/finalBoss.png");
    }

    public static Enemy makeSuperBoss(Vector2 p, String spritePath) {
        Enemy e = new Enemy(p);
        e.isBoss = true;
        e.visualScale = BOSS_SCALE * 1.4f;
        e.hp = 120;
        e.maxHp = e.hp;                                // <<< NOVO
        e.r = 30f * (BOSS_SCALE / NORMAL_SCALE);
        e.baseSpeed = 80f;

        // Troca o sprite para o arquivo específico
        if (e.sheetTex != null) e.sheetTex.dispose();
        e.sheetTex = new Texture(Gdx.files.internal(spritePath));
        int fw = e.sheetTex.getWidth() / COLS;
        int fh = e.sheetTex.getHeight() / ROWS;
        TextureRegion[][] grid = TextureRegion.split(e.sheetTex, fw, fh);
        Array<TextureRegion> frames = new Array<>(COLS);
        for (int c = 0; c < COLS; c++) frames.add(grid[0][c]);
        e.walkAnim = new Animation<>(WALK_FRAME_SEC, frames, Animation.PlayMode.LOOP);
        e.current = frames.first();

        e.bossAnim = null; // <<< ESSENCIAL: força usar a animação do sprite do SUPER BOSS
        return e;
    }

    /** Move na direção do alvo (ex.: player) e ajusta a orientação. */
    public void updateTowards(float dt, Vector2 target, float speed) {
        if (dying) return;

        float dx = target.x - pos.x;
        if (Math.abs(dx) > 0.5f) facingLeft = (dx < 0f);

        tmp.set(target).sub(pos);
        if (tmp.len2() > 1e-4f) pos.mulAdd(tmp.nor(), speed * dt);
    }

    /** Inicia a “morte” (fade/encolher). */
    public void startDying() {
        if (dying) return;
        dying = true;
        dieTime = 0f;
    }

    public boolean isDying() { return dying; }
    public boolean isDyingFinished() { return diedAndFinished; }

    /** Desenho com SpriteBatch. */
    public void render(SpriteBatch batch, float dt) {
        animTime += dt;

        // Se existir bossAnim e for boss comum, usa; super boss usa walkAnim.
        Animation<TextureRegion> anim = (isBoss && bossAnim != null) ? bossAnim : walkAnim;
        current = anim.getKeyFrame(animTime, true);

        // flip horizontal
        boolean wantFlipX = (facingLeft != BASE_FACES_LEFT);
        if (current.isFlipX() != wantFlipX) current.flip(true, false);

        float fw = current.getRegionWidth()  * visualScale;
        float fh = current.getRegionHeight() * visualScale;

        float drawX = pos.x - fw * 0.5f;
        float drawY = pos.y - fh * 0.25f;

        if (dying) {
            dieTime += dt;
            if (dieTime >= DIE_DURATION) {
                diedAndFinished = true;
                return;
            }
            float t = dieTime / DIE_DURATION;
            float alpha = 1f - t;
            float s = 1f - 0.6f * t;

            float w = fw * s;
            float h = fh * s;
            float x = pos.x - w * 0.5f;
            float y = pos.y - h * 0.25f;

            Color old = batch.getColor();
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(current, x, y, w, h);
            batch.setColor(old);
        } else {
            batch.draw(current, drawX, drawY, fw, fh);
        }
    }

    /** Fallback antigo (círculo), caso alguém ainda chame via ShapeRenderer. */
    public void render(ShapeRenderer shapes) {
        shapes.setColor(0.85f, 0.2f, 0.25f, 1f);
        shapes.circle(pos.x, pos.y, r, 20);
    }

    public void dispose() {
        if (sheetTex != null) sheetTex.dispose();
    }
}
