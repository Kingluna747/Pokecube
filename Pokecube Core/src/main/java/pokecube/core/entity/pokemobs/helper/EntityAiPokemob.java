/**
 *
 */
package pokecube.core.entity.pokemobs.helper;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.ai.pokemob.PokemobAIHurt;
import pokecube.core.ai.pokemob.PokemobAILook;
import pokecube.core.ai.pokemob.PokemobAIUtilityMove;
import pokecube.core.ai.pokemob.PokemobSitShoulder;
import pokecube.core.ai.thread.aiRunnables.AIAttack;
import pokecube.core.ai.thread.aiRunnables.AICombatMovement;
import pokecube.core.ai.thread.aiRunnables.AIFindTarget;
import pokecube.core.ai.thread.aiRunnables.AIFollowOwner;
import pokecube.core.ai.thread.aiRunnables.AIGatherStuff;
import pokecube.core.ai.thread.aiRunnables.AIGuardEgg;
import pokecube.core.ai.thread.aiRunnables.AIHungry;
import pokecube.core.ai.thread.aiRunnables.AIIdle;
import pokecube.core.ai.thread.aiRunnables.AIMate;
import pokecube.core.ai.thread.aiRunnables.AIStoreStuff;
import pokecube.core.ai.thread.logicRunnables.LogicCollision;
import pokecube.core.ai.thread.logicRunnables.LogicFloatFlySwim;
import pokecube.core.ai.thread.logicRunnables.LogicInLiquid;
import pokecube.core.ai.thread.logicRunnables.LogicInMaterials;
import pokecube.core.ai.thread.logicRunnables.LogicMiscUpdate;
import pokecube.core.ai.thread.logicRunnables.LogicMountedControl;
import pokecube.core.ai.thread.logicRunnables.LogicMovesUpdates;
import pokecube.core.ai.utils.AISaveHandler;
import pokecube.core.ai.utils.GuardAI;
import pokecube.core.ai.utils.PokeNavigator;
import pokecube.core.ai.utils.PokemobJumpHelper;
import pokecube.core.ai.utils.PokemobMoveHelper;
import pokecube.core.blocks.nests.TileEntityNest;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.EntityPokemob;
import pokecube.core.events.InitAIEvent;
import pokecube.core.events.handlers.EventsHandler;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.HappinessType;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.moves.PokemobDamageSource;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer;
import thut.api.entity.ai.AIThreadManager;
import thut.api.entity.ai.AIThreadManager.AIStuff;
import thut.api.entity.ai.ILogicRunnable;
import thut.api.maths.Vector3;
import thut.lib.CompatWrapper;

/** @author Manchou */
public abstract class EntityAiPokemob extends EntityMountablePokemob
{

    public EntityAiPokemob(World world)
    {
        super(world);
        here = Vector3.getNewVector();
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase p_82196_1_, float p_82196_2_)
    {
        // TODO decide if I want to do something here?
    }

    @Override
    public boolean canBreatheUnderwater()
    {
        return (pokemobCap.getType1() == PokeType.getType("water") || pokemobCap.getType2() == PokeType.getType("water")
                || pokemobCap.getPokedexEntry().shouldDive || pokemobCap.getPokedexEntry().swims());
    }

    @Override
    public void fall(float distance, float damageMultiplier)
    {
        PokedexEntry entry = pokemobCap.getPokedexEntry();
        boolean canFloat = entry.floats() || entry.flys() || pokemobCap.canUseFly();
        if (distance > 4 + height) distance = 0;
        if (distance < 5) damageMultiplier = 0;
        if (!canFloat) super.fall(distance, damageMultiplier);
    }

    /** Checks if the entity's current position is a valid location to spawn
     * this entity. */
    @Override
    public boolean getCanSpawnHere()
    {
        return this.getEntityWorld().checkNoEntityCollision(this.getEntityBoundingBox());
    }

    @Override
    public EntityMoveHelper getMoveHelper()
    {
        if (pokemobCap.mover != null) return pokemobCap.mover;
        return super.getMoveHelper();
    }

