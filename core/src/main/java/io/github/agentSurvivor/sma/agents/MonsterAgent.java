package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class MonsterAgent extends Agent {
    public static final String NAME = "mobs";

    // Estado observado
    private int alive = 0;
    private int spawnedTotal = 0;
    private int killedTotal = 0;

    private int targetAlive = 6;
    private long lastRequestMs = 0;
    private long minIntervalMs = 600;

    // Do snapshot do mundo
    private boolean spawnLocked = false;
    private int difficultyIdx = 0;

    private long requestCooldownMs = 900L;

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }

                String c = msg.getContent();
                if (c == null) return;

                if (c.startsWith("{")) {
                    if (c.contains("\"type\":\"MONSTER_SPAWNED\"")) onSpawn();
                    else if (c.contains("\"type\":\"MONSTER_DIED\"")) onDeath();
                    else if (c.contains("\"type\":\"GAME_RESET\""))   onReset();
                    else if (c.contains("\"type\":\"WORLD_SNAPSHOT\"")) parseSnapshot(c);
                } else {
                    switch (c) {
                        case "ENEMY_SPAWNED": onSpawn(); break;
                        case "ENEMY_KILLED":  onDeath(); break;
                        case "GAME_RESET":    onReset(); break;
                    }
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 500) {
            @Override protected void onTick() {
                maybeRequestSpawn();
            }
        });
    }

    private void onSpawn() {
        alive++;
        spawnedTotal++;
        System.out.println("[monster] NASCEU  | vivos=" + alive + " (spawns acumulados=" + spawnedTotal + ")");
    }

    private void onDeath() {
        if (alive > 0) alive--;
        killedTotal++;
        System.out.println("[monster] MORREU  | vivos=" + alive + " (mortes acumuladas=" + killedTotal + ")");
    }

    private void onReset() {
        alive = 0; spawnedTotal = 0; killedTotal = 0;
        System.out.println("[monster] RESET   | vivos=0 (totais zerados)");
    }

    private void parseSnapshot(String json) {
        spawnLocked = json.contains("\"spawnLocked\":true");
        int i = json.indexOf("\"difficulty\":");
        if (i >= 0) {
            int j = i + "\"difficulty\":".length();
            int k = j;
            while (k < json.length() && Character.isDigit(json.charAt(k))) k++;
            try { difficultyIdx = Integer.parseInt(json.substring(j, k)); } catch (Exception ignore) {}
        }
    }

    private void maybeRequestSpawn() {
        long now = System.currentTimeMillis();
        if (now - lastRequestMs < minIntervalMs) return;

        int need = targetAlive - alive;
        if (need <= 0) return;

        jade.lang.acl.ACLMessage cmd = new jade.lang.acl.ACLMessage(jade.lang.acl.ACLMessage.INFORM);
        cmd.addReceiver(new jade.core.AID(GameBridgeAgent.NAME, jade.core.AID.ISLOCALNAME));
        cmd.setContent("{\"cmd\":\"REQUEST_SPAWN\",\"count\":"+need+"}");
        send(cmd);

        System.out.println("[monster] solicitou spawn de " + need + " inimigos (alvo="+targetAlive+")");
        lastRequestMs = now;
    }

    private void sendSpawnRequest(int count) {
        String payload = "{\"cmd\":\"REQUEST_SPAWN\",\"count\":" + count + "}";
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setContent(payload);
        out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
        send(out);

        System.out.println("[monster] pediu spawn: count=" + count);
    }
}
