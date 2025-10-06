package io.github.agentSurvivor.systems;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.agentSurvivor.codes.Enemy;
import io.github.agentSurvivor.codes.GameLogic;


public class EnemySystem {
    public void update(Array<Enemy> enemies, float dt, Vector2 playerPos, float elapsed) {
        float spd = GameLogic.enemySpeed(elapsed);
        for (int i = enemies.size - 1; i >= 0; i--) {
            enemies.get(i).updateTowards(dt, playerPos, spd);
        }
    }
}
