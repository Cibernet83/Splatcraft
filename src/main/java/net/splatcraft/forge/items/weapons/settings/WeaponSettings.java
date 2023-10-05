package net.splatcraft.forge.items.weapons.settings;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.GsonHelper;

import java.util.Optional;

public class WeaponSettings extends AbstractWeaponSettings<WeaponSettings.DataRecord>
{
    public float projectileSize;
    public int projectileLifespan = 600;
    public float projectileSpeed;
    public int projectileCount = 1;

    public int firingSpeed;
    public int startupTicks;

    public int dischargeTicks;

    public float groundInaccuracy;
    public float airInaccuracy;

    public float inkConsumption;
    public float minInkConsumption;
    public int inkRecoveryCooldown;

    public float baseDamage;
    public float minDamage;
    public float chargedDamage;
    public int damageDecayStartTick;
    public float damageDecayPerTick;

    public float chargerMobility;
    public boolean fastMidAirCharge;
    public float chargerPiercesAt;
    public float chargeSpeed;
    public float dischargeSpeed;

    public int rollCount;
    public float rollSpeed;
    public float rollInaccuracy;
    public float rollInkConsumption;
    public int rollInkRecoveryCooldown;
    public int rollCooldown;
    public int lastRollCooldown;

    //glooga dualies are *so* special, screw them
    public float rollBaseDamage;
    public float rollMinDamage;
    public int rollDamageDecayStartTick;
    public float rollDamageDecayPerTick;

    public static final WeaponSettings DEFAULT = new WeaponSettings("default");

    public WeaponSettings(String name) {
        super(name);
    }

    @Override
    public float calculateDamage(int tickCount, boolean airborne, float charge, boolean isOnRollCooldown) {
        if (isOnRollCooldown) {
            int e = tickCount - rollDamageDecayStartTick;
            return Math.max(e > 0 ? rollBaseDamage - (e * rollDamageDecayPerTick) : rollBaseDamage, rollMinDamage);
        }

        if (charge > 0.0f)
            return charge >= 1.0f ? chargedDamage : minDamage + (baseDamage - minDamage) * charge;

        int e = tickCount - damageDecayStartTick;
        return Math.max(e > 0 ? baseDamage - (e * damageDecayPerTick) : baseDamage, minDamage);
    }

    @Override
    public float getMinDamage() {
        return minDamage;
    }

    @Override
    public Codec<DataRecord> getCodec() {
        return DataRecord.CODEC;
    }

    @Override
    public void deserialize(DataRecord data)
    {
        ProjectileDataRecord projectile = data.projectile;

        setProjectileSize(projectile.size);
        setProjectileSpeed(projectile.speed);
        projectile.lifespan.ifPresent(this::setProjectileLifespan);
        projectile.count.ifPresent(this::setProjectileCount);

        setFiringSpeed(projectile.firingSpeed);
        setGroundInaccuracy(projectile.groundInaccuracy);
        setAirInaccuracy(projectile.airInaccuracy);

        setInkConsumption(projectile.inkConsumption);
        setInkRecoveryCooldown(projectile.inkRecoveryCooldown);

        setBaseDamage(projectile.baseDamage);
        setMinDamage(projectile.minDamage);
        setDamageDecayStartTick(projectile.damageDecayStartTick);
        setDamageDecayPerTick(projectile.damageDecayPerTick);

        data.dualieRoll.ifPresent(dualieRoll ->
        {
            setRollCount(dualieRoll.count);
            setRollSpeed(dualieRoll.speed);
            setRollInkConsumption(dualieRoll.inkConsumption);
            setRollCooldown(dualieRoll.finalRollDuration);
            setLastRollCooldown(dualieRoll.finalRollDuration);
        });

        data.dualieTurret.ifPresent(dualieTurret ->
        {
            setRollInaccuracy(dualieTurret.fireInaccuracy);
            dualieTurret.baseDamage.ifPresent(this::setRollBaseDamage);
            dualieTurret.minDamage.ifPresent(this::setRollMinDamage);
            dualieTurret.damageDecayPerTick.ifPresent(this::setRollDamageDecayPerTick);
            dualieTurret.damageDecayStartTick.ifPresent(this::setRollDamageDecayStartTick);
        });

        data.charger.ifPresent(charger ->
        {
            setChargedDamage(charger.chargedDamage);
            setChargerMobility(charger.mobility);
            setFastMidAirCharge(charger.fastMidairCharge);
            setChargerPiercesAt(charger.piercesAtCharge);
        });
    }

