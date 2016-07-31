package pokecube.core.events.handlers;

import static thut.api.terrain.TerrainSegment.GRIDSIZE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nfunk.jep.JEP;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.FMLCommonHandler;
import pokecube.core.PokecubeCore;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.SpawnData;
import pokecube.core.database.SpawnBiomeMatcher;
import pokecube.core.events.SpawnEvent;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.utils.ChunkCoordinate;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.Tools;
import pokecube.core.world.terrain.PokecubeTerrainChecker;
import thut.api.maths.ExplosionCustom;
import thut.api.maths.Vector3;
import thut.api.terrain.BiomeType;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;

/** @author Manchou Heavily modified by Thutmose */
public final class SpawnHandler
{
    private static final Map<ChunkCoordinate, Integer> forbiddenSpawningCoords = new HashMap<ChunkCoordinate, Integer>();
    public static HashMap<Integer, String>             functions               = new HashMap<Integer, String>();
    public static HashMap<Integer, Integer[]>          subBiomeLevels          = new HashMap<Integer, Integer[]>();
    public static boolean                              doSpawns                = true;
    public static boolean                              onlySubbiomes           = false;
    public static Predicate<Integer>                   biomeToRefresh          = new Predicate<Integer>()
                                                                               {
                                                                                   @Override
                                                                                   public boolean apply(Integer input)
                                                                                   {
                                                                                       if (input < 256) return true;
                                                                                       return input == BiomeType.SKY
                                                                                               .getType()
                                                                                               || input == BiomeType.CAVE
                                                                                                       .getType()
                                                                                               || input == BiomeType.VILLAGE
                                                                                                       .getType()
                                                                                               || input == BiomeType.ALL
                                                                                                       .getType()
                                                                                               || input == PokecubeTerrainChecker.INSIDE
                                                                                                       .getType()
                                                                                               || input == BiomeType.NONE
                                                                                                       .getType();
                                                                                   }
                                                                               };

    static
    {
        functions.put(-1, "(50)*(sin(x*8*10^-3)^8 + sin(y*8*10^-3)^8)");
        functions.put(0, "(50)*(sin(x*10^-3)^8 + sin(y*10^-3)^8)");
        functions.put(1, "10+r/130;r");
        functions.put(2, "(50)*(sin(x*0.5*10^-3)^8 + sin(y*0.5*10^-3)^8)");
    }

    private static Vector3                    vec1        = Vector3.getNewVector();
    private static Vector3                    vec2        = Vector3.getNewVector();
    private static Vector3                    temp        = Vector3.getNewVector();
    public static double                      MAX_DENSITY = 1;
    public static int                         MAXNUM      = 10;
    public static boolean                     lvlCap      = false;
    public static boolean                     expFunction = false;
    public static int                         capLevel    = 50;
    public static final HashMap<Integer, JEP> parsers     = new HashMap<Integer, JEP>();

    public static boolean addForbiddenSpawningCoord(BlockPos pos, int dimensionId, int distance)
    {
        return addForbiddenSpawningCoord(pos.getX(), pos.getY(), pos.getZ(), dimensionId, distance);
    }

    public static boolean addForbiddenSpawningCoord(int x, int y, int z, int dim, int range)
    {
        ChunkCoordinate coord = new ChunkCoordinate(x, y, z, dim);
        if (forbiddenSpawningCoords.containsKey(coord)) return false;
        forbiddenSpawningCoords.put(coord, range);
        return true;
    }

    public static void addSpawn(PokedexEntry entry)
    {
    }

    public static void addSpawn(PokedexEntry entry, Biome b)
    {
    }

    public static boolean canPokemonSpawnHere(Vector3 location, World worldObj, PokedexEntry entry)
    {
        if (!location.clearOfBlocks(worldObj) || !canSpawn(null, entry.getSpawnData(), location, worldObj, true))
            return false;
        if (!temp.set(location).addTo(0, entry.height, 0).clearOfBlocks(worldObj)) return false;
        if (!temp.set(location).addTo(entry.width / 2, 0, 0).clearOfBlocks(worldObj)) return false;
        if (!temp.set(location).addTo(0, 0, entry.width / 2).clearOfBlocks(worldObj)) return false;
        if (!temp.set(location).addTo(0, 0, -entry.width / 2).clearOfBlocks(worldObj)) return false;
        if (!temp.set(location).addTo(-entry.width / 2, 0, 0).clearOfBlocks(worldObj)) return false;
        IBlockState state = temp.set(location).addTo(0, -1, 0).getBlockState(worldObj);
        Block down = state.getBlock();
        return down.canCreatureSpawn(state, worldObj, temp.getPos(),
                net.minecraft.entity.EntityLiving.SpawnPlacementType.ON_GROUND);
    }

