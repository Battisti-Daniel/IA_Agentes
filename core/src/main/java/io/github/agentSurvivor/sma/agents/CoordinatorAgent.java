package io.github.agentSurvivor.sma.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class CoordinatorAgent extends Agent {

    public static final String NAME = "Coordenador dos eventos";

    // >>> Constantes usadas por outros agentes (faltavam e causavam o erro)
    public static final String ONT_EVENT = "ONT_EVENT";
    public static final String ONT_CMD   = "ONT_CMD";

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            @Override public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }

                String content = msg.getContent();
                if (content == null) return;

                // Logs "bonitos" para o console (modo relatório)
                if (content.startsWith("{\"type\":\"AGENT_DECISION\"")) {
                    System.out.println("[agent] decisão " + content);
                } else if (content.startsWith("{\"type\":\"WORLD_SNAPSHOT\"")) {
                    System.out.println("[world] " + content);
                }else if (content.contains("\"type\":\"MONSTER_SPAWNED\"")) {
                    System.out.println("[Coordenador] evento: NASCIMENTO de monstro");
                }
                else if (content.contains("\"type\":\"MONSTER_DIED\"")) {
                    System.out.println("[Coordenador] evento: MORTE de monstro");
                }
                else {
                    System.out.println("[" + getLocalName() + "] recebeu: " + content +
                        " (de " + msg.getSender().getLocalName() + ")");
                }

                // Tradução básica de JSON → tokens simples (opcional)
                String token = normalize(content);

                // Fan-out para agentes especializados
                fanout(token, MusicAgent.NAME, MonsterAgent.NAME);

                // Exemplos de efeitos imediatos:
                if (token.startsWith("PLAYER_DIED")) {
                    // sugere sequência "bridge→lastCastle"
                    sendCmdToGame("MUSIC|sequence");
                }
            }
        });
    }

    private void fanout(String content, String... receivers) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setOntology(ONT_EVENT); // opcional: marcação de ontologia
        out.setContent(content);
        for (String r : receivers) out.addReceiver(new AID(r, AID.ISLOCALNAME));
        send(out);
    }

    private void sendCmdToGame(String cmd) {
        ACLMessage out = new ACLMessage(ACLMessage.INFORM);
        out.setOntology(ONT_CMD); // opcional: marcação de ontologia
        out.setContent(cmd);
        out.addReceiver(new AID(GameBridgeAgent.NAME, AID.ISLOCALNAME));
        send(out);
    }

    private String normalize(String c) {
        if (!c.startsWith("{")) return c; // já é token simples
        if (c.contains("\"type\":\"MONSTER_SPAWNED\"")) return "ENEMY_SPAWNED";
        if (c.contains("\"type\":\"MONSTER_DIED\""))   return "ENEMY_KILLED";
        if (c.contains("\"type\":\"BOSS_SPAWNED\""))   return "BOSS_SPAWNED";
        if (c.contains("\"type\":\"PLAYER_DIED\""))    return "PLAYER_DIED";
        if (c.contains("\"type\":\"PLAYER_SHOT\""))    return "PLAYER_SHOT";
        if (c.contains("\"type\":\"PLAYER_SCORED\""))  return "PLAYER_SCORED";
        if (c.contains("\"type\":\"GAME_RESET\""))     return "GAME_RESET";
        if (c.contains("\"type\":\"AGENT_DECISION\"")) return "AGENT_DECISION";
        if (c.contains("\"type\":\"WORLD_SNAPSHOT\"")) return "WORLD_SNAPSHOT";
        if (c.contains("\"type\":\"MUSIC_STARTED\"")) return "MUSIC_STARTED";
        if (c.contains("\"type\":\"MUSIC_STOPPED\"")) return "MUSIC_STOPPED";
        return c;
    }
}
