package pokecube.pokeplayer.tileentity;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.database.Database;
import pokecube.core.handlers.PokecubePlayerDataHandler;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.utils.Tools;
import pokecube.pokeplayer.PokeInfo;
import pokecube.pokeplayer.PokePlayer;
import thut.lib.CompatWrapper;

public class TileEntityTransformer extends TileEntityOwnable implements ITickable
{
    ItemStack stack    = CompatWrapper.nullStack;
    int[]     nums     = {};
    boolean   random   = false;
    int       stepTick = 20;

    public ItemStack getStack(ItemStack stack)
    {
        return stack;
    }

    public void onInteract(EntityPlayer player)
    {
        if (world.isRemote || random) return;
        if (canEdit(player))
        {
            if (!CompatWrapper.isValid(stack) && PokecubeManager.isFilled(player.getHeldItemMainhand()))
            {
                setStack(player.getHeldItemMainhand());
                player.inventory.setInventorySlotContents(player.inventory.currentItem, CompatWrapper.nullStack);
            }
            else
            {
                Tools.giveItem(player, stack);
                stack = CompatWrapper.nullStack;
            }
        }
    }

    public void onStepped(EntityPlayer player)
    {
        if (world.isRemote || stepTick > 0) return;
        PokeInfo info = PokecubePlayerDataHandler.getInstance().getPlayerData(player).getData(PokeInfo.class);
        boolean isPokemob = info.getPokemob(world) != null;
        if ((CompatWrapper.isValid(stack) || random) && !isPokemob)
        {
            IPokemob pokemob = getPokemob();
            if (pokemob != null) PokePlayer.PROXY.setPokemob(player, pokemob);
            if (CompatWrapper.isValid(stack) && pokemob != null)
            {
                stack = CompatWrapper.nullStack;
                stepTick = 50;
            }
            return;
        }
        else if (!CompatWrapper.isValid(stack) && !random && isPokemob)
        {
            stepTick = 50;
            IPokemob poke = PokePlayer.PROXY.getPokemob(player);
            NBTTagCompound tag = ((Entity) poke).getEntityData();
            poke.setPokemonNickname(tag.getString("oldName"));
            tag.removeTag("oldName");
            tag.removeTag("isPlayer");
            tag.removeTag("playerID");
            ItemStack pokemob = PokecubeManager.pokemobToItem(poke);
            if (player.capabilities.isFlying)
            {
                player.capabilities.isFlying = false;
                player.sendPlayerAbilities();
            }
            PokePlayer.PROXY.setPokemob(player, null);
            stack = pokemob;
            return;
        }
    }

    private IPokemob getPokemob()
    {
        if (random)
        {
            int num = 0;
            if (nums != null && nums.length > 0)
            {
                num = nums[new Random().nextInt(nums.length)];
            }
            else
            {
                List<Integer> numbers = Lists.newArrayList(Database.data.keySet());
                num = numbers.get(world.rand.nextInt(numbers.size()));
            }
            Entity entity = PokecubeMod.core.createPokemob(Database.getEntry(num), world);
            if (entity != null)
            {
                ((IPokemob) entity).specificSpawnInit();
            }
            return (IPokemob) entity;
        }
        IPokemob pokemob = PokecubeManager.itemToPokemob(stack, world);
        return pokemob;
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound)
    {
        super.readFromNBT(tagCompound);
        if (tagCompound.hasKey("stack"))
        {
            NBTTagCompound tag = tagCompound.getCompoundTag("stack");
            stack = CompatWrapper.fromTag(tag);
        }
        if (tagCompound.hasKey("nums"))
        {
            nums = tagCompound.getIntArray("nums");
        }
        stepTick = tagCompound.getInteger("stepTick");
        random = tagCompound.getBoolean("random");
    }

    public void setStack(ItemStack stack)
    {
        this.stack = stack;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound)
    {
        super.writeToNBT(tagCompound);
        if (CompatWrapper.isValid(stack))
        {
            NBTTagCompound tag = new NBTTagCompound();
            stack.writeToNBT(tag);
            tagCompound.setTag("stack", tag);
        }
        if (nums != null)
        {
            tagCompound.setIntArray("nums", nums);
        }
        tagCompound.setInteger("stepTick", stepTick);
        tagCompound.setBoolean("random", random);
        return tagCompound;
    }

    @Override
    public void update()
    {
        stepTick--;
    }

}
