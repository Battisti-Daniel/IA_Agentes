package io.github.agentSurvivor.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import io.github.agentSurvivor.codes.Bullet;

public class BulletSystem {
    public void update(Array<Bullet> bullets, float dt) {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(dt);
            if (b.isExpired() || b.isOffscreen(w, h, 10f)) {
                bullets.removeIndex(i);
            }
        }
    }
}
