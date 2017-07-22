package pokecube.core.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;

public class GiftCommand extends CommandBase
{
    private List<String> aliases;

    public GiftCommand()
    {
        this.aliases = new ArrayList<String>();
        this.aliases.add("pokegift");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender)
    {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (sender instanceof EntityPlayer)
        {
            if (!PokecubeMod.core.getConfig().mysterygift)
            {
                CommandTools.sendError(sender, "pokecube.command.giftdisabled");
                return;
            }
            if (args.length > 0)
            {
                String code = args[0];
                String giftSt = PokecubeMod.gifts.get(code);
                if (giftSt != null)
                {
                    EntityPlayer player = (EntityPlayer) sender;

                    if (player.getEntityData().getString("code:" + code).equals(code))
                    {

                        CommandTools.sendError(sender, "pokecube.command.giftdenyused");
                        return;
                    }
                    String[] gift = giftSt.split(";");

                    String name = gift[0];

                    IPokemob mob = CapabilityPokemob.getPokemobFor(
                            PokecubeMod.core.createPokemob(Database.getEntry(name), sender.getEntityWorld()));
                    MakeCommand.setToArgs(gift, mob, 1, null);
                    mob.setOriginalOwnerUUID(new UUID(12345, 54321));
                    mob.setPokecube(new ItemStack(PokecubeItems.getFilledCube(13)));
                    mob.setPokemonOwner(player);
                    mob.setHp(((EntityLiving) mob).getMaxHealth());
                    mob.returnToPokecube();
                    CommandTools.sendMessage(sender, "pokecube.command.gift");
                    player.getEntityData().setString("code:" + code, code);

                    return;
                }
                CommandTools.sendError(sender, "pokecube.command.giftinvalid");
                return;
            }
            CommandTools.sendError(sender, "pokecube.command.giftneedcode");
            return;
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
        return aliases.get(0);
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/" + aliases.get(0) + "<giftCode>";
    }

    @Override
    /** Return the required permission level for this command. */
    public int getRequiredPermissionLevel()
    {
        return 0;
    }
}
