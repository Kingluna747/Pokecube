package pokecube.core.moves.animations.presets;

import java.util.Random;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemDye;
import net.minecraft.world.IWorldEventListener;
import pokecube.core.PokecubeCore;
import pokecube.core.interfaces.IMoveAnimation;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.moves.animations.AnimPreset;
import pokecube.core.moves.animations.MoveAnimationBase;
import thut.api.maths.Vector3;

@AnimPreset(getPreset="beam")
public class ParticleBeam extends MoveAnimationBase
{
    Vector3 v    = Vector3.getNewVector();
    boolean old  = false;
    Vector3 v1   = Vector3.getNewVector();

    public ParticleBeam()
    {
    }

    @Override
    public void clientAnimation(MovePacketInfo info, IWorldEventListener world, float partialTick)
    {

    }

    @Override
    public void initColour(long time, float partialTicks, Move_Base move)
    {
        if (customColour) return;
        if (particle.equals("airbubble"))
        {

        }
        else if (particle.equals("aurora"))
        {
            int rand = ItemDye.DYE_COLORS[new Random(time / 10).nextInt(ItemDye.DYE_COLORS.length)];
            rgba = 0x61000000 + rand;
        }
        else if (particle.equals("iceshard"))
        {
            rgba = 0x78000000 + EnumDyeColor.CYAN.getMapColor().colorValue;
        }
        else
        {
            rgba = getColourFromMove(move, 255);
        }
    }

    @Override
    public void spawnClientEntities(MovePacketInfo info)
    {
        Vector3 source = info.source;
        Vector3 target = info.target;
        initColour((info.attacker.getEntityWorld().getWorldTime()) * 20, 0, info.move);
        double dist = source.distanceTo(target);
        double frac = dist * info.currentTick / getDuration();
        Vector3 temp = Vector3.getNewVector().set(target).subtractFrom(source).norm();
        for (double i = frac; i < dist; i += 0.1)
            PokecubeCore.proxy.spawnParticle(info.attacker.getEntityWorld(), particle, source.add(temp.scalarMult(i)), null,
                    rgba, particleLife);
    }

    @Override
    public IMoveAnimation init(String preset)
    {
        rgba = 0xFFFFFFFF;
        String[] args = preset.split(":");
        this.particle = "misc";
        density = 0.5f;
        for (int i = 1; i < args.length; i++)
        {
            String ident = args[i].substring(0, 1);
            String val = args[i].substring(1);
            if (ident.equals("d"))
            {
                density = Float.parseFloat(val);
            }
            else if (ident.equals("p"))
            {
                this.particle = val;
            }
            else if (ident.equals("l"))
            {
                particleLife = Integer.parseInt(val);
            }
            else if (ident.equals("c"))
            {
                initRGBA(val);
            }
        }
        return this;
    }
}
