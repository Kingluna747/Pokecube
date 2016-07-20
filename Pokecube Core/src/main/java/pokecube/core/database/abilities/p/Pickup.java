package pokecube.core.database.abilities.p;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import pokecube.core.PokecubeItems;
import pokecube.core.database.abilities.Ability;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;

public class Pickup extends Ability
{

    @Override
    public void onAgress(IPokemob mob, EntityLivingBase target)
    {
    }

    @Override
    public void onMoveUse(IPokemob mob, MovePacket move)
    {
    }

    @Override
    public void onUpdate(IPokemob mob)
    {
        EntityLivingBase poke = (EntityLivingBase) mob;
        if (poke.ticksExisted % 200 == 0 && Math.random() < 0.1)
        {
            if (poke.getHeldItemMainhand() == null)
            {
                List<?> items = new ArrayList<Object>(PokecubeItems.heldItems);
                Collections.shuffle(items);
                ItemStack item = (ItemStack) items.get(0);

                if (item != null) poke.setHeldItem(EnumHand.MAIN_HAND, item.copy());
                ;
            }
        }
    }

}
