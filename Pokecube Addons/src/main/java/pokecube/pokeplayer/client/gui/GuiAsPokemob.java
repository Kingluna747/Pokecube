package pokecube.pokeplayer.client.gui;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import pokecube.core.PokecubeCore;
import pokecube.core.client.gui.GuiDisplayPokecubeInfo;
import pokecube.core.client.gui.GuiTeleport;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IMoveNames;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.PokecubeSerializer.TeleDest;
import pokecube.core.utils.Tools;
import pokecube.pokeplayer.PokePlayer;
import pokecube.pokeplayer.network.PacketDoActions;
import thut.api.maths.Vector3;

public class GuiAsPokemob extends GuiDisplayPokecubeInfo
{
    public static int     moveIndex = 0;
    public static boolean useMove   = false;

    public GuiAsPokemob()
    {
        super();
    }

    @Override
    public IPokemob[] getPokemobsToDisplay()
    {
        IPokemob pokemob = PokePlayer.PROXY.getPokemob(minecraft.thePlayer);
        if (pokemob != null) return new IPokemob[] { pokemob };
        return super.getPokemobsToDisplay();
    }

    @Override
    public IPokemob getCurrentPokemob()
    {
        IPokemob pokemob = PokePlayer.PROXY.getPokemob(minecraft.thePlayer);
        if (pokemob == null) return super.getCurrentPokemob();
        currentMoveIndex = moveIndex;
        return pokemob;
    }

    @Override
    public void pokemobAttack()
    {
        IPokemob pokemob = PokePlayer.PROXY.getPokemob(minecraft.thePlayer);
        if (pokemob == null)
        {
            super.pokemobAttack();
            return;
        }

        if (!useMove) return;
        useMove = false;

        EntityPlayer player = minecraft.thePlayer;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(11));
        buffer.writeByte(PacketDoActions.MOVEUSE);
        float range = 16;

        float contactRange = Math.max(1.5f, pokemob.getSize() * pokemob.getPokedexEntry().length);
        Move_Base move = MovesUtils.getMoveFromName(pokemob.getMove(pokemob.getMoveIndex()));
        if ((move.getAttackCategory() & IMoveConstants.CATEGORY_CONTACT) > 0) range = contactRange;

        Entity target = Tools.getPointedEntity(player, range);
        buffer.writeInt(target != null ? target.getEntityId() : 0);

        if (pokemob.getMove(pokemob.getMoveIndex()) == null) { return; }
        Vector3 look = Vector3.getNewVector().set(player.getLookVec());
        Vector3 pos = Vector3.getNewVector().set(player).addTo(0, player.getEyeHeight(), 0);
        Vector3 v = pos.findNextSolidBlock(player.worldObj, look, range);
        boolean attack = false;

        if (target != null)
        {
            if (v == null) v = Vector3.getNewVector();
            v.set(target);
        }
        else if (v == null)
        {
            v = pos.add(look.scalarMultBy(range));
        }

        attack = true;
        if (pokemob.getMove(pokemob.getMoveIndex()).equalsIgnoreCase(IMoveNames.MOVE_TELEPORT))
        {
            if (!GuiTeleport.instance().getState())
            {
                GuiTeleport.instance().setState(true);
                return;
            }
            GuiTeleport.instance().setState(false);

            Minecraft minecraft = (Minecraft) PokecubeCore.getMinecraftInstance();
            List<TeleDest> locations = PokecubeSerializer.getInstance()
                    .getTeleports(minecraft.thePlayer.getUniqueID().toString());

            if (locations.size() > 0)
            {
                buffer.writeBoolean(true);
            }
            else
            {
                buffer.writeBoolean(false);
            }
        }
        else if (!attack)
        {
            if ((target != null || v != null))
            {
                // String mess =
                // StatCollector.translateToLocalFormatted("pokemob.action.usemove",
                // pokemob.getPokemonDisplayName(),
                // MovesUtils.getTranslatedMove(move.getName()));
                // pokemob.displayMessageToOwner(mess);//TODO move message
            }
            buffer.writeBoolean(false);
        }
        else buffer.writeBoolean(false);

        if (v != null)
        {
            v.writeToBuff(buffer);
        }

        if (range == contactRange && target == null) return;

        PacketDoActions packet = new PacketDoActions(buffer);
        PokecubePacketHandler.sendToServer(packet);
    }

    @Override
    public void pokemobBack()
    {
        if (!isPokemob()) super.pokemobBack();
        else
        {

        }
    }

    @Override
    public void nextMove(int i)
    {
        if (!isPokemob()) super.nextMove(i);
        else
        {
            setMove(moveIndex + i);
        }
    }

    @Override
    public void previousMove(int j)
    {
        if (!isPokemob()) super.previousMove(j);
        else
        {
            setMove(moveIndex - j);
        }
    }

    @Override
    public void setMove(int num)
    {
        if (!isPokemob()) super.setMove(num);
        else
        {
            int numMoves = 0;
            String[] moves = getCurrentPokemob().getMoves();
            for (int i = 0; i < moves.length; i++)
            {
                numMoves++;
                if (moves[i] == null)
                {
                    break;
                }
            }
            if (num < 0)
            {
                num = numMoves - 1;
            }
            else if (num >= numMoves)
            {
                num = 0;
            }
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(6));
            buffer.writeByte(PacketDoActions.MOVEINDEX);
            buffer.writeByte((byte) num);
            PacketDoActions packet = new PacketDoActions(buffer);
            PokecubePacketHandler.sendToServer(packet);
        }
    }

    boolean isPokemob()
    {
        IPokemob pokemob = PokePlayer.PROXY.getPokemob(minecraft.thePlayer);
        return pokemob != null;
    }
}
