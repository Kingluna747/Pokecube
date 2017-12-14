package pokecube.core.client.gui.watch;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.resources.I18n;
import pokecube.core.client.gui.helper.ScrollGui;
import pokecube.core.client.gui.watch.util.ListPage;
import pokecube.core.client.gui.watch.util.WatchPage;

public class StartPage extends ListPage
{
    private static class PageEntry implements IGuiListEntry
    {
        final WatchPage page;
        final GuiButton button;
        final int       offsetY;
        final int       guiHeight;

        public PageEntry(WatchPage page, int offsetY, int guiHeight)
        {
            this.page = page;
            button = new GuiButton(0, 0, 0, 140, 20, this.page.getTitle());
            this.offsetY = offsetY;
            this.guiHeight = guiHeight;
        }

        @Override
        public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_)
        {

        }

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY,
                boolean isSelected)
        {
            boolean fits = true;
            button.yPosition = y - 0;
            button.xPosition = x - 2;
            fits = button.yPosition >= offsetY;
            fits = fits && button.yPosition + button.height <= offsetY + guiHeight;
            if (fits)
            {
                button.drawButton(page.mc, mouseX, mouseY);
            }
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY)
        {
            boolean fits = true;
            fits = button.yPosition >= offsetY;
            fits = fits && button.yPosition + button.height <= offsetY + guiHeight;
            if (fits)
            {
                button.playPressSound(page.mc.getSoundHandler());
                // Index plus 1 as 0 is the start page, and no button for it.
                page.watch.pages.get(page.watch.index).onPageClosed();
                page.watch.index = slotIndex + 1;
                page.watch.pages.get(page.watch.index).onPageOpened();
            }
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY)
        {

        }

    }

    public StartPage(GuiPokeWatch watch)
    {
        super(watch);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initList()
    {
        List<IGuiListEntry> entries = Lists.newArrayList();
        int offsetX = (watch.width - 160) / 2 + 10;
        int offsetY = (watch.height - 160) / 2 + 20;
        int height = 120;
        for (WatchPage page : watch.pages)
        {
            if (!(page instanceof StartPage)) entries.add(new PageEntry(page, offsetY, height));
        }
        list = new ScrollGui(mc, 140, height, 20, offsetX, offsetY, entries);
    }

    @Override
    public String getTitle()
    {
        return I18n.format("pokewatch.title.start");
    }

}
