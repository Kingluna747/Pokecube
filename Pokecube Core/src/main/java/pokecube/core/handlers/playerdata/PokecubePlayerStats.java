package pokecube.core.handlers.playerdata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatisticsManager;
import net.minecraftforge.common.AchievementPage;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.achievements.AchievementCatch;
import pokecube.core.achievements.AchievementHatch;
import pokecube.core.achievements.AchievementKill;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import thut.core.common.handlers.PlayerDataHandler.PlayerData;

/** Player capture/hatch/kill stats */
public class PokecubePlayerStats extends PlayerData
{
    private Map<PokedexEntry, Integer> hatches;
    private Map<PokedexEntry, Integer> captures;
    private Map<PokedexEntry, Integer> kills;
    protected boolean                  hasFirst = false;
    private NBTTagCompound             backup;
    protected UUID                     uuid;

    public PokecubePlayerStats()
    {
        super();
    }

    public void initAchievements(StatisticsManager manager)
    {
        captures = Maps.newHashMap();
        hatches = Maps.newHashMap();
        kills = Maps.newHashMap();
        for (PokedexEntry e : catchAchievements.keySet())
        {
            int num = manager.readStat(catchAchievements.get(e));
            if (num > 0) captures.put(e, num);
        }
        for (PokedexEntry e : hatchAchievements.keySet())
        {
            int num = manager.readStat(hatchAchievements.get(e));
            if (num > 0) hatches.put(e, num);
        }
        for (PokedexEntry e : killAchievements.keySet())
        {
            int num = manager.readStat(killAchievements.get(e));
            if (num > 0) kills.put(e, num);
        }
    }

    public void addCapture(UUID player, PokedexEntry entry)
    {
        Achievement ach = catchAchievements.get(entry);
        if (ach == null)
        {
            System.err.println("missing for " + entry);
            return;
        }
        int num = getManager(player).readStat(ach);
        getCaptures(player).put(entry, num + 1);
        getPlayer(player).addStat(ach);
    }

    public void addKill(UUID player, PokedexEntry entry)
    {
        Achievement ach = killAchievements.get(entry);
        if (ach == null)
        {
            System.err.println("missing for " + entry);
            return;
        }
        int num = getManager(player).readStat(ach);
        getKills(player).put(entry, num + 1);
        getPlayer(player).addStat(ach);
    }

    public void addHatch(UUID player, PokedexEntry entry)
    {
        Achievement ach = hatchAchievements.get(entry);
        if (ach == null) { return; }
        int num = getManager(player).readStat(ach);
        getHatches(player).put(entry, num + 1);
        getPlayer(player).addStat(ach);
    }

    public void setHasFirst()
    {
        hasFirst = true;
    }

    @Override
    public String getIdentifier()
    {
        return "pokecube-stats";
    }

    @Override
    public boolean shouldSync()
    {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag)
    {
        tag = backup;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag)
    {
        EntityPlayer player = PokecubeCore.proxy.getPlayer(uuid);
        if (player == null || tag == null) return;

        if (player.worldObj.isRemote)
        {
            initAchievements(getManager(uuid));
            return;
        }
        backup = tag;
        NBTTagCompound temp = tag.getCompoundTag("kills");
        PokedexEntry entry;
        hasFirst = tag.getBoolean("F");
        for (String s : temp.getKeySet())
        {
            int num = temp.getInteger(s);
            if (num > 0 && (entry = Database.getEntry(s)) != null)
            {
                for (int i = 0; i < num; i++)
                    addKill(uuid, entry);
            }
        }
        temp = tag.getCompoundTag("captures");
        for (String s : temp.getKeySet())
        {
            int num = temp.getInteger(s);
            if (num > 0 && (entry = Database.getEntry(s)) != null)
            {
                for (int i = 0; i < num; i++)
                    addCapture(uuid, entry);
            }
        }
        temp = tag.getCompoundTag("hatches");
        for (String s : temp.getKeySet())
        {
            int num = temp.getInteger(s);
            if (num > 0 && (entry = Database.getEntry(s)) != null)
            {
                for (int i = 0; i < num; i++)
                    addHatch(uuid, entry);
            }
        }
    }

    @Override
    public String dataFileName()
    {
        return "pokecubeStats";
    }

    public Map<PokedexEntry, Integer> getCaptures(UUID player)
    {
        if (captures == null) initAchievements(getManager(player));
        return captures;
    }

    public Map<PokedexEntry, Integer> getKills(UUID player)
    {
        if (kills == null) initAchievements(getManager(player));
        return kills;
    }

    public Map<PokedexEntry, Integer> getHatches(UUID player)
    {
        if (hatches == null) initAchievements(getManager(player));
        return hatches;
    }

    private StatisticsManager getManager(UUID player)
    {
        return PokecubeCore.proxy.getManager(player);
    }

    private EntityPlayer getPlayer(UUID player)
    {
        return PokecubeCore.proxy.getPlayer(player);
    }

