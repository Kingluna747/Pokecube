package pokecube.core.ai.thread.aiRunnables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.utils.PokecubeSerializer;
import thut.api.entity.ai.AIThreadManager;
import thut.api.entity.ai.IAIRunnable;
import thut.api.maths.Vector3;
import thut.lib.ItemStackTools;

public abstract class AIBase implements IAIRunnable
{
    public static class PlaySound implements IRunnable
    {
        final int           dim;
        final Vector3       loc;
        final SoundEvent    sound;
        final SoundCategory cat;
        final float         volume;
        final float         pitch;

        public PlaySound(int dim, Vector3 loc, SoundEvent sound, SoundCategory cat, float volume, float pitch)
        {
            this.dim = dim;
            this.sound = sound;
            this.volume = volume;
            this.loc = loc;
            this.pitch = pitch;
            this.cat = cat;
        }

        @Override
        public boolean run(World world)
        {
            if (dim != world.provider.getDimension()) return false;
            world.playSound(null, loc.x, loc.y, loc.z, sound, cat, volume, pitch);
            return true;
        }

    }

    public static class InventoryChange implements IRunnable
    {
        public final int       entity;
        public final int       dim;
        public final int       slot;
        public final int       minSlot;
        public final ItemStack stack;

        public InventoryChange(Entity entity, int slot, ItemStack stack, boolean min)
        {
            this.entity = entity.getEntityId();
            this.dim = entity.dimension;
            this.stack = stack;
            if (min)
            {
                minSlot = slot;
                this.slot = -1;
            }
            else
            {
                this.slot = slot;
                minSlot = 0;
            }
        }

        @Override
        public boolean run(World world)
        {
            if (dim != world.provider.getDimension()) return false;
            Entity e = world.getEntityByID(entity);
            if (e == null || !(e instanceof IPokemob)) return false;
            if (slot > 0) ((IPokemob) e).getPokemobInventory().setInventorySlotContents(slot, stack);
            else if (!ItemStackTools.addItemStackToInventory(stack, ((IPokemob) e).getPokemobInventory(), minSlot))
            {
                e.entityDropItem(stack, 0);
            }
            return true;
        }

    }

    public static interface IRunnable
    {
        /** @param world
         * @return task ran sucessfully */
        boolean run(World world);
    }

    /** A thread-safe object used to set which move a pokemob is to use.
     * 
     * @author Thutmose */
    public static class MoveInfo implements IRunnable
    {
        public static final Comparator<MoveInfo> compare = new Comparator<MoveInfo>()
                                                         {
                                                             @Override
                                                             public int compare(MoveInfo o1, MoveInfo o2)
                                                             {
                                                                 if (o1.dim != o2.dim) return 0;
                                                                 if (FMLCommonHandler.instance()
                                                                         .getSide() == Side.SERVER)
                                                                 {
                                                                     WorldServer world = FMLCommonHandler.instance()
                                                                             .getMinecraftServerInstance()
                                                                             .worldServerForDimension(o1.dim);
                                                                     Entity e1 = world.getEntityByID(o1.attacker);
                                                                     Entity e2 = world.getEntityByID(o2.attacker);
                                                                     if (e1 instanceof IPokemob
                                                                             && e2 instanceof IPokemob) { return pokemobComparator
                                                                                     .compare((IPokemob) e1,
                                                                                             (IPokemob) e2); }
                                                                 }
                                                                 return 0;
                                                             }
                                                         };

        public final int                         attacker;
        public final int                         targetEnt;
        public final int                         dim;
        public final Vector3                     target;
        public final float                       distance;

        public MoveInfo(int _attacker, int _targetEnt, int dimension, Vector3 _target, float _distance)
        {
            attacker = _attacker;
            targetEnt = _targetEnt;
            target = _target;
            distance = _distance;
            dim = dimension;
        }

