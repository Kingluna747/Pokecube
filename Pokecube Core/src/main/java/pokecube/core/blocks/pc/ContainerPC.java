package pokecube.core.blocks.pc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.network.PCPacketHandler.MessageServer;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.PokecubePacketHandler.PokecubeServerPacket;
import pokecube.core.utils.PCSaveHandler;

public class ContainerPC extends Container
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
        // System.out.println(ConfigHandler.ONLYPOKECUBES);
        if (itemstack == null) return false;

        boolean eggorCube = PokecubeManager.isFilled(itemstack) || itemstack.getItem() == PokecubeItems.pokemobEgg;

        return eggorCube;
    }

    public final InventoryPC     inv;
    public final InventoryPlayer invPlayer;
    public final TileEntityPC    pcTile;

    public boolean               release   = false;
    // private GuiPC gpc;

    public boolean[]             toRelease = new boolean[54];

    public ContainerPC(InventoryPlayer ivplay, TileEntityPC pc)
    {
        super();
        xOffset = 0;
        yOffset = 0;
        InventoryPC temp = pc != null ? pc.getPC() != null ? pc.getPC() : InventoryPC.getPC(ivplay.player)
                : InventoryPC.getPC(ivplay.player);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) inv = new InventoryPC(temp);
        else inv = temp;
        invPlayer = ivplay;
        pcTile = pc;
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
        this.inventoryItemStacks.add(inv.getStackInSlot(par1Slot.getSlotIndex()));
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
        n = inv.getPage() * 54;
        for (int i = 0; i < 6; i++)
        {
            for (int j = 0; j < 9; j++)
            {
                addSlotToContainer(new SlotPC(inv, n + j + i * 9, 8 + j * 18 + xOffset, 18 + i * 18 + yOffset));
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
        inv.boxes[inv.getPage()] = name;

        if (PokecubeCore.isOnClientSide())
        {
            byte[] string = name.getBytes();
            byte[] message = new byte[string.length + 2];

            message[0] = 11;
            message[1] = (byte) string.length;
            for (int i = 2; i < message.length; i++)
            {
                message[i] = string[i - 2];
            } // TODO move this to PC packet handler instead
            PokecubeServerPacket packet = PokecubePacketHandler.makeServerPacket(PokecubeServerPacket.STATS, message);
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
        return inv.boxes[inv.getPage()];
    }

    @SideOnly(Side.CLIENT)
    public String getPageNb()
    {
        return Integer.toString(inv.getPage() + 1);
    }

    public boolean getRelease()
    {
        return release;
    }

    @Override
    public Slot getSlot(int par1)
    {
        return this.inventorySlots.get(par1);
    }

    public void gotoInventoryPage(int page)
    {
        if (page - 1 == inv.getPage()) return;

        inv.setPage(page - 1);

        bindInventories();
    }

    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        PCSaveHandler.getInstance().savePC(player.getUniqueID().toString());
        super.onContainerClosed(player);
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

    public void setRelease(boolean bool)
    {
        if (release && !bool)
        {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("page", inv.getPage());

            for (int i = 0; i < 54; i++)
            {
                if (toRelease[i])
                {
                    nbt.setBoolean("val" + i, true);
                }
            }
            MessageServer mess = new MessageServer(MessageServer.PCRELEASE, nbt);
            PokecubePacketHandler.sendToServer(mess);
        }
        release = bool;
    }

    @Override
    public ItemStack slotClick(int slot, int j, int flag, EntityPlayer entityplayer)
    {
        if (release)
        {
            if (slot < 54 && slot >= 0) toRelease[slot] = !toRelease[slot];
            return null;
        }
        return super.slotClick(slot, j, flag, entityplayer);
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
        inv.setPage((inv.getPage() == 0) && (dir == -1) ? InventoryPC.PAGECOUNT - 1
                : (inv.getPage() + dir) % InventoryPC.PAGECOUNT);
        bindInventories();
    }
}
