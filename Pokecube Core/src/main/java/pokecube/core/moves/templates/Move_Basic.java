/**
 *
 */
package pokecube.core.moves.templates;

import static pokecube.core.utils.PokeType.getAttackEfficiency;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import pokecube.core.PokecubeCore;
import pokecube.core.database.abilities.Ability;
import pokecube.core.database.moves.MoveEntry;
import pokecube.core.events.MoveUse;
import pokecube.core.events.MoveUse.MoveWorldAction;
import pokecube.core.interfaces.IMoveConstants;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.interfaces.IPokemob.MovePacket;
import pokecube.core.interfaces.IPokemob.Stats;
import pokecube.core.interfaces.Move_Base;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import pokecube.core.moves.PokemobDamageSource;
import pokecube.core.moves.animations.AnimationMultiAnimations;
import pokecube.core.utils.PokeType;
import thut.api.entity.IHungrymob;
import thut.api.maths.Vector3;
import thut.api.terrain.TerrainManager;
import thut.api.terrain.TerrainSegment;
import thut.lib.Accessor;

/** @author Manchou */
public class Move_Basic extends Move_Base implements IMoveConstants
{
    public static ItemStack createStackedBlock(IBlockState state)
    {
        int i = 0;
        Item item = Item.getItemFromBlock(state.getBlock());

        if (item != null && item.getHasSubtypes())
        {
            i = state.getBlock().getMetaFromState(state);
        }
        return new ItemStack(item, 1, i);
    }

    public static boolean shouldSilk(IPokemob pokemob)
    {
        if (pokemob.getAbility() == null) return false;
        Ability ability = pokemob.getAbility();
        return pokemob.getLevel() > 90 && ability.toString().equalsIgnoreCase("hypercutter");
    }

    public static void silkHarvest(IBlockState state, BlockPos pos, World worldIn, EntityPlayer player)
    {
        java.util.ArrayList<ItemStack> items = new java.util.ArrayList<ItemStack>();
        ItemStack itemstack = createStackedBlock(state);

        if (itemstack != null)
        {
            items.add(itemstack);
        }

        net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(items, worldIn, pos, worldIn.getBlockState(pos),
                0, 1.0f, true, player);
        for (ItemStack stack : items)
        {
            Block.spawnAsEntity(worldIn, pos, stack);
        }
    }

    Vector3 v  = Vector3.getNewVector();

    Vector3 v1 = Vector3.getNewVector();

    /** Constructor for a Pokemob move. <br/>
     * The attack category defines the way the mob will move in order to make
     * its attack.
     *
     * @param name
     *            the English name of the attack, used as identifier and
     *            translation key
     * @param attackCategory
     *            can be either {@link MovesUtils#CATEGORY_CONTACT} or
     *            {@link MovesUtils#CATEGORY_DISTANCE} */
    public Move_Basic(String name)
    {
        super(name);
    }