        @Override
        public boolean run(World world)
        {
            if (dim != world.provider.getDimension()) return false;
            Entity e = world.getEntityByID(attacker);
            if (e == null || !(e instanceof EntityLiving)) return false;

            EntityLiving mob = (EntityLiving) e;
            List<?> unloadedMobs = world.unloadedEntityList;
            if (!mob.getEntityWorld().loadedEntityList.contains(mob) || unloadedMobs.contains(mob)) return false;
            if (!(mob.isDead || mob.getHealth() <= 0))
            {
                ((IPokemob) mob).executeMove(world.getEntityByID(targetEnt), target, distance);
            }
            return true;
        }
    }

    /** A thread-safe object used to set the current path for an entity.
     * 
     * @author Thutmose */
    public static class PathInfo implements IRunnable
    {
        public final Path   path;
        public final int    pather;
        public final int    dim;
        public final double speed;

        public PathInfo(int _pather, int dimension, Path _path, double _speed)
        {
            path = _path;
            pather = _pather;
            speed = _speed;
            dim = dimension;
        }

        @Override
        public boolean run(World world)
        {
            if (dim != world.provider.getDimension()) return false;
            Entity e = world.getEntityByID(pather);
            if (e == null || !(e instanceof EntityLiving)) { return false; }
            EntityLiving mob = (EntityLiving) e;
            List<?> unloadedMobs = world.unloadedEntityList;
            if (!mob.getEntityWorld().loadedEntityList.contains(mob) || unloadedMobs.contains(mob)) { return false; }

            if (!(mob.isDead || mob.getHealth() <= 0))
            {
                mob.getNavigator().setPath(path, speed);
            }
            return true;
        }
    }

    public static class StateInfo implements IRunnable
    {
        public final int     pokemobUid;
        public final int     state;
        public final boolean value;

        public StateInfo(int uid, int state_, boolean value_)
        {
            pokemobUid = uid;
            state = state_;
            value = value_;
        }

        @Override
        public boolean run(World world)
        {
            IPokemob pokemob = PokecubeSerializer.getInstance().getPokemob(pokemobUid);
            if (pokemob == null) return false;
            pokemob.setPokemonAIState(state, value);
            return true;
        }
    }

    /** A thread safe object used to set the attack target of an entity.
     * 
     * @author Thutmose */
    public static class TargetInfo implements IRunnable
    {
        public final int attacker;
        public final int target;
        public final int dim;

        public TargetInfo(int _attacker, int _target, int dimension)
        {
            attacker = _attacker;
            target = _target;
            dim = dimension;
        }

        @Override
        public boolean run(World world)
        {
            if (dim != world.provider.getDimension()) return false;
            Entity e = world.getEntityByID(attacker);
            Entity e1 = world.getEntityByID(target);
            if (e == null || !(e instanceof EntityLiving)) { return false; }
            if (!(e1 instanceof EntityLivingBase))
            {
                e1 = null;
            }
            EntityLiving mob = (EntityLiving) e;
            List<?> unloadedMobs = world.unloadedEntityList;

            boolean mobExists = !(unloadedMobs.contains(e));

            if (!mobExists || (!(e1 instanceof EntityPlayer)
                    && ((e1 != null && !e1.getEntityWorld().loadedEntityList.contains(e1))
                            || (e1 != null && unloadedMobs.contains(e1))))) { return false; }
            if (!(mob.isDead || mob.getHealth() <= 0))
            {
                mob.setAttackTarget((EntityLivingBase) e1);
            }
            return true;
        }
    }

    /** Sorts pokemobs by move order. */
    public static final Comparator<IPokemob> pokemobComparator = new Comparator<IPokemob>()
                                                               {
                                                                   @Override
                                                                   public int compare(IPokemob o1, IPokemob o2)
                                                                   {
                                                                       int speed1 = o1.getStat(Stats.VIT, true);
                                                                       int speed2 = o2.getStat(Stats.VIT, true);
                                                                       return speed2 - speed1;
                                                                   }
                                                               };

    protected IBlockAccess                   world;

    int                                      priority          = 0;

    int                                      mutex             = 0;

    protected Vector<IRunnable>              toRun             = new Vector<IRunnable>();

