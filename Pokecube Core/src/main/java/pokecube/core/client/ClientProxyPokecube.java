/**
 *
 */
package pokecube.core.client;

import static pokecube.core.PokecubeItems.registerItemTexture;
import static pokecube.core.handlers.ItemHandler.log0;
import static pokecube.core.handlers.ItemHandler.log1;
import static pokecube.core.handlers.ItemHandler.plank0;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Locale;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.properties.IProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pokecube.core.CommonProxyPokecube;
import pokecube.core.PokecubeItems;
import pokecube.core.blocks.berries.BlockBerryLog;
import pokecube.core.blocks.berries.BlockBerryWood;
import pokecube.core.blocks.healtable.TileHealTable;
import pokecube.core.blocks.pc.ContainerPC;
import pokecube.core.blocks.pc.TileEntityPC;
import pokecube.core.blocks.pokecubeTable.TileEntityPokecubeTable;
import pokecube.core.blocks.tradingTable.BlockTradingTable;
import pokecube.core.blocks.tradingTable.ContainerTMCreator;
import pokecube.core.blocks.tradingTable.TileEntityTradingTable;
import pokecube.core.client.gui.GuiChooseFirstPokemob;
import pokecube.core.client.gui.GuiDisplayPokecubeInfo;
import pokecube.core.client.gui.GuiInfoMessages;
import pokecube.core.client.gui.GuiPokedex;
import pokecube.core.client.gui.GuiPokemob;
import pokecube.core.client.gui.GuiTeleport;
import pokecube.core.client.gui.blocks.GuiHealTable;
import pokecube.core.client.gui.blocks.GuiPC;
import pokecube.core.client.gui.blocks.GuiTMCreator;
import pokecube.core.client.gui.blocks.GuiTradingTable;
import pokecube.core.client.items.BerryTextureHandler;
import pokecube.core.client.items.FossilTextureHandler;
import pokecube.core.client.items.HeldItemTextureHandler;
import pokecube.core.client.items.MegaStoneTextureHandler;
import pokecube.core.client.items.VitaminTextureHandler;
import pokecube.core.client.items.WearableTextureHandler;
import pokecube.core.client.models.ModelPokemobEgg;
import pokecube.core.client.render.RenderMoves;
import pokecube.core.client.render.blocks.RenderPokecubeTable;
import pokecube.core.client.render.blocks.RenderTradingTable;
import pokecube.core.client.render.entity.RenderPokecube;
import pokecube.core.client.render.entity.RenderPokemobs;
import pokecube.core.client.render.entity.RenderProfessor;
import pokecube.core.client.render.particle.IParticle;
import pokecube.core.client.render.particle.ParticleFactory;
import pokecube.core.client.render.particle.ParticleHandler;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.EntityPokemob;
import pokecube.core.entity.professor.EntityProfessor;
import pokecube.core.events.handlers.EventsHandlerClient;
import pokecube.core.handlers.Config;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.berries.BerryManager;
import pokecube.core.items.pokecubes.EntityPokecube;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import pokecube.core.items.pokemobeggs.ItemPokemobEgg;
import pokecube.core.moves.animations.EntityMoveUse;
import pokecube.core.utils.Tools;
import thut.api.maths.Vector3;

