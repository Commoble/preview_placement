package net.commoble.preview_placement.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.ExtendedUnbakedGeometry;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedColors.PerQuad;
import net.neoforged.neoforge.client.model.quad.BakedColors.PerVertex;

/**
 * 
 */
public enum PlacementPreviewModelLoader implements UnbakedModelLoader<CuboidModel>
{
	INSTANCE;

	@Override
	public CuboidModel read(JsonObject modelContents, JsonDeserializationContext context)
	{
		// we use the vanilla model loader to parse everything
		CuboidModel baseModel = context.deserialize(modelContents.get("model"), CuboidModel.class);
        PlacementPreviewModelGeometry geometry = new PlacementPreviewModelGeometry(baseModel);
        return new CuboidModel(
			geometry,
			baseModel.guiLight(),
			baseModel.ambientOcclusion(),
			baseModel.transforms(),
			baseModel.textureSlots(),
			baseModel.parent(),
			baseModel.rootTransform(),
			baseModel.partVisibility(),
			baseModel.itemLayerFaceData());
	}
	
	public static record PlacementPreviewModelGeometry(UnbakedModel baseModel) implements ExtendedUnbakedGeometry
	{

		@Override
		public QuadCollection bake(TextureSlots textureSlots, ModelBaker baker, ModelState state, ModelDebugName debugName, ContextMap additionalProperties)
		{
			ResolvedModel resolvedModel = baker.resolveInlineModel(this.baseModel, debugName);
			QuadCollection baseQuads = resolvedModel.bakeTopGeometry(textureSlots, baker, state);
	        QuadCollection.Builder builder = new QuadCollection.Builder();
	        
	        double alpha = ClientProxy.CLIENTCONFIG.previewPlacementOpacity().get();

			for (Direction dir : Direction.values())
			{ 
				for (BakedQuad quad : baseQuads.getQuads(dir))
				{
					builder.addCulledFace(dir, getTranslucifiedQuad(quad, alpha));
				}
			}
			for (BakedQuad quad : baseQuads.getQuads(null))
			{
				builder.addUnculledFace(getTranslucifiedQuad(quad, alpha));
			}
			return builder.build();
		}

		private static BakedQuad getTranslucifiedQuad(BakedQuad baseQuad, double alpha)
		{
			BakedColors newColors = getTranslucifiedColors(baseQuad.bakedColors(), alpha);
			MaterialInfo baseMat = baseQuad.materialInfo();
			return new BakedQuad(
				baseQuad.position0(),
				baseQuad.position1(),
				baseQuad.position2(),
				baseQuad.position3(),
				baseQuad.packedUV0(),
				baseQuad.packedUV1(),
				baseQuad.packedUV2(),
				baseQuad.packedUV3(),
				baseQuad.direction(),
				new MaterialInfo(
					baseMat.sprite(),
					ChunkSectionLayer.TRANSLUCENT,
					Sheets.translucentBlockItemSheet(),
					baseMat.tintIndex(),
					baseMat.shade(),
					baseMat.lightEmission(),
					baseMat.ambientOcclusion()),
				baseQuad.bakedNormals(),
				newColors);
		}

		private static BakedColors getTranslucifiedColors(BakedColors baseColors, double alpha)
		{
			return switch (baseColors) {
				case PerQuad(int argb) -> new PerQuad(translucifyArgb(argb,alpha));
				case PerVertex(int argb0, int argb1, int argb2, int argb3) -> new PerVertex(
					translucifyArgb(argb0, alpha),
					translucifyArgb(argb1, alpha),
					translucifyArgb(argb2, alpha),
					translucifyArgb(argb3, alpha));
			};
		}
		
		private static int translucifyArgb(int argb, double alphaMultiplier)
		{
			int alpha = (int)((double)ARGB.alpha(argb) * alphaMultiplier);
			return ARGB.color(alpha, ARGB.red(argb), ARGB.green(argb), ARGB.blue(argb));
		}
	}
}
