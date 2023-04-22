package net.splatcraft.forge.entities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.blocks.StageBarrierBlock;
import net.splatcraft.forge.client.particles.InkExplosionParticleData;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.handlers.WeaponHandler;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.items.weapons.settings.IDamageCalculator;
import net.splatcraft.forge.items.weapons.settings.WeaponSettings;
import net.splatcraft.forge.registries.SplatcraftEntities;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.InkDamageUtils;
import net.splatcraft.forge.util.InkExplosion;

public class InkProjectileEntity extends ProjectileItemEntity implements IColoredEntity {

    private static final DataParameter<String> PROJ_TYPE = EntityDataManager.defineId(InkProjectileEntity.class, DataSerializers.STRING);
    private static final DataParameter<Integer> COLOR = EntityDataManager.defineId(InkProjectileEntity.class, DataSerializers.INT);
    private static final DataParameter<Float> PROJ_SIZE = EntityDataManager.defineId(InkProjectileEntity.class, DataSerializers.FLOAT);

    public float gravityVelocity = 0.075f;
    public int lifespan = 600;
    public boolean explodes = false;
    public boolean bypassMobDamageMultiplier = false;
    public boolean canPierce = false;
    public boolean persistent = false;
    public ItemStack sourceWeapon = ItemStack.EMPTY;
    public float trailSize = 0;
    public int trailCooldown = 0;
    public String damageType = "splat";
    public boolean causesHurtCooldown = false;
    public boolean throwerAirborne = false;
    public float charge;
    public boolean isOnRollCooldown = false;

    public IDamageCalculator damage = WeaponSettings.DEFAULT;
    public InkBlockUtils.InkType inkType;


    public InkProjectileEntity(EntityType<? extends ProjectileItemEntity> type, World level) {
        super(type, level);
    }

    public InkProjectileEntity(World level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, IDamageCalculator damage, ItemStack sourceWeapon) {
        super(SplatcraftEntities.INK_PROJECTILE, thrower, level);
        setColor(color);
        setProjectileSize(projectileSize);
        this.throwerAirborne = !thrower.isOnGround();
        this.damage = damage;
        this.inkType = inkType;
        this.sourceWeapon = sourceWeapon;
    }

    public InkProjectileEntity(World level, LivingEntity thrower, int color, InkBlockUtils.InkType inkType, float projectileSize, IDamageCalculator damage) {
        this(level, thrower, color, inkType, projectileSize, damage, ItemStack.EMPTY);
    }

    public InkProjectileEntity(World level, LivingEntity thrower, ItemStack sourceWeapon, InkBlockUtils.InkType inkType, float projectileSize, IDamageCalculator damage) {
        this(level, thrower, ColorUtils.getInkColor(sourceWeapon), inkType, projectileSize, damage, sourceWeapon);
    }

    public InkProjectileEntity setShooterTrail() {
        trailCooldown = 4;
        trailSize = getProjectileSize() * 0.75f;
        return this;
    }

    public InkProjectileEntity setChargerStats(float charge, int lifespan, boolean canPierce) {
        this.charge = charge;
        trailSize = getProjectileSize() * 1.1f;
        this.lifespan = lifespan;
        gravityVelocity = 0;
        this.canPierce = canPierce;
        setProjectileType(Types.CHARGER);
        return this;
    }

    public InkProjectileEntity setBlasterStats(int lifespan) {
        this.lifespan = lifespan;
        gravityVelocity = 0;
        trailSize = getProjectileSize() * 0.5f;
        explodes = true;
        setProjectileType(Types.BLASTER);
        return this;
    }

    public InkProjectileEntity setRollerSwingStats() {
        setProjectileType(Types.ROLLER);

        if (!throwerAirborne) {
            damageType = "roll";
            causesHurtCooldown = true;
        } else trailSize = getProjectileSize() * 0.5f;
        return this;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(COLOR, ColorUtils.DEFAULT);
        entityData.define(PROJ_TYPE, Types.SHOOTER);
        entityData.define(PROJ_SIZE, 1.0f);
    }

    @Override
    public void onSyncedDataUpdated(DataParameter<?> dataParameter) {
        if (dataParameter.equals(PROJ_SIZE)) {
            refreshDimensions();
        }

        super.onSyncedDataUpdated(dataParameter);
    }

    @Override
    protected Item getDefaultItem() {
        return SplatcraftItems.splattershot;
    }

