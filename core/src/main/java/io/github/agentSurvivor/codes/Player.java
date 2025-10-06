package io.github.agentSurvivor.codes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.function.BiConsumer;

public class Player {
    public enum State { IDLE, WALK, ATTACK }

    // posição/colisão
    public final Vector2 pos = new Vector2();
    public float r = 12f;

    // stats/config
    public final PlayerStats stats;
    public int hp;
    public int lives;

    // timers
    private float fireTimer = 0f;     // cooldown de tiro
    private float invulnTimer = 0f;   // invulnerabilidade (pós-revive)
    private float attackTimer = 0f;   // tempo restante segurando a animação de ataque

    // animação
    private Texture idleTex, walkTex, attackTex;
    private Animation<TextureRegion> idleAnim, walkAnim, attackAnim;
    private float animTime = 0f;
    private boolean facingLeft = false;
    private State state = State.IDLE;
    private float drawScale = 2f;
    private TextureRegion currentFrame;

    // ajuste dos spritesheets (CONFIRA com seus PNGs!)
    private static final int IDLE_COLS = 5, IDLE_ROWS = 1;  // idle.png
    private static final int WALK_COLS = 8, WALK_ROWS = 1;  // walking.png
    private static final int ATT_COLS  = 6, ATT_ROWS  = 1;  // attack.png

    private static final float IDLE_FRAME_SEC = 0.12f;
    private static final float WALK_FRAME_SEC = 0.10f;
    private static final float ATT_FRAME_SEC  = 0.08f;

    // fallback caso queira “segurar” o ataque por um mínimo
    private static final float ATTACK_HOLD_MIN = 0.35f;

    // util
    private final Vector2 tmp = new Vector2();

    public Player(Vector2 start, PlayerStats stats) {
        this.pos.set(start);
        this.stats = stats;
        this.hp = stats.maxHp;
        this.lives = stats.lives;
        loadAnimations();
    }

    private void loadAnimations() {
        // *** Ajuste os caminhos conforme seu projeto (dentro de core/assets) ***
        idleTex   = new Texture(Gdx.files.internal("sprite_player/idle.png"));
        walkTex   = new Texture(Gdx.files.internal("sprite_player/walking.png"));
        attackTex = new Texture(Gdx.files.internal("sprite_player/attack.png"));

        idleAnim   = makeAnim(idleTex,   IDLE_COLS, IDLE_ROWS, IDLE_FRAME_SEC,  Animation.PlayMode.LOOP);
        walkAnim   = makeAnim(walkTex,   WALK_COLS, WALK_ROWS, WALK_FRAME_SEC,  Animation.PlayMode.LOOP);
        // o ataque não precisa ser LOOP aqui, pois vamos controlar via getKeyFrame(looping=false)
        attackAnim = makeAnim(attackTex, ATT_COLS,  ATT_ROWS,  ATT_FRAME_SEC,   Animation.PlayMode.NORMAL);
    }

