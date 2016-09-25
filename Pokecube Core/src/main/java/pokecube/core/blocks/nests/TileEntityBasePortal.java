package pokecube.core.blocks.nests;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.world.dimensions.PokecubeDimensionManager;

public class TileEntityBasePortal extends TileEntityOwnable
{
    public boolean exit        = false;
    public boolean sendToUsers = false;
    public int     exitDim     = 0;

    public void transferPlayer(EntityPlayer playerIn)
    {
        if (!sendToUsers && placer == null) return;
        String owner = sendToUsers ? playerIn.getCachedUniqueIdString() : placer.toString();
        BlockPos exitLoc = PokecubeDimensionManager.getBaseEntrance(owner, worldObj.provider.getDimension());
        if (exitLoc == null)
        {
            PokecubeDimensionManager.setBaseEntrance(owner, worldObj.provider.getDimension(), pos);
            exitLoc = pos;
        }
        double dist = exitLoc.distanceSq(pos);
        if (dist > 36)
        {
            worldObj.setBlockState(pos, Blocks.STONE.getDefaultState());
            playerIn.addChatMessage(new TextComponentTranslation("pokemob.removebase.stale"));
        }
        else PokecubeDimensionManager.sendToBase(owner, playerIn, exitLoc.getX(), exitLoc.getY(), exitLoc.getZ(),
                exitDim);
    }

    @Override
    public boolean shouldBreak()
    {
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        this.sendToUsers = tagCompound.getBoolean("allPlayers");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("allPlayer", sendToUsers);
        return tagCompound;
    }
}
