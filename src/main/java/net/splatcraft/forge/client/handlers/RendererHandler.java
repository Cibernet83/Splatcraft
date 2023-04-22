package net.splatcraft.forge.client.handlers;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.obfuscate.client.event.RenderItemEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BreakableBlock;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameplateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.layer.InkAccessoryLayer;
import net.splatcraft.forge.client.layer.InkOverlayLayer;
import net.splatcraft.forge.client.renderer.InkSquidRenderer;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.IPlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.entities.subs.AbstractSubWeaponEntity;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.ChargerItem;
import net.splatcraft.forge.items.weapons.SubWeaponItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.ClientUtils;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.ArrayList;
import java.util.List;

import static net.splatcraft.forge.items.weapons.WeaponBaseItem.enoughInk;

//@Mod.EventBusSubscriber(modid = Splatcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class RendererHandler {
    @Deprecated
    public static final RenderState.TransparencyState TRANSLUCENT_TRANSPARENCY = new RenderState.TransparencyState("translucent_transparency", () ->
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () ->
    {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });
    private static final ResourceLocation WIDGETS = new ResourceLocation(Splatcraft.MODID, "textures/gui/widgets.png");
    private static InkSquidRenderer squidRenderer = null;
    private static final List<LivingRenderer> hasCustomLayers = new ArrayList<>();
    private static float tickTime = 0;
    private static float oldCooldown = 0;
    private static int squidTime = 0;
    private static float prevInkPctg = 0;
    private static float inkFlash = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerRender(RenderPlayerEvent event)
    {
        PlayerEntity player = event.getPlayer();
        if (player.isSpectator()) return;

        if (PlayerInfoCapability.isSquid(player))
        {
            event.setCanceled(true);
            if (squidRenderer == null)
                squidRenderer = new InkSquidRenderer(event.getRenderer().getDispatcher());
            if (!InkBlockUtils.canSquidHide(player)) {
                squidRenderer.render(player, player.yHeadRot, event.getPartialRenderTick(), event.getMatrixStack(), event.getBuffers(), event.getLight());
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.RenderLivingEvent.Post<>(player, squidRenderer, event.getPartialRenderTick(), event.getMatrixStack(), event.getBuffers(), event.getLight()));
            } else event.getRenderer().getDispatcher().setRenderShadow(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingRenderer(RenderLivingEvent.Pre<?, ?> event)
    {
        if(!hasCustomLayers.contains(event.getRenderer()))
        {
            if(event.getRenderer() instanceof PlayerRenderer)
                ((PlayerRenderer)event.getRenderer()).addLayer(new InkAccessoryLayer((PlayerRenderer)event.getRenderer()));
            event.getRenderer().addLayer(new InkOverlayLayer(event.getRenderer()));

            hasCustomLayers.add(event.getRenderer());
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if (PlayerCooldown.hasPlayerCooldown(player) && !player.isSpectator()) {
            player.inventory.selected = PlayerCooldown.getPlayerCooldown(player).getSlotIndex();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerRenderPost(RenderPlayerEvent.Post event)
    {
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event)
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if (PlayerInfoCapability.isSquid(player))
        {
            event.setCanceled(true);
            return;
        }



        if (PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player).getHand().equals(event.getHand()))
        {
            PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
            float time = (float) cooldown.getTime();
            float maxTime = (float) cooldown.getMaxTime();
            if (time != oldCooldown)
            {
                oldCooldown = time;
                tickTime = 0;
            }
            tickTime = (tickTime + 1) % 10;
            float yOff = -0.5f * ((time - event.getPartialTicks()) / maxTime);// - (tickTime/20f));

            if (player != null && player.getItemInHand(event.getHand()).getItem() instanceof WeaponBaseItem)
            {
                switch (((WeaponBaseItem) player.getItemInHand(event.getHand()).getItem()).getPose())
                {
                    case ROLL:
                        yOff = -((time - event.getPartialTicks()) / maxTime) + 0.5f;
                        break;
                    case BRUSH:
                        event.getMatrixStack().mulPose(Vector3f.YN.rotation(yOff * ((player.getMainArm() == HandSide.RIGHT ? event.getHand().equals(Hand.MAIN_HAND) : event.getHand().equals(Hand.OFF_HAND)) ? 1 : -1)));
                        yOff = 0;
                        break;
                }
            }

            event.getMatrixStack().translate(0, yOff, 0);
        } else
        {
            tickTime = 0;
        }
    }

    @SubscribeEvent
    public static void onItemRender(RenderItemEvent event)
    {
        if (event instanceof RenderItemEvent.Gui.Pre && event.getItem().getItem().equals(SplatcraftItems.powerEgg))
        {
            IBakedModel modelIn = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getModelManager().getModel(new ModelResourceLocation(event.getItem().getItem().getRegistryName() + "#inventory"));
            renderItem(event.getItem(), event.getTransformType(), true, event.getMatrixStack(), event.getRenderTypeBuffer(), event.getLight(), event.getOverlay(), modelIn);
            event.setCanceled(true);
        }
        else if(event.getItem().getItem() instanceof SubWeaponItem)
        {
            MatrixStack matrixStack = event.getMatrixStack();
            AbstractSubWeaponEntity sub = ((SubWeaponItem)event.getItem().getItem()).entityType.create(Minecraft.getInstance().player.level);
            sub.setColor(ColorUtils.getInkColor(event.getItem()));
            sub.setItem(event.getItem());
            sub.readItemData(event.getItem().getOrCreateTag().getCompound("EntityData"));

            sub.isItem = true;

            Minecraft.getInstance().getItemRenderer().getItemModelShaper().getModelManager().getModel(new ModelResourceLocation(event.getItem().getItem().getRegistryName() + "#inventory"))
                    .handlePerspective(event.getTransformType(), matrixStack);
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sub).render(sub, 0, event.getPartialTicks(), matrixStack, event.getRenderTypeBuffer(), event.getLight());
            if(!matrixStack.clear())
                matrixStack.popPose();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemRenderHand(RenderHandEvent event)
    {
        MatrixStack matrixStack = event.getMatrixStack();

        matrixStack.pushPose();
        if(event.getItemStack().getItem() instanceof SubWeaponItem)
        {
            HandSide handside = event.getHand() == Hand.MAIN_HAND ? Minecraft.getInstance().player.getMainArm() : Minecraft.getInstance().player.getMainArm().getOpposite();
            AbstractSubWeaponEntity sub = ((SubWeaponItem)event.getItemStack().getItem()).entityType.create(Minecraft.getInstance().player.level);
            sub.setColor(ColorUtils.getInkColor(event.getItemStack()));
            sub.setItem(event.getItemStack());
            sub.readItemData(event.getItemStack().getOrCreateTag().getCompound("EntityData"));

            sub.isItem = true;

            float p_228405_5_ = event.getSwingProgress();
            float p_228405_7_ = event.getEquipProgress();

            float f5 = -0.4F * MathHelper.sin(MathHelper.sqrt(p_228405_5_) * (float)Math.PI);
            float f6 = 0.2F * MathHelper.sin(MathHelper.sqrt(p_228405_5_) * ((float)Math.PI * 2F));
            float f10 = -0.2F * MathHelper.sin(p_228405_5_ * (float)Math.PI);
            int l = handside == HandSide.RIGHT ? 1 : -1;
            matrixStack.translate((float) l * f5, f6, f10);
            applyItemArmTransform(matrixStack, handside, p_228405_7_);
            applyItemArmAttackTransform(matrixStack, handside, p_228405_5_);

            Minecraft.getInstance().getItemRenderer().getItemModelShaper().getModelManager().getModel(new ModelResourceLocation(event.getItemStack().getItem().getRegistryName() + "#inventory"))
                    .handlePerspective(handside == HandSide.RIGHT ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, matrixStack);

            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sub).render(sub, 0, event.getPartialTicks(), matrixStack, event.getBuffers(), event.getLight());
            if(!matrixStack.clear())
                matrixStack.popPose();
            event.setCanceled(true);
        }
         matrixStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyItemArmTransform(MatrixStack p_228406_1_, HandSide p_228406_2_, float p_228406_3_) {
        int i = p_228406_2_ == HandSide.RIGHT ? 1 : -1;
        p_228406_1_.translate((float) i * 0.56F, -0.52F + p_228406_3_ * -0.6F, -0.72F);
    }

    @OnlyIn(Dist.CLIENT)
    private static void applyItemArmAttackTransform(MatrixStack p_228399_1_, HandSide p_228399_2_, float p_228399_3_) {
        int i = p_228399_2_ == HandSide.RIGHT ? 1 : -1;
        float f = MathHelper.sin(p_228399_3_ * p_228399_3_ * (float)Math.PI);
        p_228399_1_.mulPose(Vector3f.YP.rotationDegrees((float)i * (45.0F + f * -20.0F)));
        float f1 = MathHelper.sin(MathHelper.sqrt(p_228399_3_) * (float)Math.PI);
        p_228399_1_.mulPose(Vector3f.ZP.rotationDegrees((float)i * f1 * -20.0F));
        p_228399_1_.mulPose(Vector3f.XP.rotationDegrees(f1 * -80.0F));
        p_228399_1_.mulPose(Vector3f.YP.rotationDegrees((float)i * -45.0F));
    }

    protected static void renderItem(ItemStack itemStackIn, ItemCameraTransforms.TransformType transformTypeIn, boolean leftHand, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn, IBakedModel modelIn)
    {
        if (!itemStackIn.isEmpty())
        {
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

            matrixStackIn.pushPose();
            boolean flag = transformTypeIn == ItemCameraTransforms.TransformType.GUI || transformTypeIn == ItemCameraTransforms.TransformType.GROUND || transformTypeIn == ItemCameraTransforms.TransformType.FIXED;
            if (itemStackIn.getItem() == Items.TRIDENT && flag)
            {
                modelIn = itemRenderer.getItemModelShaper().getModelManager().getModel(new ModelResourceLocation("minecraft:trident#inventory"));
            }

            modelIn = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(matrixStackIn, modelIn, transformTypeIn, leftHand);
            matrixStackIn.translate(-0.5D, -0.5D, -0.5D);
            if (!modelIn.isCustomRenderer() && (itemStackIn.getItem() != Items.TRIDENT || flag))
            {
                boolean flag1;
                if (transformTypeIn != ItemCameraTransforms.TransformType.GUI && !transformTypeIn.firstPerson() && itemStackIn.getItem() instanceof BlockItem)
                {
                    Block block = ((BlockItem) itemStackIn.getItem()).getBlock();
                    flag1 = !(block instanceof BreakableBlock) && !(block instanceof StainedGlassPaneBlock);
                } else
                {
                    flag1 = true;
                }
                if (modelIn.isLayered())
                {
                    net.minecraftforge.client.ForgeHooksClient.drawItemLayered(itemRenderer, modelIn, itemStackIn, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn, flag1);
                } else
                {
                    RenderType rendertype = getItemEntityTranslucent(PlayerContainer.BLOCK_ATLAS);
                    IVertexBuilder ivertexbuilder;
                    if (itemStackIn.getItem() == Items.COMPASS && itemStackIn.hasFoil())
                    {
                        matrixStackIn.pushPose();
                        MatrixStack.Entry matrixstack$entry = matrixStackIn.last();
                        if (transformTypeIn == ItemCameraTransforms.TransformType.GUI)
                        {
                            matrixstack$entry.pose().multiply(0.5F);
                        } else if (transformTypeIn.firstPerson())
                        {
                            matrixstack$entry.pose().multiply(0.75F);
                        }

                        if (flag1)
                        {
                            ivertexbuilder = ItemRenderer.getCompassFoilBufferDirect(bufferIn, rendertype, matrixstack$entry);
                        } else
                        {
                            ivertexbuilder = ItemRenderer.getCompassFoilBuffer(bufferIn, rendertype, matrixstack$entry);
                        }

                        matrixStackIn.popPose();
                    } else if (flag1)
                    {
                        ivertexbuilder = ItemRenderer.getFoilBufferDirect(bufferIn, rendertype, true, itemStackIn.hasFoil());
                    } else
                    {
                        ivertexbuilder = ItemRenderer.getFoilBuffer(bufferIn, rendertype, true, itemStackIn.hasFoil());
                    }

                    itemRenderer.renderModelLists(modelIn, itemStackIn, combinedLightIn, combinedOverlayIn, matrixStackIn, ivertexbuilder);
                }
            } else
            {
                itemStackIn.getItem().getItemStackTileEntityRenderer().renderByItem(itemStackIn, transformTypeIn, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
            }

            matrixStackIn.popPose();
        }
    }

    protected static RenderType getItemEntityTranslucent(ResourceLocation locationIn)
    {
        RenderType.State rendertype$state = RenderType.State.builder().setTextureState(new RenderState.TextureState(locationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                /*.target(field_241712_U_)*/.setDiffuseLightingState(new RenderState.DiffuseLightingState(true)).setAlphaState(new RenderState.AlphaState(0.003921569F)).setLightmapState(new RenderState.LightmapState(true))
                .setOverlayState(new RenderState.OverlayState(true)).createCompositeState(true);
        return RenderType.create("item_entity_translucent", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, false, rendertype$state);
    }

    @SubscribeEvent
    public static void onChatMessage(ClientChatReceivedEvent event)
    {
        ClientWorld level = Minecraft.getInstance().level;
        if (level != null && SplatcraftGameRules.getBooleanRuleValue(level, SplatcraftGameRules.COLORED_PLAYER_NAMES) && event.getMessage() instanceof TranslationTextComponent)
        {
            TranslationTextComponent component = (TranslationTextComponent) event.getMessage();
            //TreeMap<String, AbstractClientPlayerEntity> players = Maps.newTreeMap();
            //Minecraft.getInstance().level.getPlayers().forEach(player -> players.put(player.getDisplayName().getString(), player));


            List<String> players = new ArrayList<>();
            ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
            if (connection != null)
                connection.getOnlinePlayers().forEach(info -> players.add(getDisplayName(info).getString()));

            for (Object obj : component.getArgs())
            {
                if (!(obj instanceof TextComponent))
                    continue;

                TextComponent msgChildren = (TextComponent) obj;
                String key = msgChildren.getString();

                if (!msgChildren.getSiblings().isEmpty() && players.contains(key))
                    msgChildren.setStyle(msgChildren.getStyle().withColor(Color.fromRgb(ClientUtils.getClientPlayerColor(key))));
            }
        }
    }

    @SubscribeEvent
    public static void renderNameplate(RenderNameplateEvent event)
    {
        if (SplatcraftGameRules.getBooleanRuleValue(event.getEntity().level, SplatcraftGameRules.COLORED_PLAYER_NAMES) && event.getEntity() instanceof LivingEntity)
        {
            int color = ColorUtils.getEntityColor(event.getEntity());
            if (SplatcraftConfig.Client.getColorLock())
            {
                color = ColorUtils.getLockedColor(color);
            }
            if (color != -1)
            {
                event.setContent(((TextComponent) event.getContent()).setStyle(Style.EMPTY.withColor(Color.fromRgb(color))));
            }
        }
    }

    public static ITextComponent getDisplayName(NetworkPlayerInfo info)
    {
        return info.getTabListDisplayName() != null ? info.getTabListDisplayName().copy() : ScorePlayerTeam.formatNameForTeam(info.getTeam(), new StringTextComponent(info.getProfile().getName()));
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void renderGui(RenderGameOverlayEvent event)
    {
        PlayerEntity player = Minecraft.getInstance().player;
        if (player == null || !PlayerInfoCapability.hasCapability(player))
        {
            return;
        }
        IPlayerInfo info = PlayerInfoCapability.get(player);

        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        if (event instanceof RenderGameOverlayEvent.Pre && event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR))
        {
            if (player.getMainHandItem().getItem() instanceof ChargerItem || player.getOffhandItem().getItem() instanceof ChargerItem)
            {

                MatrixStack matrixStack = event.getMatrixStack();
                matrixStack.pushPose();
                RenderSystem.enableBlend();
                Minecraft.getInstance().getTextureManager().bind(WIDGETS);

                AbstractGui.blit(matrixStack, width / 2 - 15, height / 2 + 14, 30, 9, 88, 0, 30, 9, 256, 256);
                if (PlayerInfoCapability.hasCapability(player) && PlayerInfoCapability.get(player).getPlayerCharge() != null) {
                    float charge = PlayerInfoCapability.get(player).getPlayerCharge().charge;
                    AbstractGui.blit(matrixStack, width / 2 - 15, height / 2 + 14, (int) (30 * charge), 9, 88, 9, (int) (30 * charge), 9, 256, 256);
                }

                matrixStack.popPose();
            }
        }

        boolean showCrosshairInkIndicator = SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.CROSSHAIR);
        boolean isHoldingMatchItem = SplatcraftTags.Items.MATCH_ITEMS.contains(player.getMainHandItem().getItem()) || SplatcraftTags.Items.MATCH_ITEMS.contains(player.getOffhandItem().getItem());
        boolean showLowInkWarning = showCrosshairInkIndicator && SplatcraftConfig.Client.lowInkWarning.get() && (isHoldingMatchItem || info.isSquid()) && !enoughInk(player, null, 10f, 0, false);

        boolean canUse = true;
        boolean hasTank = player.getItemBySlot(EquipmentSlotType.CHEST).getItem() instanceof InkTankItem;
        float inkPctg = 0;
        if (hasTank) {
            ItemStack stack = player.getItemBySlot(EquipmentSlotType.CHEST);
            inkPctg = InkTankItem.getInkAmount(stack) / ((InkTankItem) stack.getItem()).capacity;
            if (isHoldingMatchItem)
                canUse = ((InkTankItem) stack.getItem()).canUse(player.getMainHandItem().getItem()) || ((InkTankItem) stack.getItem()).canUse(player.getOffhandItem().getItem());
        }
        if (info.isSquid() || showLowInkWarning || !canUse) {
            if (event.getType().equals(RenderGameOverlayEvent.ElementType.HOTBAR)) {
                squidTime++;

                if (showCrosshairInkIndicator) {
                    int heightAnim = Math.min(14, squidTime);
                    int glowAnim = Math.max(0, Math.min(18, squidTime - 16));
                    float[] rgb = ColorUtils.hexToRGB(info.getColor());

                    MatrixStack matrixStack = event.getMatrixStack();
                    matrixStack.pushPose();
                    RenderSystem.enableBlend();
                    Minecraft.getInstance().getTextureManager().bind(WIDGETS);

                    if (enoughInk(player, null, 220, 0, false)) { // checks if you have unlimited ink
                        AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 131, 18, 2, 256, 256);
                        AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 131, 18, 4 + heightAnim, 256, 256);

                        RenderSystem.color3f(rgb[0], rgb[1], rgb[2]);

                        AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 18, 131, 18, 4 + heightAnim, 256, 256);
                        AbstractGui.blit(matrixStack, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 149, glowAnim, 18, 256, 256);
                    } else {
                        AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 2, 0, 95, 18, 2, 256, 256);
                        AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9 + 14 - heightAnim, 18, 4 + heightAnim, 0, 95, 18, 4 + heightAnim, 256, 256);

                        if (inkPctg != prevInkPctg && inkPctg == 1) {
                            inkFlash = 0.1f;
                        }
                        inkFlash = Math.max(0, inkFlash - 0.002f);

                        float inkPctgLerp = lerp(prevInkPctg, inkPctg, 0.05f);
                        float inkSize = (1 - inkPctg) * 18;

                        RenderSystem.color3f(rgb[0] + inkFlash, rgb[1] + inkFlash, rgb[2] + inkFlash);
                        matrixStack.translate(0, inkSize - Math.floor(inkSize), 0);
                        AbstractGui.blit(matrixStack, width / 2 + 9, (int) (height / 2 - 9 + (14 - heightAnim) + (1 - inkPctgLerp) * 18), 18, (int) ((4 + heightAnim) * inkPctgLerp), 18, 95 + inkSize, 18, (int) ((4 + heightAnim) * inkPctg), 256, 256);
                        matrixStack.translate(0, -(inkSize - Math.floor(inkSize)), 0);

                        if (SplatcraftConfig.Client.vanillaInkDurability.get())
                        {
                            float[] durRgb = ColorUtils.hexToRGB(MathHelper.hsvToRgb(Math.max(0.0F, inkPctgLerp) / 3.0F, 1.0F, 1.0F));
                            RenderSystem.color3f(durRgb[0], durRgb[1], durRgb[2]);
                        } else
                        {
                            RenderSystem.color3f(rgb[0], rgb[1], rgb[2]);
                        }

                        AbstractGui.blit(matrixStack, width / 2 + 9 + 18 - glowAnim, height / 2 - 9, glowAnim, 18, 18 - glowAnim, 113, glowAnim, 18, 256, 256);

                        RenderSystem.color3f(1, 1, 1);
                        if (glowAnim == 18) {
                            if (!canUse) {
                                AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9, 36, 112, 18, 18, 256, 256);
                            } else if (showLowInkWarning) {
                                AbstractGui.blit(matrixStack, width / 2 + 9, height / 2 - 9, 18, 112, 18, 18, 256, 256);
                            }
                        }
                    }
                    matrixStack.popPose();
                }
                prevInkPctg = inkPctg;
            }
        } else
        {
            squidTime = 0;
        }

    }

    private static float lerp(float a, float b, float f)
    {
        return a + f * (b - a);
    }
}
