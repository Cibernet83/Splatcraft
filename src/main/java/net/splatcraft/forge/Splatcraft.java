package net.splatcraft.forge;

import java.util.Objects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.splatcraft.forge.client.handlers.ClientSetupHandler;
import net.splatcraft.forge.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.handlers.ScoreboardHandler;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftCommands;
import net.splatcraft.forge.registries.SplatcraftEntities;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftRegisties;
import net.splatcraft.forge.registries.SplatcraftStats;
import net.splatcraft.forge.registries.SplatcraftTileEntities;
import net.splatcraft.forge.worldgen.SplatcraftOreGen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Splatcraft.MODID)
public class Splatcraft {
    public static final String MODID = "splatcraft";
    public static final String MODNAME = "Splatcraft";
    public static String version;
    public static final Logger LOGGER = LogManager.getLogger(MODNAME);

    public Splatcraft() {
        for (IModInfo m : ModList.get().getMods()) { // Forge is stupid
            if (Objects.equals(m.getModId(), MODID) && m.getVersion() != null)
            {
                version = m.getVersion().toString();
                break;
            }
        }

        SplatcraftRegisties.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SplatcraftConfig.clientConfig);
        SplatcraftConfig.loadConfig(SplatcraftConfig.clientConfig, FMLPaths.CONFIGDIR.get().resolve(Splatcraft.MODID + "-client.toml").toString());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(FMLJavaModLoadingContext.get().getModEventBus());

        //addBuiltinPack("classic_weapons", Component.literal("Splatcraft - Classic Weapons"));

    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        SplatcraftPacketHandler.registerMessages();

        SplatcraftGameRules.registerGamerules();
        SplatcraftTags.register();
        SplatcraftStats.register();
        ScoreboardHandler.register();
        SplatcraftCommands.registerArguments();

        SplatcraftOreGen.registerOres();
        SplatcraftItems.postRegister();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        SplatcraftEntities.bindRenderers();
        SplatcraftKeyHandler.registerKeys();
        SplatcraftBlocks.setRenderLayers();
        SplatcraftTileEntities.bindTESR();

        event.enqueueWork(() ->
        {
            SplatcraftItems.registerModelProperties();
            ClientSetupHandler.bindScreenContainers();
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        SplatcraftGameRules.booleanRules.replaceAll((k, v) -> event.getServer().getGameRules().getBoolean(SplatcraftGameRules.getRuleFromIndex(k)));
        SplatcraftGameRules.intRules.replaceAll((k, v) -> event.getServer().getGameRules().getInt(SplatcraftGameRules.getRuleFromIndex(k)));
    }
}