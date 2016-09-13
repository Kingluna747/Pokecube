package pokecube.adventures.utils;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import pokecube.adventures.entity.trainers.TypeTrainer;
import pokecube.core.database.BiomeMatcher;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.Tools;

public class TrainerEntryLoader
{
    static XMLDatabase       database;
    static Set<TrainerEntry> entries = Sets.newHashSet();

    @XmlRootElement(name = "TYPETRAINERSET")
    public static class XMLDatabase
    {
        @XmlElement(name = "TYPETRAINER")
        private List<TrainerEntry> trainers = Lists.newArrayList();
    }

    @XmlRootElement(name = "TYPETRAINER")
    public static class TrainerEntry
    {
        @XmlAttribute
        String  tradeTemplate = "default";
        @XmlElement(name = "TYPE")
        String  type;
        @XmlElement(name = "POKEMON")
        String  pokemon;
        @XmlElement(name = "BIOMES")
        String  biomes;
        @XmlElement(name = "RATE")
        int     spawnRate;
        @XmlElement(name = "GENDER")
        String  gender;
        @XmlElement(name = "MATERIAL")
        String  material      = "air";
        @XmlElement(name = "BAG")
        Bag     bag;
        @XmlElement(name = "BELT")
        boolean belt          = true;
        @XmlElement(name = "HELD")
        Held    held;
    }

    @XmlRootElement(name = "BAG")
    public static class Bag
    {
        @XmlAnyAttribute
        Map<QName, String> values;
        @XmlElement(name = "tag")
        String             tag;
    }

    @XmlRootElement(name = "HELD")
    public static class Held
    {
        @XmlAnyAttribute
        Map<QName, String> values;
        @XmlElement(name = "tag")
        String             tag;
    }

    public static XMLDatabase loadDatabase(File file) throws Exception
    {
        JAXBContext jaxbContext = JAXBContext.newInstance(XMLDatabase.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        XMLDatabase database = (XMLDatabase) unmarshaller.unmarshal(new FileReader(file));
        return database;
    }

    public static void makeEntries(File file) throws Exception
    {
        if (database == null)
        {
            database = loadDatabase(file);
            entries.addAll(database.trainers);
        }
        for (TrainerEntry entry : entries)
        {
            String name = entry.type;
            TypeTrainer type = TypeTrainer.typeMap.get(name);
            if (type == null) type = new TypeTrainer(name);
            byte male = 1;
            byte female = 2;
            type.tradeTemplate = entry.tradeTemplate;
            type.hasBag = entry.bag != null;
            if (type.hasBag)
            {
                if (entry.bag.tag != null) entry.bag.values.put(new QName("tag"), entry.bag.tag);
                ItemStack bag = Tools.getStack(entry.bag.values);
                type.bag = bag;
            }
            type.hasBelt = entry.belt;
            type.weight = entry.spawnRate;
            type.genders = (byte) (entry.gender.equalsIgnoreCase("male") ? male
                    : entry.gender.equalsIgnoreCase("female") ? female : male + female);

            String[] pokeList = entry.pokemon == null ? new String[] {} : entry.pokemon.split(",");
            if (!entry.material.equalsIgnoreCase("air"))
            {
                if (entry.material.equalsIgnoreCase("water"))
                {
                    type.material = Material.WATER;
                }
            }
            if (entry.held != null)
            {
                if (entry.held.tag != null) entry.held.values.put(new QName("tag"), entry.held.tag);
                ItemStack held = Tools.getStack(entry.held.values);
                type.held = held;
            }
            if (entry.biomes != null) type.matcher = new BiomeMatcher(entry.biomes);
            if (pokeList.length == 0) continue;
            if (!pokeList[0].startsWith("-"))
            {
                for (String s : pokeList)
                {
                    PokedexEntry e = Database.getEntry(s);
                    if (s != null && !type.pokemon.contains(e) && e != null)
                    {
                        type.pokemon.add(e);
                    }
                    else if (e == null)
                    {
                        // System.err.println("Error in reading of "+s);
                    }
                }
            }
            else
            {
                String[] types = pokeList[0].replace("-", "").split(":");
                if (types[0].equalsIgnoreCase("all"))
                {
                    for (PokedexEntry s : Database.spawnables)
                    {
                        if (!s.legendary && s.getPokedexNb() != 151 && s != null)
                        {
                            type.pokemon.add(s);
                        }
                    }
                }
                else
                {
                    for (int i = 0; i < types.length; i++)
                    {
                        PokeType pokeType = PokeType.getType(types[i]);
                        if (pokeType != PokeType.unknown)
                        {
                            for (PokedexEntry s : Database.spawnables)
                            {
                                if (s.isType(pokeType) && !s.legendary && s.getPokedexNb() != 151 && s != null)
                                {
                                    type.pokemon.add(s);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
