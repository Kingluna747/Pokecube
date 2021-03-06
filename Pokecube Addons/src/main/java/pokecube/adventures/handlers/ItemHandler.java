package pokecube.adventures.handlers;

import static pokecube.core.PokecubeItems.addSpecificItemStack;
import static pokecube.core.PokecubeItems.getItem;
import static pokecube.core.PokecubeItems.register;
import static pokecube.core.interfaces.PokecubeMod.creativeTabPokecube;

import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import pokecube.adventures.PokecubeAdv;
import pokecube.adventures.blocks.afa.ItemBlockAFA;
import pokecube.adventures.blocks.cloner.block.ItemBlockCloner;
import pokecube.adventures.comands.Config;
import pokecube.adventures.handlers.loot.Loot;
import pokecube.adventures.handlers.loot.LootHelpers;
import pokecube.adventures.items.ItemBadge;
import pokecube.adventures.items.ItemTarget;
import pokecube.adventures.items.ItemTrainer;
import pokecube.adventures.items.bags.ItemBag;
import pokecube.core.PokecubeItems;
import pokecube.core.interfaces.IMoveNames;
import pokecube.core.items.ItemTM;

public class ItemHandler
{
    public static Item badges = new ItemBadge().setRegistryName(PokecubeAdv.ID, "badge");

    public static void addBadges(Object registry)
    {
        PokecubeItems.register(badges, registry);
        for (String s : ItemBadge.variants)
        {
            ItemStack stack = new ItemStack(badges);
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setString("type", s);
            PokecubeItems.addSpecificItemStack(s, stack);
            PokecubeItems.addToHoldables(s);
        }
    }

    public static void registerItems(Object registry)
    {
        Item expshare = (new Item()).setUnlocalizedName("exp_share").setRegistryName(PokecubeAdv.ID, "exp_share");
        expshare.setHasSubtypes(true);
        expshare.setCreativeTab(creativeTabPokecube);
        register(expshare, registry);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            PokecubeItems.registerItemTexture(expshare, 0,
                    new ModelResourceLocation("pokecube_adventures:exp_share", "inventory"));
        }

        PokecubeItems.addToHoldables("exp_share");

        Item mewHair = (new Item()).setUnlocalizedName("silkyhair").setRegistryName(PokecubeAdv.ID, "mewhair");
        register(mewHair, registry);
        PokecubeItems.addGeneric("mewhair", mewHair);

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            PokecubeItems.registerItemTexture(mewHair, 0,
                    new ModelResourceLocation("pokecube_adventures:mewhair", "inventory"));
        }

        Item target = new ItemTarget().setUnlocalizedName("pokemobTarget")
                .setRegistryName(PokecubeAdv.ID, "pokemobTarget").setCreativeTab(creativeTabPokecube);
        register(target, registry);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ModelBakery.registerItemVariants(target, new ResourceLocation("pokecube_adventures:spawner"));
            PokecubeItems.registerItemTexture(target, 0,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(target, 1,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(target, 2,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(target, 3,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
        }
        Item trainer = new ItemTrainer().setUnlocalizedName("trainerspawner")
                .setRegistryName(PokecubeAdv.ID, "trainerspawner").setCreativeTab(creativeTabPokecube);
        register(trainer, registry);
        addSpecificItemStack("traderSpawner", new ItemStack(trainer, 1, 2));
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ModelBakery.registerItemVariants(trainer, new ResourceLocation("pokecube_adventures:spawner"));
            PokecubeItems.registerItemTexture(trainer, 0,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(trainer, 1,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(trainer, 2,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
            PokecubeItems.registerItemTexture(trainer, 3,
                    new ModelResourceLocation("pokecube_adventures:spawner", "inventory"));
        }
        Item bag = new ItemBag().setUnlocalizedName("pokecubebag").setRegistryName(PokecubeAdv.ID, "pokecubebag")
                .setCreativeTab(creativeTabPokecube);
        register(bag, registry);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
        {
            ModelBakery.registerItemVariants(bag, new ResourceLocation("pokecube_adventures:bag"));
            PokecubeItems.registerItemTexture(bag, 0,
                    new ModelResourceLocation("pokecube_adventures:bag", "inventory"));
        }
        addSpecificItemStack("warplinker", new ItemStack(target, 1, 1));
        addBadges(registry);

        ItemBlock item = new ItemBlockCloner(BlockHandler.cloner);
        item.setRegistryName(BlockHandler.cloner.getRegistryName());
        register(item, registry);

        item = new ItemBlockAFA(BlockHandler.afa);
        item.setRegistryName(BlockHandler.afa.getRegistryName());
        register(item, registry);

        item = new ItemBlock(BlockHandler.siphon);
        item.setRegistryName(BlockHandler.siphon.getRegistryName());
        register(item, registry);

        item = new ItemBlock(BlockHandler.warppad);
        item.setRegistryName(BlockHandler.warppad.getRegistryName());
        register(item, registry);

    }

    public static void handleLoot()
    {
        ItemStack share = PokecubeItems.getStack("exp_share");
        if (Config.instance.exp_shareLoot) LootHelpers.addLootEntry(LootTableList.CHESTS_SIMPLE_DUNGEON, null,
                Loot.getEntryItem(share, 10, 1, "pokecube_adventures:exp_share"));
        if (Config.instance.HMLoot)
        {
            ItemStack cut = new ItemStack(getItem("tm"));
            ItemTM.addMoveToStack(IMoveNames.MOVE_CUT, cut);
            ItemStack flash = new ItemStack(getItem("tm"));
            ItemTM.addMoveToStack(IMoveNames.MOVE_FLASH, flash);
            ItemStack dig = new ItemStack(getItem("tm"));
            ItemTM.addMoveToStack(IMoveNames.MOVE_DIG, dig);
            ItemStack rocksmash = new ItemStack(getItem("tm"));
            ItemTM.addMoveToStack(IMoveNames.MOVE_ROCKSMASH, rocksmash);

            LootHelpers.addLootEntry(LootTableList.CHESTS_JUNGLE_TEMPLE, null,
                    Loot.getEntryItem(cut, 10, 1, "pokecube_adventures:cut"));
            LootHelpers.addLootEntry(LootTableList.CHESTS_ABANDONED_MINESHAFT, null,
                    Loot.getEntryItem(dig, 10, 1, "pokecube_adventures:dig"));
            LootHelpers.addLootEntry(LootTableList.CHESTS_ABANDONED_MINESHAFT, null,
                    Loot.getEntryItem(rocksmash, 10, 1, "pokecube_adventures:rocksmash"));
            LootHelpers.addLootEntry(LootTableList.CHESTS_SIMPLE_DUNGEON, null,
                    Loot.getEntryItem(flash, 10, 1, "pokecube_adventures:flash"));
        }

    }
}
