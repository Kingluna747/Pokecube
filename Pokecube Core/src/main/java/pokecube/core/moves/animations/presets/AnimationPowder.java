package pokecube.core.moves.animations.presets;

import java.util.Random;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.world.IWorldEventListener;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IMoveAnimation;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.moves.animations.AnimPreset;
import pokecube.core.moves.animations.MoveAnimationBase;
import thut.api.maths.Vector3;

@AnimPreset(getPreset="powder")
public class AnimationPowder extends MoveAnimationBase
{

    String  particle;
    float   width        = 1;
    boolean reverse      = false;
    int     meshId       = 0;

    public AnimationPowder()
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void clientAnimation(MovePacketInfo info, IWorldEventListener world, float partialTick)
    {
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void initColour(long time, float partialTicks, Move_Base move)
    {
        reallyInitRGBA();
        if (particle.equals("airbubble"))
        {
            rgba = 0x78000000 + EnumDyeColor.CYAN.getColorValue();
        }
        else if (particle.equals("aurora"))
        {
            int rand = ItemDye.DYE_COLORS[new Random(time / 10).nextInt(ItemDye.DYE_COLORS.length)];
            rgba = 0x61000000 + rand;
        }
        else if (particle.equals("iceshard"))
        {
            rgba = 0x78000000 + EnumDyeColor.CYAN.getColorValue();
        }
        else if (!customColour)
        {
            rgba = getColourFromMove(move, 255);
        }
    }

    @Override
    public void spawnClientEntities(MovePacketInfo info)
    {
        Vector3 target = info.target;
        initColour((info.attacker.getEntityWorld().getWorldTime()) * 20, 0, info.move);
        Vector3 temp = Vector3.getNewVector();
        Random rand = new Random();
        for (int i = 0; i < 100 * density; i++)
        {
            temp.set(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());
            temp.scalarMult(0.010 * width);
            temp.addTo(target);
            PokecubeCore.proxy.spawnParticle(info.attacker.world, particle, temp.copy(), null, rgba, particleLife);
        }
    }

    @Override
    public IMoveAnimation init(String preset)
    {
        this.particle = "powder";
        duration = 50;
        particleLife = 1;
        String[] args = preset.split(":");
        for (int i = 1; i < args.length; i++)
        {
            String ident = args[i].substring(0, 1);
            String val = args[i].substring(1);
            if (ident.equals("w"))
            {
                width = Float.parseFloat(val);
            }
            else if (ident.equals("d"))
            {
                density = Float.parseFloat(val);
            }
            else if (ident.equals("r"))
            {
                reverse = Boolean.parseBoolean(val);
            }
            else if (ident.equals("c"))
            {
                initRGBA(val);
            }
            else if (ident.equals("l"))
            {
                particleLife = Integer.parseInt(val);
            }
            else if (ident.equals("p"))
            {
                this.particle = val;
            }
        }
        return this;
    }

}