    @Override
    public void attack(IPokemob attacker, Entity attacked)
    {
        if (attacker.getStatus() == STATUS_SLP)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_SLP, false);
            return;
        }
        if (attacker.getStatus() == STATUS_FRZ)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_FRZ, false);
            return;
        }
        if (attacker.getStatus() == STATUS_PAR && Math.random() > 0.75)
        {
            MovesUtils.displayStatusMessages(attacker, attacked, STATUS_PAR, false);
            return;
        }
        if (AnimationMultiAnimations.isThunderAnimation(getAnimation(attacker)))
        {
            EntityLightningBolt lightning = new EntityLightningBolt(attacked.getEntityWorld(), 0, 0, 0, false);
            attacked.onStruckByLightning(lightning);
        }
        if (attacked instanceof EntityCreeper)
        {
            EntityCreeper creeper = (EntityCreeper) attacked;
            if (move.type == PokeType.getType("psychic") && creeper.getHealth() > 0)
            {
                Accessor.explode(creeper);
            }
        }
        playSounds((Entity) attacker, attacked, null);
        byte statusChange = STATUS_NON;
        byte changeAddition = CHANGE_NONE;
        if (move.statusChange != STATUS_NON && MovesUtils.rand.nextInt(100) <= move.statusChance)
        {
            statusChange = move.statusChange;
        }
        if (move.change != CHANGE_NONE && MovesUtils.rand.nextInt(100) <= move.chanceChance)
        {
            changeAddition = move.change;
        }
        MovePacket packet = new MovePacket(attacker, attacked, name, move.type, getPWR(attacker, attacked), move.crit,
                statusChange, changeAddition);

        boolean self = (this.getAttackCategory() & CATEGORY_SELF) > 0;
        boolean doAttack = true;
        if (!self) doAttack = attacked != attacker;
        if (doAttack) onAttack(packet);
    }

    @Override
    public void attack(IPokemob attacker, Vector3 location)
    {
        List<Entity> targets = new ArrayList<Entity>();

        Entity entity = (Entity) attacker;

        if (!move.notIntercepable && attacker.getPokemonAIState(IMoveConstants.ANGRY))
        {
            Vec3d loc1 = new Vec3d(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
            Vec3d loc2 = new Vec3d(location.x, location.y, location.z);
            RayTraceResult pos = entity.getEntityWorld().rayTraceBlocks(loc1, loc2, false);
            if (pos != null)
            {
                location.set(pos.hitVec);
            }
        }
        if (move.multiTarget)
        {
            targets.addAll(MovesUtils.targetsHit(((Entity) attacker), location));
        }
        else if (!move.notIntercepable)
        {
            targets.add(MovesUtils.targetHit(entity, location));
        }
        else
        {
            List<Entity> subTargets = new ArrayList<Entity>();
            if (subTargets.contains(attacker)) subTargets.remove(attacker);
            targets.addAll(subTargets);
        }
        if ((move.attackCategory & CATEGORY_SELF) != 0)
        {
            targets.clear();
            targets.add((Entity) attacker);
        }
        int n = targets.size();
        playSounds((Entity) attacker, null, location);
        if (n > 0)
        {
            for (Entity e : targets)
            {
                if (e != null)
                {
                    Entity attacked = e;
                    attack(attacker, attacked);
                }
            }
        }
        else
        {
            MovesUtils.displayEfficiencyMessages(attacker, null, -1, 0);
        }
        doWorldAction(attacker, location);
    }

    @Override
    public void doWorldAction(IPokemob attacker, Vector3 location)
    {
        for (String s : PokecubeCore.core.getConfig().damageBlocksBlacklist)
        {
            if (s.equals(name)) return;
        }
        deny:
        if (!PokecubeMod.pokemobsDamageBlocks)
        {
            for (String s : PokecubeCore.core.getConfig().damageBlocksWhitelist)
            {
                if (s.equals(name)) break deny;
            }
            return;
        }
        MoveWorldAction.PreAction preEvent = new MoveWorldAction.PreAction(this, attacker, location);
        if (!MinecraftForge.EVENT_BUS.post(preEvent))
        {
            MoveWorldAction.OnAction onEvent = new MoveWorldAction.OnAction(this, attacker, location);
            MinecraftForge.EVENT_BUS.post(onEvent);
            MoveWorldAction.PostAction postEvent = new MoveWorldAction.PostAction(this, attacker, location);
            MinecraftForge.EVENT_BUS.post(postEvent);
        }
    }

    @Override
    public void applyHungerCost(IPokemob attacker)
    {
        int pp = getPP();
        float relative = (50 - pp) / 30;
        relative = relative * relative;
        IHungrymob mob = (IHungrymob) attacker;
        mob.setHungerTime(mob.getHungerTime() + (int) (relative * 100));
    }

    @Override
    public Move_Base getMove(String name)
    {
        return MovesUtils.getMoveFromName(name);
    }

    @Override
    public void postAttack(MovePacket packet)
    {
        IPokemob attacker = packet.attacker;
        Entity attacked = packet.attacked;
        attacker.onMoveUse(packet);
        if (attacked instanceof IPokemob)
        {
            ((IPokemob) attacked).onMoveUse(packet);
        }
        MinecraftForge.EVENT_BUS.post(new MoveUse.ActualMoveUse.Post(packet.attacker, this, packet.attacked));
    }

    @Override
    public void preAttack(MovePacket packet)
    {
        MinecraftForge.EVENT_BUS.post(new MoveUse.ActualMoveUse.Pre(packet.attacker, this, packet.attacked));
        IPokemob attacker = packet.attacker;
        attacker.getMoveStats().nextMoveTick = (int) (((Entity) attacker).ticksExisted
                + PokecubeMod.core.getConfig().attackCooldown * this.getPostDelayFactor(attacker));
        Entity attacked = packet.attacked;
        attacker.onMoveUse(packet);
        if (attacked instanceof IPokemob)
        {
            ((IPokemob) attacked).onMoveUse(packet);
        }
    }

    @Override
    public void onAttack(MovePacket packet)
    {
        preAttack(packet);
        if (packet.denied) return;
        MovePacket backup = packet;
        IPokemob attacker = packet.attacker;
        Entity attacked = packet.attacked;
        Random rand = new Random();
        String attack = packet.attack;
        PokeType type = packet.attackType;
        int PWR = packet.PWR;
        int criticalLevel = packet.criticalLevel;
        float criticalFactor = packet.critFactor;
        byte statusChange = packet.statusChange;
        byte changeAddition = packet.changeAddition;
        float stabFactor = packet.stabFactor;
        if (!packet.stab)
        {
            packet.stab = packet.attacker.isType(packet.attackType);
        }
        if (!packet.stab)
        {
            stabFactor = 1;
        }
        if (packet.canceled)
        {
            MovesUtils.displayEfficiencyMessages(attacker, attacked, -2, 0);
            packet = new MovePacket(attacker, attacked, attack, type, PWR, criticalLevel, statusChange, changeAddition,
                    false);
            packet.hit = false;
            packet.didCrit = false;
            postAttack(packet);
            return;
        }
        if (packet.failed)
        {
            MovesUtils.displayEfficiencyMessages(attacker, attacked, -2, 0);
            packet = new MovePacket(attacker, attacked, attack, type, PWR, criticalLevel, statusChange, changeAddition,
                    false);
            packet.hit = false;
            packet.didCrit = false;
            postAttack(packet);
            return;
        }

        if (packet.infatuateTarget && attacked instanceof IPokemob)
        {
            ((IPokemob) attacked).getMoveStats().infatuateTarget = (Entity) attacker;
        }

        if (packet.infatuateAttacker)
        {
            attacker.getMoveStats().infatuateTarget = attacked;
        }

        attacker = packet.attacker;
        attacked = packet.attacked;
        attack = packet.attack;
        type = packet.attackType;
        PWR = packet.PWR;
        criticalLevel = packet.criticalLevel;
        criticalFactor = packet.critFactor;
        statusChange = packet.statusChange;
        changeAddition = packet.changeAddition;
        boolean toSurvive = packet.noFaint;

        if (attacked == null)
        {
            packet = new MovePacket(attacker, attacked, attack, type, PWR, criticalLevel, statusChange, changeAddition,
                    false);
            packet.hit = false;
            packet.didCrit = false;
            postAttack(packet);
            return;
        }

        float efficiency = 1;

        if (attacked instanceof IPokemob)
        {
            efficiency = getAttackEfficiency(type, ((IPokemob) attacked).getType1(), ((IPokemob) attacked).getType2());
        }

        float criticalRatio = 1;

        if (attacker.getMoveStats().SPECIALTYPE == IPokemob.TYPE_CRIT)
        {
            criticalLevel += 1;
            attacker.getMoveStats().SPECIALTYPE = 0;
        }

        int critcalRate = 16;

        if (criticalLevel == 2)
        {
            critcalRate = 8;
        }

        if (criticalLevel == 3)
        {
            critcalRate = 4;
        }

        if (criticalLevel == 4)
        {
            critcalRate = 3;
        }

        if (criticalLevel == 5)
        {
            critcalRate = 2;
        }

        if (criticalLevel > 0 && rand.nextInt(critcalRate) == 0)
        {
            criticalRatio = criticalFactor;
        }

        float attackStrength = attacker.getAttackStrength() * PWR / 150;

        if (attacked instanceof IPokemob)
        {
            attackStrength = MovesUtils.getAttackStrength(attacker, (IPokemob) attacked,
                    packet.getMove().getCategory(attacker), PWR, packet);

            int moveAcc = packet.getMove().move.accuracy;
            if (moveAcc > 0)
            {
                double accuracy = attacker.getModifiers().getDefaultMods().getModifier(Stats.ACCURACY);
                double evasion = attacker.getModifiers().getDefaultMods().getModifier(Stats.EVASION);
                double moveAccuracy = (moveAcc) / 100d;

                double hitModifier = moveAccuracy * accuracy / evasion;

                if (hitModifier < Math.random())
                {
                    efficiency = -1;
                }

            }
            if (moveAcc == -3)
            {
                double moveAccuracy = ((attacker.getLevel() - ((IPokemob) attacked).getLevel()) + 30) / 100d;

                double hitModifier = attacker.getLevel() < ((IPokemob) attacked).getLevel() ? -1 : moveAccuracy;

                if (hitModifier < Math.random())
                {
                    efficiency = -1;
                }
            }
        }
        if (attacked != attacker && attacked instanceof IPokemob && attacker instanceof EntityLiving)
        {
            if (((EntityLiving) attacked).getAttackTarget() != attacker)
                ((EntityLiving) attacked).setAttackTarget((EntityLivingBase) attacker);
            ((IPokemob) attacked).setPokemonAIState(IMoveConstants.ANGRY, true);
        }
        if (efficiency > 0 && packet.applyOngoing)
        {
            Move_Ongoing ongoing;
            if (MovesUtils.getMoveFromName(attack) instanceof Move_Ongoing)
            {
                ongoing = (Move_Ongoing) MovesUtils.getMoveFromName(attack);
                if (ongoing.onTarget() && attacked instanceof IPokemob) ((IPokemob) attacked).addOngoingEffect(ongoing);
                if (ongoing.onSource()) attacker.addOngoingEffect(ongoing);
            }
        }
        TerrainSegment terrain = TerrainManager.getInstance().getTerrainForEntity((Entity) attacker);
        float terrainDamageModifier = MovesUtils.getTerrainDamageModifier(type, (Entity) attacker, terrain);

        int finalAttackStrength = Math.max(0, Math.round(attackStrength * efficiency * criticalRatio
                * terrainDamageModifier * stabFactor * packet.superEffectMult));

        float healRatio;
        float damageRatio;

        int beforeHealth = (int) ((EntityLivingBase) attacked).getHealth();

        if (efficiency > 0 && MoveEntry.oneHitKos.contains(attack))
        {
            finalAttackStrength = beforeHealth;
        }

        if (toSurvive)
        {
            finalAttackStrength = Math.min(finalAttackStrength, beforeHealth - 1);
            finalAttackStrength = Math.max(0, finalAttackStrength);
        }

        boolean wild = !attacker.getPokemonAIState(TAMED);

        if (PokecubeMod.core.getConfig().maxWildPlayerDamage >= 0 && wild && attacked instanceof EntityPlayer)
        {
            finalAttackStrength = Math.min(PokecubeMod.core.getConfig().maxWildPlayerDamage, finalAttackStrength);
        }
        else if (PokecubeMod.core.getConfig().maxOwnedPlayerDamage >= 0 && !wild && attacked instanceof EntityPlayer)
        {
            finalAttackStrength = Math.min(PokecubeMod.core.getConfig().maxOwnedPlayerDamage, finalAttackStrength);
        }

        if (wild && attacked instanceof EntityPlayer)
        {
            finalAttackStrength *= PokecubeMod.core.getConfig().wildPlayerDamageRatio;
        }
        else if (!wild && attacked instanceof EntityPlayer)
        {
            finalAttackStrength *= PokecubeMod.core.getConfig().ownedPlayerDamageRatio;
        }
        else if (!(attacked instanceof IPokemob))
        {
            finalAttackStrength *= PokecubeMod.core.getConfig().pokemobToOtherMobDamageRatio;
        }

        if (attacked instanceof IPokemob)
        {
            IPokemob mob = (IPokemob) attacked;
            if (mob.getAbility() != null)
            {
                finalAttackStrength = mob.getAbility().beforeDamage(mob, packet, finalAttackStrength);
            }
        }

        if ((move.attackCategory & CATEGORY_SELF) == 0 && move.defrosts && attacked instanceof IPokemob
                && (((IPokemob) attacked).getStatus() & IMoveConstants.STATUS_FRZ) > 0)
        {
            ((IPokemob) attacked).healStatus();
        }

        if (!((move.attackCategory & CATEGORY_SELF) > 0 && PWR == 0) && finalAttackStrength > 0)
        {
            if (attacked instanceof EntityPlayer)
            {
                DamageSource source1 = new PokemobDamageSource("mob", (EntityLivingBase) attacker,
                        MovesUtils.getMoveFromName(attack));
                DamageSource source2 = new PokemobDamageSource("mob", (EntityLivingBase) attacker,
                        MovesUtils.getMoveFromName(attack));
                source2.setDamageBypassesArmor();
                source2.setMagicDamage();
                float d1, d2;
                if (wild)
                {
                    d2 = (float) (finalAttackStrength
                            * Math.min(1, PokecubeMod.core.getConfig().wildPlayerDamageMagic));
                    d1 = finalAttackStrength - d2;
                }
                else
                {
                    d2 = (float) (finalAttackStrength
                            * Math.min(1, PokecubeMod.core.getConfig().ownedPlayerDamageMagic));
                    d1 = finalAttackStrength - d2;
                }
                attacked.attackEntityFrom(source1, d1);
                attacked.attackEntityFrom(source2, d2);
            }
            else
            {
                DamageSource source = new PokemobDamageSource("mob", (EntityLivingBase) attacker,
                        MovesUtils.getMoveFromName(attack));
                attacked.attackEntityFrom(source, finalAttackStrength);
            }

            if (attacked instanceof IPokemob)
            {
                if (move.category == SPECIAL)
                    ((IPokemob) attacked).getMoveStats().SPECIALDAMAGETAKENCOUNTER += finalAttackStrength;
                if (move.category == PHYSICAL)
                    ((IPokemob) attacked).getMoveStats().PHYSICALDAMAGETAKENCOUNTER += finalAttackStrength;
            }
        }

        if ((efficiency > 0 || packet.getMove().move.attackCategory == CATEGORY_SELF) && statusChange != STATUS_NON)
        {
            MovesUtils.setStatus(attacked, statusChange);
        }
        if (efficiency > 0 && changeAddition != CHANGE_NONE) MovesUtils.addChange(attacked, changeAddition);

        if (packet.getMove().getPWR(attacker, attacked) > 0)
            MovesUtils.displayEfficiencyMessages(attacker, attacked, efficiency, criticalRatio);

        int afterHealth = (int) Math.max(0, ((EntityLivingBase) attacked).getHealth());

        int damageDealt = beforeHealth - afterHealth;

        healRatio = (packet.getMove().move.damageHealRatio) / 100;
        damageRatio = packet.getMove().move.selfDamage;
        if (damageRatio > 0)
        {
            if (packet.getMove().move.selfDamageType == MoveEntry.TOTALHP)
            {
                float max = ((EntityLiving) attacker).getMaxHealth();
                float diff = max * damageRatio / 100f;
                ((EntityLiving) attacker).setHealth(max - diff);
            }
            if (packet.getMove().move.selfDamageType == MoveEntry.MISS && efficiency <= 0)
            {
                float max = ((EntityLiving) attacker).getMaxHealth();
                float diff = max * damageRatio / 100f;
                ((EntityLiving) attacker).attackEntityFrom(DamageSource.FALL, diff);
            }
            if (packet.getMove().move.selfDamageType == MoveEntry.DAMAGEDEALT)
            {
                float diff = damageDealt * damageRatio / 100f;
                ((EntityLiving) attacker).attackEntityFrom(DamageSource.FALL, diff);
            }
            if (packet.getMove().move.selfDamageType == MoveEntry.RELATIVEHP)
            {
                float current = ((EntityLiving) attacker).getHealth();
                float diff = current * damageRatio / 100f;
                ((EntityLiving) attacker).attackEntityFrom(DamageSource.FALL, diff);
            }
        }

        if (healRatio > 0)
        {
            float toHeal = Math.max(1, (damageDealt * healRatio));
            ((EntityLiving) attacker).setHealth(
                    Math.min(((EntityLiving) attacker).getMaxHealth(), ((EntityLiving) attacker).getHealth() + toHeal));
        }

        healRatio = (move.selfHealRatio) / 100;
        boolean canHeal = ((EntityLiving) attacker).getHealth() < ((EntityLiving) attacker).getMaxHealth();
        if (healRatio > 0 && canHeal && attacker.getMoveStats().SELFRAISECOUNTER == 0)
        {
            ((EntityLiving) attacker).setHealth(Math.min(((EntityLiving) attacker).getMaxHealth(),
                    ((EntityLiving) attacker).getHealth() + (((EntityLiving) attacker).getMaxHealth() * healRatio)));
            attacker.getMoveStats().SELFRAISECOUNTER = (PokecubeMod.core.getConfig().attackCooldown * 4);
            attacker.setAttackCooldown(attacker.getMoveStats().SELFRAISECOUNTER);
        }

        packet = new MovePacket(attacker, attacked, attack, type, PWR, criticalLevel, statusChange, changeAddition,
                false);
        packet.hit = efficiency >= 0;
        packet.didCrit = criticalRatio > 1;
        packet.damageDealt = beforeHealth - afterHealth;
        backup.damageDealt = packet.damageDealt;
        handleStatsChanges(packet);
        postAttack(packet);
    }

    @Override
    public void handleStatsChanges(MovePacket packet)
    {
        boolean shouldEffect = packet.attackedStatModProb > 0 || packet.attackerStatModProb > 0;
        if (!shouldEffect) return;
        boolean effect = false;
        if (packet.attacked instanceof IPokemob && hasStatModTarget && packet.hit)
        {
            effect = MovesUtils.handleStats((IPokemob) packet.attacked, (Entity) packet.attacker, packet, true);
        }
        if (packet.getMove().hasStatModSelf)
        {
            effect = MovesUtils.handleStats(packet.attacker, (Entity) packet.attacker, packet, false);
        }
        if (!effect)
        {
            MovesUtils.displayStatsMessage(packet.attacker, packet.attacked, -2, (byte) 0, (byte) 0);
        }
    }
}