@SideOnly(Side.CLIENT)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ClientProxyPokecube extends CommonProxyPokecube
{
    private static BitSet            models      = new BitSet();
    static boolean                   init        = true;
    static boolean                   first       = true;

    public static KeyBinding         nextMob;

    public static KeyBinding         nextMove;

    public static KeyBinding         previousMob;

    public static KeyBinding         previousMove;

    public static KeyBinding         mobBack;

    public static KeyBinding         mobAttack;

    public static KeyBinding         mobStance;

    public static KeyBinding         mobMegavolve;

    public static KeyBinding         noEvolve;

    public static KeyBinding         mobMove1;

    public static KeyBinding         mobMove2;

    public static KeyBinding         mobMove3;

    public static KeyBinding         mobMove4;

    public static KeyBinding         mobUp;
    public static KeyBinding         mobDown;
    private HashMap<Integer, Object> cubeRenders = new HashMap<Integer, Object>();

    public ClientProxyPokecube()
    {
        if (first) instance = this;
        first = false;
        EventsHandlerClient hndlr = new EventsHandlerClient();
        MinecraftForge.EVENT_BUS.register(hndlr);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public Object getClientGuiElement(int guiID, EntityPlayer player, World world, int x, int y, int z)
    {
        BlockPos pos = new BlockPos(x, y, z);

        if (guiID == Config.GUIPOKECENTER_ID) { return new GuiHealTable(player.inventory); }
        if (guiID == Config.GUIDISPLAYPOKECUBEINFO_ID) { return null; }
        if (guiID == Config.GUIDISPLAYTELEPORTINFO_ID) { return null; }
        if (guiID == Config.GUIPOKEDEX_ID)
        {
            Entity entityHit = Tools.getPointedEntity(player, 16);
            if (entityHit instanceof IPokemob) return new GuiPokedex((IPokemob) entityHit, player);
            return new GuiPokedex(null, player);
        }
        if (guiID == Config.GUIPOKEMOB_ID)
        {
            IPokemob e = (IPokemob) PokecubeMod.core.getEntityProvider().getEntity(world, x, true);
            return new GuiPokemob(player.inventory, e);
        }
        if (guiID == Config.GUITRADINGTABLE_ID)
        {
            TileEntityTradingTable tile = (TileEntityTradingTable) world.getTileEntity(pos);
            boolean tmc = world.getBlockState(pos).getValue(BlockTradingTable.TMC);
            if (!tmc) return new GuiTradingTable(player.inventory, tile);
            return new GuiTMCreator(new ContainerTMCreator(tile, player.inventory));
        }
        if (guiID == Config.GUIPC_ID)
        {
            TileEntityPC tile = (TileEntityPC) world.getTileEntity(pos);
            ContainerPC pc = new ContainerPC(player.inventory, tile);
            return new GuiPC(pc);
        }

        if (guiID == Config.GUICHOOSEFIRSTPOKEMOB_ID) { return new GuiChooseFirstPokemob(null); }
        return null;
    }

    @Override
    public String getFolderName()
    {
        if (FMLClientHandler.instance().getClient().theWorld != null)
            return FMLClientHandler.instance().getClient().theWorld.provider.getSaveFolder();
        return "";
    }

    @Override
    public IThreadListener getMainThreadListener()
    {
        if (isOnClientSide()) { return Minecraft.getMinecraft(); }
        return super.getMainThreadListener();
    }

    @Override
    public ISnooperInfo getMinecraftInstance()
    {
        if (isOnClientSide()) { return Minecraft.getMinecraft(); }
        return super.getMinecraftInstance();
    }

    @Override
    public EntityPlayer getPlayer(String playerName)
    {
        if (playerName != null) { return super.getPlayer(playerName); }
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public World getWorld()
    {
        if (FMLCommonHandler.instance()
                .getEffectiveSide() == Side.CLIENT) { return FMLClientHandler.instance().getWorldClient(); }
        return super.getWorld();
    }

    @Override
    public void initClient()
    {
        super.initClient();

        ResourceLocation pokecenterloop = new ResourceLocation("pokecube:sounds/pokecenterloop.ogg");
        try
        {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(pokecenterloop);
            res.close();
        }
        catch (Exception e)
        {
            TileHealTable.noSound = true;
        }
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new IItemColor()
        {
            @Override
            public int getColorFromItemstack(ItemStack stack, int tintIndex)
            {
                int damage = ItemPokemobEgg.getNumber(stack);
                PokedexEntry entry = Database.getEntry(damage);
                if (entry != null)
                {
                    int colour = entry.getType1().colour;
                    if (tintIndex == 0) { return colour; }
                    colour = entry.getType2().colour;
                    return colour;
                }
                return 0xffff00;

            }
        }, PokecubeItems.pokemobEgg);
    }

    @Override
    public boolean isOnClientSide()
    {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
    }

    @Override
    public boolean isSoundPlaying(Vector3 location)
    {
        try
        {
            BlockPos num = new BlockPos(location.intX(), location.intY(), location.intZ());
            Object sound = Minecraft.getMinecraft().renderGlobal.mapSoundPositions.get(num);
            return sound != null && Minecraft.getMinecraft().getSoundHandler().isSoundPlaying((ISound) sound);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void preInit(FMLPreInitializationEvent evt)
    {
        super.preInit(evt);

        RenderingRegistry.registerEntityRenderingHandler(EntityProfessor.class, new IRenderFactory<EntityLiving>()
        {
            @Override
            public Render<? super EntityLiving> createRenderFor(RenderManager manager)
            {
                return new RenderProfessor<>(manager);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityPokecube.class, new IRenderFactory<EntityLiving>()
        {
            @Override
            public Render<? super EntityLiving> createRenderFor(RenderManager manager)
            {
                return new RenderPokecube<>(manager);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityPokemob.class, new IRenderFactory<EntityLivingBase>()
        {
            @Override
            public Render<? super EntityLivingBase> createRenderFor(RenderManager manager)
            {
                return RenderPokemobs.getInstance(manager);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityPokemobEgg.class, new IRenderFactory<Entity>()
        {
            @Override
            public Render<? super Entity> createRenderFor(RenderManager manager)
            {
                return new RenderLiving(manager, new ModelPokemobEgg(), 0.25f)
                {
                    @Override
                    protected ResourceLocation getEntityTexture(Entity egg)
                    {
                        return new ResourceLocation(PokecubeMod.ID + ":textures/egg.png");
                    }
                };
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityMoveUse.class, new IRenderFactory<EntityMoveUse>()
        {
            @Override
            public Render<? super EntityMoveUse> createRenderFor(RenderManager manager)
            {
                return new RenderMoves(manager);
            }
        });

        Item tm = PokecubeItems.getItem("tm");

        for (int i = 0; i < 19; i++)
        {
            ModelBakery.registerItemVariants(tm, new ResourceLocation("pokecube:tm" + i));
            PokecubeItems.registerItemTexture(tm, i, new ModelResourceLocation("pokecube:tm" + i, "inventory"));
        }
        ModelBakery.registerItemVariants(tm, new ResourceLocation("pokecube:rarecandy"));
        PokecubeItems.registerItemTexture(tm, 20, new ModelResourceLocation("pokecube:rarecandy", "inventory"));

        ModelBakery.registerItemVariants(tm, new ResourceLocation("pokecube:emerald_shard"));
        PokecubeItems.registerItemTexture(tm, 19, new ModelResourceLocation("pokecube:emerald_shard", "inventory"));

        OBJLoader.INSTANCE.addDomain(PokecubeMod.ID.toLowerCase(java.util.Locale.ENGLISH));

        Item item2 = Item.getItemFromBlock(PokecubeItems.tableBlock);
        ModelLoader.setCustomModelResourceLocation(item2, 0,
                new ModelResourceLocation(PokecubeMod.ID + ":pokecube_table", "inventory"));

        item2 = Item.getItemFromBlock(PokecubeItems.getBlock("pc"));
        ModelLoader.setCustomModelResourceLocation(item2, 0,
                new ModelResourceLocation(PokecubeMod.ID + ":pc_base", "inventory"));
        ModelLoader.setCustomModelResourceLocation(item2, 8,
                new ModelResourceLocation(PokecubeMod.ID + ":pc_top", "inventory"));

        item2 = Item.getItemFromBlock(PokecubeItems.getBlock("tradingtable"));
        ModelLoader.setCustomModelResourceLocation(item2, 0,
                new ModelResourceLocation(PokecubeMod.ID + ":tradingtable", "inventory"));
        ModelLoader.setCustomModelResourceLocation(item2, 8,
                new ModelResourceLocation(PokecubeMod.ID + ":tmc", "inventory"));

        ModelLoader.setCustomStateMapper(log0,
                (new StateMap.Builder()).withName(BlockBerryLog.VARIANT0).withSuffix("wood").build());
        ModelLoader.setCustomStateMapper(log1,
                (new StateMap.Builder()).withName(BlockBerryLog.VARIANT4).withSuffix("wood").build());

        ModelLoader.setCustomStateMapper(plank0,
                (new StateMap.Builder()).withName(BlockBerryWood.VARIANT).withSuffix("plank").build());

        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:pechaplank"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:oranplank"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:leppaplank"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:sitrusplank"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:enigmaplank"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(plank0), new ResourceLocation("pokecube:nanabplank"));
        registerItemTexture(Item.getItemFromBlock(plank0), 0,
                new ModelResourceLocation("pokecube:pechaplank", "inventory"));
        registerItemTexture(Item.getItemFromBlock(plank0), 1,
                new ModelResourceLocation("pokecube:oranplank", "inventory"));
        registerItemTexture(Item.getItemFromBlock(plank0), 2,
                new ModelResourceLocation("pokecube:leppaplank", "inventory"));
        registerItemTexture(Item.getItemFromBlock(plank0), 3,
                new ModelResourceLocation("pokecube:sitrusplank", "inventory"));
        registerItemTexture(Item.getItemFromBlock(plank0), 4,
                new ModelResourceLocation("pokecube:enigmaplank", "inventory"));
        registerItemTexture(Item.getItemFromBlock(plank0), 5,
                new ModelResourceLocation("pokecube:nanabplank", "inventory"));

        ModelBakery.registerItemVariants(Item.getItemFromBlock(log0), new ResourceLocation("pokecube:pechawood"));
        registerItemTexture(Item.getItemFromBlock(log0), 0,
                new ModelResourceLocation("pokecube:pechawood", "inventory"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(log0), new ResourceLocation("pokecube:oranwood"));
        registerItemTexture(Item.getItemFromBlock(log0), 1,
                new ModelResourceLocation("pokecube:oranwood", "inventory"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(log0), new ResourceLocation("pokecube:leppawood"));
        registerItemTexture(Item.getItemFromBlock(log0), 2,
                new ModelResourceLocation("pokecube:leppawood", "inventory"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(log0), new ResourceLocation("pokecube:sitruswood"));
        registerItemTexture(Item.getItemFromBlock(log0), 3,
                new ModelResourceLocation("pokecube:sitruswood", "inventory"));

        ModelBakery.registerItemVariants(Item.getItemFromBlock(log1), new ResourceLocation("pokecube:enigmawood"));
        registerItemTexture(Item.getItemFromBlock(log1), 0,
                new ModelResourceLocation("pokecube:enigmawood", "inventory"));
        ModelBakery.registerItemVariants(Item.getItemFromBlock(log1), new ResourceLocation("pokecube:nanabwood"));
        registerItemTexture(Item.getItemFromBlock(log1), 1,
                new ModelResourceLocation("pokecube:nanabwood", "inventory"));

        Block crop = BerryManager.berryCrop;
        StateMap map = (new StateMap.Builder()).withName(BerryManager.type).ignore(new IProperty[] { BlockCrops.AGE })
                .withSuffix("crop").build();
        ModelLoader.setCustomStateMapper(crop, map);

        map = (new StateMap.Builder()).withName(BerryManager.type).withSuffix("fruit").build();
        ModelLoader.setCustomStateMapper(BerryManager.berryFruit, map);

        map = (new StateMap.Builder())
                .ignore(new IProperty[] { BerryManager.type, BlockLeaves.CHECK_DECAY, BlockLeaves.DECAYABLE }).build();
        ModelLoader.setCustomStateMapper(BerryManager.berryLeaf, map);

        MegaStoneTextureHandler.registerItemModels();
        BerryTextureHandler.registerItemModels();
        VitaminTextureHandler.registerItemModels();
        FossilTextureHandler.registerItemModels();
        HeldItemTextureHandler.registerItemModels();
        WearableTextureHandler.registerItemModels();
    }

    @Override
    public void registerKeyBindings()
    {
        ClientRegistry.registerKeyBinding(nextMob = new KeyBinding("Next Pokemob", Keyboard.KEY_RIGHT, "Pokecube"));
        ClientRegistry
                .registerKeyBinding(previousMob = new KeyBinding("Previous Pokemob", Keyboard.KEY_LEFT, "Pokecube"));
        ClientRegistry.registerKeyBinding(nextMove = new KeyBinding("Next Move", Keyboard.KEY_DOWN, "Pokecube"));
        ClientRegistry.registerKeyBinding(previousMove = new KeyBinding("Previous Move", Keyboard.KEY_UP, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobBack = new KeyBinding("Pokemob Back", Keyboard.KEY_R, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobAttack = new KeyBinding("Pokemob Attack", Keyboard.KEY_G, "Pokecube"));
        ClientRegistry
                .registerKeyBinding(mobStance = new KeyBinding("Pokemob Stance", Keyboard.KEY_BACKSLASH, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobMegavolve = new KeyBinding("Mega Evolve", Keyboard.KEY_M, "Pokecube"));
        ClientRegistry.registerKeyBinding(noEvolve = new KeyBinding("Stop Evolution", Keyboard.KEY_B, "Pokecube"));

        ClientRegistry.registerKeyBinding(mobMove1 = new KeyBinding("Move 1", Keyboard.KEY_Y, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobMove2 = new KeyBinding("Move 2", Keyboard.KEY_U, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobMove3 = new KeyBinding("Move 3", Keyboard.KEY_H, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobMove4 = new KeyBinding("Move 4", Keyboard.KEY_J, "Pokecube"));

        ClientRegistry.registerKeyBinding(mobUp = new KeyBinding("Pokemob Up", Keyboard.KEY_NONE, "Pokecube"));
        ClientRegistry.registerKeyBinding(mobDown = new KeyBinding("Pokemob Down", Keyboard.KEY_NONE, "Pokecube"));
    }

    @Override
    public void registerPokecubeRenderer(int cubeId, Render renderer, Object mod)
    {
        if (!RenderPokecube.pokecubeRenderers.containsKey(cubeId))
        {
            RenderPokecube.pokecubeRenderers.put(cubeId, renderer);
            cubeRenders.put(cubeId, mod);
        }
        else
        {
            Mod annotation = mod.getClass().getAnnotation(Mod.class);
            if (annotation.modid().equals(PokecubeMod.defaultMod))
            {
                RenderPokecube.pokecubeRenderers.put(cubeId, renderer);
                cubeRenders.put(cubeId, mod);
            }
        }
    }

    /** Used to register a model for the pokemob
     * 
     * @param nb
     *            - the pokedex number
     * @param model
     *            - the model */
    @Override
    public void registerPokemobModel(int nb, ModelBase model, Object mod)
    {
        registerPokemobModel(Database.getEntry(nb).getName(), model, mod);
    }

    @Override
    public void registerPokemobModel(String name, ModelBase model, Object mod)
    {
        if (Database.getEntry(name) == null)
        {
            Mod annotation = mod.getClass().getAnnotation(Mod.class);
            RenderPokemobs.addModel(name.toLowerCase(Locale.ENGLISH) + annotation.modid(), model);
        }
        else
        {
            Mod annotation = mod.getClass().getAnnotation(Mod.class);
            RenderPokemobs.addModel(Database.getEntry(name).getName().toLowerCase(Locale.ENGLISH) + annotation.modid(),
                    model);
            int number = Database.getEntry(name).getPokedexNb();
            if (models.get(number))
            {
                String modid = annotation.modid();
                if (!modid.equals(PokecubeMod.defaultMod)) return;
            }
            models.set(number);
        }
    }

    /** Used to register a custom renderer for the pokemob
     * 
     * @param nb
     *            - the pokedex number
     * @param renderer
     *            - the renderer */
    @Override
    public void registerPokemobRenderer(int nb, Render renderer, Object mod)
    {
        Mod annotation = mod.getClass().getAnnotation(Mod.class);
        RenderPokemobs.addCustomRenderer(Database.getEntry(nb).getTrimmedName() + annotation.modid(), renderer);
    }

    @Override
    public void registerPokemobRenderer(String name, Render renderer, Object mod)
    {
        if (Database.getEntry(name) == null)
        {
            Mod annotation = mod.getClass().getAnnotation(Mod.class);
            RenderPokemobs.addCustomRenderer(name + annotation.modid(), renderer);
            Thread.dumpStack();
        }
        else
        {
            Mod annotation = mod.getClass().getAnnotation(Mod.class);
            RenderPokemobs.addCustomRenderer(Database.getEntry(name).getTrimmedName() + annotation.modid(), renderer);
        }
    }

    @Override
    public void registerRenderInformation()
    {
        super.registerRenderInformation();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityPokecubeTable.class, new RenderPokecubeTable());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityTradingTable.class, new RenderTradingTable());

        MinecraftForge.EVENT_BUS.register(new GuiDisplayPokecubeInfo());
        GuiTeleport.create();
        new GuiInfoMessages();
    }

    @Override
    public void spawnParticle(String par1Str, Vector3 location, Vector3 velocity)
    {
        if (velocity == null) velocity = Vector3.empty;
        String[] args = par1Str.split(",");
        if (args.length == 4)
        {

        }
        else if (args.length == 2)
        {
            float offset = Float.parseFloat(args[1]);
            location.y += offset;
        }
        if (par1Str.toLowerCase(java.util.Locale.ENGLISH).contains("smoke"))
        {
            if (par1Str.contains("large"))
            {
                Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SMOKE_LARGE, location.x, location.y,
                        location.z, 0, 0, 0, 0);
                return;
            }
            Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, location.x, location.y,
                    location.z, 0, 0, 0, 0);
            return;
        }
        if (par1Str.contains("flame"))
        {
            Minecraft.getMinecraft().theWorld.spawnParticle(EnumParticleTypes.FLAME, location.x, location.y, location.z,
                    0, 0, 0, 0);
            return;
        }
        IParticle particle = ParticleFactory.makeParticle(par1Str, velocity);
        ParticleHandler.Instance().addParticle(location, particle);
    }
}
