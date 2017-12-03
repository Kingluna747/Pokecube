package pokecube.core.ai.properties;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import pokecube.core.utils.TimePeriod;

public interface IGuardAICapability
{
    public static enum GuardState
    {
        IDLE, RUNNING, COOLDOWN
    }

    public static class Storage implements Capability.IStorage<IGuardAICapability>
    {
        private BlockPos readFromTag(NBTTagCompound tag)
        {
            if (!tag.hasKey("x")) return null;
            return new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        }

        @Override
        public void readNBT(Capability<IGuardAICapability> capability, IGuardAICapability instance, EnumFacing side,
                NBTBase nbt)
        {
            if (nbt instanceof NBTTagCompound)
            {
                NBTTagCompound data = (NBTTagCompound) nbt;
                instance.setPos(readFromTag(data.getCompoundTag("pos")));
                instance.setRoamDistance(data.getFloat("roamDistance"));
                instance.setState(GuardState.values()[data.getInteger("state")]);
                NBTTagCompound tag = data.getCompoundTag("activeTime");
                instance.setActiveTime(new TimePeriod(tag.getInteger("start"), tag.getInteger("end")));
            }
        }

        @Override
        public NBTBase writeNBT(Capability<IGuardAICapability> capability, IGuardAICapability instance, EnumFacing side)
        {
            NBTTagCompound ret = new NBTTagCompound();
            NBTTagCompound tag = new NBTTagCompound();
            writeToTag(tag, instance.getPos());
            ret.setTag("pos", tag);
            tag = new NBTTagCompound();
            if (instance.getActiveTime() != null)
            {
                tag.setInteger("start", instance.getActiveTime().startTick);
                tag.setInteger("end", instance.getActiveTime().endTick);
            }
            ret.setTag("activeTime", tag);
            ret.setInteger("state", instance.getState().ordinal());
            ret.setFloat("roamDistance", instance.getRoamDistance());
            return ret;
        }

        private void writeToTag(NBTTagCompound tag, BlockPos pos)
        {
            if (pos == null) return;
            tag.setInteger("x", pos.getX());
            tag.setInteger("y", pos.getY());
            tag.setInteger("z", pos.getZ());
        }

    }

    TimePeriod getActiveTime();

    BlockPos getPos();

    float getRoamDistance();

    GuardState getState();

    void setActiveTime(TimePeriod active);

    void setPos(BlockPos pos);

    void setRoamDistance(float roam);

    void setState(GuardState state);
}
