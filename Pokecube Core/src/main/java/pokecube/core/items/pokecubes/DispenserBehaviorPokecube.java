package pokecube.core.items.pokecubes;

import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.util.FakePlayer;
import pokecube.core.PokecubeItems;
import pokecube.core.interfaces.IPokecube;
import pokecube.core.interfaces.PokecubeMod;
import thut.api.maths.Vector3;

public class DispenserBehaviorPokecube implements IBehaviorDispenseItem
{

    @Override
    public ItemStack dispense(IBlockSource iblocksource, ItemStack itemstack)
    {
        FakePlayer player = PokecubeMod.getFakePlayer();
        player.world = iblocksource.getWorld();
        player.posX = iblocksource.getX();
        player.posY = iblocksource.getY() - player.getEyeHeight();
        player.posZ = iblocksource.getZ();

        Vector3 direction = Vector3.getNewVector().set(iblocksource.getX(), iblocksource.getY(), iblocksource.getZ());

        if (itemstack.getItem() == PokecubeItems.pokemobEgg)
        {
            itemstack.onItemUse(player, iblocksource.getWorld(), iblocksource.getBlockPos(), EnumHand.MAIN_HAND,
                    EnumFacing.UP, 0.5f, 0.5f, 0.5f);
        }
        else if (itemstack.getItem() instanceof IPokecube)
        {
            IPokecube cube = (IPokecube) itemstack.getItem();
            cube.throwPokecube(iblocksource.getWorld(), player, itemstack, direction, 0.5f);
        }
        itemstack.splitStack(1);
        return itemstack;
    }

}
