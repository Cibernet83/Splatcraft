package com.cibernet.splatcraft.client.renderer.subs;

import com.cibernet.splatcraft.Splatcraft;
import com.cibernet.splatcraft.client.model.subs.BurstBombModel;
import com.cibernet.splatcraft.entities.subs.BurstBombEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class BurstBombRenderer extends SubWeaponRenderer<BurstBombEntity, BurstBombModel>
{
    private static final BurstBombModel MODEL = new BurstBombModel();
    private static final ResourceLocation TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/weapons/sub/burst_bomb.png");
    private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation(Splatcraft.MODID, "textures/weapons/sub/burst_bomb_ink.png");

    public BurstBombRenderer(EntityRendererManager manager)
    {
        super(manager);
    }

    @Override
    public void render(BurstBombEntity entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {

        matrixStackIn.push();
        matrixStackIn.translate(0.0D, 0.2/*0.15000000596046448D*/, 0.0D);
        matrixStackIn.rotate(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevRotationYaw, entityIn.rotationYaw) - 180.0F));
        matrixStackIn.rotate(Vector3f.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.prevRotationPitch, entityIn.rotationPitch)+90F));
        matrixStackIn.scale(1, -1, 1);
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.pop();
    }

    @Override
    public ResourceLocation getEntityTexture(BurstBombEntity entity)
    {
        return TEXTURE;
    }

    @Override
    public BurstBombModel getModel()
    {
        return MODEL;
    }

    @Override
    public ResourceLocation getOverlayTexture(BurstBombEntity entity)
    {
        return OVERLAY_TEXTURE;
    }

}
