package org.endlessmc.invchest.task;

import org.endlessmc.invchest.InvChest;
import org.endlessmc.invchest.storage.BoundContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransferScheduler {
    private final InvChest plugin;
    private BukkitTask task;

    public TransferScheduler(InvChest plugin) {
        this.plugin = plugin;
    }

    public void start(int intervalSeconds) {
        stop();
        long ticks = intervalSeconds * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                iterate();
            }
        }, ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void iterate() {
        if (plugin.store().isGlobalPaused()) {
            return;
        }
        for (Map.Entry<UUID, List<BoundContainer>> e : plugin.store().snapshot().entrySet()) {
            UUID uid = e.getKey();
            Player p = Bukkit.getPlayer(uid);
            if (p == null) continue;
            List<BoundContainer> list = e.getValue();
            for (BoundContainer bc : list) {
                if (bc.paused) continue;
                Location loc = bc.toLocation();
                if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    continue;
                }
                if (!loc.getBlock().getType().name().equals(bc.type)) {
                    plugin.store().unbind(uid, bc);
                    plugin.store().save();
                    if (bc.chunkRange >= 0 && plugin.chunkLoader().isSupported()) {
                        plugin.chunkLoader().release(loc, bc.chunkRange);
                    }
                    p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, "container.missing.auto_unbound", null)));
                    continue;
                }
                Inventory inv = containerInventory(loc);
                if (inv == null) continue;
                int moved = transfer(p, inv, plugin.config().getTransferMode(), plugin.config().getMaxItemsPerTransfer());
                if (moved > 0 && plugin.config().isTransferMessageEnabled()) {
                    String key = plugin.config().getTransferMode().equals("all") ? "transfer.complete.all" : "transfer.complete.try";
                    p.sendMessage(color(plugin.lang().get(p, "prefix", null) + plugin.lang().get(p, key, null)));
                }
            }
        }
    }

    private Inventory containerInventory(Location loc) {
        BlockState state = loc.getBlock().getState();
        if (state instanceof InventoryHolder) {
            return ((InventoryHolder) state).getInventory();
        }
        return null;
    }

    private int transfer(Player p, Inventory source, String mode, int maxItems) {
        boolean all = "all".equals(mode);
        int moved = 0;
        int processed = 0;
        for (int i = 0; i < source.getSize() && processed < maxItems; i++) {
            ItemStack it = source.getItem(i);
            if (it == null) continue;

            int amount = Math.min(it.getAmount(), maxItems - processed);
            ItemStack attempt = it.clone();
            attempt.setAmount(amount);
            Map<Integer, ItemStack> left = p.getInventory().addItem(attempt);
            int leftover = 0;
            for (ItemStack rem : left.values()) {
                leftover += rem.getAmount();
            }
            int transferred = amount - leftover;

            // all 模式：背包放不下的部分直接掉落在玩家位置
            if (all && leftover > 0) {
                for (ItemStack rem : left.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), rem);
                }
            }

            // try 模式：背包完全无法接收当前物品时停止
            if (!all && transferred == 0) {
                break;
            }

            int sourceRemaining = it.getAmount() - amount;
            if (sourceRemaining <= 0) {
                source.setItem(i, null);
            } else {
                ItemStack updated = it.clone();
                updated.setAmount(sourceRemaining);
                source.setItem(i, updated);
            }

            moved += transferred;
            processed += amount;

            if (!all && leftover > 0) {
                break;
            }
        }
        return moved;
    }

    private String color(String s) {
        return s.replace('&', '§');
    }
}
