package dev.emi.emi.mixin.accessor;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import dev.emi.emi.EmiPort;

/**
 * 26.3 fallback for vanilla axe block transformations. The AxeItem/STRIPPABLES
 * mixin target was removed in 26.3 in favor of datapack BlockTransformers.
 * This registry lookup covers ordinary vanilla/modded stripped wood pairs.
 */
public final class AxeItemAccessor {
    private AxeItemAccessor() {}

    public static Map<Block, Block> getStrippedBlocks() {
        Map<Block, Block> out = new HashMap<>();
        for (Block input : EmiPort.getBlockRegistry()) {
            Identifier id = EmiPort.getBlockRegistry().getKey(input);
            if (id == null || id.getPath().startsWith("stripped_")) continue;
            Block stripped = EmiPort.getBlockRegistry().getValue(
                EmiPort.id(id.getNamespace(), "stripped_" + id.getPath()));
            if (stripped != null && stripped != Blocks.AIR && stripped != input) {
                out.put(input, stripped);
            }
        }
        return out;
    }
}
