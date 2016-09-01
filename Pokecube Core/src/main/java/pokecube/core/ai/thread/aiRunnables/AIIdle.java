package pokecube.core.ai.thread.aiRunnables;

import java.util.Random;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.EnumFacing;
import pokecube.core.database.PokedexEntry;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import thut.api.TickHandler;
import thut.api.maths.Vector3;

public class AIIdle extends AIBase
{
    final private EntityLiving entity;
    final IPokemob             mob;
    final PokedexEntry         entry;
    private double             xPosition;
    private double             yPosition;
    private double             zPosition;
    private double             speed;
    private double             maxLength = 16;

    Vector3                    v         = Vector3.getNewVector();
    Vector3                    v1        = Vector3.getNewVector();

    public AIIdle(EntityLiving entity)
    {
        this.entity = entity;
        this.setMutex(2);
        mob = (IPokemob) entity;
        entry = mob.getPokedexEntry();
        this.speed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
    }

    private void doFloatingIdle()
    {
        v.set(xPosition, yPosition, zPosition);
        Vector3 temp = Vector3.getNextSurfacePoint(world, v, Vector3.secondAxisNeg, v.y);
        if (temp == null) return;
        yPosition = temp.y + entry.preferedHeight;
    }

    private void doFlyingIdle()
    {
        boolean sitting = mob.getPokemonAIState(IMoveConstants.SITTING);
        boolean tamed = mob.getPokemonAIState(IMoveConstants.TAMED) && !mob.getPokemonAIState(IMoveConstants.STAYING);
        boolean up = Math.random() < 0.9;
        if (sitting && up && !tamed)
        {
            mob.setPokemonAIState(IMoveConstants.SITTING, false);
        }
        else if (Math.random() > 0.95 && !tamed)
        {
            boolean down = true;
            Vector3 loc = Vector3.findNextSolidBlock(world, v.set(mob), Vector3.secondAxisNeg, v.y);
            if (loc != null && loc.offsetBy(EnumFacing.UP).getBlockMaterial(world).isLiquid())
            {
                down = false;
            }
            else if (loc == null)
            {
                down = false;
            }
            if (down) mob.setPokemonAIState(IMoveConstants.SITTING, true);
        }
        else if (Math.random() > 0.75) doGroundIdle();
    }

    private void doGroundIdle()
    {
        v.set(xPosition, yPosition, zPosition);
        for (int i = 0; i > -yPosition; i--)
        {
            if (!v.addTo(0, i, 0).isAir(world))
            {
                break;
            }
        }
        yPosition = v.y;
    }

    public void doStationaryIdle()
    {
        xPosition = entity.posX;
        yPosition = entity.posY;
        zPosition = entity.posZ;
    }

    public void doWaterIdle()
    {

    }

    @Override
    public void reset()
    {
    }

    @Override
    public void run()
    {
        if (entry.flys())
        {
            doFlyingIdle();
        }
        else if (entry.floats())
        {
            doFloatingIdle();
        }
        else if (entry.swims() && entity.isInWater())
        {
            doWaterIdle();
        }
        else if (entry.isStationary)
        {
            doStationaryIdle();
        }
        else
        {
            doGroundIdle();
        }
        v1.set(entity);
        v.set(this.xPosition, this.yPosition, this.zPosition);

        if (v.isEmpty() || v1.distToSq(v) <= 1 || mob.getPokemonAIState(IMoveConstants.SITTING)) return;

        mob.setPokemonAIState(IMoveConstants.IDLE, true);
        Path path = this.entity.getNavigator().getPathToXYZ(this.xPosition, this.yPosition, this.zPosition);
        if (path != null && path.getCurrentPathLength() > maxLength) path = null;
        addEntityPath(entity.getEntityId(), entity.dimension, path, speed);
        mob.setPokemonAIState(IMoveConstants.IDLE, false);
    }

    @Override
    public boolean shouldRun()
    {
        Path current = null;
        world = TickHandler.getInstance().getWorldCache(entity.dimension);

        if (world == null || mob.getPokedexEntry().isStationary || mob.getPokemonAIState(IMoveConstants.EXECUTINGMOVE)
                || entity.getAttackTarget() != null || mob.getPokemonAIState(IMoveConstants.PATHING))
            return false;
        if ((current = entity.getNavigator().getPath()) != null && entity.getNavigator().noPath())
        {
            addEntityPath(entity.getEntityId(), entity.dimension, null, speed);
            current = null;
        }
        if (current != null && entity.getAttackTarget() == null)
        {
            v.set(current.getFinalPathPoint());
            v1.set(entity);
            double diff = 4 * entity.width;// TODO refine tis to using length
                                           // too
            diff = Math.max(2, diff);
            if (v.distToSq(v1) < diff)
            {
                addEntityPath(entity.getEntityId(), entity.dimension, null, speed);
                return false;
            }
            else
            {
                return false;
            }
        }
        if (entity.getAttackTarget() != null || !entity.getNavigator().noPath()) return false;

        mob.getPokedexEntry().flys();
        if (mob.getPokemonAIState(IMoveConstants.SITTING) && mob.getPokemonAIState(IMoveConstants.TAMED)
                && !mob.getPokemonAIState(IMoveConstants.STAYING))
            return false;
        if (mob.getPokemonAIState(IMoveConstants.SLEEPING)) return false;
        else if (this.entity.isBeingRidden() || current != null)
        {
            return false;
        }
        else if (entity.ticksExisted % (50 + new Random(mob.getRNGValue()).nextInt(50)) == 0)
        {
            boolean tameFactor = mob.getPokemonAIState(IMoveConstants.TAMED)
                    && !mob.getPokemonAIState(IMoveConstants.STAYING);
            int distance = (int) (maxLength = tameFactor ? 8 : 16);
            v.clear();
            if (!tameFactor)
            {
                if (mob.getHome() == null
                        || (mob.getHome().getX() == 0 && mob.getHome().getY() == 0 & mob.getHome().getZ() == 0))
                {
                    v1.set(mob);
                    mob.setHome(v1.intX(), v1.intY(), v1.intZ(), 16);
                }
                distance = (int) Math.min(distance, mob.getHomeDistance());
                v.set(mob.getHome());
            }
            else
            {
                EntityLivingBase setTo = entity;
                if (mob.getPokemonOwner() != null) setTo = mob.getPokemonOwner();
                v.set(setTo);
            }
            v1.set((v.isEmpty() && mob.getPokemonAIState(IMoveConstants.STAYING)) ? entity : v);
            Vector3 v = SpawnHandler.getRandomPointNear(world, v1, distance);

            double diff = Math.max(mob.getPokedexEntry().length * mob.getSize(),
                    mob.getPokedexEntry().width * mob.getSize());

            diff = Math.max(2, diff);
            if (v == null || this.v.distToSq(v) < diff)
            {
                return false;
            }
            else
            {
                this.xPosition = v.x;
                this.yPosition = Math.round(v.y);
                this.zPosition = v.z;
                return true;
            }
        }
        return false;
    }

}
