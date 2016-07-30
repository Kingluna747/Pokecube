package pokecube.core.ai.thread.aiRunnables;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.IChatComponent;
import pokecube.core.ai.thread.IAICombat;
import pokecube.core.commands.CommandTools;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import pokecube.core.utils.Tools;
import thut.api.TickHandler;
import thut.api.entity.IMultibox;
import thut.api.maths.Matrix3;
import thut.api.maths.Vector3;

public class AIAttack extends AIBase implements IAICombat
{
    final EntityLiving attacker;
    EntityLivingBase   entityTarget;
    Vector3            targetLoc   = Vector3.getNewVector();
    Move_Base          attack;
    Matrix3            targetBox   = new Matrix3();
    Matrix3            attackerBox = new Matrix3();

    Vector3            v           = Vector3.getNewVector();
    Vector3            v1          = Vector3.getNewVector();
    Vector3            v2          = Vector3.getNewVector();
    double             movementSpeed;

    private int        chaseTime;
    private int        delayTime   = -1;

    boolean            running     = false;

    public AIAttack(EntityLiving par1EntityLiving)
    {
        this.attacker = par1EntityLiving;
        this.movementSpeed = attacker.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue();
        this.setMutex(3);
    }

    private void applyDelay(boolean distanced)
    {
        byte[] mods = ((IPokemob) attacker).getModifiers();
        int cd = PokecubeMod.core.getConfig().attackCooldown;
        int def = distanced ? cd : cd / 2;
        if (entityTarget instanceof EntityPlayer) def *= distanced ? 3 : 2;
        double accuracyMod = Tools.modifierToRatio(mods[6], true);
        delayTime = (int) (def / accuracyMod);
    }

    private void checkMateFight(IPokemob pokemob)
    {
        if (pokemob.getPokemonAIState(IMoveConstants.MATEFIGHT))
        {
            if (entityTarget instanceof IPokemob)
            {
                IPokemob target = (IPokemob) entityTarget;
                if (((EntityLiving) target).getHealth() < ((EntityLiving) target).getMaxHealth() / 1.5f)
                {
                    setPokemobAIState((IPokemob) attacker, IMoveConstants.MATEFIGHT, false);
                    setPokemobAIState(target, IMoveConstants.MATEFIGHT, false);
                    addTargetInfo(attacker, null);
                    pokemob.setPokemonAIState(IMoveConstants.ANGRY, false);
                    ((EntityLiving) target).setAttackTarget(null);
                    target.setPokemonAIState(IMoveConstants.ANGRY, false);
                }
            }
            else
            {
                setPokemobAIState((IPokemob) attacker, IMoveConstants.MATEFIGHT, false);
            }
        }
    }

    public boolean continueExecuting()
    {
        entityTarget = attacker.getAttackTarget();

        if (entityTarget != null && entityTarget.isDead)
        {
            addTargetInfo(attacker.getEntityId(), -1, attacker.dimension);
            entityTarget = null;
        }
        return entityTarget != null && !entityTarget.isDead;
    }

    @Override
    public void reset()
    {
        if (running)
        {
            running = false;
            delayTime = -1;
            addEntityPath(attacker.getEntityId(), attacker.dimension, null, movementSpeed);
        }
    }

