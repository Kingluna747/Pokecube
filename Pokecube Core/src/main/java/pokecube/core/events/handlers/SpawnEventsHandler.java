package pokecube.core.events.handlers;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.SpawnBiomeMatcher.SpawnCheck;
import pokecube.core.events.SpawnEvent;
import pokecube.core.interfaces.PokecubeMod;
import thut.api.maths.Vector3;

public class SpawnEventsHandler
{

    public SpawnEventsHandler()
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void CapLevel(SpawnEvent.Level event)
    {
        int level = event.getInitialLevel();
        if (SpawnHandler.lvlCap) level = Math.min(level, SpawnHandler.capLevel);
        event.setLevel(level);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void PickSpawn(SpawnEvent.Pick.Pre event)
    {
        Vector3 v = event.getLocation();
        World world = event.world;
        List<PokedexEntry> entries = Lists.newArrayList(Database.spawnables);
        Collections.shuffle(entries);
        int index = 0;
        PokedexEntry dbe = entries.get(index);
        SpawnCheck checker = new SpawnCheck(v, world);
        float weight = dbe.getSpawnData().getWeight(dbe.getSpawnData().getMatcher(checker));
        double random = Math.random();
        int n = 0;
        int max = entries.size();
        Vector3 vbak = v.copy();
        while (weight <= random && n++ < max)
        {
            dbe = entries.get((++index) % entries.size());
            weight = dbe.getSpawnData().getWeight(dbe.getSpawnData().getMatcher(checker));
            if (!dbe.flys() && random > weight)
            {
                v = Vector3.getNextSurfacePoint2(world, vbak, Vector3.secondAxisNeg, 10);
                if (v != null) Vector3.movePointOutOfBlocks(v, world);
                if (v != null) weight = dbe.getSpawnData().getWeight(dbe.getSpawnData().getMatcher(world, v));
                else weight = 0;
            }
            else if (v == null)
            {
                v = vbak.copy();
            }
        }
        if (random > weight || v == null) return;
        if (dbe.legendary)
        {
            int level = SpawnHandler.getSpawnLevel(world, v, dbe);
            if (level < PokecubeMod.core.getConfig().minLegendLevel) { return; }
        }
        event.setPick(dbe);
    }
}
