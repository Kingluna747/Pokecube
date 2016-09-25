package pokecube.core.events.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.ai.properties.GuardAICapability;
import pokecube.core.ai.properties.IGuardAICapability;
import pokecube.core.ai.thread.PokemobAIThread;
import pokecube.core.blocks.TileEntityOwnable;
import pokecube.core.blocks.nests.TileEntityBasePortal;
import pokecube.core.database.Database;
import pokecube.core.database.Pokedex;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.helper.EntityPokemobBase;
import pokecube.core.entity.professor.EntityProfessor;
import pokecube.core.events.EvolveEvent;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.berries.BerryManager;
import pokecube.core.items.pokecubes.EntityPokecube;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.moves.PokemobTerrainEffects;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.packets.PacketChoose;
import pokecube.core.network.packets.PacketDataSync;
import pokecube.core.utils.PokeType;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.Tools;
import thut.api.boom.ExplosionCustom;
import thut.api.entity.IMobColourable;
import thut.api.maths.Vector3;
import thut.api.terrain.BiomeType;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;

public class EventsHandler
{
    public static class MeteorAreaSetter
    {
        Map<Integer, List<BlockPos>> toProcess = Maps.newHashMap();

        public MeteorAreaSetter()
        {
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void TickEvent(WorldTickEvent evt)
        {
            if (evt.phase == Phase.END && evt.side != Side.CLIENT)
            {
                List<BlockPos> thisTick = toProcess.get(evt.world.provider.getDimension());
                if (thisTick == null || thisTick.isEmpty()) return;
                int i = 0;
                for (i = 0; i < Math.min(1000, thisTick.size()); i++)
                {
                    BlockPos pos = thisTick.get(i);
                    TerrainManager.getInstance().getTerrain(evt.world, pos).setBiome(pos, BiomeType.METEOR.getType());
                }
                for (i = 0; i < Math.min(1000, thisTick.size()); i++)
                    thisTick.remove(i);
            }
        }

        public void addBlocks(Collection<BlockPos> toAdd, int dimension)
        {
            List<BlockPos> blocks = toProcess.get(dimension);
            if (blocks == null) toProcess.put(dimension, blocks = Lists.newArrayList());
            blocks.addAll(toAdd);
        }

        public void clear()
        {
            toProcess.clear();
        }
    }

    public static class ChooseFirst
    {
        final EntityPlayer player;

