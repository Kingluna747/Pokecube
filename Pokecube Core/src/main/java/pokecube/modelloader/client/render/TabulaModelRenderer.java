package pokecube.modelloader.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import pokecube.core.client.render.entity.RenderPokemob;
import pokecube.core.client.render.entity.RenderPokemobs;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.modelloader.client.render.TabulaPackLoader.TabulaModelSet;
import pokecube.modelloader.client.render.wrappers.TabulaWrapper;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.tabula.components.ModelJson;

public class TabulaModelRenderer<T extends EntityLiving> extends RenderLivingBase<T> implements IModelRenderer<T>
{
    public TabulaModelSet set;
    public TabulaWrapper  model;

    // Values used to properly reset GL state after rendering.
    boolean               blend;
    boolean               light;
    int                   src;
    int                   dst;

    private boolean       checkedForSleep   = false;
    private boolean       hasSleepAnimation = false;

    public TabulaModelRenderer(TabulaModelSet set)
    {
        super(Minecraft.getMinecraft().getRenderManager(), null, 0);
        this.set = set;
        this.model = new TabulaWrapper(set);
        mainModel = model;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        PokedexEntry entry = null;
        if (entity instanceof IPokemob) entry = ((IPokemob) entity).getPokedexEntry();
        else return;
        if (set == null)
        {
            System.err.println(entry);
            set = TabulaPackLoader.modelMap.get(entry.getBaseForme());
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity)
    {
        return RenderPokemobs.getInstance().getEntityTexturePublic(entity);
    }

    @Override
    public IPartTexturer getTexturer()
    {
        return set.texturer;
    }

    @Override
    public boolean hasPhase(String phase)
    {
        ModelJson modelj = null;
        if (set != null) modelj = set.parser.modelMap.get(set.model);
        return set.loadedAnimations.containsKey(phase) || (modelj != null && modelj.animationMap.containsKey(phase));
    }

    private void postRenderStatus()
    {
        if (light) GL11.glEnable(GL11.GL_LIGHTING);
        if (!blend) GL11.glDisable(GL11.GL_BLEND);
        GL11.glBlendFunc(src, dst);
        model.statusRender = false;
    }

    private void preRenderStatus()
    {
        blend = GL11.glGetBoolean(GL11.GL_BLEND);
        light = GL11.glGetBoolean(GL11.GL_LIGHTING);
        src = GL11.glGetInteger(GL11.GL_BLEND_SRC);
        dst = GL11.glGetInteger(GL11.GL_BLEND_DST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        model.statusRender = true;
    }

    @Override
    public void renderStatus(T entity, double d0, double d1, double d2, float f, float partialTick)
    {
        preRenderStatus();
        RenderPokemob.renderStatus(this, entity, d0, d1, d2, f, partialTick);
        postRenderStatus();
    }

    @Override
    public void setPhase(String phase)
    {
        model.phase = phase;
    }

    @Override
    protected boolean canRenderName(T entity)
    {
        return entity.getEntityData().getBoolean("isPlayer");
    }

    @Override
    protected void applyRotations(T par1EntityLiving, float par2, float par3, float par4)
    {
        super.applyRotations(par1EntityLiving, par2, par3, par4);
        if (!checkedForSleep)
        {
            checkedForSleep = true;
            hasSleepAnimation = hasPhase("sleeping") || hasPhase("sleep") || hasPhase("asleep");
        }
        if (hasSleepAnimation) return;
        boolean status = ((IPokemob) par1EntityLiving).getStatus() == IMoveConstants.STATUS_SLP;
        if (status || ((IPokemob) par1EntityLiving).getPokemonAIState(IMoveConstants.SLEEPING))
        {
            short timer = ((IPokemob) par1EntityLiving).getStatusTimer();
            float ratio = 1F;
            if (status)
            {
                if (timer <= 200 && timer > 175)
                {
                    ratio = 1F - ((timer - 175F) / 25F);
                }
                if (timer >= 0 && timer <= 25)
                {
                    ratio = 1F - ((25F - timer) / 25F);
                }
            }
            GL11.glTranslatef(0.5F * ratio, 0.2F * ratio, 0.0F);
            GL11.glRotatef(80 * ratio, 0.0F, 0.0F, 1F);
        }
    }
}
