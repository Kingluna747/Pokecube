package pokecube.core.interfaces.capabilities.impl;

import java.util.Vector;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.PokecubeCore;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.EntityPokemob;
import pokecube.core.events.EggEvent;
import pokecube.core.interfaces.IMoveNames;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.items.pokemobeggs.EntityPokemobEgg;
import pokecube.core.utils.Tools;
import thut.api.entity.IBreedingMob;
import thut.api.entity.IHungrymob;
import thut.api.maths.Vector3;

public abstract class PokemobSexed extends PokemobStats
{

    @Override
    public boolean canMate(EntityAnimal entityAnimal)
    {
        if (entityAnimal instanceof IPokemob)
        {
            PokedexEntry thisEntry = getPokedexEntry();
            PokedexEntry thatEntry = ((IPokemob) entityAnimal).getPokedexEntry();

            boolean transforms = false;
            boolean otherTransforms = false;
            for (String s : getMoves())
            {
                if (s != null && s.equalsIgnoreCase(IMoveNames.MOVE_TRANSFORM)) transforms = true;
            }
            for (String s : ((IPokemob) entityAnimal).getMoves())
            {
                if (s != null && s.equalsIgnoreCase(IMoveNames.MOVE_TRANSFORM)) otherTransforms = true;
            }

            // can't breed two transformers
            if (transforms && otherTransforms) return false;
            else if (transforms || otherTransforms) // Anything else will mate
                                                    // with ditto
                return true;
            boolean neutral = this.getSexe() == IPokemob.NOSEXE;
            return thisEntry.areRelated(thatEntry)
                    && (neutral || ((IPokemob) entityAnimal).getSexe() != this.getSexe());

        }

        return false;
    }

    @Override
    public Object getChild(IBreedingMob male)
    {
        boolean transforms = false;
        boolean otherTransforms = ((IPokemob) male).getTransformedTo() != null;
        String[] moves = getMoves();
        for (String s : moves)
        {
            if (s != null && s.equalsIgnoreCase(IMoveNames.MOVE_TRANSFORM)) transforms = true;
        }
        if (!otherTransforms) for (String s : ((IPokemob) male).getMoves())
        {
            if (s != null && s.equalsIgnoreCase(IMoveNames.MOVE_TRANSFORM)) otherTransforms = true;
        }
        if (transforms && !otherTransforms
                && ((IPokemob) male).getTransformedTo() != getEntity()) { return male.getChild(this); }
        return getPokedexEntry().getChild(((IPokemob) male).getPokedexEntry());
    }

    @Override
    /** Which entity is this pokemob trying to breed with
     * 
     * @return */
    public Entity getLover()
    {
        return lover;
    }

    @Override
    public int getLoveTimer()
    {
        return loveTimer;
    }

    @Override
    public Vector<IBreedingMob> getMalesForBreeding()
    {
        return males;
    }

    public void lay(IPokemob male)
    {
        here.set(getEntity());
        if (PokecubeMod.debug) System.out.println(this + " lay()");
        if (getEntity().getEntityWorld().isRemote) { return; }
        int num = Tools.countPokemon(getEntity().getEntityWorld(), here, PokecubeMod.core.getConfig().maxSpawnRadius);
        if (!(getOwner() instanceof EntityPlayer) && num > PokecubeMod.core.getConfig().mobSpawnNumber * 1.25) return;
        Vector3 pos = Vector3.getNewVector().set(this).addTo(0,
                Math.max(getPokedexEntry().height * getSize() / 4, 0.5f), 0);
        if (pos.isClearOfBlocks(getEntity().getEntityWorld()))
        {
            Entity eggItem = null;
            try
            {
                eggItem = new EntityPokemobEgg(getEntity().getEntityWorld(), here.x, here.y, here.z, getEntity(), male);
            }
            catch (Exception e1)
            {
                e1.printStackTrace();
            }
            EggEvent.Lay event;
            try
            {
                event = new EggEvent.Lay(eggItem);
                MinecraftForge.EVENT_BUS.post(event);
                if (!event.isCanceled())
                {
                    egg = eggItem;
                    getEntity().getEntityWorld().spawnEntityInWorld(egg);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return;
        }
    }

    protected void mate(IBreedingMob male)
    {
        if (male == null || ((Entity) male).isDead) return;
        if (this.getSexe() == MALE || male.getSexe() == FEMALE && male != this)
        {
            ((EntityPokemob) male).mateWith(this);
            return;
        }
        int hungerValue = PokecubeMod.core.getConfig().pokemobLifeSpan / 2;
        if (male instanceof IHungrymob)
        {
            IHungrymob hungry = (IHungrymob) male;
            hungry.setHungerTime(hungry.getHungerTime() + hungerValue);
        }
        setHungerTime(getHungerTime() + hungerValue);
        ((EntityPokemob) male).setLover(null);
        ((EntityPokemob) male).resetInLove();
        getEntity().setAttackTarget(null);
        ((EntityPokemob) male).setAttackTarget(null);
        lay((IPokemob) male);
        resetLoveStatus();
        lover = null;
    }

    @Override
    public void mateWith(final IBreedingMob male)
    {
        PokecubeCore.proxy.getMainThreadListener().addScheduledTask(new Runnable()
        {
            @Override
            public void run()
            {
                mate(male);
            }
        });
    }

    @Override
    public void resetLoveStatus()
    {
        setLoveTimer(rand.nextInt(600) - getBreedingDelay(null));
        setLover(null);
        setPokemonAIState(MATING, false);
        setPokemonAIState(ANGRY, false);
        if (males != null) males.clear();
    }

    @Override
    /** Sets the entity to try to breed with
     * 
     * @param lover */
    public void setLover(final Entity newLover)
    {
        this.lover = newLover;
    }

    private int getBreedingDelay(IPokemob mate)
    {
        return PokecubeMod.core.getConfig().breedingDelay;
    }

    @Override
    public void setLoveTimer(final int value)
    {
        loveTimer = value;
    }

    @Override
    public boolean tryToBreed()
    {
        return loveTimer > 0 || lover != null;
    }
}