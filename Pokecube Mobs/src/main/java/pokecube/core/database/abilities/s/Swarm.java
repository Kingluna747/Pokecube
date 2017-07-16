package pokecube.core.database.abilities.s;

import net.minecraft.entity.EntityLivingBase;
import pokecube.core.database.abilities.Ability;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.utils.PokeType;

public class Swarm extends Ability
{

    @Override
    public void onAgress(IPokemob mob, EntityLivingBase target)
    {
    }

    @Override
    public void onMoveUse(IPokemob mob, MovePacket move)
    {

        if (!move.pre) return;
        if (mob == move.attacker && move.attackType == PokeType.getType("bug")
                && ((EntityLivingBase) mob).getHealth() < ((EntityLivingBase) mob).getMaxHealth() / 3)
        {
            move.PWR *= 1.5;
        }
    }

    @Override
    public void onUpdate(IPokemob mob)
    {
    }

}
