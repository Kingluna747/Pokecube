package pokecube.core.client.items;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import pokecube.core.PokecubeItems;
import pokecube.core.handlers.HeldItemHandler;

public class MegaStoneTextureHandler
{
    public static class MegaStone implements ItemMeshDefinition
    {
        @Override
        public ModelResourceLocation getModelLocation(ItemStack stack)
        {
            NBTTagCompound tag = stack.getTagCompound();
            String variant = "megastone";
            if (tag != null)
            {
                String stackname = tag.getString("pokemon");
                variant = stackname.toLowerCase();
            }
            return getLocation(variant);
        }
    }

    public static ModelResourceLocation getLocation(String name)
    {
        return new ModelResourceLocation(new ResourceLocation("pokecube", "item/megastone"),
                "type=" + name.toLowerCase());
    }

    private static void registerItemVariant(String variant)
    {
        ModelBakery.registerItemVariants(PokecubeItems.megastone,
                new ModelResourceLocation(new ResourceLocation("pokecube", "item/megastone"), variant));
    }

    public static void registerItemModels()
    {
        ModelLoader.setCustomMeshDefinition(PokecubeItems.megastone, new MegaStone());
        for (String s : HeldItemHandler.megaVariants)
        {
            registerItemVariant("type=" + s);
        }
    }
}
