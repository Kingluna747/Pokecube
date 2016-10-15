package pokecube.core.client.gui;

import java.util.LinkedList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.PokecubeMod;

public class GuiInfoMessages
{
    private static GuiInfoMessages instance;

    public static void addMessage(ITextComponent message)
    {
        instance.messages.push(message.getFormattedText());
        instance.time = Minecraft.getMinecraft().thePlayer.ticksExisted;
        instance.recent.push(message.getFormattedText());
        if (instance.messages.size() > 100)
        {
            instance.messages.remove(0);
        }
    }

    public static void clear()
    {
        if (instance != null)
        {
            instance.messages.clear();
            instance.recent.clear();
        }
    }

    private LinkedList<String> messages = Lists.newLinkedList();
    private LinkedList<String> recent   = Lists.newLinkedList();
    long                       time     = 0;

    int                        offset   = 0;

    public GuiInfoMessages()
    {
        MinecraftForge.EVENT_BUS.register(this);
        instance = this;
    }

    @SideOnly(Side.CLIENT)
    public void draw(RenderGameOverlayEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getType() == ElementType.CHAT && !(minecraft.currentScreen instanceof GuiChat)) return;
        if (event.getType() != ElementType.CHAT && (minecraft.currentScreen instanceof GuiChat)) return;
        int i = Mouse.getDWheel();
        int texH = minecraft.fontRendererObj.FONT_HEIGHT;
        int trim = PokecubeCore.core.getConfig().messageWidth;
        GL11.glPushMatrix();
        minecraft.entityRenderer.setupOverlayRendering();
        GuiDisplayPokecubeInfo
                .applyTransform(
                        PokecubeCore.core.getConfig().messageRef, PokecubeMod.core.getConfig().messagePos, new int[] {
                                PokecubeMod.core.getConfig().messageWidth, 7 * minecraft.fontRendererObj.FONT_HEIGHT },
                        PokecubeMod.core.getConfig().messageSize);
        int w = 0;
        int h = 0;
        int x = w, y = h;
        GL11.glNormal3f(0.0F, -1.0F, 0.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(0, -texH * 7, 0);
        GlStateManager.translate(0, 0, 0);
        int num = -1;
        if (event.getType() == ElementType.CHAT)
        {
            num = 7;
            offset += (int) (i != 0 ? Math.signum(i) : 0);
            if (offset < 0) offset = 0;
            if (offset > messages.size() - 7) offset = messages.size() - 7;
        }
        else if (time > minecraft.thePlayer.ticksExisted - 30)
        {
            num = 6;
            offset = 0;
        }
        else
        {
            offset = 0;
            num = 6;
            time = minecraft.thePlayer.ticksExisted;
            if (recent.size() > 0)
            {
                recent.remove(0);
            }
        }
        while (recent.size() > 8)
            recent.remove(0);
        List<String> toUse = num == 7 ? messages : recent;
        int size = toUse.size() - 1;
        num = Math.min(num, size + 1);
        for (int l = 0; l < num; l++)
        {
            int index = (l + offset);
            if (index < 0) index = 0;
            if (index > size) break;
            String mess = toUse.get(index);
            List<String> mess1 = minecraft.fontRendererObj.listFormattedStringToWidth(mess, trim);
            for (int j = 0; j < mess1.size(); j++)
            {
                h = y + texH * (l + j);
                w = x - trim;
                GuiScreen.drawRect(w, h, w + trim, h + texH, 0x66000000);
                minecraft.fontRendererObj.drawString(mess1.get(j), x - trim, h, 0xffffff, true);
                if (j != 0) l++;
            }
        }
        GL11.glPopMatrix();
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onRenderHotbar(RenderGameOverlayEvent.Post event)
    {
        try
        {
            if (!((Minecraft) PokecubeCore.getMinecraftInstance()).gameSettings.hideGUI
                    && (event.getType() == ElementType.HOTBAR || event.getType() == ElementType.CHAT))
            {
                draw(event);
            }
        }
        catch (Exception e)
        {

        }
    }
}
