package net.commoble.preview_placement.client;

import org.jspecify.annotations.Nullable;

import net.commoble.preview_placement.PreviewPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@Mod(value=PreviewPlacement.MODID, dist=Dist.CLIENT)
@EventBusSubscriber(modid=PreviewPlacement.MODID, value=Dist.CLIENT)
public class ClientProxy
{
	public static final ClientConfig CLIENTCONFIG = PreviewPlacement.config(PreviewPlacement.MODID, ModConfig.Type.CLIENT, ClientConfig::create);

	private static final ContextKey<PlacementPreviewRenderState> PREVIEW_STATE_KEY = new ContextKey<>(PreviewPlacement.id(PreviewPlacement.MODID));
	
	@SubscribeEvent
	public static void onExtractLevelRenderState(ExtractLevelRenderStateEvent event)
	{
		if (ClientProxy.CLIENTCONFIG.showPlacementPreview().get())
		{
			Minecraft mc = Minecraft.getInstance();
			LocalPlayer player = mc.player;
			BlockAndTintGetter world = mc.level;
			if (world != null && player != null && mc.hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() != HitResult.Type.MISS)
			{
				InteractionHand hand = player.getUsedItemHand();
				ItemStack stack = player.getItemInHand(hand);
				Item item = stack.getItem();
				if (item instanceof BlockItem blockItem && stack.is(PreviewPlacement.PLACEMENT_PREVIEW_TAG))
				{
					Block block = blockItem.getBlock();
					Direction directionAwayFromTargetedBlock = blockHitResult.getDirection();
					BlockPos placePos = blockHitResult.getBlockPos().relative(directionAwayFromTargetedBlock);
					
					BlockState existingState = world.getBlockState(placePos);
					if (existingState.isAir() || existingState.canBeReplaced())
					{
						@Nullable BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, blockHitResult));
						if (state != null)
						{
							event.getRenderState().setRenderData(PREVIEW_STATE_KEY, PlacementPreviewRenderState.extract(world, placePos, state));
						}					
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event)
	{
		LevelRenderState levelRenderState = event.getLevelRenderState();
		@Nullable PlacementPreviewRenderState previewState = levelRenderState.getRenderData(PREVIEW_STATE_KEY);
		if (previewState != null)
		{
			previewState.render(event.getPoseStack(), levelRenderState.cameraRenderState.pos, event.getSubmitNodeCollector());
		}
	}
}
