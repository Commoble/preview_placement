package net.commoble.preview_placement.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedColors.PerQuad;
import net.neoforged.neoforge.client.model.quad.BakedColors.PerVertex;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

public record PlacementPreviewRenderState(
	BlockPos placePos,
	List<MutableQuad> quads,
	QuadInstance quadInstance
) implements CustomGeometryRenderer
{
	public static PlacementPreviewRenderState extract(BlockAndTintGetter level, BlockPos placePos, BlockState state)
	{
		Minecraft mc = Minecraft.getInstance();
		ModelManager models = mc.getModelManager();
		
		BlockStateModel model = models.getBlockStateModelSet().get(state);
		RandomSource seededRandom = RandomSource.create();
		seededRandom.setSeed(state.getSeed(placePos));
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(level, placePos, state, seededRandom, parts);
		List<MutableQuad> quads = new ArrayList<>();
		Direction[] cullDirs = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
		double alpha = ClientProxy.CLIENTCONFIG.previewPlacementOpacity().get();
		for (BlockStateModelPart part : parts)
		{
			for (Direction dir : cullDirs)
			{
				for (BakedQuad quad : part.getQuads(dir))
				{
					MutableQuad mutaquad = new MutableQuad();
					mutaquad.setFrom(quad);
					MaterialInfo mat = quad.materialInfo();
					mutaquad.setSprite(mat.sprite(), ChunkSectionLayer.TRANSLUCENT, Sheets.translucentBlockItemSheet());
					mutaquad.setColor(getTranslucifiedColors(quad.bakedColors(), alpha));
					quads.add(mutaquad);
				}
			}
		}
		int blockLight = level.getBrightness(LightLayer.BLOCK, placePos);
		int skyLight = level.getBrightness(LightLayer.SKY, placePos);
		int packedLight = LightCoordsUtil.pack(blockLight, skyLight);
		QuadInstance quadInstance = new QuadInstance();
		quadInstance.setLightCoords(packedLight);
		
		return new PlacementPreviewRenderState(placePos, quads, quadInstance);
	}

	public boolean render(PoseStack poseStack, Vec3 cameraPos, SubmitNodeCollector collector)
	{
		BlockPos pos = this.placePos;
		
		poseStack.pushPose();
	
		// the current position of the matrix stack is the position of the player's
		// viewport (the head, essentially)
		// we want to move it to the correct position to render the block at
		double offsetX = pos.getX() - cameraPos.x();
		double offsetY = pos.getY() - cameraPos.y();
		double offsetZ = pos.getZ() - cameraPos.z();
		poseStack.translate(offsetX, offsetY, offsetZ);
		collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), this);
		
		poseStack.popPose();
		return false;
	}

	@Override
	public void render(Pose pose, VertexConsumer buffer)
	{
		for (MutableQuad quad : this.quads)
		{
			buffer.putMutableQuad(pose, quad, this.quadInstance);
		}
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