    @Override
    public void tick() {
        super.tick();

        if (isInWater()) {
            remove();
            return;
        }

        if (!level.isClientSide && !persistent && lifespan-- <= 0) {
            float dmg = damage.calculateDamage(this.tickCount, throwerAirborne, charge, isOnRollCooldown);
            InkExplosion.createInkExplosion(level, getOwner(), blockPosition(), getProjectileSize() * 0.85f, explodes ? damage.getMinDamage() : dmg, dmg, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);
            if (explodes) {
                level.broadcastEntityEvent(this, (byte) 3);
                level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            }
            remove();
        }

        if (trailSize > 0 && (trailCooldown == 0 || tickCount % trailCooldown == 0)) {
            for (double y = getY(); y >= 0 && getY() - y <= 8; y--) {
                BlockPos inkPos = new BlockPos(getX(), y, getZ());
                if (!InkBlockUtils.canInkPassthrough(level, inkPos)) {
                    if (!isInvisible())
                        level.broadcastEntityEvent(this, (byte) 1);
                    InkExplosion.createInkExplosion(level, getOwner(), inkPos.relative(Direction.UP), trailSize, 0, 0, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);
                    InkExplosion.createInkExplosion(level, getOwner(), blockPosition(), trailSize, 0, 0, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);
                    break;
                }
            }
        }

    }

