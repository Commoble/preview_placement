package net.commoble.preview_placement.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;

import net.commoble.preview_placement.JsonDataProvider;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class PlacementPreviewDefinition
{
	private PlacementPreviewDefinition() {}
	
	public static final Codec<Map<Block,Map<String,Variant>>> CODEC = Codec.unboundedMap(
		BuiltInRegistries.BLOCK.byNameCodec(),
		Codec.unboundedMap(
			Codec.STRING, Variant.CODEC));
	
	/**
	 * Helper to add a dataprovider for placement preview jsons
	 * @param event GatherDataEvent
	 * @param variants Map of blockstate variants by item id.
	 * Use {@link PlacementPreviewDefinition#singleVariant} or {@link PlacementPreviewDefinition#variants} to create these
	 */
	public static void addDataProvider(GatherDataEvent event, Map<Identifier, Map<Block, Map<String,Variant>>> variants)
	{
		DataGenerator generator = event.getGenerator();
		generator.addProvider(true, JsonDataProvider.create(event.getLookupProvider(), generator.getPackOutput(), generator, PackOutput.Target.RESOURCE_PACK, "preview_placement/placement_preview", CODEC, variants));
	}
	
	/**
	 * Creates and returns a PlacementPreview for a variants-type blockstate json which assigns one model to all states.
	 * @param variant Variant to use, e.g. {@snippet :
	 * {
	 *   "model": "foo:bar",
	 *   "x": 90,
	 *   "y": 180
	 * }
	 * }see {@link PlacementPreviewDefinition#variant} or its overloads for helpers to create variants
	 * @return PlacementPreviewDefinition for a json of the form {@snippet :
	 * {
	 *   "": {"model": "foo:bar"}
	 * }}
	 */
	public static Map<String,Variant> singleVariant(Variant variant)
	{
		return variants(variants -> variants.addMultiPropertyVariant(propertyValues -> {}, variant));
	}
	
	/**
	 * Builds and returns a PlacementPreviewDefinition for a placement preview json with multiple variants.
	 * @param variantsBuilder Variants builder which cases can be added too, see Variants javadoc for details
	 * @return PlacementPreviewDefinition for a placement preview json of the form {@snippet :
	 * {
	 *  "powered=false": {"model": "foo:bar"},
	 *  "powered=true": {"model": "foo:bar_powered"}
	 * }}
	 */
	public static Map<String,Variant> variants(Consumer<Variants> variantsBuilder)
	{
		Variants variants = Variants.builder();
		variantsBuilder.accept(variants);
		return variants.variants.entrySet().stream().collect(Collectors.toMap(
			entry -> String.join(",", entry.getKey().stream().map(PropertyValue::toString).toList()),
			entry -> entry.getValue()));
	}
	
	/**
	 * {@return new Varient with no rotation}
	 * @param item Identifier of an *item json*, e.g. "minecraft:dirt"
	 */
	public static Variant variant(Identifier item)
	{
		return variant(item, Quadrant.R0, Quadrant.R0, Quadrant.R0);
	}
	
	/**
	 * {@return new Variant with specified rotation}
	 * @param item Identifier of an *item json*, e.g. "minecraft:dirt"
	 * @param x x-rotation to apply to the model
	 * @param y y-rotation to apply to the model
	 */
	public static Variant variant(Identifier item, Quadrant x, Quadrant y)
	{
		return variant(item, x, y, Quadrant.R0);
	}
	
	/**
	 * {@return new Variant with specified rotation}
	 * @param item Identifier of an *item json*, e.g. "minecraft:dirt"
	 * @param x x-rotation to apply to the model
	 * @param y y-rotation to apply to the model
	 * @param z z-rotation to apply to the model
	 */
	public static Variant variant(Identifier item, Quadrant x, Quadrant y, Quadrant z)
	{
		return new Variant(item)
			.withXRot(x)
			.withYRot(y)
			.withZRot(z);
	}

	/**
	 * Represents a structure similar to a "variants" block in a blockstate json..
	 * Can be used as a builder like so: {@snippet :
	 * PlacementPreviewDefinition.variants(variants -> variants
	 *   .addVariant(BlockStateProperties.POWERED, false, PlacementPreviewDefinition.variant(Identifier.fromNamespaceAndPath("foo, "bar")))
	 *   .addVariant(propertyValues -> propertyValues
	 *     .addPropertyValue(BlockStateProperties.POWERED, true)
	 *     .addPropertyValue(BlockStateProperties.LIT, false),
	 *     PlacementPreviewDefinition.variant(Identifier.fromNamespaceAndPath("foo, "bar_powered"))));
	 * }
	 * Which results in the placement preview json {@snippet :
	 * {
	 *   "powered=false": {"model": "foo:bar"},
	 *   "powered=true,lit=false": {"model": "foo:bar_powered"}
	 * }
	 * }
	 * @param variants Map of blockstate property predicates to models
	 */
	public static record Variants(Map<List<PropertyValue<?>>, Variant> variants)
	{
		private static Variants builder()
		{
			return new Variants(new HashMap<>());
		}
		
		/**
		 * Adds a single-property variant to your placement preview json., e.g. "powered=true": {"model": "foo:bar"}
		 * @param <T> type of the blockstate property to filter blockstates by
		 * @param property blockstate Property to filter blockstates by, e.g. {@link BlockStateProperties#POWERED}
		 * @param value value of the blockstate Property to filter blockstates by, e.g. true
		 * @param variant Variant to assign to blockstates which have that property value,
		 * see {@link PlacementPreviewDefinition#variant} or its overloads to help create the variant
		 * @return this
		 */
		public <T extends Comparable<T>> Variants addVariant(Property<T> property, T value, Variant variant)
		{
			this.variants.put(List.of(new PropertyValue<>(property,value)), variant);
			return this;
		}
		
		/**
		 * Adds a multi-property variant to your placement preview json, e.g. "powered=true,lit=false": {"model": "foo:bar"}
		 * @param propertyValuesBuilder PropertyValueList builder to add property values to, see its javadoc for details
		 * @param variant Variant to assign to blockstates which have all specified property values,
		 * see {@link PlacementPreviewDefinition#variant} or its overloads to help create the variant
		 * @return this
		 */
		public Variants addMultiPropertyVariant(Consumer<PropertyValueList> propertyValuesBuilder, Variant variant)
		{
			PropertyValueList list = PropertyValueList.builder();
			propertyValuesBuilder.accept(list);
			this.variants.put(list.propertyValues, variant);
			return this;
		}
	}
	
	/**
	 * Component of Variants definitions, representing a property-value entry, e.g. "powered=true"
	 * 
	 * @param property Property of a blockstate.
	 * @param value value of a blockstate Property.
	 */
	private static record PropertyValue<T extends Comparable<T>>(Property<T> property, T value)
	{		
		@Override
		public String toString()
		{
			return this.property.getName() + "=" + this.property.getName(value);
		}
	}
	
	/**
	 * Component of Variants definitions, representing one or more property-value entries, e.g. "powered=true,lit=false"
	 */
	public static record PropertyValueList(List<PropertyValue<?>> propertyValues)
	{
		private static PropertyValueList builder()
		{
			return new PropertyValueList(new ArrayList<>());
		}
		
		/**
		 * Adds a blockstate property value to this variant's state filter, e.g. "powered=true"
		 * @param <T> type of the blockstate property to filter blockstates by
		 * @param property blockstate Property to filter blockstates by, e.g. {@link BlockStateProperties#POWERED}
		 * @param value value of the blockstate Property to filter blockstates by, e.g. true
		 * @return this
		 */
		public <T extends Comparable<T>> PropertyValueList addPropertyValue(Property<T> property, T value)
		{
			this.propertyValues.add(new PropertyValue<>(property, value));
			return this;
		}
	}
}
