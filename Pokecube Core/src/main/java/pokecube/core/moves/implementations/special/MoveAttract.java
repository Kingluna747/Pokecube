package pokecube.core.moves.implementations.special;

import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.moves.templates.Move_Basic;

public class MoveAttract extends Move_Basic
{

    public MoveAttract()
    {
        super("attract");
    }

    @Override
    public void onAttack(MovePacket packet)
    {
        packet.infatuateTarget = true;
        super.onAttack(packet);
    }
}
