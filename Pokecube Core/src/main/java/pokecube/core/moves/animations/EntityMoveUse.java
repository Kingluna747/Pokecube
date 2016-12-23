package pokecube.core.moves.animations;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Rotations;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.interfaces.IMoveAnimation.MovePacketInfo;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import thut.api.maths.Vector3;

public class EntityMoveUse extends Entity
{
    private static final Rotations        EMPTYLOCATION = new Rotations(0.0F, 0.0F, 0.0F);
    static final DataParameter<String>    MOVENAME      = EntityDataManager.<String> createKey(EntityMoveUse.class,
            DataSerializers.STRING);
    static final DataParameter<Rotations> ENDLOC        = EntityDataManager.<Rotations> createKey(EntityMoveUse.class,
            DataSerializers.ROTATIONS);
    static final DataParameter<Rotations> STARTLOC      = EntityDataManager.<Rotations> createKey(EntityMoveUse.class,
            DataSerializers.ROTATIONS);
    static final DataParameter<Integer>   USER          = EntityDataManager.<Integer> createKey(EntityMoveUse.class,
            DataSerializers.VARINT);
    static final DataParameter<Integer>   TARGET        = EntityDataManager.<Integer> createKey(EntityMoveUse.class,
            DataSerializers.VARINT);
    static final DataParameter<Integer>   TICK          = EntityDataManager.<Integer> createKey(EntityMoveUse.class,
            DataSerializers.VARINT);
    static final DataParameter<Integer>   APPLYTICK     = EntityDataManager.<Integer> createKey(EntityMoveUse.class,
            DataSerializers.VARINT);

    Vector3                               end           = Vector3.getNewVector();
    Vector3                               start         = Vector3.getNewVector();
    boolean                               applied       = false;

    public EntityMoveUse(World worldIn)
    {
        super(worldIn);
        this.setSize(1f, 1f);
        this.ignoreFrustumCheck = true;
    }

    public EntityMoveUse setMove(Move_Base move)
    {
        String name = "";
        if (move != null)
        {
            name = move.name;
        }
        this.getDataManager().set(MOVENAME, name);
        if (move.getAnimation((IPokemob) getUser()) != null)
        {
            getDataManager().set(TICK, move.getAnimation((IPokemob) getUser()).getDuration() + 1);
            setApplicationTick(getAge() - move.getAnimation((IPokemob) getUser()).getApplicationTick());
        }
        else getDataManager().set(TICK, 1);
        return this;
    }

    public Move_Base getMove()
    {
        return MovesUtils.getMoveFromName(this.getDataManager().get(MOVENAME));
    }

    public EntityMoveUse setStart(Vector3 location)
    {
        start.set(location);
        start.moveEntity(this);
        getDataManager().set(STARTLOC, new Rotations((float) start.x, (float) start.y, (float) start.z));
        return this;
    }

    public EntityMoveUse setEnd(Vector3 location)
    {
        end.set(location);
        getDataManager().set(ENDLOC, new Rotations((float) end.x, (float) end.y, (float) end.z));
        return this;
    }

    public Vector3 getStart()
    {
        Rotations rot = getDataManager().get(STARTLOC);
        start.x = rot.getX();
        start.y = rot.getY();
        start.z = rot.getZ();
        return start;
    }

    public Vector3 getEnd()
    {
        Rotations rot = getDataManager().get(ENDLOC);
        end.x = rot.getX();
        end.y = rot.getY();
        end.z = rot.getZ();
        return end;
    }

    public EntityMoveUse setUser(Entity user)
    {
        getDataManager().set(USER, user.getEntityId());
        return this;
    }

    public Entity getUser()
    {
        return PokecubeMod.core.getEntityProvider().getEntity(worldObj, getDataManager().get(USER), true);
    }

    public EntityMoveUse setTarget(Entity target)
    {
        if (target != null) getDataManager().set(TARGET, target.getEntityId());
        else getDataManager().set(TARGET, -1);
        return this;
    }

    public Entity getTarget()
    {
        return worldObj.getEntityByID(getDataManager().get(TARGET));
    }

    public int getAge()
    {
        return getDataManager().get(TICK);
    }

    public int getApplicationTick()
    {
        return getDataManager().get(APPLYTICK);
    }

    public void setApplicationTick(int tick)
    {
        getDataManager().set(APPLYTICK, tick);
    }

    @Override
    public void onUpdate()
    {
        int age = getAge() - 1;
        if (getMove() == null || this.isDead || age < 0)
        {
            this.setDead();
            return;
        }
        Move_Base attack = getMove();
        Entity user;
        if ((user = getUser()) == null || this.isDead || user.isDead || !user.addedToChunk)
        {
            this.setDead();
            return;
        }
        if (user instanceof EntityLivingBase && ((EntityLivingBase) user).getHealth() <= 1)
        {
            this.setDead();
            return;
        }
        if (worldObj.isRemote && attack.getAnimation((IPokemob) user) != null)
            attack.getAnimation((IPokemob) user).spawnClientEntities(getMoveInfo());

        if (!applied && age <= getApplicationTick())
        {
            applied = true;
            this.doMoveUse();
        }

        if (age == 0)
        {
            this.setDead();
        }
        getDataManager().set(TICK, age);
    }

    public MovePacketInfo getMoveInfo()
    {
        MovePacketInfo info = new MovePacketInfo(getMove(), getUser(), getTarget(), getStart(), getEnd());
        info.currentTick = info.move.getAnimation((IPokemob) info.attacker).getDuration() - (getAge() - 1);
        return info;
    }

    private void doMoveUse()
    {
        Move_Base attack = getMove();
        Entity user;
        if ((user = getUser()) == null || this.isDead || user.isDead) return;
        if (!worldObj.isRemote)
        {
            if (attack.move.notIntercepable)
            {
                MovesUtils.doAttack(attack.name, (IPokemob) user, getTarget());
            }
            else
            {
                MovesUtils.doAttack(attack.name, (IPokemob) user, getEnd());
            }
        }
    }

    @Override
    protected void entityInit()
    {
        this.getDataManager().register(MOVENAME, "");
        this.getDataManager().register(ENDLOC, EMPTYLOCATION);
        this.getDataManager().register(STARTLOC, EMPTYLOCATION);
        this.getDataManager().register(USER, -1);
        this.getDataManager().register(TARGET, -1);
        this.getDataManager().register(TICK, 0);
        this.getDataManager().register(APPLYTICK, 0);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound)
    {
        // Do nothing, if it needs to load/save, it should delete itself
        // instead.
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound)
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return TileEntity.INFINITE_EXTENT_AABB;
    }
}
