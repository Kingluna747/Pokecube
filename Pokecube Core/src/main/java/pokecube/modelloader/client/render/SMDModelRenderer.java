package pokecube.modelloader.client.render;

import java.util.HashMap;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.FMLClientHandler;
import pokecube.core.client.render.entity.RenderPokemobs;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IModel;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.smd.SMDModel;
import thut.core.client.render.tabula.components.Animation;

public class SMDModelRenderer<T extends EntityLiving> extends RenderLivingBase<T> implements IModelRenderer<T>
{
    public static final ResourceLocation FRZ          = new ResourceLocation(PokecubeMod.ID, "textures/FRZ.png");
    public static final ResourceLocation PAR          = new ResourceLocation(PokecubeMod.ID, "textures/PAR.png");
    public IAnimationChanger             animator;
    public SMDModel                      model;
    public String                        currentPhase = "idle";

    boolean                              blend;

    boolean                              light;

    int                                  src;

    int                                  dst;
    private boolean                      statusRender = false;

    public SMDModelRenderer(RenderManager renderManagerIn)
    {
        super(renderManagerIn, null, 0);
    }

    @Override
    public void doRender(T entity, double d, double d1, double d2, float f, float partialTick)
    {
        float f2 = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTick);
        float f3 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTick);
        float f4;
        if (entity.isRiding() && entity.getRidingEntity() instanceof EntityLivingBase)
        {
            EntityLivingBase entitylivingbase1 = (EntityLivingBase) entity.getRidingEntity();
            f2 = this.interpolateRotation(entitylivingbase1.prevRenderYawOffset, entitylivingbase1.renderYawOffset,
                    partialTick);
            f4 = MathHelper.wrapDegrees(f3 - f2);

            if (f4 < -85.0F)
            {
                f4 = -85.0F;
            }

            if (f4 >= 85.0F)
            {
                f4 = 85.0F;
            }

            f2 = f3 - f4;

            if (f4 * f4 > 2500.0F)
            {
                f2 += f4 * 0.2F;
            }
        }
        f4 = this.handleRotationFloat(entity, partialTick);
        float f6 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTick;

        if (f6 > 1.0F)
        {
            f6 = 1.0F;
        }
        GL11.glPushMatrix();
        if (animator != null) currentPhase = animator.modifyAnimation(entity, partialTick, currentPhase);
        GlStateManager.disableCull();
        model.setAnimation(currentPhase);
        if (!statusRender)
        {
            // model set textures;
        }
        model.render();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(T var1)
    {
        return RenderPokemobs.getInstance().getEntityTexturePublic(var1);
    }

    @Override
    public IPartTexturer getTexturer()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasAnimation(String phase)
    {
        // TODO Auto-generated method stub
        return false;
    }

    private void postRenderStatus()
    {
        if (light) GL11.glEnable(GL11.GL_LIGHTING);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(src, dst);
        statusRender = false;
    }

    private void preRenderStatus()
    {
        blend = GL11.glGetBoolean(GL11.GL_BLEND);
        light = GL11.glGetBoolean(GL11.GL_LIGHTING);
        src = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        dst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        statusRender = true;
    }

    @Override
    public void renderStatus(T entity, double d, double d1, double d2, float f, float partialTick)
    {
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        byte status;
        if ((status = pokemob.getStatus()) == IMoveConstants.STATUS_NON) return;
        ResourceLocation texture = null;
        if (status == IMoveConstants.STATUS_FRZ)
        {
            texture = FRZ;
        }
        else if (status == IMoveConstants.STATUS_PAR)
        {
            texture = PAR;
        }
        if (texture == null) return;

        FMLClientHandler.instance().getClient().renderEngine.bindTexture(texture);

        float time = (((Entity) pokemob).ticksExisted + partialTick);
        GL11.glPushMatrix();

        float speed = status == IMoveConstants.STATUS_FRZ ? 0.001f : 0.005f;

        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        float var5 = time * speed;
        float var6 = time * speed;
        GL11.glTranslatef(var5, var6, 0.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        float var7 = status == IMoveConstants.STATUS_FRZ ? 0.5f : 1F;
        GL11.glColor4f(var7, var7, var7, 0.5F);
        var7 = status == IMoveConstants.STATUS_FRZ ? 1.08f : 1.05F;
        GL11.glScalef(var7, var7, var7);
        preRenderStatus();
        doRender(entity, d, d1, d2, f, partialTick);
        postRenderStatus();
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glPopMatrix();
    }

    @Override
    public void setAnimation(String phase)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void scaleEntity(Entity entity, IModel model, float partialTick)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IAnimationChanger getAnimationChanger()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAnimation()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HashMap<String, Animation> getAnimations()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
