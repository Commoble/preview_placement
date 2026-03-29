package net.commoble.preview_placement;

import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class PreviewPlacement
{
	public static final String MODID = "preview_placement";
	
	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
	
	public static <T> T config(
		final String modid,
		final ModConfig.Type configType,
		final Function<ModConfigSpec.Builder, T> configFactory)
	{
		final var mod = ModList.get().getModContainerById(modid).get();
		final org.apache.commons.lang3.tuple.Pair<T, ModConfigSpec> entry = new ModConfigSpec.Builder()
			.configure(configFactory);
		final T config = entry.getLeft();
		final ModConfigSpec spec = entry.getRight();
		mod.registerConfig(configType,spec);
		
		return config;
	}
}
