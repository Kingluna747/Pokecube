package pokecube.modelloader.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.client.render.entity.RenderPokemob;
import pokecube.core.client.render.entity.RenderPokemobs;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.modelloader.client.render.AnimationLoader.Model;
import pokecube.modelloader.client.render.wrappers.ModelWrapper;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IExtendedModelPart;
import thut.core.client.render.model.IModelRenderer;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.tabula.components.Animation;
import thut.core.client.render.x3d.X3dModel;

public class DefaultIModelRenderer<T extends EntityLiving> extends RenderLivingBase<T> implements IModelRenderer<T>
{
    public static class Vector5
    {
        public Vector4 rotations;
        public int     time;

        public Vector5()
        {
            this.time = 0;
            this.rotations = new Vector4();
        }

        public Vector5(Vector4 rotation, int time)
        {
            this.rotations = rotation;
            this.time = time;
        }

        public Vector5 interpolate(Vector5 v, float time, boolean wrap)
        {
            if (v.time == 0) return this;

            if (Double.isNaN(rotations.x))
            {
                rotations = new Vector4();
            }
            Vector4 rotDiff = rotations.copy();

            if (rotations.x == rotations.z && rotations.z == rotations.y && rotations.y == rotations.w
                    && rotations.w == 0)
            {
                rotations.x = 1;
            }

            if (!v.rotations.equals(rotations))
            {
                rotDiff = v.rotations.subtractAngles(rotations);

                rotDiff = rotations.addAngles(rotDiff.scalarMult(time));
            }
            if (Double.isNaN(rotDiff.x))
            {
                rotDiff = new Vector4(0, 1, 0, 0);
            }
            Vector5 ret = new Vector5(rotDiff, v.time);
            return ret;
        }

        @Override
        public String toString()
        {
            return "|r:" + rotations + "|t:" + time;
        }
    }

    public static final String          DEFAULTPHASE      = "idle";
    public String                       name;
    public String                       currentPhase      = "idle";
    private boolean                     checkedForSleep   = false;
    private boolean                     hasSleepAnimation = false;
    public HashMap<String, PartInfo>    parts             = Maps.newHashMap();
    HashMap<String, ArrayList<Vector5>> global;
    public HashMap<String, Animation>   animations        = new HashMap<String, Animation>();
    public Set<String>                  headParts         = Sets.newHashSet();
    public TextureHelper                texturer;

    public IAnimationChanger            animator;;
    public Vector3                      offset            = Vector3.getNewVector();;
    public Vector3                      scale             = Vector3.getNewVector();

    public Vector5                      rotations         = new Vector5();

    public ModelWrapper                 model;
    public int                          headDir           = 2;
    public int                          headAxis          = 2;
    public int                          headAxis2         = 0;
    /** A set of names of shearable parts. */
    public Set<String>                  shearableParts    = Sets.newHashSet();

    /** A set of namess of dyeable parts. */
    public Set<String>                  dyeableParts      = Sets.newHashSet();
    public float[]                      headCaps          = { -180, 180 };

    public float[]                      headCaps1         = { -20, 40 };
    ResourceLocation                    texture;

    // Values used to properly reset the GL state after rendering.
    boolean                             blend;
    boolean                             light;
    int                                 src;
    int                                 dst;

    public DefaultIModelRenderer(HashMap<String, ArrayList<Vector5>> global, Model model)
    {
        super(Minecraft.getMinecraft().getRenderManager(), null, 0);
        name = model.name;
        this.model = new ModelWrapper(model, this);
        ModelWrapperEvent evt = new ModelWrapperEvent(this.model, name);
        MinecraftForge.EVENT_BUS.post(evt);
        this.model = evt.wrapper;
        this.mainModel = this.model;
        this.texture = model.texture;
        if (model.model.getResourcePath().contains(".x3d")) this.model.imodel = new X3dModel(model.model);
        if (this.model.imodel == null) { return; }
        initModelParts();
        if (headDir == 2)
        {
            headDir = (this.model.imodel instanceof X3dModel) ? 1 : -1;
        }
        this.global = global;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private HashMap<String, PartInfo> getChildren(IExtendedModelPart part)
    {
        HashMap<String, PartInfo> partsList = new HashMap<String, PartInfo>();
        for (String s : part.getSubParts().keySet())
        {
            PartInfo p = new PartInfo(s);
            IExtendedModelPart subPart = part.getSubParts().get(s);
            p.children = getChildren(subPart);
            partsList.put(s, p);
        }
        return partsList;
    }

    @Override
    protected ResourceLocation getEntityTexture(T var1)
    {
        return RenderPokemobs.getInstance().getEntityTexturePublic(var1);
    }

    private PartInfo getPartInfo(String partName)
    {
        PartInfo ret = null;
        for (PartInfo part : parts.values())
        {
            if (part.name.equalsIgnoreCase(partName)) return part;
            ret = getPartInfo(partName, part);
            if (ret != null) return ret;
        }
        for (IExtendedModelPart part : model.getParts().values())
        {
            if (part.getName().equals(partName))
            {
                PartInfo p = new PartInfo(part.getName());
                p.children = getChildren(part);
                boolean toAdd = true;
                IExtendedModelPart parent = part.getParent();
                while (parent != null && toAdd)
                {
                    toAdd = !parts.containsKey(parent.getName());
                    parent = parent.getParent();
                }
                if (toAdd) parts.put(partName, p);
                return p;
            }
        }

        return ret;
    }

    private PartInfo getPartInfo(String partName, PartInfo parent)
    {
        PartInfo ret = null;
        for (PartInfo part : parent.children.values())
        {
            if (part.name.equalsIgnoreCase(partName)) return part;
            ret = getPartInfo(partName, part);
            if (ret != null) return ret;
        }

        return ret;
    }

    @Override
    public IPartTexturer getTexturer()
    {
        return texturer;
    }

    @Override
    public boolean hasPhase(String phase)
    {
        return DefaultIModelRenderer.DEFAULTPHASE.equals(phase) || animations.containsKey(phase);
    }

    private void initModelParts()
    {
        if (model == null) return;

        for (String s : model.getParts().keySet())
        {
            if (model.getParts().get(s).getParent() == null && !parts.containsKey(s))
            {
                PartInfo p = getPartInfo(s);
                parts.put(s, p);
            }
        }
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
    public void renderStatus(T entity, double d, double d1, double d2, float f, float partialTick)
    {
        preRenderStatus();
        RenderPokemob.renderStatus(this, entity, d, d1, d2, f, partialTick);
        postRenderStatus();
    }

    @Override
    public void setPhase(String phase)
    {
        currentPhase = phase;
    }

    public void updateModel(HashMap<String, ArrayList<Vector5>> global, Model model)
    {
        name = model.name;
        this.texture = model.texture;
        initModelParts();
        this.global = global;
    }

    @Override
    protected boolean canRenderName(T entity)
    {
        return entity.getEntityData().getBoolean("isPlayer");
    }

    @Override
    protected void rotateCorpse(T par1EntityLiving, float par2, float par3, float par4)
    {
        super.rotateCorpse(par1EntityLiving, par2, par3, par4);
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
