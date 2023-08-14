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

public final class AutoTool extends JavaPlugin {

    private static AutoTool plugin;
    private static String NMS_VERSION;
    private static Class craftItemStack;
    private static Method asNMSCopy;
    private static Method getNMSBlock;
    private static Method getItem;
    private static Method getDestroySpeed;
    private static Class item;

    @Override
    public void onEnable() {
        plugin = this;

        String v = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = v.substring(v.lastIndexOf('.') + 1);

        try {
            Class craftBlockClass = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".block.CraftBlock");
            Method[] craftBlockMethods = craftBlockClass.getDeclaredMethods();
            for (Method method : craftBlockMethods) {
                if (method.getParameterCount() == 0 && (method.getName().equals("getData0") || method.getName().equals("getNMS") || method.getName().equals("getNMSBlock"))) {
                    getNMSBlock = method;
                    break;
                }
            }
            getNMSBlock.setAccessible(true);

            craftItemStack = Class.forName("org.bukkit.craftbukkit." + NMS_VERSION + ".inventory.CraftItemStack");
            asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
            asNMSCopy.setAccessible(true);
            ItemStack itemStack = new ItemStack(Material.STONE);
            Object nmsItemStack = asNMSCopy.invoke(craftItemStack, itemStack);
            Class nmsItemStackClass = nmsItemStack.getClass();
            Method[] nmsItemStackMethods = nmsItemStackClass.getDeclaredMethods();
            for (Method method : nmsItemStackMethods) {
                if (method.getName().equals("getItem")) {
                    getItem = method;
                    break;
                } else if (method.getParameterCount() == 0) {
                    String returnType = method.getReturnType().getName();
                    returnType = returnType.substring(returnType.lastIndexOf('.') + 1);
                    if (returnType.equals("Item")) {
                        getItem = method;
                        break;
                    }
                }
            }
            getItem.setAccessible(true);
            Object nmsItem = getItem.invoke(nmsItemStack); //nms material
            Class itemClass = nmsItem.getClass();
            item = getSuperClass(itemClass);
            Method[] itemClassMethods = item.getDeclaredMethods();
            for (Method method : itemClassMethods) {
                if (method.getName().equals("getDestroySpeed")) {
                    getDestroySpeed = method;
                    break;
                } else {
                    if (method.getReturnType() == float.class && method.getParameterCount() == 2) {
                        Class<?>[] parameters = { nmsItemStackClass, getNMSBlock.getReturnType() };
                        Class<?>[] methodParams = method.getParameterTypes();
                        if (Arrays.equals(methodParams, parameters)) {
                            getDestroySpeed = method;
                            break;
                        }
                    }
                }
            }
            getDestroySpeed.setAccessible(true);
        } catch (Exception e) {
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
                Object nmsItemStack = asNMSCopy.invoke(craftItemStack, itemStack);
                Object nmsItem = getItem.invoke(nmsItemStack); //nms material

                Object nmsBlockData = getNMSBlock.invoke(block);
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
