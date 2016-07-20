package pokecube.core.moves.implementations.statusstat;

import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.moves.templates.Move_Basic;

public class MoveGrowl extends Move_Basic
{

    public MoveGrowl()
    {
        super("growl");
    }

    @Override
    public void preAttack(MovePacket packet)
    {
        super.preAttack(packet);
        sound = packet.attacker.getSound();
    }
}
