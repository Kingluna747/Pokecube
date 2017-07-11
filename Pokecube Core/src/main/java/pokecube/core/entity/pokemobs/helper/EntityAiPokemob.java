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
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
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
import pokecube.core.ai.utils.AISaveHandler.PokemobAI;
import pokecube.core.ai.utils.GuardAI;
import pokecube.core.ai.utils.PokeNavigator;
import pokecube.core.ai.utils.PokemobJumpHelper;
import pokecube.core.ai.utils.PokemobMoveHelper;
import pokecube.core.blocks.nests.TileEntityNest;
import pokecube.core.database.PokedexEntry;
import pokecube.core.events.EggEvent;
import pokecube.core.events.InitAIEvent;
import pokecube.core.events.handlers.EventsHandler;
import pokecube.core.handlers.Config;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.handlers.playerdata.PokecubePlayerStats;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemobUseable;
import pokecube.core.interfaces.Nature;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.ItemPokedex;
import pokecube.core.items.berries.ItemBerry;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.moves.PokemobDamageSource;
import pokecube.core.moves.PokemobTerrainEffects;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.Tools;
import thut.api.entity.ai.AIThreadManager;
import thut.api.entity.ai.AIThreadManager.AIStuff;
import thut.api.entity.ai.ILogicRunnable;
import thut.api.maths.Vector3;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;
import thut.lib.CompatWrapper;

/** @author Manchou */
public abstract class EntityAiPokemob extends EntityMountablePokemob
{

    public GuardAI              guardAI;
    public PokemobAIUtilityMove utilMoveAI;
    public LogicMountedControl  controller;
    protected AIStuff           aiStuff;

    protected PokeNavigator     navi;
    protected PokemobMoveHelper mover;
    boolean                     initAI         = true;
    boolean                     popped         = false;
    protected PokemobAI         aiObject;
    boolean                     isAFish        = false;

    public TerrainSegment       currentTerrain = null;

    public EntityAiPokemob(World world)
    {
        super(world);
        here = Vector3.getNewVector();
    }

    ///////////////////////////////////////// Init
    ///////////////////////////////////////// things///////////////////////////////

    @Override
    public boolean attackEntityAsMob(Entity par1Entity)
    {
        return super.attackEntityAsMob(par1Entity);
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase p_82196_1_, float p_82196_2_)
    {
    }

    ///////////////////////////////////// AI
    ///////////////////////////////////// States///////////////////////////////

    @Override
    public boolean canBreatheUnderwater()
    {
        return (getType1() == PokeType.water || getType2() == PokeType.water || getPokedexEntry().shouldDive
                || getPokedexEntry().swims());
    }

    @Override
    public void fall(float distance, float damageMultiplier)
    {
        PokedexEntry entry = getPokedexEntry();
        boolean canFloat = entry.floats() || entry.flys() || canUseFly();
        if (distance > 4 + height) distance = 0;
        if (distance < 5) damageMultiplier = 0;
        if (!canFloat) super.fall(distance, damageMultiplier);
    }

    ////////////////// Things which happen every tick///////////////////////////

    /** @return the aiObject */
    public PokemobAI getAiObject()
    {
        if (aiObject == null)
        {
            aiObject = AISaveHandler.instance().getAI(this);
        }
        return aiObject;
    }