    ////////////////////////// Death Related things////////////////////////

    @Override
    public PathNavigate getNavigator()
    {
        if (pokemobCap.navi != null) return pokemobCap.navi;

        return super.getNavigator();
    }

    /////////////////////// Target related
    /////////////////////// things//////////////////////////////////

    @Override
    /** Called when a user uses the creative pick block button on this entity.
     *
     * @param target
     *            The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, Null if nothing
     *         should be added. */
    public ItemStack getPickedResult(RayTraceResult target)
    {
        return ItemPokemobEgg.getEggStack(pokemobCap);
    }

    /////////////////// Movement related things///////////////////////////

    ////////////////////////////// Misc////////////////////////////////////////////////////////////////
    @Override
    /** Get number of ticks, at least during which the living entity will be
     * silent. */
    public int getTalkInterval()
    {// TODO config option for this maybe?
        return 400;
    }

    /*
     * Override to fix bad detection of isInWater for little mobs and to skip
     * the handle of water movement on water mobs
     */
    @Override
    public boolean handleWaterMovement()
    {
        if (isInWater())
        {
            if (!this.inWater)
            {
                if (!pokemobCap.swims())
                {
                    float f = MathHelper.sqrt(this.motionX * this.motionX * 0.20000000298023224D
                            + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.20000000298023224D) * 0.2F;

                    if (f > 1.0F)
                    {
                        f = 1.0F;
                    }

                    this.playSound(SoundEvents.ENTITY_GENERIC_SWIM, f,
                            1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
                    float f1 = MathHelper.floor(this.getEntityBoundingBox().minY);
                    int i;
                    float f2;
                    float f3;

                    for (i = 0; i < 1.0F + this.width * 20.0F; ++i)
                    {
                        f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        f3 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        this.getEntityWorld().spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX + f2, f1 + 1.0F,
                                this.posZ + f3, this.motionX, this.motionY - this.rand.nextFloat() * 0.2F,
                                this.motionZ);
                    }

                    for (i = 0; i < 1.0F + this.width * 20.0F; ++i)
                    {
                        f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        f3 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        this.getEntityWorld().spawnParticle(EnumParticleTypes.WATER_SPLASH, this.posX + f2, f1 + 1.0F,
                                this.posZ + f3, this.motionX, this.motionY, this.motionZ);
                    }
                }
            }

            this.fallDistance = 0.0F;
            this.inWater = true;
            extinguish();
        }
        else
        {
            this.inWater = false;
        }

        return isInWater();
    }

    @Override
    public void init(int nb)
    {
        super.init(nb);
        if (getEntityWorld() != null) initAI(pokemobCap.getPokedexEntry());
    }

