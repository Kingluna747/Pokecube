package pokecube.core.interfaces.pokemob.commandhandlers;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Sets;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import pokecube.core.events.handlers.EventsHandler;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.handlers.playerdata.PokecubePlayerData;
import pokecube.core.interfaces.IMoveNames;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.IHasCommands.IMobCommandHandler;
import pokecube.core.moves.MovesUtils;
import pokecube.core.utils.PokecubeSerializer.TeleDest;
import thut.api.entity.Transporter;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.core.common.commands.CommandTools;
import thut.lib.CompatWrapper;

public class TeleportHandler implements IMobCommandHandler
{
    public static float                MINDIST        = 5;
    public static final Set<Integer>   invalidDests   = Sets.newHashSet();

    public static Predicate<ItemStack> VALIDTELEITEMS = new Predicate<ItemStack>()
                                                      {
                                                          @Override
                                                          public boolean test(ItemStack t)
                                                          {
                                                              return t.getItem() == Items.ENDER_PEARL;
                                                          }
                                                      };

    public static void initTeleportRestrictions()
    {
        invalidDests.clear();
        for (int i : PokecubeMod.core.getConfig().teleDimBlackList)
        {
            invalidDests.add(new Integer(i));
        }
    }

    public static void unsetTeleport(TeleDest teleport, String uuid)
    {
        Iterator<TeleDest> dests = getTeleports(uuid).iterator();
        while (dests.hasNext())
        {
            TeleDest dest = dests.next();
            if (dest.loc.withinDistance(MINDIST, teleport.loc))
            {
                dests.remove();
            }
        }
    }

    public static int getTeleIndex(String uuid)
    {
        return PokecubePlayerDataHandler.getInstance().getPlayerData(uuid).getData(PokecubePlayerData.class)
                .getTeleIndex();
    }

    public static TeleDest getTeleport(String uuid)
    {
        List<TeleDest> list = PokecubePlayerDataHandler.getInstance().getPlayerData(uuid)
                .getData(PokecubePlayerData.class).getTeleDests();
        int index = getTeleIndex(uuid);
        TeleDest d = null;
        if (list.size() > index)
        {
            d = list.get(index);
        }
        return d;
    }

    public static List<TeleDest> getTeleports(String uuid)
    {
        return PokecubePlayerDataHandler.getInstance().getPlayerData(uuid).getData(PokecubePlayerData.class)
                .getTeleDests();
    }

    public static void setTeleIndex(String uuid, int index)
    {
        PokecubePlayerDataHandler.getInstance().getPlayerData(uuid).getData(PokecubePlayerData.class)
                .setTeleIndex(index);
    }

    public static void setTeleport(String uuid, TeleDest teleport)
    {
        boolean set = false;
        List<TeleDest> list = getTeleports(uuid);
        ListIterator<TeleDest> dests = list.listIterator();
        while (dests.hasNext())
        {
            TeleDest dest = dests.next();
            if (dest.loc.withinDistance(MINDIST, teleport.loc))
            {
                if (set) dests.remove();
                else
                {
                    set = true;
                    dests.set(teleport);
                }
            }
        }
        if (!set)
        {
            list.add(teleport);
        }
    }

    public static void setTeleport(Vector4 v, String uuid)
    {
        TeleDest d = new TeleDest(v);
        setTeleport(uuid, d);
    }

    public static void renameTeleport(Vector4 v, String uuid, String customName)
    {
        TeleDest teleport = new TeleDest(v).setName(customName);
        setTeleport(uuid, teleport);
    }

    @Override
    public void handleCommand(IPokemob pokemob) throws Exception
    {
        EntityPlayer player = (EntityPlayer) pokemob.getOwner();
        TeleDest d = getTeleport(player.getCachedUniqueIdString());
        if (d == null) return;
        Vector3 loc = d.getLoc();
        Integer dim = d.getDim();
        Integer oldDim = player.dimension;
        int needed = PokecubeMod.core.getConfig().telePearlsCostSameDim;
        if (dim != oldDim)
        {
            needed = PokecubeMod.core.getConfig().telePearlsCostOtherDim;
            if (invalidDests.contains(dim) || invalidDests.contains(oldDim))
            {
                ITextComponent text = CommandTools.makeTranslatedMessage("pokemob.teleport.invalid", "red");
                pokemob.displayMessageToOwner(text);
                return;
            }
        }
        int count = 0;
        for (int i = 2; i < pokemob.getPokemobInventory().getSizeInventory(); i++)
        {
            ItemStack stack = pokemob.getPokemobInventory().getStackInSlot(i);
            if (CompatWrapper.isValid(stack))
            {
                if (VALIDTELEITEMS.test(stack)) count += CompatWrapper.getStackSize(stack);
            }
        }
        if (needed > count)
        {
            ITextComponent text = CommandTools.makeTranslatedMessage("pokemob.teleport.noitems", "red", needed);
            pokemob.displayMessageToOwner(text);
            return;
        }
        if (needed > 0)
        {
            for (int i = 2; i < pokemob.getPokemobInventory().getSizeInventory(); i++)
            {
                ItemStack stack = pokemob.getPokemobInventory().getStackInSlot(i);
                if (CompatWrapper.isValid(stack))
                {
                    if (VALIDTELEITEMS.test(stack))
                    {
                        int toRemove = Math.min(needed, CompatWrapper.getStackSize(stack));
                        stack.splitStack(toRemove);
                        needed -= toRemove;
                        if (CompatWrapper.isValid(stack)) pokemob.getPokemobInventory().setStackInSlot(i, stack);
                        else pokemob.getPokemobInventory().setStackInSlot(i, CompatWrapper.nullStack);
                    }
                    if (needed <= 0) break;
                }
            }
        }
        if (needed > 0)
        {
            ITextComponent text = CommandTools.makeTranslatedMessage("pokemob.teleport.noitems", "red", needed);
            pokemob.displayMessageToOwner(text);
            return;
        }
        ITextComponent attackName = new TextComponentTranslation(
                MovesUtils.getUnlocalizedMove(IMoveNames.MOVE_TELEPORT));
        ITextComponent text = CommandTools.makeTranslatedMessage("pokemob.move.used", "green",
                pokemob.getPokemonDisplayName(), attackName);
        pokemob.displayMessageToOwner(text);
        EventsHandler.recallAllPokemobsExcluding(player, (IPokemob) null);
        Transporter.teleportEntity(player, loc, dim, false);
    }

    @Override
    public void writeToBuf(ByteBuf buf)
    {
    }

    @Override
    public void readFromBuf(ByteBuf buf)
    {
    }

}
