package pokecube.core.entity.pokemobs.helper;

import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.EVSGENE;
import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.IVSGENE;
import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.MOVESGENE;
import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.NATUREGENE;
import static pokecube.core.entity.pokemobs.genetics.GeneticsManager.SIZEGENE;

import java.util.Random;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import pokecube.core.database.PokedexEntry;
import pokecube.core.entity.pokemobs.genetics.epigenes.EVsGene;
import pokecube.core.entity.pokemobs.genetics.epigenes.MovesGene;
import pokecube.core.entity.pokemobs.genetics.genes.IVsGene;
import pokecube.core.entity.pokemobs.genetics.genes.SizeGene;
import pokecube.core.interfaces.PokecubeMod;
import pokecube.core.moves.MovesUtils;
import pokecube.core.utils.PokecubeSerializer;
import pokecube.core.utils.Tools;
import thut.api.entity.genetics.Alleles;
import thut.api.entity.genetics.IMobGenetics;

/** This class will store the various stats of the pokemob as Alleles, and will
 * provider quick getters and setters for the genes. */
public abstract class EntityGeneticsPokemob extends EntityTameablePokemob
{
    public static float scaleFactor = 0.075f;
    Alleles             genesSize;
    Alleles             genesIVs;
    Alleles             genesEVs;
    Alleles             genesMoves;
    Alleles             genesNature;

    public EntityGeneticsPokemob(World world)
    {
        super(world);
    }

    @Override
    public float getSize()
    {
        if (genesSize == null)
        {
            IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
            if (genes == null) return 1;
            genesSize = genes.getAlleles().get(SIZEGENE);
            if (genesSize == null)
            {
                genesSize = new Alleles();
                genes.getAlleles().put(SIZEGENE, genesSize);
                SizeGene size = new SizeGene();
                float scale = 1 + scaleFactor * (float) (new Random()).nextGaussian();
                size.setValue(scale);
                genesSize.getAlleles()[0] = size;
                genesSize.getAlleles()[1] = size;
                genesSize.refreshExpressed();
            }
        }
        Float size = genesSize.getExpressed().getValue();
        return (float) (size * PokecubeMod.core.getConfig().scalefactor);
    }

    @Override
    public void setIVs(byte[] ivs)
    {
        if (genesIVs == null) getIVs();
        if (genesIVs != null) genesIVs.getExpressed().setValue(ivs);
    }

    @Override
    public void setEVs(byte[] evs)
    {
        int[] ints = PokecubeSerializer.byteArrayAsIntArray(evs);
        dataManager.set(EVS1DW, ints[0]);
        dataManager.set(EVS2DV, ints[1]);
        if (genesEVs == null) getIVs();
        if (genesEVs != null) genesEVs.getExpressed().setValue(evs);
    }

    @Override
    public byte[] getEVs()
    {
        if (!isServerWorld())
        {
            int[] ints = new int[] { dataManager.get(EVS1DW), dataManager.get(EVS2DV) };
            byte[] evs = PokecubeSerializer.intArrayAsByteArray(ints);
            return evs;
        }
        else
        {
            if (genesEVs == null)
            {
                IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
                if (genes == null) return new byte[] { 0, 0, 0, 0, 0, 0 };
                genesEVs = genes.getAlleles().get(EVSGENE);
                if (genesEVs == null)
                {
                    genesEVs = new Alleles();
                    genes.getAlleles().put(EVSGENE, genesEVs);
                    EVsGene ivs = new EVsGene();
                    byte[] iv = new byte[] { 0, 0, 0, 0, 0, 0 };
                    ivs.setValue(iv);
                    genesEVs.getAlleles()[0] = ivs;
                    genesEVs.getAlleles()[1] = ivs;
                    genesEVs.refreshExpressed();
                    genesEVs.getExpressed().setValue(new byte[] { 0, 0, 0, 0, 0, 0 });
                }
            }
            return genesEVs.getExpressed().getValue();
        }
    }

