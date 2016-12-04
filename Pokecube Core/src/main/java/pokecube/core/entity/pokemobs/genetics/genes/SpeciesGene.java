package pokecube.core.entity.pokemobs.genetics.genes;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.genetics.GeneticsManager;
import pokecube.core.interfaces.IPokemob;
import thut.api.entity.genetics.Gene;

public class SpeciesGene implements Gene
{
    public static byte getSexe(int baseValue, Random random)
    {
        if (baseValue == 255) { return IPokemob.NOSEXE; }
        if (random.nextInt(255) >= baseValue) { return IPokemob.MALE; }
        return IPokemob.FEMALE;
    }

    public static class SpeciesInfo
    {
        public byte         value;
        public PokedexEntry entry;

        NBTTagCompound save()
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setByte("G", value);
            if (entry != null) tag.setString("E", entry.getName());
            return tag;
        }

        void load(NBTTagCompound tag)
        {
            value = tag.getByte("G");
            entry = Database.getEntry(tag.getString("E"));
        }

        public SpeciesInfo clone()
        {
            SpeciesInfo info = new SpeciesInfo();
            info.value = value;
            info.entry = entry;
            return info;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (!(obj instanceof SpeciesInfo)) return false;
            SpeciesInfo info = (SpeciesInfo) obj;
            return value == info.value && (entry == null ? true : entry.equals(info.entry));
        }
    }

    SpeciesInfo info = new SpeciesInfo();

    /** The value here is of format {gender, ratio}. */
    public SpeciesGene()
    {
        info.value = 0;
    }

    @Override
    public Gene interpolate(Gene other)
    {
        SpeciesGene newGene = new SpeciesGene();
        SpeciesGene otherG = (SpeciesGene) other;
        SpeciesGene mother = info.value == IPokemob.FEMALE ? this : otherG;
        newGene.setValue(mother.info.clone());
        newGene.mutate();
        return newGene;
    }

    @Override
    public Gene mutate()
    {
        SpeciesGene newGene = new SpeciesGene();
        newGene.setValue(info.clone());
        newGene.info.value = getSexe(newGene.info.entry.getSexeRatio(), new Random());
        return newGene;
    }

    @Override
    public ResourceLocation getKey()
    {
        return GeneticsManager.SPECIESGENE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValue()
    {
        return (T) info;
    }

    @Override
    public <T> void setValue(T value)
    {
        info = (SpeciesInfo) value;
    }

    @Override // This one is epigenetic, as pokedex entry can change via various
              // means, which could affect breeding.
    public boolean isEpigenetic()
    {
        return true;
    }

    @Override
    public NBTTagCompound save()
    {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("V", info.save());
        return tag;
    }

    @Override
    public void load(NBTTagCompound tag)
    {
        info.load(tag.getCompoundTag("V"));
    }

}
