/**
 *
 */
package pokecube.core.items;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.village.Village;
import net.minecraft.world.World;
import pokecube.core.PokecubeCore;
import pokecube.core.blocks.healtable.BlockHealTable;
import pokecube.core.commands.CommandTools;
import pokecube.core.database.Database;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.stats.StatsCollector;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.handlers.Config;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeClientPacket;
import pokecube.core.utils.PokecubeSerializer;
import thut.api.maths.Vector3;
import thut.api.maths.Vector4;
import thut.api.terrain.TerrainManager;

/** @author Manchou */
public class ItemPokedex extends Item
{
    public ItemPokedex()
    {
        super();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack itemstack, World world, EntityPlayer player,
            EnumHand hand)
    {
        if (!world.isRemote) SpawnHandler.refreshTerrain(Vector3.getNewVector().set(player), player.worldObj);
        if (!player.isSneaking())
        {
            showGui(player);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
        }
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemstack);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        Vector3 hit = Vector3.getNewVector().set(pos);
        Block block = hit.getBlockState(worldIn).getBlock();
        if (block instanceof BlockHealTable)
        {
            if (worldIn.isRemote) CommandTools.sendMessage(playerIn, "pokedex.setteleport");
            Vector4 loc = new Vector4(playerIn);
            loc.y++;
            PokecubeSerializer.getInstance().setTeleport(loc, playerIn.getCachedUniqueIdString());
            PokecubeSerializer.getInstance().save();
            if (!worldIn.isRemote)
            {
                NBTTagCompound teletag = new NBTTagCompound();
                PokecubeSerializer.getInstance().writePlayerTeleports(playerIn.getUniqueID(), teletag);

                PokecubeClientPacket packet = new PokecubeClientPacket(PokecubeClientPacket.TELEPORTLIST, teletag);
                PokecubePacketHandler.sendToClient(packet, playerIn);
            }
            return EnumActionResult.SUCCESS;
        }

        if (playerIn.isSneaking() && !worldIn.isRemote)
        {
            String biomeList = "Nothing";

            List<PokedexEntry> pokemobs = Lists.newArrayList();

            for (PokedexEntry e : Database.spawnables)
            {
                if (e.getSpawnData().isValid(worldIn, hit))
                {
                    pokemobs.add(e);
                }
            }
            if (!pokemobs.isEmpty())
            {
                final World world = worldIn;
                final Vector3 v = hit;
                Collections.sort(pokemobs, new Comparator<PokedexEntry>()
                {
                    @Override
                    public int compare(PokedexEntry o1, PokedexEntry o2)
                    {
                        float w1 = o1.getSpawnData().getWeight(o1.getSpawnData().getMatcher(world, v));
                        float w2 = o2.getSpawnData().getWeight(o2.getSpawnData().getMatcher(world, v));
                        return (int) (w2 * 10e6 - w1 * 10e6);
                    }
                });
                biomeList = pokemobs.toString();
            }
            ITextComponent message = CommandTools.makeTranslatedMessage("pokedex.locationinfo1", "green",
                    Database.spawnables.size());
            playerIn.addChatMessage(message);
            message = CommandTools.makeTranslatedMessage("pokedex.locationinfo2", "green",
                    Pokedex.getInstance().getEntries().size());
            playerIn.addChatMessage(message);
            message = CommandTools.makeTranslatedMessage("pokedex.locationinfo3", "", biomeList);
            playerIn.addChatMessage(message);
        }

        if (!playerIn.isSneaking()) showGui(playerIn);
        return EnumActionResult.FAIL;
    }

    private void showGui(EntityPlayer player)
    {
        if (PokecubeCore.isOnClientSide())
        {
            player.openGui(PokecubeCore.instance, Config.GUIPOKEDEX_ID, player.getEntityWorld(), 0, 0, 0);
        }
        else
        {
            NBTTagCompound nbt = new NBTTagCompound();
            StatsCollector.writeToNBT(nbt);

            NBTTagCompound tag = new NBTTagCompound();
            TerrainManager.getInstance().getTerrainForEntity(player).saveToNBT(tag);

            nbt.setBoolean("hasTerrain", true);
            nbt.setTag("terrain", tag);

            List<Village> villages = player.getEntityWorld().getVillageCollection().getVillageList();
            if (villages.size() > 0)
            {
                final BlockPos pos = player.getPosition();
                Collections.sort(villages, new Comparator<Village>()
                {
                    @Override
                    public int compare(Village o1, Village o2)
                    {
                        return (int) (pos.distanceSq(o1.getCenter()) - pos.distanceSq(o2.getCenter()));
                    }
                });
                Vector3 temp = Vector3.getNewVector().set(villages.get(0).getCenter());
                temp.writeToNBT(tag, "village");
            }
            PokecubeClientPacket packet = new PokecubeClientPacket(PokecubeClientPacket.STATS, nbt);
            PokecubePacketHandler.sendToClient(packet, player);
        }
    }

}
