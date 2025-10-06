package io.github.agentSurvivor.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Align;
import io.github.agentSurvivor.GameMain;
import io.github.agentSurvivor.bridge.GameBridge;
import io.github.agentSurvivor.codes.GifDecoder; // você já incluiu essa classe no projeto
import com.badlogic.gdx.audio.Music;

public class MenuScreen extends ScreenAdapter {
    private final GameMain game;

    // render
    private SpriteBatch batch;
    private GlyphLayout layout;

    // fontes
    private BitmapFont titleFont, optionFont, hintFont;

    // bg animado (GIF)
    private Animation<TextureRegion> gifAnim;
    private float gifTime = 0f;

    // áudio
    private Music bgm;
    private Sound changeSfx;

    // UI helpers
    private Texture pixel; // 1x1 branco para desenhar retângulos coloridos
    private int selected = 0; // 0=Humano, 1=Agente

    // cores da UI
    private final Color panelBg     = new Color(0f, 0f, 0f, 0.45f);
    private final Color optionBg    = new Color(0.08f, 0.1f, 0.16f, 0.75f);
    private final Color highlightBg = new Color(0.25f, 0.5f, 0.9f, 0.65f);
    private final Color titleColor  = Color.WHITE;
    private final Color textNormal  = Color.LIGHT_GRAY;
    private final Color textSelected= Color.WHITE;

    public MenuScreen(GameMain game) { this.game = game; }

    private Music lobbyMusic;
    private static final float LOBBY_GAIN = 0.10f; // bem baixo
    private int volumePercent = 10;                // opcional: pode ler de um Settings global

    @Override public void show() {
        batch  = new SpriteBatch();
        layout = new GlyphLayout();

        // --- pixel 1x1 para desenhar "divs" (retângulos) ---
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // --- fontes (TTF) com fallback ---
        loadFonts();

        // --- GIF de fundo ---
        FileHandle gif = Gdx.files.internal("bg/castle.gif");
        if (gif.exists()) {
            gifAnim = GifDecoder.loadGIFAnimation(Animation.PlayMode.LOOP, gif.read());
        } else {
            Gdx.app.error("MenuScreen", "Não encontrei assets/bg/menu.gif (fundo ficará preto).");
        }

        // --- música do lobby ---
        if (Gdx.files.internal("song/home.mp3").exists()) {
            bgm = Gdx.audio.newMusic(Gdx.files.internal("song/home.mp3"));
            bgm.setLooping(true);
            bgm.setVolume(0.3f);
            bgm.play();
        }

        // --- sfx de mudança de opção (use Sound para efeito curto) ---
        if (Gdx.files.internal("song/changeOp.mp3").exists()) {
            changeSfx = Gdx.audio.newSound(Gdx.files.internal("song/changeOp.mp3"));
        }
    }

    private void loadFonts() {
        // tenta carregar uma TTF; se não achar, usa BitmapFont padrão
        FileHandle f1 = Gdx.files.internal("fonts/font.ttf");
        FileHandle f2 = Gdx.files.internal("fonts/font-Bold.ttf");

        int sw = Gdx.graphics.getWidth();
        float uiScale = sw / 960f; // escala leve conforme resolução

        if (f1.exists() || f2.exists()) {
            FileHandle chosen = f1.exists() ? f1 : f2;

            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(chosen);
            FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.color = Color.WHITE;
            p.borderColor = new Color(0,0,0,0.6f);
            p.borderWidth = 2 * Math.max(1, Math.round(uiScale));
            p.shadowColor = new Color(0,0,0,0.4f);
            p.shadowOffsetX = Math.max(1, Math.round(uiScale));
            p.shadowOffsetY = Math.max(1, Math.round(uiScale));

            p.size = Math.round(48 * uiScale);
            titleFont = gen.generateFont(p);

            p.size = Math.round(26 * uiScale);
            p.borderWidth = Math.max(1, Math.round(uiScale));
            optionFont = gen.generateFont(p);

            p.size = Math.round(16 * uiScale);
            hintFont = gen.generateFont(p);

            gen.dispose();
        } else {
            // fallback: fonte padrão
            titleFont = new BitmapFont(); titleFont.getData().setScale(1.8f);
            optionFont = new BitmapFont(); optionFont.getData().setScale(1.2f);
            hintFont   = new BitmapFont(); hintFont.getData().setScale(0.9f);
        }
    }

