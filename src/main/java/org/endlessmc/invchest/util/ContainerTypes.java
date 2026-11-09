package org.endlessmc.invchest.util;

/**
 * Shared container type checks used by commands and listeners.
 */
public final class ContainerTypes {
    private ContainerTypes() {
    }

    public static boolean isSupported(String name) {
        if (name == null) {
            return false;
        }
        if ("CHEST".equals(name) || "TRAPPED_CHEST".equals(name) || "BARREL".equals(name)) {
            return true;
        }
        return isShulkerBox(name);
    }

    public static boolean isShulkerBox(String name) {
        if (name == null) {
            return false;
        }
        if ("SHULKER_BOX".equals(name)) {
            return true;
        }
        return name.endsWith("_SHULKER_BOX");
    }

    public static boolean isDoubleChest(String name) {
        return "CHEST".equals(name) || "TRAPPED_CHEST".equals(name);
    }
}
