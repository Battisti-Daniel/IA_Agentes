package io.github.agentSurvivor.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.github.agentSurvivor.GameMain;
import io.github.agentSurvivor.bridge.GameBridge;
import io.github.agentSurvivor.codes.*;
import io.github.agentSurvivor.logic.Spawner;
import io.github.agentSurvivor.ui.HudRenderer;
import io.github.agentSurvivor.world.WorldState;
import io.github.agentSurvivor.systems.PlayerSystem;

public class GameScreen extends ScreenAdapter {
    private final GameMain game;

    // chão
    private Texture floorTex;
    private TextureRegion floorRegion;
    private float floorScale = 2f;

    // render
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    // módulos/estado
    private final WorldState world = new WorldState();
    private final Spawner spawner = new Spawner();
    private final HudRenderer hud = new HudRenderer();
    private final PlayerSystem playerSystem = new PlayerSystem();

    // GEM (ponto)
    private Texture gemTex;
    private TextureRegion gemRegion;
    private float gemScale = 1.2f;

    // BOSS (sprite 1x4)
    private static final String BOSS_PATH = "sprite_mob/boss.png";
    private Texture bossTex;

    // ===== Música =====
    private Music[] playlist;
    private int currentTrack = 0;
    private boolean musicEnabled = true;
    private float musicCheckTimer = 0f;

    // Ganhos (reduzem volume sem mexer no slider do jogador)
    private static final float MUSIC_GAIN = 0.20f; // música discreta
    private static final float SFX_GAIN   = 1.50f; // base dos SFX alta

    // Ganhos por efeito
    private static final float SHOOT_GAIN = 2.0f;
    private static final float HURT_GAIN  = 2.3f;
    private static final float BOSS_GAIN  = 3.0f;

    // ===== SFX =====
    private Sound sfxShoot;
    private Sound sfxHurt;
    private Sound sfxBossSpawn;

    // Volume mestre (0–100) controlado no menu Opções
    private int volumePercent = 15;

    // entidades
    private Player player;
    private final Array<Enemy> enemies = new Array<>();
    private final Array<Bullet> bullets = new Array<>();
    private final Array<Vector2> gems = new Array<>();

    // ====== LEVEL-UP (menu de upgrades) ======
    private static class UpgradeOption {
        final Type type;
        final String titulo;
        final String desc;
        final int peso;
        UpgradeOption(Type type, String titulo, String desc, int peso) {
            this.type = type; this.titulo = titulo; this.desc = desc; this.peso = peso;
        }
        enum Type { HP, MOVE_SPEED, PROJ_SPEED, ATK_SPEED, EXTRA_BULLET, EXTRA_LIFE }
    }

    private static final UpgradeOption[] ALL_UPGRADES = new UpgradeOption[] {
        new UpgradeOption(UpgradeOption.Type.HP,          "+10 HP",                   "Aumenta vida máxima e cura +10",   5),
        new UpgradeOption(UpgradeOption.Type.MOVE_SPEED,  "+5% Velocidade de mov.",  "Anda mais rápido",                 4),
        new UpgradeOption(UpgradeOption.Type.PROJ_SPEED,  "+10% Velocidade proj.",   "Projéteis viajam mais rápido",     3),
        new UpgradeOption(UpgradeOption.Type.ATK_SPEED,   "+5% Velocidade de atk",   "Reduz o cooldown de tiro",         3),
        new UpgradeOption(UpgradeOption.Type.EXTRA_BULLET,"+1 projétil por disparo", "Atira mais balas (máx. 6)",        2),
        new UpgradeOption(UpgradeOption.Type.EXTRA_LIFE,  "+1 Vida (revive)",        "Ganha chance extra de reviver",    1),
    };

    private boolean choosingUpgrade = false;
    private UpgradeOption[] currentChoices = new UpgradeOption[3];
    private int nextUpgradeAt = 7;

    // ====== Pausa / Menus ======
    private boolean paused = false;

