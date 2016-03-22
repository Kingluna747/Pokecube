package pokecube.pokeplayer;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.pokeplayer.network.PacketPokePlayer.MessageClient;

public class EventsHandler
{
    private static Proxy proxy;

    public EventsHandler(Proxy proxy)
    {
        EventsHandler.proxy = proxy;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void interactEvent(PlayerInteractEvent event)
    {

    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == Phase.END)
        {
            proxy.updateInfo(event.player);
        }
    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {
        if (!evt.player.worldObj.isRemote)
        {
        }
    }

    @SubscribeEvent
    public void PlayerJoinWorld(EntityJoinWorldEvent evt)
    {
        if (!(evt.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) evt.entity;
        if (!player.worldObj.isRemote)
        {
            new SendPacket(player);
            new SendExsistingPacket(player);
        }
    }

    public static class SendPacket
    {
        final EntityPlayer player;

        public SendPacket(EntityPlayer player)
        {
            this.player = player;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event)
        {
            if (event.player == player)
            {
                proxy.getPokemob(player);
                boolean pokemob = player.getEntityData().getBoolean("isPokemob");
                PokeInfo info = proxy.playerMap.get(player.getUniqueID());
                if (info == null) pokemob = false;
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(6));
                MessageClient message = new MessageClient(buffer);
                buffer.writeByte(MessageClient.SETPOKE);
                buffer.writeInt(player.getEntityId());
                buffer.writeBoolean(pokemob);
                if (pokemob)
                {
                    buffer.writeFloat(info.originalHeight);
                    buffer.writeFloat(info.originalWidth);
                    buffer.writeNBTTagCompoundToBuffer(player.getEntityData().getCompoundTag("Pokemob"));
                }
                PokecubeMod.packetPipeline.sendToDimension(message, player.dimension);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    public static class SendExsistingPacket
    {
        final EntityPlayer player;

        public SendExsistingPacket(EntityPlayer player)
        {
            this.player = player;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onPlayerTick(TickEvent.PlayerTickEvent event)
        {
            if (event.player == player)
            {
                for (EntityPlayer player1 : player.worldObj.playerEntities)
                {
                    proxy.getPokemob(player1);
                    boolean pokemob = player1.getEntityData().getBoolean("isPokemob");
                    PokeInfo info = proxy.playerMap.get(player1.getUniqueID());
                    if (info == null) pokemob = false;
                    PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(6));
                    MessageClient message = new MessageClient(buffer);
                    buffer.writeByte(MessageClient.SETPOKE);
                    buffer.writeInt(player1.getEntityId());
                    buffer.writeBoolean(pokemob);
                    if (pokemob)
                    {
                        buffer.writeFloat(info.originalHeight);
                        buffer.writeFloat(info.originalWidth);
                        buffer.writeNBTTagCompoundToBuffer(player1.getEntityData().getCompoundTag("Pokemob"));
                    }
                    PokecubeMod.packetPipeline.sendTo(message, (EntityPlayerMP) player);
                    MinecraftForge.EVENT_BUS.unregister(this);
                }
            }
        }
    }
}
