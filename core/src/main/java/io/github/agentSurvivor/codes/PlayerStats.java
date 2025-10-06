package io.github.agentSurvivor.codes;

public class PlayerStats {
    // vida / vidas
    public int maxHp = 25;
    public int lives = 2;
    public float reviveInvulnTime = 1.5f;

    // movimento
    public float moveSpeed = 180f;

    // tiro
    public float fireCooldown = 2.0f;     // cooldown do disparo (segundos)
    public float bulletSpeed  = 420f;      // velocidade da bala
    public int   projectilesPerShot = 1;   // quantas balas por disparo (1 = tiro simples)

    public static PlayerStats defaultStats() {
        return new PlayerStats();
    }

    // Helpers opcionais para upgrades
    public PlayerStats withMoveSpeed(float v)         { this.moveSpeed = v; return this; }
    public PlayerStats withFireCooldown(float v)      { this.fireCooldown = v; return this; }
    public PlayerStats withBulletSpeed(float v)       { this.bulletSpeed = v; return this; }
    public PlayerStats withProjectiles(int n)         { this.projectilesPerShot = n; return this; }
    public PlayerStats withMaxHp(int v)               { this.maxHp = v; return this; }
    public PlayerStats withLives(int v)               { this.lives = v; return this; }
    public PlayerStats withReviveInvuln(float v)      { this.reviveInvulnTime = v; return this; }
}
