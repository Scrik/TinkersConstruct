package mods.tinker.tconstruct.items.tools;

import mods.tinker.tconstruct.common.TContent;
import mods.tinker.tconstruct.library.ActiveToolMod;
import mods.tinker.tconstruct.library.TConstructRegistry;
import mods.tinker.tconstruct.library.tools.AbilityHelper;
import mods.tinker.tconstruct.library.tools.HarvestTool;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class Excavator extends HarvestTool
{
    public Excavator(int itemID)
    {
        super(itemID, 2);
        this.setUnlocalizedName("InfiTool.Excavator");
    }

    @Override
    protected Material[] getEffectiveMaterials ()
    {
        return materials;
    }

    @Override
    protected String getHarvestType ()
    {
        return "shovel";
    }

    static Material[] materials = { Material.grass, Material.ground, Material.sand, Material.snow, Material.craftedSnow, Material.clay };

    @Override
    public Item getHeadItem ()
    {
        return TContent.excavatorHead;
    }

    @Override
    public Item getHandleItem ()
    {
        return TContent.toughRod;
    }

    @Override
    public Item getAccessoryItem ()
    {
        return TContent.toughBinding;
    }

    @Override
    public Item getExtraItem ()
    {
        return TContent.toughRod;
    }

    @Override
    public float getRepairCost ()
    {
        return 4.0f;
    }

    public float getDurabilityModifier ()
    {
        return 2.75f;
    }

    @Override
    public int durabilityTypeAccessory ()
    {
        return 1;
    }

    @Override
    public int durabilityTypeExtra ()
    {
        return 1;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderPasses (int metadata)
    {
        return 10;
    }

    @Override
    public int getPartAmount ()
    {
        return 4;
    }

    @Override
    public String getIconSuffix (int partType)
    {
        switch (partType)
        {
        case 0:
            return "_excavator_head";
        case 1:
            return "_excavator_head_broken";
        case 2:
            return "_excavator_handle";
        case 3:
            return "_excavator_binding";
        case 4:
            return "_excavator_grip";
        default:
            return "";
        }
    }

    @Override
    public String getEffectSuffix ()
    {
        return "_excavator_effect";
    }

    @Override
    public String getDefaultFolder ()
    {
        return "excavator";
    }

    /* Excavator Specific */

    @Override
    public boolean onBlockStartBreak (ItemStack stack, int x, int y, int z, EntityPlayer player)
    {
        World world = player.worldObj;
        int blockID = 0;
        int meta = 0;
        if (!stack.hasTagCompound())
            return false;

        MovingObjectPosition mop = AbilityHelper.raytraceFromEntity(world, player, true, 6);
        if (mop == null)
            return super.onBlockStartBreak(stack, x, y, z, player);
        
        int xRange = 1;
        int yRange = 1;
        int zRange = 1;
        switch (mop.sideHit)
        {
        case 0:
        case 1:
            yRange = 0;
            break;
        case 2:
        case 3:
            zRange = 0;
            break;
        case 4:
        case 5:
            xRange = 0;
            break;
        }
        NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
        for (int xPos = x - xRange; xPos <= x + xRange; xPos++)
        {
            for (int yPos = y - yRange; yPos <= y + yRange; yPos++)
            {
                for (int zPos = z - zRange; zPos <= z + zRange; zPos++)
                {
                    if (!(tags.getBoolean("Broken")))
                    {
                        boolean cancelHarvest = false;
                        for (ActiveToolMod mod : TConstructRegistry.activeModifiers)
                        {
                            if (mod.beforeBlockBreak(this, stack, xPos, yPos, zPos, player))
                                cancelHarvest = true;
                        }

                        if (!cancelHarvest)
                        {
                            int localblockID = world.getBlockId(xPos, yPos, zPos);
                            Block block = Block.blocksList[localblockID];
                            if (block != null)
                            {
                                for (int iter = 0; iter < materials.length; iter++)
                                {
                                    if (materials[iter] == block.blockMaterial)
                                    {
                                        meta = world.getBlockMetadata(xPos, yPos, zPos);
                                        world.setBlockToAir(xPos, yPos, zPos);
                                        if (!player.capabilities.isCreativeMode)
                                        {
                                            block.harvestBlock(world, player, xPos, yPos, zPos, meta);
                                            onBlockDestroyed(stack, world, localblockID, xPos, yPos, zPos, player);
                                        }
                                        blockID = localblockID;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!world.isRemote)
            world.playAuxSFX(2001, x, y, z, blockID + (meta << 12));
        return super.onBlockStartBreak(stack, x, y, z, player);
    }

    @Override
    public float getStrVsBlock (ItemStack stack, Block block, int meta)
    {
        if (!stack.hasTagCompound())
            return 1.0f;

        NBTTagCompound tags = stack.getTagCompound().getCompoundTag("InfiTool");
        if (tags.getBoolean("Broken"))
            return 0.1f;

        Material[] materials = getEffectiveMaterials();
        for (int i = 0; i < materials.length; i++)
        {
            if (materials[i] == block.blockMaterial)
            {
                float speed = tags.getInteger("MiningSpeed");
                speed /= 200f;
                int hlvl = MinecraftForge.getBlockHarvestLevel(block, meta, getHarvestType());
                int durability = tags.getInteger("Damage");

                float shoddy = tags.getFloat("Shoddy");
                speed += shoddy * durability / 100f;

                if (hlvl <= tags.getInteger("HarvestLevel"))
                    return speed;
                return 0.1f;
            }
        }
        return super.getStrVsBlock(stack, block, meta);
    }
}