    protected void initAI(PokedexEntry entry)
    {
        pokemobCap.navi = new PokeNavigator(this, getEntityWorld());
        pokemobCap.mover = new PokemobMoveHelper(this);
        jumpHelper = new PokemobJumpHelper(this);
        pokemobCap.aiStuff = new AIStuff(this);

        float moveSpeed = 0.5f;
        float speedFactor = (float) (1 + Math.sqrt(entry.getStatVIT()) / (100F));
        moveSpeed *= speedFactor;
        if (entry.flys()) moveSpeed /= 1.25f;

        this.getNavigator().setSpeed(moveSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(moveSpeed);

        // Add in the vanilla like AI methods.
        pokemobCap.guardAI = new GuardAI(this, this.getCapability(EventsHandler.GUARDAI_CAP, null));
        this.tasks.addTask(5, pokemobCap.guardAI);
        this.tasks.addTask(5, pokemobCap.utilMoveAI = new PokemobAIUtilityMove(this));
        this.tasks.addTask(8, new PokemobAILook(this, EntityPlayer.class, 8.0F, 1f));
        if (PokecubeCore.core.getConfig().pokemobsOnShoulder)
            this.tasks.addTask(8, new PokemobSitShoulder((EntityPokemob) this));
        this.targetTasks.addTask(3, new PokemobAIHurt(this, entry.isSocial));

        // Add in the various logic AIs that are needed on both client and
        // server.
        pokemobCap.aiStuff.addAILogic(new LogicInLiquid(pokemobCap));
        pokemobCap.aiStuff.addAILogic(new LogicCollision(pokemobCap));
        pokemobCap.aiStuff.addAILogic(new LogicMovesUpdates(this));
        pokemobCap.aiStuff.addAILogic(new LogicInMaterials(this));
        pokemobCap.aiStuff.addAILogic(new LogicFloatFlySwim(this));
        pokemobCap.aiStuff.addAILogic(new LogicMiscUpdate(this));

        // Controller is done separately for ease of locating it for controls.
        pokemobCap.aiStuff.addAILogic(pokemobCap.controller = new LogicMountedControl(pokemobCap));

        if (getEntityWorld().isRemote) return;

        // Add in the Custom type of AI tasks.
        pokemobCap.aiStuff.addAITask(new AIAttack(this).setPriority(200));
        pokemobCap.aiStuff.addAITask(new AICombatMovement(this).setPriority(250));
        if (!entry.isStationary)
        {
            pokemobCap.aiStuff
                    .addAITask(new AIFollowOwner(this, 2 + this.width + this.length, 2 + this.width + this.length)
                            .setPriority(400));
        }
        pokemobCap.aiStuff.addAITask(new AIGuardEgg(this).setPriority(250));
        pokemobCap.aiStuff.addAITask(new AIMate(this).setPriority(300));
        pokemobCap.aiStuff.addAITask(new AIHungry(this, new EntityItem(getEntityWorld()), 16).setPriority(300));
        AIStoreStuff ai = new AIStoreStuff(this);
        pokemobCap.aiStuff.addAITask(ai.setPriority(350));
        pokemobCap.aiStuff.addAITask(new AIGatherStuff(this, 32, ai).setPriority(400));
        pokemobCap.aiStuff.addAITask(new AIIdle(this).setPriority(500));
        pokemobCap.aiStuff.addAITask(new AIFindTarget(this).setPriority(400));

        // Send notification event of AI initilization, incase anyone wants to
        // affect it.
        MinecraftForge.EVENT_BUS.post(new InitAIEvent(this));
    }

    @Override
    /** Checks if the entity is in range to render by using the past in distance
     * and comparing it to its average edge length * 64 * renderDistanceWeight
     * Args: distance */
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance)
    {
        double d0 = this.getEntityBoundingBox().getAverageEdgeLength();

        if (Double.isNaN(d0))
        {
            d0 = 1.0D;
        }
        d0 = Math.max(1, d0);

        d0 = d0 * 64.0D * 10;
        return distance < d0 * d0;
    }

    /** Checks if this entity is inside water (if inWater field is true as a
     * result of handleWaterMovement() returning true) */
    @Override
    public boolean isInWater()
    {
        return pokemobCap.getPokemonAIState(IMoveConstants.INWATER);
    }

    @Override
    public void jump()
    {
        if (getEntityWorld().isRemote) return;
        boolean ladder = this.isOnLadder();
        if (!ladder && !this.isInWater() && !this.isInLava())
        {
            if (!this.onGround) return;

            boolean pathing = !this.getNavigator().noPath();
            double factor = 0.1;
            if (pathing)
            {
                Path path = this.getNavigator().getPath();
                Vector3 point = Vector3.getNewVector().set(path.getPathPointFromIndex(path.getCurrentPathIndex()));
                factor = 0.05 * point.distTo(here);
                factor = Math.max(0.2, factor);
            }
            // The extra factor fixes tiny pokemon being unable to jump up one
            // block.
            this.motionY += 0.5D + factor * 1 / pokemobCap.getPokedexEntry().height;

            Potion jump = Potion.getPotionFromResourceLocation("jump_boost");

            if (this.isPotionActive(jump))
            {
                this.motionY += (this.getActivePotionEffect(jump).getAmplifier() + 1) * 0.1F;
            }
            if (isBeingRidden())
            {
                motionY += 0.3;
            }

            if (this.isSprinting())
            {
                float f = this.rotationYaw * 0.017453292F;
                this.motionX -= MathHelper.sin(f) * 0.2F;
                this.motionZ += MathHelper.cos(f) * 0.2F;
            }

            this.isAirBorne = true;
            ForgeHooks.onLivingJump(this);
        }
        else
        {
            this.motionY += ladder ? 0.1 : 0.03999999910593033D;
        }
    }

