package org.endlessmc.invchest.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BindingStore {
    private final Plugin plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;
    private final Map<UUID, List<BoundContainer>> byPlayer = new HashMap<>();
    private final Map<String, UUID> byLocationKey = new HashMap<>();
    private boolean globalPaused = false;

    public BindingStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bind.json");
    }

    public void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            save();
            return;
        }
        
        // Try to load as new structure (object with "paused" and "data")
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root != null) {
                if (root.has("paused")) {
                    this.globalPaused = root.get("paused").getAsBoolean();
                }
                
                if (root.has("data")) {
                    Type typeList = new TypeToken<Map<String, List<BoundContainer>>>(){}.getType();
                    Map<String, List<BoundContainer>> rawList = gson.fromJson(root.get("data"), typeList);
                    loadFromMap(rawList, false);
                    return; // Loaded successfully
                }
                
                // If it's a map (old format), it won't have "data" usually, but JsonObject parse might succeed if it's just keys
                // Let's try to parse as old map format if "data" is missing
            }
        } catch (Exception ignored) {}

        // Fallback: try legacy load (raw map of lists)
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type typeList = new TypeToken<Map<String, List<BoundContainer>>>(){}.getType();
            Map<String, List<BoundContainer>> rawList = gson.fromJson(reader, typeList);
            if (rawList != null) {
                loadFromMap(rawList, true);
                return;
            }
        } catch (Exception ignored) {}
        
        // Fallback: try legacy load (raw map of single objects)
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type typeSingle = new TypeToken<Map<String, BoundContainer>>(){}.getType();
            Map<String, BoundContainer> rawSingle = gson.fromJson(reader, typeSingle);
            byPlayer.clear();
            byLocationKey.clear();
            if (rawSingle != null) {
                for (Map.Entry<String, BoundContainer> e : rawSingle.entrySet()) {
                    UUID uid = UUID.fromString(e.getKey());
                    List<BoundContainer> list = new ArrayList<BoundContainer>();
                    if (e.getValue() != null) {
                        e.getValue().name = "绑定1"; // Default name for very old data
                        e.getValue().chunkRange = -1; // Default range
                        list.add(e.getValue());
                        byLocationKey.put(e.getValue().key(), uid);
                    }
                    byPlayer.put(uid, list);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadFromMap(Map<String, List<BoundContainer>> rawList, boolean legacy) {
        byPlayer.clear();
        byLocationKey.clear();
        if (rawList != null) {
            for (Map.Entry<String, List<BoundContainer>> e : rawList.entrySet()) {
                UUID uid = UUID.fromString(e.getKey());
                List<BoundContainer> list = e.getValue() == null ? new ArrayList<BoundContainer>() : e.getValue();
                byPlayer.put(uid, list);
                
                // Fix missing fields and populate index
                int idx = 1;
                for (BoundContainer bc : list) {
                    if (bc.name == null) {
                        bc.name = "绑定" + idx; // Default name
                    }
                    if (legacy && bc.chunkRange == 0) {
                        // 旧版数据没有 chunkRange 字段，Gson 会将其设为 0；
                        // 迁移时按“不常加载”处理，避免误加载区块。
                        bc.chunkRange = -1;
                    }
                    byLocationKey.put(bc.key(), uid);
                    idx++;
                }
            }
        }
    }

    public void save() {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            Map<String, Object> root = new HashMap<>();
            root.put("paused", globalPaused);
            
            Map<String, List<BoundContainer>> data = new HashMap<>();
            for (Map.Entry<UUID, List<BoundContainer>> e : byPlayer.entrySet()) {
                data.put(e.getKey().toString(), e.getValue());
            }
            root.put("data", data);
            
            gson.toJson(root, writer);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save bindings", e);
        }
    }

    public List<BoundContainer> getBounds(UUID player) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return new ArrayList<BoundContainer>();
        return new ArrayList<BoundContainer>(list);
    }

    public boolean bind(UUID player, BoundContainer container, int maxPerPlayer) {
        if (byLocationKey.containsKey(container.key())) return false;
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) {
            list = new ArrayList<BoundContainer>();
            byPlayer.put(player, list);
        }
        if (maxPerPlayer > 0 && list.size() >= maxPerPlayer) return false;
        
        // Ensure name uniqueness
        if (container.name != null) {
            for (BoundContainer b : list) {
                if (container.name.equals(b.name)) return false; // Name collision
            }
        }
        
        list.add(container);
        byLocationKey.put(container.key(), player);
        return true;
    }
    
    public String getNextAutoName(UUID player, String format) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return format.replace("{number}", "1");
        int i = 1;
        while (true) {
            String name = format.replace("{number}", String.valueOf(i));
            boolean exists = false;
            for (BoundContainer b : list) {
                if (name.equals(b.name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) return name;
            i++;
        }
    }
    
    public BoundContainer getBoundByName(UUID player, String name) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return null;
        for (BoundContainer b : list) {
            if (b.name != null && b.name.equals(name)) return b;
        }
        return null;
    }

    public void unbindAll(UUID player) {
        List<BoundContainer> list = byPlayer.remove(player);
        if (list != null) {
            for (BoundContainer bc : list) {
                byLocationKey.remove(bc.key());
            }
        }
    }

    public boolean unbind(UUID player, BoundContainer container) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return false;
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            // Match by key OR name if provided (but container arg usually comes from location logic which has key)
            if (bc.key().equals(container.key())) {
                list.remove(i);
                byLocationKey.remove(bc.key());
                removed = true;
                break;
            }
        }
        if (list.isEmpty()) {
            byPlayer.remove(player);
        }
        return removed;
    }
    
    public boolean unbindByName(UUID player, String name) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return false;
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            if (bc.name != null && bc.name.equals(name)) {
                list.remove(i);
                byLocationKey.remove(bc.key());
                removed = true;
                break;
            }
        }
        if (list.isEmpty()) {
            byPlayer.remove(player);
        }
        return removed;
    }

    public boolean setPaused(UUID player, String name, boolean paused) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null) return false;
        for (BoundContainer bc : list) {
            if (bc.name != null && bc.name.equals(name)) {
                bc.paused = paused;
                return true;
            }
        }
        return false;
    }

    public boolean setLocked(UUID player, String name, boolean locked) {
        BoundContainer container = getBoundByName(player, name);
        if (container == null) return false;
        container.locked = locked;
        return true;
    }

    public BoundContainer getBoundAt(Location loc) {
        UUID owner = ownerOf(loc);
        if (owner == null) return null;
        String key = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0).key();
        for (BoundContainer bc : getBounds(owner)) {
            if (bc.key().equals(key)) return bc;
        }
        return null;
    }

    public boolean setAllPaused(UUID player, boolean paused) {
        List<BoundContainer> list = byPlayer.get(player);
        if (list == null || list.isEmpty()) return false;
        for (BoundContainer bc : list) {
            bc.paused = paused;
        }
        return true;
    }

    public boolean setAllPaused(boolean paused) {
        if (byPlayer.isEmpty()) return false;
        for (List<BoundContainer> list : byPlayer.values()) {
            for (BoundContainer bc : list) {
                bc.paused = paused;
            }
        }
        return true;
    }

    public UUID ownerOf(Location loc) {
        // Just for key generation
        BoundContainer bc = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0);
        return byLocationKey.get(bc.key());
    }

    public Map<UUID, List<BoundContainer>> snapshot() {
        Map<UUID, List<BoundContainer>> snap = new HashMap<>();
        for (Map.Entry<UUID, List<BoundContainer>> e : byPlayer.entrySet()) {
            snap.put(e.getKey(), new ArrayList<BoundContainer>(e.getValue()));
        }
        return snap;
    }

    public boolean unbindByLocation(Location loc) {
        UUID owner = ownerOf(loc);
        if (owner == null) return false;
        List<BoundContainer> list = byPlayer.get(owner);
        if (list == null) return false;
        String key = new BoundContainer(loc, loc.getBlock().getType().name(), null, 0).key();
        boolean removed = false;
        for (int i = 0; i < list.size(); i++) {
            BoundContainer bc = list.get(i);
            if (bc.key().equals(key)) {
                list.remove(i);
                removed = true;
                break;
            }
        }
        byLocationKey.remove(key);
        if (list.isEmpty()) {
            byPlayer.remove(owner);
        }
        return removed;
    }
    
    public boolean isGlobalPaused() {
        return globalPaused;
    }
    
    public void setGlobalPaused(boolean paused) {
        this.globalPaused = paused;
        save();
    }
}
