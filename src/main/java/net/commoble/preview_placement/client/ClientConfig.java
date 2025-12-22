package net.commoble.preview_placement.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record ClientConfig(
	ConfigValue<Boolean> showPlacementPreview,
	ConfigValue<Double> previewPlacementOpacity)
{	
	public static ClientConfig create(ModConfigSpec.Builder builder)
	{
		ConfigValue<Boolean> showPlacementPreview = builder
			.comment("Render preview of items specified in preview_placement/placement_preview asset folder before placing them")
			.translation("previewPlacement.showPlacementPreview")
			.define("showPlacementPreview", true);
		ConfigValue<Double> previewPlacementOpacity = builder
			.comment("Opacity of the render preview for models using the preview_placement:placement_preview model loader. Higher value = less transparent, lower = more transparent")
			.translation("placementPreview.previewPlacementOpacity")
			.defineInRange("previewPlacementOpacity", 0.5D, 0D, 1D);
		
		return new ClientConfig(showPlacementPreview, previewPlacementOpacity);
	}
}
