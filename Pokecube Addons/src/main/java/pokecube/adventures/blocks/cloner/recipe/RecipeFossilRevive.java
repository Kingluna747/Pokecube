package pokecube.adventures.blocks.cloner.recipe;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import pokecube.adventures.blocks.cloner.block.BlockCloner;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.utils.Tools;
import thut.lib.CompatWrapper;

public class RecipeFossilRevive implements IPoweredRecipe
{
    private static List<RecipeFossilRevive>                  recipeList = Lists.newArrayList();
    private static HashMap<PokedexEntry, RecipeFossilRevive> entryMap   = Maps.newHashMap();

    private static Comparator<RecipeFossilRevive>            comparator = new Comparator<RecipeFossilRevive>()
                                                                        {
                                                                            @Override
                                                                            public int compare(RecipeFossilRevive arg0,
                                                                                    RecipeFossilRevive arg1)
                                                                            {
                                                                                return arg1.priority - arg0.priority;
                                                                            }
                                                                        };

    public static List<RecipeFossilRevive> getRecipeList()
    {
        return Lists.newArrayList(recipeList);
    }

    public static void addRecipe(RecipeFossilRevive toAdd)
    {
        recipeList.add(toAdd);
        if (toAdd.pokedexEntry != null)
        {
            entryMap.put(toAdd.pokedexEntry, toAdd);
            recipeList.sort(comparator);
        }
    }

    public static RecipeFossilRevive getRecipe(PokedexEntry entry)
    {
        return entryMap.get(entry);
    }

    public PokedexEntry          pokedexEntry;
    public int                   energyCost;
    public int                   priority    = 0;
    public int                   level       = 20;
    public List<Integer>         remainIndex = Lists.newArrayList();
    public List<String>          neededGenes = Lists.newArrayList();
    public final List<ItemStack> recipeItems;
    public boolean               tame        = true;
    private IPokemob             pokemob;

    public RecipeFossilRevive(List<ItemStack> inputList, PokedexEntry entry, int cost)
    {
        this.recipeItems = inputList;
        this.pokedexEntry = entry;
        this.energyCost = cost;
    }

    public RecipeFossilRevive setTame(boolean tame)
    {
        this.tame = tame;
        return this;
    }

    public RecipeFossilRevive setLevel(int level)
    {
        this.level = level;
        return this;
    }

    public IPokemob getPokemob()
    {
        if (pokemob == null && pokedexEntry != null)
        {
            pokemob = (IPokemob) PokecubeMod.core.createPokemob(pokedexEntry, null);
            if (pokemob == null)
            {
                this.pokedexEntry = null;
            }
            else
            {
                pokemob.setPokedexEntry(pokedexEntry);
            }
        }
        return pokemob;
    }

    /** Used to check if a recipe matches current crafting inventory */
    @Override
    public boolean matches(InventoryCrafting inv, World worldIn)
    {
        if (inv.getSizeInventory() < getRecipeSize()) return false;
        ItemStack dna = inv.getStackInSlot(0);
        ItemStack egg = inv.getStackInSlot(1);
        if (!(CompatWrapper.isValid(egg) && CompatWrapper.isValid(dna))) return false;

        List<ItemStack> list = Lists.newArrayList(this.recipeItems);
        for (int i = 0; i < inv.getHeight(); ++i)
        {
            for (int j = 0; j < inv.getWidth(); ++j)
            {
                ItemStack itemstack = inv.getStackInRowAndColumn(j, i);

                if (CompatWrapper.isValid(itemstack))
                {
                    boolean flag = false;

                    for (ItemStack itemstack1 : list)
                    {
                        boolean matches = false;
                        if (itemstack1.getMetadata() == 32767) matches = itemstack.getItem() == itemstack1.getItem();
                        else matches = Tools.isSameStack(itemstack, itemstack1);
                        if (matches)
                        {
                            flag = true;
                            list.remove(itemstack1);
                            break;
                        }
                    }
                    if (!flag) { return false; }
                }
            }
        }
        return list.isEmpty();
    }

    @Override
    public int getEnergyCost()
    {
        return energyCost;
    }

    @Override
    public ItemStack toKeep(int slot, ItemStack stackIn, InventoryCrafting inv)
    {
        boolean remain = false;
        if (CompatWrapper.isValid(stackIn))
        {
            for (Integer i1 : remainIndex)
            {
                ItemStack stack = recipeItems.get(i1).copy();
                if (stack.getMetadata() == 32767) remain = stackIn.getItem() == stack.getItem();
                else remain = Tools.isSameStack(stackIn, stack);
            }
        }
        if (!remain)
        {
            ItemStack stack = net.minecraftforge.common.ForgeHooks.getContainerItem(stackIn);
            if (!CompatWrapper.isValid(stack))
            {
                if (CompatWrapper.isValid(stackIn)) stackIn.splitStack(1);
            }
            else stackIn = stack;
        }
        return stackIn;
    }

    @Override
    public boolean complete(IPoweredProgress tile)
    {
        List<ItemStack> remaining = Lists.newArrayList(getRemainingItems(tile.getCraftMatrix()));
        for (int i = 0; i < remaining.size(); i++)
        {
            ItemStack stack = remaining.get(i);
            if (CompatWrapper.isValid(stack)) tile.setInventorySlotContents(i, stack.copy());
            else tile.decrStackSize(i, 1);
        }
        tile.setInventorySlotContents(tile.getOutputSlot(), getRecipeOutput());
        World world = ((TileEntity) tile).getWorld();
        BlockPos pos = ((TileEntity) tile).getPos();
        EntityLiving entity = (EntityLiving) PokecubeMod.core.createPokemob(pokedexEntry, world);
        if (entity != null)
        {
            entity.setHealth(entity.getMaxHealth());
            // to avoid the death on spawn
            int exp = Tools.levelToXp(pokedexEntry.getEvolutionMode(), level);
            // that will make your pokemob around level 3-5.
            // You can give him more XP if you want
            entity = (EntityLiving) ((IPokemob) entity).setForSpawn(exp);
            if (tile.getUser() != null && tame) ((IPokemob) entity).setPokemonOwner(tile.getUser());
            EnumFacing dir = world.getBlockState(pos).getValue(BlockCloner.FACING);
            entity.setLocationAndAngles(pos.getX() + 0.5 + dir.getFrontOffsetX(), pos.getY() + 1,
                    pos.getZ() + 0.5 + dir.getFrontOffsetZ(), world.rand.nextFloat() * 360F, 0.0F);
            world.spawnEntityInWorld(entity);
            entity.playLivingSound();
        }
        return true;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv)
    {
        return CompatWrapper.nullStack;
    }

    @Override
    public int getRecipeSize()
    {
        return this.recipeItems.size();
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return CompatWrapper.nullStack;
    }
}