    // Achievements
    public static Achievement                        get1stPokemob;
    // public static HashMap<Integer, Achievement> pokemobAchievements;

    public static AchievementPage                    achievementPageCatch;
    public static AchievementPage                    achievementPageKill;
    public static AchievementPage                    achievementPageHatch;
    public static HashMap<PokedexEntry, Achievement> catchAchievements = Maps.newHashMap();
    public static HashMap<PokedexEntry, Achievement> hatchAchievements = Maps.newHashMap();
    public static HashMap<PokedexEntry, Achievement> killAchievements  = Maps.newHashMap();

    public static void initAchievements()
    {
        if (get1stPokemob == null)
        {
            System.out.println("REGISTERING ACHIEVEMENT");
            get1stPokemob = (new AchievementCatch(null, -3, -3, PokecubeItems.getItem("pokedex"), null));
            get1stPokemob.registerStat();
            AchievementList.ACHIEVEMENTS.add(get1stPokemob);
            achievementPageCatch = new AchievementPage("Pokecube Captures");
            AchievementPage.registerAchievementPage(achievementPageCatch);
            achievementPageHatch = new AchievementPage("Pokecube Hatchs");
            AchievementPage.registerAchievementPage(achievementPageHatch);
            achievementPageKill = new AchievementPage("Pokecube Kills");
            AchievementPage.registerAchievementPage(achievementPageKill);
        }
    }

    /** This is moved here for future 1.12 stuff. */
    public static void registerAchievements(PokedexEntry e)
    {
        if (e.getStats() == null || e.evs == null)
        {
            System.err.println(new NullPointerException(e + " is missing stats or evs " + e.getStats() + " " + e.evs));
        }
        int x = -2 + (e.getPokedexNb() / 16) * 2;
        int y = -2 + (e.getPokedexNb() % 16) * 2 - 1;
        if (!hatchAchievements.containsKey(e) && e.evolvesFrom == null)
        {
            registerHatchAchieve(e);
        }
        if (!catchAchievements.containsKey(e))
        {
            registerCatchAchieve(e);
        }
        if (!killAchievements.containsKey(e))
        {
            AchievementKill kill = new AchievementKill(e, x, y, PokecubeItems.getEmptyCube(0), null);
            kill.registerStat();
            achievementPageKill.getAchievements().add(kill);
            killAchievements.put(e, kill);
        }
    }

    private static Achievement registerHatchAchieve(PokedexEntry e)
    {
        if (hatchAchievements.containsKey(e)) return hatchAchievements.get(e);
        int x = -2 + (e.getPokedexNb() / 16) * 2;
        int y = -2 + (e.getPokedexNb() % 16) * 2 - 1;
        PokedexEntry actual = e.getBaseForme() != null ? e.getBaseForme() : e;
        Achievement hatch = actual != e ? registerHatchAchieve(actual)
                : new AchievementHatch(actual, x, y, PokecubeItems.getEmptyCube(0), null);
        if (e != actual) hatchAchievements.put(e, hatch);
        if (!hatchAchievements.containsKey(actual))
        {
            hatch.registerStat();
            achievementPageHatch.getAchievements().add(hatch);
            hatchAchievements.put(actual, hatch);
        }
        return hatch;
    }

    private static Achievement registerCatchAchieve(PokedexEntry e)
    {
        if (catchAchievements.containsKey(e)) return catchAchievements.get(e);
        int x = -2 + (e.getPokedexNb() / 16) * 2;
        int y = -4 + (e.getPokedexNb() % 16) * 2 - 1;
        Achievement parent = null;
        if (PokecubeCore.core.getConfig().catchOrderRequired && e.evolvesFrom != null && e.getSpawnData() == null)
        {
            boolean baseCanSpawn = false;
            PokedexEntry from = e.evolvesFrom;
            while (from != null && !baseCanSpawn)
            {
                baseCanSpawn = from.getSpawnData() != null;
                from = from.evolvesFrom;
            }
            if (baseCanSpawn) parent = registerCatchAchieve(e.evolvesFrom);
        }
        PokedexEntry actual = e.getBaseForme() != null ? e.getBaseForme() : e;
        Achievement catc = actual != e ? registerCatchAchieve(actual)
                : new AchievementCatch(actual, x, y, PokecubeItems.getEmptyCube(0), parent);
        if (e != actual) catchAchievements.put(e, catc);
        if (!catchAchievements.containsKey(actual))
        {
            catc.registerStat();
            achievementPageCatch.getAchievements().add(catc);
            catchAchievements.put(actual, catc);
        }
        return catc;
    }

    private static Map<String, Achievement> achievements = Maps.newHashMap();

    public static Achievement getAchievement(String desc)
    {
        return achievements.get(desc);
    }

    public static void initMap()
    {
        for (Achievement a : AchievementList.ACHIEVEMENTS)
        {
            if (a == null) continue;
            try
            {
                String name = a.statId;
                if (name != null) achievements.put(name, a);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void reset()
    {
        achievements.clear();
    }
}