    public static boolean canSpawn(TerrainSegment terrain, SpawnData data, Vector3 v, World world,
            boolean respectDensity)
    {
        if (data == null) return false;
        if (respectDensity)
        {
            int count = Tools.countPokemon(world, v, PokecubeMod.core.getConfig().maxSpawnRadius);
            if (count > PokecubeMod.core.getConfig().mobSpawnNumber * PokecubeMod.core.getConfig().mobDensityMultiplier)
                return false;
        }
        return data.isValid(world, v);
    }

    /** Checks there's no spawner in the area
     * 
     * @param world
     * @param chunkPosX
     * @param chunkPosY
     * @param chunkPosZ
     * @return */
    public static boolean checkNoSpawnerInArea(World world, int chunkPosX, int chunkPosY, int chunkPosZ)
    {
        ArrayList<ChunkCoordinate> coords = new ArrayList<ChunkCoordinate>(forbiddenSpawningCoords.keySet());

        for (ChunkCoordinate coord : coords)
        {
            int tolerance = forbiddenSpawningCoords.get(coord);
            if (chunkPosX >= coord.getX() - tolerance && chunkPosZ >= coord.getZ() - tolerance
                    && chunkPosY >= coord.getY() - tolerance && chunkPosY <= coord.getY() + tolerance
                    && chunkPosX <= coord.getX() + tolerance && chunkPosZ <= coord.getZ() + tolerance
                    && world.provider.getDimension() == coord.dim) { return false; }
        }
        return true;
    }

    public static EntityLiving creatureSpecificInit(EntityLiving entityliving, World world, double posX, double posY,
            double posZ, Vector3 spawnPoint)
    {
        if (ForgeEventFactory.doSpecialSpawn(entityliving, world, (float) posX, (float) posY,
                (float) posZ)) { return null; }

        if (entityliving instanceof IPokemob)
        {
            IPokemob pokemob = (IPokemob) entityliving;
            int maxXP = 10;
            int level = 1;

            if (expFunction)
            {
                maxXP = getSpawnXp(world, Vector3.getNewVector().set(posX, posY, posZ), pokemob.getPokedexEntry());
                level = Tools.levelToXp(pokemob.getPokedexEntry().getEvolutionMode(), maxXP);
            }
            else
            {
                level = getSpawnLevel(world, Vector3.getNewVector().set(posX, posY, posZ), pokemob.getPokedexEntry());
            }

            if (lvlCap) level = Math.min(level, capLevel);
            maxXP = Tools.levelToXp(pokemob.getPokedexEntry().getEvolutionMode(), level);

            pokemob = pokemob.setExp(maxXP, true, true);
            pokemob.specificSpawnInit();
            return (EntityLiving) pokemob;
        }
        return null;
    }

    public static Vector3 getRandomPointNear(IBlockAccess world, Vector3 v, int distance)
    {
        Vector3 ret = v;
        Vector3 temp = ret.copy();
        int rand = Math.abs(new Random().nextInt());
        if (distance % 2 == 0) distance++;
        int num = distance * distance * distance;
        for (int i = 0; i < num; i++)
        {
            int j = (i + rand) % num;
            int x = j % (distance) - distance / 2;
            int y = (j / distance) % (distance) - distance / 2;
            int z = (j / (distance * distance)) % (distance) - distance / 2;
            y = Math.max(1, y);
            temp.set(ret).addTo(x, y, z);
            if (temp.isClearOfBlocks(world)) { return temp; }

        }
        return null;
    }

