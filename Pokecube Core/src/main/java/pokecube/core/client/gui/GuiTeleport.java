/**
 *
 */
package pokecube.core.client.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.core.PokecubeCore;
import pokecube.core.client.Resources;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.pokemob.commandhandlers.TeleportHandler;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeServerPacket;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer.TeleDest;

public class GuiTeleport extends Gui
{
    protected static int       lightGrey = 0xDDDDDD;
    private static GuiTeleport instance;

    public static void create()
    {
        if (instance != null) MinecraftForge.EVENT_BUS.unregister(instance);
        instance = new GuiTeleport();
    }

    public static GuiTeleport instance()
    {
        if (instance == null) create();

        if (instance.locations == null)
            instance.locations = TeleportHandler.getTeleports(instance.minecraft.thePlayer.getCachedUniqueIdString());

        return instance;
    }

    protected FontRenderer fontRenderer;

    protected Minecraft    minecraft;

    public List<TeleDest>  locations;

    public int             indexLocation = 0;

    boolean                state         = false;

    /**
     *
     */
    private GuiTeleport()
    {
        minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
        MinecraftForge.EVENT_BUS.register(this);
        fontRenderer = minecraft.fontRendererObj;
        instance = this;
    }

    private void draw(RenderGameOverlayEvent.Post event)
    {
        GuiDisplayPokecubeInfo.teleDims[0] = 89;
        GuiDisplayPokecubeInfo.teleDims[1] = 25;
        IPokemob pokemob = GuiDisplayPokecubeInfo.instance().getCurrentPokemob();
        if (pokemob == null) return;
        GlStateManager.pushMatrix();
        GuiDisplayPokecubeInfo.applyTransform(PokecubeCore.core.getConfig().teleRef,
                PokecubeMod.core.getConfig().telePos, GuiDisplayPokecubeInfo.teleDims,
                PokecubeMod.core.getConfig().teleSize);
        GlStateManager.enableBlend();
        int h = 0;
        int w = 0;
        locations = TeleportHandler.getTeleports(minecraft.thePlayer.getCachedUniqueIdString());
        int i = 0;
        int xOffset = 0;
        int yOffset = 0;
        int dir = 1;
        // bind texture
        minecraft.renderEngine.bindTexture(Resources.GUI_BATTLE);
        this.drawTexturedModalRect(xOffset + w, yOffset + h, 44, 0, 90, 13);
        fontRenderer.drawString(I18n.format("gui.pokemob.teleport"), 2 + xOffset + w, 2 + yOffset + h, lightGrey);

        for (int k = 0; k < 1; k++)
        {
            if (k >= instance().locations.size()) break;
            TeleDest location = instance().locations.get((k + instance().indexLocation) % instance().locations.size());
            if (location != null)
            {
                if (k == 0) GL11.glColor4f(0F, 0.1F, 1.0F, 1.0F);
                else GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                String name = location.getName();
                int shift = 13 + 12 * i + yOffset + h;
                if (dir == -1)
                {
                    shift -= 25;
                }
                // bind texture
                minecraft.renderEngine.bindTexture(Resources.GUI_BATTLE);
                this.drawTexturedModalRect(xOffset + w, shift, 44, 22, 91, 12);
                fontRenderer.drawString(name, 5 + xOffset + w, shift + 2, PokeType.getType("fire").colour);
            }
            i++;
            GlStateManager.disableBlend();
        }
        GlStateManager.popMatrix();
    }

    public boolean getState()
    {
        return instance().state;
    }

    public void nextMove()
    {
        instance().indexLocation++;
        if (instance().locations.size() > 0)
            instance().indexLocation = instance().indexLocation % instance().locations.size();
        else instance().indexLocation = 0;
        PokecubeServerPacket packet = new PokecubeServerPacket(
                new byte[] { PokecubeServerPacket.TELEPORT, (byte) GuiTeleport.instance().indexLocation });
        PokecubePacketHandler.sendToServer(packet);
    }

    @SubscribeEvent
    public void onRenderHotbar(RenderGameOverlayEvent.Post event)
    {
        try
        {
            if (instance().state && (minecraft.currentScreen == null || GuiArranger.toggle)
                    && !((Minecraft) PokecubeCore.getMinecraftInstance()).gameSettings.hideGUI
                    && event.getType() == ElementType.HOTBAR)
                draw(event);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public void previousMove()
    {
        instance().indexLocation--;
        if (instance().indexLocation < 0) instance().indexLocation = Math.max(0, instance().locations.size() - 1);
        PokecubeServerPacket packet = new PokecubeServerPacket(
                new byte[] { PokecubeServerPacket.TELEPORT, (byte) GuiTeleport.instance().indexLocation });
        PokecubePacketHandler.sendToServer(packet);
    }

    public void refresh()
    {
        instance.locations = TeleportHandler.getTeleports(instance.minecraft.thePlayer.getCachedUniqueIdString());
    }

    public void setState(boolean state)
    {
        instance().state = state;
    }
}
