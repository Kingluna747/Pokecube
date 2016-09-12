package pokecube.core.network.packets;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import pokecube.core.PokecubeCore;
import pokecube.core.world.dimensions.PokecubeDimensionManager;

public class PacketSyncDimIds implements IMessage, IMessageHandler<PacketSyncDimIds, IMessage>
{
    public NBTTagCompound data = new NBTTagCompound();

    public PacketSyncDimIds()
    {
    }

    @Override
    public IMessage onMessage(final PacketSyncDimIds message, final MessageContext ctx)
    {
        PokecubeCore.proxy.getMainThreadListener().addScheduledTask(new Runnable()
        {
            public void run()
            {
                processMessage(ctx, message);
            }
        });
        return null;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        try
        {
            data = buffer.readNBTTagCompoundFromBuffer();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        PacketBuffer buffer = new PacketBuffer(buf);
        buffer.writeNBTTagCompoundToBuffer(data);
    }

    void processMessage(MessageContext ctx, PacketSyncDimIds message)
    {
        if (message.data.hasKey("border"))
        {
            int border = message.data.getInteger("border");
            int dim = message.data.getInteger("dim");
            EntityPlayer player = PokecubeCore.getPlayer(null);
            if (player.dimension == dim)
            {
                player.worldObj.getWorldBorder().setSize(border);
            }
        }
        else PokecubeDimensionManager.getInstance().loadFromTag(message.data);
    }
}
