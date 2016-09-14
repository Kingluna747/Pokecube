package pokecube.core.world.gen.village.buildings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces.PieceWeight;
import net.minecraft.world.gen.structure.StructureVillagePieces.Start;
import pokecube.core.interfaces.PokecubeMod;
import thut.api.maths.Vector3;

public class ComponentPokeMart extends ComponentVillageBase
{
    public static Class<? extends EntityLiving> seller      = null;
    public static Method                        setLocation = null;

    public static ComponentPokeMart buildComponent(PieceWeight villagePiece, Start startPiece,
            List<StructureComponent> pieces, Random random, int p1, int p2, int p3, EnumFacing facing, int p5)
    {
        StructureBoundingBox structureboundingbox = StructureBoundingBox.getComponentToAddBoundingBox(p1, p2, p3, 0, 0,
                0, 9, 8, 9, facing);
        return canVillageGoDeeper(structureboundingbox)
                && StructureComponent.findIntersecting(pieces, structureboundingbox) == null
                        ? new ComponentPokeMart(startPiece, p5, random, structureboundingbox, facing) : null;
    }

    private int averageGroundLevel = -1;
    boolean     spawned            = false;

    public ComponentPokeMart()
    {

    }

    public ComponentPokeMart(Start par1ComponentVillageStartPiece, int par2, Random par3Random,
            StructureBoundingBox par4StructureBoundingBox, EnumFacing par5)
    {
        super(par1ComponentVillageStartPiece, par2, par3Random, par4StructureBoundingBox, par5);
        this.coordBaseMode = par5;
        this.boundingBox = par4StructureBoundingBox;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox structureboundingbox)
    {
        averageGroundLevel = -1;
        if (this.averageGroundLevel < 0)
        {
            this.averageGroundLevel = this.getAverageGroundLevel(world, structureboundingbox);

            if (this.averageGroundLevel < 0) { return true; }

            this.boundingBox.offset(0, this.averageGroundLevel - this.boundingBox.maxY + 6 - 1, 0);
        }

        IBlockState spruce = Blocks.PLANKS.getDefaultState().withProperty(BlockPlanks.VARIANT,
                BlockPlanks.EnumType.SPRUCE);
        IBlockState clay = Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockColored.COLOR,
                EnumDyeColor.BLUE);
        IBlockState glass = Blocks.GLASS_PANE.getDefaultState();
        IBlockState logs = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE);
        IBlockState redCarpet = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.RED);
        IBlockState whiteCarpet = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.WHITE);
        IBlockState blackCarpet = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.BLACK);
        IBlockState greyCarpet = Blocks.CARPET.getDefaultState().withProperty(BlockCarpet.COLOR, EnumDyeColor.GRAY);

        // Hollow it out
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 0, 0, 8, 8, 8, Blocks.AIR.getDefaultState(), false);

        // Roof
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 6, 0, 8, 6, 8, clay, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 1, 6, 1, 7, 7, 7, clay, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 2, 6, 2, 6, 8, 6, clay, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 1, 6, 1, 7, 6, 7, spruce, false);

        // Floor
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 0, 0, 8, 2, 8, Blocks.COBBLESTONE.getDefaultState(),
                false);
        this.fillWithMetaBlocks(world, structureboundingbox, 1, 2, 1, 7, 2, 7, spruce, false);

        // Walls
        this.fillWithMetaBlocks(world, structureboundingbox, 8, 3, 0, 8, 5, 8, spruce, false);// RIGHT
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 3, 8, 8, 5, 8, spruce, false);// LEFT
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 3, 0, 0, 5, 8, spruce, false);// BACK
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 3, 0, 8, 5, 0, spruce, false);// FRONT
        // CORNERS
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 3, 0, 0, 5, 0, logs, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 3, 8, 0, 5, 8, logs, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 8, 3, 8, 8, 5, 8, logs, false);
        this.fillWithMetaBlocks(world, structureboundingbox, 8, 3, 0, 8, 5, 0, logs, false);

        // Windows
        this.fillWithMetaBlocks(world, structureboundingbox, 2, 4, 0, 2, 4, 0, glass, false);// front
        this.fillWithMetaBlocks(world, structureboundingbox, 6, 4, 0, 6, 4, 0, glass, false);// front
        this.fillWithMetaBlocks(world, structureboundingbox, 8, 4, 2, 8, 4, 3, glass, false);// left
        this.fillWithMetaBlocks(world, structureboundingbox, 8, 4, 5, 8, 4, 6, glass, false);// left
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 4, 2, 0, 4, 3, glass, false);// right
        this.fillWithMetaBlocks(world, structureboundingbox, 0, 4, 5, 0, 4, 6, glass, false);// right

        // carpet
        this.fillWithMetaBlocks(world, structureboundingbox, 3, 3, 2, 5, 3, 2, whiteCarpet, false);// white
        this.fillWithMetaBlocks(world, structureboundingbox, 3, 3, 3, 5, 3, 3, blackCarpet, false);// black
        this.fillWithMetaBlocks(world, structureboundingbox, 3, 3, 4, 5, 3, 4, redCarpet, false);// red
        this.placeBlockAtCurrentPosition(world, greyCarpet, 4, 3, 3, structureboundingbox);// grey

        // Ceiling Light
        // Ceiling Light
        this.placeBlockAtCurrentPosition(world, Blocks.REDSTONE_BLOCK.getDefaultState(), 4, 7, 3, structureboundingbox);// redstone
        // to
        // power
        // lamp
        this.placeBlockAtCurrentPosition(world, Blocks.LIT_REDSTONE_LAMP.getDefaultState(), 4, 6, 3,
                structureboundingbox);// lamp

        // counter
        this.fillWithMetaBlocks(world, structureboundingbox, 3, 3, 5, 5, 3, 5,
                Blocks.DOUBLE_STONE_SLAB.getDefaultState(), false);// front
        this.placeBlockAtCurrentPosition(world, Blocks.DOUBLE_STONE_SLAB.getDefaultState(), 6, 3, 6,
                structureboundingbox);// side
        this.placeBlockAtCurrentPosition(world, Blocks.DOUBLE_STONE_SLAB.getDefaultState(), 2, 3, 6,
                structureboundingbox);// side
        this.fillWithMetaBlocks(world, structureboundingbox, 2, 2, 6, 6, 2, 7,
                Blocks.DOUBLE_STONE_SLAB.getDefaultState(), false);// floor

        // accessories
        this.fillWithMetaBlocks(world, structureboundingbox, 2, 3, 2, 2, 4, 4, Blocks.BOOKSHELF.getDefaultState(),
                false);// Bookselves
        this.fillWithMetaBlocks(world, structureboundingbox, 6, 3, 2, 6, 4, 4, Blocks.BOOKSHELF.getDefaultState(),
                false);// Bookselves

        // DOOR
        this.func_189927_a(world, structureboundingbox, random, 4, 3, 0, coordBaseMode.rotateY());

        // Stairs
        Vector3 here = toAbsolute(4, 2, -1);
        if ((here.isAir(world) || here.getBlockState(world).getBlock().isCollidable())
                && !here.offset(EnumFacing.DOWN).isAir(world))
            this.placeBlockAtCurrentPosition(world, Blocks.STONE_STAIRS.getDefaultState(), 4, 2, -1,
                    structureboundingbox);//
        this.placeBlockAtCurrentPosition(world, Blocks.AIR.getDefaultState(), 4, 3, -1, structureboundingbox);//
        this.placeBlockAtCurrentPosition(world, Blocks.AIR.getDefaultState(), 4, 4, -1, structureboundingbox);//
        this.placeBlockAtCurrentPosition(world, Blocks.AIR.getDefaultState(), 4, 5, -1, structureboundingbox);//
        this.placeBlockAtCurrentPosition(world, Blocks.AIR.getDefaultState(), 4, 6, -1, structureboundingbox);//

        for (int l = 0; l < 6; ++l)
        {
            for (int i1 = 0; i1 < 9; ++i1)
            {
                this.clearCurrentPositionBlocksUpwards(world, i1, 9, l, structureboundingbox);
                this.fillDownwards(world, Blocks.COBBLESTONE.getDefaultState(), i1, -1, l, structureboundingbox);
            }
        }
        if (null != seller && PokecubeMod.core.getConfig().pokemartMerchant && !spawned)
        {
            spawned = true;
            int globalX = getXWithOffset(4, 6);
            int globalY = getYWithOffset(3);
            int globalZ = getZWithOffset(4, 6);
            try
            {
                EntityLiving merchant = seller.getConstructor(new Class[] { World.class })
                        .newInstance(new Object[] { world });
                merchant.setPosition(globalX, globalY, globalZ);
                world.spawnEntityInWorld(merchant);

                if (setLocation != null)
                {
                    setLocation.invoke(merchant, Vector3.getNewVector().set(globalX, globalY, globalZ));
                }
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e)
            {
                e.printStackTrace();
            }
        }

        return true;
    }

    protected int getVillagerType(int alreadySpawned)
    {
        return 0;// VillageHandlerCubeSalesman.ID;
    }
}
