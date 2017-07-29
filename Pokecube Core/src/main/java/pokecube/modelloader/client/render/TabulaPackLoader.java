package pokecube.modelloader.client.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IShearable;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.modelloader.IMobProvider;
import thut.api.maths.Vector3;
import thut.core.client.render.animation.AnimationBuilder;
import thut.core.client.render.animation.AnimationRandomizer;
import thut.core.client.render.animation.AnimationRegistry;
import thut.core.client.render.animation.AnimationRegistry.IPartRenamer;
import thut.core.client.render.model.IAnimationChanger;
import thut.core.client.render.model.IModelRenderer.Vector5;
import thut.core.client.render.model.IPartTexturer;
import thut.core.client.render.tabula.components.Animation;
import thut.core.client.render.tabula.components.CubeGroup;
import thut.core.client.render.tabula.components.CubeInfo;
import thut.core.client.render.tabula.components.ModelJson;
import thut.core.client.render.tabula.model.tabula.TabulaModel;
import thut.core.client.render.tabula.model.tabula.TabulaModelParser;

public class TabulaPackLoader extends AnimationLoader
{
    public static class TabulaModelSet implements IPartRenamer, IAnimationChanger
    {
        /** The pokemon associated with this model. */
        final PokedexEntry                entry;
        public final TabulaModel          model;
        public final TabulaModelParser    parser;
        public IPartTexturer              texturer         = null;
        public AnimationRandomizer        animator         = null;

        /** Animations to merge together, animation key is merged into animation
         * value. so key of idle and value of walk will merge the idle animation
         * into the walk animation. */
        private HashMap<String, String>   mergedAnimations = Maps.newHashMap();

        /** The root part of the head. */
        private Set<String>               headRoots        = Sets.newHashSet();
        private String                    headRoot         = "";
        /** A set of identifiers of shearable parts. */
        public Set<String>                shearableIdents  = Sets.newHashSet();
        /** A set of identifiers of dyeable parts. */
        public Set<String>                dyeableIdents    = Sets.newHashSet();
        /** Animations loaded from the XML */
        public HashMap<String, Animation> loadedAnimations = Maps.newHashMap();
        /** Internal, used to determine if it should copy xml data from base
         * forme. */
        private boolean                   foundExtra       = false;

        public TabulaModelSet(TabulaModel model, TabulaModelParser parser, ResourceLocation extraData,
                PokedexEntry entry)
        {
            this.model = model;
            this.parser = parser;
            this.entry = entry;
            processMetadata();
            try
            {
                parse(extraData);
                foundExtra = true;
            }
            catch (Exception e)
            {
                String name = entry.getBaseName();
                String old = entry.getName();
                ResourceLocation test2 = new ResourceLocation(extraData.getResourceDomain(), extraData.getResourcePath()
                        .replace(old.toLowerCase(Locale.ENGLISH), name.toLowerCase(Locale.ENGLISH)));
                try
                {
                    parse(test2);
                    foundExtra = true;
                }
                catch (Exception e1)
                {
                    // Not really important for tabula models, they can have
                    // animations built in.
                }
            }
        }

        public TabulaModelSet(TabulaModelSet from, ResourceLocation extraData, PokedexEntry entry)
        {
            this(from.model, from.parser, extraData, entry);
        }

        private void addAnimation(Animation animation)
        {
            String key = animation.name;
            if (loadedAnimations.containsKey(key))
            {
                AnimationBuilder.merge(animation, loadedAnimations.get(key));
            }
            else
            {
                loadedAnimations.put(key, animation);
            }
        }

        @Override
        public void convertToIdents(String[] names)
        {
            for (int i = 0; i < names.length; i++)
            {
                boolean found = false;
                for (ModelJson json : parser.modelMap.values())
                {
                    if (json.nameMap.containsKey(names[i]))
                    {
                        Object o = json.nameMap.get(names[i]);
                        for (String ident : json.identifierMap.keySet())
                        {
                            if (json.identifierMap.get(ident) == o)
                            {
                                names[i] = ident;
                                found = true;
                                break;
                            }
                        }
                        break;
                    }
                }
                if (!found) names[i] = null;
            }
        }

