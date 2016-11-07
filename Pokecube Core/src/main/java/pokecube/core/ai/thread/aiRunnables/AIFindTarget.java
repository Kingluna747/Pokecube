package pokecube.core.ai.thread.aiRunnables;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import thut.api.TickHandler;
import thut.api.entity.IHungrymob;
import thut.api.entity.ai.AIThreadManager;
import thut.api.entity.ai.IAICombat;
import thut.api.maths.Vector3;

public class AIFindTarget extends AIBase implements IAICombat
{
    final IPokemob          pokemob;
    final IHungrymob        hungryMob;
    final EntityLiving      entity;
    Vector3                 v                = Vector3.getNewVector();
    Vector3                 v1               = Vector3.getNewVector();

    final Predicate<Object> validGuardTarget = new Predicate<Object>()
                                             {
                                                 @Override
                                                 public boolean apply(Object input)
                                                 {
                                                     if (input instanceof IPokemob && input != pokemob)
                                                     {
                                                         IPokemob mob = (IPokemob) input;
                                                         if (mob.getPokemobTeam() != pokemob
                                                                 .getPokemobTeam()) { return true; }
                                                     }
                                                     else if (PokecubeMod.pokemobsDamagePlayers
                                                             && input instanceof EntityLivingBase)
                                                     {
                                                         EntityLivingBase mob = (EntityLivingBase) input;

                                                         if (mob instanceof EntityVillager) return false;
                                                         if (mob instanceof EntityPokemobEgg) return false;

                                                         if (mob.getTeam() != pokemob.getPokemobTeam()) { return true; }
                                                     }
                                                     return false;
                                                 }
                                             };

    public AIFindTarget(EntityLivingBase mob)
    {
        this.pokemob = (IPokemob) mob;
        this.entity = (EntityLiving) mob;
        this.hungryMob = (IHungrymob) pokemob;
    }

