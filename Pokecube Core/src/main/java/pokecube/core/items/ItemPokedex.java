/**
 *
 */
package pokecube.core.items;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.blocks.healtable.BlockHealTable;
import pokecube.core.commands.CommandTools;
import pokecube.core.database.Database;
import pokecube.core.database.Pokedex;
import pokecube.core.database.stats.StatsCollector;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.handlers.Config;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeClientPacket;
import pokecube.core.utils.PokecubeSerializer;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;

/** @author Manchou */
public class ItemPokedex extends Item
{
    public ItemPokedex()
    {
        super();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
    {

        if (!player.isSneaking())
        {
            showGui(player);
            return itemstack;
        }

        return itemstack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ)
    {
        Vector3 hit = Vector3.getNewVector().set(pos);
        Block block = hit.getBlockState(worldIn).getBlock();
        if (block instanceof BlockHealTable)
        {
            if (worldIn.isRemote) CommandTools.sendMessage(playerIn, "pokedex.setteleport");
            Vector4 loc = new Vector4(playerIn);
            loc.y++;
            PokecubeSerializer.getInstance().setTeleport(loc, playerIn.getUniqueID().toString());
            PokecubeSerializer.getInstance().save();
            if (!worldIn.isRemote)
            {
                NBTTagCompound teletag = new NBTTagCompound();
                PokecubeSerializer.getInstance().writePlayerTeleports(playerIn.getUniqueID(), teletag);

                PokecubeClientPacket packet = new PokecubeClientPacket(PokecubeClientPacket.TELEPORTLIST, teletag);
                PokecubePacketHandler.sendToClient(packet, playerIn);
            }
            return true;
        }

        if (playerIn.isSneaking() && !worldIn.isRemote)
        {
            TerrainSegment t = TerrainManager.getInstance().getTerrian(worldIn, hit);
            int b = t.getBiome(hit);
            String biomeList = SpawnHandler.spawnLists.get(b) != null ? SpawnHandler.spawnLists.get(b).toString()
                    : "Nothing";
            playerIn.addChatMessage(new ChatComponentTranslation("pokedex.locationinfo", Database.spawnables.size(),
                    Pokedex.getInstance().getEntries().size(), biomeList));
        }

        if (!playerIn.isSneaking()) showGui(playerIn);
        return super.onItemUse(stack, playerIn, worldIn, pos, side, hitX, hitY, hitZ);
    }

    private void showGui(EntityPlayer player)
    {
        if (PokecubeCore.isOnClientSide())
        {
            player.openGui(PokecubeCore.instance, Config.GUIPOKEDEX_ID, player.worldObj, 0, 0, 0);
        }
        else
        {
            NBTTagCompound nbt = new NBTTagCompound();
            StatsCollector.writeToNBT(nbt);

            NBTTagCompound tag = new NBTTagCompound();
            TerrainManager.getInstance().getTerrainForEntity(player).saveToNBT(tag);

            nbt.setBoolean("hasTerrain", true);
            nbt.setTag("terrain", tag);

            PokecubeClientPacket packet = new PokecubeClientPacket(PokecubeClientPacket.STATS, nbt);
            PokecubePacketHandler.sendToClient(packet, player);
        }
    }

}
