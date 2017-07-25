package pokecube.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;

public class CountCommand extends CommandBase
{
    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getCommandName()
    {
        return "pokecount";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/pokecount";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender cSender, String[] args) throws CommandException
    {
        boolean specific = args.length > 0;
        World world = cSender.getEntityWorld();
        List<Entity> entities = new ArrayList<Entity>(world.loadedEntityList);
        int count1 = 0;
        int count2 = 0;
        String name = "";
        Map<PokedexEntry, Integer> counts = Maps.newHashMap();
        PokedexEntry entry = null;
        if (specific)
        {
            name = args[1];
            entry = Database.getEntry(name);
            if (entry == null) throw new CommandException(name + " not found");
        }
        for (Entity o : entities)
        {
            IPokemob e = CapabilityPokemob.getPokemobFor(o);
            if (e != null)
            {
                if (!specific || e.getPokedexEntry() == entry)
                {
                    if (o.getDistance(cSender.getPositionVector().xCoord, cSender.getPositionVector().yCoord,
                            cSender.getPositionVector().zCoord) > PokecubeMod.core.getConfig().maxSpawnRadius)
                        count2++;
                    else count1++;
                    Integer i = counts.get(e.getPokedexEntry());
                    if (i == null) i = 0;
                    counts.put(e.getPokedexEntry(), i + 1);
                }
            }
        }
        cSender.addChatMessage(CommandTools.makeTranslatedMessage("pokecube.command.count", "", count1, count2));
        cSender.addChatMessage(new TextComponentString(counts.toString()));
    }
}
