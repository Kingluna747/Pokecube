package pokecube.adventures.ai.trainers;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import pokecube.adventures.comands.Config;
import pokecube.adventures.entity.helper.EntityHasAIStates;
import pokecube.adventures.entity.trainers.EntityLeader;
import pokecube.adventures.entity.trainers.EntityTrainer;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.moves.MovesUtils;
import pokecube.core.utils.PokeType;
import thut.api.maths.Vector3;

public class EntityAITrainer extends EntityAIBase
{

    World                             world;

    // The entity (normally a player) that is the target of this trainer.
    Class<? extends EntityLivingBase> targetClass;
    Vector3                           loc = Vector3.getNewVector();

    // The trainer Entity
    final EntityTrainer               trainer;

    public EntityAITrainer(EntityTrainer trainer, Class<? extends EntityLivingBase> targetClass)
    {
        this.trainer = trainer;
        this.world = trainer.getEntityWorld();
        this.setMutexBits(3);
        this.targetClass = targetClass;
    }

    private boolean checkPokemobTarget()
    {
        if (trainer.getTarget() != null)
        {
            if (!trainer.outMob.getPokemonAIState(IMoveConstants.ANGRY)
                    || ((EntityLiving) trainer.outMob).getAttackTarget() == null)
            {
                ((EntityLiving) trainer.outMob).setAttackTarget(trainer.getTarget());
            }
        }
        return ((EntityLiving) trainer.outMob).getAttackTarget() instanceof IPokemob;
    }

    private void considerSwapMove()
    {
        // TODO choose between damaging/stats/status moves
        setMostDamagingMove();
    }

    private boolean considerSwapPokemob()
    {
        // TODO check if the target pokemob is bad matchup, consider swapping to
        // better choice.
        return false;
    }

    void doAggression()
    {
        boolean angry = trainer.getTarget() != null;
        if (angry)
        {
            if (!Vector3.isVisibleEntityFromEntity(trainer, trainer.getTarget()))
            {
                angry = false;
                trainer.setAIState(EntityTrainer.INBATTLE, true);
                trainer.setTarget(null);
                return;
            }
        }
        else
        {
            return;
        }
        if (trainer instanceof EntityLeader)
        {
            if (((EntityLeader) trainer).hasDefeated(trainer.getTarget()))
            {
                trainer.setTarget(null);
                return;
            }
        }
        if (angry && !trainer.getEntityWorld().isRemote && trainer.outMob == null)
        {
            trainer.throwCubeAt(trainer.getTarget());
        }
    }

    private int getPower(String move, IPokemob user, Entity target)
    {
        Move_Base attack = MovesUtils.getMoveFromName(move);
        int pwr = attack.getPWR(user, target);
        if (target instanceof IPokemob)
        {
            IPokemob mob = (IPokemob) target;
            pwr *= PokeType.getAttackEfficiency(attack.getType(user), mob.getType1(), mob.getType2());
        }
        return pwr;
    }

    /** Resets the task */
    @Override
    public void resetTask()
    {
        trainer.resetPokemob();
    }

    private void setMostDamagingMove()
    {
        IPokemob outMob = trainer.outMob;
        int index = outMob.getMoveIndex();
        int max = 0;
        Entity target = ((EntityLiving) outMob).getAttackTarget();
        String[] moves = outMob.getMoves();
        for (int i = 0; i < 4; i++)
        {
            String s = moves[i];
            if (s != null)
            {
                int temp = getPower(s, outMob, target);
                if (temp > max)
                {
                    index = i;
                    max = temp;
                }
            }
        }
        outMob.setMoveIndex(index);
    }

    @Override
    public boolean shouldExecute()
    {
        trainer.lowerCooldowns();
        if (!trainer.isEntityAlive()) return false;
        if (trainer.getTarget() != null && trainer.getTarget().isDead)
        {
            trainer.setTarget(null);
            resetTask();
            return false;
        }
        if (trainer.getTarget() != null || trainer.cooldown > trainer.getEntityWorld().getTotalWorldTime()) return true;
        Vector3 here = loc.set(trainer);
        EntityLivingBase target = null;
        List<? extends EntityLivingBase> targets = world.getEntitiesWithinAABB(targetClass,
                here.getAABB().expand(16, 16, 16));
        for (Object o : targets)
        {
            EntityLivingBase e = (EntityLivingBase) o;
            if (Vector3.isVisibleEntityFromEntity(trainer, e))
            {
                target = e;
                break;
            }
        }
        if (target == null)
        {
            if (trainer.outID != null || trainer.outMob != null)
            {
                resetTask();
            }
            return false;
        }

        boolean onCooldown = trainer.attackCooldown > 0;
        if (onCooldown) return false;

        if (target instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) target;
            if (player.capabilities.isCreativeMode) target = null;
            else if (trainer.friendlyCooldown > 0) target = null;
            else if (trainer instanceof EntityLeader)
            {
                if (((EntityLeader) trainer).hasDefeated(target)) target = null;
            }
        }
        if (target != trainer.getTarget()) trainer.setTarget(target);
        if (trainer.getTarget() == null) return false;
        return trainer.getTarget() != null;
    }

    /** Execute a one shot task or start executing a continuous task */
    @Override
    public void startExecuting()
    {
    }

    /** Updates the task */
    @Override
    public void updateTask()
    {
        if (trainer.getTarget() == null) return;
        trainer.faceEntity(trainer.getTarget(), trainer.rotationPitch, trainer.rotationYaw);
        boolean hasOut = trainer.outMob != null && !((Entity) trainer.outMob).isDead;
        if (trainer.attackCooldown > 0 && !hasOut && !trainer.getAIState(EntityHasAIStates.THROWING))
        {
            if (trainer.attackCooldown == Config.instance.trainerSendOutDelay / 2)
            {
                ItemStack nextStack = trainer.getNextPokemob();
                if (nextStack != null)
                {
                    IPokemob next = PokecubeManager.itemToPokemob(nextStack, world);
                    if (next != null)
                        trainer.getTarget().addChatMessage(new TextComponentTranslation("pokecube.trainer.next",
                                trainer.getDisplayName(), next.getPokemonDisplayName()));
                }
            }
            return;
        }
        if (trainer.getTarget() == null) return;
        double distance = trainer.getDistanceSqToEntity(trainer.getTarget());
        if (distance > 1024)
        {
            trainer.setTarget(null);
        }
        else if (trainer.outMob != null || trainer.outID != null)
        {
            if (checkPokemobTarget())
            {
                if (!considerSwapPokemob()) considerSwapMove();
            }
            else setMostDamagingMove();
        }
        else
        {
            doAggression();
        }
    }
}
