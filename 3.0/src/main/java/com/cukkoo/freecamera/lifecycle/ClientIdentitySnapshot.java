package com.cukkoo.freecamera.lifecycle;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.UUID;

public final class ClientIdentitySnapshot {
    public enum ValidationResult {
        VALID,
        LEVEL_MISSING,
        PLAYER_MISSING,
        CONNECTION_MISSING,
        LEVEL_REPLACED,
        PLAYER_REPLACED,
        PLAYER_UUID_CHANGED,
        CONNECTION_REPLACED,
        DIMENSION_CHANGED,
        PLAYER_DIED,
        SPECTATOR_ENTERED,
        PERSPECTIVE_CHANGED
    }

    private final ClientLevel level;
    private final LocalPlayer player;
    private final UUID playerUuid;
    private final ClientPacketListener connection;
    private final ResourceKey<Level> dimension;
    private final CameraType perspective;

    private ClientIdentitySnapshot(
            ClientLevel level,
            LocalPlayer player,
            UUID playerUuid,
            ClientPacketListener connection,
            ResourceKey<Level> dimension,
            CameraType perspective
    ) {
        this.level = level;
        this.player = player;
        this.playerUuid = playerUuid;
        this.connection = connection;
        this.dimension = dimension;
        this.perspective = perspective;
    }

    public static ClientIdentitySnapshot capture(Minecraft client) {
        ClientLevel level = Objects.requireNonNull(client.level, "level");
        LocalPlayer player = Objects.requireNonNull(client.player, "player");
        ClientPacketListener connection = Objects.requireNonNull(client.getConnection(), "connection");
        return new ClientIdentitySnapshot(
                level,
                player,
                player.getUUID(),
                connection,
                level.dimension(),
                client.options.getCameraType()
        );
    }

    public ValidationResult validate(Minecraft client) {
        if (client.level == null) return ValidationResult.LEVEL_MISSING;
        if (client.player == null) return ValidationResult.PLAYER_MISSING;
        if (client.getConnection() == null) return ValidationResult.CONNECTION_MISSING;
        if (client.level != level) return ValidationResult.LEVEL_REPLACED;
        if (client.player != player) return ValidationResult.PLAYER_REPLACED;
        if (!client.player.getUUID().equals(playerUuid)) return ValidationResult.PLAYER_UUID_CHANGED;
        if (client.getConnection() != connection) return ValidationResult.CONNECTION_REPLACED;
        if (!client.level.dimension().equals(dimension)) return ValidationResult.DIMENSION_CHANGED;
        if (!client.player.isAlive()) return ValidationResult.PLAYER_DIED;
        if (client.player.isSpectator()) return ValidationResult.SPECTATOR_ENTERED;
        if (client.options.getCameraType() != perspective) return ValidationResult.PERSPECTIVE_CHANGED;
        return ValidationResult.VALID;
    }
}
