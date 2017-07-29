package pokecube.modelloader.client.render;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLiving;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.modelloader.client.render.TabulaPackLoader.TabulaModelSet;
import pokecube.modelloader.client.render.wrappers.TabulaWrapper;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.tabula.components.Animation;
import thut.core.client.render.tabula.components.ModelJson;

public class TabulaModelRenderer<T extends EntityLiving> extends AbstractModelRenderer<T>
{
    public TabulaModelSet set;
    public TabulaWrapper  model;

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
        IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
        if (pokemob != null) entry = pokemob.getPokedexEntry();
        else return;
        if (set == null)
        {
            System.err.println(entry);
            set = TabulaPackLoader.modelMap.get(entry.getBaseForme());
        }
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    public IPartTexturer getTexturer()
    {
        return set.texturer;
    }

    @Override
    public boolean hasAnimation(String phase)
    {
        ModelJson modelj = null;
        if (set != null) modelj = set.parser.modelMap.get(set.model);
        return set.loadedAnimations.containsKey(phase) || (modelj != null && modelj.animationMap.containsKey(phase));
    }

    @Override
    public void setAnimation(String phase)
    {
        model.phase = phase;
    }

    @Override
    void setStatusRender(boolean value)
    {
        model.statusRender = value;
    }

    @Override
    public IAnimationChanger getAnimationChanger()
    {
        return set.animator;
    }

    @Override
    public String getAnimation()
    {
        return model.phase;
    }

    @Override
    public HashMap<String, Animation> getAnimations()
    {
        return set.loadedAnimations;
    }
}
