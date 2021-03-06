package pokecube.core.interfaces.pokemob;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import pokecube.core.PokecubeCore;
import pokecube.core.PokecubeItems;
import pokecube.core.database.Database;
import pokecube.core.database.PokedexEntry;
import pokecube.core.database.PokedexEntry.EvolutionData;
import pokecube.core.entity.pokemobs.genetics.GeneticsManager;
import pokecube.core.events.EvolveEvent;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.HappinessType;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.interfaces.capabilities.CapabilityPokemob;
import pokecube.core.moves.MovesUtils;
import pokecube.core.network.PokecubePacketHandler;
import pokecube.core.network.pokemobs.PacketSyncNewMoves;
import pokecube.core.network.pokemobs.PokemobPacketHandler.MessageServer;
import pokecube.core.utils.TagNames;
import pokecube.core.utils.Tools;
import thut.api.entity.genetics.IMobGenetics;
import thut.api.network.PacketHandler;
import thut.core.common.commands.CommandTools;
import thut.lib.CompatWrapper;

public interface ICanEvolve extends IHasEntry, IHasOwner
{

    static class EvoTicker
    {
        final World          world;
        final Entity         evo;
        final ITextComponent pre;
        final long           evoTime;
        final UUID           id;
        boolean              set = false;

        public EvoTicker(World world, long evoTime, Entity evo, ITextComponent pre)
        {
            this.world = world;
            this.evoTime = evoTime;
            this.evo = evo;
            this.pre = pre;
            this.id = evo.getUniqueID();
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void tick(WorldTickEvent evt)
        {
            if (evt.world != world || evt.phase != Phase.END) return;
            boolean exists = false;
            for (Entity e : evt.world.loadedEntityList)
            {
                if (e.getUniqueID().equals(id))
                {
                    exists = true;
                    break;
                }
            }
            if (evt.world.getTotalWorldTime() > evoTime && !exists)
            {
                IPokemob pokemob = CapabilityPokemob.getPokemobFor(evo);
                evt.world.spawnEntityInWorld(evo);
                ITextComponent mess = CommandTools.makeTranslatedMessage("pokemob.evolve.success", "green",
                        pre.getFormattedText(), pokemob.getPokedexEntry().getName());
                pokemob.displayMessageToOwner(mess);
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    default void cancelEvolve()
    {
        if (!isEvolving()) return;
        EntityLivingBase entity = getEntity();
        if (getEntity().getEntityWorld().isRemote)
        {
            MessageServer message = new MessageServer(MessageServer.CANCELEVOLVE, entity.getEntityId());
            PokecubePacketHandler.sendToServer(message);
            return;
        }
        setEvolutionTicks(-1);
        this.setPokemonAIState(EVOLVING, false);
        this.displayMessageToOwner(new TextComponentTranslation("pokemob.evolution.cancel",
                CapabilityPokemob.getPokemobFor(entity).getPokemonDisplayName()));
    }

    /** Called when give item. to override when the pokemob evolve with a stone.
     *
     * @param itemId
     *            the shifted index of the item
     * @return whether should evolve */
    default boolean canEvolve(ItemStack stack)
    {
        if (stack != CompatWrapper.nullStack && Tools.isSameStack(stack, PokecubeItems.getStack("everstone")))
            return false;
        if (this.getPokedexEntry().canEvolve() && !PokecubeCore.isOnClientSide())
        {
            for (EvolutionData d : getPokedexEntry().getEvolutions())
            {
                if (d.shouldEvolve(CapabilityPokemob.getPokemobFor(getEntity()), stack)) { return true; }
            }
        }
        return false;
    }

    default boolean isEvolving()
    {
        return this.getPokemonAIState(EVOLVING);
    }

    /** Called when the level is up. Should be overridden to handle level up
     * events like evolution or move learning.
     * 
     * @param level
     *            the new level */
    default IPokemob levelUp(int level)
    {
        EntityLivingBase theEntity = getEntity();
        IPokemob theMob = CapabilityPokemob.getPokemobFor(theEntity);
        List<String> moves = Database.getLevelUpMoves(theMob.getPokedexEntry(), level, theMob.getMoveStats().oldLevel);
        Collections.shuffle(moves);
        if (!theEntity.getEntityWorld().isRemote)
        {
            ITextComponent mess = new TextComponentTranslation("pokemob.info.levelup", theMob.getPokemonDisplayName(),
                    level + "");
            theMob.displayMessageToOwner(mess);
        }
        HappinessType.applyHappiness(theMob, HappinessType.LEVEL);
        if (moves != null)
        {
            if (theMob.getPokemonAIState(IMoveConstants.TAMED))
            {
                String[] current = theMob.getMoves();
                if (current[3] != null)
                {
                    for (String s : current)
                    {
                        if (s == null) continue;
                        for (String s1 : moves)
                        {
                            if (s.equals(s1))
                            {
                                moves.remove(s1);
                                break;
                            }
                        }
                    }
                    for (String s : moves)
                    {
                        ITextComponent move = new TextComponentTranslation(MovesUtils.getUnlocalizedMove(s));
                        ITextComponent mess = new TextComponentTranslation("pokemob.move.notify.learn",
                                theMob.getPokemonDisplayName(), move);
                        theMob.displayMessageToOwner(mess);
                        if (!theMob.getMoveStats().newMoves.contains(s))
                        {
                            theMob.getMoveStats().newMoves.add(s);
                            PacketSyncNewMoves.sendUpdatePacket((IPokemob) this);
                        }
                    }
                    PacketHandler.sendEntityUpdate(getEntity());
                    return theMob;
                }
            }
            for (String s : moves)
            {
                theMob.learn(s);
            }
        }
        return theMob;
    }

    default IPokemob megaEvolve(PokedexEntry newEntry)
    {
        EntityLivingBase thisEntity = getEntity();
        IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        Entity evolution = thisEntity;
        IPokemob evoMob = thisMob;
        if (newEntry != null && newEntry != getPokedexEntry())
        {
            setPokemonAIState(EVOLVING, true);
            if (newEntry.getPokedexNb() != getPokedexNb())
            {
                evolution = PokecubeMod.core.createPokemob(newEntry, thisEntity.getEntityWorld());
                if (evolution == null)
                {
                    System.err.println("No Entry for " + newEntry);
                    return thisMob;
                }
                evoMob = CapabilityPokemob.getPokemobFor(evolution);
                // Flag the mob as evolving.
                evoMob.setPokemonAIState(EVOLVING, true);

                // Sync health and nickname
                ((EntityLivingBase) evolution).setHealth(thisEntity.getHealth());
                if (this.getPokemonNickname().equals(this.getPokedexEntry().getName())) this.setPokemonNickname("");

                // Sync tags besides the ones that define species and form.
                NBTTagCompound tag = thisMob.writePokemobData();
                tag.getCompoundTag(TagNames.OWNERSHIPTAG).removeTag(TagNames.POKEDEXNB);
                tag.getCompoundTag(TagNames.VISUALSTAG).removeTag(TagNames.FORME);
                evoMob.readPokemobData(tag);

                // Sync held item
                evoMob.setHeldItem(thisMob.getHeldItem());

                // Sync genes
                IMobGenetics oldGenes = thisEntity.getCapability(IMobGenetics.GENETICS_CAP, null);
                IMobGenetics newGenes = evolution.getCapability(IMobGenetics.GENETICS_CAP, null);
                newGenes.getAlleles().putAll(oldGenes.getAlleles());
                GeneticsManager.handleEpigenetics(evoMob);
                evoMob.onGenesChanged();
                // Set entry, this should fix expressed species gene.
                evoMob.setPokedexEntry(newEntry);

                // Sync entity data, UUID and location.
                evolution.getEntityData().merge(thisEntity.getEntityData());
                evolution.setUniqueId(thisEntity.getUniqueID());
                evolution.copyLocationAndAnglesFrom(thisEntity);

                // Set this mob wild, then kill it.
                this.setPokemonOwner((UUID) null);
                thisEntity.setDead();

                // Schedule adding to world.
                if (thisEntity.addedToChunk)
                {
                    long evoTime = thisEntity.getEntityWorld().getTotalWorldTime() + 2;
                    new EvoTicker(thisEntity.getEntityWorld(), evoTime, evolution, thisMob.getPokemonDisplayName());
                }
            }
            else
            {
                evolution = thisEntity;
                evoMob.setPokedexEntry(newEntry);
                evoMob.setAbility(newEntry.getAbility(thisMob.getAbilityIndex(), evoMob));
            }
        }
        return evoMob;
    }

    /** Evolve the pokemob.
     *
     * @param delayed
     *            true if we want to display the evolution animation
     * @return the evolution or this if the evolution failed */
    default IPokemob evolve(boolean delayed, boolean init)
    {
        EntityLivingBase thisEntity = getEntity();
        IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        return evolve(delayed, init, thisMob.getHeldItem());
    }

    /** Evolve the pokemob.
     *
     * @param delayed
     *            true if we want to display the evolution animation
     * @param init
     *            true if this is called during initialization of the mob
     * @param stack
     *            the itemstack to check for evolution.
     * @return the evolution or null if the evolution failed, or this if the
     *         evolution succeeded, but delayed. */
    default IPokemob evolve(boolean delayed, boolean init, ItemStack stack)
    {
        EntityLivingBase thisEntity = getEntity();
        IPokemob thisMob = CapabilityPokemob.getPokemobFor(thisEntity);
        // If Init, then don't bother about getting ready for animations and
        // such, just evolve directly.
        if (init)
        {
            boolean neededItem = false;
            PokedexEntry evol = null;
            EvolutionData data = null;
            // Find which evolution to use.
            for (EvolutionData d : this.getPokedexEntry().getEvolutions())
            {
                if (d.shouldEvolve(thisMob, stack))
                {
                    evol = d.evolution;
                    if (!d.shouldEvolve(thisMob, CompatWrapper.nullStack)) neededItem = true;
                    data = d;
                    break;
                }
            }
            if (evol != null)
            {
                // Send evolve event.
                EvolveEvent evt = new EvolveEvent.Pre(thisMob, evol);
                MinecraftForge.EVENT_BUS.post(evt);
                if (evt.isCanceled()) return null;
                // change to new forme.
                IPokemob evo = this.megaEvolve(((EvolveEvent.Pre) evt).forme);
                // Remove held item if it had one.
                if (neededItem)
                {
                    evo.setHeldItem(CompatWrapper.nullStack);
                }
                // Init things like moves.
                evo.getMoveStats().oldLevel = data.level - 1;
                evo.levelUp(evo.getLevel());

                // Learn evolution moves and update ability.
                for (String s : evo.getPokedexEntry().getEvolutionMoves())
                    evo.learn(s);
                evo.setAbility(evo.getPokedexEntry().getAbility(thisMob.getAbilityIndex(), evo));

                // Send post evolve event.
                evt = new EvolveEvent.Post(evo);
                MinecraftForge.EVENT_BUS.post(evt);
                // Kill old entity.
                getEntity().setDead();
                return evo;
            }
            return null;
        }
        // Do not evolve if it is dead, or can't evolve.
        else if (this.getPokedexEntry().canEvolve() && !thisEntity.isDead)
        {
            boolean neededItem = false;
            PokedexEntry evol = null;
            EvolutionData data = null;
            // look for evolution data to use.
            for (EvolutionData d : getPokedexEntry().getEvolutions())
            {
                if (d.shouldEvolve(thisMob, stack))
                {
                    evol = d.evolution;
                    if (!d.shouldEvolve(thisMob, CompatWrapper.nullStack) && stack == thisMob.getHeldItem())
                        neededItem = true;
                    data = d;
                    break;
                }
            }

            if (evol != null)
            {
                EvolveEvent evt = new EvolveEvent.Pre(thisMob, evol);
                MinecraftForge.EVENT_BUS.post(evt);
                if (evt.isCanceled()) return null;
                if (delayed)
                {
                    // If delayed, set the pokemob as starting to evolve, and
                    // set the evolution for display effects.
                    if (stack != CompatWrapper.nullStack) setEvolutionStack(stack.copy());
                    this.setEvolutionTicks(PokecubeMod.core.getConfig().evolutionTicks + 50);
                    this.setEvolvingEffects(evol);
                    this.setPokemonAIState(EVOLVING, true);
                    // Send the message about evolving, to let user cancel.
                    this.displayMessageToOwner(
                            new TextComponentTranslation("pokemob.evolution.start", thisMob.getPokemonDisplayName()));
                    return thisMob;
                }
                // Evolve the mob.
                IPokemob evo = megaEvolve(((EvolveEvent.Pre) evt).forme);
                if (evo != null)
                {
                    // Clear held item if used for evolving.
                    if (neededItem)
                    {
                        evo.setHeldItem(CompatWrapper.nullStack);
                    }
                    evt = new EvolveEvent.Post(evo);
                    MinecraftForge.EVENT_BUS.post(evt);
                    // Lean any moves that should are supposed to have just
                    // learnt.
                    if (delayed) evo.getMoveStats().oldLevel = evo.getLevel() - 1;
                    else if (data != null) evo.getMoveStats().oldLevel = data.level - 1;
                    evo.levelUp(evo.getLevel());

                    // Don't immediately try evolving again, only wild ones
                    // should do that.
                    evo.setEvolutionTicks(-1);
                    evo.setPokemonAIState(EVOLVING, false);

                    // Learn evolution moves and update ability.
                    for (String s : evo.getPokedexEntry().getEvolutionMoves())
                        evo.learn(s);
                    evo.setAbility(evo.getPokedexEntry().getAbility(thisMob.getAbilityIndex(), evo));

                    // Kill old entity.
                    thisEntity.setDead();
                }
                return evo;
            }
        }
        return null;
    }

    /** The evolution tick will be set when the mob evolves and then is
     * decreased each tick. It is used to render a special effect.
     * 
     * @param evolutionTicks
     *            the evolutionTicks to set */
    void setEvolutionTicks(int evolutionTicks);

    /** This itemstack will be used to evolve the pokemob after evolutionTicks
     * runs out. */
    void setEvolutionStack(ItemStack stack);

    /** This is the itemstack we are using for evolution, it is stored here for
     * use when evolution actually occurs. */
    ItemStack getEvolutionStack();

    /** Can set a custom entry for use with colouring the evolution effects. */
    default void setEvolvingEffects(PokedexEntry entry)
    {

    }

    /** This entry is used for colouring evolution effects. */
    default PokedexEntry getEvolutionEntry()
    {
        return getPokedexEntry();
    }
}
