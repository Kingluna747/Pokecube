package pokecube.core.handlers;

import static pokecube.core.PokecubeItems.getItem;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.database.stats.CaptureStats;
import pokecube.core.events.PokedexInspectEvent;
import pokecube.core.handlers.PokecubePlayerDataHandler.PokecubePlayerCustomData;
import pokecube.core.interfaces.IMoveNames;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.ItemTM;
import pokecube.core.utils.Tools;

public class PokedexInspector
{
    public static interface IInspectReward
    {
        boolean inspect(PokecubePlayerCustomData data, Entity entity, boolean giveReward);
    }

    public static class TranslatedReward implements IInspectReward
    {
        @Override
        public boolean inspect(PokecubePlayerCustomData data, Entity entity, boolean giveReward)
        {
            String lang = data.tag.getString("lang");
            if (lang.isEmpty()) lang = "en_US";
            if (data.tag.getBoolean("inspect-guidebook")) return false;
            if (giveReward)
            {
                ItemStack book = getInfoBook(lang);
                data.tag.setBoolean("inspect-guidebook", true);
                entity.addChatMessage(new TextComponentTranslation("pokedex.inspect.guidebook"));
                EntityPlayer entityPlayer = (EntityPlayer) entity;
                Tools.giveItem(entityPlayer, book);
                PokecubePlayerDataHandler.saveCustomData(entity.getCachedUniqueIdString());
            }
            return true;
        }

        public ItemStack getInfoBook(String lang)
        {
            String name = "";
            InputStream inputstream;
            Map<String, String> table = Maps.newHashMap();
            try
            {
                inputstream = LanguageMap.class.getResourceAsStream("/assets/pokecube/lang/" + lang + ".lang");
                table = LanguageMap.parseLangFile(inputstream);
                inputstream.close();
            }
            catch (IOException e1)
            {
                PokecubeMod.log("Error loading book for " + lang);
            }
            name = table.get("pokecube.infobook1.json");
            if (name == null)
            {
                inputstream = LanguageMap.class.getResourceAsStream("/assets/pokecube/lang/en_US.lang");
                table = LanguageMap.parseLangFile(inputstream);
                try
                {
                    inputstream.close();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                name = table.get("pokecube.infobook1.json");
            }

            ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
            try
            {
                stack.setTagCompound(JsonToNBT.getTagFromJson(name));
            }
            catch (NBTException e)
            {
                e.printStackTrace();
            }
            return stack;
        }
    }

    public static class InspectCapturesReward implements IInspectReward
    {
        final ItemStack reward;
        final Field     configField;
        final String    message;
        final String    tagString;

        public InspectCapturesReward(ItemStack reward, Field configField, String message, String tagString)
        {
            this.reward = reward;
            this.configField = configField;
            this.message = message;
            this.tagString = tagString;
        }

        private boolean check(Entity entity, String configArg, NBTTagCompound tag, ItemStack reward, int num,
                boolean giveReward)
        {
            if (reward == null || tag.getBoolean(tagString)) return false;
            if (matches(num, configArg))
            {
                if (giveReward)
                {
                    tag.setBoolean(tagString, true);
                    entity.addChatMessage(new TextComponentTranslation(message));
                    EntityPlayer entityPlayer = (EntityPlayer) entity;
                    Tools.giveItem(entityPlayer, reward);
                    PokecubePlayerDataHandler.saveCustomData(entity.getCachedUniqueIdString());
                }
                return true;
            }
            return false;
        }

        private boolean matches(int num, String arg)
        {
            int required = 0;
            if (arg.contains("%"))
            {
                required = (int) (Double.parseDouble(arg.replace("%", "")) * Database.spawnables.size() / 100d);
            }
            else
            {
                required = (int) (Double.parseDouble(arg));
            }
            return required <= num;
        }

        @Override
        public boolean inspect(PokecubePlayerCustomData data, Entity entity, boolean giveReward)
        {
            int num = CaptureStats.getNumberUniqueCaughtBy(entity.getCachedUniqueIdString());
            try
            {
                return check(entity, (String) configField.get(PokecubeMod.core.getConfig()), data.tag, reward, num,
                        giveReward);
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            return false;
        }
    }

    public static boolean inspect(EntityPlayer player, boolean reward)
    {
        PokedexInspectEvent evt;
        MinecraftForge.EVENT_BUS.post(evt = new PokedexInspectEvent(player, reward));
        if (evt.isCanceled())
        {
            String uuid = evt.getEntity().getCachedUniqueIdString();
            PokecubePlayerDataHandler.getInstance().save(uuid);
        }
        return evt.isCanceled();
    }

    public static Set<IInspectReward> rewards = Sets.newHashSet();

    public static void init() throws NoSuchFieldException, SecurityException
    {
        ItemStack cut = new ItemStack(getItem("tm"));
        ItemTM.addMoveToStack(IMoveNames.MOVE_CUT, cut);
        ItemStack flash = new ItemStack(getItem("tm"));
        ItemTM.addMoveToStack(IMoveNames.MOVE_FLASH, flash);
        ItemStack rocksmash = new ItemStack(getItem("tm"));
        ItemTM.addMoveToStack(IMoveNames.MOVE_ROCKSMASH, rocksmash);

        rewards.add(new InspectCapturesReward(PokecubeItems.getStack("exp_share"),
                PokecubeMod.core.getConfig().getClass().getDeclaredField("exp_shareRequirement"),
                "pokedex.inspect.exp_share", "inspect-exp_share"));
        rewards.add(new InspectCapturesReward(cut,
                PokecubeMod.core.getConfig().getClass().getDeclaredField("cutTMRequirement"), "pokedex.inspect.cutTM",
                "inspect-cutTM"));
        rewards.add(new InspectCapturesReward(flash,
                PokecubeMod.core.getConfig().getClass().getDeclaredField("flashTMRequirement"),
                "pokedex.inspect.flashTM", "inspect-flashTM"));
        rewards.add(new InspectCapturesReward(rocksmash,
                PokecubeMod.core.getConfig().getClass().getDeclaredField("rocksmashTMRequirement"),
                "pokedex.inspect.rocksmashTM", "inspect-rocksmashTM"));
        rewards.add(new InspectCapturesReward(PokecubeItems.getStack("mastercube"),
                PokecubeMod.core.getConfig().getClass().getDeclaredField("mastercubeRequirement"),
                "pokedex.inspect.mastercube", "inspect-mastercube"));
        rewards.add(new InspectCapturesReward(PokecubeItems.getStack("shiny_charm"),
                PokecubeMod.core.getConfig().getClass().getDeclaredField("shinycharmRequirement"),
                "pokedex.inspect.shinycharm", "inspect-shinycharm"));
        rewards.add(new TranslatedReward());
    }

    public PokedexInspector()
    {
        MinecraftForge.EVENT_BUS.register(this);
        if (rewards.isEmpty()) try
        {
            init();
        }
        catch (NoSuchFieldException | SecurityException e)
        {
            e.printStackTrace();
        }
    }

    @SubscribeEvent(receiveCanceled = false, priority = EventPriority.LOWEST)
    public void inspectEvent(PokedexInspectEvent evt)
    {
        String uuid = evt.getEntity().getCachedUniqueIdString();
        PokecubePlayerCustomData data = PokecubePlayerDataHandler.getInstance().getPlayerData(uuid)
                .getData(PokecubePlayerCustomData.class);
        boolean done = false;
        for (IInspectReward reward : rewards)
        {
            done = done || reward.inspect(data, evt.getEntity(), evt.shouldReward);
        }
        if (done) evt.setCanceled(true);
    }
}
