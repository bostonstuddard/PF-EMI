package dev.emi.emi.platform.forge;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.text.WordUtils;
import org.objectweb.asm.Type;

import com.google.common.collect.Lists;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.mixin.accessor.BrewingRecipeRegistryAccessor;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.recipe.EmiBrewingRecipe;
import dev.emi.emi.registry.EmiPluginContainer;
import dev.emi.emi.runtime.EmiLog;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.component.ComponentChanges;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.ModFileScanData;

public class EmiAgnosForge extends EmiAgnos {
	static {
		EmiAgnos.delegate = new EmiAgnosForge();
	}

	@Override
	protected boolean isForgeAgnos() {
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	protected String getModNameAgnos(String namespace) {
		if (namespace.equals("c")) {
			return "Common";
		}
		Optional<? extends ModContainer> container = ModList.get().getModContainerById(namespace);
		if (container.isPresent()) {
			return container.get().getModInfo().getDisplayName();
		}
		container = ModList.get().getModContainerById(namespace.replace('_', '-'));
		if (container.isPresent()) {
			return container.get().getModInfo().getDisplayName();
		}
		return WordUtils.capitalizeFully(namespace.replace('_', ' '));
	}

	@Override
	protected Path getConfigDirectoryAgnos() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	protected boolean isDevelopmentEnvironmentAgnos() {
		return !FMLLoader.isProduction();
	}

	@Override
	protected boolean isModLoadedAgnos(String id) {
		return ModList.get().isLoaded(id);
	}

	@Override
	protected List<String> getAllModNamesAgnos() {
		return ModList.get().getMods().stream().map(m -> m.getDisplayName()).toList();
	}

	@Override
	protected List<String> getModsWithPluginsAgnos() {
		List<String> mods = Lists.newArrayList();
		Type entrypointType = Type.getType(EmiEntrypoint.class);
		for (ModFileScanData data : ModList.get().getAllScanData()) {
			for (ModFileScanData.AnnotationData annot : data.getAnnotations()) {
				try {
					if (entrypointType.equals(annot.annotationType())) {
						mods.add(data.getIModInfoData().get(0).getMods().get(0).getModId());
					}
				} catch (Throwable t) {
					EmiLog.error("Exception locating EMI entrypoint:", t);
				}
			}
		}
		return mods;
	}

	@Override
	protected List<EmiPluginContainer> getPluginsAgnos() {
		List<EmiPluginContainer> containers = Lists.newArrayList();
		Type entrypointType = Type.getType(EmiEntrypoint.class);
		for (ModFileScanData data : ModList.get().getAllScanData()) {
			for (ModFileScanData.AnnotationData annot : data.getAnnotations()) {
				try {
					if (entrypointType.equals(annot.annotationType())) {
						Class<?> clazz = Class.forName(annot.memberName());
						if (EmiPlugin.class.isAssignableFrom(clazz)) {
							Class<? extends EmiPlugin> pluginClass = clazz.asSubclass(EmiPlugin.class);
							EmiPlugin plugin = pluginClass.getConstructor().newInstance();
							String id = data.getIModInfoData().get(0).getMods().get(0).getModId();
							containers.add(new EmiPluginContainer(plugin, id));
						} else {
							EmiLog.error("EmiEntrypoint " + annot.memberName() + " does not implement EmiPlugin");
						}
					}
				} catch (Throwable t) {
					EmiLog.error("Exception constructing entrypoint:", t);
				}
			}
		}
		return containers;
	}

	@Override
	protected void addBrewingRecipesAgnos(EmiRegistry registry) {
		World world = MinecraftClient.getInstance().world;
		if (world == null) {
			return;
		}
		ContextParameterMap paramMap = SlotDisplayContexts.createParameters(world);
		BrewingRecipeRegistry brewingRegistry = world.getBrewingRecipeRegistry();
		BrewingRecipeRegistryAccessor brewingRegistryAccess = (BrewingRecipeRegistryAccessor) brewingRegistry;

		for (Ingredient ingredient : brewingRegistryAccess.getPotionTypes()) {
			for (ItemStack stack : ingredient.toDisplay().getStacks(paramMap)) {
				String pid = EmiUtil.subId(stack.getItem());
				for (BrewingRecipeRegistry.Recipe<Potion> recipe : brewingRegistryAccess.getPotionRecipes()) {
					try {
						if (!recipe.ingredient().toDisplay().getStacks(paramMap).isEmpty()) {
							Identifier id = EmiPort.id("emi", "/brewing/" + pid
								+ "/" + EmiUtil.subId(recipe.ingredient().toDisplay().getStacks(paramMap).get(0).getItem())
								+ "/" + EmiUtil.subId(EmiPort.getPotionRegistry().getId(recipe.from().value()))
								+ "/" + EmiUtil.subId(EmiPort.getPotionRegistry().getId(recipe.to().value())));
							registry.addRecipe(new EmiBrewingRecipe(
								EmiStack.of(EmiPort.setPotion(stack.copy(), recipe.from().value())), EmiIngredient.of(recipe.ingredient()),
								EmiStack.of(EmiPort.setPotion(stack.copy(), recipe.to().value())), id));
						}
					} catch (Exception e) {
						EmiLog.error("Error registering brewing recipe", e);
					}
				}
			}
		}

		for (BrewingRecipeRegistry.Recipe<Item> recipe : brewingRegistryAccess.getItemRecipes()) {
			try {
				if (!recipe.ingredient().toDisplay().getStacks(paramMap).isEmpty()) {
					String gid = EmiUtil.subId(recipe.ingredient().toDisplay().getStacks(paramMap).get(0).getItem());
					String iid = EmiUtil.subId(recipe.from().value());
					String oid = EmiUtil.subId(recipe.to().value());
					Consumer<RegistryEntry<Potion>> potionRecipeGen = entry -> {
						Potion potion = entry.value();
						if (brewingRegistry.isBrewable(entry)) {
							Identifier id = EmiPort.id("emi", "/brewing/item/"
								+ EmiUtil.subId(entry.getKey().get().getValue()) + "/" + gid + "/" + iid + "/" + oid);
							registry.addRecipe(new EmiBrewingRecipe(
								EmiStack.of(EmiPort.setPotion(new ItemStack(recipe.from().value()), potion)), EmiIngredient.of(recipe.ingredient()),
								EmiStack.of(EmiPort.setPotion(new ItemStack(recipe.to().value()), potion)), id));
						}
					};
					if (recipe.from().value() instanceof PotionItem) {
						EmiPort.getPotionRegistry().streamEntries().forEach(potionRecipeGen);
					} else {
						potionRecipeGen.accept(Potions.AWKWARD);
					}
				}
			} catch (Exception e) {
				EmiLog.error("Error registering brewing recipe", e);
			}
		}

		// Forge patches the vanilla brewing registry with its modded IBrewingRecipe list.
		for (IBrewingRecipe ibr : brewingRegistry.getRecipes()) {
			try {
				if (ibr instanceof BrewingRecipe recipe) {
					List<ItemStack> ingredients = recipe.getIngredient().toDisplay().getStacks(paramMap);
					if (ingredients.isEmpty()) {
						continue;
					}
					for (ItemStack is : recipe.getInput().toDisplay().getStacks(paramMap)) {
						EmiStack input = EmiStack.of(is);
						EmiIngredient ingredient = EmiIngredient.of(recipe.getIngredient());
						EmiStack output = EmiStack.of(recipe.getOutput(is, ingredients.get(0)));
						Identifier id = EmiPort.id("emi", "/brewing/forge/"
							+ EmiUtil.subId(input.getId()) + "/"
							+ EmiUtil.subId(ingredient.getEmiStacks().get(0).getId()) + "/"
							+ EmiUtil.subId(output.getId()));
						registry.addRecipe(new EmiBrewingRecipe(input, ingredient, output, id));
					}
				}
			} catch (Exception e) {
				EmiLog.error("Error registering Forge brewing recipe", e);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<String> getAllModAuthorsAgnos() {
		return ModList.get().getMods().stream().flatMap(m -> {
			Optional<Object> opt = m.getConfig().getConfigElement("authors");
			if (opt.isPresent()) {
				Object obj = opt.get();
				if (obj instanceof String authors) {
					return Lists.newArrayList(authors.split("\\,")).stream().map(String::trim);
				} else if (obj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String) {
					return ((List<String>) list).stream();
				}
			}
			return Stream.empty();
		}).distinct().toList();
	}

	@Override
	protected List<TooltipComponent> getItemTooltipAgnos(ItemStack stack) {
		MinecraftClient client = MinecraftClient.getInstance();
		return ForgeHooksClient.gatherTooltipComponents(stack, Screen.getTooltipFromItem(client, stack), stack.getTooltipData(),
			0, Integer.MAX_VALUE, Integer.MAX_VALUE, client.textRenderer);
	}

	@Override
	protected Text getFluidNameAgnos(Fluid fluid, ComponentChanges componentChanges) {
		// Forge 57 still stores Forge FluidStack metadata separately from vanilla data components.
		// PF-EMI keeps the shared component-based stack API, so Forge-only fluid metadata is
		// represented by the underlying fluid for naming/rendering.
		return new FluidStack(fluid, 1000).getDisplayName();
	}

	@Override
	protected List<Text> getFluidTooltipAgnos(Fluid fluid, ComponentChanges componentChanges) {
		List<Text> tooltip = Lists.newArrayList();
		tooltip.add(getFluidName(fluid, componentChanges));
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.options.advancedItemTooltips) {
			tooltip.add(EmiPort.literal(EmiPort.getFluidRegistry().getId(fluid).toString()).formatted(Formatting.DARK_GRAY));
		}
		return tooltip;
	}

	@Override
	protected boolean isFloatyFluidAgnos(FluidEmiStack stack) {
		Fluid fluid = stack.getKeyOfType(Fluid.class);
		return fluid != null && fluid.getFluidType().isLighterThanAir();
	}

	@Override
	protected void renderFluidAgnos(FluidEmiStack stack, DrawContext draw, int x, int y, float delta,
			int xOff, int yOff, int width, int height) {
		Fluid fluid = stack.getKeyOfType(Fluid.class);
		if (fluid == null) {
			return;
		}
		IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
		Identifier texture = ext.getStillTexture();
		if (texture == null) {
			return;
		}
		int color = ext.getTintColor();
		MinecraftClient client = MinecraftClient.getInstance();
		Sprite sprite = client.getAtlasManager().getAtlasTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).getSprite(texture);
		EmiRenderHelper.drawTintedSprite(draw, sprite, color, x, y, xOff, yOff, width, height);
	}

	@Override
	protected EmiStack createFluidStackAgnos(Object object) {
		if (object instanceof FluidStack f) {
			return EmiStack.of(f.getFluid(), ComponentChanges.EMPTY, f.getAmount());
		}
		return EmiStack.EMPTY;
	}

	@Override
	protected boolean canBatchAgnos(ItemStack stack) {
		return false;
	}

	@Override
	protected Map<Item, Integer> getFuelMapAgnos() {
		Object2IntMap<Item> fuelMap = new Object2IntOpenHashMap<>();
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.getNetworkHandler() == null) {
			return fuelMap;
		}
		for (Item item : EmiPort.getItemRegistry()) {
			int time = item.getDefaultStack().getBurnTime(null);
			if (time > 0) {
				fuelMap.put(item, time);
			}
		}
		return fuelMap;
	}

	@Override
	protected boolean isEnchantableAgnos(ItemStack stack, Enchantment enchantment) {
		return stack.canApplyAtEnchantingTable(RegistryEntry.of(enchantment));
	}
}