    @Override
    public void run()
    {
        if (!continueExecuting())
        {
            reset();
            return;
        }
        PathEntity path;
        if (!running)
        {
            if (!(attack == null || ((attack.getAttackCategory() & IMoveConstants.CATEGORY_SELF) != 0))
                    && attacker.riddenByEntity == null)
            {
                path = this.attacker.getNavigator().getPathToEntityLiving(entityTarget);
                addEntityPath(attacker.getEntityId(), attacker.dimension, path, movementSpeed);
            }
            targetLoc.set(entityTarget);
            this.chaseTime = 0;
            running = true;
            if (PokecubeMod.core.getConfig().pokemobagresswarning && delayTime == -1
                    && entityTarget instanceof EntityPlayer
                    && !((IPokemob) attacker).getPokemonAIState(IMoveConstants.TAMED)
                    && ((EntityPlayer) entityTarget).getLastAttacker() != attacker
                    && ((EntityPlayer) entityTarget).getAITarget() != attacker)
            {
                delayTime = PokecubeMod.core.getConfig().pokemobagressticks;
                IChatComponent message = CommandTools.makeTranslatedMessage("pokemob.agress", "red",
                        ((IPokemob) attacker).getPokemonDisplayName());
                entityTarget.addChatMessage(message);
            }
            else
            {
                delayTime = 0;
            }
            return;
        }

        this.attacker.getLookHelper().setLookPositionWithEntity(entityTarget, 30.0F, 30.0F);

        IPokemob pokemob = (IPokemob) attacker;

        checkMateFight(pokemob);

        if (pokemob.getPokemonAIState(IMoveConstants.EXECUTINGMOVE) && targetLoc.isEmpty())
        {
            setPokemobAIState((IPokemob) attacker, IMoveConstants.EXECUTINGMOVE, false);
        }

        if (chaseTime > 200)
        {
            addTargetInfo(attacker.getEntityId(), -1, attacker.dimension);
            pokemob.setPokemonAIState(IMoveConstants.ANGRY, false);
            addEntityPath(attacker.getEntityId(), attacker.dimension, null, movementSpeed);
            return;
        }

        double var1 = (double) (this.attacker.width * 2.0F) * (this.attacker.width * 2.0F);
        boolean distanced = false;
        boolean self = false;
        Move_Base move = null;
        if (attacker instanceof IPokemob)
        {
            IPokemob mob = (IPokemob) attacker;
            move = MovesUtils.getMoveFromName(mob.getMove(mob.getMoveIndex()));

            if (mob.getPokemonAIState(IMoveConstants.HUNTING) && !pokemob.getPokemonAIState(IMoveConstants.TAMED))
            {
                if (move == null || move.getPWR(mob, entityTarget) <= 0)
                {
                    for (int i = 0; i < 4; i++)
                    {
                        String choice = mob.getMove(i);
                        if (choice != null && !choice.isEmpty())
                        {
                            move = MovesUtils.getMoveFromName(choice);
                            if (move != null && move.getPWR(mob, entityTarget) > 0)
                            {
                                mob.setMoveIndex(i);
                            }
                        }
                    }
                }
                move = MovesUtils.getMoveFromName(mob.getMove(mob.getMoveIndex()));
            }
            else if (mob.getPokemonAIState(IMoveConstants.GUARDING))
            {
                int index = mob.getMoveIndex();
                int max = 0;
                String[] moves = mob.getMoves();
                for (int i = 0; i < 4; i++)
                {
                    String s = moves[i];
                    if (s != null)
                    {
                        int temp = Tools.getPower(s, mob, entityTarget);
                        if (temp > max)
                        {
                            index = i;
                            max = temp;
                        }
                    }
                }
                if (index != mob.getMoveIndex()) mob.setMoveIndex(index);
            }

            if (move == null) move = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);

            if ((move.getAttackCategory() & IMoveConstants.CATEGORY_DISTANCE) > 0)
            {
                var1 = 256;
                distanced = true;
            }
            if ((move.getAttackCategory() & IMoveConstants.CATEGORY_SELF) > 0)
            {
                self = true;
            }
        }

        double dist = this.attacker.getDistanceSq(this.entityTarget.posX, this.entityTarget.posY,
                this.entityTarget.posZ);
        boolean canSee = dist < 1 || Vector3.isVisibleEntityFromEntity(attacker, entityTarget);

        boolean inRange = false;

        ((IMultibox) attacker).setBoxes();
        ((IMultibox) attacker).setOffsets();
        attackerBox.set(((IMultibox) attacker).getBoxes().get("main"));
        attackerBox.addOffsetTo(v2.set(((IMultibox) attacker).getOffsets().get("main")).addTo(v1.set(attacker)));
        if (distanced)
        {
            inRange = dist < var1;
        }
        else if (entityTarget instanceof IMultibox)
        {
            IMultibox target = (IMultibox) entityTarget;
            target.setBoxes();
            target.setOffsets();
            inRange = attackerBox.doCollision(v2.setToVelocity(attacker), (Entity) target);
        }
        else
        {
            targetBox.set(entityTarget.getEntityBoundingBox());
            inRange = attackerBox.intersects(targetBox);
        }
        if (self)
        {
            inRange = true;
            targetLoc.set(attacker);
        }