        @Override
        public int getColourForPart(String partIdentifier, Entity entity, int default_)
        {
            if (dyeableIdents.contains(partIdentifier))
            {
                int rgba = 0xFF000000;
                IPokemob pokemob = CapabilityPokemob.getPokemobFor(entity);
                rgba += EnumDyeColor.byDyeDamage(pokemob.getSpecialInfo()).getMapColor().colorValue;
                return rgba;
            }
            return default_;
        }

        @Override
        public float[] getHeadInfo()
        {
            return headInfo;
        }

        /** Returns true of the given identifier matches the part listed as the
         * root of the head.
         * 
         * @param identifier
         * @return */
        @Override
        public boolean isHeadRoot(String identifier)
        {
            if (!headRoots.isEmpty()) { return headRoots.contains(identifier); }
            return identifier.equals(headRoot);
        }

        @Override
        public boolean isPartHidden(String part, Entity entity, boolean default_)
        {
            if (shearableIdents.contains(part))
            {
                boolean shearable = ((IShearable) entity).isShearable(new ItemStack(Items.SHEARS),
                        entity.getEntityWorld(), entity.getPosition());
                return !shearable;
            }
            return default_;
        }

        @Override
        public String modifyAnimation(EntityLiving entity, float partialTicks, String phase)
        {
            return animator.modifyAnimation(entity, partialTicks, phase);
        }

        public void parse(ResourceLocation animation) throws Exception
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(animation);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(res.getInputStream());
            res.close();
            doc.getDocumentElement().normalize();

            NodeList modelList = doc.getElementsByTagName("model");

            Vector3 offset = null;
            Vector5 rotation = null;
            Vector3 scale = null;

