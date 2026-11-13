package dev.emi.emi.mixin.accessor;

import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to vanilla 26.2 trade templates. */
@Mixin(VillagerTrade.class)
public interface VillagerTradeAccessor {
    @Accessor("wants") TradeCost emi$getWants();
    @Accessor("additionalWants") Optional<TradeCost> emi$getAdditionalWants();
    @Accessor("gives") ItemStackTemplate emi$getGives();
}
