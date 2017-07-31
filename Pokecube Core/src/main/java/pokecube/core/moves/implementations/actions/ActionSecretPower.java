package pokecube.core.moves.implementations.actions;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import pokecube.core.interfaces.IMoveAction;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;

public class ActionSecretPower implements IMoveAction
{
    public static Map<UUID, Vector4> pendingBaseLocations = Maps.newHashMap();

    public ActionSecretPower()
    {
    }

    @Override
    public boolean applyEffect(IPokemob attacker, Vector3 location)
    {
        if (attacker.getPokemonAIState(IMoveConstants.ANGRY)) return false;
        if (!(attacker.getPokemonOwner() instanceof EntityPlayerMP)) return false;
        long time = attacker.getEntity().getEntityData().getLong("lastAttackTick");
        if (time + (20 * 3) > attacker.getEntity().getEntityWorld().getTotalWorldTime()) return false;
        EntityPlayerMP owner = (EntityPlayerMP) attacker.getPokemonOwner();
        IBlockState state = location.getBlockState(owner.getEntityWorld());
        if (!(PokecubeTerrainChecker.isTerrain(state) || PokecubeTerrainChecker.isWood(state)))
        {
            TextComponentTranslation message = new TextComponentTranslation("pokemob.createbase.deny.wrongloc");
            owner.sendMessage(message);
            return false;
        }
        BreakEvent evt = new BreakEvent(owner.getEntityWorld(), location.getPos(),
                location.getBlockState(owner.getEntityWorld()), owner);
        MinecraftForge.EVENT_BUS.post(evt);
        if (evt.isCanceled())
        {
            TextComponentTranslation message = new TextComponentTranslation("pokemob.createbase.deny.noperms");
            owner.sendMessage(message);
            return false;
        }
        pendingBaseLocations.put(owner.getUniqueID(), new Vector4(location.x, location.y, location.z, owner.dimension));
        TextComponentTranslation message = new TextComponentTranslation("pokemob.createbase.confirm",
                location.set(location.getPos()));
        message.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/pokebase confirm " + owner.posX + " " + owner.posY + " " + owner.posZ));
        owner.sendMessage(message);
        return true;
    }

    @Override
    public String getMoveName()
    {
        return "secretpower";
    }
}
