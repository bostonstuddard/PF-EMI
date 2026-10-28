package dev.emi.emi.stack.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.stack.MobEffectEmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.GsonHelper;

/** Makes the internal effect entries safe to favorite and persist across reloads. */
public final class MobEffectEmiStackSerializer implements EmiIngredientSerializer<MobEffectEmiStack> {
	@Override
	public String getType() {
		return "emi:status_effect";
	}

	@Override
	public EmiIngredient deserialize(JsonElement element) {
		if (!element.isJsonObject()) {
			return EmiStack.EMPTY;
		}
		JsonObject json = element.getAsJsonObject();
		return BuiltInRegistries.MOB_EFFECT.getOptional(EmiPort.id(GsonHelper.getAsString(json, "id", "")))
			.<EmiIngredient>map(effect -> new MobEffectEmiStack(effect)
				.setAmount(GsonHelper.getAsLong(json, "amount", 1))
				.setChance(GsonHelper.getAsFloat(json, "chance", 1)))
			.orElse(EmiStack.EMPTY);
	}

	@Override
	public JsonElement serialize(MobEffectEmiStack stack) {
		JsonObject json = new JsonObject();
		json.addProperty("type", getType());
		json.addProperty("id", stack.getId().toString());
		if (stack.getAmount() != 1) {
			json.addProperty("amount", stack.getAmount());
		}
		if (stack.getChance() != 1) {
			json.addProperty("chance", stack.getChance());
		}
		return json;
	}
}
