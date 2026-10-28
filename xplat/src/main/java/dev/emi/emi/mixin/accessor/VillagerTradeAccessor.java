package dev.emi.emi.mixin.accessor;

import java.util.Optional;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.TradeCost;
import net.minecraft.world.item.trading.VillagerTrade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only data access to datapack-defined 26.3 trades for EMI presentation. */
@Mixin(VillagerTrade.class)
public interface VillagerTradeAccessor {
																		@Accessor("wants")
																							TradeCost emi$getWants();

																									@Accessor("additionalWants")
																																						Optional<TradeCost> emi$getAdditionalWants();

																		@Accessor("gives")
																													ItemStackTemplate emi$getGives();
}
