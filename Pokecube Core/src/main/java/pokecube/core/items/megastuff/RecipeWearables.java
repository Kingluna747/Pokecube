package pokecube.core.items.megastuff;

import java.util.List;
import java.util.Locale;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import thut.lib.CompatWrapper;
import thut.lib.IDefaultRecipe;

public class RecipeWearables implements IDefaultRecipe
{
    private ItemStack output = CompatWrapper.nullStack;

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv)
    {
        return output;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return output;
    }

    @Override
    public boolean matches(InventoryCrafting inv, World worldIn)
    {
        output = CompatWrapper.nullStack;
        boolean ring = false;
        boolean dye = false;
        ItemStack dyeStack = CompatWrapper.nullStack;
        ItemStack ringStack = CompatWrapper.nullStack;
        for (int i = 0; i < inv.getSizeInventory(); i++)
        {
            ItemStack stack = inv.getStackInSlot(i);
            if (CompatWrapper.isValid(stack))
            {
                if (stack.getItem() instanceof ItemMegawearable)
                {
                    ring = true;
                    ringStack = stack;
                    continue;
                }
                List<ItemStack> dyes = OreDictionary.getOres("dye");
                boolean isDye = false;
                for (ItemStack dye1 : dyes)
                {
                    if (OreDictionary.itemMatches(dye1, stack, false))
                    {
                        isDye = true;
                        break;
                    }
                }
                if (isDye)
                {
                    dye = true;
                    dyeStack = stack;
                }
            }
        }
        if (dye && ring)
        {
            output = ringStack.copy();
            if (!output.hasTagCompound()) output.setTagCompound(new NBTTagCompound());
            int[] ids = OreDictionary.getOreIDs(dyeStack);
            int colour = dyeStack.getItemDamage();
            for (int i : ids)
            {
                String name = OreDictionary.getOreName(i);
                if (name.startsWith("dye") && name.length() > 3)
                {
                    String val = name.replace("dye", "").toUpperCase(Locale.ENGLISH);
                    try
                    {
                        EnumDyeColor type = EnumDyeColor.valueOf(val);
                        colour = type.getDyeDamage();
                        break;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            output.getTagCompound().setInteger("dyeColour", colour);
        }
        return CompatWrapper.isValid(output);
    }

    ResourceLocation registryName;

    @Override
    public IRecipe setRegistryName(ResourceLocation name)
    {
        registryName = name;
        return this;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return registryName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<IRecipe> getRegistryType()
    {
        Class<?> clazz = getClass();
        Class<IRecipe> ret = (Class<IRecipe>) clazz;
        return ret;
    }

}