    @Override
    /** Moves the entity based on the specified heading. Args: strafe, up,
     * forward */// TODO fix minor bugs here.
    public void travel(float strafe, float up, float forward)
    {
        PokedexEntry entry = pokemobCap.getPokedexEntry();
        IPokemob transformed = CapabilityPokemob.getPokemobFor(pokemobCap.getTransformedTo());
        if (transformed != null)
        {
            entry = transformed.getPokedexEntry();
        }
        int aiState = pokemobCap.getTotalAIState();
        boolean isAbleToFly = entry.floats() || entry.flys();
        boolean isWaterMob = entry.swims();

        if (!(isAbleToFly || (isWaterMob && isInWater())))
        {
            super.travel(strafe, up, forward);
            return;
        }

        if (this.isServerWorld() || this.canPassengerSteer())
        {
            if (!this.isInWater())
            {
                if (!this.isInLava())
                {
                    float f6 = 0.91F;
                    BlockPos.PooledMutableBlockPos blockpos$pooledmutableblockpos = BlockPos.PooledMutableBlockPos
                            .retain(this.posX, this.getEntityBoundingBox().minY - 1.0D, this.posZ);

                    if (this.onGround)
                    {
                        f6 = this.world.getBlockState(blockpos$pooledmutableblockpos).getBlock().slipperiness * 0.91F;
                    }

                    float f7 = 0.16277136F / (f6 * f6 * f6);
                    float f8;

                    if (this.onGround || isAbleToFly)
                    {
                        f8 = this.getAIMoveSpeed() * f7;
                    }
                    else
                    {
                        f8 = this.jumpMovementFactor;
                    }

                    this.moveRelative(strafe, up, forward, f8);
                    f6 = 0.91F;

                    if (this.onGround)
                    {
                        f6 = this.world.getBlockState(blockpos$pooledmutableblockpos.setPos(this.posX,
                                this.getEntityBoundingBox().minY - 1.0D, this.posZ)).getBlock().slipperiness * 0.91F;
                    }

                    if (this.isOnLadder())
                    {
                        this.motionX = MathHelper.clamp(this.motionX, -0.15000000596046448D, 0.15000000596046448D);
                        this.motionZ = MathHelper.clamp(this.motionZ, -0.15000000596046448D, 0.15000000596046448D);
                        this.fallDistance = 0.0F;

                        if (this.motionY < -0.15D)
                        {
                            this.motionY = -0.15D;
                        }
                    }

                    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

                    if (this.isCollidedHorizontally && this.isOnLadder())
                    {
                        this.motionY = 0.2D;
                    }

                    if (this.isPotionActive(MobEffects.LEVITATION))
                    {
                        this.motionY += (0.05D
                                * (double) (this.getActivePotionEffect(MobEffects.LEVITATION).getAmplifier() + 1)
                                - this.motionY) * 0.2D;
                    }
                    else
                    {
                        blockpos$pooledmutableblockpos.setPos(this.posX, 0.0D, this.posZ);

                        if (!this.world.isRemote || this.world.isBlockLoaded(blockpos$pooledmutableblockpos)
                                && this.world.getChunkFromBlockCoords(blockpos$pooledmutableblockpos).isLoaded())
                        {
                            if (!this.hasNoGravity()
                                    && (!isAbleToFly || this.getAIState(IMoveConstants.SITTING, aiState)
                                            || this.getAIState(IMoveConstants.SLEEPING, aiState)))
                            {
                                this.motionY -= 0.08D;
                            }
                        }
                        else if (this.posY > 0.0D)
                        {
                            this.motionY = -0.1D;
                        }
                        else
                        {
                            this.motionY = 0.0D;
                        }
                    }

                    this.motionY *= isAbleToFly ? f6 : 0.9800000190734863D;
                    this.motionX *= (double) f6;
                    this.motionZ *= (double) f6;
                    blockpos$pooledmutableblockpos.release();
                }
                else
                {
                    double d4 = this.posY;
                    this.moveRelative(strafe, up, forward, 0.02F);
                    this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
                    this.motionX *= 0.5D;
                    this.motionY *= 0.5D;
                    this.motionZ *= 0.5D;

                    if (!this.hasNoGravity())
                    {
                        this.motionY -= 0.02D;
                    }

                    if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX,
                            this.motionY + 0.6000000238418579D - this.posY + d4, this.motionZ))
                    {
                        this.motionY = 0.30000001192092896D;
                    }
                }
            }
            else
            {
                double d0 = this.posY;
                float f1 = this.getWaterSlowDown();
                float f2 = 0.02F;
                float f3 = (float) EnchantmentHelper.getDepthStriderModifier(this);
                if (isWaterMob) f3 *= 2.5;

                if (f3 > 3.0F)
                {
                    f3 = 3.0F;
                }

                if (!this.onGround)
                {
                    f3 *= 0.5F;
                }

                if (f3 > 0.0F)
                {
                    f1 += (0.54600006F - f1) * f3 / 3.0F;
                    f2 += (this.getAIMoveSpeed() - f2) * f3 / 3.0F;
                }
                this.moveRelative(strafe, up, forward, f2);
                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
                this.motionX *= (double) f1;
                this.motionY *= 0.800000011920929D;
                this.motionZ *= (double) f1;

                if (!this.hasNoGravity() && !isWaterMob)
                {
                    this.motionY -= 0.02D;
                }

                if (!isWaterMob && this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX,
                        this.motionY + 0.6000000238418579D - this.posY + d0, this.motionZ))
                {
                    this.motionY = 0.30000001192092896D;
                }
            }
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        double d5 = this.posX - this.prevPosX;
        double d7 = this.posZ - this.prevPosZ;
        double d9 = isAbleToFly ? this.posY - this.prevPosY : 0.0D;
        float f10 = MathHelper.sqrt(d5 * d5 + d9 * d9 + d7 * d7) * 4.0F;

        if (f10 > 1.0F)
        {
            f10 = 1.0F;
        }

        this.limbSwingAmount += (f10 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    public void moveRelative(float strafe, float up, float forward, float friction)
    {
        float f = strafe * strafe + up * up + forward * forward;

        if (f >= 1.0E-4F)
        {
            f = MathHelper.sqrt(f);

            if (f < 1.0F)
            {
                f = 1.0F;
            }

            f = friction / f;
            strafe = strafe * f;
            up = up * f;
            forward = forward * f;
            float f1 = MathHelper.sin(this.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(this.rotationYaw * 0.017453292F);
            this.motionX += (double) (strafe * f2 - forward * f1);
            this.motionY += (double) up;
            this.motionZ += (double) (forward * f2 + strafe * f1);
        }
    }

    @Override
    public void onDeath(DamageSource damageSource)
    {
        if (ForgeHooks.onLivingDeath(this, damageSource)) return;
        Entity entity = damageSource.getTrueSource();
        EntityLivingBase entitylivingbase = this.getAttackingEntity();

        if (this.scoreValue >= 0 && entitylivingbase != null)
        {
            entitylivingbase.awardKillScore(this, this.scoreValue, damageSource);
        }
        if (entity != null)
        {
            if (damageSource instanceof PokemobDamageSource)
            {
                ((PokemobDamageSource) damageSource).getImmediateSource().onKillEntity(this);
            }
            else entity.onKillEntity(this);
        }
        else if (getAttackTarget() != null)
        {
            getAttackTarget().onKillEntity(this);
        }
        this.dead = true;
        this.getCombatTracker().reset();

        if (!this.getEntityWorld().isRemote)
        {
            int i = 0;

            if (entity instanceof EntityPlayer)
            {
                i = EnchantmentHelper.getLootingModifier((EntityLivingBase) entity);
            }

            captureDrops = true;
            capturedDrops.clear();

            boolean shadowDrop = (pokemobCap.isShadow() && pokemobCap.getLevel() < 40);

            if (this.canDropLoot() && this.world.getGameRules().getBoolean("doMobLoot") && !shadowDrop)
            {
                this.dropFewItems(this.recentlyHit > 0, i);
                this.dropEquipment(this.recentlyHit > 0, i);

                if (recentlyHit > 0 && !pokemobCap.getPokemonAIState(IPokemob.TAMED))
                {
                    int i1 = this.getExperiencePoints(this.attackingPlayer);
                    i1 = net.minecraftforge.event.ForgeEventFactory.getExperienceDrop(this, this.attackingPlayer, i1);
                    while (i1 > 0)
                    {
                        int j = EntityXPOrb.getXPSplit(i1);
                        i1 -= j;
                        this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, j));
                    }
                }
            }
            captureDrops = false;
            if (!net.minecraftforge.common.ForgeHooks.onLivingDrops(this, damageSource, capturedDrops, i,
                    recentlyHit > 0))
            {
                for (EntityItem item : capturedDrops)
                {
                    world.spawnEntity(item);
                }
            }
        }

        this.world.setEntityState(this, (byte) 3);
    }

    @Override
    protected void onDeathUpdate()
    {
        if (!PokecubeCore.isOnClientSide() && pokemobCap.getPokemonAIState(IMoveConstants.TAMED))
        {
            HappinessType.applyHappiness(pokemobCap, HappinessType.FAINT);
            ITextComponent mess = new TextComponentTranslation("pokemob.action.faint.own",
                    pokemobCap.getPokemonDisplayName());
            pokemobCap.displayMessageToOwner(mess);
            pokemobCap.returnToPokecube();
        }
        if (!pokemobCap.getPokemonAIState(IMoveConstants.TAMED))
        {
            AISaveHandler.instance().removeAI(this);
            if (this.getHeldItemMainhand() != CompatWrapper.nullStack) PokecubeItems.deValidate(getHeldItemMainhand());
        }
        super.onDeathUpdate();
    }

    @Override
    public void onLivingUpdate()
    {
        if (this.isOnLadder())
        {
            onGround = true;
        }
        super.onLivingUpdate();
        if (isServerWorld() && isPokemonShaking && !isPokemonWet && !hasPath() && onGround)
        {
            isPokemonWet = true;
            timePokemonIsShaking = 0.0F;
            prevTimePokemonIsShaking = 0.0F;
            getEntityWorld().setEntityState(this, (byte) 8);
        }
    }

    //////////////// Jumping related//////////////////////////

    @Override
    public void onUpdate()
    {
        if (pokemobCap.getPokedexEntry().floats() || pokemobCap.getPokedexEntry().flys()) fallDistance = 0;
        dimension = getEntityWorld().provider.getDimension();
        super.onUpdate();
        for (ILogicRunnable logic : pokemobCap.aiStuff.aiLogic)
        {
            logic.doServerTick(getEntityWorld());
        }
        if (egg != null && egg.isDead)
        {
            egg = null;
        }
        headRotationOld = headRotation;
        if (looksWithInterest)
        {
            headRotation = headRotation + (1.0F - headRotation) * 0.4F;
        }
        else
        {
            headRotation = headRotation + (0.0F - headRotation) * 0.4F;
        }
        if (looksWithInterest)
        {

        }
        if (isWet() && !(pokemobCap.canUseSurf()))
        {
            isPokemonShaking = true;
            isPokemonWet = false;
            timePokemonIsShaking = 0.0F;
            prevTimePokemonIsShaking = 0.0F;
        }
        else if ((isPokemonShaking || isPokemonWet) && isPokemonWet)
        {
            prevTimePokemonIsShaking = timePokemonIsShaking;
            timePokemonIsShaking += 0.05F;
            if (prevTimePokemonIsShaking >= 2.0F)
            {
                isPokemonShaking = false;
                isPokemonWet = false;
                prevTimePokemonIsShaking = 0.0F;
                timePokemonIsShaking = 0.0F;
            }
            if (timePokemonIsShaking > 0.4F && !pokemobCap.swims())
            {
                float f = (float) posY;
                int i = (int) (MathHelper.sin((timePokemonIsShaking - 0.4F) * (float) Math.PI) * 7F);

                for (int j = 0; j < i; j++)
                {
                    float f1 = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                    float f2 = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                    getEntityWorld().spawnParticle(EnumParticleTypes.WATER_SPLASH, posX + f1, f + 0.8F, posZ + f2,
                            motionX, motionY, motionZ);
                }
            }
        }
    }

    /////////////////////////// Interaction
    /////////////////////////// logic/////////////////////////////////////////////////////
    // 1.11
    public boolean processInteract(EntityPlayer player, EnumHand hand)
    {
        return processInteract(player, hand, player.getHeldItem(hand));
    }

    // 1.10
    public boolean processInteract(EntityPlayer player, EnumHand hand, ItemStack held)
    {
        return false;
    }

    @Override
    public void setAttackTarget(EntityLivingBase entity)
    {
        if (entity != null && entity.equals(pokemobCap.getPokemonOwner())) { return; }
        if (entity != null && entity.equals(this)) { return; }
        super.setAttackTarget(entity);
    }

    @Override
    public void setDead()
    {
        if (addedToChunk)
        {
            PokecubeSerializer.getInstance().removePokemob(pokemobCap);
            AISaveHandler.instance().removeAI(this);
            if (pokemobCap.getHome() != null && pokemobCap.getHome().getY() > 0
                    && getEntityWorld().isAreaLoaded(pokemobCap.getHome(), 2))
            {
                TileEntity te = getEntityWorld().getTileEntity(pokemobCap.getHome());
                if (te != null && te instanceof TileEntityNest)
                {
                    TileEntityNest nest = (TileEntityNest) te;
                    nest.removeResident(pokemobCap);
                }
            }
        }
        super.setDead();
    }

    @Override
    public void setJumping(boolean jump)
    {
        if (!getEntityWorld().isRemote)
        {
            pokemobCap.setPokemonAIState(IMoveConstants.JUMPING, jump);
        }
        else
        {
            isJumping = pokemobCap.getPokemonAIState(IMoveConstants.JUMPING);
        }
    }

    @Override
    protected void updateEntityActionState()
    {
        ++this.idleTime;
        this.getEntityWorld().profiler.startSection("checkDespawn");
        this.despawnEntity();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("sensing");
        this.senses.clearSensingCache();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("targetSelector");
        this.targetTasks.onUpdateTasks();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("goalSelector");
        this.tasks.onUpdateTasks();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("navigation");
        this.getNavigator().onUpdateNavigation();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("mob tick");
        // Run last tick's results from AI stuff
        pokemobCap.aiStuff.runServerThreadTasks(getEntityWorld());
        // Schedule AIStuff to tick for next tick.
        AIThreadManager.scheduleAITick(pokemobCap.aiStuff);
        this.updateAITasks();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.startSection("controls");
        this.getEntityWorld().profiler.startSection("move");
        this.getMoveHelper().onUpdateMoveHelper();
        this.getEntityWorld().profiler.endStartSection("look");
        this.getLookHelper().onUpdateLook();
        this.getEntityWorld().profiler.endStartSection("jump");
        this.getJumpHelper().doJump();
        this.getEntityWorld().profiler.endSection();
        this.getEntityWorld().profiler.endSection();
    }

    @Override
    public AIStuff getAI()
    {
        return pokemobCap.getAI();
    }

    @Override
    public boolean selfManaged()
    {
        return pokemobCap.selfManaged();
    }

    @Override
    public void setSwingingArms(boolean swingingArms)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void setJumpPower(int jumpPowerIn)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean canJump()
    {
        return true;
    }

    @Override
    public void handleStartJump(int p_184775_1_)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleStopJump()
    {
        // TODO Auto-generated method stub

    }
}
