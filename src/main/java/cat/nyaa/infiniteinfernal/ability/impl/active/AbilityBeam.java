package cat.nyaa.infiniteinfernal.ability.impl.active;

import cat.nyaa.infiniteinfernal.InfPlugin;
import cat.nyaa.infiniteinfernal.ability.ActiveAbility;
import cat.nyaa.infiniteinfernal.configs.ParticleConfig;
import cat.nyaa.infiniteinfernal.mob.IMob;
import cat.nyaa.infiniteinfernal.utils.Utils;
import com.google.common.util.concurrent.AtomicDouble;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbilityBeam extends ActiveAbility {
    @Serializable
    public int length = 10;
    @Serializable
    public ParticleConfig particle = new ParticleConfig();
    @Serializable
    public double speed = 1;
    @Serializable
    public double spawnsPerBlock = 2;
    double lengthPerSpawn = 1 / spawnsPerBlock;
    @Serializable
    public double cone = 0;
    @Serializable
    public boolean pierce = false;
    @Serializable
    public boolean ignoreWall = false;
    @Serializable
    public double range = 40;
    @Serializable
    public double damageMultiplier = 1;
    @Serializable
    public int burst = 1;
    @Serializable
    public int burstInterval = 10;

    private Set<Material> transp = Stream.of(Material.values())
            .filter(material -> material.isBlock())
            .filter(material -> !material.isSolid() || !material.isOccluding())
            .collect(Collectors.toSet());


    @Override
    public void active(IMob iMob) {
        LivingEntity mobEntity = iMob.getEntity();
        Utils.getValidTargets(iMob, iMob.getEntity().getNearbyEntities(range, range, range))
                .forEach(entity -> {
                    for (int i = 0; i < burst; i++) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Vector eyeLocation = mobEntity.getEyeLocation().getDirection();
                                Vector conedDir = Utils.cone(eyeLocation, cone);
                                beam(iMob, conedDir);
                            }
                        }.runTaskLater(InfPlugin.plugin, i * burstInterval);
                    }
                });
    }

    public void beam(IMob from, Vector direction) {
        new MovingTask(from, direction).runTask(InfPlugin.plugin);
    }

    private NamespacedKey getNamespacedKey() {
        return new NamespacedKey(InfPlugin.plugin, getName());
    }

    @Override
    public String getName() {
        return "Beam";
    }

    private class MovingTask extends BukkitRunnable {
        private IMob iMob;
        private Vector direction;
        private int length = AbilityBeam.this.length;
        private Particle particleType = AbilityBeam.this.particle.type;
        private double offsetX = AbilityBeam.this.particle.delta.get(0);
        private double offsetY = AbilityBeam.this.particle.delta.get(1);
        private double offsetZ = AbilityBeam.this.particle.delta.get(2);
        private double particleSpeedExtra = AbilityBeam.this.particle.speed;
        private double speed = AbilityBeam.this.speed;
        private double spawnsPerBlock = AbilityBeam.this.spawnsPerBlock;
        private double lengthPerSpawn = 1 / spawnsPerBlock;
        private boolean pierce = AbilityBeam.this.pierce;
        private boolean ignoreWall = AbilityBeam.this.ignoreWall;
        private double damage;

        public MovingTask(IMob iMob, Vector direction) {
            this.iMob = iMob;
            damage = AbilityBeam.this.damageMultiplier * iMob.getDamage();
            this.direction = direction;
        }

        private void spawnParticle(LivingEntity from, World world, Location lastLocation, int i) {
            if ((lastLocation.distance(from.getEyeLocation()) < 1)) {
                return;
            }
//        if (from instanceof Player) {
//            ((Player) from).spawnParticle(this.particleType, lastLocation, i / 2, offsetX, offsetY, offsetZ, speed, extraData);
//        }
            world.spawnParticle(this.particleType, lastLocation, i, offsetX, offsetY, offsetZ, particleSpeedExtra, Utils.parseExtraData(particle.extraData), particle.forced);
        }

        private boolean canHit(Location loc, Entity entity) {
            BoundingBox boundingBox = entity.getBoundingBox();
            BoundingBox particleBox;
            double x = Math.max(offsetX, 0.1);
            double y = Math.max(offsetY, 0.1);
            double z = Math.max(offsetZ, 0.1);
            particleBox = BoundingBox.of(loc, x + 0.1, y + 0.1, z + 0.1);
            return boundingBox.overlaps(particleBox) || particleBox.overlaps(boundingBox);
        }

        private boolean tryHit(LivingEntity from, Location loc, boolean canHitSelf, double damage) {
            double offsetLength = new Vector(offsetX, offsetY, offsetZ).length();
            double length = Double.isNaN(offsetLength) ? 0 : Math.max(offsetLength, 10);
            Collection<Entity> candidates = from.getWorld().getNearbyEntities(loc, length, length, length);
            boolean result = false;
            if (!pierce) {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (canHitSelf || !entity.equals(from)) && !entity.isDead())
                        .filter(entity -> canHit(loc, entity))
                        .limit(1)
                        .collect(Collectors.toList());
                if (!collect.isEmpty()) {
                    Entity entity = collect.get(0);
                    if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).damage(damage, from);
                    }
                    return true;
                }
            } else {
                List<Entity> collect = candidates.stream()
                        .filter(entity -> (entity instanceof LivingEntity) && (canHitSelf || !entity.equals(from)))
                        .filter(entity -> canHit(loc, entity))
                        .collect(Collectors.toList());

                if (!collect.isEmpty()) {
                    collect.stream()
                            .map(entity -> ((LivingEntity) entity))
                            .forEach(livingEntity -> {
                                livingEntity.damage(damage, from);
                            });
                    result = true;
                }
            }
            return result;
        }


        @Override
        public void run() {
            LivingEntity from = iMob.getEntity();
            try {
                World world = from.getWorld();
                if (Double.isInfinite(lengthPerSpawn)) {
                    return;
                }
                Location lastLocation = from.getEyeLocation();
                int totalSteps = (int) Math.round(length / lengthPerSpawn);
                final double lengthPerTick = speed / 20;
                AtomicDouble lengthRemains = new AtomicDouble(0);
                AtomicInteger currentStep = new AtomicInteger(0);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (currentStep.getAndAdd(1) >= totalSteps) {
                            this.cancel();
                        }
                        double lengthInThisTick = lengthPerTick + lengthRemains.get();
                        while ((lengthInThisTick -= lengthPerSpawn) > 0) {
                            boolean isHit = tryHit(from, lastLocation, false, damage);
                            Block block = lastLocation.getBlock();
                            if (block.getChunk().isLoaded()) {
                                if (transp.contains(block.getType())) {
                                    spawnParticle(from, world, lastLocation, 1);
                                } else if (!ignoreWall) {
                                    this.cancel();
                                } else {
                                    spawnParticle(from, world, lastLocation, 1);
                                }
                            }
                            Vector step = direction.normalize().multiply(lengthPerSpawn);
                            lastLocation.add(step);
                            if (isHit && !pierce) {
                                this.cancel();
                            }
                        }
                        lengthRemains.set(lengthInThisTick + lengthPerSpawn);
                    }
                }.runTaskTimer(InfPlugin.plugin, 0, 1);
            } catch (Exception e) {
                this.cancel();
            }
        }
    }
}
