package pokecube.core.handlers;

import net.minecraft.item.ItemStack;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.RecipeSorter.Category;
import pokecube.core.Mod_Pokecube_Helper;
import pokecube.core.database.Database;
import pokecube.core.items.berries.RecipeBrewBerries;
import pokecube.core.items.megastuff.RecipeWearables;
import pokecube.core.items.pokecubes.RecipePokeseals;
import pokecube.core.items.revive.RecipeRevive;

public class RecipeHandler extends Mod_Pokecube_Helper
{
    public static void initRecipes(Object event)
    {
        Database.loadRecipes(event);
        RecipeSorter.register("pokecube:rings", RecipeWearables.class, Category.SHAPELESS, "after:minecraft:shapeless");
        RecipeSorter.register("pokecube:pokeseals", RecipePokeseals.class, Category.SHAPELESS,
                "after:minecraft:shapeless");
        RecipeSorter.register("pokecube:revive", RecipeRevive.class, Category.SHAPELESS, "after:minecraft:shaped");

        OreDictionary.registerOre("logWood", new ItemStack(ItemHandler.log0, 1, OreDictionary.WILDCARD_VALUE));
        OreDictionary.registerOre("logWood", new ItemStack(ItemHandler.log1, 1, OreDictionary.WILDCARD_VALUE));
        OreDictionary.registerOre("plankWood", new ItemStack(ItemHandler.plank0, 1, OreDictionary.WILDCARD_VALUE));
        for (int i = 0; i < 4; i++)
            GameRegistry.addShapelessRecipe(new ItemStack(ItemHandler.plank0, 4, i),
                    new ItemStack(ItemHandler.log0, 1, i));
        for (int i = 0; i < 2; i++)
            GameRegistry.addShapelessRecipe(new ItemStack(ItemHandler.plank0, 4, i + 4),
                    new ItemStack(ItemHandler.log1, 1, i));

        GameRegistry.addRecipe(new RecipeWearables());
        GameRegistry.addRecipe(new RecipePokeseals());
        GameRegistry.addRecipe(new RecipeRevive());
        BrewingRecipeRegistry.addRecipe(new RecipeBrewBerries());
    }
}