    private Animation<TextureRegion> makeAnim(Texture tex, int cols, int rows, float frameSec, Animation.PlayMode pm) {
        int fw = tex.getWidth() / cols;
        int fh = tex.getHeight() / rows;

        TextureRegion[][] grid = TextureRegion.split(tex, fw, fh);
        Array<TextureRegion> frames = new Array<>(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                frames.add(grid[r][c]);
            }
        }
        Animation<TextureRegion> a = new Animation<>(frameSec, frames);
        a.setPlayMode(pm);
        return a;
    }

    // ================== LÓGICA ==================

    /** Atualize timers (chame 1x por frame). */
    public void updateTimers(float dt) {
        if (fireTimer > 0f)   fireTimer   -= dt;
        if (invulnTimer > 0f) invulnTimer -= dt;

        if (attackTimer > 0f) {
            attackTimer -= dt;
            if (attackTimer < 0f) attackTimer = 0f;
        }
    }

    public boolean canShoot() { return fireTimer <= 0f; }
    public boolean isInvulnerable() { return invulnTimer > 0f; }

    /** Move usando stats.moveSpeed; dir pode ser normalizado ou não. */
    public void move(Vector2 dir, float dt, float screenW, float screenH) {
        if (dir.len2() > 0.0001f) {
            tmp.set(dir).nor().scl(stats.moveSpeed * dt);
            pos.add(tmp);
            onMoved(tmp);

            // só troca para WALK se não estiver “segurando” animação de ataque
            if (attackTimer <= 0f) {
                state = State.WALK;
            }
        } else {
            if (attackTimer <= 0f) {
                state = State.IDLE;
            }
        }

        pos.x = MathUtils.clamp(pos.x, r, screenW - r);
        pos.y = MathUtils.clamp(pos.y, r, screenH - r);
    }

    /** Dispara N projéteis (fan/spread) em direção ao alvo, respeitando cooldown. */
    public void shootAt(Vector2 target, BiConsumer<Vector2, Vector2> spawnBullet) {
        if (!canShoot()) return;

        Vector2 baseDir = new Vector2(target).sub(pos).nor();

        // vire o sprite para o lado do tiro
        if (baseDir.x < -0.01f) facingLeft = true;
        else if (baseDir.x > 0.01f) facingLeft = false;

        int n = Math.max(1, stats.projectilesPerShot);
        if (n == 1) {
            spawnBullet.accept(new Vector2(pos), baseDir.scl(stats.bulletSpeed));
        } else {
            float step = MathUtils.degreesToRadians * 8f; // 8° entre balas
            float total = step * (n - 1);
            float start = -total / 2f;
            for (int i = 0; i < n; i++) {
                float ang = start + i * step;
                Vector2 dir = new Vector2(baseDir).rotateRad(ang).nor().scl(stats.bulletSpeed);
                spawnBullet.accept(new Vector2(pos), dir);
            }
        }

        fireTimer = stats.fireCooldown;
        setAttackHold();
    }

    /** Aplica dano (respeita invulnerabilidade). */
    public void hit(int dmg) {
        if (isInvulnerable()) return;
        hp -= dmg;
    }

    /** Knockback a partir de uma origem. */
    public void knockbackFrom(Vector2 source, float strength) {
        tmp.set(pos).sub(source).nor().scl(strength);
        pos.add(tmp);
    }

    /** Tenta reviver consumindo 1 vida; retorna true se reviveu. */
    public boolean tryRevive(float screenW, float screenH) {
        if (hp > 0) return false;
        if (lives <= 0) return false;
        lives--;
        hp = stats.maxHp;
        pos.set(screenW / 2f, screenH / 2f);
        invulnTimer = stats.reviveInvulnTime;
        return true;
    }

    private void onMoved(Vector2 delta) {
        if (delta.x < -0.01f) facingLeft = true;
        else if (delta.x > 0.01f) facingLeft = false;
    }

    /** Entra no estado de ataque e segura a animação por sua duração. */
    private void setAttackHold() {
        state = State.ATTACK;
        animTime = 0f;

        // segure pelo tempo da animação (ou use um mínimo)
        float animDur = (attackAnim != null) ? attackAnim.getAnimationDuration() : ATTACK_HOLD_MIN;
        attackTimer = Math.max(animDur, ATTACK_HOLD_MIN);
    }

    // ================== RENDER ==================

    public void render(SpriteBatch batch, float dt) {
        animTime += dt;

        Animation<TextureRegion> a =
            (state == State.ATTACK) ? attackAnim :
                (state == State.WALK)   ? walkAnim   : idleAnim;

        // loop apenas quando NÃO for ataque
        boolean looping = (state != State.ATTACK);
        currentFrame = a.getKeyFrame(animTime, looping);

        // flip horizontal quando necessário
        boolean flipX = (facingLeft && !currentFrame.isFlipX()) ||
            (!facingLeft && currentFrame.isFlipX());
        if (flipX) currentFrame.flip(true, false);

        float fw = currentFrame.getRegionWidth()  * drawScale;
        float fh = currentFrame.getRegionHeight() * drawScale;
        float x  = pos.x - fw * 0.5f;
        float y  = pos.y - fh * 0.35f; // “assenta” no chão

        batch.draw(currentFrame, x, y, fw, fh);

        // desfaz o flip para não “sujar” o frame para o próximo draw
        if (flipX) currentFrame.flip(true, false);
    }

    public void dispose() {
        if (idleTex   != null) idleTex.dispose();
        if (walkTex   != null) walkTex.dispose();
        if (attackTex != null) attackTex.dispose();
    }
}