    private void drawRect(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private void drawCentered(BitmapFont f, String text, float y, Color color) {
        f.setColor(color);
        layout.setText(f, text);
        float x = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        f.draw(batch, layout, x, y);
    }

    private void drawOption(String text, float centerY, boolean selected) {
        float sw = Gdx.graphics.getWidth();
        float w  = Math.min(520, sw * 0.7f);
        float h  = 44f;

        float x  = (sw - w) * 0.5f;
        float y  = centerY - h * 0.5f; // retângulo centrado no eixo Y

        // fundo da opção
        drawRect(x, y, w, h, selected ? highlightBg : optionBg);

        // texto 100% centralizado dentro do retângulo
        drawCenteredInRect(
            optionFont,
            text,
            x, y, w, h,
            selected ? textSelected : textNormal
        );
    }

    @Override public void render(float delta) {
        // input
        if (Gdx.input.isKeyJustPressed(Keys.UP) || Gdx.input.isKeyJustPressed(Keys.DOWN)) {
            if (changeSfx != null) changeSfx.play(0.25f);
            selected = 1 - selected;
        }
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            GameBridge.get().setAgentMode(selected == 1);
            game.setScreen(new GameScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) Gdx.app.exit();
        if (Gdx.input.isKeyJustPressed(Keys.M) && bgm != null) {
            bgm.setVolume(bgm.getVolume() > 0 ? 0f : 0.3f);
        }

        gifTime += delta;

        // clear
        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        batch.begin();

        // --- fundo (GIF) ---
        if (gifAnim != null) {
            TextureRegion frame = gifAnim.getKeyFrame(gifTime, true);
            batch.draw(frame, 0, 0, sw, sh);
        }

        // --- painel por trás das opções (a "div") ---
        float panelW = Math.min(640, sw * 0.8f);
        float panelH = 160f;
        float panelX = (sw - panelW) * 0.5f;
        float panelY = 200f;
        drawRect(panelX, panelY, panelW, panelH, panelBg);

        // --- título ---
        drawCenteredOnScreen(titleFont, "Agent Survivor", sh - 90f, titleColor);


        // --- opções ---
        drawOption("Jogar (Humano)",  panelY + panelH * 0.66f, selected == 0);
        drawOption("Jogar (Agente)",  panelY + panelH * 0.34f, selected == 1);

        batch.end();
    }

    @Override public void hide() {
        if (bgm != null && bgm.isPlaying()) bgm.pause();
    }

    @Override public void dispose() {
        batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (optionFont != null) optionFont.dispose();
        if (hintFont != null)   hintFont.dispose();
        if (bgm != null)        bgm.dispose();
        if (changeSfx != null)  changeSfx.dispose();
        if (pixel != null)      pixel.dispose();
    }

    // centraliza um texto no retângulo (x,y,w,h)
    private void drawCenteredInRect(BitmapFont f, String text, float x, float y, float w, float h, Color color) {
        f.setColor(color);
        layout.setText(f, text);
        float textX = x + (w - layout.width) * 0.5f;
        // y do BitmapFont é a "baseline": para centralizar verticalmente, soma metade da altura do layout
        float textY = y + (h + layout.height) * 0.5f;
        f.draw(batch, layout, textX, textY);
    }

    // centraliza na tela (X e Y), útil pro título
    private void drawCenteredOnScreen(BitmapFont f, String text, float centerY, Color color) {
        f.setColor(color);
        layout.setText(f, text);
        float sw = Gdx.graphics.getWidth();
        float textX = (sw - layout.width) * 0.5f;
        float textY = centerY + layout.height * 0.5f; // baseline centralizada
        f.draw(batch, layout, textX, textY);
    }

}
