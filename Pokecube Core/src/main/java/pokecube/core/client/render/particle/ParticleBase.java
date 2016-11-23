package pokecube.core.client.render.particle;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import pokecube.core.client.render.PTezzelator;
import thut.api.maths.Vector3;

public class ParticleBase implements IParticle, IAnimatedParticle
{
    int     duration  = 10;
    int     lifetime  = 10;
    int     initTime  = 0;
    long    lastTick  = 0;
    int     animSpeed = 2;
    double  size      = 1;
    int     rgba      = 0xFFFFFFFF;
    String  name;
    boolean billboard = true;

    Vector3 velocity  = Vector3.empty;
    int[][] tex       = new int[1][2];

    public ParticleBase(int x, int y)
    {
        tex[0][0] = x;
        tex[0][1] = y;
        name = "";
    }

    @Override
    public int getDuration()
    {
        return duration;
    }

    @Override
    public void kill()
    {
        if (velocity != Vector3.empty && velocity != null)
        {
            // velocity.freeVectorFromPool();
        }
    }

    @Override
    public long lastTick()
    {
        return lastTick;
    }

    @Override
    public void render(double renderPartialTicks)
    {
        // This will draw a textured, coloured quad
        PTezzelator tez = PTezzelator.instance;
        ResourceLocation texture;
        GL11.glPushMatrix();

        if (billboard)
        {
            RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();
            GL11.glRotatef(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
        }
        setColour();

        float alpha = ((rgba >> 24) & 255) / 255f;
        float red = ((rgba >> 16) & 255) / 255f;
        float green = ((rgba >> 8) & 255) / 255f;
        float blue = (rgba & 255) / 255f;

        VertexFormat format = DefaultVertexFormats.POSITION_COLOR;
        texture = new ResourceLocation("pokecube", "textures/particles.png");
        FMLClientHandler.instance().getClient().renderEngine.bindTexture(texture);
        format = DefaultVertexFormats.POSITION_TEX_COLOR;

        int num = (getDuration() / animSpeed) % tex.length;

        int u = tex[num][0], v = tex[num][1];

        Vector3 temp = Vector3.getNewVector();

        double factor = ((lifetime - getDuration()) + renderPartialTicks);

        temp.set(velocity).scalarMultBy(factor);

        double u1 = u * 1d / 16d, v1 = v * 1d / 16d;
        double u2 = (u + 1) * 1d / 16d, v2 = (v + 1) * 1d / 16d;
        tez.begin(GL11.GL_QUADS, format);
        // Face 1
        tez.vertex(temp.x - size, temp.y - size, temp.z).tex(u1, v2).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x, temp.y - size, temp.z).tex(u2, v2).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x, temp.y + size, temp.z).tex(u2, v1).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x - size, temp.y + size, temp.z).tex(u1, v1).color(red, green, blue, alpha).endVertex();
        // Face 2
        tez.vertex(temp.x - size, temp.y - size, temp.z).tex(u1, v2).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x - size, temp.y + size, temp.z).tex(u1, v1).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x, temp.y + size, temp.z).tex(u2, v1).color(red, green, blue, alpha).endVertex();
        tez.vertex(temp.x, temp.y - size, temp.z).tex(u2, v2).color(red, green, blue, alpha).endVertex();
        tez.end();
        GL11.glPopMatrix();
    }

    void setColour()
    {
        if (name.equalsIgnoreCase("aurora"))
        {
            rgba = 0xFF000000;
            int num = ((getDuration() + initTime) / animSpeed) % 16;
            rgba += EnumDyeColor.byMetadata(num).getMapColor().colorValue;
        }
    }

    @Override
    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    @Override
    public void setLastTick(long tick)
    {
        lastTick = tick;
    }

    @Override
    public void setLifetime(int ticks)
    {
        duration = lifetime = ticks;
    }

    @Override
    public void setAnimSpeed(int speed)
    {
        animSpeed = Math.max(speed, 5);
    }

    @Override
    public void setTex(int[][] textures)
    {
        tex = textures;
    }

    public void setVelocity(Vector3 v)
    {
        velocity = v;
    }

    @Override
    public void setColour(int colour)
    {
        rgba = colour;
    }

    @Override
    public void setSize(float size)
    {
        this.size = size;
    }

    @Override
    public void setStartTime(int start)
    {
        initTime = start;
    }
}