        public ChooseFirst(EntityPlayer player)
        {
            this.player = player;
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onPlayerJoin(TickEvent.PlayerTickEvent event)
        {
            if (event.player == player && player.ticksExisted > 0)
            {
                PacketChoose packet;
                packet = new PacketChoose(PacketChoose.OPENGUI);
                boolean hasStarter = PokecubeSerializer.getInstance().hasStarter(player);
                if (hasStarter)
                {
                    packet.data.setBoolean("C", false);
                    packet.data.setBoolean("H", hasStarter);
                }
                else
                {
                    boolean special = false;
                    if (PokecubePacketHandler.specialStarters.containsKey(player.getCachedUniqueIdString())
                            || PokecubePacketHandler.specialStarters
                                    .containsKey(player.getName().toLowerCase(java.util.Locale.ENGLISH)))
                    {
                        special = true;
                    }
                    packet = PacketChoose.createOpenPacket(!special, special, PokecubeMod.core.getStarters());
                }
                PokecubePacketHandler.sendToClient(packet, event.player);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    @CapabilityInject(IGuardAICapability.class)
    public static final Capability<IGuardAICapability> GUARDAI_CAP = null;

    public static IGuardAICapability.Storage           storage;
    static double                                      max         = 0;
    static int                                         count       = 0;
    static int                                         countAbove  = 0;
    static double                                      mean        = 0;

    static long                                        starttime   = 0;

    static boolean                                     notified    = false;

    // 4 = 1 per 10mins, 2 = 1 per 10s, 5 = 1 per 48 hours
    public static double                               candyChance = 4.5;

    public static double                               juiceChance = 3.5;

    public static List<IPokemob> getPokemobs(EntityLivingBase owner, double distance)
    {
        List<IPokemob> ret = new ArrayList<IPokemob>();

        AxisAlignedBB box = new AxisAlignedBB(owner.posX, owner.posY, owner.posZ, owner.posX, owner.posY, owner.posZ)
                .expand(distance, distance, distance);

        List<EntityLivingBase> pokemobs = owner.getEntityWorld().getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (Object o : pokemobs)
        {
            if (o instanceof IPokemob)
            {
                IPokemob mob = (IPokemob) o;
                if (mob.getPokemonOwner() == owner)
                {
                    ret.add(mob);
                }
            }
        }

        return ret;
    }

    public static int getShadowPokemonNb(Entity hostile)
    {
        String temp = hostile.getName().toLowerCase(java.util.Locale.ENGLISH).trim().replace(" ", "");

        PokedexEntry entry = null;

        ArrayList<PokedexEntry> list = Database.mobReplacements.get(temp);
        if (list != null)
        {
            Collections.shuffle(list);
            entry = list.get(0);
            while (Pokedex.getInstance().getEntry(entry.getPokedexNb()) == null && list.size() > 0)
            {
                list.remove(0);
                entry = list.get(0);
            }
            if (list.size() == 0)
            {
                Database.mobReplacements.remove(temp);
            }
        }
        return entry == null ? 249 : entry.getPokedexNb();
    }

    public static void recallAllPokemobsExcluding(EntityPlayer player, IPokemob excluded)
    {
        List<?> pokemobs = new ArrayList<Object>(player.getEntityWorld().loadedEntityList);
        for (Object o : pokemobs)
        {
            if (o instanceof IPokemob)
            {
                IPokemob mob = (IPokemob) o;
                if (mob != excluded && mob.getPokemonOwner() == player
                        && !mob.getPokemonAIState(IMoveConstants.STAYING))
                {
                    mob.returnToPokecube();
                }
            }
            else if (o instanceof EntityPokecube)
            {
                EntityPokecube mob = (EntityPokecube) o;
                if (mob.getEntityItem() != null)
                {
                    String name = PokecubeManager.getOwner(mob.getEntityItem());
                    if (name != null && (name.equalsIgnoreCase(player.getName())
                            || name.equals(player.getCachedUniqueIdString())))
                    {
                        ItemStack cube = mob.getEntityItem();
                        ItemTossEvent evt = new ItemTossEvent(
                                new EntityItem(mob.getEntityWorld(), mob.posX, mob.posY, mob.posZ, cube), player);
                        MinecraftForge.EVENT_BUS.post(evt);
                    }
                }
            }
        }
    }

    public static void setFromNBT(IPokemob pokemob, NBTTagCompound tag)
    {
        float scale = tag.getFloat("scale");
        if (scale > 0)
        {
            pokemob.setSize(scale);
        }
        pokemob.setSexe((byte) tag.getInteger(PokecubeSerializer.SEXE));
        boolean shiny = tag.getBoolean("shiny");
        pokemob.setShiny(shiny);
        byte[] rgbaBytes = new byte[4];
        // TODO remove the legacy colour support eventually.
        if (tag.hasKey("colours", 7))
        {
            rgbaBytes = tag.getByteArray("colours");
        }
        else
        {
            rgbaBytes[0] = tag.getByte("red");
            rgbaBytes[1] = tag.getByte("green");
            rgbaBytes[2] = tag.getByte("blue");
            rgbaBytes[3] = 127;
        }
        if (pokemob instanceof IMobColourable)
        {
            ((IMobColourable) pokemob).setRGBA(rgbaBytes[0] + 128, rgbaBytes[1] + 128, rgbaBytes[2] + 128,
                    rgbaBytes[2] + 128);
        }
        String forme = tag.getString("forme");
        pokemob.changeForme(forme);
        pokemob.setSpecialInfo(tag.getInteger("specialInfo"));
    }

    public MeteorAreaSetter meteorprocessor;

    public EventsHandler()
    {
        CapabilityManager.INSTANCE.register(IGuardAICapability.class, storage = new IGuardAICapability.Storage(),
                GuardAICapability.class);
        MinecraftForge.EVENT_BUS.register(new StatsHandler());
        PokemobAIThread aiTicker = new PokemobAIThread();
        MinecraftForge.EVENT_BUS.register(aiTicker);
        MinecraftForge.EVENT_BUS.register(this);
        meteorprocessor = new MeteorAreaSetter();
        new SpawnEventsHandler();
    }

    @SubscribeEvent
    public void BreakBlock(BreakEvent evt)
    {
        if (evt.getState().getBlock() == Blocks.MOB_SPAWNER)
        {
            ItemStack stack = PokecubeItems.getRandomSpawnerDrop();
            if (stack == null) return;
            EntityItem item = new EntityItem(evt.getWorld(), evt.getPos().getX() + 0.5, evt.getPos().getY() + 0.5,
                    evt.getPos().getZ() + 0.5, stack);
            evt.getWorld().spawnEntityInWorld(item);
        }
        if (evt.getState().getBlock() == PokecubeItems.pokecenter)
        {
            int meta = evt.getState().getBlock().getMetaFromState(evt.getState());
            if (meta == 1 && !evt.getPlayer().capabilities.isCreativeMode) evt.setCanceled(true);
        }
        TileEntity tile;
        if ((tile = evt.getWorld().getTileEntity(evt.getPos())) instanceof TileEntityBasePortal)
        {
            if (!((TileEntityBasePortal) tile).canEdit(evt.getPlayer()))
            {
                evt.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void clearNetherBridge(InitMapGenEvent evt)
    {
        if (PokecubeMod.core.getConfig().deactivateMonsters && evt.getType() == InitMapGenEvent.EventType.NETHER_BRIDGE)
        {
            ((MapGenNetherBridge) evt.getNewGen()).getSpawnList().clear();
        }
    }

    @SubscribeEvent
    public void EntityJoinWorld(EntityJoinWorldEvent evt)
    {
        if (PokecubeMod.core.getConfig().disableMonsters && !(evt.getEntity() instanceof IPokemob)
                && evt.getEntity() instanceof IMob
                && !(evt.getEntity() instanceof EntityDragon || evt.getEntity() instanceof EntityDragonPart))
        {
            evt.getEntity().setDead();
            // TODO maybe replace stuff here
            evt.setCanceled(true);
        }
        else if (evt.getEntity() instanceof EntityCreeper)
        {
            EntityAIAvoidEntity<EntityPokemobBase> avoidAI;
            EntityCreeper creeper = (EntityCreeper) evt.getEntity();
            avoidAI = new EntityAIAvoidEntity<EntityPokemobBase>(creeper, EntityPokemobBase.class,
                    new Predicate<EntityPokemobBase>()
                    {
                        @Override
                        public boolean apply(EntityPokemobBase input)
                        {
                            return input.isType(PokeType.psychic);
                        }
                    }, 6.0F, 1.0D, 1.2D);
            creeper.tasks.addTask(3, avoidAI);
        }
    }

    @SubscribeEvent
    public void explosionEvents(ExplosionEvent.Detonate evt)
    {
        if (evt.getExplosion() instanceof ExplosionCustom)
        {
            ExplosionCustom boom = (ExplosionCustom) evt.getExplosion();
            if (!boom.meteor) return;
            meteorprocessor.addBlocks(evt.getAffectedBlocks(), evt.getWorld().provider.getDimension());
        }
    }

    @SubscribeEvent
    public void interactEventLeftClick(PlayerInteractEvent.LeftClickBlock evt)
    {
        if (evt.getEntityPlayer().getHeldItemMainhand() != null
                && evt.getEntityPlayer().getHeldItemMainhand().getItem() == Items.STICK)
        {
            TileEntity te = evt.getWorld().getTileEntity(evt.getPos());
            if (te instanceof TileEntityOwnable)
            {
                IBlockState state = evt.getWorld().getBlockState(evt.getPos());
                TileEntityOwnable tile = (TileEntityOwnable) te;
                if (tile.canEdit(evt.getEntity()) && tile.shouldBreak())
                {
                    Block b = state.getBlock();
                    b.dropBlockAsItem(evt.getWorld(), evt.getPos(), state, 0);
                    evt.getWorld().setBlockToAir(evt.getPos());
                }
            }
        }
    }

    @SubscribeEvent
    public void interactEvent(PlayerInteractEvent.RightClickBlock evt)
    {
        if (evt.getEntityPlayer().getEntityWorld().isRemote
                || evt.getEntityPlayer().getEntityWorld().rand.nextInt(10) != 0)
            return;
        TerrainSegment t = TerrainManager.getInstance().getTerrainForEntity(evt.getEntityPlayer());
        t.checkIndustrial(evt.getEntityPlayer().getEntityWorld());
    }

    /** Applies the exp from lucky egg and exp share. TODO move this out of
     * PCEventsHandler.
     * 
     * @param evt */
    @SubscribeEvent
    public void KillEvent(pokecube.core.events.KillEvent evt)
    {
        IPokemob killer = evt.killer;
        IPokemob killed = evt.killed;

        if (killer != null && evt.giveExp)
        {
            EntityLivingBase owner = killer.getPokemonOwner();

            ItemStack stack = ((EntityLivingBase) killer).getHeldItemMainhand();
            if (stack != null && PokecubeItems.getStack("luckyegg").isItemEqual(stack))
            {
                int exp = killer.getExp() + Tools.getExp(1, killed.getBaseXP(), killed.getLevel());
                killer.setExp(exp, true);
            }
            if (owner != null)
            {
                List<IPokemob> pokemobs = PCEventsHandler.getOutMobs(owner);
                for (IPokemob mob : pokemobs)
                {
                    if (mob != null)
                    {
                        IPokemob poke = mob;
                        if (((EntityLiving) poke).getHeldItemMainhand() != null) if (((EntityLiving) poke)
                                .getHeldItemMainhand().isItemEqual(PokecubeItems.getStack("exp_share")))
                        {
                            int exp = poke.getExp() + Tools.getExp(1, killed.getBaseXP(), killed.getLevel());
                            poke.setExp(exp, true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void livingHurtEvent(LivingHurtEvent evt)
    {
        if (evt.getEntityLiving() instanceof EntityPlayer && evt.getSource() == DamageSource.inWall)
        {
            if (evt.getEntityLiving().getRidingEntity() instanceof IPokemob) evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void livingSetTargetEvent(LivingSetAttackTargetEvent evt)
    {
        if (evt.getTarget() != null && evt.getEntityLiving() instanceof EntityLiving)
        {
            List<IPokemob> pokemon = getPokemobs(evt.getTarget(), 32);
            if (pokemon.isEmpty()) return;
            double closest = 1000;
            IPokemob newtarget = null;
            for (IPokemob e : pokemon)
            {
                double dist = ((Entity) e).getDistanceSqToEntity(evt.getEntityLiving());
                if (dist < closest && !(e.getPokemonAIState(IMoveConstants.STAYING)
                        && e.getPokemonAIState(IMoveConstants.SITTING)))
                {
                    closest = dist;
                    newtarget = e;
                }
            }
            if (newtarget != null)
            {
                ((EntityLiving) evt.getEntityLiving()).setAttackTarget((EntityLivingBase) newtarget);
                if (evt.getEntityLiving() instanceof IPokemob)
                {
                    ((IPokemob) evt.getEntityLiving()).setPokemonAIState(IMoveConstants.ANGRY, true);
                    ((IPokemob) evt.getEntityLiving()).setPokemonAIState(IMoveConstants.SITTING, false);
                }
                ((EntityLiving) newtarget).setAttackTarget(evt.getEntityLiving());
                newtarget.setPokemonAIState(IMoveConstants.ANGRY, true);
            }
        }
    }

    @SubscribeEvent
    public void livingUpdate(LivingUpdateEvent evt)
    {
        if (evt.getEntity().getEntityWorld().isRemote || evt.getEntity().isDead) return;
        if (evt.getEntityLiving().getEntityWorld().getTotalWorldTime() % 40 == 0)
        {
            TerrainSegment terrain = TerrainManager.getInstance().getTerrainForEntity(evt.getEntityLiving());
            PokemobTerrainEffects effect = (PokemobTerrainEffects) terrain.geTerrainEffect("pokemobEffects");
            if (effect == null)
            {
                terrain.addEffect(effect = new PokemobTerrainEffects(), "pokemobEffects");
            }
            effect.doEffect(evt.getEntityLiving(), false);
        }
        if (evt.getEntityLiving() instanceof EntityPlayer)
        {
            SpawnHandler.refreshTerrain(Vector3.getNewVector().set(evt.getEntityLiving()),
                    evt.getEntity().getEntityWorld());
        }

        if (evt.getEntityLiving() instanceof IPokemob && ((IPokemob) evt.getEntityLiving()).getPokedexNb() == 213)
        {
            IPokemob shuckle = (IPokemob) evt.getEntityLiving();

            if (evt.getEntityLiving().getEntityWorld().isRemote) return;

            ItemStack item = evt.getEntityLiving().getHeldItemMainhand();
            if (item == null) return;
            Item itemId = item.getItem();
            boolean berry = item.isItemEqual(BerryManager.getBerryItem("oran"));
            Random r = new Random();
            if (berry && r.nextGaussian() > juiceChance)
            {
                if (shuckle.getPokemonOwner() != null)
                {
                    String message = "A sweet smell is coming from "
                            + shuckle.getPokemonDisplayName().getFormattedText();
                    ((EntityPlayer) shuckle.getPokemonOwner()).addChatMessage(new TextComponentString(message));
                }
                shuckle.setHeldItem(new ItemStack(PokecubeItems.berryJuice));
                return;
            }
            berry = itemId == PokecubeItems.berryJuice;
            if (berry && (r.nextGaussian() > candyChance))
            {
                ItemStack candy = PokecubeItems.makeCandyStack();
                if (candy == null) return;

                if (shuckle.getPokemonOwner() != null)
                {
                    String message = "The smell coming from " + shuckle.getPokemonDisplayName().getFormattedText()
                            + " has changed";
                    ((EntityPlayer) shuckle.getPokemonOwner()).addChatMessage(new TextComponentString(message));
                }
                shuckle.setHeldItem(candy);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onEntityCapabilityAttach(AttachCapabilitiesEvent.Entity event)
    {
        if (event.getEntity() instanceof IPokemob || event.getEntity() instanceof EntityProfessor)
        {
            class Provider extends GuardAICapability implements ICapabilitySerializable<NBTTagCompound>
            {
                @Override
                public void deserializeNBT(NBTTagCompound nbt)
                {
                    storage.readNBT(GUARDAI_CAP, this, null, nbt);
                }

                @SuppressWarnings("unchecked") // There isnt anything sane we
                                               // can do about this.
                @Override
                public <T> T getCapability(Capability<T> capability, EnumFacing facing)
                {
                    if (GUARDAI_CAP != null && capability == GUARDAI_CAP) return (T) this;
                    return null;
                }

                @Override
                public boolean hasCapability(Capability<?> capability, EnumFacing facing)
                {
                    return GUARDAI_CAP != null && capability == GUARDAI_CAP;
                }

                @Override
                public NBTTagCompound serializeNBT()
                {
                    return (NBTTagCompound) storage.writeNBT(GUARDAI_CAP, this, null);
                }
            }
            event.addCapability(new ResourceLocation("pokecube:GuardAI"), new Provider());
        }
    }

    @SubscribeEvent
    public void PlayerLoggin(PlayerLoggedInEvent evt)
    {
        EntityPlayer entityPlayer = evt.player;
        if (!evt.player.getEntityWorld().isRemote)
        {
            PacketDataSync.sendInitHandshake(entityPlayer);
            PacketDataSync.sendInitPacket(entityPlayer, "pokecube-data");
            PacketDataSync.sendInitPacket(entityPlayer, "pokecube-stats");
        }

        if (evt.player != null)
        {
            if (!evt.player.getEntityWorld().isRemote)
            {
                if (PokecubeMod.core.getConfig().guiOnLogin)
                {
                    new ChooseFirst(evt.player);
                }
            }
        }
    }

    @SubscribeEvent
    public void TickEvent(WorldTickEvent evt)
    {
        if (evt.phase == Phase.END && evt.side != Side.CLIENT)
        {
            PokecubeCore.instance.spawner.tick(evt.world);
        }
    }

    @SubscribeEvent
    public void travelToDimension(EntityTravelToDimensionEvent evt)
    {
        Entity entity = evt.getEntity();
        if (entity.getEntityWorld().isRemote) return;

        ArrayList<?> list = new ArrayList<Object>(entity.getEntityWorld().loadedEntityList);
        for (Object o : list)
        {
            if (o instanceof IPokemob)
            {
                IPokemob mob = (IPokemob) o;
                boolean stay = mob.getPokemonAIState(IMoveConstants.STAYING);
                if (mob.getPokemonAIState(IMoveConstants.TAMED) && (mob.getPokemonOwner() == entity) && !stay)
                    mob.returnToPokecube();
            }
        }
    }

    @SubscribeEvent
    public void evolveEvent(EvolveEvent.Pre evt)
    {
        if (evt.mob.getPokedexEntry() == Database.getEntry("Tyrogue"))
        {
            int[] stats = evt.mob.getActualStats();
            if (stats[1] > stats[2]) evt.forme = "Hitmonlee";
            else if (stats[2] > stats[1]) evt.forme = "Hitmonchan";
            else evt.forme = "Hitmontop";
        }
    }

    @SubscribeEvent
    public void worldLoadEvent(Load evt)
    {
        if (evt.getWorld().isRemote) { return; }
        PokecubeMod.getFakePlayer(evt.getWorld());
    }

    @SubscribeEvent
    public void WorldSave(WorldEvent.Save evt)
    {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER && evt.getWorld().provider.getDimension() == 0)
        {
            long time = System.nanoTime();
            PokecubeSerializer.getInstance().save();
            double dt = (System.nanoTime() - time) / 1000000d;
            if (dt > 20) System.err.println("Took " + dt + "ms to save pokecube data");
        }
    }
}