    @Override
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);

        switch (id) {
            case -1:
                level.addParticle(new InkExplosionParticleData(getColor(), .5f), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                break;
            case 1:
                if (getProjectileType().equals(Types.CHARGER))
                    level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize()), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, 0, -0.1, 0);
                else
                    level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize()), this.getX() - this.getDeltaMovement().x() * 0.25D, this.getY() - this.getDeltaMovement().y() * 0.25D, this.getZ() - this.getDeltaMovement().z() * 0.25D, this.getDeltaMovement().x(), this.getDeltaMovement().y(), this.getDeltaMovement().z());
                break;
            case 2:
                level.addParticle(new InkSplashParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                break;
            case 3:
                level.addParticle(new InkExplosionParticleData(getColor(), getProjectileSize() * 2), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                break;
        }

    }

    @Override
    protected void onHitEntity(EntityRayTraceResult result) {
        super.onHitEntity(result);

        Entity target = result.getEntity();
        float dmg = damage.calculateDamage(this.tickCount, throwerAirborne, charge, isOnRollCooldown);

        if(!level.isClientSide() && target instanceof SpawnShieldEntity && !InkDamageUtils.canDamage(target, this))
        {
            remove();
            level.broadcastEntityEvent(this, (byte) -1);
        }

        if (target instanceof LivingEntity) {
            if (InkDamageUtils.isSplatted((LivingEntity) target)) return;

            if (InkDamageUtils.doDamage(level, (LivingEntity) target, dmg, getColor(), getOwner(), this, sourceWeapon, bypassMobDamageMultiplier, damageType, causesHurtCooldown) &&
                    charge >= 1.0f && getOwner() instanceof ServerPlayerEntity)
                ((ServerPlayerEntity) getOwner()).connection.send(new SChangeGameStatePacket(SChangeGameStatePacket.ARROW_HIT_PLAYER, 0.0F));
        }

        if (!canPierce) {
            if (explodes) {
                InkExplosion.createInkExplosion(level, getOwner(), blockPosition(), getProjectileSize() * 0.85f, damage.getMinDamage(), dmg, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);
                level.broadcastEntityEvent(this, (byte) 3);
                level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            } else
                level.broadcastEntityEvent(this, (byte) 2);

            if(!level.isClientSide)
                remove();
        }
    }

    @Override
    protected void onHitBlock(BlockRayTraceResult result) {
        if (InkBlockUtils.canInkPassthrough(level, result.getBlockPos()))
            return;

        if (level.getBlockState(result.getBlockPos()).getBlock() instanceof ColoredBarrierBlock &&
                ((ColoredBarrierBlock) level.getBlockState(result.getBlockPos()).getBlock()).canAllowThrough(result.getBlockPos(), this))
            return;

        super.onHitBlock(result);

        float dmg = damage.calculateDamage(this.tickCount, throwerAirborne, charge, isOnRollCooldown);
        InkExplosion.createInkExplosion(level, getOwner(), blockPosition(), getProjectileSize() * 0.85f, explodes ? damage.getMinDamage() : dmg, dmg, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);
        if (explodes) {
            level.broadcastEntityEvent(this, (byte) 3);
            level.playSound(null, getX(), getY(), getZ(), SplatcraftSounds.blasterExplosion, SoundCategory.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
        } else if(level.getBlockState(result.getBlockPos()).getBlock() instanceof StageBarrierBlock)
            level.broadcastEntityEvent(this, (byte) -1);
        else level.broadcastEntityEvent(this, (byte) 2);
        if (!level.isClientSide)
            this.remove();
    }

    @Override
    public void shootFromRotation(Entity thrower, float pitch, float yaw, float pitchOffset, float velocity, float inaccuracy) {
        super.shootFromRotation(thrower, pitch, yaw, pitchOffset, velocity, inaccuracy);
        InkExplosion.createInkExplosion(level, getOwner(), thrower.blockPosition(), 0.75f, 0, 0, bypassMobDamageMultiplier, getColor(), inkType, sourceWeapon);

        Vector3d posDiff = new Vector3d(0, 0, 0);

        if (thrower instanceof PlayerEntity) {
            posDiff = thrower.position().subtract(WeaponHandler.getPlayerPrevPos((PlayerEntity) thrower));
            if (thrower.isOnGround())
                posDiff.multiply(1, 0, 1);
        }


        moveTo(getX() + posDiff.x(), getY() + posDiff.y(), getZ() + posDiff.z());
        Vector3d throwerSpeed = thrower.getDeltaMovement();
        Vector3d speed = getDeltaMovement()
                .subtract(throwerSpeed.x, thrower.isOnGround() ? 0.0 : throwerSpeed.y, throwerSpeed.z)
                .add(Math.min(2.5, throwerSpeed.x * 0.8), 0.0, Math.min(2.5, throwerSpeed.z * 0.8))
                .add(posDiff.multiply(0.8, 0.8, 0.8));
        setDeltaMovement(speed);
    }

    @Override
    protected void onHit(RayTraceResult result) {
        RayTraceResult.Type rayType = result.getType();
        if (rayType == RayTraceResult.Type.ENTITY) {
            this.onHitEntity((EntityRayTraceResult) result);
        } else if (rayType == RayTraceResult.Type.BLOCK) {
            onHitBlock((BlockRayTraceResult) result);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT nbt) {
        if (nbt.contains("Size"))
            setProjectileSize(nbt.getFloat("Size"));
        if (nbt.contains("Color"))
            setColor(ColorUtils.getColorFromNbt(nbt));

        if (nbt.contains("GravityVelocity"))
            gravityVelocity = nbt.getFloat("GravityVelocity");
        if (nbt.contains("Lifespan"))
            lifespan = nbt.getInt("Lifespan");

        trailCooldown = nbt.getInt("TrailCooldown");
        trailSize = nbt.getFloat("TrailSize");
        bypassMobDamageMultiplier = nbt.getBoolean("BypassMobDamageMultiplier");
        canPierce = nbt.getBoolean("CanPierce");
        explodes = nbt.getBoolean("Explodes");
        persistent = nbt.getBoolean("Persistent");
        causesHurtCooldown = nbt.getBoolean("CausesHurtCooldown");
        damageType = nbt.getString("DamageType");

        setInvisible(nbt.getBoolean("Invisible"));

        String type = nbt.getString("ProjectileType");
        setProjectileType(type.isEmpty() ? Types.DEFAULT : type);
        inkType = InkBlockUtils.InkType.values.getOrDefault(new ResourceLocation(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);

        sourceWeapon = ItemStack.of(nbt.getCompound("SourceWeapon"));

        if(sourceWeapon.getItem() instanceof WeaponBaseItem)
            damage = ((WeaponBaseItem) sourceWeapon.getItem()).damageCalculator;
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT nbt) {
        nbt.putFloat("Size", getProjectileSize());
        nbt.putInt("Color", getColor());

        nbt.putFloat("GravityVelocity", gravityVelocity);
        nbt.putInt("Lifespan", lifespan);
        nbt.putFloat("TrailSize", trailSize);
        nbt.putInt("TrailCooldown", trailCooldown);
        nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
        nbt.putBoolean("CanPierce", canPierce);
        nbt.putBoolean("Explodes", explodes);
        nbt.putBoolean("Persistent", persistent);
        nbt.putBoolean("CausesHurtCooldown", causesHurtCooldown);

        nbt.putBoolean("Invisible", isInvisible());

        nbt.putString("DamageType", damageType);
        nbt.putString("ProjectileType", getProjectileType());
        nbt.putString("InkType", inkType.getSerializedName());
        nbt.put("SourceWeapon", sourceWeapon.save(new CompoundNBT()));
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public EntitySize getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getProjectileSize() / 2f);
    }

    @Override
    public float getGravity() {
        return gravityVelocity;
    }

    public float getProjectileSize() {
        return entityData.get(PROJ_SIZE);
    }

    public void setProjectileSize(float size) {
        entityData.set(PROJ_SIZE, size);
        reapplyPosition();
        refreshDimensions();
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public int getColor() {
        return entityData.get(COLOR);
    }

    @Override
    public void setColor(int color) {
        entityData.set(COLOR, color);
    }

    public String getProjectileType() {
        return entityData.get(PROJ_TYPE);
    }

    public void setProjectileType(String v) {
        entityData.set(PROJ_TYPE, v);
    }

    public static class Types {
        public static final String DEFAULT = "ink";
        public static final String SHOOTER = "shooter";
        public static final String CHARGER = "charger";
        public static final String ROLLER = "roller";
        public static final String BLASTER = "blaster";
    }
}