    private enum PauseSub { ROOT, DIFFICULTY, OPTIONS }
    private PauseSub pauseSub = PauseSub.ROOT;
    private int pauseIndex = 0;   // 0=Resume, 1=Dificuldade, 2=Opções

    private enum Difficulty { FACIL, MEDIANO, DIFICIL }
    private Difficulty difficulty = Difficulty.FACIL;
    private int diffIndex = 1; // 0=fácil,1=mediano,2=difícil

    // multiplicadores por dificuldade
    private float diffSpawnMul = 1f;      // spawns
    private float diffSpeedMul = 1f;      // velocidade
    private float diffDamageMul = 1f;     // dano
    private float diffHpMul     = 1f;     // HP

    // --- progressão por abates ---
    private int killedTotal = 0;
    private int currentKillTier = 0; // 1 tier a cada 20 kills

    public GameScreen(GameMain game) { this.game = game; }

    @Override public void show() {
        shapes = new ShapeRenderer();
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        hud.create();

        Gdx.input.setCatchKey(Input.Keys.ESCAPE, true);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

        // chão
        floorTex = new Texture(Gdx.files.internal("tiles/floor.png"));
        floorTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        floorRegion = new TextureRegion(floorTex);

        // gem
        gemTex = new Texture(Gdx.files.internal("icons/coin.png"));
        gemTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        gemRegion = new TextureRegion(gemTex);

        // sfx (tenta .ogg → .wav → .mp3)
        sfxShoot     = loadSoundSmart("song/hit");
        sfxHurt      = loadSoundSmart("song/hitPlayer");
        sfxBossSpawn = loadSoundSmart("song/spawnBoss");

        initMusic(); // cria playlist e aplica volume

        // boss frames (1x4)
        loadBossFrames();

        setDifficulty(difficulty);
        resetGame();
    }

