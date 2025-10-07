package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

/** Exemplo simples: monitora # de inimigos e pode pedir spawns. */
public class MonsterAgent extends Agent {
    public static final String NAME = "monster";

    private int alive = 0;
    private boolean finalGateClosed = false; // quando atingir 105/7, para tudo

    @Override
    protected void setup() {
        // Recebe eventos do coordenador
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }
                String ev = msg.getContent();

                if ("ENEMY_SPAWNED".equals(ev)) alive++;
                else if ("ENEMY_KILLED".equals(ev) && alive > 0) alive--;
                else if ("BOSS_SPAWNED".equals(ev)) alive++; // conta o boss na banda
                else if ("GAME_RESET".equals(ev)) { alive = 0; finalGateClosed = false; }

                // Apenas log (estilo "Bombeiro")
                System.out.println("[" + getLocalName() + "] estado: alive=" + alive);
            }
        });

        // Ritmo de checagem para (eventualmente) pedir spawns ao jogo
        addBehaviour(new TickerBehaviour(this, 500) {
            @Override protected void onTick() {
                if (finalGateClosed) return;    // quando o jogo travar spawns, não pede mais
                if (alive < 8) {                // alvo simples de 8 inimigos
                    ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                    req.setContent("CMD|SPAWN_BASIC|count=" + (8 - alive));
                    req.addReceiver(new AID(CoordinatorAgent.NAME, AID.ISLOCALNAME));
                    send(req);
                }
            }
        });
    }
}