    /** Checks if the entity's current position is a valid location to spawn
     * this entity. */
    @Override
    public boolean getCanSpawnHere()
    {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox());
    }

    @Override
    public float getDirectionPitch()
    {
        return dataManager.get(DIRECTIONPITCHDW);
    }

    @Override
    public EntityMoveHelper getMoveHelper()
    {
        if (mover != null) return mover;
        return super.getMoveHelper();
    }

    @Override
    public double getMovementSpeed()
    {
        return this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
    }
    ////////////////////////// Death Related things////////////////////////

    @Override
    public PathNavigate getNavigator()
    {
        if (navi != null) return navi;

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
        return ItemPokemobEgg.getEggStack(this);
    }

    @Override
    public boolean getPokemonAIState(int state)
    {

        if (state == SADDLED)
        {
            handleArmourAndSaddle();
        }

        return (dataManager.get(AIACTIONSTATESDW) & state) != 0;
    }

    /////////////////// Movement related things///////////////////////////

    ////////////////////////////// Misc////////////////////////////////////////////////////////////////
    @Override
    /** Get number of ticks, at least during which the living entity will be
     * silent. */
    public int getTalkInterval()
    {
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
                if (!swims())
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
                        this.world.spawnParticle(EnumParticleTypes.WATER_BUBBLE, this.posX + f2, f1 + 1.0F,
                                this.posZ + f3, this.motionX, this.motionY - this.rand.nextFloat() * 0.2F,
                                this.motionZ);
                    }

                    for (i = 0; i < 1.0F + this.width * 20.0F; ++i)
                    {
                        f2 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        f3 = (this.rand.nextFloat() * 2.0F - 1.0F) * this.width;
                        this.world.spawnParticle(EnumParticleTypes.WATER_SPLASH, this.posX + f2, f1 + 1.0F,
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
    }

    protected void initAI(PokedexEntry entry)
    {
        initAI = false;
        navi = new PokeNavigator(this, world);
        mover = new PokemobMoveHelper(this);
        jumpHelper = new PokemobJumpHelper(this);
        aiStuff = new AIStuff(this);

        float moveSpeed = 0.5f;
        float speedFactor = (float) (1 + Math.sqrt(entry.getStatVIT()) / (100F));
        moveSpeed *= speedFactor;
        if (entry.flys()) moveSpeed /= 1.25f;

        this.getNavigator().setSpeed(moveSpeed);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(moveSpeed);

        // Add in the vanilla like AI methods.
        this.guardAI = new GuardAI(this, this.getCapability(EventsHandler.GUARDAI_CAP, null));
        this.tasks.addTask(5, guardAI);
        this.tasks.addTask(5, utilMoveAI = new PokemobAIUtilityMove(this));
        this.tasks.addTask(8, new PokemobAILook(this, EntityPlayer.class, 8.0F, 1f));
        this.targetTasks.addTask(3, new PokemobAIHurt(this, entry.isSocial));

        for (int xy = 0; xy < entry.species.length; xy++)
        {

            if (entry.species[xy].equalsIgnoreCase("FISH") || entry.species[xy].equalsIgnoreCase("JELLYFISH")
                    || entry.species[xy].equalsIgnoreCase("WHALE") || entry.species[xy].equalsIgnoreCase("echinoderm")
                    || entry.species[xy].equalsIgnoreCase("gastropoda"))
            {
                isAFish = true;
                break;
            }

        }

        // Add in the various logic AIs that are needed on both client and
        // server.
        aiStuff.addAILogic(new LogicInLiquid(this));
        aiStuff.addAILogic(new LogicCollision(this));
        aiStuff.addAILogic(new LogicMovesUpdates(this));
        aiStuff.addAILogic(new LogicInMaterials(this));
        aiStuff.addAILogic(new LogicFloatFlySwim(this));
        aiStuff.addAILogic(new LogicMiscUpdate(this));

        // Controller is done separately for ease of locating it for controls.
        controller = new LogicMountedControl(this);

        if (world.isRemote) return;

        // Add in the Custom type of AI tasks.
        aiStuff.addAITask(new AIAttack(this).setPriority(200));
        aiStuff.addAITask(new AICombatMovement(this).setPriority(250));
        if (!entry.isStationary)
        {
            aiStuff.addAITask(new AIFollowOwner(this, 2 + this.width + this.length, 2 + this.width + this.length)
                    .setPriority(400));
        }
        aiStuff.addAITask(new AIGuardEgg(this).setPriority(250));
        aiStuff.addAITask(new AIMate(this).setPriority(300));
        aiStuff.addAITask(new AIHungry(this, new EntityItem(world), 16).setPriority(300));
        AIStoreStuff ai = new AIStoreStuff(this);
        aiStuff.addAITask(ai.setPriority(350));
        aiStuff.addAITask(new AIGatherStuff(this, 32, ai).setPriority(400));
        aiStuff.addAITask(new AIIdle(this).setPriority(500));
        aiStuff.addAITask(new AIFindTarget(this).setPriority(400));

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
        return getPokemonAIState(INWATER);
    }

    @Override
    public void jump()
    {
        if (world.isRemote) return;
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
            this.motionY += 0.5D + factor * 1 / getPokedexEntry().height;

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
    /** Moves the entity based on the specified heading. Args: strafe,
     * forward */// TODO fix minor bugs here.
    public void travel(float strafe, float up, float forward)
    {
        if (true)
        {
            PokedexEntry entry = getPokedexEntry();
            if (getTransformedTo() instanceof IPokemob)
            {
                entry = ((IPokemob) getTransformedTo()).getPokedexEntry();
            }
            int aiState = dataManager.get(AIACTIONSTATESDW);
            boolean isAbleToFly = entry.floats() || entry.flys();
            boolean isWaterMob = entry.swims();

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
                            f6 = this.world.getBlockState(blockpos$pooledmutableblockpos).getBlock().slipperiness
                                    * 0.91F;
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
                                    this.getEntityBoundingBox().minY - 1.0D, this.posZ)).getBlock().slipperiness
                                    * 0.91F;
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
                                if (!this.hasNoGravity() && (!isAbleToFly || this.getAIState(SITTING, aiState)
                                        || this.getAIState(SLEEPING, aiState)))
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

                        this.motionY *= 0.9800000190734863D;
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
            return;
        }
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

        if (!this.world.isRemote)
        {
            int i = 0;

            if (entity instanceof EntityPlayer)
            {
                i = EnchantmentHelper.getLootingModifier((EntityLivingBase) entity);
            }

            captureDrops = true;
            capturedDrops.clear();

            boolean shadowDrop = (this.isShadow() && this.getLevel() < 40);

            if (this.canDropLoot() && this.world.getGameRules().getBoolean("doMobLoot") && !shadowDrop)
            {
                this.dropFewItems(this.recentlyHit > 0, i);
                this.dropEquipment(this.recentlyHit > 0, i);

                if (recentlyHit > 0 && !getPokemonAIState(IPokemob.TAMED))
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
            if (this.isAncient())
            {

                ItemStack eggItemStack = ItemPokemobEgg.getEggStack(getPokedexEntry());
                Entity eggItem = new EntityPokemobEgg(world, posX, posY, posZ, eggItemStack, this);
                EggEvent.Lay event = new EggEvent.Lay(eggItem);
                MinecraftForge.EVENT_BUS.post(event);

                if (!event.isCanceled())
                {
                    egg = eggItem;
                    world.spawnEntity(egg);
                }
            }
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
        if (!PokecubeCore.isOnClientSide() && getPokemonAIState(IMoveConstants.TAMED))
        {
            HappinessType.applyHappiness(this, HappinessType.FAINT);
            ITextComponent mess = new TextComponentTranslation("pokemob.action.faint.own",
                    getPokemonDisplayName().getFormattedText());
            displayMessageToOwner(mess);
            returnToPokecube();
        }
        if (!getPokemonAIState(IMoveConstants.TAMED))
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
        controller.doServerTick(world);
        super.onLivingUpdate();
        if (isServerWorld() && isPokemonShaking && !isPokemonWet && !hasPath() && onGround)
        {
            isPokemonWet = true;
            timePokemonIsShaking = 0.0F;
            prevTimePokemonIsShaking = 0.0F;
            world.setEntityState(this, (byte) 8);
        }
    }

    //////////////// Jumping related//////////////////////////

    @Override
    public void onUpdate()
    {
        if (initAI)
        {
            initAI(getPokedexEntry());
        }
        if (popped && getPokemonAIState(TRADED))
        {
            evolve(true, false);
            popped = false;
        }
        if (getPokedexEntry().floats() || getPokedexEntry().flys()) fallDistance = 0;
        dimension = world.provider.getDimension();
        super.onUpdate();
        if (world.isRemote)
        {
            int id = dataManager.get(ATTACKTARGETIDDW);
            if (id >= 0 && getAttackTarget() == null)
            {
                setAttackTarget((EntityLivingBase) PokecubeMod.core.getEntityProvider().getEntity(world, id, false));
            }
            if (id < 0 && getAttackTarget() != null)
            {
                setAttackTarget(null);
            }
        }
        for (ILogicRunnable logic : aiStuff.aiLogic)
        {
            logic.doServerTick(world);
        }
        int state = dataManager.get(AIACTIONSTATESDW);
        if (getAIState(IMoveConstants.TAMED, state) && (getPokemonOwnerID() == null))
        {
            setPokemonAIState(IMoveConstants.TAMED, false);
        }
        if (loveTimer > 600)
        {
            resetLoveStatus();
        }
        if (ticksExisted > EXITCUBEDURATION && getAIState(EXITINGCUBE, state))
        {
            setPokemonAIState(EXITINGCUBE, false);
        }
        if (this.getPokemonAIState(IMoveConstants.SITTING) && !this.getNavigator().noPath())
        {
            this.getNavigator().clearPathEntity();
        }
        // TODO move this over to a capability or something.
        TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(this);
        if (!t.equals(currentTerrain))
        {
            currentTerrain = t;
            PokemobTerrainEffects effect = (PokemobTerrainEffects) currentTerrain.geTerrainEffect("pokemobEffects");
            effect.doEntryEffect(this);
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
        if (isWet() && !(this.canUseSurf()))
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
            if (timePokemonIsShaking > 0.4F && !swims())
            {
                float f = (float) posY;
                int i = (int) (MathHelper.sin((timePokemonIsShaking - 0.4F) * (float) Math.PI) * 7F);

                for (int j = 0; j < i; j++)
                {
                    float f1 = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                    float f2 = (rand.nextFloat() * 2.0F - 1.0F) * width * 0.5F;
                    world.spawnParticle(EnumParticleTypes.WATER_SPLASH, posX + f1, f + 0.8F, posZ + f2, motionX,
                            motionY, motionZ);
                }
            }
        }
    }

    @Override
    public void popFromPokecube()
    {
        super.popFromPokecube();
        popped = true;
        if (world.isRemote) return;
        this.playSound(this.getSound(), 0.5f, 1);
        if (this.isShiny())
        {
            Vector3 particleLoc = Vector3.getNewVector();
            for (int i = 0; i < 20; ++i)
            {
                particleLoc.set(posX + rand.nextFloat() * width * 2.0F - width, posY + 0.5D + rand.nextFloat() * height,
                        posZ + rand.nextFloat() * width * 2.0F - width);
                PokecubeMod.core.spawnParticle(world, EnumParticleTypes.VILLAGER_HAPPY.getParticleName(), particleLoc,
                        null);
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
        if (hand != EnumHand.MAIN_HAND) return false;
        ItemStack key = new ItemStack(Items.SHEARS, 1, Short.MAX_VALUE);
        // Check shearable interaction.
        if (getPokedexEntry().interact(key) && CompatWrapper.isValid(held)
                && Tools.isSameStack(key, held)) { return false; }
        // Check Pokedex Entry defined Interaction for player.
        if (getPokedexEntry().interact(player, this, true)) return true;
        Item torch = Item.getItemFromBlock(Blocks.TORCH);
        boolean isOwner = false;
        if (getPokemonAIState(IMoveConstants.TAMED) && getOwner() != null)
        {
            isOwner = getOwner().getEntityId() == player.getEntityId();
        }
        if (isOwner)
        {
            // Either push pokemob around, or if sneaking, make it try to climb
            // on shoulder
            if (CompatWrapper.isValid(held) && (held.getItem() == Items.STICK || held.getItem() == torch))
            {
                Vector3 look = Vector3.getNewVector().set(player.getLookVec()).scalarMultBy(1);
                look.y = 0.2;
                this.motionX += look.x;
                this.motionY += look.y;
                this.motionZ += look.z;
                return false;
            }
            // Debug thing to maximize happiness
            if (CompatWrapper.isValid(held) && held.getItem() == Items.APPLE)
            {
                if (player.capabilities.isCreativeMode && player.isSneaking())
                {
                    this.addHappiness(255);
                }
            }
            // Debug thing to increase hunger time
            if (CompatWrapper.isValid(held) && held.getItem() == Items.GOLDEN_HOE)
            {
                if (player.capabilities.isCreativeMode && player.isSneaking())
                {
                    this.setHungerTime(this.getHungerTime() + 4000);
                }
            }
            // Use shiny charm to make shiny
            if (CompatWrapper.isValid(held)
                    && ItemStack.areItemStackTagsEqual(held, PokecubeItems.getStack("shiny_charm")))
            {
                if (player.isSneaking())
                {
                    this.setShiny(!this.isShiny());
                    held.splitStack(1);
                }
                return true;
            }
        }

        // is Dyeable
        if (CompatWrapper.isValid(held) && getPokedexEntry().dyeable)
        {
            if (held.getItem() == Items.DYE)
            {
                setSpecialInfo(held.getItemDamage());
                CompatWrapper.increment(held, -1);
                return true;
            }
            else if (held.getItem() == Items.SHEARS) { return false; }
        }

        // Open Pokedex Gui
        if (CompatWrapper.isValid(held) && held.getItem() instanceof ItemPokedex)
        {
            if (!player.isSneaking())
            {
                player.openGui(PokecubeCore.instance, Config.GUIPOKEDEX_ID, world, (int) posX, (int) posY, (int) posZ);
                PokecubePlayerDataHandler.getInstance().getPlayerData(player).getData(PokecubePlayerStats.class)
                        .inspect(player, this);
            }
            return true;
        }
        boolean deny = getPokemonAIState(IMoveConstants.NOITEMUSE);
        if (deny && getAttackTarget() == null)
        {
            deny = false;
            this.setPokemonAIState(NOITEMUSE, false);
        }

        if (deny)
        {
            // Add message here about cannot use items right now
            player.sendMessage(new TextComponentTranslation("pokemob.action.cannotuse"));
            return false;
        }

        // Owner only interactions.
        if (isOwner && !PokecubeCore.isOnClientSide())
        {
            if (CompatWrapper.isValid(held))
            {
                // Check if it should evolve from item, do so if yes.
                if (PokecubeItems.isValidEvoItem(held) && canEvolve(held))
                {
                    IPokemob evolution = evolve(true, false, held);
                    if (evolution != null)
                    {
                        CompatWrapper.increment(held, -1);
                        if (!CompatWrapper.isValid(held))
                        {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem,
                                    CompatWrapper.nullStack);
                        }
                    }
                    return true;
                }
                int fav = Nature.getFavouriteBerryIndex(getNature());
                // Check if favourte berry and sneaking, if so, do breeding
                // stuff.
                if (player.isSneaking() && getAttackTarget() == null && held.getItem() instanceof ItemBerry
                        && (fav == -1 || fav == held.getItemDamage()))
                {
                    if (!player.capabilities.isCreativeMode)
                    {
                        CompatWrapper.increment(held, -1);
                        if (!CompatWrapper.isValid(held))
                        {
                            player.inventory.setInventorySlotContents(player.inventory.currentItem,
                                    CompatWrapper.nullStack);
                        }
                    }
                    this.loveTimer = 10;
                    this.setAttackTarget(null);
                    this.world.setEntityState(this, (byte) 18);
                    return true;
                }
                // Otherwise check if useable item.
                if (held.getItem() instanceof IPokemobUseable)
                {
                    boolean used = ((IPokemobUseable) held.getItem()).itemUse(held, this, player);
                    this.setPokemonAIState(NOITEMUSE, true);
                    if (used)
                    {
                        held.splitStack(1);
                        return true;
                    }
                }
                // Try to hold the item.
                if (canBeHeld(held))
                {
                    ItemStack heldItem = getHeldItemMainhand();
                    if (heldItem != CompatWrapper.nullStack)
                    {
                        dropItem();
                    }
                    ItemStack toSet = held.copy();
                    CompatWrapper.setStackSize(toSet, 1);
                    setHeldItem(toSet);
                    this.setPokemonAIState(NOITEMUSE, true);
                    CompatWrapper.increment(held, -1);
                    if (!CompatWrapper.isValid(held))
                    {
                        player.inventory.setInventorySlotContents(player.inventory.currentItem,
                                CompatWrapper.nullStack);
                    }
                    return true;
                }
            }

            // Check saddle for riding.
            if (getPokemonAIState(SADDLED) && !player.isSneaking()
                    && (!CompatWrapper.isValid(held) || !(held.getItem() instanceof ItemPokedex)))
            {
                if (!handleHmAndSaddle(player, new ItemStack(Items.SADDLE)))
                {
                    this.setJumping(false);
                    return false;
                }
                return true;
            }

            // Open Gui
            openGUI(player);
            return true;
        }
        return false;
    }

    @Override
    public void setAttackTarget(EntityLivingBase entity)
    {
        if (entity != null && entity.equals(this.getPokemonOwner())) { return; }
        if (entity != null && entity.equals(this)) { return; }
        if (entity != null) setPokemonAIState(SITTING, false);
        if (entity != null && !world.isRemote)
        {
            dataManager.set(ATTACKTARGETIDDW, Integer.valueOf(entity.getEntityId()));
        }
        if (entity == null && !world.isRemote)
        {
            dataManager.set(ATTACKTARGETIDDW, Integer.valueOf(-1));
        }
        if (entity == null)
        {
            this.getEntityData().setString("lastMoveHitBy", "");
        }
        if (entity != getAttackTarget() && getAbility() != null && !world.isRemote)
        {
            getAbility().onAgress(this, entity);
        }
        super.setAttackTarget(entity);
    }

    @Override
    public void setDead()
    {
        if (addedToChunk)
        {
            PokecubeSerializer.getInstance().removePokemob(this);
            AISaveHandler.instance().removeAI(this);
            if (getAbility() != null)
            {
                getAbility().destroy();
            }
            if (getHome() != null && getHome().getY() > 0 && world.isAreaLoaded(getHome(), 2))
            {
                TileEntity te = world.getTileEntity(getHome());
                if (te != null && te instanceof TileEntityNest)
                {
                    TileEntityNest nest = (TileEntityNest) te;
                    nest.removeResident(this);
                }
            }
        }
        super.setDead();
    }

    @Override
    public void setDirectionPitch(float pitch)
    {
        dataManager.set(DIRECTIONPITCHDW, pitch);
    }

    @Override
    public void setJumping(boolean jump)
    {
        if (!world.isRemote)
        {
            setPokemonAIState(JUMPING, jump);
        }
        else
        {
            isJumping = getPokemonAIState(JUMPING);
        }
    }

    @Override // Is actually move Up
    public void setMoveForward(float forward)
    {
        this.moveForward = forward;
    }

    @Override
    public void setPokemonAIState(int state, boolean flag)
    {
        int byte0 = dataManager.get(AIACTIONSTATESDW);
        if (state == STAYING)
        {
            here.set(this);
            setHome(here.intX(), here.intY(), here.intZ(), 16);
        }
        if (flag)
        {
            dataManager.set(AIACTIONSTATESDW, Integer.valueOf((byte0 | state)));
        }
        else
        {
            dataManager.set(AIACTIONSTATESDW, Integer.valueOf((byte0 & -state - 1)));
        }
    }

    @Override
    protected void updateAITasks()
    {
        super.updateAITasks();
    }

    @Override
    /** main AI tick function, replaces updateEntityActionState */
    protected void updateAITick()
    {
        super.updateAITick();
    }

    @Override
    protected void updateEntityActionState()
    {
        ++this.idleTime;
        ;
        navi.refreshCache();
        this.world.profiler.startSection("checkDespawn");
        this.despawnEntity();
        this.world.profiler.endSection();
        this.world.profiler.startSection("sensing");
        this.senses.clearSensingCache();
        this.world.profiler.endSection();
        this.world.profiler.startSection("targetSelector");
        this.targetTasks.onUpdateTasks();
        this.world.profiler.endSection();
        this.world.profiler.startSection("goalSelector");
        this.tasks.onUpdateTasks();
        this.world.profiler.endSection();
        this.world.profiler.startSection("navigation");
        this.getNavigator().onUpdateNavigation();
        this.world.profiler.endSection();
        this.world.profiler.startSection("mob tick");
        // Run last tick's results from AI stuff
        this.aiStuff.runServerThreadTasks(world);
        // Schedule AIStuff to tick for next tick.
        AIThreadManager.scheduleAITick(aiStuff);
        this.updateAITasks();
        this.updateAITick();
        this.world.profiler.endSection();
        this.world.profiler.startSection("controls");
        this.world.profiler.startSection("move");
        this.getMoveHelper().onUpdateMoveHelper();
        this.world.profiler.endStartSection("look");
        this.getLookHelper().onUpdateLook();
        this.world.profiler.endStartSection("jump");
        this.getJumpHelper().doJump();
        this.world.profiler.endSection();
        this.world.profiler.endSection();
    }

    @Override
    public AIStuff getAI()
    {
        return aiStuff;
    }

    @Override
    public boolean selfManaged()
    {
        return true;
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
