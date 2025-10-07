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

// >>> PONTE COM O SMA (JADE)
import io.github.agentSurvivor.sma.agents.SmaGateway;

public class GameScreen extends ScreenAdapter {
    private final GameMain game;

    private boolean inFinalBossSequence = false;

    private boolean finalPrereqsMet = false;

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
    private static String BOSS_PATH = "sprite_mob/boss.png";
    private Texture bossTex;

    // ===== Música =====
    private Music[] playlist;
    private int currentTrack = 0;
    private boolean musicEnabled = true;
    private float musicCheckTimer = 0f;

    // Música do boss final
    private Music finalBossMusic;
    private boolean finalBossMusicPlaying = false;

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
    private int nextUpgradeAt = 5;

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

    // multiplicador vindo do SMA (ex.: SET_SPAWN_RATE)
    private float smaSpawnMul   = 1f;

    // --- progressão por abates ---
    private int killedTotal = 0;
    private int bossKilledTotal = 0;
    private int currentKillTier = 0;

    // Controle do boss especial
    private boolean superBossSpawned = false;
    private boolean bloqueiaSpawns = false; // bloqueia spawns normais
    private static final String SUPER_BOSS_PATH = "sprite_mob/finalBoss.png";
    private float bossFinalSpawnTimer = -1f; // < 0 = não iniciou

    private Music bridgeMusic, lastCastleMusic;
    private boolean usingBridgeChain = false;
    private boolean bridgeWasPlaying = false, lastCastleWasPlaying = false;

    public GameScreen(GameMain game) { this.game = game; }

    @Override public void show() {
        io.github.agentSurvivor.sma.agents.SmaStarter.startIfNeeded();
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

        // Carrega música do boss final
        finalBossMusic = loadMusicSmart("song/finalBoss");
        if (finalBossMusic != null) {
            finalBossMusic.setLooping(true);
            finalBossMusic.setVolume(slider01() * MUSIC_GAIN);
        }

        setDifficulty(difficulty);
        resetGame();
    }

    private void stopFinalBossMusic() {
        if (finalBossMusic != null && finalBossMusicPlaying) {
            finalBossMusic.stop();
            finalBossMusicPlaying = false;
        }
    }

    private void stopBridgeChain() {
        usingBridgeChain = false;
        if (bridgeMusic != null && bridgeMusic.isPlaying()) bridgeMusic.stop();
        if (lastCastleMusic != null && lastCastleMusic.isPlaying()) lastCastleMusic.stop();
    }

    private void startBridgeChain() {
        // mata tudo que possa estar tocando
        if (playlist != null) for (Music m : playlist) if (m.isPlaying()) m.stop();
        stopFinalBossMusic();

        if (bridgeMusic == null) bridgeMusic = loadMusicSmart("song/bridge");
        if (lastCastleMusic == null) lastCastleMusic = loadMusicSmart("song/lastCastle");

        float vol = slider01() * MUSIC_GAIN;
        if (bridgeMusic != null) { bridgeMusic.setLooping(false); bridgeMusic.setVolume(vol); }
        if (lastCastleMusic != null){ lastCastleMusic.setLooping(false); lastCastleMusic.setVolume(vol); }

        usingBridgeChain = true;

        if (bridgeMusic != null) {
            bridgeMusic.setOnCompletionListener(new Music.OnCompletionListener() {
                @Override public void onCompletion(Music music) {
                    if (usingBridgeChain && lastCastleMusic != null) {
                        lastCastleMusic.play();
                    }
                }
            });
            bridgeMusic.play();
        } else if (lastCastleMusic != null) {
            lastCastleMusic.play();
        } else {
            // não achou nenhuma: volta pra playlist normal
            usingBridgeChain = false;
            playCurrent();
        }
    }