            for (int i = 0; i < modelList.getLength(); i++)
            {
                Node modelNode = modelList.item(i);
                NodeList partsList = modelNode.getChildNodes();
                for (int j = 0; j < partsList.getLength(); j++)
                {
                    Node part = partsList.item(j);
                    if (part.getNodeName().equals("phase"))
                    {
                        Node phase = part.getAttributes().getNamedItem("name") == null
                                ? part.getAttributes().getNamedItem("type") : part.getAttributes().getNamedItem("name");
                        String phaseName = phase.getNodeValue();
                        // Look for preset animations to load in
                        boolean preset = false;

                        for (String s : AnimationRegistry.animations.keySet())
                        {
                            if (phaseName.equals(s))
                            {
                                addAnimation(AnimationRegistry.make(s, part.getAttributes(), this));
                                preset = true;
                            }
                        }
                        // Global offset and rotation settings (legacy support
                        // for head stuff as well)
                        if (phaseName.equals("global"))
                        {
                            try
                            {
                                offset = getOffset(part, offset);
                                scale = getScale(part, scale);
                                rotation = getRotation(part, rotation);
                                headAxis = getHeadAxis(part, headAxis);
                                headDir = getHeadDir(part, headDir);
                                headDir = Math.min(1, headDir);
                                headDir = Math.max(-1, headDir);
                                setHeadCaps(part, headCap, headCap1);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        // Texture Animation info
                        else if (phaseName.equals("textures"))
                        {
                            setTextureDetails(part, entry);
                        }
                        // Try to load in an animation.
                        else if (!preset)
                        {
                            Animation anim = AnimationBuilder.build(part, this);
                            if (anim != null)
                            {
                                addAnimation(anim);
                            }
                        }
                    }
                    // Read in Animation Merges
                    else if (part.getNodeName().equals("merges"))
                    {
                        String[] merges = part.getAttributes().getNamedItem("merge").getNodeValue().split("->");
                        mergedAnimations.put(merges[0], merges[1]);
                    }
                    else if (part.getNodeName().equals("customTex"))
                    {
                        texturer = new TextureHelper(part);
                    }
                    else if (part.getNodeName().equals("subAnims"))
                    {
                        animator = new AnimationRandomizer(part);
                    }
                    else if (part.getNodeName().equals("metadata"))
                    {
                        try
                        {
                            offset = getOffset(part, offset);
                            scale = getScale(part, scale);
                            rotation = getRotation(part, rotation);
                            headDir = getHeadDir(part, headDir);
                            headAxis = getHeadAxis(part, 1);
                            Set<String> toAdd = Sets.newHashSet();
                            addStrings("head", part, toAdd);
                            ArrayList<String> temp = new ArrayList<String>(toAdd);
                            toAdd.clear();
                            String[] names = temp.toArray(new String[0]);
                            this.convertToIdents(names);
                            for (String s : names)
                                headRoots.add(s);
                            temp.clear();
                            addStrings("shear", part, toAdd);
                            temp.addAll(toAdd);
                            toAdd.clear();
                            names = temp.toArray(new String[0]);
                            this.convertToIdents(names);
                            for (String s : names)
                                shearableIdents.add(s);
                            addStrings("dye", part, toAdd);
                            temp.addAll(toAdd);
                            names = temp.toArray(new String[0]);
                            this.convertToIdents(names);
                            for (String s : names)
                                dyeableIdents.add(s);
                            setHeadCaps(part, headCap, headCap1);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (offset != null)
            {
                this.shift.set(offset);
            }
            if (scale != null)
            {
                this.scale.set(scale);
            }
            if (rotation != null)
            {
                this.rotation = rotation;
            }
            else
            {
                this.rotation = new Vector5();
            }
        }

        private void postInitAnimations()
        {
            HashSet<String> toRemove = Sets.newHashSet();
            for (Animation anim : model.getAnimations())
            {
                for (String s : loadedAnimations.keySet())
                {
                    if (s.equals(anim.name))
                    {
                        Animation loaded = loadedAnimations.get(s);
                        AnimationBuilder.merge(loaded, anim);
                        toRemove.add(s);
                    }
                }
            }
            for (String s : mergedAnimations.keySet())
            {
                String toName = mergedAnimations.get(s);
                Animation to = null;
                Animation from = null;
                for (Animation anim : model.getAnimations())
                {
                    if (s.equals(anim.name))
                    {
                        from = anim;
                    }
                    if (toName.equals(anim.name))
                    {
                        to = anim;
                    }
                    if (to != null && from != null) break;
                }
                if (to == null || from == null) for (Animation anim : loadedAnimations.values())
                {
                    if (from == null) if (s.equals(anim.name))
                    {
                        from = anim;
                    }
                    if (to == null) if (toName.equals(anim.name))
                    {
                        to = anim;
                    }
                    if (to != null && from != null) break;
                }
                if (from != null && to == null)
                {
                    to = new Animation();
                    to.name = toName;
                    to.identifier = toName;
                    to.loops = from.loops;
                    loadedAnimations.put(toName, to);
                }

                if (to != null && from != null)
                {
                    AnimationBuilder.merge(from, to);
                }
            }
            for (String s : toRemove)
            {
                loadedAnimations.remove(s);
            }
            if (animator != null)
            {
                Set<Animation> anims = Sets.newHashSet();
                anims.addAll(model.getAnimations());
                anims.addAll(loadedAnimations.values());
                animator.init(anims);
            }
            headInfo[0] = headCap[0];
            headInfo[1] = headCap[1];
            headInfo[2] = headDir;
            headInfo[3] = headCap1[0];
            headInfo[4] = headCap1[1];
            headInfo[5] = headAxis;
        }

        private void processMetadata()
        {
            for (CubeInfo cube : model.getCubes())
            {
                processMetadataForCubeInfo(cube);
            }
            for (CubeGroup group : model.getCubeGroups())
            {
                processMetadataForCubeGroup(group);
            }
        }

        private void processMetadataForCubeGroup(CubeGroup group)
        {
            for (CubeInfo cube : group.cubes)
            {
                processMetadataForCubeInfo(cube);
            }
            for (CubeGroup group1 : group.cubeGroups)
            {
                processMetadataForCubeGroup(group1);
            }
        }

        private void processMetadataForCubeInfo(CubeInfo cube)
        {
            if (headRoot.isEmpty() && cube.name.toLowerCase(java.util.Locale.ENGLISH).contains("head")
                    && cube.parentIdentifier != null)
            {
                headRoot = cube.identifier;
            }
            for (String s : cube.metadata)
            {
                if (s.equalsIgnoreCase("shearable"))
                {
                    shearableIdents.add(cube.identifier);
                }
                if (s.equalsIgnoreCase("dyeable"))
                {
                    dyeableIdents.add(cube.identifier);
                }
                if (s.equalsIgnoreCase("head"))
                {
                    headRoots.add(cube.identifier);
                }
            }
            for (CubeInfo cube1 : cube.children)
            {
                processMetadataForCubeInfo(cube1);
            }
        }
    }

    public static HashMap<PokedexEntry, TabulaModelSet> modelMap = new HashMap<PokedexEntry, TabulaModelSet>();

    public static void clear()
    {
        AnimationLoader.clear();
        modelMap.clear();
    }

    public static void remove(PokedexEntry entry)
    {
        AnimationLoader.remove(entry);
        modelMap.remove(entry);
    }

    public static boolean loadModel(IMobProvider provider, String path, HashSet<String> toReload)
    {
        ResourceLocation model = new ResourceLocation(path + ".tbl");
        String anim = path + ".xml";
        ResourceLocation extraData = new ResourceLocation(anim);

        String[] args = path.split(":");
        String[] args2 = args[1].split("/");
        String name = args2[args2.length > 1 ? args2.length - 1 : 0];
        PokedexEntry entry = Database.getEntry(name);
        try
        {
            if (entry != null)
            {
                IResource res = Minecraft.getMinecraft().getResourceManager().getResource(model);
                ZipInputStream zip = new ZipInputStream(res.getInputStream());
                Scanner scanner = new Scanner(zip);
                zip.getNextEntry();
                String json = scanner.nextLine();
                TabulaModelParser parser = new TabulaModelParser();
                TabulaModel tbl = parser.parse(json);
                TabulaModelSet set = new TabulaModelSet(tbl, parser, extraData, entry);
                modelMap.put(entry, set);
                if (!modelMaps.containsKey(entry.getName())
                        || modelMaps.get(entry.getName()) instanceof TabulaModelRenderer)
                    AnimationLoader.modelMaps.put(entry.getName(), new TabulaModelRenderer<>(set));
                scanner.close();
                res.close();
            }
            return entry != null;
        }
        catch (IOException e)
        {
            if (entry.getBaseForme() != null)
            {
                TabulaModelSet set;
                if ((set = modelMap.get(entry.getBaseForme())) == null)
                {
                    toReload.add(path);
                }
                else
                {
                    set = new TabulaModelSet(set, extraData, entry);
                    modelMap.put(entry, set);
                    if (!modelMaps.containsKey(entry.getName())
                            || modelMaps.get(entry.getName()) instanceof TabulaModelRenderer)
                        AnimationLoader.modelMaps.put(entry.getName(), new TabulaModelRenderer<>(set));
                }
            }
        }
        return false;
    }

    public static void postProcess()
    {
        for (PokedexEntry entry : modelMap.keySet())
        {
            TabulaModelSet set = modelMap.get(entry);
            if (!set.foundExtra)
            {
                PokedexEntry base = entry.getBaseForme();
                TabulaModelSet baseSet = modelMap.get(base);
                if (baseSet != null)
                {
                    set.rotation = baseSet.rotation;
                    set.scale.set(baseSet.scale);
                    set.shift.set(baseSet.shift);
                    set.headCap = baseSet.headCap;
                    set.headCap1 = baseSet.headCap1;
                }
                else
                {
                    // Not really important for tabula models, they can have
                    // this built in.
                }
            }
            set.postInitAnimations();
            if (set.rotation == null)
            {
                set.rotation = new Vector5();
            }
            if (set.scale.isEmpty())
            {
                set.scale.set(1, 1, 1);
            }
        }
    }
}