    @Override
    public DataRecord serialize() {
        return new DataRecord(new ProjectileDataRecord(projectileSize, projectileSpeed, Optional.of(projectileLifespan), Optional.of(projectileCount), firingSpeed,
                groundInaccuracy, airInaccuracy, inkConsumption, inkRecoveryCooldown, baseDamage, minDamage, damageDecayStartTick, damageDecayPerTick),
                Optional.of(new DualieRollDataRecord(rollCount, rollSpeed, rollInkConsumption, rollCooldown, lastRollCooldown)),
                Optional.of(new DualieTurretDataRecord(rollInaccuracy, Optional.of(rollBaseDamage), Optional.of(rollMinDamage), Optional.of(rollDamageDecayStartTick), Optional.of(rollDamageDecayPerTick))),
                Optional.of(new ChargerDataRecord(chargedDamage, chargerMobility, fastMidAirCharge, chargerPiercesAt)));
    }

    public WeaponSettings setProjectileSize(float projectileSize) {
        this.projectileSize = projectileSize;
        return this;
    }

    public WeaponSettings setProjectileLifespan(int projectileLifespan) {
        this.projectileLifespan = projectileLifespan;
        return this;
    }

    public WeaponSettings setProjectileSpeed(float projectileSpeed) {
        this.projectileSpeed = projectileSpeed;
        return this;
    }

    public WeaponSettings setProjectileCount(int projectileCount) {
        this.projectileCount = projectileCount;
        return this;
    }

    public WeaponSettings setFiringSpeed(int firingSpeed) {
        this.firingSpeed = firingSpeed;
        return this;
    }

    public WeaponSettings setStartupTicks(int startupTicks) {
        this.startupTicks = startupTicks;
        this.chargeSpeed = 1f / (float)startupTicks;
        return this;
    }

    public WeaponSettings setDischargeTicks(int dischargeTicks)
    {
        this.dischargeTicks = dischargeTicks;
        this.dischargeSpeed = 1f / (float) dischargeTicks;
        return this;
    }

    public WeaponSettings setGroundInaccuracy(float groundInaccuracy) {
        this.groundInaccuracy = groundInaccuracy;
        this.airInaccuracy = groundInaccuracy;
        return this;
    }

    public WeaponSettings setAirInaccuracy(float airInaccuracy) {
        this.airInaccuracy = airInaccuracy;
        return this;
    }

    public WeaponSettings setInkConsumption(float inkConsumption) {
        this.inkConsumption = inkConsumption;
        return this;
    }

    public WeaponSettings setMinInkConsumption(float minInkConsumption) {
        this.minInkConsumption = minInkConsumption;
        return this;
    }

    public WeaponSettings setInkRecoveryCooldown(int inkRecoveryCooldown) {
        this.inkRecoveryCooldown = inkRecoveryCooldown;
        return this;
    }

    public WeaponSettings setBaseDamage(float baseDamage) {
        this.baseDamage = baseDamage;
        this.rollBaseDamage = baseDamage;
        this.chargedDamage = baseDamage;
        return this;
    }

    public WeaponSettings setMinDamage(float minDamage) {
        this.minDamage = minDamage;
        this.rollMinDamage = minDamage;
        return this;
    }

    public WeaponSettings setChargedDamage(float chargedDamage) {
        this.chargedDamage = chargedDamage;
        return this;
    }

    public WeaponSettings setDamageDecayStartTick(int damageDecayStartTick) {
        this.damageDecayStartTick = damageDecayStartTick;
        this.rollDamageDecayStartTick = damageDecayStartTick;
        return this;
    }

