/**
 * 
 */
package pokecube.core.entity.pokemobs.helper;

import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import pokecube.core.PokecubeCore;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.network.pokemobs.PokemobPacketHandler.MessageServer;
import pokecube.core.utils.PokeType;

/** Handles the HM behaviour.
 * 
 * @author Manchou */
public abstract class EntityMountablePokemob extends EntityEvolvablePokemob
{
    public static enum MountState
    {
        UP, NONE, DOWN
    }

    private int       mountCounter       = 0;
    public float      landSpeedFactor    = 1;
    public float      waterSpeedFactor   = 0.25f;
    public float      airbornSpeedFactor = 0.02f;
    public float      speedFactor        = 1;

    private float     hungerFactor       = 1;
    public boolean    canUseSaddle       = false;
    private boolean   canFly             = false;
    private boolean   canSurf            = false;

    private boolean   canDive            = false;
    protected double  yOffset;
    public MountState state;

    public int        counterMount       = 0;

    protected boolean pokemobJumping;

    protected float   jumpPower;

    private int       lastMessage        = 0;

    public EntityMountablePokemob(World world)
    {
        super(world);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float i)
    {
        if (isRiding())
        {
            mountEntity(null);
            setPokemonAIState(SHOULDER, false);
            counterMount = 0;
        }
        return super.attackEntityFrom(source, i);
    }

    @Override
    public boolean canUseDive()
    {
        return canDive;
    }

    @Override
    public boolean canUseFly()
    {
        return canFly;
    }

    @Override
    public boolean canUseSurf()
    {
        return canSurf;
    }

    public boolean checkHunger()
    {
        if (this.riddenByEntity instanceof EntityPlayer
                && !((EntityPlayer) this.riddenByEntity).capabilities.isCreativeMode)
        {
            int hunger = getHungerTime();
            if (hunger < 0.85 * PokecubeMod.core.getConfig().pokemobLifeSpan)
            {
                hungerFactor = 1;
                return true;
            }
            else
            {
                if (this.ticksExisted % 20 == 0 && !worldObj.isRemote)
                {
                    String mess = StatCollector.translateToLocal("pokemob.hungry.slow");
                    ((EntityPlayer) this.riddenByEntity).addChatMessage(new ChatComponentText(mess));
                }
                hungerFactor = 0.01f;
                return false;
            }

        }
        hungerFactor = 1;
        return false;
    }

    /** Returns the Y offset from the entity's position for any entity riding
     * this one. */
    @Override
    public double getMountedYOffset()
    {
        return this.height * this.getPokedexEntry().mountedOffset;
    }

    @Override
    public boolean getOnGround()
    {
        return onGround;
    }

    @Override
    public double getYOffset()
    {
        double ret = yOffset;

        if (getPokemonAIState(HELD))
        {

        }

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT
                && ridingEntity == PokecubeCore.getProxy().getPlayer(null))
        {
            ret = -ridingEntity.height + 0.25;
            this.onGround = true;
            return ret;
        }

