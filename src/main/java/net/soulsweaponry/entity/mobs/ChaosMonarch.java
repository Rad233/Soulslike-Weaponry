package net.soulsweaponry.entity.mobs;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.soulsweaponry.config.ConfigConstructor;
import net.soulsweaponry.entity.ai.goal.ChaosMonarchGoal;
import net.soulsweaponry.items.ChaosSet;
import net.soulsweaponry.registry.EffectRegistry;
import net.soulsweaponry.registry.ItemRegistry;
import net.soulsweaponry.registry.SoundRegistry;
import net.soulsweaponry.registry.WeaponRegistry;
import net.soulsweaponry.util.CustomDeathHandler;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.IAnimationTickable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class ChaosMonarch extends BossEntity implements IAnimatable, IAnimationTickable {

    public AnimationFactory factory = GeckoLibUtil.createFactory(this);
    public int deathTicks;
    private int spawnTicks;
    private static final TrackedData<Integer> ATTACK = DataTracker.registerData(ChaosMonarch.class, TrackedDataHandlerRegistry.INTEGER);

    public ChaosMonarch(EntityType<? extends BossEntity> entityType, World world) {
        super(entityType, world, Color.PURPLE);
        this.setDrops(WeaponRegistry.WITHERED_WABBAJACK);
        this.setDrops(ItemRegistry.LORD_SOUL_VOID);
        this.setDrops(ItemRegistry.CHAOS_CROWN);
        this.setDrops(ItemRegistry.CHAOS_ROBES);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        switch (this.getAttack()) {
            case SPAWN:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("spawn"));
            break;
            case TELEPORT:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("teleport"));
            break;
            case MELEE:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("swing_staff"));
            break;
            case LIGHTNING:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("lightning_call"));
            break;
            case SHOOT:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("shoot"));
            break;
            case BARRAGE:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("barrage"));
            break;
            case DEATH:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("death"));
            break;
            default:
                event.getController().setAnimation(new AnimationBuilder().addAnimation("idle"));
            break;
        }
        return PlayState.CONTINUE;
    }

    @Override
	protected void initGoals() {
        this.goalSelector.add(1, new ChaosMonarchGoal(this));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, AccursedLordBoss.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(5, (new RevengeGoal(this, new Class[0])).setGroupRevenge());
		super.initGoals();
	}

    public static DefaultAttributeContainer.Builder createBossAttributes() {
        return HostileEntity.createHostileAttributes()
        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 60D)
        .add(EntityAttributes.GENERIC_MAX_HEALTH, ConfigConstructor.chaos_monarch_health)
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15D)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 20.0D)
        .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
        .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
        .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 2.0D);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ATTACK, 0);
    }

    @Override
    public int getTicksUntilDeath() {
        return 80;
    }

    @Override
    public int getDeathTicks() {
        return this.deathTicks;
    }

    @Override
    public void setDeath() {
        this.setAttack(7);
    }

    @Override
    public void updatePostDeath() {
        this.deathTicks++;
        if (this.deathTicks >= this.getTicksUntilDeath() && !this.world.isClient()) {
            this.world.sendEntityStatus(this, EntityStatuses.ADD_DEATH_PARTICLES);
            CustomDeathHandler.deathExplosionEvent(world, this.getBlockPos(), true, SoundRegistry.DAWNBREAKER_EVENT);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getAttack() == Attack.SPAWN) {
            this.spawnTicks++;
            DefaultParticleType[] dragonParticles = {ParticleTypes.DRAGON_BREATH, ParticleTypes.DRAGON_BREATH};
            DefaultParticleType[] portalParticles = {ParticleTypes.PORTAL};
            if (this.spawnTicks % 2 == 0 && this.spawnTicks < 20) {
                this.particleExplosion(portalParticles, 4f);
            }
            if (this.spawnTicks == 40) {
                this.particleExplosion(dragonParticles, .5f);
                this.world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.HOSTILE, 1f, 1f);
            }
            if (this.spawnTicks >= 60) {
                this.setAttack(0);;
            }
        }
    }

    /* static enum Test {
        ATTACK,
        SHOOT,
        IDLE,
        DEATH
    }
    public static final Test TESTING[] = Test.values();
    System.out.println(TESTING[MathHelper.floor(this.getHeadYaw() / 90.0 + 0.5) & 3]); */
    /* & <-- verifies both operands
    && <-- stops evaluating if the first operand evaluates to false since the result will be false */

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source == DamageSource.LIGHTNING_BOLT) {
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.hasStatusEffect(EffectRegistry.DECAY) && this.age % 10 == 0) this.heal(this.getStatusEffect(EffectRegistry.DECAY).getAmplifier() + 1 + this.getAttackingPlayers().size());
        if (this.hasStatusEffect(StatusEffects.LEVITATION)) this.removeStatusEffect(StatusEffects.LEVITATION);
        this.turnBlocks(this.world, this.getBlockPos());
    }

    private void turnBlocks(World world, BlockPos blockPos) {
        ChaosSet cape = (ChaosSet) ItemRegistry.CHAOS_ROBES;
        cape.turnBlocks(this, world, blockPos, 3);
    }

    private void particleExplosion(DefaultParticleType[] particles, float sizeModifier) {
        this.roundParticleOutburst(this.world, 1000, particles, this.getX(), this.getY() + 3, this.getZ(), sizeModifier);
    }

    public void roundParticleOutburst(World world, double points, DefaultParticleType[] particles, double x, double y, double z, float sizeModifier) {
        double phi = Math.PI * (3. - Math.sqrt(5.));
        for (int i = 0; i < points; i++) {
            double velocityY = 1 - (i/(points - 1)) * 2;
            double radius = Math.sqrt(1 - velocityY*velocityY);
            double theta = phi * i;
            double velocityX = Math.cos(theta) * radius;
            double velocityZ = Math.sin(theta) * radius;
            for (int j = 0; j < particles.length; j++) {
                world.addParticle(particles[j], true, x, y, z, velocityX*sizeModifier, velocityY*sizeModifier, velocityZ*sizeModifier);
            }
        } 
    }

    /**
     * Set the attack based on integer as Attack Id
     * <p>Idle: > 1 || 7 <
     * <p>Spawn: 1
     * <p>Teleport: 2
     * <p>Melee: 3
     * <p>Lightning: 4
     * <p>Shoot: 5
     * <p>Barrage 6
     * <p>Death: 7
     */
    public void setAttack(int attackId) {
        this.dataTracker.set(ATTACK, attackId);
    }

    /**
     * Turns the given integer previously to the datatracker into an enum for switch
     * statements.
     * 
     * <p> Was previously:
     * public int getAttack() {
     *      return this.dataTracker.get(ATTACK);
     * }
     * <p> Was changed to current function for clarity reasons.
     */
    
    public Attack getAttack() {
        return Attack.values()[this.dataTracker.get(ATTACK)];
    }

    public static enum Attack {
        IDLE,
        SPAWN,
        TELEPORT,
        MELEE,
        LIGHTNING,
        SHOOT,
        BARRAGE,
        DEATH
    }

    public Vec3d getRotationVec(float pitch, float yaw) {
        float f = pitch * ((float)Math.PI / 180);
        float g = -yaw * ((float)Math.PI / 180);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }
    
    @Override
    public boolean disablesShield() {
        return true;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    @Override
    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    @Override
    public int tickTimer() {
        return age;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 0, this::predicate));    
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_WITHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_WITHER_DEATH;
    }
}