    /** Given a player, find a random position near it. */
    public static Vector3 getRandomSpawningPointNearEntity(World world, Entity player, int maxRange, int maxTries)
    {
        if (player == null) return null;

        Vector3 temp;
        Vector3 temp1 = vec1.set(player);

        Vector3 ret = temp1;
        temp = ret.copy();
        int rand = Math.abs(new Random().nextInt());
        int distance = maxRange;
        if (distance % 2 == 0) distance++;
        int num = distance * distance;
        if (maxTries > 0) num = Math.min(num, maxTries);
        for (int i = 0; i < num; i++)
        {
            for (int k = 0; k <= 20; k++)
            {
                int j = (i + rand) % num;
                int x = j % (distance) - distance / 2;
                int z = (j / distance) % (distance) - distance / 2;
                int y = 10 - world.rand.nextInt(20);
                temp.set(ret).addTo(x, y, z);
                if (temp.isClearOfBlocks(world)) { return temp; }
            }

        }

        if (temp == null) temp = Vector3.getNewVector().set(player);

        temp1 = Vector3.getNextSurfacePoint2(world, temp, vec2.set(EnumFacing.DOWN), temp.y);

        if (temp1 != null)
        {
            temp1.y++;
            return temp1;
        }
        return temp;
    }

    public static int getSpawnLevel(World world, Vector3 location, PokedexEntry pokemon)
    {
        int spawnLevel = 1;

        TerrainSegment t = TerrainManager.getInstance().getTerrian(world, location);
        int b = t.getBiome(location);
        if (subBiomeLevels.containsKey(b))
        {
            Integer[] range = subBiomeLevels.get(b);
            int dl = range[1] - range[0];
            if (dl > 0) dl = new Random().nextInt(dl) + 1;
            int level = range[0] + dl;
            return level;
        }

        Vector3 spawn = temp.set(world.getSpawnPoint());
        if (!PokecubeCore.core.getConfig().spawnCentered) spawn.clear();
        JEP toUse;
        int type = world.provider.getDimension();
        boolean isNew = false;
        String function = "";
        if (functions.containsKey(type))
        {
            function = functions.get(type);
        }
        else
        {
            function = functions.get(0);
        }
        if (parsers.containsKey(type))
        {
            toUse = parsers.get(type);
        }
        else
        {
            parsers.put(type, new JEP());
            toUse = parsers.get(type);
            isNew = true;
        }
        if (Double.isNaN(toUse.getValue()))
        {
            toUse = new JEP();
            parsers.put(type, toUse);
            isNew = true;
        }

        boolean r = function.split(";").length == 2;
        if (!r)
        {
            parseExpression(toUse, function, location.x - spawn.x, location.z - spawn.z, r, isNew);
        }
        else
        {
            double d = location.distToSq(spawn);
            parseExpression(toUse, function.split(";")[0], d, location.y, r, isNew);
        }
        spawnLevel = (int) Math.abs(toUse.getValue());
        int variance = PokecubeMod.core.getConfig().levelVariance;
        variance = new Random().nextInt(Math.max(1, variance));
        spawnLevel += variance;
        spawnLevel = Math.max(spawnLevel, 1);
        return spawnLevel;
    }

    public static int getSpawnXp(World world, Vector3 location, PokedexEntry pokemon)
    {
        int maxXp = 10;

        if (!expFunction) { return Tools.levelToXp(pokemon.getEvolutionMode(),
                getSpawnLevel(world, location, pokemon)); }

        TerrainSegment t = TerrainManager.getInstance().getTerrian(world, location);
        int b = t.getBiome(location);
        if (subBiomeLevels.containsKey(b))
        {
            Integer[] range = subBiomeLevels.get(b);
            int dl = range[1] - range[0];
            if (dl > 0) dl = new Random().nextInt(dl) + 1;
            int level = range[0] + dl;
            maxXp = Math.max(10, Tools.levelToXp(pokemon.getEvolutionMode(), level));
            return maxXp;
        }

        Vector3 spawn = temp.set(world.getSpawnPoint());
        JEP toUse;
        int type = world.provider.getDimension();
        boolean isNew = false;
        String function = "";
        if (functions.containsKey(type))
        {
            function = functions.get(type);
        }
        else
        {
            function = functions.get(0);
        }
        if (parsers.containsKey(type))
        {
            toUse = parsers.get(type);
        }
        else
        {
            parsers.put(type, new JEP());
            toUse = parsers.get(type);
            isNew = true;
        }
        if (Double.isNaN(toUse.getValue()))
        {
            toUse = new JEP();
            parsers.put(type, toUse);
            isNew = true;
        }

        boolean r = function.split(";").length == 2;
        if (!r)
        {
            parseExpression(toUse, function, location.x - spawn.x, location.z - spawn.z, r, isNew);
        }
        else
        {
            double d = location.distToSq(spawn);
            parseExpression(toUse, function.split(";")[0], d, location.y, r, isNew);
        }
        maxXp = (int) Math.abs(toUse.getValue());
        maxXp = Math.max(maxXp, 10);
        int level = Tools.xpToLevel(pokemon.getEvolutionMode(), maxXp);
        int variance = PokecubeMod.core.getConfig().levelVariance;
        level = level + new Random().nextInt(Math.max(1, variance));
        level = Math.max(1, level);
        return Tools.levelToXp(pokemon.getEvolutionMode(), level);
    }