        return ret;// - 1.6F;
    }

    /** Called when a player interacts with its pokemob with an item such as HM
     * or saddle.
     * 
     * @param entityplayer
     *            the player which makes the action
     * @param itemstack
     *            the id of the item
     * @return if the use worked */
    protected boolean handleHmAndSaddle(EntityPlayer entityplayer, ItemStack itemstack)
    {
        if ((riddenByEntity != null && riddenByEntity != entityplayer)) return false;
        if (isRidable(entityplayer))
        {
            if (!worldObj.isRemote) entityplayer.mountEntity(this);
            return true;
        }
        return false;
    }

    public void initRidable()
    {
        if (isType(PokeType.water) || getPokedexEntry().swims() || getPokedexEntry().shouldSurf
                || getPokedexEntry().shouldDive)
        {
            this.setCanSurf(true);
        }
        if (canUseSurf() && getPokedexEntry().shouldDive)
        {
            this.setCanDive(true);
        }
        if ((isType(PokeType.flying) && getPokedexEntry().shouldFly) || (getPokedexEntry().flys())
                || getPokedexEntry().shouldFly)
        {
            this.setCanFly(true);
        }
    }

    @Override
    public boolean interact(EntityPlayer entityplayer)
    {
        if (entityplayer == ridingEntity && getPokemonAIState(SHOULDER)) { return false; }

        return super.interact(entityplayer);
    }

    public boolean isPokemobJumping()
    {
        return this.pokemobJumping;
    }

    public boolean isRidable(Entity rider)
    {
        PokedexEntry entry = this.getPokedexEntry();
        if (entry == null)
        {
            System.err.println("Null Entry for " + this);
            return false;
        }
        return (entry.height * getSize() + entry.width * getSize()) > rider.width
                && Math.max(entry.width, entry.length) * getSize() > rider.width * 1.8;
    }

    /** Returns true if the entity is riding another entity, used by render to
     * rotate the legs to be in 'sit' position for players. */
    @Override
    public boolean isRiding()
    {
        return ridingEntity != null || getFlag(2);
    }

    /** Called when a player mounts an entity. e.g. mounts a pig, mounts a
     * boat. */
    @Override
    public void mountEntity(Entity entityIn)
    {
        if (entityIn == null)
        {
            mountCounter = 5;
            motionX = motionY = motionZ = 0;
        }
        super.mountEntity(entityIn);
    }

    /** Moves the entity based on the specified heading. Args: strafe,
     * forward */
    @Override
    public void moveEntityWithHeading(float strafe, float forward)
    {
        if (this.riddenByEntity != null)
        {
            this.getNavigator().clearPathEntity();
            this.prevRotationYaw = this.rotationYaw = this.riddenByEntity.rotationYaw;
            this.rotationPitch = this.riddenByEntity.rotationPitch * 0.5F;
            this.setRotation(this.rotationYaw, this.rotationPitch);
            this.rotationYawHead = this.renderYawOffset = this.rotationYaw;
            strafe = ((EntityLivingBase) this.riddenByEntity).moveStrafing * 0.5F;
            forward = ((EntityLivingBase) this.riddenByEntity).moveForward;
            this.riddenByEntity.onGround = true;
            this.riddenByEntity.fallDistance = 0;
            this.riddenByEntity.fall(0, 0);

            if (canUseDive()) this.riddenByEntity.setAir(300);

            if (forward <= 0.0F)
            {
                forward *= 0.25F;
            }

            if (onGround && !isInWater() && forward > 0)
            {
                forward = landSpeedFactor;
            }
            if (isInWater())
            {
                forward = forward > 0 ? waterSpeedFactor : 0;
                if (this.canUseSurf() && !this.canUseDive() && this.riddenByEntity.isInWater()) this.motionY = 0;
            }
            if (!this.onGround && this.canUseFly())
            {
                forward = forward > 0 ? airbornSpeedFactor : 0;
                this.jumpPower = 0.0F;
            }
            boolean dive = false;
            boolean jump = false;
            if ((dive = (this.canUseDive() && isInWater())) || this.canUseFly())
            {
                motionY = state == MountState.UP ? 0.5 : state == MountState.DOWN ? -0.5 : 0;

                if (dive)
                {
                    this.riddenByEntity.setAir(300);
                    PotionEffect effect = ((EntityLivingBase) this.riddenByEntity)
                            .getActivePotionEffect(Potion.nightVision);
                    if (effect == null
                            || effect.getDuration() < 220 && this.riddenByEntity.isInsideOfMaterial(Material.water))
                        ((EntityLivingBase) this.riddenByEntity)
                                .addPotionEffect(new PotionEffect(Potion.nightVision.id, 250));
                }
            }
            else if (state == MountState.UP)
            {
                jump = state == MountState.UP;
            }

            if (jump && !this.isPokemobJumping() && this.onGround)
            {
                this.motionY = 0.5 + this.jumpPower;

                if (this.isPotionActive(Potion.jump))
                {
                    this.motionY += (this.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F;
                }

                this.setPokemobJumping(true);
                this.isAirBorne = true;

                if (forward > 0.0F)
                {
                    float f2 = MathHelper.sin(this.rotationYaw * (float) Math.PI / 180.0F);
                    float f3 = MathHelper.cos(this.rotationYaw * (float) Math.PI / 180.0F);
                    this.motionX += -0.4F * f2 * this.jumpPower;
                    this.motionZ += 0.4F * f3 * this.jumpPower;
                    this.playSound("mob.horse.jump", 0.4F, 1.0F);
                }

                this.jumpPower = 0.0F;
            }

            this.stepHeight = 1.0F;
            this.jumpMovementFactor = this.getAIMoveSpeed() * 0.1F;

            if (!worldObj.isAreaLoaded(getPosition(), 32, false))
            {
                motionX = motionZ = 0;
                strafe = forward = 0;
                if (lastMessage < ticksExisted - 20)
                {
                    lastMessage = ticksExisted;
                    this.riddenByEntity.addChatMessage(new ChatComponentTranslation("pokemob.areanotloaded"));
                }
            }

            this.setAIMoveSpeed(
                    (float) this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).getAttributeValue());
            this.moveEntityWithHeading2(strafe, forward);

            if (this.onGround || isInWater())
            {
                this.jumpPower = 0.0F;
                this.setPokemobJumping(false);
            }

            this.prevLimbSwingAmount = this.limbSwingAmount;
            double d0 = this.posX - this.prevPosX;
            double d1 = this.posZ - this.prevPosZ;
            float f4 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

            if (f4 > 1.0F)
            {
                f4 = 1.0F;
            }

            this.limbSwingAmount += (f4 - this.limbSwingAmount) * 0.4F;
            this.limbSwing += this.limbSwingAmount;

            if (worldObj.isRemote && this.riddenByEntity == PokecubeCore.getPlayer(null))
            {
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(27));
                buffer.writeByte(MessageServer.SYNCPOS);
                buffer.writeInt(getEntityId());
                buffer.writeFloat((float) posX);
                buffer.writeFloat((float) posY);
                buffer.writeFloat((float) posZ);
                buffer.writeFloat((float) motionX);
                buffer.writeFloat((float) motionY);
                buffer.writeFloat((float) motionZ);
                MessageServer message = new MessageServer(buffer);
                PokecubeMod.packetPipeline.sendToServer(message);
            }
        }
        else
        {
            this.stepHeight = 0.5F;
            this.jumpMovementFactor = 0.02F;
            super.moveEntityWithHeading(strafe, forward);
            new Exception().printStackTrace();
        }
    }

    /** Moves the entity based on the specified heading. Args: strafe,
     * forward */
    private void moveEntityWithHeading2(float strafe, float forward)
    {
        double d0;

        speedFactor = ((float) this.getPokedexEntry().getStatVIT()) / 75;

        if (Math.random() < 0.1 / (this.getLevel()))
        {
            this.setHungerTime(this.getHungerTime() + PokecubeMod.core.getConfig().pokemobLifeSpan / 5);
        }
        checkHunger();

        if (this.isInWater() && (!(this.canUseSurf())))
        {
            d0 = this.posY;
            this.moveFlying(strafe, forward, 0.04F);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.800000011920929D;
            this.motionY *= 0.800000011920929D;
            this.motionZ *= 0.800000011920929D;
            this.motionY -= 0.02D;

            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX,
                    this.motionY + 0.6000000238418579D - this.posY + d0, this.motionZ))
            {
                this.motionY = 0.30000001192092896D;
            }
        }
        else if (this.isInLava())
        {
            d0 = this.posY;
            this.moveFlying(strafe, forward, 0.02F);
            this.moveEntity(this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.5D;
            this.motionY *= 0.5D;
            this.motionZ *= 0.5D;
            this.motionY -= 0.02D;

            if (this.isCollidedHorizontally && this.isOffsetPositionInLiquid(this.motionX,
                    this.motionY + 0.6000000238418579D - this.posY + d0, this.motionZ))
            {
                this.motionY = 0.30000001192092896D;
            }
        }
        else
        {
            float f2 = 0.91F;

            if (this.onGround)
            {
                f2 = 0.54600006F;
                Block i = this.worldObj.getBlockState(getPosition().down()).getBlock();

                if (i != null)
                {
                    f2 = i.slipperiness * 0.91F;
                }
            }

            float f3 = 0.16277136F / (f2 * f2 * f2);
            float f4;

            if (this.onGround && !isInWater())
            {
                f4 = this.landSpeedFactor * f3 * 0.15f * this.speedFactor;
            }
            else if (isInWater())
            {
                f4 = this.waterSpeedFactor * f3 * 0.15f * this.speedFactor;
            }
            else if (this.canUseFly())
            {
                f4 = this.airbornSpeedFactor * f3 * 0.15f * this.speedFactor;
            }
            else
            {
                f4 = this.jumpMovementFactor;
            }
            f4 *= hungerFactor;

            this.moveFlying(strafe, forward, f4);
            f2 = 0.91F;

            if (this.onGround)
            {
                f2 = 0.54600006F;
                Block j = this.worldObj.getBlockState(getPosition().down()).getBlock();

                if (j != null)
                {
                    f2 = j.slipperiness * 0.91F;
                }
            }

            if (this.isOnLadder())
            {
                float f5 = 0.15F;

                if (this.motionX < (-f5))
                {
                    this.motionX = (-f5);
                }

                if (this.motionX > f5)
                {
                    this.motionX = f5;
                }

                if (this.motionZ < (-f5))
                {
                    this.motionZ = (-f5);
                }

                if (this.motionZ > f5)
                {
                    this.motionZ = f5;
                }

                this.fallDistance = 0.0F;

                if (this.motionY < -0.15D)
                {
                    this.motionY = -0.15D;
                }
            }

            this.moveEntity(this.motionX, this.motionY, this.motionZ);

            if (this.isCollidedHorizontally && this.isOnLadder())
            {
                this.motionY = 0.2D;
            }

            if (this.worldObj.isRemote && (!this.worldObj.isAreaLoaded(getPosition(), 10)
                    || !this.worldObj.getChunkFromBlockCoords(getPosition()).isLoaded()))
            {
                if (this.posY > 0.0D)
                {
                    this.motionY = -0.1D;
                }
                else
                {
                    this.motionY = 0.0D;
                }
            }
            else
            {
                this.motionY -= 0.08D;
            }

            this.motionY *= 0.9800000190734863D;
            this.motionX *= f2;
            this.motionZ *= f2;
        }

        this.prevLimbSwingAmount = this.limbSwingAmount;
        d0 = this.posX - this.prevPosX;
        double d1 = this.posZ - this.prevPosZ;
        float f6 = MathHelper.sqrt_double(d0 * d0 + d1 * d1) * 4.0F;

        if (f6 > 1.0F)
        {
            f6 = 1.0F;
        }

        this.limbSwingAmount += (f6 - this.limbSwingAmount) * 0.4F;
        this.limbSwing += this.limbSwingAmount;
    }

    @Override
    public void onUpdate()
    {
        super.onUpdate();
        if (mountCounter > 0) motionX = motionY = motionZ = 0;
        mountCounter--;
        if (ridingEntity != null)
        {
            rotationYaw = ridingEntity.rotationYaw;
            if (this.getAttackTarget() != null && !worldObj.isRemote)
            {
                mountEntity(null);
                counterMount = 0;
                setPokemonAIState(SHOULDER, false);
            }
        }
    }

    public EntityMountablePokemob setCanDive(boolean bool)
    {
        this.canDive = bool;
        this.setCanSurf(bool);
        return this;
    }

    /** Sets can use saddle and can use fly, and sets airspeed factor to 3 if
     * bool is true;
     * 
     * @param bool
     * @return */
    public EntityMountablePokemob setCanFly(boolean bool)
    {
        this.canFly = bool;
        this.airbornSpeedFactor = bool ? 3 : airbornSpeedFactor;
        return this;
    }

    /** Sets both can use saddle and can use surf, also sets waterspeed factor
     * to 2 if bool is true.
     * 
     * @param bool
     * @return */
    public EntityMountablePokemob setCanSurf(boolean bool)
    {
        this.canSurf = bool;
        this.waterSpeedFactor = bool ? 2 : waterSpeedFactor;
        return this;
    }

    @Deprecated
    public EntityMountablePokemob setCanUseSaddle(boolean bool)
    {
        this.canUseSaddle = bool;
        return this;
    }

    public void setPokemobJumping(boolean par1)
    {
        // this.isJumping = par1;
        this.pokemobJumping = par1;
    }

    public EntityMountablePokemob setSpeedFactors(double land, double air, double water)
    {
        landSpeedFactor = (float) land;
        waterSpeedFactor = (float) water;
        airbornSpeedFactor = (float) air;
        return this;
    }

    /** If the rider should be dismounted from the entity when the entity goes
     * under water
     *
     * @param rider
     *            The entity that is riding
     * @return if the entity should be dismounted when under water */
    @Override
    public boolean shouldDismountInWater(Entity rider)
    {
        return !this.canUseDive();
    }

    /** main AI tick function, replaces updateEntityActionState */// TODO move
                                                                  // this over
                                                                  // to an AI
                                                                  // class
    @Override
    protected void updateAITick()
    {
        super.updateAITick();
        if (!getPokedexEntry().canSitShoulder || !getPokemonAIState(IMoveConstants.TAMED)) return;

        if (counterMount++ > 50000)
        {
            counterMount = 0;
        }
        if (ridingEntity != null && !getPokemonAIState(SITTING))
        {
            EntityLivingBase entityplayer = getPokemonOwner();

            if (entityplayer != null)
            {
                mountEntity(null);
                setPokemonAIState(SHOULDER, false);
                counterMount = 0;
            }
        }
    }
}