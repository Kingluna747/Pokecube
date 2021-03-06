package pokecube.core.commands;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;

public class CullCommand extends CommandBase
{
    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getCommandName()
    {
        return "pokecull";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/pokecull";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender cSender, String[] args) throws CommandException
    {
        boolean specific = args.length > 1;

        World world = cSender.getEntityWorld();
        List<Entity> entities = new ArrayList<Entity>(world.loadedEntityList);
        String name = "";
        PokedexEntry entry = null;
        if (specific)
        {
            name = args[1];
            entry = Database.getEntry(name);
            if (entry == null) throw new CommandException(name + " not found");
        }
        int n = 0;
        for (Entity o : entities)
        {
            IPokemob e = CapabilityPokemob.getPokemobFor(o);
            if (e != null)
            {
                if (!specific || e.getPokedexEntry() == entry)
                {
                    if (!e.getPokemonAIState(IMoveConstants.TAMED) && o.getEntityWorld().getClosestPlayerToEntity(o,
                            PokecubeMod.core.getConfig().maxSpawnRadius) == null)
                    {
                        o.setDead();
                        n++;
                    }
                }
            }
        }
        cSender.addChatMessage(new TextComponentString("Culled " + n));
    }
}
