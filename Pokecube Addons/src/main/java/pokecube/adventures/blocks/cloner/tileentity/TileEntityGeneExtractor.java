package pokecube.adventures.blocks.cloner.tileentity;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.Optional.InterfaceList;
import pokecube.adventures.blocks.cloner.ClonerHelper;
import pokecube.adventures.blocks.cloner.crafting.CraftMatrix;
import pokecube.adventures.blocks.cloner.crafting.PoweredProcess;
import pokecube.adventures.blocks.cloner.recipe.IPoweredRecipe;
import pokecube.adventures.blocks.cloner.recipe.RecipeExtract;
import pokecube.adventures.blocks.cloner.recipe.RecipeSelector;
import pokecube.adventures.blocks.cloner.recipe.RecipeSelector.SelectorValue;
import pokecube.core.utils.Tools;
import thut.api.entity.genetics.Alleles;
import thut.api.entity.genetics.Gene;
import thut.api.entity.genetics.IMobGenetics;
import thut.lib.CompatWrapper;

@InterfaceList({ @Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "OpenComputers") })
public class TileEntityGeneExtractor extends TileClonerBase implements SimpleComponent
{
    int[][] sidedSlots = new int[6][];

    public TileEntityGeneExtractor()
    {
        /** 1 slot for object to extract genes from, 1 slot for gene container,
         * 1 slot for output, 1 slot for stabiliser. */
        super(4, 3);
        for (int i = 0; i < 6; i++)
        {
            if (sidedSlots[i] == null)
            {
                sidedSlots[i] = new int[] { 0, 1, 2, 3 };
            }
        }
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        return sidedSlots[side.ordinal()];
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        switch (index)
        {
        case 0:// DNA Container
            return ClonerHelper.isDNAContainer(stack) && ClonerHelper.getGenes(stack) == null;
        case 1:// DNA Selector
            boolean hasGenes = !ClonerHelper.getGeneSelectors(stack).isEmpty();
            boolean selector = hasGenes || RecipeSelector.getSelectorValue(stack) != RecipeSelector.defaultSelector;
            return hasGenes || selector;
        case 2:// DNA Source
            IMobGenetics genes = ClonerHelper.getGenes(stack);
            if (genes == null && CompatWrapper.isValid(stack)) for (ItemStack stack1 : ClonerHelper.DNAITEMS.keySet())
            {
                if (Tools.isSameStack(stack1, stack)) { return true; }
            }
            return genes != null;
        }
        return false;
    }

    @Override
    public String getName()
    {
        return "extractor";
    }

    @Override
    public CraftMatrix getCraftMatrix()
    {
        if (craftMatrix == null) this.craftMatrix = new CraftMatrix(null, this, 1, 3);
        return craftMatrix;
    }

    @Override
    public boolean isValid(Class<? extends IPoweredRecipe> recipe)
    {
        return recipe == RecipeExtract.class;
    }

    @Override
    public String getComponentName()
    {
        return "dnaExtractor";
    }

    @Callback
    @Optional.Method(modid = "OpenComputers")
    public Object[] getSourceInfo(Context context, Arguments args) throws Exception
    {
        IMobGenetics genes = ClonerHelper.getGenes(getStackInSlot(2));
        if (genes == null) throw new Exception("No Genes found in source slot.");
        List<String> values = Lists.newArrayList();
        for (ResourceLocation l : genes.getAlleles().keySet())
        {
            Alleles a = genes.getAlleles().get(l);
            Gene expressed = a.getExpressed();
            Gene parent1 = a.getAlleles()[0];
            Gene parent2 = a.getAlleles()[1];
            values.add(l.getResourcePath());
            values.add(expressed.toString());
            values.add(parent1.toString());
            values.add(parent2.toString());
        }
        return new Object[] { values.toArray() };
    }

    @Callback
    @Optional.Method(modid = "OpenComputers")
    public Object[] getSelectorInfo(Context context, Arguments args) throws Exception
    {
        ItemStack selector = getStackInSlot(1);
        Set<Class<? extends Gene>> getSelectors = ClonerHelper.getGeneSelectors(selector);
        if (getSelectors.isEmpty()) throw new Exception("No Selector found.");
        List<String> values = Lists.newArrayList();
        for (Class<? extends Gene> geneC : getSelectors)
        {
            try
            {
                Gene gene = geneC.newInstance();
                values.add(gene.getKey().getResourcePath());
            }
            catch (InstantiationException | IllegalAccessException e)
            {
            }
        }
        SelectorValue value = ClonerHelper.getSelectorValue(selector);
        values.add(value.toString());
        return new Object[] { values.toArray() };
    }

    @Callback
    @Optional.Method(modid = "OpenComputers")
    public Object[] setSelector(Context context, Arguments args) throws Exception
    {
        ItemStack selector = getStackInSlot(1);
        Set<Class<? extends Gene>> getSelectors = ClonerHelper.getGeneSelectors(selector);
        if (!getSelectors.isEmpty()) throw new Exception("Cannot set custom selector when a valid one is in the slot.");
        List<String> values = Lists.newArrayList();
        for (int i = 0; i < args.count(); i++)
        {
            values.add(args.checkString(i));
        }
        if (values.isEmpty()) throw new Exception("You need to specify some genes");
        RecipeExtract fixed = new RecipeExtract(true);
        ItemStack newSelector = new ItemStack(Items.WRITTEN_BOOK);
        newSelector.setTagCompound(new NBTTagCompound());
        NBTTagList pages = new NBTTagList();
        for (String s : values)
            pages.appendTag(new NBTTagString(String.format("{\"text\":\"%s\"}", s)));
        newSelector.getTagCompound().setTag("pages", pages);
        SelectorValue value = RecipeSelector.getSelectorValue(getStackInSlot(1));
        newSelector.getTagCompound().setTag(ClonerHelper.SELECTORTAG, value.save());
        newSelector.setStackDisplayName("Selector");
        fixed.setSelector(newSelector);
        this.setProcess(new PoweredProcess());
        this.getProcess().setTile(this);
        this.getProcess().recipe = fixed;
        this.getProcess().needed = fixed.getEnergyCost();
        // This is to inform the tile that it should recheck recipe.
        setInventorySlotContents(1, selector);
        return new String[] { "Set" };
    }
}
