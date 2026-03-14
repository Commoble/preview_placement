package net.commoble.preview_placement.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantSelector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PlacementPreviewReloadListener extends SimpleJsonResourceReloadListener<Map<Block, Map<String,Variant>>>
{
	private static final Logger LOGGER = LogUtils.getLogger();
	private static Map<BlockState, Variant> data = new HashMap<>();
	private static Map<Variant, PlacementPreview> cache = new HashMap<>();
	private static Set<Item> relevantItems = new HashSet<>();
	
	protected PlacementPreviewReloadListener()
	{
		super(PlacementPreviewDefinition.CODEC, FileToIdConverter.json("preview_placement/placement_preview"));
	}
	
	public static boolean doesItemHavePreview(Item item)
	{
		return relevantItems.contains(item);
	}
	
	public static @Nullable PlacementPreview getPlacementPreview(BlockState state)
	{
		@Nullable Variant variant = data.get(state);
		if (variant == null)
			return null;
		@Nullable PlacementPreview preview = cache.get(variant);
		if (preview == null)
		{
			ItemStack stack = new ItemStack(Items.STICK);
			stack.set(DataComponents.ITEM_MODEL, variant.modelLocation());
			ModelState rotation = variant.modelState().asModelState(); 
			preview = new PlacementPreview(stack, rotation);
			cache.put(variant, preview);
		}
		return preview;
	}

	@Override
	protected void apply(@Nullable Map<Identifier, Map<Block, Map<String,Variant>>> data, ResourceManager resourceManager, ProfilerFiller profiler)
	{
		applyWithFixForEclipse(data == null ? Map.of() : data, resourceManager, profiler);
	}
	
	protected void applyWithFixForEclipse(Map<Identifier, Map<Block, Map<String,Variant>>> data, ResourceManager resourceManager, ProfilerFiller profiler)
	{
		Map<BlockState, Variant> results = new HashMap<>();
		Set<Item> relevantItems = new HashSet<>();
		loopOverItems:
		for (var entry : data.entrySet())
		{
			Map<BlockState,Variant> stateVariants = new HashMap<>();
			Identifier itemId = entry.getKey();
			Item item = BuiltInRegistries.ITEM.getValue(itemId);
			if (item == Items.AIR)
			{
				LOGGER.error("Found placement preview file for invalid item id: {}", itemId);
				continue;
			}
			if (!(item instanceof BlockItem))
			{
				LOGGER.error("Found placement preview file for unsupported item: {} (only BlockItems are currently supported)", itemId);
				continue;
			}
			loopOverBlocks:
			for (var blockVariants : entry.getValue().entrySet())
			{
				Block block = blockVariants.getKey();
				Map<String,Variant> variants = blockVariants.getValue();
				var stateDefinition = block.getStateDefinition();
				for (var variantSelector : variants.entrySet())
				{
					String selector = variantSelector.getKey();
					Variant variant = variantSelector.getValue();
					try {
						var predicate = VariantSelector.predicate(stateDefinition, selector);
						for (BlockState state : stateDefinition.getPossibleStates())
						{
							if (predicate.test(state))
							{
								@Nullable Variant existingVariant = stateVariants.put(state, variant);
								if (existingVariant != null)
								{
									LOGGER.warn("Overlapping placement preview definition for state {}", state);
									continue loopOverItems;
								}
							}
						}
					}
					catch (Exception e)
					{
						LOGGER.error("Exception loading placement preview for item {}, block {}, variant {}: {}", item, block, selector, e.getMessage());
						continue loopOverBlocks;
					}
				}
			}
			// if no errors, keep all declared states for this item
			results.putAll(stateVariants);
			relevantItems.add(item);
		}
		PlacementPreviewReloadListener.data = results;
		PlacementPreviewReloadListener.cache = new HashMap<>();
		PlacementPreviewReloadListener.relevantItems = relevantItems;
	}
}