    public static boolean isPointValidForSpawn(World world, Vector3 point, PokedexEntry dbe)
    {
        int i = point.intX();
        int j = point.intY();
        int k = point.intZ();
        if (!checkNoSpawnerInArea(world, i, j, k)) { return false; }
        boolean validLocation = canPokemonSpawnHere(point, world, dbe);
        return validLocation;
    }

    public static void loadFunctionFromString(String args)
    {
        String[] strings = args.split(":");
        if (strings.length == 0) return;
        int id = Integer.parseInt(strings[0]);
        functions.put(id, strings[1]);
    }

    public static void loadFunctionsFromStrings(String[] args)
    {
        for (String s : args)
        {
            loadFunctionFromString(s);
        }
    }

    private static void parseExpression(JEP parser, String toParse, double xValue, double yValue, boolean r,
            boolean isNew)
    {
        if (isNew)
        {
            parser.initFunTab(); // clear the contents of the function table
            parser.addStandardFunctions();
            parser.initSymTab(); // clear the contents of the symbol table
            parser.addStandardConstants();
            parser.addComplex(); // among other things adds i to the symbol
                                 // table
            if (!r)
            {
                parser.addVariable("x", xValue);
                parser.addVariable("y", yValue);
            }
            else
            {
                parser.addVariable("r", xValue);
            }
            parser.parseExpression(toParse);
        }
        else
        {
            if (!r)
            {
                parser.setVarValue("x", xValue);
                parser.setVarValue("y", yValue);
            }
            else
            {
                parser.setVarValue("r", xValue);
            }
        }
    }

    public static boolean removeForbiddenSpawningCoord(BlockPos pos, int dimensionId)
    {
        return removeForbiddenSpawningCoord(pos.getX(), pos.getY(), pos.getZ(), dimensionId);
    }

    public static boolean removeForbiddenSpawningCoord(int x, int y, int z, int dim)
    {
        ChunkCoordinate coord = new ChunkCoordinate(x, y, z, dim);
        return forbiddenSpawningCoords.remove(coord) != null;
    }

    public JEP parser = new JEP();
    Vector3    v      = Vector3.getNewVector();
    Vector3    v1     = Vector3.getNewVector();
    Vector3    v2     = Vector3.getNewVector();

    Vector3    v3     = Vector3.getNewVector();

    public SpawnHandler()
    {
        if (PokecubeMod.core.getConfig().pokemonSpawn) MinecraftForge.EVENT_BUS.register(this);
    }

    private int doSpawnForLocation(World world, Vector3 v)
    {
        int ret = 0;
        if (!v.doChunksExist(world, 32)) return ret;
        int radius = PokecubeMod.core.getConfig().maxSpawnRadius;
        int num = 0;
        int height = v.getMaxY(world);
        AxisAlignedBB box = v.getAABB();
        List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class,
                box.expand(radius, Math.max(height, radius), radius));
        for (Object o : list)
        {
            if (o instanceof IPokemob) num++;
        }
        boolean player = Tools.isAnyPlayerInRange(radius, 10, world, v);
        if (num > MAX_DENSITY * MAXNUM || !player) return ret;
        if (v.y < 0 || !checkNoSpawnerInArea(world, v.intX(), v.intY(), v.intZ())) return ret;
        refreshTerrain(v, world);
        TerrainSegment t = TerrainManager.getInstance().getTerrian(world, v);
        int b = t.getBiome(v);
        if (onlySubbiomes && b <= 255) { return ret; }
        List<PokedexEntry> entries = Database.spawnables;
        int index = world.rand.nextInt(entries.size());
        PokedexEntry dbe = entries.get(index);
        float weight = dbe.getSpawnData().getWeight(dbe.getSpawnData().getMatcher(world, v));
        double random = Math.random();
        Vector3.movePointOutOfBlocks(v, world);
        int n = 0;
        int max = entries.size();
        while (weight <= random && n++ < max)
        {
            dbe = entries.get((++index) % entries.size());
            if (!dbe.flys())
            {
                v = Vector3.getNextSurfacePoint2(world, v, Vector3.secondAxisNeg, v.y);
                if (v != null) Vector3.movePointOutOfBlocks(v, world);
            }
            weight = dbe.getSpawnData().getWeight(dbe.getSpawnData().getMatcher(world, v));
        }
        if (random > weight) return ret;
        if (v == null) { return ret; }
        if (dbe.legendary)
        {
            int level = getSpawnLevel(world, v, dbe);
            if (level < PokecubeMod.core.getConfig().minLegendLevel) { return ret; }
        }
        if (!isPointValidForSpawn(world, v, dbe)) return ret;
        num = 0;
        long time = System.nanoTime();

