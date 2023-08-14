package me.usainsrht.autotool;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;

public final class AutoTool extends JavaPlugin {

    private static AutoTool plugin;
    private static String NMS_VERSION;
    private static String getNMSBlockMethodName;

    @Override
    public void onEnable() {
        plugin = this;

        String v = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = v.substring(v.lastIndexOf('.') + 1);

        try {
            Class craftBlockClass = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".block.CraftBlock");
            Method[] methods = craftBlockClass.getDeclaredMethods();
            if (Arrays.stream(methods).anyMatch(method -> method.getName().equals("getData0"))) {
                getNMSBlockMethodName = "getData0";
            } else if (Arrays.stream(methods).anyMatch(method -> method.getName().equals("getNMS"))) {
                getNMSBlockMethodName = "getNMS";
            } else getNMSBlockMethodName = "getNMSBlock";
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(new BlockDamageListener(), this);
    }

    @Override
    public void onDisable() {

    }

    public static AutoTool getPlugin() {
        return plugin;
    }

    public static void autoTool(Player entity, Block block) {
        PlayerInventory inventory = entity.getInventory();
        float highest = 0;
        int highestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType() == Material.AIR || block.getDrops(itemStack).isEmpty()) continue;
            try {
                Class craftItemStackClass = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".inventory.CraftItemStack");
                Method asNMSCopy = craftItemStackClass.getDeclaredMethod("asNMSCopy", ItemStack.class);
                Object nmsItemStack = asNMSCopy.invoke(craftItemStackClass, itemStack);
                Method getItem = nmsItemStack.getClass().getDeclaredMethod("getItem");
                Object nmsItem = getItem.invoke(nmsItemStack); //nms material
                Class itemClass = nmsItem.getClass();
                itemClass = getSuperClass(itemClass);

                Method getNMSBlockData = block.getClass().getDeclaredMethod(getNMSBlockMethodName);
                getNMSBlockData.setAccessible(true);
                Object nmsBlockData = getNMSBlockData.invoke(block);
                //Method getDestroySpeed = itemClass.getMethod("getDestroySpeed", getSuperClass(nmsItemStack.getClass()), getSuperClass(nmsBlockData.getClass()));
                Method getDestroySpeed = Stream.of(itemClass.getDeclaredMethods())
                        .filter((m) -> m.getName().equals("getDestroySpeed"))
                        .findFirst()
                        .get();
                float destroySpeed = (float) getDestroySpeed.invoke(nmsItem, nmsItemStack, nmsBlockData);
                if (destroySpeed > highest) {
                    highest = destroySpeed;
                    highestSlot = i;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (highestSlot == -1) return;
        inventory.setHeldItemSlot(highestSlot);
    }

    public static Class getSuperClass(Object obj) {
        Class claz;
        if (obj instanceof Class) {
            claz = (Class) obj;
        } else claz = obj.getClass();
        Class clazz = claz;
        while (clazz != null) {
            clazz = clazz.getSuperclass();
            if (clazz == null || clazz == Object.class || Modifier.isAbstract(clazz.getModifiers())) break;
            claz = clazz;
        }
        return claz;
    }
}