    protected Vector<IRunnable>              moves             = new Vector<IRunnable>();

    protected void addEntityPath(Entity entity, Path path, double speed)
    {
        toRun.add(new PathInfo(entity.getEntityId(), entity.dimension, path, speed));
    }

    /** threadsafe path determination.
     * 
     * @param id
     * @param dim
     * @param path
     * @param speed */
    protected void addEntityPath(int id, int dim, Path path, double speed)
    {
        toRun.add(new PathInfo(id, dim, path, speed));
    }

    /** Thread safe attack setting */
    protected void addMoveInfo(int attacker, int targetEnt, int dim, Vector3 target, float distance)
    {
        if (!moves.isEmpty()) System.err.println("adding duplicate move");
        else moves.add(new MoveInfo(attacker, targetEnt, dim, target, distance));
    }

    /** Thread safe AI state setting
     * 
     * @param uid
     * @param state
     * @param value */
    protected void addStateInfo(int uid, int state, boolean value)
    {
        toRun.add(new StateInfo(uid, state, value));
    }

    protected void addTargetInfo(Entity attacker, Entity target)
    {
        int targetId = target == null ? -1 : target.getEntityId();
        addTargetInfo(attacker.getEntityId(), targetId, attacker.dimension);
    }

    /** Thread safe target swapping information.
     * 
     * @param attacker
     * @param target
     * @param dim */
    protected void addTargetInfo(int attacker, int target, int dim)
    {
        toRun.add(new TargetInfo(attacker, target, dim));
    }

    @Override
    public void doMainThreadTick(World world)
    {
        ArrayList<IRunnable> runs = Lists.newArrayList();
        runs.addAll(toRun);
        for (IRunnable run : runs)
        {
            boolean ran = run.run(world);
            if (ran)
            {
                toRun.remove(run);
            }
        }
        runs = Lists.newArrayList();
        runs.addAll(moves);
        for (IRunnable run : runs)
        {
            boolean ran = run.run(world);
            if (ran)
            {
                moves.remove(run);
            }
        }
    }

    protected List<Object> getEntitiesWithinDistance(Entity source, float distance, Class<?>... targetClass)
    {
        Vector<?> entities = AIThreadManager.worldEntities.get(source.dimension);
        List<Object> list = new ArrayList<Object>();
        double dsq = distance * distance;
        if (entities != null)
        {
            List<?> temp = new ArrayList<Object>(entities);
            for (Object o : temp)
            {
                boolean correctClass = true;
                for (Class<?> claz : targetClass)
                {
                    correctClass = correctClass && claz.isInstance(o);
                }
                if (correctClass)
                {
                    if (source.getDistanceSqToEntity((Entity) o) < dsq)
                    {
                        list.add(o);
                    }
                }
            }
        }
        return list;
    }

    List<Object> getEntitiesWithinDistance(Vector3 source, int dimension, float distance, Class<?>... targetClass)
    {
        Vector<?> entities = AIThreadManager.worldEntities.get(dimension);
        List<Object> list = new ArrayList<Object>();
        if (entities != null)
        {
            List<?> temp = new ArrayList<Object>(entities);
            for (Object o : temp)
            {
                boolean correctClass = true;
                for (Class<?> claz : targetClass)
                {
                    correctClass = correctClass && claz.isInstance(o);
                }
                if (correctClass)
                {
                    if (source.distToEntity(((Entity) o)) < distance)
                    {
                        list.add(o);
                    }
                }
            }
        }
        return list;
    }

    @Override
    public int getMutex()
    {
        return mutex;
    }

    @Override
    public int getPriority()
    {
        return priority;
    }

    @Override
    public IAIRunnable setMutex(int mutex)
    {
        this.mutex = mutex;
        return this;
    }

    protected void setPokemobAIState(IPokemob pokemob, int state, boolean value)
    {
        addStateInfo(pokemob.getPokemonUID(), state, value);
    }

    @Override
    public IAIRunnable setPriority(int prior)
    {
        priority = prior;
        return this;
    }

}
