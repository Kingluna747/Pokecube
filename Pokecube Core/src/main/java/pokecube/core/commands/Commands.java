package pokecube.core.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import pokecube.core.blocks.pc.InventoryPC;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.EvolutionData;
import pokecube.core.database.moves.MovesParser;
import pokecube.core.database.moves.json.JsonMoves;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.handlers.playerdata.PokecubePlayerStats;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.moves.MovesUtils;
import pokecube.core.moves.animations.AnimationMultiAnimations;
import pokecube.core.network.packets.PacketDataSync;
import pokecube.core.utils.PokecubeSerializer;
import thut.core.common.commands.CommandTools;

public class Commands extends CommandBase
{
    private List<String> aliases;

    public Commands()
    {
        this.aliases = new ArrayList<String>();
        this.aliases.add("pokecube");
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public int compareTo(ICommand arg0)
    {
        return super.compareTo(arg0);
    }

    private boolean doDebug(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
    {
//        if (args[0].equalsIgnoreCase("items"))
//        {
//            WorldServer world = (WorldServer) cSender.getEntityWorld();
//            List<Entity> items = world.loadedEntityList;
//            for (Entity e : items)
//            {
//                if (e instanceof EntityItem) e.setDead();
//            }
//            return true;
//        }
        return false;
    }

    private boolean doMeteor(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
    {
        return false;
    }

    private boolean doRecall(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
            throws CommandException
    {
        if (args[0].equalsIgnoreCase("recall")) {

        return true; }
        return false;
    }

    private boolean doReset(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
            throws CommandException
    {
        if (args[0].equalsIgnoreCase("resetreward"))
        {
            if (args.length >= 3)
            {
                EntityPlayer player = null;
                String name = null;
                if (targets == null)
                {
                    name = args[1];
                    player = getPlayer(cSender.getServer(), cSender, name);
                }
                else
                {
                    player = targets[0];
                }
                String reward = args[2];
                boolean check = args.length == 3;
                if (isOp || !FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer())
                {
                    if (player != null)
                    {
                        NBTTagCompound tag = PokecubePlayerDataHandler.getCustomDataTag(player);
                        if (check)
                        {
                            boolean has = tag.getBoolean(reward);
                            cSender.addChatMessage(CommandTools.makeTranslatedMessage("pokecube.command.checkreward",
                                    "", player.getName(), reward, has));
                        }
                        else
                        {
                            tag.setBoolean(reward, false);
                            cSender.addChatMessage(CommandTools.makeTranslatedMessage("pokecube.command.resetreward",
                                    "", player.getName(), reward));
                            PokecubePlayerDataHandler.saveCustomData(player);
                        }
                    }
                    else
                    {
                        throw new PlayerNotFoundException(args[1]);
                    }
                }
                else
                {
                    CommandTools.sendNoPermissions(cSender);
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    private boolean doFixAcievements(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
            throws CommandException
    {
        if (args[0].equalsIgnoreCase("fixFromPC") && args.length == 2)
        {
            EntityPlayer player = getPlayer(cSender.getServer(), cSender, args[1]);
            InventoryPC pc = InventoryPC.getPC(player);
            PokecubePlayerStats stats = PokecubePlayerDataHandler.getInstance().getPlayerData(player)
                    .getData(PokecubePlayerStats.class);
            for (ItemStack stack : pc.getContents())
            {
                PokedexEntry entry;
                if (stack != null && (entry = PokecubeManager.getEntry(stack)) != null)
                {
                    List<PokedexEntry> toAdd = Lists.newArrayList();
                    populateFromNextForme(toAdd, entry);
                    System.out.println(toAdd);
                    for (PokedexEntry prev : toAdd)
                    {
                        boolean has = stats.getCaptures(player.getUniqueID()).containsKey(prev);
                        has = has || stats.getHatches(player.getUniqueID()).containsKey(prev);
                        if (!has)
                        {
                            stats.addCapture(player.getUniqueID(), entry);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void populateFromNextForme(List<PokedexEntry> toAdd, PokedexEntry entry)
    {
        for (PokedexEntry e : entry.related)
        {
            for (EvolutionData d : e.evolutions)
            {
                if (d.evolution == entry)
                {
                    toAdd.add(e);
                    populateFromNextForme(toAdd, e);
                    break;
                }
            }
        }
    }

    private boolean doSetHasStarter(ICommandSender cSender, String[] args, boolean isOp, EntityPlayerMP[] targets)
    {
        if (args[0].equalsIgnoreCase("denystarter") && args.length == 2)
        {
            WorldServer world = (WorldServer) cSender.getEntityWorld();
            EntityPlayer player = null;

            int num = 1;
            int index = 0;
            String name = null;

            if (targets != null)
            {
                num = targets.length;
            }
            else
            {
                name = args[1];
                player = world.getPlayerEntityByName(name);
            }

            for (int i = 0; i < num; i++)
                if (isOp || !FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer())
                {
                    if (targets != null)
                    {
                        player = targets[index];
                    }
                    if (player != null)
                    {
                        PokecubeSerializer.getInstance().setHasStarter(player, true);
                        PacketDataSync.sendInitPacket(player, "pokecube-data");
                        cSender.addChatMessage(
                                new TextComponentTranslation("pokecube.command.denystarter", player.getName()));
                    }
                }
                else
                {
                    CommandTools.sendNoPermissions(cSender);
                    return false;
                }
            return true;
        }
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0)
        {
            CommandTools.sendBadArgumentsTryTab(sender);
            return;
        }
        if (args[0].equals("reloadAnims"))
        {
            try
            {
                File moves = new File(Database.CONFIGLOC + args[1] + ".json");
                File anims = new File(Database.CONFIGLOC + args[2] + ".json");
                JsonMoves.loadMoves(moves);
                JsonMoves.merge(anims, moves);
                MovesParser.load(moves);
            }
            catch (Exception e)
            {
                throw new CommandException("Error loading animations");
            }
            for (Move_Base move : MovesUtils.moves.values())
            {
                if (move.move.baseEntry != null && move.move.baseEntry.animations != null
                        && !move.move.baseEntry.animations.isEmpty())
                {
                    move.setAnimation(new AnimationMultiAnimations(move.move));
                    continue;
                }
            }
            CommandTools.sendMessage(sender, "Reloaded move animations.");
            return;
        }

        EntityPlayerMP[] targets = null;
        for (int i = 1; i < args.length; i++)
        {
            String s = args[i];
            if (s.contains("@"))
            {
                ArrayList<EntityPlayer> targs = new ArrayList<EntityPlayer>(
                        EntitySelector.matchEntities(sender, s, EntityPlayer.class));
                targets = targs.toArray(new EntityPlayerMP[0]);
            }
        }
        boolean isOp = CommandTools.isOp(sender);
        boolean message = false;

        if (doRecall(sender, args, isOp, targets)) { throw new CommandException("Use '/pokerecall'"); }

        message |= doDebug(sender, args, isOp, targets);
        message |= doReset(sender, args, isOp, targets);
        message |= doMeteor(sender, args, isOp, targets);
        message |= doSetHasStarter(sender, args, isOp, targets);
        message |= doFixAcievements(sender, args, isOp, targets);
        if (!message)
        {
            CommandTools.sendBadArgumentsTryTab(sender);
        }
    }

    @Override
    public List<String> getCommandAliases()
    {
        return this.aliases;
    }

    @Override
    public String getCommandName()
    {
        return "pokecube";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender)
    {
        return "pokecube <text>";
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args,
            BlockPos pos)
    {
        List<String> ret = new ArrayList<String>();
        if (args[0].isEmpty())
        {
            return ret;
        }
        if (ret.isEmpty() && args.length == 1)
        {
            ret.add("reloadAnims moves animations");
        }
        return ret;
    }

    @Override
    public boolean isUsernameIndex(String[] astring, int i)
    {
        return false;
    }
}
