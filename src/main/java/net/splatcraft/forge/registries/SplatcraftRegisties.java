package net.splatcraft.forge.registries;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.splatcraft.forge.worldgen.SplatcraftOreGen;

public class SplatcraftRegisties
{
	public static void register()
	{
		SplatcraftBlocks.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftAttributes.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftItems.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftItemGroups.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftTileEntities.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftTileEntities.CONTAINER_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftOreGen.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftLoot.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftParticleTypes.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
		SplatcraftCommands.ARGUMENT_REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
	}
}
