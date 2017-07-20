package pokecube.core.interfaces.pokemob;

import net.minecraft.entity.EntityLivingBase;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;

public interface IHasEntry extends IHasMobAIStates
{
    // Returns the mob associated with this object.
    default EntityLivingBase getEntity()
    {
        return (EntityLivingBase) this;
    }

    /** @return the {@link PokedexEntry} of the species of this Pokemob */
    PokedexEntry getPokedexEntry();

    /** @return the int pokedex number */
    default Integer getPokedexNb()
    {
        return getPokedexEntry().getPokedexNb();
    }

    default boolean isShadow()
    {
        return getPokedexEntry().isShadowForme;
    }

    boolean isShiny();

    /** @param entityIn */
    default void setEntity(EntityLivingBase entityIn)
    {
        // Nope for default impl
    }

    /** @return the {@link PokedexEntry} of the species of this Pokemob */
    IPokemob setPokedexEntry(PokedexEntry newEntry);
}
