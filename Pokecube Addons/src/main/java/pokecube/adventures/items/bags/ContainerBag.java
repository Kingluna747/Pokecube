package pokecube.adventures.items.bags;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.interfaces.IPokemobUseable;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeServerPacket;

public class ContainerBag extends Container
{

    public static int STACKLIMIT = 64;
    public static int yOffset;
    public static int xOffset;

    /** Returns true if the item is a filled pokecube.
     *
     * @param itemstack
     *            the itemstack to test
     * @return true if the id is a filled pokecube one, false otherwise */
    public static boolean isItemValid(ItemStack itemstack)
    {
        boolean valid = PokecubeItems.isValidHeldItem(itemstack) || itemstack.getItem() instanceof IPokemobUseable;
        valid |= PokecubeItems.getFossilNumber(itemstack) != 0;
        boolean cube = PokecubeItems.getEmptyCube(itemstack) == itemstack.getItem()
                && !PokecubeManager.isFilled(itemstack);
        return valid || cube;
    }

    public final InventoryBag    invBag;
    public final InventoryPlayer invPlayer;

    public ContainerBag(InventoryPlayer ivplay)
    {
        super();
        xOffset = 0;
        yOffset = 0;
        invBag = InventoryBag.getBag(ivplay.player);
        invPlayer = ivplay;
        bindInventories();
    }

    /**
     * 
     */
    @Override
    protected Slot addSlotToContainer(Slot par1Slot)
    {
        par1Slot.slotNumber = this.inventorySlots.size();
        this.inventorySlots.add(par1Slot);
        this.inventoryItemStacks.add(invBag.getStackInSlot(par1Slot.getSlotIndex()));
        return par1Slot;
    }

    protected void bindInventories()
    {
        clearSlots();
        bindPCInventory();
        bindPlayerInventory();
    }

    protected void bindPCInventory()
    {
        int n = 0;
        n = invBag.getPage() * 54;
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlotToContainer(new SlotBag(invBag, n + j + i * 9, 8 + j * 18 + xOffset, 18 + i * 18 + yOffset));
            }
        }
        // int k = 0;
        for (Object o : inventorySlots)
        {
            if (o instanceof Slot)
            {
                ((Slot) o).onSlotChanged();
            }
        }
    }

    protected void bindPlayerInventory()
    {
        int offset = 64 + yOffset;

        for (int i = 0; i < 9; i++)
        {
            addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18 + xOffset, 142 + offset));
        }
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18 + xOffset, 84 + i * 18 + offset));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer entityplayer)
    {
        return true;
    }

    public void changeName(String name)
    {
        invBag.boxes[invBag.getPage()] = name;

        if (PokecubeCore.isOnClientSide())
        {
            byte[] string = name.getBytes();
            byte[] message = new byte[string.length + 2];

            message[0] = 11;
            message[1] = (byte) string.length;
            for (int i = 2; i < message.length; i++)
            {
                message[i] = string[i - 2];
            }
            PokecubeServerPacket packet = PokecubePacketHandler.makeServerPacket((byte) 6, message);
            PokecubePacketHandler.sendToServer(packet);
            return;
        }
    }

    protected void clearSlots()
    {
        this.inventorySlots.clear();
    }

    @SideOnly(Side.CLIENT)
    public String getPage()
    {
        return invBag.boxes[invBag.getPage()];
    }

    @SideOnly(Side.CLIENT)
    public String getPageNb()
    {
        return Integer.toString(invBag.getPage() + 1);
    }

    @Override
    public Slot getSlot(int par1)
    {
        return super.getSlot(par1);
    }

    public void gotoInventoryPage(int page)
    {
        if (page - 1 == invBag.getPage()) return;
        invBag.setPage(page - 1);
        bindInventories();
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        super.onContainerClosed(player);
        invBag.closeInventory(player);
    }

    /** args: slotID, itemStack to put in slot */
    @Override
    public void putStackInSlot(int par1, ItemStack par2ItemStack)
    {
        this.getSlot(par1).putStack(par2ItemStack);
    }

    @Override
    @SideOnly(Side.CLIENT)

    /** places itemstacks in first x slots, x being aitemstack.lenght */
    public void putStacksInSlots(ItemStack[] par1ArrayOfItemStack)
    {
        for (int i = 0; i < par1ArrayOfItemStack.length; ++i)
        {
            if (this.getSlot(i) != null) this.getSlot(i).putStack(par1ArrayOfItemStack[i]);
        }
    }

    @Override
    public ItemStack slotClick(int i, int j, int flag, EntityPlayer entityplayer)
    {
        return super.slotClick(i, j, flag, entityplayer);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index)
    {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(index);
        int numRows = 6;
        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < numRows * 9)
            {
                if (!this.mergeItemStack(itemstack1, numRows * 9, this.inventorySlots.size(), true)) { return null; }
            }
            else if (!this.mergeItemStack(itemstack1, 0, numRows * 9, false)) { return null; }

            if (itemstack1.stackSize == 0)
            {
                slot.putStack((ItemStack) null);
            }
            else
            {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }

    public void updateInventoryPages(int dir, InventoryPlayer invent)
    {
        invBag.setPage((invBag.getPage() == 0) && (dir == -1) ? InventoryBag.PAGECOUNT - 1
                : (invBag.getPage() + dir) % InventoryBag.PAGECOUNT);
        bindInventories();
    }

}
