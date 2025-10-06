package io.github.agentSurvivor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.*;
import io.github.agentSurvivor.codes.Player;

public class HudRenderer {
    private BitmapFont hudFont, bigFont;
    private final GlyphLayout layout = new GlyphLayout();

    public void create() {
        hudFont = new BitmapFont();
        bigFont = new BitmapFont();
        bigFont.getData().setScale(1.5f);
    }

    public void renderTopLeft(SpriteBatch batch, Player player, int score, boolean agentMode) {
        hudFont.setColor(Color.WHITE);
        layout.setText(hudFont, "HP: " + player.hp + "   Score: " + score + "   Modo: " + (agentMode ? "AGENTE" : "HUMANO"));
        hudFont.draw(batch, layout, 10, Gdx.graphics.getHeight() - 10);
    }

    public void renderGameOver(SpriteBatch batch) {
        String t1 = "GAME OVER";
        String t2 = "ENTER: Reiniciar   |   ESC: Menu";

        bigFont.setColor(Color.WHITE);
        layout.setText(bigFont, t1);
        bigFont.draw(batch, layout,
            (Gdx.graphics.getWidth() - layout.width) / 2f,
            Gdx.graphics.getHeight() / 2f + 40);

        layout.setText(hudFont, t2);
        hudFont.draw(batch, layout,
            (Gdx.graphics.getWidth() - layout.width) / 2f,
            Gdx.graphics.getHeight() / 2f - 4);
    }

    public void dispose() {
        if (hudFont != null) hudFont.dispose();
        if (bigFont != null) bigFont.dispose();
    }
}