        ret += num = doSpawnForType(world, v, dbe, parser, t);

        double dt = (System.nanoTime() - time) / 10e3D;
        if (dt > 2000)
        {
            String toLog = "location:" + v + " took:" + dt + "\u00B5" + "s" + " for:" + dbe + " spawned:" + num;
            PokecubeMod.log(toLog);
        }
        return ret;
    }

    private int doSpawnForType(World world, Vector3 loc, PokedexEntry dbe, JEP parser, TerrainSegment t)
    {
        SpawnData entry = dbe.getSpawnData();

        int totalSpawnCount = 0;
        Vector3 offset = v1.clear();
        Vector3 point = v2.clear();
        SpawnBiomeMatcher matcher = entry.getMatcher(world, loc);
        byte distGroupZone = 6;
        Random rand = new Random();

        int n = Math.max(entry.getMax(matcher) - entry.getMin(matcher), 1);
        int spawnNumber = entry.getMin(matcher) + rand.nextInt(n);

        for (int i = 0; i < spawnNumber; i++)
        {
            offset.set(rand.nextInt(distGroupZone) - rand.nextInt(distGroupZone), rand.nextInt(1) - rand.nextInt(1),
                    rand.nextInt(distGroupZone) - rand.nextInt(distGroupZone));
            v.set(loc);
            point.set(v.addTo(offset));
            Vector3.movePointOutOfBlocks(point, world);
            if (point == null || !isPointValidForSpawn(world, point, dbe)) continue;

            float x = (float) point.x + 0.5F;
            float y = (float) point.y;
            float z = (float) point.z + 0.5F;

            float var28 = x - world.getSpawnPoint().getX();
            float var29 = y - world.getSpawnPoint().getY();
            float var30 = z - world.getSpawnPoint().getZ();
            float distFromSpawnPoint = var28 * var28 + var29 * var29 + var30 * var30;

            if (!checkNoSpawnerInArea(world, (int) x, (int) y, (int) z)) continue;
            float dist = PokecubeMod.core.getConfig().minSpawnRadius;
            boolean player = Tools.isAnyPlayerInRange(dist, dist, world, point);
            if (player) continue;
            if (distFromSpawnPoint >= 256.0F)
            {
                EntityLiving entityliving = null;
                try
                {
                    if (dbe.getPokedexNb() > 0)
                    {
                        entityliving = (EntityLiving) PokecubeMod.core.createEntityByPokedexEntry(dbe, world);
                        entityliving.setHealth(entityliving.getMaxHealth());
                        entityliving.setLocationAndAngles((double) x + 0.5F, (double) y + 0.5F, (double) z + 0.5F,
                                world.rand.nextFloat() * 360.0F, 0.0F);
                        if (entityliving.getCanSpawnHere())
                        {
                            if ((entityliving = creatureSpecificInit(entityliving, world, x, y, z,
                                    v3.set(entityliving))) != null)
                            {
                                SpawnEvent.Post evt = new SpawnEvent.Post(dbe, v3, world, (IPokemob) entityliving);
                                MinecraftForge.EVENT_BUS.post(evt);
                                world.spawnEntityInWorld(entityliving);
                                totalSpawnCount++;
                            }
                        }
                        else
                        {
                            entityliving.setDead();
                        }
                    }
                }
                catch (Throwable e)
                {
                    if (entityliving != null)
                    {
                        entityliving.setDead();
                    }

                    System.err.println("Wrong Id while spawn: " + dbe.getName());
                    e.printStackTrace();

                    return totalSpawnCount;
                }
            }

        }
        return totalSpawnCount;
    }

    public void spawn(World world)
    {
        if (world.getDifficulty() == EnumDifficulty.PEACEFUL || !doSpawns) return;
        List<EntityPlayer> players = new ArrayList<EntityPlayer>(world.playerEntities);
        if (players.isEmpty()) return;
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++)
        {
            Vector3 v = getRandomSpawningPointNearEntity(world, players.get(0),
                    PokecubeMod.core.getConfig().maxSpawnRadius, 0);
            AxisAlignedBB box = v.getAABB();
            int radius = PokecubeMod.core.getConfig().maxSpawnRadius;
            int height = v.getMaxY(world);
            List<EntityLivingBase> list = world.getEntitiesWithinAABB(EntityLivingBase.class,
                    box.expand(radius, Math.max(height, radius), radius));
            if (v != null && list.size() < MAXNUM * MAX_DENSITY)
            {
                long time = System.nanoTime();
                int num = doSpawnForLocation(world, v);
                double dt = (System.nanoTime() - time) / 10e3D;
                if (dt > 2000)
                {
                    PokecubeMod.log(dt + "\u00B5" + "s for player " + players.get(0).getDisplayNameString() + " at " + v
                            + ", spawned " + num);
                }
            }
        }
    }

    public static void refreshTerrain(Vector3 location, World world)
    {
        TerrainSegment t = TerrainManager.getInstance().getTerrian(world, location);
        Vector3 temp1 = Vector3.getNewVector();
        int x0 = t.chunkX * 16, y0 = t.chunkY * 16, z0 = t.chunkZ * 16;
        int dx = GRIDSIZE / 2;
        int dy = GRIDSIZE / 2;
        int dz = GRIDSIZE / 2;
        int x1 = x0 + dx, y1 = y0 + dy, z1 = z0 + dz;
        // outer:
        for (int i = x1; i < x1 + 16; i += GRIDSIZE)
            for (int j = y1; j < y1 + 16; j += GRIDSIZE)
                for (int k = z1; k < z1 + 16; k += GRIDSIZE)
                {
                    temp1.set(i, j, k);
                    int biome = t.getBiome(i, j, k);
                    if (biomeToRefresh.apply(biome))
                    {
                        biome = t.adjustedCaveBiome(world, temp1);
                        if (biome == -1) biome = t.adjustedNonCaveBiome(world, temp1);
                        t.setBiome(i, j, k, biome);
                    }
                }
    }

    public void tick(World world)
    {
        if (PokecubeCore.isOnClientSide())
        {
            System.out.println(FMLCommonHandler.instance().getEffectiveSide());
            return;
        }
        try
        {
            spawn(world);
            if (PokecubeMod.core.getConfig().meteors)
            {
                if (!world.provider.isSurfaceWorld()) return;
                if (world.provider.getHasNoSky()) return;

                List<Object> players = new ArrayList<Object>(world.playerEntities);
                if (players.size() < 1) return;
                if (Math.random() > 0.999)
                {
                    Random rand = new Random();
                    Entity player = (Entity) players.get(rand.nextInt(players.size()));
                    int dx = rand.nextInt(200) - 100;
                    int dz = rand.nextInt(200) - 100;

                    Vector3 v = this.v.set(player).add(dx, 0, dz);
                    if (world.getClosestPlayer(v.x, v.y, v.z, 96, false) != null) return;

                    v.add(0, 255 - player.posY, 0);

                    if (PokecubeSerializer.getInstance().canMeteorLand(v))
                    {
                        Vector3 direction = v1.set(rand.nextGaussian() / 2, -1, rand.nextGaussian() / 2);
                        Vector3 location = Vector3.getNextSurfacePoint(world, v, direction, 255);

                        if (location != null)
                        {
                            float energy = (float) Math.abs((rand.nextGaussian() + 1) * 50);
                            ExplosionCustom boom = new ExplosionCustom(world, PokecubeMod.getFakePlayer(world),
                                    location, energy).setMeteor(true);
                            String message = "Meteor at " + v + " with Direction of " + direction + " and energy of "
                                    + energy;
                            System.out.println(message);
                            boom.doExplosion();
                            PokecubeSerializer.getInstance().addMeteorLocation(v);

                        }
                    }
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
