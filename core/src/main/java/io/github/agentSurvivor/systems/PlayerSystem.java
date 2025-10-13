package io.github.agentSurvivor.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.agentSurvivor.codes.Enemy;
import io.github.agentSurvivor.codes.Player;

import java.util.function.BiConsumer;

public class PlayerSystem {
    private final Vector2 dir = new Vector2();
    private final Vector2 mouseWorld = new Vector2();

    public void update(
        Player player,
        float dt,
        boolean agentMode,
        Array<Enemy> enemies,
        float screenW,
        float screenH,
        BiConsumer<Vector2, Vector2> spawnBullet
    ) {
        player.updateTimers(dt);

        if (!agentMode) {
            // movimento (WASD/Setas)
            dir.setZero();
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  dir.x -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir.x += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    dir.y += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  dir.y -= 1f;

            player.move(dir, dt, screenW, screenH);

            // tiro (barra de espaço) em direção ao mouse (1 bala; cooldown controlado no Player)
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && player.canShoot()) {
                mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
                mouseWorld.y = screenH - mouseWorld.y;
                player.shootAt(mouseWorld, spawnBullet);
            }else if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && player.canShoot()) {
                mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
                mouseWorld.y = screenH - mouseWorld.y;
                player.shootAt(mouseWorld, spawnBullet);
            }
        } else {
            player.move(dir.setZero(), dt, screenW, screenH);
        }
    }
}
