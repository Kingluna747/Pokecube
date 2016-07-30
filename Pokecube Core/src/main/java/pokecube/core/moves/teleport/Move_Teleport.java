package pokecube.core.moves.teleport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import pokecube.core.events.handlers.EventsHandler;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.moves.templates.Move_Utility;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeClientPacket;
import pokecube.core.utils.PokecubeSerializer.TeleDest;
import thut.api.entity.Transporter;
import thut.api.entity.Transporter.TelDestination;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;

public class Move_Teleport extends Move_Utility
{

    /** Teleport the entity to a random nearby position */
    public static boolean teleportRandomly(EntityLivingBase toTeleport)
    {
        double var1;
        double var3;
        double var5;
        Vector3 v = SpawnHandler.getRandomSpawningPointNearEntity(toTeleport.worldObj, toTeleport, 64);
        Vector3 v2 = Vector3.getNextSurfacePoint2(toTeleport.worldObj, v, Vector3.secondAxisNeg, 64);
        if (v2 != null) v.y = v2.y + 1;
        var1 = v.x;
        var3 = v.y;
        var5 = v.z;
        return teleportTo(toTeleport, var1, var3, var5);
    }

    /** Teleport the entity */
    protected static boolean teleportTo(EntityLivingBase toTeleport, double par1, double par3, double par5)
    {
        short var30 = 128;
        int num;

        TeleDest d = new TeleDest(new Vector4(par1, par3, par5, toTeleport.dimension));
        Vector3 loc = d.getLoc();
        int dim = d.getDim();

        World dest = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dim);

        TelDestination link = new TelDestination(dest, loc.getAABB(), loc.x, loc.y, loc.z, loc.intX(), loc.intY(),
                loc.intZ());
        Transporter.teleportEntity(toTeleport, link);
        for (num = 0; num < var30; ++num)
        {
            double var19 = num / (var30 - 1.0D);
            float var21 = (toTeleport.getRNG().nextFloat() - 0.5F) * 0.2F;
            float var22 = (toTeleport.getRNG().nextFloat() - 0.5F) * 0.2F;
            float var23 = (toTeleport.getRNG().nextFloat() - 0.5F) * 0.2F;
            double var24 = par1 + (toTeleport.posX - par1) * var19
                    + (toTeleport.getRNG().nextDouble() - 0.5D) * toTeleport.width * 2.0D;
            double var26 = par3 + (toTeleport.posY - par3) * var19
                    + toTeleport.getRNG().nextDouble() * toTeleport.height;
            double var28 = par5 + (toTeleport.posZ - par5) * var19
                    + (toTeleport.getRNG().nextDouble() - 0.5D) * toTeleport.width * 2.0D;
            toTeleport.worldObj.spawnParticle(EnumParticleTypes.PORTAL, var24, var26, var28, var21, var22, var23);
        }

        toTeleport.worldObj.playSoundEffect(par1, par3, par5, "mob.endermen.portal", 1.0F, 1.0F);
        toTeleport.playSound("mob.endermen.portal", 1.0F, 1.0F);
        return true;

    }

    public Move_Teleport(String name)
    {
        super(name);
    }

    @Override
    public boolean doAttack(IPokemob attacker, Entity attacked, float f)
    {
        doWorldAction(attacker, null);
        ((EntityCreature) attacker).setAttackTarget(null);
        attacker.setPokemonAIState(IMoveConstants.ANGRY, false);

        if (attacked instanceof EntityLiving)
        {
            ((EntityLiving) attacked).setAttackTarget(null);
        }
        if (attacked instanceof EntityCreature)
        {
            ((EntityCreature) attacker).setAttackTarget(null);
        }
        if (attacked instanceof IPokemob)
        {
            ((IPokemob) attacked).setPokemonAIState(IMoveConstants.ANGRY, false);
        }
        return true;
    }

    @Override
    public void doWorldAction(IPokemob user, Vector3 location)
    {
        boolean angry = user.getPokemonAIState(IMoveConstants.ANGRY);
        if (!angry && user.getPokemonOwner() instanceof EntityPlayer && ((EntityLivingBase) user).isServerWorld())
        {
            EventsHandler.recallAllPokemobsExcluding((EntityPlayer) user.getPokemonOwner(), (IPokemob) null);
            PokecubeClientPacket packet = new PokecubeClientPacket(new byte[] { PokecubeClientPacket.TELEPORTINDEX });
            PokecubePacketHandler.sendToClient(packet, (EntityPlayer) user.getPokemonOwner());
        }
        if (angry)
        {
            if (!user.getPokemonAIState(IMoveConstants.TAMED)) teleportRandomly((EntityLivingBase) user);
            user.setPokemonAIState(IMoveConstants.ANGRY, false);
        }
    }
}
