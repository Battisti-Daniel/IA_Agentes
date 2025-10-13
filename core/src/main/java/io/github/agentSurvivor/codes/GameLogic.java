package io.github.agentSurvivor.codes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public final class GameLogic {
    private GameLogic() {}

    // ====== PROGRESSÃO POR ABATE (tier global) ======
    private static int sKillTier = 0;
    public static void setKillTier(int tier) { sKillTier = Math.max(0, tier); }

    // ====== VELOCIDADE (com e sem multiplicador — para compatibilidade) ======
    public static float enemySpeed(float elapsed, float diffSpeedMul) {
        float base = 50f + Math.min(80f, elapsed * 2.0f);
        float tierMul = 1f + 0.08f * sKillTier;
        return base * tierMul * Math.max(0.1f, diffSpeedMul);
    }

    public static float enemySpeed(float elapsed) {
        return enemySpeed(elapsed, 1f);
    }

    // ---------------- SPAWNS ----------------

    public static Enemy spawnEnemyAtEdge(float w, float h) {
        Vector2 p = randEdge(w, h);
        int baseHp = 1;
        int hpScaled = Math.max(1, Math.round(baseHp * (1f + 0.15f * sKillTier)));
        return new Enemy(p, 10f, 60f, hpScaled);
    }

    public static Enemy spawnEnemyAtEdge(float w, float h, float elapsed, float hpMulFromDiff) {
        Enemy e = spawnEnemyAtEdge(w, h);
        e.hp = Math.max(1, Math.round(e.hp * Math.max(0.1f, hpMulFromDiff)));
        return e;
    }

    public static Enemy spawnBossAtEdge(float w, float h) {
        Vector2 p = randEdge(w, h);
        Enemy e = Enemy.makeBoss(p);
        e.hp = Math.max(e.hp, Math.round(e.hp * (1f + 0.20f * sKillTier)));
        return e;
    }

    public static Enemy spawnBossAtEdge(float w, float h, float elapsed, float hpMulFromDiff) {
        Enemy e = spawnBossAtEdge(w, h);
        e.hp = Math.max(1, Math.round(e.hp * Math.max(0.1f, hpMulFromDiff)));
        return e;
    }

    private static Vector2 randEdge(float w, float h) {
        int edge = MathUtils.random(3);
        float x=0,y=0;
        switch (edge) {
            case 0: x = MathUtils.random(w); y = h + 20; break;
            case 1: x = w + 20; y = MathUtils.random(h); break;
            case 2: x = MathUtils.random(w); y = -20; break;
            default: x = -20; y = MathUtils.random(h); break;
        }
        return new Vector2(x, y);
    }

    // ------------- COLISÕES / PONTOS -------------

    public static boolean handleBulletEnemyCollisions(Array<Bullet> bullets, Array<Enemy> enemies, Array<Vector2> gems) {
        boolean bossKilled = false;

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            boolean hit = false;

            for (int j = bullets.size - 1; j >= 0; j--) {
                Bullet b = bullets.get(j);
                if (intersectsCircle(e.pos, e.r, b.pos, b.r)) {
                    bullets.removeIndex(j);
                    e.hp = Math.max(0, e.hp - 1);
                    hit = true;
                    break;
                }
            }

            if (hit && e.hp <= 0) {
                if (e.isBoss) bossKilled = true;
                gems.add(new Vector2(e.pos));
                enemies.removeIndex(i);
            }
        }
        return bossKilled;
    }

    public static boolean handleEnemyPlayerCollisions(
        Array<Enemy> enemies,
        Player player,
        float knockbackStrength,
        float elapsed,
        float diffDamageMul
    ) {
        boolean bossTouched = false;
        float tierMul = 1f + 0.10f * sKillTier;

        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (intersectsCircle(e.pos, e.r, player.pos, player.r)) {
                int base = e.isBoss ? 12 : 6;
                int dmg = Math.max(1, Math.round(base * tierMul * Math.max(0.1f, diffDamageMul)));
                player.hp = Math.max(0, player.hp - dmg);
                player.knockbackFrom(e.pos, knockbackStrength);

                if (e.isBoss) {
                    Vector2 away = new Vector2(e.pos).sub(player.pos);
                    if (away.len2() > 0.001f) {
                        away.nor().scl(20f);
                        e.pos.add(away);
                    }
                } else {
                    enemies.removeIndex(i);
                }
            }
        }
        return bossTouched;
    }

    public static int pickGems(Array<Vector2> gems, Player player, float pickRadius, int pointsPerGem) {
        int gained = 0;
        float pickR2 = pickRadius * pickRadius;
        for (int i = gems.size - 1; i >= 0; i--) {
            Vector2 g = gems.get(i);
            if (g.dst2(player.pos) <= pickR2) {
                gems.removeIndex(i);
                gained += pointsPerGem;
            }
        }
        return gained;
    }

    // ===== helpers geométricos =====
    public static boolean intersectsCircle(Vector2 c1, float r1, Vector2 c2, float r2) {
        float dx = c1.x - c2.x;
        float dy = c1.y - c2.y;
        float rr = r1 + r2;
        return dx*dx + dy*dy <= rr*rr;
    }
}
