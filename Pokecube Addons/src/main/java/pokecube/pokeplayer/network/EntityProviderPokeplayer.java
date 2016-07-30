package pokecube.pokeplayer.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import pokecube.core.network.EntityProvider;
import pokecube.pokeplayer.PokePlayer;

public class EntityProviderPokeplayer extends EntityProvider
{

    public EntityProviderPokeplayer(EntityProvider defaults)
    {
        super(defaults);
    }

    @Override
    public Entity getEntity(World world, int id, boolean expectsPokemob)
    {
        Entity ret = world.getEntityByID(id);
        if (expectsPokemob
                && ret instanceof EntityPlayer) { return (Entity) PokePlayer.PROXY.getPokemob((EntityPlayer) ret); }
        return super.getEntity(world, id, expectsPokemob);
    }

}
