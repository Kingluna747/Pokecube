package pokecube.core.blocks.berries;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeItems;
import pokecube.core.blocks.berries.TileEntityBerries.Type;
import pokecube.core.items.berries.BerryManager;

/** @author Oracion
 * @author Manchou */
public class BlockBerryCrop extends BlockCrops implements ITileEntityProvider
{
    public BlockBerryCrop()
    {
        super();
        this.setTickRandomly(true);
        disableStats();
        float var3 = 0.3F;
        this.setBlockBounds(0.5F - var3, -0.05F, 0.5F - var3, 0.5F + var3, 1F, 0.5F + var3);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, Integer.valueOf(0))
                .withProperty(BerryManager.type, "cheri"));
    }

    /** Gets passed in the blockID of the block below and supposed to return
     * true if its allowed to grow on the type of blockID passed in. Args:
     * blockID */
    protected boolean canThisPlantGrowOnThisBlockID(Block par1)
    {
        return par1 == Blocks.farmland;
    }

    @Override
    protected BlockState createBlockState()
    {
        return new BlockState(this, new IProperty[] { AGE, BerryManager.type });
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta)
    {
        return new TileEntityBerries(Type.CROP);
    }

    @Override
    /** Get the actual Block state of this Block at the given position. This
     * applies properties not visible in the metadata, such as fence
     * connections. */
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        TileEntityBerries tile = (TileEntityBerries) worldIn.getTileEntity(pos);
        String name = tile == null ? "cheri" : BerryManager.berryNames.get(tile.getBerryId());
        if (name == null) name = "cheri";
        return state.withProperty(BerryManager.type, name);
    }

    @Override
    /** This returns a complete list of items dropped from this block.
     *
     * @param world
     *            The current world
     * @param pos
     *            Block position in world
     * @param state
     *            Current state
     * @param fortune
     *            Breakers fortune level
     * @return A ArrayList containing all items this block drops */
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        List<ItemStack> ret = new java.util.ArrayList<ItemStack>();

        Random rand = world instanceof World ? ((World) world).rand : RANDOM;

        int count = quantityDropped(state, fortune, rand);
        for (int i = 0; i < count; i++)
        {
            TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);
            ItemStack stack = BerryManager.getBerryItem(BerryManager.berryNames.get(tile.getBerryId()));
            if (stack != null)
            {
                ret.add(stack);
            }
        }
        return ret;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World worldIn, BlockPos pos)
    {
        return PokecubeItems.berries;
    }

    @Override
    /** Called when a user uses the creative pick block button on this block
     *
     * @param target
     *            The full target the player is looking at
     * @return A ItemStack to add to the player's inventory, Null if nothing
     *         should be added. */
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player)
    {
        TileEntityBerries tile = (TileEntityBerries) world.getTileEntity(pos);
        if (tile == null) return BerryManager.getBerryItem(1);
        return BerryManager.getBerryItem(tile.getBerryId());
    }

    @Override
    public void grow(World worldIn, BlockPos pos, IBlockState state)
    {
        TileEntityBerries tile = (TileEntityBerries) worldIn.getTileEntity(pos);
        tile.growCrop();
    }

    /** Returns the quantity of items to drop on block destruction. */
    @Override
    public int quantityDropped(Random par1Random)
    {
        return 1;
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
    }

}
