package pokecube.core.ai.utils;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import thut.api.maths.Vector3;
import thut.api.pathing.IPathingMob;
import thut.api.pathing.Paths;

public class PokeNavigator extends PathNavigate
{
    private final EntityLiving       theEntity;
    private final World              worldObj;
    /** The Path being followed. */
    private Path                     currentPath;
    private double                   speed;
    Vector3                          v               = Vector3.getNewVector();
    Vector3                          v1              = Vector3.getNewVector();
    Vector3                          v2              = Vector3.getNewVector();
    Vector3                          v3              = Vector3.getNewVector();
    /** The number of blocks (extra) +/- in each axis that get pulled out as
     * cache for the pathfinder's search space */
    private final IAttributeInstance pathSearchRange;
    private boolean                  noSunPathfind;
    /** Time, in number of ticks, following the current path */
    private int                      totalTicks;
    /** The time when the last position check was done (to detect successful
     * movement) */
    private int                      ticksAtLastPos;
    /** Coordinates of the entity's position last time a check was done (part of
     * monitoring getting 'stuck') */
    private Vec3d                    lastPosCheck    = new Vec3d(0.0D, 0.0D, 0.0D);
    /** If the entity can swim. Swimming AI enables this and the pathfinder will
     * also cause the entity to swim straight upwards when underwater */
    private boolean                  canSwim;

    private boolean                  canDive;

    private boolean                  canFly;
    public final Paths               pathfinder;

    final IPokemob                   pokemob;

    long                             lastCacheUpdate = 0;

    int                              sticks          = 0;

    public PokeNavigator(EntityLiving entity, World world)
    {
        super(entity, world);
        this.theEntity = entity;
        this.worldObj = world;
        this.pathSearchRange = entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
        pokemob = (IPokemob) entity;
        canSwim = true;
        canDive = ((IPathingMob) entity).swims();
        pathfinder = new Paths(world);
    }

    /** If on ground or swimming and can swim */
    @Override
    public boolean canNavigate()
    {
        if (pokemob.getPokemonAIState(IPokemob.SLEEPING) || pokemob.getStatus() == IPokemob.STATUS_SLP
                || pokemob.getStatus() == IPokemob.STATUS_FRZ)
            return false;
        if (pokemob.getPokemonAIState(IPokemob.SITTING)) return false;
        return this.theEntity.onGround || this.canSwim && this.isInLiquid() || this.canFly;
    }

    /** sets active PathHeap to null */
    @Override
    public synchronized void clearPathEntity()
    {
        this.currentPath = null;
    }

    @Override
    public Vec3d getEntityPosition()
    {
        return new Vec3d(this.theEntity.posX, this.getPathableYPos(), this.theEntity.posZ);
    }

    private int getNextPoint()
    {
        int index = getPath().getCurrentPathIndex();

        if (index + 1 >= getPath().getCurrentPathLength() || index == 0) return index;

        PathPoint current = getPath().getPathPointFromIndex(index - 1);
        PathPoint next = getPath().getPathPointFromIndex(index);
        v.set(current);
        v1.set(next);
        v2.set(v.subtractFrom(v1));
        while (index + 1 < getPath().getCurrentPathLength())
        {
            current = getPath().getPathPointFromIndex(index);
            next = getPath().getPathPointFromIndex(index + 1);
            if (!v2.equals(v.set(current).subtractFrom(v1.set(next)))) { return index; }
            index++;
        }
        return index;
    }

    /** gets the actively used PathHeap */
    @Override
    public Path getPath()
    {
        return this.currentPath;
    }

