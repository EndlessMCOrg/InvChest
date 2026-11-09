package org.endlessmc.invchest.util;

import org.endlessmc.invchest.InvChest;
import org.endlessmc.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages chunk force-loading logic.
 * Downgrades gracefully on versions that do not support setForceLoaded (pre-1.13.1).
 */
public class ChunkLoader {
    private final InvChest plugin;
    private final boolean supported;
    private Method setForceLoadedMethod;
    private Method isForceLoadedMethod;
    private final Map<String, ChunkRef> forcedChunks = new HashMap<>();

    public ChunkLoader(InvChest plugin) {
        this.plugin = plugin;
        this.supported = checkSupport();
    }

    private boolean checkSupport() {
        try {
            setForceLoadedMethod = Chunk.class.getMethod("setForceLoaded", boolean.class);
            isForceLoadedMethod = Chunk.class.getMethod("isForceLoaded");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public boolean isSupported() {
        return supported;
    }

    private boolean setForceLoaded(Chunk chunk, boolean force) {
        if (!supported || setForceLoadedMethod == null) return false;
        try {
            setForceLoadedMethod.invoke(chunk, force);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to update chunk force-load state", e);
            return false;
        }
    }

    private boolean isForceLoaded(Chunk chunk) {
        if (!supported || isForceLoadedMethod == null) return false;
        try {
            return (boolean) isForceLoadedMethod.invoke(chunk);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to read chunk force-load state", e);
            return false;
        }
    }

    private String chunkKey(World world, int cx, int cz) {
        String name = world.getName();
        return name.length() + ":" + name + ":" + cx + ":" + cz;
    }

    private void applyForceLoaded(World world, int cx, int cz, boolean force) {
        String key = chunkKey(world, cx, cz);
        Chunk chunk = world.getChunkAt(cx, cz);
        if (force) {
            boolean alreadyLoaded = isForceLoaded(chunk);
            if (setForceLoaded(chunk, true) && !alreadyLoaded) {
                forcedChunks.put(key, new ChunkRef(world, cx, cz));
            }
        } else {
            if (!forcedChunks.containsKey(key)) return;
            if (setForceLoaded(chunk, false)) {
                forcedChunks.remove(key);
            }
        }
    }

    /**
     * Applies force load to chunks around the location within range.
     * Only works if supported and range >= 0.
     * @param loc Center location
     * @param range Radius (0 = self, >0 = radius)
     * @return true if applied, false if not supported or range < 0
     */
    public boolean keepLoaded(Location loc, int range) {
        if (loc == null || !supported || range < 0) return false;
        if (!plugin.config().isChunkLoadingEnabled()) return false;

        World w = loc.getWorld();
        if (w == null) return false;
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        int maxRange = plugin.config().getMaxChunkRange();
        if (range > maxRange) range = maxRange;

        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                applyForceLoaded(w, x, z, true);
            }
        }
        return true;
    }

    /**
     * Releases force load for chunks around the location within range.
     * @param loc Center location
     * @param range Radius
     */
    public void release(Location loc, int range) {
        release(loc, range, null);
    }

    /**
     * Releases force load for chunks around the location within range.
     * @param loc Center location
     * @param range Radius
     * @param excludePlayer player whose bindings should be ignored (e.g. on quit)
     */
    public void release(Location loc, int range, UUID excludePlayer) {
        if (loc == null || !supported || range < 0) return;

        World w = loc.getWorld();
        if (w == null) return;
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        int maxRange = plugin.config().getMaxChunkRange();
        if (range > maxRange) range = maxRange;

        for (int x = cx - range; x <= cx + range; x++) {
            for (int z = cz - range; z <= cz + range; z++) {
                if (!forcedChunks.containsKey(chunkKey(w, x, z))) {
                    continue;
                }
                if (shouldKeepLoaded(w, x, z, excludePlayer)) {
                    continue;
                }
                applyForceLoaded(w, x, z, false);
            }
        }
    }

    private boolean shouldKeepLoaded(World w, int cx, int cz, UUID excludePlayer) {
        // Check all online players' bindings
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (excludePlayer != null && excludePlayer.equals(p.getUniqueId())) {
                continue;
            }
            List<BoundContainer> bounds = plugin.store().getBounds(p.getUniqueId());
            for (BoundContainer bc : bounds) {
                if (bc.chunkRange < 0) continue;
                Location loc = bc.toLocation();
                if (loc == null || !loc.getWorld().equals(w)) continue;

                int bcx = loc.getBlockX() >> 4;
                int bcz = loc.getBlockZ() >> 4;
                int r = Math.min(bc.chunkRange, plugin.config().getMaxChunkRange());

                if (cx >= bcx - r && cx <= bcx + r && cz >= bcz - r && cz <= bcz + r) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updatePlayer(UUID uuid, boolean online) {
        if (!supported) return;
        List<BoundContainer> list = plugin.store().getBounds(uuid);
        for (BoundContainer bc : list) {
            if (bc.chunkRange < 0) continue;
            Location loc = bc.toLocation();
            if (loc == null) continue;
            if (online) {
                keepLoaded(loc, bc.chunkRange);
            } else {
                release(loc, bc.chunkRange, uuid);
            }
        }
    }

    public void unloadAll() {
        if (!supported) return;
        for (ChunkRef ref : new ArrayList<ChunkRef>(forcedChunks.values())) {
            setForceLoaded(ref.world.getChunkAt(ref.x, ref.z), false);
        }
        forcedChunks.clear();
    }

    public void refreshAll() {
        if (!supported) return;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            updatePlayer(p.getUniqueId(), true);
        }
    }

    private static final class ChunkRef {
        private final World world;
        private final int x;
        private final int z;

        private ChunkRef(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }
    }
}