    @Override
    public byte[] getIVs()
    {
        if (genesIVs == null)
        {
            IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
            if (genes == null) return new byte[] { 0, 0, 0, 0, 0, 0 };
            genesIVs = genes.getAlleles().get(IVSGENE);
            if (genesIVs == null)
            {
                genesIVs = new Alleles();
                genes.getAlleles().put(IVSGENE, genesIVs);
                IVsGene ivs = new IVsGene();
                byte[] iv = new byte[] { Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand),
                        Tools.getRandomIV(rand), Tools.getRandomIV(rand), Tools.getRandomIV(rand) };
                ivs.setValue(iv);
                genesIVs.getAlleles()[0] = ivs;
                genesIVs.getAlleles()[1] = ivs;
                genesIVs.refreshExpressed();
            }
        }
        return genesIVs.getExpressed().getValue();
    }

    @Override
    public String[] getMoves()
    {
        if (!isServerWorld())
        {
            String movesString = dataManager.get(MOVESDW);
            String[] moves = getMoveStats().moves;
            if (movesString != null && movesString.length() > 2)
            {
                String[] movesSplit = movesString.split(",");
                for (int i = 0; i < Math.min(4, movesSplit.length); i++)
                {
                    String move = movesSplit[i];

                    if (move != null && move.length() > 1 && MovesUtils.isMoveImplemented(move))
                    {
                        moves[i] = move;
                    }
                }
            }
            return moves;
        }
        else
        {
            String[] moves = getMoveStats().moves;
            if (genesMoves == null)
            {
                IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
                if (genes == null) return moves;
                genesMoves = genes.getAlleles().get(MOVESGENE);
                if (genesMoves == null)
                {
                    genesMoves = new Alleles();
                    genes.getAlleles().put(MOVESGENE, genesMoves);
                    MovesGene gene = new MovesGene();
                    gene.setValue(moves);
                    genesMoves.getAlleles()[0] = gene;
                    genesMoves.getAlleles()[1] = gene;
                    genesMoves.refreshExpressed();
                }
            }
            return getMoveStats().moves = genesMoves.getExpressed().getValue();
        }
    }

    @Override
    public void setMove(int i, String moveName)
    {
        String[] moves = getMoves();
        moves[i] = moveName;
        setMoves(moves);
    }

    public void setMoves(String[] moves)
    {
        String movesString = "";

        if (moves != null && moves.length == 4)
        {
            if (genesMoves == null)
            {
                IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
                if (genes == null) return;
                genesMoves = genes.getAlleles().get(MOVESGENE);
                if (genesMoves == null)
                {
                    genesMoves = new Alleles();
                    genes.getAlleles().put(MOVESGENE, genesMoves);
                    MovesGene gene = new MovesGene();
                    gene.setValue(moves);
                    genesMoves.getAlleles()[0] = gene;
                    genesMoves.getAlleles()[1] = gene;
                    genesMoves.refreshExpressed();
                }
            }
            genesMoves.getExpressed().setValue(getMoveStats().moves = moves);
        }
        dataManager.set(MOVESDW, movesString);
    }

    @Override
    public void setSize(float size)
    {
        getSize();
        if (genesSize == null) return;
        if (isAncient()) size = 2;
        float a = 1, b = 1, c = 1;
        PokedexEntry entry = getPokedexEntry();
        if (entry != null)
        {
            a = entry.width * size;
            b = entry.height * size;
            c = entry.length * size;
            if (a < 0.01 || b < 0.01 || c < 0.01)
            {
                float min = 0.01f / Math.min(a, Math.min(c, b));
                size *= min / PokecubeMod.core.getConfig().scalefactor;
            }
        }
        genesSize.getExpressed().setValue(size);
    }

    @Override
    public void onGenesChanged()
    {
        IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
        Alleles allele = genes.getAlleles().get(SIZEGENE);
        if (allele != null) genesSize = allele;
        else getSize();
        allele = genes.getAlleles().get(IVSGENE);
        if (allele != null) genesIVs = allele;
        else getIVs();
        allele = genes.getAlleles().get(EVSGENE);
        if (allele != null) genesEVs = allele;
        else getEVs();
        allele = genes.getAlleles().get(MOVESGENE);
        if (allele != null) genesMoves = allele;
        else getMoves();
        allele = genes.getAlleles().get(NATUREGENE);
        if (allele != null) genesNature = allele;
        else getNature();
    }

    /** Use this for anything that does not change or need to be updated. */
    @Override
    public void writeSpawnData(ByteBuf data)
    {
        IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
        if (genes != null)
        {
            NBTTagList list = (NBTTagList) IMobGenetics.GENETICS_CAP.writeNBT(genes, null);
            PacketBuffer buffer = new PacketBuffer(data);
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setTag("g", list);
            buffer.writeCompoundTag(nbt);
        }
    }

    @Override
    public void readSpawnData(ByteBuf data)
    {
        PacketBuffer buffer = new PacketBuffer(data);
        try
        {
            NBTTagCompound tag = buffer.readCompoundTag();
            NBTTagList list = (NBTTagList) tag.getTag("g");
            IMobGenetics genes = getCapability(IMobGenetics.GENETICS_CAP, null);
            IMobGenetics.GENETICS_CAP.readNBT(genes, null, list);
            onGenesChanged();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbttagcompound)
    {
        super.readEntityFromNBT(nbttagcompound);
        onGenesChanged();
    }
}
