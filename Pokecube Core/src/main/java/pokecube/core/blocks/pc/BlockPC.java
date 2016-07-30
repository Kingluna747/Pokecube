package pokecube.core.blocks.pc;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.handlers.Config;
import pokecube.core.interfaces.PokecubeMod;

public class BlockPC extends Block implements ITileEntityProvider
{
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool      TOP    = PropertyBool.create("top");
    private ExtendedBlockState            state  = new ExtendedBlockState(this, new IProperty[0],
            new IUnlistedProperty[] { OBJModel.OBJProperty.instance });

    public BlockPC()
    {
        super(Material.glass);
        this.setLightOpacity(0);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TOP,
                Boolean.valueOf(false)));
        this.setHardness(100);
        this.setResistance(100);
        this.setLightLevel(1f);
    }

    @Override
    protected BlockState createBlockState()
    {
        return new BlockState(this, new IProperty[] { FACING, TOP });
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return new TileEntityPC();
    }

    /** Gets the metadata of the item this Block can drop. This method is called
     * when the block gets destroyed. It returns the metadata of the dropped
     * item based on the old metadata of the block. */
    @Override
    public int damageDropped(IBlockState state)
    {
        return state.getValue(TOP)?8:0;
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        List<String> visible = Lists.newArrayList();
        if ((state.getValue(TOP)))
        {
            visible.add("pc_top");
        }
        else
        {
            visible.add("pc_base");
        }
        EnumFacing facing = state.getValue(FACING);
        facing = facing.rotateYCCW();

        TRSRTransformation transform = new TRSRTransformation(facing);
        OBJModel.OBJState retState = new OBJModel.OBJState(visible, true, transform);
        return ((IExtendedBlockState) this.state.getBaseState()).withProperty(OBJModel.OBJProperty.instance, retState);
    }

    @Override
    /** Convert the BlockState into the correct metadata value */
    public int getMetaFromState(IBlockState state)
    {
        int ret = state.getValue(FACING).getIndex();
        if ((state.getValue(TOP))) ret += 8;
        return ret;
    }

    @Override
    /** Convert the given metadata into a BlockState for this Block */
    public IBlockState getStateFromMeta(int meta)
    {
        EnumFacing enumfacing = EnumFacing.getFront(meta & 7);

        boolean top = (meta & 8) > 0;
        if (enumfacing.getAxis() == EnumFacing.Axis.Y)
        {
            enumfacing = EnumFacing.NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing).withProperty(TOP, Boolean.valueOf(top));
    }

    @Override
    public boolean isFullCube()
    {
        return false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return false;
    }

    @Override
    public boolean isVisuallyOpaque()
    {
        return false;
    }

    /** Called upon block activation (right click on the block.) */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side,
            float hitX, float hitY, float hitZ)
    {
        int meta = this.getMetaFromState(state);

        this.setLightLevel(1f);
        if (!((meta & 8) > 0)) { return false; }
        IBlockState down = world.getBlockState(pos.down());
        Block idDown = down.getBlock();
        int metaDown = idDown.getMetaFromState(down);
        if (!((!((metaDown & 8) > 0)) && idDown == this)) return false;

        InventoryPC inventoryPC = InventoryPC.getPC(player.getUniqueID().toString());

        if (inventoryPC != null)
        {
            if (world.isRemote)
            {
                return true;
            }
            else
            {
                player.openGui(PokecubeMod.core, Config.GUIPC_ID, world, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        else
        {
            return true;
        }
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
            int meta, EntityLivingBase placer)
    {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te instanceof TileEntityOwnable)
        {
            TileEntityOwnable tile = (TileEntityOwnable) te;
            tile.setPlacer(placer);
        }
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(TOP,
                ((meta & 8) > 0));
    }
}