package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class MusicAgent extends Agent {
    public static final String NAME = "musica";

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }
                String ev = msg.getContent();
                if (ev == null) return;

                System.out.println("[music] recebeu: " + ev);

                boolean isJson = ev.startsWith("{");

                if ((isJson && ev.contains("\"type\":\"GAME_RESET\"")) || "GAME_RESET".equals(ev)) {
                    sendToGame("MUSIC|stop");
                    return;
                }

                if ((isJson && ev.contains("\"type\":\"BOSS_SPAWNED\"")) || "BOSS_SPAWNED".equals(ev)) {
                    sendToGame("MUSIC|play|finalBoss");
                    return;
                }

                if ((isJson && ev.contains("\"type\":\"PLAYER_DIED\"")) || "PLAYER_DIED".equals(ev)) {
                    sendToGame("MUSIC|sequence");
                    return;
                }

                if (isJson && ev.contains("\"type\":\"MUSIC_STARTED\"")) {
                    System.out.println("[music] começou a tocar (confirmação do jogo): " + ev);
                    return;
                }
                if (isJson && ev.contains("\"type\":\"MUSIC_STOPPED\"")) {
                    System.out.println("[music] música parada (confirmação do jogo): " + ev);
                }
            }
        });
    }

    private void sendToGame(String cmd) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setOntology(CoordinatorAgent.ONT_CMD);
        out.setContent(cmd);
        out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
        send(out);
    }
}
