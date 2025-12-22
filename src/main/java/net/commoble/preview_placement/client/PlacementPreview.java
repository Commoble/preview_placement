package net.commoble.preview_placement.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.CustomBlockOutlineRenderer;

public record PlacementPreview(ItemStack stack, ModelState rotation)
{

	public CustomBlockOutlineRenderer extractRenderState(Level level, BlockPos placePos, Player player)
	{
		Minecraft mc = Minecraft.getInstance();
		ItemModelResolver resolver = mc.getItemModelResolver();
		ItemStackRenderState itemState = new ItemStackRenderState();
		resolver.updateForTopItem(itemState, stack, ItemDisplayContext.NONE, level, player, player.getId());
		int blockLight = level.getBrightness(LightLayer.BLOCK, placePos);
		int skyLight = level.getBrightness(LightLayer.SKY, placePos);
		int packedLight = LightTexture.pack(blockLight, skyLight);
		return new PlacementPreviewRenderState(itemState, placePos, this.rotation, packedLight);
	}
	
	public static record PlacementPreviewRenderState(
		ItemStackRenderState itemState,
		BlockPos placePos,
		ModelState rotation,
		int packedLight
		) implements CustomBlockOutlineRenderer
	{

		@Override
		public boolean render(BlockOutlineRenderState renderState, BufferSource buffer, PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState)
		{
			BlockPos pos = this.placePos;
			Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
			
			poseStack.pushPose();
		
			// the current position of the matrix stack is the position of the player's
			// viewport (the head, essentially)
			// we want to move it to the correct position to render the block at
			double offsetX = pos.getX() - cameraPos.x();
			double offsetY = pos.getY() - cameraPos.y();
			double offsetZ = pos.getZ() - cameraPos.z();
			poseStack.translate(offsetX, offsetY, offsetZ);
			// item renderer renders the center of the item model at 0,0,0 in the cube
			poseStack.translate(0.5D,0.5D,0.5D); 
			poseStack.mulPose(rotation.transformation().getMatrix());
			// for whatever reason, we need to untranslate after we rotate the block...
			// BER doesn't need this, why do we need it here?
//			poseStack.translate(-0.5D,-0.5D,-0.5D);
			
			SubmitNodeCollector collector = Minecraft.getInstance().levelRenderer.submitNodeStorage;
			this.itemState.submit(poseStack, collector, this.packedLight, OverlayTexture.NO_OVERLAY, 0);
			
			poseStack.popPose();
			return false;
		}
		
	}
	
}