package org.endlessmc.invchest.listener;

import org.endlessmc.invchest.InvChest;
import org.endlessmc.invchest.storage.BoundContainer;
import org.endlessmc.invchest.util.ContainerTypes;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContainerProtectionListener implements Listener {
    private final InvChest plugin;

    public ContainerProtectionListener(InvChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        if (holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            BoundContainer container = boundContainer(holderLocation(dc.getLeftSide()));
            if (container == null) container = boundContainer(holderLocation(dc.getRightSide()));
            protectOpen(event, player, container);
            return;
        }
        protectOpen(event, player, boundContainer(holderLocation(holder)));
    }

    private void protectOpen(InventoryOpenEvent event, Player player, BoundContainer container) {
        if (container == null || !container.isLocked()) return;
        Location loc = container.toLocation();
        if (loc == null) return;
        UUID owner = plugin.store().ownerOf(loc);
        if (owner != null && owner.equals(player.getUniqueId())) return;
        event.setCancelled(true);
        sendProtectedMessage(player, owner);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        String name = b.getType().name();
        if (!ContainerTypes.isSupported(name)) return;
        Location targetLoc = boundLocation(b);
        if (targetLoc == null) return;
        UUID owner = plugin.store().ownerOf(targetLoc);
        if (owner == null) return;
        Player p = event.getPlayer();
        if (p.getUniqueId().equals(owner)) {
            BoundContainer bound = plugin.store().getBoundAt(targetLoc);
            plugin.store().unbindByLocation(targetLoc);
            plugin.store().save();
            releaseChunk(bound, targetLoc);
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "unbind.success", null)));
            return;
        }
        event.setCancelled(true);
        sendProtectedMessage(p, owner);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        Inventory src = event.getSource();
        if (isBoundHolder(src.getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private Location holderLocation(InventoryHolder holder) {
        if (holder instanceof BlockState) {
            return ((BlockState) holder).getLocation();
        }
        return null;
    }

    private boolean isBound(Location loc) {
        if (loc == null) return false;
        return plugin.store().ownerOf(loc) != null;
    }

    private boolean isBoundHolder(InventoryHolder holder) {
        if (holder instanceof DoubleChest) {
            DoubleChest dc = (DoubleChest) holder;
            if (isBound(holderLocation(dc.getLeftSide()))) return true;
            return isBound(holderLocation(dc.getRightSide()));
        }
        return isBound(holderLocation(holder));
    }

    private Location neighborBoundLocation(Block b) {
        String name = b.getType().name();
        if (!ContainerTypes.isDoubleChest(name)) return null;
        Block n1 = b.getRelative(1, 0, 0);
        Block n2 = b.getRelative(-1, 0, 0);
        Block n3 = b.getRelative(0, 0, 1);
        Block n4 = b.getRelative(0, 0, -1);
        Location l;
        l = n1.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n2.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n3.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        l = n4.getLocation();
        if (plugin.store().ownerOf(l) != null) return l;
        return null;
    }

    private Location boundLocation(Block b) {
        Location direct = b.getLocation();
        if (plugin.store().ownerOf(direct) != null) return direct;
        return neighborBoundLocation(b);
    }

    private BoundContainer boundContainer(Location loc) {
        if (loc == null) return null;
        return plugin.store().getBoundAt(loc);
    }

    private void sendProtectedMessage(Player p, UUID owner) {
        if (p.hasPermission("invchest.inf") && owner != null) {
            String name = ownerName(owner);
            String msg = plugin.lang().get(p, "container.protected.owner", null).replace("{player}", name);
            p.sendMessage(color(plugin.lang().get(p, "prefix", null) + msg));
            return;
        }
        p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.protected", null)));
    }

    private String ownerName(UUID owner) {
        try {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(owner);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Exception ignored) {}
        return owner.toString();
    }

    private void handleExplosion(List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;
        for (Block b : new ArrayList<Block>(blocks)) {
            String name = b.getType().name();
            if (!ContainerTypes.isSupported(name)) continue;
            Location targetLoc = boundLocation(b);
            if (targetLoc == null) continue;
            BoundContainer bound = plugin.store().getBoundAt(targetLoc);
            if (bound == null) continue;
            plugin.store().unbindByLocation(targetLoc);
            releaseChunk(bound, targetLoc);
        }
        plugin.store().save();
    }

    private void releaseChunk(BoundContainer bound, Location loc) {
        if (bound == null) return;
        if (bound.chunkRange < 0 || !plugin.chunkLoader().isSupported()) return;
        Location releaseLoc = loc != null ? loc : bound.toLocation();
        if (releaseLoc == null) return;
        plugin.chunkLoader().release(releaseLoc, bound.chunkRange);
    }

    private String color(String s) {
        return s.replace('&', '§');
    }
}
