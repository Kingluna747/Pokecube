/**
 *
 */
package pokecube.core.entity.pokemobs;

import net.minecraft.world.World;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.helper.EntityPokemobBase;

public class EntityPokemob extends EntityPokemobBase
{
    public EntityPokemob(World world)
    {
        super(world);
        if (getClass().getName().contains("GenericPokemob") && pokedexNb == 0)
        {
            String num = getClass().getSimpleName().replace("GenericPokemob", "").trim();
            PokedexEntry entry = Database.getEntry(Integer.parseInt(num));
            if (entry != null && entry.getPokedexNb() > 0) init(entry.getPokedexNb());
        }
    }

    public EntityPokemob(World world, String name)
    {
        super(world);
        PokedexEntry entry = Database.getEntry(name);
        if (entry != null && entry.getPokedexNb() > 0) init(entry.getPokedexNb());
    }
}