    /** Tenta carregar um Sound testando OGG → WAV → MP3. */
    private Sound loadSoundSmart(String baseNoExt) {
        String[] candidates = new String[] {
            baseNoExt + ".ogg",
            baseNoExt + ".wav",
            baseNoExt + ".mp3"
        };
        for (String path : candidates) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    Sound s = Gdx.audio.newSound(Gdx.files.internal(path));
                    Gdx.app.log("SFX", "Carregado: " + path);
                    return s;
                }
            } catch (Throwable t) {
                // tenta próximo formato
            }
        }
        Gdx.app.error("SFX", "Falha ao carregar SFX para base: " + baseNoExt + " (tente .ogg ou .wav)");
        return null;
    }

    private Music loadMusicSmart(String baseNoExt) {
        String[] candidates = new String[] {
            baseNoExt + ".ogg",
            baseNoExt + ".mp3"
        };
        for (String path : candidates) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    Music m = Gdx.audio.newMusic(Gdx.files.internal(path));
                    Gdx.app.log("MUSIC", "Carregado: " + path);
                    return m;
                }
            } catch (Throwable t) {
                // tenta próximo
            }
        }
        Gdx.app.error("MUSIC", "Não encontrei: " + baseNoExt + " (.ogg/.mp3) em assets/");
        return null;
    }

    private final Music.OnCompletionListener nextTrack = new Music.OnCompletionListener() {
        @Override public void onCompletion(Music music) {
            if (!musicEnabled || playlist == null || playlist.length == 0) return;
            currentTrack = (currentTrack + 1) % playlist.length;
            playCurrent();
        }
    };

    private void initMusic() {
        Array<Music> list = new Array<>();

        // Varre pasta /song: adiciona .ogg/.mp3, exceto sons curtos e lobby
        try {
            com.badlogic.gdx.files.FileHandle dir = Gdx.files.internal("song");
            if (dir.exists() && dir.isDirectory()) {
                com.badlogic.gdx.files.FileHandle[] files = dir.list();
                java.util.Arrays.sort(files, (a, b) -> a.name().compareToIgnoreCase(b.name()));
                for (com.badlogic.gdx.files.FileHandle f : files) {
                    if (f.isDirectory()) continue;
                    String ext = f.extension().toLowerCase();
                    if (!ext.equals("ogg") && !ext.equals("mp3")) continue;

                    String base = f.nameWithoutExtension().toLowerCase();
                    if (base.equals("lobby")) continue; // lobby toca no MenuScreen
                    if (base.startsWith("hit") || base.contains("sfx") || base.contains("spawn")) continue;

                    try {
                        Music m = Gdx.audio.newMusic(f);
                        m.setLooping(false);
                        m.setOnCompletionListener(nextTrack);
                        list.add(m);
                        Gdx.app.log("MUSIC", "Playlist + " + f.path());
                    } catch (Throwable t) {
                        Gdx.app.error("MUSIC", "Falhou ao carregar: " + f.path(), t);
                    }
                }
            }
        } catch (Throwable t) {
            Gdx.app.error("MUSIC", "Erro listando a pasta 'song/'", t);
        }

        // fallback se vazio
        if (list.size == 0) {
            Music m1 = loadMusicSmart("song/bridge");
            Music m2 = loadMusicSmart("song/lastCastle");
            if (m1 != null) list.add(m1);
            if (m2 != null) list.add(m2);
        }

        playlist = list.toArray(Music.class);
        if (playlist.length == 0) {
            musicEnabled = false;
            Gdx.app.error("MUSIC", "Nenhuma faixa .ogg/.mp3 encontrada em core/assets/song/");
            return;
        }

        for (Music m : playlist) {
            m.setLooping(false);
            m.setOnCompletionListener(nextTrack);
        }

        applyVolumesToMusic();
        currentTrack = 0;
        playCurrent();
    }

    private void applyVolumesToMusic() {
        float slider = Math.max(0f, Math.min(1f, volumePercent / 100f));
        float vol = slider * MUSIC_GAIN;
        if (playlist != null) for (Music m : playlist) if (m != null) m.setVolume(vol);
    }

    private void playCurrent() {
        if (!musicEnabled || playlist == null || playlist.length == 0) return;
        for (int i = 0; i < playlist.length; i++) {
            if (i != currentTrack && playlist[i].isPlaying()) playlist[i].stop();
        }
        playlist[currentTrack].play();
    }

    private void stepMusicFallback(float dt) {
        if (!musicEnabled || playlist == null || playlist.length == 0) return;
        musicCheckTimer += dt;
        if (musicCheckTimer < 0.25f) return;
        musicCheckTimer = 0f;
        if (paused) return; // não pular música enquanto pause visível
        Music m = playlist[currentTrack];
        if (!m.isPlaying()) {
            currentTrack = (currentTrack + 1) % playlist.length;
            playCurrent();
        }
    }

    private void loadBossFrames() {
        try {
            bossTex = new Texture(Gdx.files.internal(BOSS_PATH));
            bossTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            int cols = 4, rows = 1;
            int fw = bossTex.getWidth() / cols;
            int fh = bossTex.getHeight() / rows;
            TextureRegion[][] grid = TextureRegion.split(bossTex, fw, fh);
            TextureRegion[] frames = new TextureRegion[cols];
            for (int c = 0; c < cols; c++) frames[c] = grid[0][c];
            Enemy.setBossFrames(frames);
        } catch (Exception e) {
            Enemy.setBossFrames(null);
        }
    }

    private PlayerStats makeDefaultStats() {
        PlayerStats s = new PlayerStats();
        s.moveSpeed = 180f;
        s.bulletSpeed = 480f;
        s.fireCooldown = 2f;
        s.projectilesPerShot = 1;
        s.maxHp = 25;
        s.lives = 0;
        s.reviveInvulnTime = 1.5f;
        return s;
    }

    private void resetGame() {
        enemies.clear();
        bullets.clear();
        gems.clear();
        world.reset();
        spawner.reset();
        choosingUpgrade = false;
        nextUpgradeAt = 7;

        killedTotal = 0;
        currentKillTier = 0;
        GameLogic.setKillTier(0);

        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;
        player = new Player(new Vector2(cx, cy), makeDefaultStats());
    }

    // ===== Dificuldade =====
    private void setDifficulty(Difficulty d) {
        difficulty = d;
        switch (difficulty) {
            case FACIL:
                diffIndex = 0;
                diffSpawnMul = 0.85f;
                diffSpeedMul = 0.90f;
                diffDamageMul = 0.60f;
                diffHpMul     = 0.80f;
                break;
            case MEDIANO:
                diffIndex = 1;
                diffSpawnMul = 1.00f;
                diffSpeedMul = 1.00f;
                diffDamageMul = 1.00f;
                diffHpMul     = 1.00f;
                break;
            case DIFICIL:
                diffIndex = 2;
                diffSpawnMul = 1.20f;
                diffSpeedMul = 1.15f;
                diffDamageMul = 1.40f;
                diffHpMul     = 1.35f;
                break;
        }
    }

    private float slider01() { return Math.max(0f, Math.min(1f, volumePercent / 100f)); }

    // volume final do SFX = slider * SFX_GAIN * ganho_específico
    private float sfxVol(float perSfxGain) {
        float v = slider01() * SFX_GAIN * perSfxGain;
        return v > 1f ? 1f : v;
    }

    private void playShootSfx()    { if (sfxShoot     != null) sfxShoot.play(sfxVol(SHOOT_GAIN)); }
    private void playHurtSfx()     { if (sfxHurt      != null) sfxHurt.play(sfxVol(HURT_GAIN)); }
    private void playBossSpawnSfx(){ if (sfxBossSpawn != null) sfxBossSpawn.play(sfxVol(BOSS_GAIN)); }

    private int countBoss(Array<Enemy> arr) {
        int n = 0;
        for (int i = 0; i < arr.size; i++) if (arr.get(i).isBoss) n++;
        return n;
    }

    @Override public void render(float delta) {
        // === ESC só abre o pause. Quando pausado, ESC é tratado em handlePauseInput().
        if (!world.gameOver && !choosingUpgrade && !paused
            && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = true;
            pauseSub = PauseSub.ROOT;
            pauseIndex = 0;
        }

        // Em game over, só aceita ENTER/ESC do menu final
        if (world.gameOver) {
            handleGameOverInput();
        } else {
            if (!choosingUpgrade && !paused) {
                world.tick(delta);

                // aplica tier atual (progressão por abates)
                GameLogic.setKillTier(currentKillTier);

                // cria balas + SFX tiro
                playerSystem.update(
                    player, delta, GameBridge.get().isAgentMode(), enemies,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                    (origin, velocity) -> {
                        bullets.add(new Bullet(new Vector2(origin), new Vector2(velocity)));
                        playShootSfx();
                    }
                );

                updateBullets(delta);
                updateEnemies(delta);

                // ===== boss spawn SFX: detecta aumento no número de bosses
                int bossesBeforeSpawn = countBoss(enemies);

                spawner.update(
                    delta * diffSpawnMul, world,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                    enemies, world.elapsed, diffHpMul
                );

                int bossesAfterSpawn = countBoss(enemies);
                if (bossesAfterSpawn > bossesBeforeSpawn) {
                    Gdx.app.log("SFX", "Boss nasceu → tocando spawnBoss");
                    playBossSpawnSfx();
                }

                // ===== colisões e upgrade por morte de boss
                int enemiesBefore = enemies.size;
                int bossCountBeforeCollisions = countBoss(enemies);
                int hpBefore = player.hp;

                boolean bossDiedByBullet = GameLogic.handleBulletEnemyCollisions(bullets, enemies, gems);
                boolean playerTookTouch   = GameLogic.handleEnemyPlayerCollisions(
                    enemies, player, 40f, world.elapsed, diffDamageMul);

                if (player.hp < hpBefore || playerTookTouch) {
                    playHurtSfx();
                }

                int bossCountAfterCollisions = countBoss(enemies);
                boolean bossDiedByCount = bossCountAfterCollisions < bossCountBeforeCollisions;

                if ((bossDiedByBullet || bossDiedByCount) && !choosingUpgrade) {
                    openUpgradeMenu();
                }

                // contar mortos para subir o tier
                int removedThisFrame = Math.max(0, enemiesBefore - enemies.size);
                if (removedThisFrame > 0) {
                    killedTotal += removedThisFrame;
                    int newTier = killedTotal / 20; // +1 tier a cada 20 mortos
                    if (newTier > currentKillTier) {
                        currentKillTier = newTier;
                        GameLogic.setKillTier(currentKillTier);
                        Gdx.app.log("TIER", "Novo kill tier: " + currentKillTier + " (kills=" + killedTotal + ")");
                    }
                }

                // coleta (score só em gem)
                float pickR = player.r + (gemRegion != null ? gemRegion.getRegionWidth() * gemScale * 0.35f : 6f);
                int gained = GameLogic.pickGems(gems, player, pickR, 1);
                world.score += gained;

                // upgrade por pontos
                if (world.score >= nextUpgradeAt && !choosingUpgrade) {
                    openUpgradeMenu();
                }

                if (player.hp <= 0 && !player.tryRevive(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())) {
                    world.gameOver = true;
                    paused = false;          // garante que o pause não bloqueie inputs
                    choosingUpgrade = false; // fecha overlay de upgrade se estava aberto
                }
            } else if (choosingUpgrade && !paused) {
                handleUpgradeChoiceInput();
            }
        }

        // input de navegação dos menus quando pausado
        if (paused) handlePauseInput();

        // Música: listener + fallback
        stepMusicFallback(delta);

        // ===== DRAW =====
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1) Chão + Gem
        batch.setColor(Color.WHITE);
        batch.begin();
        drawFloorTiled(batch);
        drawGems(batch);
        batch.end();

        // 2) Inimigos + BALAS + Player + HUD
        batch.setColor(Color.WHITE);
        batch.begin();
        for (Bullet b : bullets) b.render(batch, delta);
        for (Enemy e : enemies)  e.render(batch, delta);
        player.render(batch, delta);
        hud.renderTopLeft(batch, player, world.score, GameBridge.get().isAgentMode());
        if (world.gameOver) hud.renderGameOver(batch);
        batch.end();

        // 3) Overlays
        if (choosingUpgrade) drawUpgradeOverlay();
        if (paused)         drawPauseOverlay();
    }

    private void drawFloorTiled(SpriteBatch batch) {
        if (floorRegion == null) return;
        float tileW = floorRegion.getRegionWidth()  * floorScale;
        float tileH = floorRegion.getRegionHeight() * floorScale;
        int cols = (int)Math.ceil(Gdx.graphics.getWidth()  / tileW) + 1;
        int rows = (int)Math.ceil(Gdx.graphics.getHeight() / tileH) + 1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = c * tileW;
                float y = r * tileH;
                batch.draw(floorRegion, x, y, tileW, tileH);
            }
        }
    }

    private void drawGems(SpriteBatch batch) {
        if (gemRegion == null) return;
        float w = gemRegion.getRegionWidth()  * gemScale;
        float h = gemRegion.getRegionHeight() * gemScale;
        float ox = w * 0.5f, oy = h * 0.5f;
        for (Vector2 g : gems) {
            float dx = Math.round(g.x - ox);
            float dy = Math.round(g.y - oy);
            batch.draw(gemRegion, dx, dy, w, h);
        }
    }

    private void updateBullets(float dt) {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(dt);
            if (b.isExpired() || b.isOffscreen(w, h, 10f)) {
                bullets.removeIndex(i);
            }
        }
    }

    private void updateEnemies(float dt) {
        float baseSpd = GameLogic.enemySpeed(world.elapsed, diffSpeedMul);
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            float local = baseSpd * (e.isBoss ? 1.15f : 1f); // boss corre um pouco mais
            e.updateTowards(dt, player.pos, local);
        }
    }

    // ====== LEVEL-UP ======
    private void openUpgradeMenu() {
        choosingUpgrade = true;
        currentChoices = rollThreeWeightedDistinct();
    }

    private void handleUpgradeChoiceInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
            applyUpgrade(currentChoices[0]);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
            applyUpgrade(currentChoices[1]);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
            applyUpgrade(currentChoices[2]);
        }
    }

    private void applyUpgrade(UpgradeOption opt) {
        switch (opt.type) {
            case HP:
                player.stats.maxHp += 10;
                player.hp = Math.min(player.stats.maxHp, player.hp + 10);
                break;
            case MOVE_SPEED:
                player.stats.moveSpeed *= 1.05f;
                break;
            case PROJ_SPEED:
                player.stats.bulletSpeed *= 1.10f;
                break;
            case ATK_SPEED:
                player.stats.fireCooldown = Math.max(0.2f, player.stats.fireCooldown * 0.95f);
                break;
            case EXTRA_BULLET:
                player.stats.projectilesPerShot = Math.min(6, player.stats.projectilesPerShot + 1);
                break;
            case EXTRA_LIFE:
                player.lives += 1;
                break;
        }
        choosingUpgrade = false;
        nextUpgradeAt += 7;
    }

    private UpgradeOption[] rollThreeWeightedDistinct() {
        Array<UpgradeOption> pool = new Array<>(ALL_UPGRADES.length);
        for (UpgradeOption u : ALL_UPGRADES) pool.add(u);

        UpgradeOption[] out = new UpgradeOption[3];
        for (int i = 0; i < 3; i++) {
            int total = 0;
            for (UpgradeOption u : pool) total += u.peso;
            int pick = MathUtils.random(1, Math.max(1, total));
            int acc = 0;
            UpgradeOption chosen = pool.first();
            for (UpgradeOption u : pool) {
                acc += u.peso;
                if (pick <= acc) { chosen = u; break; }
            }
            out[i] = chosen;
            pool.removeValue(chosen, true);
        }
        return out;
    }

    private void drawUpgradeOverlay() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.6f);
        shapes.rect(0, 0, w, h);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.1f);

        String title = "Escolha um upgrade (1/2/3)";
        font.draw(batch, title, 40, h - 40);

        float y = h - 100;
        for (int i = 0; i < 3; i++) {
            UpgradeOption opt = currentChoices[i];
            String line1 = (i + 1) + ") " + opt.titulo;
            String line2 = "   " + opt.desc;

            font.setColor(Color.valueOf("#F6E58D"));
            font.draw(batch, line1, 40, y);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, line2, 40, y - 24);
            y -= 80;
        }
        batch.end();
    }

    private void drawPauseOverlay() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        // fundo escuro
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(0, 0, w, h);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(1.15f);
        font.draw(batch, "PAUSE", 40, h - 40);

        if (pauseSub == PauseSub.ROOT) {
            String[] items = new String[] { "Resume", "Dificuldade", "Opções" };
            float y = h - 110;
            for (int i = 0; i < items.length; i++) {
                boolean sel = (i == pauseIndex);
                font.setColor(sel ? Color.valueOf("#F6E58D") : Color.LIGHT_GRAY);
                font.draw(batch, (sel ? "> " : "  ") + items[i], 60, y);
                y -= 40;
            }
            font.setColor(Color.GRAY);
            font.getData().setScale(0.95f);
            font.draw(batch, "↑/↓ para navegar, ENTER para selecionar", 60, 70);
            font.getData().setScale(1.15f);

        } else if (pauseSub == PauseSub.DIFFICULTY) {
            String[] diffs = new String[] { "Fácil", "Mediano", "Difícil" };
            float y = h - 110;
            for (int i = 0; i < diffs.length; i++) {
                boolean sel = (i == diffIndex);
                font.setColor(sel ? Color.valueOf("#F6E58D") : Color.LIGHT_GRAY);
                font.draw(batch, (sel ? "> " : "  ") + diffs[i], 60, y);
                y -= 40;
            }
            font.setColor(Color.GRAY);
            font.getData().setScale(0.95f);
            font.draw(batch, "↑/↓ para escolher, ENTER confirma | ESC volta", 60, 70);
            font.getData().setScale(1.15f);

        } else if (pauseSub == PauseSub.OPTIONS) {
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, "Volume", 60, h - 110);

            // slider simples 0..100
            int minX = 60, maxX = w - 80;
            int barY = h - 150;
            int barH = 14;
            int barW = maxX - minX;

            // barra de fundo
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(0.25f, 0.25f, 0.25f, 1f);
            shapes.rect(minX, barY, barW, barH);

            // preenchimento
            float pct = volumePercent / 100f;
            shapes.setColor(0.95f, 0.85f, 0.4f, 1f);
            shapes.rect(minX, barY, (int)(barW * pct), barH);
            shapes.end();

            font.setColor(Color.WHITE);
            font.draw(batch, volumePercent + "%", minX, barY - 8);

            font.setColor(Color.GRAY);
            font.getData().setScale(0.95f);
            font.draw(batch, "←/→ ajustam o volume | ENTER/ESC volta", 60, 70);
            font.getData().setScale(1.15f);
        }

        batch.end();
    }

    private void handleGameOverInput() {
        // garanta que nada do pause/upgrade atrapalhe
        paused = false;
        choosingUpgrade = false;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            resetGame();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void handlePauseInput() {
        if (!paused) return;

        if (pauseSub == PauseSub.ROOT) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   pauseIndex = (pauseIndex + 3 - 1) % 3;
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) pauseIndex = (pauseIndex + 1) % 3;

            // ENTER escolhe, ESC fecha o pause
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                if (pauseIndex == 0) { // Resume
                    paused = false;
                } else if (pauseIndex == 1) { // Dificuldade
                    pauseSub = PauseSub.DIFFICULTY;
                } else { // Opções
                    pauseSub = PauseSub.OPTIONS;
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                paused = false; // ESC no root fecha o pause
            }

        } else if (pauseSub == PauseSub.DIFFICULTY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP))   diffIndex = (diffIndex + 3 - 1) % 3;
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) diffIndex = (diffIndex + 1) % 3;

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                setDifficulty(diffIndex == 0 ? Difficulty.FACIL : diffIndex == 1 ? Difficulty.MEDIANO : Difficulty.DIFICIL);
                pauseSub = PauseSub.ROOT;
            }
            // ESC/Back volta pro root (não fecha direto)
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                pauseSub = PauseSub.ROOT;
            }

        } else if (pauseSub == PauseSub.OPTIONS) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  { volumePercent = Math.max(0,   volumePercent - 5); applyVolumesToMusic(); }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) { volumePercent = Math.min(100, volumePercent + 5); applyVolumesToMusic(); }

            // ENTER/ESC/Back volta pro root (não fecha direto)
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                pauseSub = PauseSub.ROOT;
            }
        }
    }

    @Override public void pause() {
        if (playlist != null) {
            for (Music m : playlist) {
                if (m.isPlaying()) m.pause();
            }
        }
    }

    @Override public void resume() {
        if (musicEnabled && playlist != null && playlist.length > 0) {
            playlist[currentTrack].play();
        }
    }

    @Override public void dispose() {
        shapes.dispose();
        batch.dispose();
        hud.dispose();
        if (font != null) font.dispose();
        if (player != null) player.dispose();
        for (Enemy e : enemies) e.dispose();
        if (floorTex != null) floorTex.dispose();
        if (gemTex != null) gemTex.dispose();
        if (bossTex != null) bossTex.dispose();
        Bullet.disposeShared();

        if (playlist != null) {
            for (Music m : playlist) {
                if (m != null) { m.stop(); m.dispose(); }
            }
        }
        if (sfxShoot     != null) sfxShoot.dispose();
        if (sfxHurt      != null) sfxHurt.dispose();
        if (sfxBossSpawn != null) sfxBossSpawn.dispose();
    }
}