    public WeaponSettings setDamageDecayPerTick(float damageDecayPerTick) {
        this.damageDecayPerTick = damageDecayPerTick;
        this.rollDamageDecayPerTick = damageDecayPerTick;
        return this;
    }

    public WeaponSettings setChargerMobility(float chargerMobility) {
        this.chargerMobility = chargerMobility;
        return this;
    }

    public WeaponSettings setFastMidAirCharge(boolean fastMidAirCharge) {
        this.fastMidAirCharge = fastMidAirCharge;
        return this;
    }

    public WeaponSettings setChargerPiercesAt(float chargerPiercesAt) {
        this.chargerPiercesAt = chargerPiercesAt;
        return this;
    }

    public WeaponSettings setRollCount(int rollCount) {
        this.rollCount = rollCount;
        return this;
    }

    public WeaponSettings setRollSpeed(float rollSpeed) {
        this.rollSpeed = rollSpeed;
        return this;
    }

    public WeaponSettings setRollInaccuracy(float rollInaccuracy) {
        this.rollInaccuracy = rollInaccuracy;
        return this;
    }

    public WeaponSettings setRollInkConsumption(float rollInkConsumption) {
        this.rollInkConsumption = rollInkConsumption;
        return this;
    }

    public WeaponSettings setRollInkRecoveryCooldown(int rollInkRecoveryCooldown) {
        this.rollInkRecoveryCooldown = rollInkRecoveryCooldown;
        return this;
    }

    public WeaponSettings setRollCooldown(int rollCooldown) {
        this.rollCooldown = rollCooldown;
        return this;
    }

    public WeaponSettings setLastRollCooldown(int lastRollCooldown) {
        this.lastRollCooldown = lastRollCooldown;
        return this;
    }

    //whoever at Nintendo made me do this, why?!
    public WeaponSettings setRollBaseDamage(float rollBaseDamage) {
        this.rollBaseDamage = rollBaseDamage;
        return this;
    }

    public WeaponSettings setRollMinDamage(float rollMinDamage) {
        this.rollMinDamage = rollMinDamage;
        return this;
    }

    public WeaponSettings setRollDamageDecayStartTick(int rollDamageDecayStartTick) {
        this.rollDamageDecayStartTick = rollDamageDecayStartTick;
        return this;
    }

    public WeaponSettings setRollDamageDecayPerTick(float rollDamageDecayPerTick) {
        this.rollDamageDecayPerTick = rollDamageDecayPerTick;
        return this;
    }

    public int getDualieOffhandFiringOffset()
    {
        return firingSpeed / 2;
    }

