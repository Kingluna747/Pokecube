package pokecube.core.commands;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class CommandTools
{
    public static boolean isOp(ICommandSender sender)
    {
        if (FMLCommonHandler.instance().getMinecraftServerInstance() != null
                && !FMLCommonHandler.instance().getMinecraftServerInstance().isDedicatedServer()) { return true; }

        if (sender instanceof EntityPlayer)
        {
            EntityPlayer player = sender.getEntityWorld().getPlayerEntityByName(sender.getName());
            UserListOpsEntry userentry = ((EntityPlayerMP) player).mcServer.getConfigurationManager().getOppedPlayers()
                    .getEntry(player.getGameProfile());
            return userentry != null && userentry.getPermissionLevel() >= 4;
        }
        else if (sender instanceof TileEntityCommandBlock) { return true; }
        return sender.getName().equalsIgnoreCase("@") || sender.getName().equals("Server");
    }

    public static IChatComponent makeError(String text)
    {
        return makeTranslatedMessage(text, "red:italic");
    }

    public static void sendBadArgumentsMissingArg(ICommandSender sender)
    {
        sender.addChatMessage(makeError("pokecube.command.invalidmissing"));
    }

    public static void sendBadArgumentsTryTab(ICommandSender sender)
    {
        sender.addChatMessage(makeError("pokecube.command.invalidtab"));
    }

    public static void sendError(ICommandSender sender, String text)
    {
        sender.addChatMessage(makeError(text));
    }

    public static void sendMessage(ICommandSender sender, String text)
    {
        IChatComponent message = makeTranslatedMessage(text, null);
        sender.addChatMessage(message);
    }

    public static void sendNoPermissions(ICommandSender sender)
    {
        sender.addChatMessage(makeError("pokecube.command.noperms"));
    }

    public static IChatComponent makeTranslatedMessage(String key, String formatting, Object... args)
    {
        IChatComponent message = null;
        if (formatting == null) formatting = "";
        String argString = "";
        int num = 1;
        if (args != null) for (Object s : args)
        {
            argString = argString + "{\"translate\":\"" + s + "\"}";
            num++;
            if (num <= args.length) argString = argString + ",";
        }
        if (argString.isEmpty()) argString = "\"\"";

        String format = "";
        if (!formatting.isEmpty())
        {
            String[] args2 = formatting.split(":");
            format = ",\"color\":\"" + args2[0] + "\"";
            if (args2.length > 1)
            {
                for (int i = 1; i < args2.length; i++)
                {
                    String arg = args2[i];
                    if (arg.equalsIgnoreCase("italic"))
                    {
                        format = format + ",\"italic\":true";
                    }
                    if (arg.equalsIgnoreCase("bold"))
                    {
                        format = format + ",\"bold\":true";
                    }
                    if (arg.equalsIgnoreCase("underlined"))
                    {
                        format = format + ",\"underlined\":true";
                    }
                    if (arg.equalsIgnoreCase("strikethrough"))
                    {
                        format = format + ",\"strikethrough\":true";
                    }
                    if (arg.equalsIgnoreCase("obfuscated"))
                    {
                        format = format + ",\"obfuscated\":true";
                    }
                }
            }
        }
        String text = "{\"translate\":\"" + key + "\",\"with\":[" + argString + "]" + format + "}";
        text = "[" + text + "]";
        try
        {
            message = IChatComponent.Serializer.jsonToComponent(text);
        }
        catch (Exception e)
        {
            message = new ChatComponentText(EnumChatFormatting.RED + "message error");
        }
        return message;
    }
}