    private Enemy findAliveBoss() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isBoss && e.hp > 0) return e;
        }
        return null;
    }

    private void drawBossHealthBar(Enemy boss) {
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        float barW = sw * 0.6f;
        float barH = 18f;
        float x = (sw - barW) * 0.5f;
        float y = sh - 42f; // margem do topo

        // Enemy precisa ter 'maxHp' público para esta barra (como no ajuste anterior)
        float pct = boss.maxHp > 0 ? Math.max(0f, Math.min(1f, boss.hp / (float) boss.maxHp)) : 0f;

        // fundo + preenchimento
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.55f); // fundo escuro
        shapes.rect(x, y, barW, barH);
        shapes.setColor(0.85f, 0.2f, 0.25f, 1f); // vermelho da vida
        shapes.rect(x, y, barW * pct, barH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // texto (BOSS 83/120) centralizado acima da barra
        batch.begin();
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
        String label = "BOSS " + Math.max(0, boss.hp) + " / " + boss.maxHp;
        com.badlogic.gdx.graphics.g2d.GlyphLayout gl =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, label);
        float tx = x + (barW - gl.width) * 0.5f;
        float ty = y + barH + 16f;
        font.draw(batch, label, tx, ty);
        batch.end();
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
        if (bridgeMusic != null) bridgeMusic.setVolume(vol);
        if (lastCastleMusic != null) lastCastleMusic.setVolume(vol);
        if (finalBossMusic != null) finalBossMusic.setVolume(vol);
    }

    private void playCurrent() {
        if (!musicEnabled || playlist == null || playlist.length == 0) return;
        for (int i = 0; i < playlist.length; i++) {
            if (i != currentTrack && playlist[i].isPlaying()) playlist[i].stop();
        }
        playlist[currentTrack].play();
    }

    private void stepMusicFallback(float dt) {
        if (usingBridgeChain || inFinalBossSequence) return; // NÃO religar playlist nesse período
        if (!musicEnabled || playlist == null || playlist.length == 0) return;
        musicCheckTimer += dt;
        if (musicCheckTimer < 0.25f) return;
        musicCheckTimer = 0f;
        if (paused) return;
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
        finalPrereqsMet = false;
        inFinalBossSequence = false;
        enemies.clear();
        bullets.clear();
        gems.clear();
        world.reset();
        spawner.reset();
        choosingUpgrade = false;
        nextUpgradeAt = 5;

        killedTotal = 0;
        bossKilledTotal = 0;
        currentKillTier = 0;
        GameLogic.setKillTier(0);

        superBossSpawned = false;
        bloqueiaSpawns = false; // libera spawns ao resetar

        float cx = Gdx.graphics.getWidth()  / 2f;
        float cy = Gdx.graphics.getHeight() / 2f;
        player = new Player(new Vector2(cx, cy), makeDefaultStats());

        superBossSpawned = false;
        bloqueiaSpawns = false;
        bossFinalSpawnTimer = -1f;

        stopFinalBossMusic();
        stopBridgeChain();
        currentTrack = 0;
        playCurrent();

        // >>> Evento para o SMA
        emit("{\"type\":\"GAME_RESET\"}");
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

    private void drawCountersTopRight(SpriteBatch batch) {
        final float margin = 20f;
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();

        String s1 = "Monstros: " + killedTotal + " / 105";
        String s2 = "Bosses: "   + bossKilledTotal + " / 7";

        com.badlogic.gdx.graphics.g2d.GlyphLayout l1 =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, s1);
        com.badlogic.gdx.graphics.g2d.GlyphLayout l2 =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout(font, s2);

        float x = sw - margin - Math.max(l1.width, l2.width);
        float y1 = sh - margin;
        float y2 = y1 - (l1.height + 6f);

        font.setColor(Color.WHITE);
        font.draw(batch, s1, x, y1);
        font.draw(batch, s2, x, y2);
    }

    @Override public void render(float delta) {
        // ESC abre pause (se não estiver em upgrade/game over)
        if (!paused && !choosingUpgrade && !world.gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
                paused = true;
                pauseSub = PauseSub.ROOT;
                pauseIndex = 0;
                return; // evita processar lógica deste frame
            }
        }

        // Game Over: só trata enter/esc
        if (world.gameOver) {
            handleGameOverInput();
        } else {
            if (!choosingUpgrade && !paused) {
                world.tick(delta);
                GameLogic.setKillTier(currentKillTier);

                // tiros
                playerSystem.update(
                    player, delta, GameBridge.get().isAgentMode(), enemies,
                    Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                    (origin, velocity) -> {
                        bullets.add(new Bullet(new Vector2(origin), new Vector2(velocity)));
                        playShootSfx();
                        // >>> Evento para o SMA
                        emit("{\"type\":\"PLAYER_SHOT\"}");
                    }
                );

                updateBullets(delta);
                updateEnemies(delta);

                // ---- antes de possíveis spawns
                int enemiesBeforeSpawns = enemies.size;
                int bossesBeforeSpawn = countBoss(enemies);

                // trava spawns quando bater os limites
                if (!bloqueiaSpawns && killedTotal >= 105) {
                    bloqueiaSpawns = true;
                    finalPrereqsMet = true; // libera etapa final (seu fluxo de boss final usa essa flag)
                    spawner.reset();
                }

                // Apenas UMA chamada ao spawner (já com multiplicador do SMA)
                if (!bloqueiaSpawns) {
                    float spawnMul = diffSpawnMul * smaSpawnMul;
                    spawner.update(
                        delta * spawnMul, world,
                        Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
                        enemies, world.elapsed, diffHpMul
                    );
                }

                // novos spawns que aconteceram neste frame
                int spawnedNow = Math.max(0, enemies.size - enemiesBeforeSpawns);
                for (int i = 0; i < spawnedNow; i++) emit("{\"type\":\"MONSTER_SPAWNED\"}");

                int bossesAfterSpawn = countBoss(enemies);
                if (bossesAfterSpawn > bossesBeforeSpawn) {
                    playBossSpawnSfx();
                    for (int i = 0; i < (bossesAfterSpawn - bossesBeforeSpawn); i++)
                        emit("{\"type\":\"BOSS_SPAWNED\"}");
                }

                int enemiesBefore = enemies.size; // para mortes após colisões
                int bossCountBeforeCollisions = countBoss(enemies);
                int hpBefore = player.hp;

                boolean bossDiedByBullet = GameLogic.handleBulletEnemyCollisions(bullets, enemies, gems);
                boolean playerTookTouch  = GameLogic.handleEnemyPlayerCollisions(
                    enemies, player, 40f, world.elapsed, diffDamageMul);

                if (player.hp < hpBefore || playerTookTouch) playHurtSfx();

                int bossCountAfterCollisions = countBoss(enemies);
                if (bossCountAfterCollisions < bossCountBeforeCollisions) {
                    bossKilledTotal += (bossCountBeforeCollisions - bossCountAfterCollisions);
                }

                // contabiliza mortos
                int removedThisFrame = Math.max(0, enemiesBefore - enemies.size);
                if (removedThisFrame > 0) {
                    killedTotal += removedThisFrame;
                    for (int i = 0; i < removedThisFrame; i++) {
                        emit("{\"type\":\"MONSTER_DIED\"}");
                        emit("{\"type\":\"PLAYER_KILL\"}");
                    }
                    int newTier = killedTotal / 20;
                    if (newTier > currentKillTier) {
                        currentKillTier = newTier;
                        GameLogic.setKillTier(currentKillTier);
                    }
                }

                // spawn do boss final (limites atingidos e tela limpa)
                if (!superBossSpawned
                    && killedTotal >= 105
                    && bossKilledTotal >= 7
                    && enemies.size == 0) {

                    if (bossFinalSpawnTimer < 0f) { // inicia só a espera
                        // Silencia tudo e impede o fallback de religar
                        if (playlist != null) {
                            for (Music m : playlist) if (m.isPlaying()) m.stop();
                        }
                        stopBridgeChain();          // garante que a sequência bridge/lastCastle não esteja tocando
                        inFinalBossSequence = true; // trava o fallback

                        bossFinalSpawnTimer = 5f;   // aguarda antes de spawnar
                        bloqueiaSpawns = true;
                    }
                }

                // Quando o timer chega a zero, spawna o SuperBoss
                if (!superBossSpawned && bossFinalSpawnTimer >= 0f) {
                    bossFinalSpawnTimer -= delta;
                    if (bossFinalSpawnTimer <= 0f) {
                        // música do boss pode ser comandada pelo MusicAgent; aqui ligamos só se ainda não estiver
                        if (finalBossMusic != null && !finalBossMusicPlaying) {
                            // Segurança: pare qualquer música que porventura tenha voltado
                            if (playlist != null) {
                                for (Music m : playlist) if (m.isPlaying()) m.stop();
                            }
                            stopBridgeChain();

                            finalBossMusic.play();
                            finalBossMusicPlaying = true;
                        }
                        Enemy superBoss = Enemy.makeSuperBoss(
                            new Vector2(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f),
                            SUPER_BOSS_PATH
                        );
                        enemies.add(superBoss);
                        superBossSpawned = true;

                        emit("{\"type\":\"MONSTER_SPAWNED\"}");
                        emit("{\"type\":\"BOSS_SPAWNED\"}");
                    }
                }

                if ((bossDiedByBullet || bossCountAfterCollisions < bossCountBeforeCollisions) && !choosingUpgrade) {
                    openUpgradeMenu();
                }

                // coleta gem/score e upgrades por pontos
                float pickR = player.r + (gemRegion != null ? gemRegion.getRegionWidth() * gemScale * 0.35f : 6f);
                int gained = GameLogic.pickGems(gems, player, pickR, 1);
                if (gained > 0) {
                    world.score += gained;
                    emit("{\"type\":\"PLAYER_SCORED\",\"score\":" + world.score + "}");
                }

                if (world.score >= nextUpgradeAt && !choosingUpgrade) {
                    openUpgradeMenu();
                }

                // morreu de vez
                if (player.hp <= 0 && !player.tryRevive(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())) {
                    world.gameOver = true;
                    paused = false;
                    choosingUpgrade = false;

                    // evento + comportamento padrão (cadeia bridge → lastCastle)
                    emit("{\"type\":\"PLAYER_DIED\"}");

                    // padrão local (caso não haja MusicAgent rodando)
                    stopFinalBossMusic();
                    startBridgeChain();
                }

            } else if (choosingUpgrade && !paused) {
                handleUpgradeChoiceInput();
            }
        }

        // comandos vindos do SMA (podem chegar a qualquer momento)
        consumeSmaCommands();

        // input de navegação do pause
        if (paused) handlePauseInput();

        // Música normal só se não for a do boss final
        if (!finalBossMusicPlaying) stepMusicFallback(delta);

        // ====== DRAW ======
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setColor(Color.WHITE);
        batch.begin();

        // fundo e pickups
        drawFloorTiled(batch);
        drawGems(batch);

        // contadores (sempre dentro do begin!)
        drawCountersTopRight(batch);

        // entidades + HUD
        for (Bullet b : bullets) b.render(batch, delta);
        for (Enemy e : enemies)  e.render(batch, delta);
        player.render(batch, delta);
        hud.renderTopLeft(batch, player, world.score, GameBridge.get().isAgentMode());
        if (world.gameOver) hud.renderGameOver(batch);

        batch.end();

        // overlays
        if (choosingUpgrade) drawUpgradeOverlay();
        if (paused)         drawPauseOverlay();

        Enemy bossForBar = findAliveBoss();
        if (bossForBar != null) drawBossHealthBar(bossForBar);
    }

    // ======= CONSUMO DE COMANDOS DO SMA =======
    private void consumeSmaCommands() {
        String cmd;
        while ((cmd = SmaGateway.pollCommandForGame()) != null) {

            if (cmd.contains("\"cmd\":\"REQUEST_SPAWN\"")) {
                if (bloqueiaSpawns) {
                    // ignorar pedidos de spawn quando travado (após 105 kills ou pós-boss final)
                    continue;
                }
                int count = parseInt(cmd, "count", 1);
                for (int i = 0; i < count; i++) {
                    float x = MathUtils.random(30f, Gdx.graphics.getWidth() - 30f);
                    float y = MathUtils.random(30f, Gdx.graphics.getHeight() - 30f);
                    enemies.add(new Enemy(new Vector2(x, y)));
                    emit("{\"type\":\"MONSTER_SPAWNED\"}");
                }
            } else if (cmd.contains("\"cmd\":\"SET_SPAWN_RATE\"")) {
                float rate = parseFloat(cmd, "rate", 1f);
                smaSpawnMul = Math.max(0.2f, Math.min(rate, 3f));

            } else if (cmd.contains("\"cmd\":\"MUSIC\"")) {
                if (cmd.contains("\"action\":\"play\"") && cmd.contains("\"finalBoss\"")) {
                    if (finalBossMusic != null && !finalBossMusicPlaying) {
                        stopBridgeChain();
                        finalBossMusic.play();
                        finalBossMusicPlaying = true;
                    }
                } else if (cmd.contains("\"action\":\"sequence\"")) {
                    startBridgeChain();
                } else if (cmd.contains("\"action\":\"resume-playlist\"")) {
                    stopBridgeChain();
                    if (!finalBossMusicPlaying) playCurrent();
                } else if (cmd.contains("\"action\":\"stop\"")) {
                    stopBridgeChain();
                    stopFinalBossMusic();
                }
            }
        }
    }

    // ===== Utilidades simples de parsing e emissão =====
    private static int parseInt(String json, String key, int defVal) {
        try {
            int ki = json.indexOf('\"' + key + '\"');
            if (ki < 0) return defVal;
            int ci = json.indexOf(':', ki);
            if (ci < 0) return defVal;
            int j = ci + 1;
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
            int k = j;
            while (k < json.length() && (Character.isDigit(json.charAt(k)) || json.charAt(k) == '-')) k++;
            return Integer.parseInt(json.substring(j, k));
        } catch (Exception e) { return defVal; }
    }

    private static float parseFloat(String json, String key, float defVal) {
        try {
            int ki = json.indexOf('\"' + key + '\"');
            if (ki < 0) return defVal;
            int ci = json.indexOf(':', ki);
            if (ci < 0) return defVal;
            int j = ci + 1;
            while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
            int k = j;
            while (k < json.length() && "0123456789+-.eE".indexOf(json.charAt(k)) >= 0) k++;
            return Float.parseFloat(json.substring(j, k));
        } catch (Exception e) { return defVal; }
    }

    private static void emit(String json) {
        SmaGateway.emitEventFromGame(json);
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
        nextUpgradeAt += 5;
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

    @Override public void hide() {
        // Para QUALQUER música do jogo quando sair desta tela
        stopBridgeChain();        // pára bridge/lastCastle se estiverem tocando
        stopFinalBossMusic();     // pára a trilha do boss final
        if (playlist != null) {
            for (Music m : playlist) {
                if (m.isPlaying()) m.stop();
            }
        }
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
            // Pare a música do jogo antes de ir para o lobby
            if (playlist != null) {
                for (Music m : playlist) {
                    if (m.isPlaying()) m.stop();
                }
            }
            // Pare a música do boss final também
            if (finalBossMusic != null && finalBossMusic.isPlaying()) {
                finalBossMusic.stop();
                finalBossMusicPlaying = false;
            }
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
        if (finalBossMusic != null && finalBossMusic.isPlaying()) {
            finalBossMusic.pause();
        }
        if (usingBridgeChain) {
            bridgeWasPlaying = bridgeMusic != null && bridgeMusic.isPlaying();
            lastCastleWasPlaying = lastCastleMusic != null && lastCastleMusic.isPlaying();
            if (bridgeMusic != null && bridgeMusic.isPlaying()) bridgeMusic.pause();
            if (lastCastleMusic != null && lastCastleMusic.isPlaying()) lastCastleMusic.pause();
        }
    }

    @Override public void resume() {
        if (usingBridgeChain) {
            if (bridgeWasPlaying && bridgeMusic != null) bridgeMusic.play();
            if (lastCastleWasPlaying && lastCastleMusic != null) lastCastleMusic.play();
            return; // não mexe na playlist quando cadeia ativa
        }
        if (musicEnabled && playlist != null && playlist.length > 0 && !finalBossMusicPlaying) {
            playlist[currentTrack].play();
        }
        if (finalBossMusic != null && finalBossMusicPlaying) {
            finalBossMusic.play();
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
        if (finalBossMusic != null) {
            finalBossMusic.stop();
            finalBossMusic.dispose();
        }
    }
}
