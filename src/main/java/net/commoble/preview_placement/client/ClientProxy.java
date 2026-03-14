package net.commoble.preview_placement.client;

import org.jspecify.annotations.Nullable;

import net.commoble.preview_placement.PreviewPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@Mod(value=PreviewPlacement.MODID, dist=Dist.CLIENT)
@EventBusSubscriber(modid=PreviewPlacement.MODID, value=Dist.CLIENT)
public class ClientProxy
{
	public static final ClientConfig CLIENTCONFIG = PreviewPlacement.config(PreviewPlacement.MODID, ModConfig.Type.CLIENT, ClientConfig::create);

	@SubscribeEvent
	public static void onRegisterModelLoaders(ModelEvent.RegisterLoaders event)
	{
		event.register(PreviewPlacement.id("placement_preview"), PlacementPreviewModelLoader.INSTANCE);
	}
	
	@SubscribeEvent
	public static void onAddClientReloadListeners(AddClientReloadListenersEvent event)
	{
		event.addListener(PreviewPlacement.id("placement_preview"), new PlacementPreviewReloadListener());
	}
	
	@SubscribeEvent
	public static void onHighlightBlock(ExtractBlockOutlineRenderStateEvent event)
	{
		if (ClientProxy.CLIENTCONFIG.showPlacementPreview().get())
		{
			LocalPlayer player = Minecraft.getInstance().player;
			if (player != null)
			{
				InteractionHand hand = player.getUsedItemHand();
				ItemStack stack = player.getItemInHand(hand);
				Item item = stack.getItem();
				if (item instanceof BlockItem blockItem && PlacementPreviewReloadListener.doesItemHavePreview(blockItem))
				{
					Block block = blockItem.getBlock();
					Level world = player.level();
					BlockHitResult rayTrace = event.getHitResult();
					Direction directionAwayFromTargetedBlock = rayTrace.getDirection();
					BlockPos placePos = rayTrace.getBlockPos().relative(directionAwayFromTargetedBlock);
					
					BlockState existingState = world.getBlockState(placePos);
					if (existingState.isAir() || existingState.canBeReplaced())
					{
						BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, rayTrace));
						if (state != null)
						{
							@Nullable PlacementPreview preview = PlacementPreviewReloadListener.getPlacementPreview(state);
							if (preview != null)
							{
								event.addCustomRenderer(preview.extractRenderState(world,placePos,player));
							}	
						}					
					}
				}
			}
		}
	}
}