    /** Returns the closest vulnerable player within the given radius, or null
     * if none is found. */
    EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double distance, int dimension)
    {
        double d4 = -1.0D;
        EntityPlayer entityplayer = null;

        Vector<?> playerEntities = AIThreadManager.worldPlayers.get(dimension);
        ArrayList<?> list = new ArrayList<Object>(playerEntities);
        if (list.isEmpty()) return null;

        for (int i = 0; i < list.size(); ++i)
        {
            if (!(list.get(i) instanceof EntityPlayer)) continue;

            EntityPlayer entityplayer1 = (EntityPlayer) list.get(i);

            if (!entityplayer1.capabilities.disableDamage && entityplayer1.isEntityAlive())
            {
                double d5 = entityplayer1.getDistanceSq(x, y, z);
                double d6 = distance;

                if (entityplayer1.isSneaking())
                {
                    d6 = distance * 0.800000011920929D;
                }

                if (entityplayer1.isInvisible())
                {
                    float f = entityplayer1.getArmorVisibility();

                    if (f < 0.1F)
                    {
                        f = 0.1F;
                    }

                    d6 *= 0.7F * f;
                }

                if ((distance < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4))
                {
                    d4 = d5;
                    entityplayer = entityplayer1;
                }
            }
        }

        return entityplayer;
    }

    /** Returns the closest vulnerable player to this entity within the given
     * radius, or null if none is found */
    EntityPlayer getClosestVulnerablePlayerToEntity(Entity entity, double distance)
    {
        return this.getClosestVulnerablePlayer(entity.posX, entity.posY, entity.posZ, distance, entity.dimension);
    }

    @Override
    public void reset()
    {

    }

    @Override
    public void run()
    {
        if (entity.getAttackTarget() == null && !pokemob.getPokemonAIState(IMoveConstants.STAYING)
                && pokemob.getPokemonAIState(IMoveConstants.TAMED) && !PokecubeCore.isOnClientSide())
        {
            List<Object> list = getEntitiesWithinDistance(entity, 16, EntityLivingBase.class);
            if (!list.isEmpty() && pokemob.getPokemonOwner() != null)
            {
                for (int j = 0; j < list.size(); j++)
                {
                    Entity entity = (Entity) list.get(j);

                    if (entity instanceof EntityCreature && ((EntityCreature) entity).getAttackTarget() != null
                            && ((EntityCreature) entity).getAttackTarget().equals(pokemob.getPokemonOwner())
                            && Vector3.isVisibleEntityFromEntity(entity, entity))
                    {
                        addTargetInfo(entity, entity);
                        setPokemobAIState(pokemob, IMoveConstants.ANGRY, true);
                        setPokemobAIState(pokemob, IMoveConstants.SITTING, false);
                        return;
                    }
                }
            }
        }

        if (entity.getAttackTarget() == null && !pokemob.getPokemonAIState(IMoveConstants.SITTING)
                && hungryMob.isCarnivore() && pokemob.getPokemonAIState(IMoveConstants.HUNTING))
        {
            List<Object> list = getEntitiesWithinDistance(entity, 16, EntityLivingBase.class);
            if (!list.isEmpty())
            {
                for (int j = 0; j < list.size(); j++)
                {
                    Entity entity = (Entity) list.get(j);

                    if (entity instanceof IPokemob
                            && pokemob.getPokedexEntry().isFood(((IPokemob) entity).getPokedexEntry())
                            && pokemob.getLevel() > ((IPokemob) entity).getLevel()
                            && Vector3.isVisibleEntityFromEntity(entity, entity))
                    {
                        addTargetInfo(entity, entity);
                        setPokemobAIState(pokemob, IMoveConstants.ANGRY, true);
                        setPokemobAIState(pokemob, IMoveConstants.SITTING, false);
                        return;
                    }
                }
            }
        }
        if (pokemob.getPokemonAIState(IMoveConstants.GUARDING))
        {
            List<EntityLivingBase> ret = new ArrayList<EntityLivingBase>();
            List<Object> pokemobs = new ArrayList<Object>();

            Vector3 centre = Vector3.getNewVector();
            if (pokemob.getPokemonAIState(IMoveConstants.STAYING) || pokemob.getPokemonOwner() == null)
                centre.set(pokemob.getHome());
            else centre.set(pokemob.getPokemonOwner());

            pokemobs = getEntitiesWithinDistance(centre, entity.dimension, 16, EntityLivingBase.class);

            for (Object o : pokemobs)
            {
                if (validGuardTarget.apply(o)) ret.add((EntityLivingBase) o);
            }
            EntityLivingBase newtarget = null;
            double closest = 1000;
            Vector3 here = v1.set(pokemob);
            for (EntityLivingBase e : ret)
            {
                double dist = e.getDistanceSqToEntity(entity);
                v.set(e);
                if (dist < closest && here.isVisible(world, v))
                {
                    closest = dist;
                    newtarget = e;
                }
            }
            if (newtarget != null && Vector3.isVisibleEntityFromEntity(entity, newtarget))
            {
                addTargetInfo(entity, newtarget);
                setPokemobAIState(pokemob, IMoveConstants.ANGRY, true);
                setPokemobAIState(pokemob, IMoveConstants.SITTING, false);
                return;
            }
        }
    }

    @Override
    public boolean shouldRun()
    {
        world = TickHandler.getInstance().getWorldCache(entity.dimension);
        if (world == null) return false;

        boolean ret = entity.getAITarget() == null && entity.getAttackTarget() == null
                && !pokemob.getPokemonAIState(IMoveConstants.SITTING);

        if (entity.getAttackTarget() != null && entity.getAttackTarget().isDead)
        {
            setPokemobAIState(pokemob, IMoveConstants.ANGRY, false);
            addTargetInfo(entity, null);
            return false;
        }

        boolean tame = pokemob.getPokemonAIState(IMoveConstants.TAMED);
        if (tame && entity.getAttackTarget() != null)
        {
            Entity owner = pokemob.getPokemonOwner();
            boolean stayOrGuard = pokemob.getPokemonAIState(IMoveConstants.GUARDING)
                    || pokemob.getPokemonAIState(IMoveConstants.STAYING);
            if (owner != null && !stayOrGuard
                    && owner.getDistanceToEntity(entity) > PokecubeMod.core.getConfig().chaseDistance)
            {
                setPokemobAIState(pokemob, IMoveConstants.ANGRY, false);
                addTargetInfo(entity, null);
                return false;
            }
        }
        if (ret && !tame && entity.getRNG().nextInt(200) == 0)
        {
            EntityPlayer player = getClosestVulnerablePlayerToEntity(entity,
                    PokecubeMod.core.getConfig().mobAggroRadius);

            if (player != null && Vector3.isVisibleEntityFromEntity(entity, player))
            {
                setPokemobAIState(pokemob, IMoveConstants.ANGRY, true);
                addTargetInfo(entity, player);
                return false;
            }
        }
        return ret;
    }

}