    public record DataRecord(
        ProjectileDataRecord projectile,
        Optional<DualieRollDataRecord> dualieRoll,
        Optional<DualieTurretDataRecord> dualieTurret,
        Optional<ChargerDataRecord> charger
    )
    {
        public static final Codec<DataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        ProjectileDataRecord.CODEC.fieldOf("projectile").forGetter(DataRecord::projectile),
                        DualieRollDataRecord.CODEC.optionalFieldOf("dualie_roll").forGetter(DataRecord::dualieRoll),
                        DualieTurretDataRecord.CODEC.optionalFieldOf("dualie_turret").forGetter(DataRecord::dualieTurret),
                        ChargerDataRecord.CODEC.optionalFieldOf("charger").forGetter(DataRecord::charger)
                ).apply(instance, DataRecord::new)
        );
    }

    record ProjectileDataRecord(
            float size,
            float speed,
            Optional<Integer> lifespan,
            Optional<Integer> count,
            int firingSpeed,
            float groundInaccuracy,
            float airInaccuracy,
            float inkConsumption,
            int inkRecoveryCooldown,
            float baseDamage,
            float minDamage,
            int damageDecayStartTick,
            float damageDecayPerTick
    )
    {
        public static final Codec<ProjectileDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("size").forGetter(ProjectileDataRecord::size),
                        Codec.FLOAT.fieldOf("speed").forGetter(ProjectileDataRecord::speed),
                        Codec.INT.optionalFieldOf("lifespan").forGetter(ProjectileDataRecord::lifespan),
                        Codec.INT.optionalFieldOf("count").forGetter(ProjectileDataRecord::count),
                        Codec.INT.fieldOf("firing_speed").forGetter(ProjectileDataRecord::firingSpeed),
                        Codec.FLOAT.fieldOf("ground_inaccuracy").forGetter(ProjectileDataRecord::groundInaccuracy),
                        Codec.FLOAT.fieldOf("air_inaccuracy").forGetter(ProjectileDataRecord::airInaccuracy),
                        Codec.FLOAT.fieldOf("ink_consumption").forGetter(ProjectileDataRecord::inkConsumption),
                        Codec.INT.fieldOf("ink_recovery_cooldown").forGetter(ProjectileDataRecord::inkRecoveryCooldown),
                        Codec.FLOAT.fieldOf("base_damage").forGetter(ProjectileDataRecord::baseDamage),
                        Codec.FLOAT.fieldOf("min_damage").forGetter(ProjectileDataRecord::minDamage),
                        Codec.INT.fieldOf("damage_decay_start_tick").forGetter(ProjectileDataRecord::damageDecayStartTick),
                        Codec.FLOAT.fieldOf("damage_decay_per_tick").forGetter(ProjectileDataRecord::damageDecayPerTick)
                ).apply(instance, ProjectileDataRecord::new)
        );
    }

    public record DualieRollDataRecord(
            int count,
            float speed,
            float inkConsumption,
            int duration,
            int finalRollDuration
    )
    {
        public static final Codec<DualieRollDataRecord> CODEC = RecordCodecBuilder.create(
        instance -> instance.group(
            Codec.INT.fieldOf("count").forGetter(DualieRollDataRecord::count),
            Codec.FLOAT.fieldOf("speed").forGetter(DualieRollDataRecord::speed),
            Codec.FLOAT.fieldOf("ink_consumption").forGetter(DualieRollDataRecord::inkConsumption),
            Codec.INT.fieldOf("duration").forGetter(DualieRollDataRecord::duration),
            Codec.INT.fieldOf("final_roll_duration").forGetter(DualieRollDataRecord::finalRollDuration)
                ).apply(instance, DualieRollDataRecord::new)
        );
    }

    public record DualieTurretDataRecord(
            float fireInaccuracy,
            Optional<Float> baseDamage,
            Optional<Float> minDamage,
            Optional<Integer> damageDecayStartTick,
            Optional<Float> damageDecayPerTick
    )
    {
        public static final Codec<DualieTurretDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("fire_inaccuracy").forGetter(DualieTurretDataRecord::fireInaccuracy),
                        Codec.FLOAT.optionalFieldOf("base_damage").forGetter(DualieTurretDataRecord::baseDamage),
                        Codec.FLOAT.optionalFieldOf("min_damage").forGetter(DualieTurretDataRecord::minDamage),
                        Codec.INT.optionalFieldOf("damage_decay_start_tick").forGetter(DualieTurretDataRecord::damageDecayStartTick),
                        Codec.FLOAT.optionalFieldOf("damage_decay_per_tick").forGetter(DualieTurretDataRecord::damageDecayPerTick)
                ).apply(instance, DualieTurretDataRecord::new)
        );
    }

    public record ChargerDataRecord(
        float chargedDamage,
        float mobility,
        boolean fastMidairCharge,
        float piercesAtCharge
    )
    {
        public static final Codec<ChargerDataRecord> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        Codec.FLOAT.fieldOf("charged_damage").forGetter(ChargerDataRecord::chargedDamage),
                        Codec.FLOAT.fieldOf("mobility").forGetter(ChargerDataRecord::mobility),
                        Codec.BOOL.fieldOf("fast_midair_charge").forGetter(ChargerDataRecord::fastMidairCharge),
                        Codec.FLOAT.fieldOf("pierces_at_charge").forGetter(ChargerDataRecord::piercesAtCharge)
                ).apply(instance, ChargerDataRecord::new)
        );
    }
}
