package com.cukkoo.freecamera.follow;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;

public final class FollowTargetTracker {
    public static final double DEFAULT_RANGE = 128.0;
    private double range=DEFAULT_RANGE;
    private boolean players=true,mobs=true,boats=true,minecarts=true,armorStands=true,vehicles=true;

    public void configure(double range,boolean players,boolean mobs,boolean boats,boolean minecarts,boolean armorStands,boolean vehicles){
        this.range=Math.clamp(range,8,256);this.players=players;this.mobs=mobs;this.boats=boats;this.minecarts=minecarts;this.armorStands=armorStands;this.vehicles=vehicles;
    }

    public Entity select(Minecraft client, double x, double y, double z, float yaw, float pitch) {
        if (client.level == null || client.player == null) return null;
        Vec3 from = new Vec3(x, y, z);
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        Vec3 to = from.add(-Math.sin(yawRad) * Math.cos(pitchRad) * range,
                -Math.sin(pitchRad) * range,
                Math.cos(yawRad) * Math.cos(pitchRad) * range);
        Entity best = null;
        double bestDistance = range * range;
        AABB searchBounds = new AABB(from, to).inflate(1.0);
        for (Entity entity : client.level.getEntities((Entity) null, searchBounds, entity -> valid(client, entity))) {
            AABB box = entity.getBoundingBox().inflate(0.15);
            java.util.Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double distance = hit.get().distanceToSqr(from);
                if (distance < bestDistance) { bestDistance = distance; best = entity; }
            }
        }
        return best;
    }

    public boolean valid(Minecraft client, Entity entity) {
        return entity != null && entity.level() == client.level && !entity.isRemoved()
                && entity.isAlive() && !entity.isInvisibleTo(client.player)
                && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb)
                && !(entity instanceof Projectile) && !(entity instanceof AreaEffectCloud)
                && included(entity) && client.getEntityRenderDispatcher().getRenderer(entity) != null;
    }

    private boolean included(Entity entity){
        if(entity instanceof Player)return players;
        if(entity instanceof ArmorStand)return armorStands;
        if(entity instanceof AbstractBoat)return boats;
        if(entity instanceof AbstractMinecart)return minecarts;
        if(entity instanceof LivingEntity)return mobs;
        return vehicles && entity.isVehicle();
    }
}