    /** Gets the safe pathing Y position for the entity depending on if it can
     * path swim or not */
    private int getPathableYPos()
    {
        boolean inWater = this.theEntity.isInWater();
        if (canDive && inWater)
        {
            return (int) (this.theEntity.posY + 0.5D);
        }
        else if (canFly && !inWater)
        {
            return (int) (this.theEntity.posY + 0.5D);
        }
        else if (inWater && this.canSwim)
        {
            int i = (int) this.theEntity.posY;
            Block block = this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.theEntity.posX), i,
                    MathHelper.floor_double(this.theEntity.posZ))).getBlock();
            int j = 0;

            do
            {
                if (block != Blocks.FLOWING_WATER && block != Blocks.WATER) { return i; }

                ++i;
                block = this.worldObj.getBlockState(new BlockPos(MathHelper.floor_double(this.theEntity.posX), i,
                        MathHelper.floor_double(this.theEntity.posZ))).getBlock();
                ++j;
            }
            while (j <= 16);

            return (int) this.theEntity.posY;
        }
        return (int) (this.theEntity.posY + 0.5D);

    }

    @Override
    protected PathFinder getPathFinder()
    {
        return null;
    }

    /** Gets the maximum distance that the path finding will search in. */
    @Override
    public float getPathSearchRange()
    {
        return (float) this.pathSearchRange.getAttributeValue();
    }

    /** Returns the path to the given EntityLiving */
    @Override
    public Path getPathToEntityLiving(Entity entity)
    {
        PokedexEntry entry = pokemob.getPokedexEntry();
        if (pokemob.getTransformedTo() instanceof IPokemob)
        {
            entry = ((IPokemob) pokemob.getTransformedTo()).getPokedexEntry();
        }
        this.canFly = entry.flys() || entry.floats();
        this.canDive = entry.swims();

        Path ret = null;

        if (this.canNavigate())
        {
            ret = pathfinder.getPathHeapToEntity(this.theEntity, entity, this.getPathSearchRange());
        }
        return ret;
    }

    @Override
    public Path getPathToPos(BlockPos pos)
    {
        PokedexEntry entry = pokemob.getPokedexEntry();
        if (pokemob.getTransformedTo() instanceof IPokemob)
        {
            entry = ((IPokemob) pokemob.getTransformedTo()).getPokedexEntry();
        }
        this.canFly = entry.flys() || entry.floats();
        this.canDive = entry.swims();
        Path current = getPath();
        if (current != null && !pokemob.getPokemonAIState(IMoveConstants.ANGRY))
        {
            Vector3 p = v.set(current.getFinalPathPoint());
            Vector3 v = v1.set(pos);
            if (p.distToSq(v) <= 1) { return current; }
        }
        Path ret = null;

        if (this.canNavigate())
        {
            ret = pathfinder.getEntityPathToXYZ(this.theEntity, pos.getX(), pos.getY(), pos.getZ(),
                    this.getPathSearchRange());
        }
        return ret;

    }

    /** Returns true when an entity of specified size could safely walk in a
     * straight line between the two points. Args: pos1, pos2, entityXSize,
     * entityYSize, entityZSize */
    @Override
    public boolean isDirectPathBetweenPoints(Vec3d start, Vec3d end, int sizeX, int sizeY, int sizeZ)
    {
        double d0 = end.xCoord - start.xCoord;
        double d1 = end.zCoord - start.zCoord;
        double dy = end.yCoord - start.yCoord;
        double d2 = d0 * d0 + d1 * d1 + dy * dy;
        if (d2 < 1.0E0D) { return true; }
        if (!canFly && dy != 0) return false;
        v.set(start);
        v1.set(end);
        Vector3 dir = v1.subtract(v);
        double dist = dir.mag();
        dir.scalarMultBy(1 / dist);
        IPathingMob pather = (IPathingMob) pokemob;
        for (int i = 0; i < dist; i++)
        {
            v1.set(v).add(dir.x * i, dir.y * i - 1, dir.z * i);
            if (pather.getBlockPathWeight(worldObj, v1) >= 40
                    || !pather.fits(worldObj, v1.addTo(0, 1, 0), null)) { return false; }
        }
        return true;
    }

    /** Returns true if the entity is in water or lava, false otherwise */
    @Override
    public boolean isInLiquid()
    {
        return theEntity.isInWater() || theEntity.isInLava();
    }

    /** If null path or reached the end */
    @Override
    public boolean noPath()
    {
        return this.getPath() == null || this.getPath().isFinished();
    }

    @Override
    public void onUpdateNavigation()
    {
        ++this.totalTicks;

        if (!this.noPath())
        {
            if (this.canNavigate())
            {
                try
                {
                    float f = this.theEntity.width;
                    f = Math.max(f, 0.5f);
                    v.set(theEntity);
                    v1.set(getPath().getFinalPathPoint());
                    if (v.distTo(v1) < f)
                    {
                        this.clearPathEntity();
                    }
                    else this.pathFollow();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            if (!this.noPath())
            {
                Vector3 targetLoc = Vector3.getNewVector();
                targetLoc.set(this.getPath().getPosition(this.theEntity));

                if (targetLoc.isEmpty()) { return; }

                float f = this.theEntity.width;
                f = Math.max(f, 0.5f);
                v.set(theEntity);
                v1.set(getPath().getFinalPathPoint());
                double dist;
                boolean loaded = worldObj.isAreaLoaded(v.getPos(), v1.getPos());

                if (!loaded || (dist = v.distTo(v1)) < f)
                {
                    this.clearPathEntity();
                    if (!loaded)
                    {
                        this.theEntity.motionX = 0;
                        this.theEntity.motionY = 0;
                        this.theEntity.motionZ = 0;
                        this.theEntity.getMoveHelper().setMoveTo(v.x, v.y, v.z, speed);
                    }
                    return;
                }
                dist = targetLoc.distTo(v);
                boolean end = targetLoc.sameBlock(v);
                int mult = end ? 5 : 2;
                float drop = 0.8f;
                double speed = this.speed;
                if (canDive && this.isInLiquid())
                {
                    speed *= 2;
                }
                while (speed * mult > dist)
                {
                    speed *= drop;
                }

                this.theEntity.getMoveHelper().setMoveTo(targetLoc.x, targetLoc.y, targetLoc.z, speed);
            }
        }
    }

    @Override
    public void pathFollow()
    {
        Vec3d vec3 = this.getEntityPosition();
        int i = this.getPath().getCurrentPathLength();
        for (int j = this.getPath().getCurrentPathIndex(); j < i; ++j)
        {
            PathPoint point;
            if ((point = this.getPath().getPathPointFromIndex(j)) == null) break;
            if (vec3.squareDistanceTo(point.xCoord, point.yCoord, point.zCoord) < 1)
            {
                this.getPath().setCurrentPathIndex(j);
                break;
            }
        }
        float f = this.theEntity.width * this.theEntity.width;
        f = Math.max(1, 0.15f);
        this.getPath().setCurrentPathIndex(getNextPoint());

        if (!((canFly || canSwim && theEntity.isInWater()) && !theEntity.onGround))
        {
            for (int j = this.getPath().getCurrentPathIndex(); j < i; ++j)
            {
                if (this.getPath().getPathPointFromIndex(j) == null) break;
                if (this.getPath().getPathPointFromIndex(j).yCoord != (int) vec3.yCoord)
                {
                    i = j;
                    break;
                }
            }
        }

        int k;

        for (k = this.getPath().getCurrentPathIndex(); k < i; ++k)
        {
            if (vec3.squareDistanceTo(this.getPath().getVectorFromIndex(this.theEntity, k)) < f)
            {
                this.getPath().setCurrentPathIndex(k + 1);
            }
        }

        for (k = this.getPath().getCurrentPathIndex() + 1; k < i - 1; ++k)
        {
            Vec3d test = this.getPath().getVectorFromIndex(theEntity, k);
            if (isDirectPathBetweenPoints(vec3, test, (int) Math.ceil(theEntity.width),
                    (int) Math.ceil(theEntity.height), (int) Math.ceil(theEntity.width)))
            {
                this.getPath().setCurrentPathIndex(k + 1);
            }
        }

        float max = 1.5f;
        if (pokemob.getPokemonAIState(IMoveConstants.ANGRY)) max = 0.5f;
        f = Math.max(f, max);
        v.set(theEntity);
        v1.set(getPath().getFinalPathPoint());
        if (v.distTo(v1) < f)
        {
            this.clearPathEntity();
            return;
        }

        f = Math.max(f, max);
        if (lastPosCheck.distanceTo(vec3) > f)
        {
            this.ticksAtLastPos = this.totalTicks;
            lastPosCheck = vec3;
        }

        int tickDiff = this.totalTicks - this.ticksAtLastPos;

        if (tickDiff > (pokemob.getPokemonAIState(IMoveConstants.IDLE) ? 100 : 200))
        {
            if (vec3.squareDistanceTo(this.lastPosCheck) < f)
            {
                sticks++;
                this.clearPathEntity();
            }
            this.ticksAtLastPos = this.totalTicks;
            lastPosCheck = vec3;
        }

        if (sticks > 100)
        {
            sticks = 0;
        }

    }

    public void refreshCache()
    {
    }

    /** Trims path data from the end to the first sun covered block */
    @Override
    public void removeSunnyPath()
    {
        if (!this.worldObj.canBlockSeeSky(new BlockPos(MathHelper.floor_double(this.theEntity.posX),
                (int) (this.theEntity.posY + 0.5D), MathHelper.floor_double(this.theEntity.posZ))))
        {
            for (int i = 0; i < this.getPath().getCurrentPathLength(); ++i)
            {
                PathPoint pathpoint = this.getPath().getPathPointFromIndex(i);

                if (this.worldObj.canBlockSeeSky(new BlockPos(pathpoint.xCoord, pathpoint.yCoord, pathpoint.zCoord)))
                {
                    this.getPath().setCurrentPathLength(i - 1);
                    return;
                }
            }
        }
    }

    /** sets the active path data if path is 100% unique compared to old path,
     * checks to adjust path for sun avoiding ents and stores end coords */
    @Override
    public boolean setPath(Path path, double speed)
    {
        if (path == getPath()) return true;
        if (path == null)
        {
            clearPathEntity();
            return false;
        }
        if (!path.isSamePath(this.getPath()))
        {
            this.currentPath = path;
        }

        if (this.noSunPathfind)
        {
            this.removeSunnyPath();
        }

        if (this.getPath().getCurrentPathLength() == 0) { return false; }
        this.speed = speed;
        Vec3d vec3 = this.getEntityPosition();
        this.ticksAtLastPos = this.totalTicks;
        lastPosCheck = vec3;
        return true;
    }

    /** Sets the speed */
    @Override
    public void setSpeed(double speed)
    {
        this.speed = speed;
    }

    /** Try to find and set a path to EntityLiving. Returns true if
     * successful. */
    @Override
    public boolean tryMoveToEntityLiving(Entity entity, double speed)
    {
        Path PathHeap = this.getPathToEntityLiving(entity);
        return PathHeap != null ? this.setPath(PathHeap, speed) : false;
    }

    /** Try to find and set a path to XYZ. Returns true if successful. */
    @Override
    public boolean tryMoveToXYZ(double x, double y, double z, double speed)
    {
        Path PathHeap = this.getPathToXYZ(MathHelper.floor_double(x), ((int) y), MathHelper.floor_double(z));
        return this.setPath(PathHeap, speed);
    }

}
