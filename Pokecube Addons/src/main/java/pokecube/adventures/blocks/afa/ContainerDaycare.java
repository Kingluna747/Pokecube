package pokecube.adventures.blocks.afa;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ContainerDaycare extends Container
{
    private static class SlotDaycare extends Slot
    {
        public SlotDaycare(IInventory inventoryIn, int index, int xPosition, int yPosition)
        {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack itemstack)
        {
            return this.inventory.isItemValidForSlot(getSlotIndex(), itemstack);
        }
    }

    public TileEntityDaycare tile;
    public World             worldObj;
    public int               range = 4;
    public BlockPos          pos;

    public ContainerDaycare(TileEntityDaycare tile, InventoryPlayer playerInv)
    {
        super();
        this.tile = tile;
        this.worldObj = tile.getWorld();
        this.pos = tile.getPos();
        bindInventories(playerInv);
    }

    public void bindInventories(InventoryPlayer playerInv)
    {
        clearSlots();
        bindPlayerInventory(playerInv);
        addSlotToContainer(new SlotDaycare(tile, 0, 15, 12));
        addSlotToContainer(new SlotDaycare(tile, 1, 15, 32));
        addSlotToContainer(new SlotDaycare(tile, 2, 15, 52));
    }

    private void bindPlayerInventory(InventoryPlayer playerInventory)
    {
        // Inventory
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                addSlotToContainer(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));

        // Action Bar
        for (int x = 0; x < 9; x++)
            addSlotToContainer(new Slot(playerInventory, x, 8 + x * 18, 142));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn)
    {
        return tile.isUseableByPlayer(playerIn);
    }

    protected void clearSlots()
    {
        this.inventorySlots.clear();
    }

    @Override
    public void addListener(IContainerListener listener)
    {
        super.addListener(listener);
        listener.sendAllWindowProperties(this, this.tile);
    }

    @Override
    /** Looks for changes made in the container, sends them to every
     * listener. */
    public void detectAndSendChanges()
    {
        for (int i = 0; i < this.listeners.size(); ++i)
        {
            IContainerListener icrafting = this.listeners.get(i);
            if (range != tile.getField(0)) icrafting.sendProgressBarUpdate(this, 0, this.tile.getField(0));
        }
        range = tile.getField(0);
        super.detectAndSendChanges();
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player)
    {
        if (slotId < 0) return null;
        if (clickTypeIn != ClickType.PICKUP && clickTypeIn != ClickType.PICKUP_ALL)
        {
            ItemStack itemstack = null;
            Slot slot = inventorySlots.get(slotId);

            if (slot != null && slot.getHasStack())
            {
                ItemStack itemstack1 = slot.getStack();
                itemstack = itemstack1.copy();

                if (slotId < 6)
                {
                    if (!mergeItemStack(itemstack1, 1, 37, true)) { return null; }
                }
                else
                {
                    if (itemstack != null && !tile.isItemValidForSlot(36, itemstack1)) { return null; }

                    if (!mergeItemStack(itemstack1, 0, 1, false)) { return null; }
                }

                if (itemstack1.stackSize == 0)
                {
                    slot.putStack(null);
                }
                else
                {
                    slot.onSlotChanged();
                }

                if (itemstack1.stackSize != itemstack.stackSize)
                {
                    // slot.onPickupFromSlot(itemstack1);
                }
                else
                {
                    return null;
                }
            }

            if (itemstack != null) { return itemstack; }
            return null;
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    {
        this.tile.setField(id, data);
    }
}
