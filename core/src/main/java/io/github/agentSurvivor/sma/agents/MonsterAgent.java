package io.github.agentSurvivor.sma.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class MonsterAgent extends Agent {

    public static final String NAME = "monster";

    private int alive = 0;
    private int spawnedTotal = 0;
    private int killedTotal = 0;

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }

                String c = msg.getContent();
                if (c == null) return;

                if (c.startsWith("{")) {
                    if (c.contains("\"type\":\"MONSTER_SPAWNED\"")) {
                        onSpawn();
                    } else if (c.contains("\"type\":\"MONSTER_DIED\"")) {
                        onDeath();
                    } else if (c.contains("\"type\":\"GAME_RESET\"")) {
                        onReset();
                    }
                } else {
                    if (c.equals("ENEMY_SPAWNED")) {
                        onSpawn();
                    } else if (c.equals("ENEMY_KILLED")) {
                        onDeath();
                    } else if (c.equals("GAME_RESET")) {
                        onReset();
                    }
                }
            }
        });
    }

    private void onSpawn() {
        alive++;
        spawnedTotal++;
        System.out.println("[monster] NASCEU  | vivos=" + alive +
            " (spawns acumulados=" + spawnedTotal + ")");
    }

    private void onDeath() {
        if (alive > 0) alive--;
        killedTotal++;
        System.out.println("[monster] MORREU  | vivos=" + alive +
            " (mortes acumuladas=" + killedTotal + ")");
    }

    private void onReset() {
        alive = 0;
        spawnedTotal = 0;
        killedTotal = 0;
        System.out.println("[monster] RESET   | vivos=0 (totais zerados)");
    }
}
