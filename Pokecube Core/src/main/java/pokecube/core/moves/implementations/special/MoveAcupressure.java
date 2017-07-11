package pokecube.core.moves.implementations.special;

import java.util.Random;

import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.moves.MovesUtils;
import pokecube.core.moves.templates.Move_Basic;

public class MoveAcupressure extends Move_Basic
{

    public MoveAcupressure()
    {
        super("acupressure");
    }

    @Override
    public void postAttack(MovePacket packet)
    {
        super.postAttack(packet);
        if (packet.canceled || packet.failed) return;
        Random r = new Random(packet.attacked.getEntityWorld().rand.nextLong());

        int rand = r.nextInt(7);
        packet.attacker.getMoveStats().SELFRAISECOUNTER = (PokecubeMod.core.getConfig().attackCooldown * 4);
        packet.attacker.setAttackCooldown(packet.attacker.getMoveStats().SELFRAISECOUNTER);
        for (int i = 0; i < 8; i++)
        {
            int stat = (rand);
            if (MovesUtils.handleStats2(packet.attacker, packet.attacked, 1 << stat, SHARP)) { return; }
            rand = (rand + 1) % 7;
        }
        MovesUtils.displayEfficiencyMessages(packet.attacker, packet.attacked, -2, 0);
    }
}