        if (!canSee)
        {
            chaseTime++;
        }
        else
        {
            chaseTime = 0;
            if (!pokemob.getPokemonAIState(IMoveConstants.EXECUTINGMOVE))
            {
                targetLoc.set(entityTarget).addTo(0, entityTarget.height / 2, 0);
            }
        }

        if (delayTime < -20)
        {
            applyDelay(distanced);
            addTargetInfo(attacker, entityTarget);
            ((IPokemob) attacker).setPokemonAIState(IMoveConstants.ANGRY, true);
            targetLoc.set(entityTarget);
        }
        boolean shouldPath = true;
        boolean delay = false;
        if ((inRange || self))
        {
            if (canSee || self)
            {
                if (delayTime <= 0)
                {
                    applyDelay(distanced);
                    delay = true;
                }
                shouldPath = false;
                setPokemobAIState((IPokemob) attacker, IMoveConstants.EXECUTINGMOVE, true);
            }
        }
        else
        {
            setPokemobAIState((IPokemob) attacker, IMoveConstants.EXECUTINGMOVE, false);
        }
        if (!delay && delayTime % 5 == 0)
        {
            addTargetInfo(attacker, entityTarget);
        }
        if (!targetLoc.isEmpty() && delay && inRange)
        {
            if ((entityTarget instanceof IPokemob
                    && !((IPokemob) entityTarget).getPokemonAIState(IMoveConstants.DODGING))
                    || !(entityTarget instanceof IPokemob) || attack.move.notIntercepable)
            {
                targetLoc.set(entityTarget).addTo(0, entityTarget.height / 2, 0);
            }
            else
            {

            }
            if (entityTarget instanceof IPokemob)
                setPokemobAIState((IPokemob) entityTarget, IMoveConstants.DODGING, false);
            if (this.attacker.getHeldItem() != null)
            {
                this.attacker.swingItem();
            }
            float f = (float) targetLoc.distToEntity(attacker);
            Vector3 loc = targetLoc.copy();
            addMoveInfo(attacker.getEntityId(), entityTarget.getEntityId(), attacker.dimension, loc, f);
            shouldPath = false;
            setPokemobAIState((IPokemob) attacker, IMoveConstants.EXECUTINGMOVE, false);
            targetLoc.clear();
        }
        if (!targetLoc.isEmpty() && shouldPath)
        {
            path = this.attacker.getNavigator().getPathToXYZ(targetLoc.x, targetLoc.y, targetLoc.z);
            if (path != null) addEntityPath(attacker.getEntityId(), attacker.dimension, path, movementSpeed);
        }
        else
        {
            addEntityPath(attacker.getEntityId(), attacker.dimension, null, movementSpeed);
        }
        delayTime--;
    }

    @Override
    public boolean shouldRun()
    {
        world = TickHandler.getInstance().getWorldCache(attacker.dimension);
        if (world == null) return false;
        EntityLivingBase var1 = attacker.getAttackTarget();
        if (var1 == null)
        {
            if (attacker.getNavigator().noPath()
                    && ((IPokemob) attacker).getPokemonAIState(IMoveConstants.EXECUTINGMOVE))
            {
                setPokemobAIState((IPokemob) attacker, IMoveConstants.EXECUTINGMOVE, false);
            }
            return false;
        }
        else if (var1.isDead)
        {
            return false;
        }
        else
        {
            attack = MovesUtils.getMoveFromName(((IPokemob) attacker).getMove(((IPokemob) attacker).getMoveIndex()));
            entityTarget = var1;
            if (attack == null) attack = MovesUtils.getMoveFromName(IMoveConstants.DEFAULT_MOVE);
            return true;
        }
    }
}
