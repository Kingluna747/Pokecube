package pokecube.adventures.legends;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.stats.ISpecialCaptureCondition;
import pokecube.core.database.stats.ISpecialSpawnCondition;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.ItemPokedex;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;
import thut.lib.CompatWrapper;

public class LegendarySpawns
{
    /** Uses player interact here to also prevent opening of inventories.
     * 
     * @param evt */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void interactRightClickBlock(PlayerInteractEvent.RightClickBlock evt)
    {
        boolean invalid = !evt.getEntityPlayer().isSneaking() || !CompatWrapper.isValid(evt.getItemStack())
                || !(evt.getItemStack().getItem() instanceof ItemPokedex) || !evt.getItemStack().hasTagCompound()
                || evt.getWorld().isRemote;
        if (invalid) return;
        Block block = null;
        EntityPlayer playerIn = evt.getEntityPlayer();
        World worldIn = evt.getWorld();
        BlockPos pos = evt.getPos();
        IBlockState state = evt.getWorld().getBlockState(evt.getPos());
        block = state.getBlock();
        PokedexEntry entry = Database.getEntry(evt.getItemStack().getTagCompound().getString("F"));
        if (block == Blocks.DIAMOND_BLOCK && entry != null)
        {
            ISpecialSpawnCondition condition = ISpecialSpawnCondition.spawnMap.get(entry);
            ISpecialCaptureCondition condition2 = ISpecialCaptureCondition.captureMap.get(entry);
            if (condition != null)
            {
                Vector3 location = Vector3.getNewVector().set(pos);
                if (condition.canSpawn(playerIn, location))
                {
                    EntityLiving entity = (EntityLiving) PokecubeMod.core.createPokemob(entry, worldIn);
                    if (condition2 != null && !condition2.canCapture(playerIn, (IPokemob) entity)) return;
                    entity.setHealth(entity.getMaxHealth());
                    location.add(0, 1, 0).moveEntity(entity);
                    condition.onSpawn((IPokemob) entity);
                    if (((IPokemob) entity).getExp() < 100)
                    {
                        entity = (EntityLiving) ((IPokemob) entity)
                                .setForSpawn(Tools.levelToXp(entry.getEvolutionMode(), 50));
                    }
                    worldIn.spawnEntity(entity);
                }
            }
            evt.setCanceled(true);
        }
    }
}
