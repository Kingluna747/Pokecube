package pokecube.core.blocks.tradingTable;

import java.util.List;
import java.util.Random;

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
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.TRSRTransformation;
import net.minecraftforge.client.model.obj.OBJModel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokecubes.PokecubeManager;

public class BlockTradingTable extends Block implements ITileEntityProvider
{
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool      TMC    = PropertyBool.create("tmc");
    private ExtendedBlockState            state  = new ExtendedBlockState(this, new IProperty[0],
            new IUnlistedProperty[] { OBJModel.OBJProperty.instance });

    public BlockTradingTable()
    {
        super(Material.cloth);
        this.setBlockBounds(0, 0, 0, 1, 0.75f, 1);
        this.setCreativeTab(PokecubeMod.creativeTabPokecube);
        this.setDefaultState(
                this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(TMC, false));
        this.setHardness(100);
        this.setResistance(100);
        this.setLightOpacity(0);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        dropItems(worldIn, pos);
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    /** Queries if this block should render in a given layer. ISmartBlockModel
     * can use MinecraftForgeClient.getRenderLayer to alter their model based on
     * layer */
    public boolean canRenderInLayer(EnumWorldBlockLayer layer)
    {
        return true;
    }

    @Override
    protected BlockState createBlockState()
    {
        return new BlockState(this, new IProperty[] { FACING, TMC });
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2)
    {
        return new TileEntityTradingTable();
    }

    private void dropItems(World world, BlockPos pos)
    {
        Random rand = new Random();
        TileEntity tile_entity = world.getTileEntity(pos);

        if (!(tile_entity instanceof IInventory)) { return; }

        if (tile_entity instanceof TileEntityTradingTable)
        {
            TileEntityTradingTable table = (TileEntityTradingTable) tile_entity;
            if (table.player1 != null) table.player1.closeScreen();
            if (table.player2 != null) table.player2.closeScreen();
        }

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        IInventory inventory = (IInventory) tile_entity;

        for (int i = 0; i < inventory.getSizeInventory(); i++)
        {
            ItemStack item = inventory.getStackInSlot(i);

            if (item != null && item.stackSize > 0)
            {
                float rx = rand.nextFloat() * 0.6F + 0.1F;
                float ry = rand.nextFloat() * 0.6F + 0.1F;
                float rz = rand.nextFloat() * 0.6F + 0.1F;
                EntityItem entity_item = new EntityItem(world, x + rx, y + ry, z + rz,
                        new ItemStack(item.getItem(), item.stackSize, item.getItemDamage()));
                if (item.hasTagCompound())
                {
                    entity_item.getEntityItem().setTagCompound((NBTTagCompound) item.getTagCompound().copy());
                }
                if (PokecubeManager.isFilled(item))
                {
                    ItemTossEvent toss = new ItemTossEvent(entity_item, PokecubeMod.getFakePlayer());
                    MinecraftForge.EVENT_BUS.post(toss);
                    boolean toPC = toss.isCanceled();
                    if (toPC)
                    {
                        continue;
                    }
                }
                float factor = 0.5F;
                entity_item.motionX = rand.nextGaussian() * factor;
                entity_item.motionY = rand.nextGaussian() * factor + 0.2F;
                entity_item.motionZ = rand.nextGaussian() * factor;
                world.spawnEntityInWorld(entity_item);
                item.stackSize = 0;
            }
        }
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        List<String> visible = Lists.newArrayList();
        // TODO better model that changes if next to PC to show it makes TMs

        visible.add(OBJModel.Group.ALL);
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
        if ((state.getValue(TMC))) ret += 8;
        return ret;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType()
    {
        return super.getRenderType();
    }

    @Override
    /** Convert the given metadata into a BlockState for this Block */
    public IBlockState getStateFromMeta(int meta)
    {
        EnumFacing enumfacing = EnumFacing.getFront(meta);

        boolean tmc = (meta & 8) > 0;
        if (enumfacing.getAxis() == EnumFacing.Axis.Y)
        {
            enumfacing = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, enumfacing).withProperty(TMC, tmc);
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

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side,
            float hitX, float hitY, float hitZ)
    {
        TileEntityTradingTable table = (TileEntityTradingTable) world.getTileEntity(pos);
        table.openGUI(player);
        return true;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
            int meta, EntityLivingBase placer)
    {
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }
}
