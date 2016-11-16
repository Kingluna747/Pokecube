package pokecube.core.blocks.nests;

import java.util.HashSet;
import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.SpawnData;
import pokecube.core.database.SpawnBiomeMatcher;
import pokecube.core.events.EggEvent;
import pokecube.core.events.handlers.SpawnHandler;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.utils.CompatWrapper;
import thut.api.maths.Vector3;

public class TileEntityNest extends TileEntity implements ITickable, IInventory
{

    private ItemStack[] inventory = new ItemStack[27];

    int                 pokedexNb = 0;

    HashSet<IPokemob>   residents = new HashSet<IPokemob>();
    int                 time      = 0;

    public boolean addForbiddenSpawningCoord()
    {
        return SpawnHandler.addForbiddenSpawningCoord(getPos(), worldObj.provider.getDimension(), 10);
    }

    public void addResident(IPokemob resident)
    {
        residents.add(resident);
    }

    @Override
    public void clear()
    {

    }

    @Override
    public void closeInventory(EntityPlayer player)
    {
    }

    @Override
    public ItemStack decrStackSize(int index, int count)
    {
        if (this.inventory[index] != null)
        {
            ItemStack itemStack;

            itemStack = inventory[index].splitStack(count);

            if (inventory[index].stackSize <= 0) inventory[index] = null;

            return itemStack;
        }
        return null;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }

    @Override
    public int getField(int id)
    {
        return 0;
    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public int getSizeInventory()
    {
        return 27;
    }

    @Override
    public ItemStack getStackInSlot(int index)
    {
        return inventory[index];
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    public void init()
    {
        //TODO init spawn for nest here.
    }

    @Override
    public void invalidate()
    {
        super.invalidate();
        pokedexNb = 0;
        removeForbiddenSpawningCoord();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return false;
    }

    @Override
    public void openInventory(EntityPlayer player)
    {
    }

    /** Reads a tile entity from NBT. */
    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        pokedexNb = nbt.getInteger("pokedexNb");
        time = nbt.getInteger("time");
        NBTBase temp = nbt.getTag("Inventory");
        if (temp instanceof NBTTagList)
        {
            NBTTagList tagList = (NBTTagList) temp;
            for (int i = 0; i < tagList.tagCount(); i++)
            {
                NBTTagCompound tag = tagList.getCompoundTagAt(i);
                byte slot = tag.getByte("Slot");

                if (slot >= 0 && slot < inventory.length)
                {
                    inventory[slot] = CompatWrapper.fromTag(tag);
                }
            }
        }
    }

    public boolean removeForbiddenSpawningCoord()
    {
        return SpawnHandler.removeForbiddenSpawningCoord(getPos(), worldObj.provider.getDimension());
    }

    public void removeResident(IPokemob resident)
    {
        residents.remove(resident);
    }

    @Override
    public ItemStack removeStackFromSlot(int index)
    {
        if (inventory[index] != null)
        {
            ItemStack stack = inventory[index];
            inventory[index] = null;
            return stack;
        }
        return null;
    }

    @Override
    public void setField(int id, int value)
    {
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack)
    {
        inventory[index] = stack;
    }

    @Override
    public void update()
    {
        time++;
        int power = worldObj.getRedstonePower(getPos(), EnumFacing.DOWN);// .getBlockPowerInput(xCoord,
                                                                         // yCoord,
                                                                         // zCoord);

        if (worldObj.isRemote || (worldObj.getDifficulty() == EnumDifficulty.PEACEFUL && power == 0)) return;

        if (worldObj.getClosestPlayer(getPos().getX(), getPos().getY(), getPos().getZ(),
                PokecubeMod.core.getConfig().maxSpawnRadius, false) == null)
            return;

        if (pokedexNb == 0 && time >= 200)
        {
            time = 0;
            init();
        }
        if (pokedexNb == 0) return;
        int num = 3;
        PokedexEntry entry = Database.getEntry(pokedexNb);

        SpawnData data = entry.getSpawnData();
        if (data != null)
        {
            Vector3 here = Vector3.getNewVector().set(this);
            SpawnBiomeMatcher matcher = data.getMatcher(worldObj, here);
            int min = data.getMin(matcher);
            num = min + worldObj.rand.nextInt(data.getMax(matcher) - min + 1);
        }
        // System.out.println("tick");
        if (residents.size() < num && time > 200 + worldObj.rand.nextInt(2000))
        {
            time = 0;
            ItemStack eggItem = ItemPokemobEgg.getEggStack(pokedexNb);
            NBTTagCompound nbt = eggItem.getTagCompound();
            nbt.setIntArray("nestLocation", new int[] { getPos().getX(), getPos().getY(), getPos().getZ() });
            eggItem.setTagCompound(nbt);
            Random rand = new Random();
            EntityPokemobEgg egg = new EntityPokemobEgg(worldObj, getPos().getX() + rand.nextGaussian(),
                    getPos().getY() + 1, getPos().getZ() + rand.nextGaussian(), eggItem, null);
            EggEvent.Lay event = new EggEvent.Lay(egg);
            MinecraftForge.EVENT_BUS.post(event);
            if (!event.isCanceled())
            {
                worldObj.spawnEntityInWorld(egg);
            }
        }
    }

    @Override
    public void validate()
    {
        super.validate();

        addForbiddenSpawningCoord();
    }

    /** Writes a tile entity to NBT.
     * 
     * @return */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setInteger("pokedexNb", pokedexNb);
        nbt.setInteger("time", time);
        NBTTagList itemList = new NBTTagList();

        for (int i = 0; i < inventory.length; i++)
        {
            ItemStack stack = inventory[i];

            if (stack != null)
            {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("Slot", (byte) i);
                stack.writeToNBT(tag);
                itemList.appendTag(tag);
            }
        }
        nbt.setTag("Inventory", itemList);
        return nbt;
    }
}
