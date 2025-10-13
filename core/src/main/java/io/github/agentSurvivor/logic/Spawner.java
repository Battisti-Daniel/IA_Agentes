package io.github.agentSurvivor.logic;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import io.github.agentSurvivor.codes.Enemy;
import io.github.agentSurvivor.codes.GameLogic;
import io.github.agentSurvivor.world.WorldState;

public class Spawner {

    private float timer = 0f;
    private float interval = 1.2f; // começa mais lento, acelera levemente
    private int spawnedCount = 0;

    public void reset() {
        timer = 0f;
        interval = 1.2f;
        spawnedCount = 0;
    }

    public boolean update(float dt,
                          WorldState world,
                          float screenW, float screenH,
                          Array<Enemy> enemies,
                          float elapsed,
                          float hpMulFromDiff) {

        boolean bossSpawnedThisFrame = false;

        // acelera de leve com o tempo (não muito agressivo)
        float targetInterval = Math.max(0.35f, 1.2f - (elapsed / 60f) * 0.15f);
        interval = MathUtils.lerp(interval, targetInterval, 0.05f);

        timer += dt;
        while (timer >= interval) {
            timer -= interval;
            spawnedCount++;

            // a cada 20 spawns -> boss
            if (spawnedCount % 20 == 0) {
                enemies.add(GameLogic.spawnBossAtEdge(screenW, screenH, elapsed, hpMulFromDiff));
                bossSpawnedThisFrame = true;
            } else {
                enemies.add(GameLogic.spawnEnemyAtEdge(screenW, screenH, elapsed, hpMulFromDiff));
            }
        }

        return bossSpawnedThisFrame;
    }
}
