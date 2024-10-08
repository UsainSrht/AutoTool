package me.usainsrht.autotool;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class AutoTool extends JavaPlugin {

    private static AutoTool instance;
    private String NMS_VERSION; //ex v1_16_R3
    private String craftBukkitPackageName;
    private Class craftItemStack; //org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
    private Method asNMSCopy; //CraftItemStack.asNMSCopy(ItemStack)
    private Method getNMSBlock; //CraftBlock.getNMS()
    private Method getItem; //net.minecraft.server.world.item.ItemStack.getItem()
    private Method getDestroySpeed; //net.minecraft.server.world.item.Item.getDestroySpeed(ItemStack, Block)
    private Class item; //net.minecraft.server.world.item.Item

    @Override
    public void onEnable() {
        instance = this;

        setNMSVersion();
        setCraftBukkitPackageName();

        try {
            setNMSBlockMethod();
            setAsNMSCopyMethod();
            Object nmsItemStack = setGetItemMethod();
            setGetDestroySpeedMethod(nmsItemStack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(new BlockDamageListener(this), this);

        getCommand("autotool").setExecutor(new AutoToolCommand(this));

        saveDefaultConfig();
    }

    public static AutoTool getInstance() {
        return instance;
    }


    public boolean isAutoToolOn(Player player) {
        return !getConfig().getStringList("autotool_disabled").contains(player.getUniqueId().toString());
    }

    /*
     * Sets NMS version to get classes that has version in its name.
     */
    public void setNMSVersion() {
        String v = Bukkit.getServer().getClass().getPackage().getName();
        NMS_VERSION = v.substring(v.lastIndexOf('.') + 1);
    }

    /*
    * Sets craftbukkit package name according to nms version.
    * */
    public void setCraftBukkitPackageName() {
        if (NMS_VERSION == null || NMS_VERSION.equalsIgnoreCase("craftbukkit")) {
            craftBukkitPackageName = "org.bukkit.craftbukkit";
        } else {
            craftBukkitPackageName = "org.bukkit.craftbukkit." + NMS_VERSION;
        }
    }

    /*
    * Sets method to get NMSBlock to get destroy speed.
    */
    public void setNMSBlockMethod() throws ClassNotFoundException {
        Class craftBlockClass = Class.forName(craftBukkitPackageName + ".block.CraftBlock");
        Method[] craftBlockMethods = craftBlockClass.getDeclaredMethods();
        for (Method method : craftBlockMethods) {
            if (method.getParameterCount() == 0 && (method.getName().equals("getData0") || method.getName().equals("getNMS") || method.getName().equals("getNMSBlock"))) {
                getNMSBlock = method;
                break;
            }
        }
        getNMSBlock.setAccessible(true);
    }

    /*
     * Sets method to get CraftItemStack as NMSItemStack.
     */
    public void setAsNMSCopyMethod() throws ClassNotFoundException, NoSuchMethodException {
        craftItemStack = Class.forName(craftBukkitPackageName + ".inventory.CraftItemStack");
        asNMSCopy = craftItemStack.getDeclaredMethod("asNMSCopy", ItemStack.class);
        asNMSCopy.setAccessible(true);
    }

    /*
     * Sets method to get Item from NMSItemStack.
     * Item is Bukkit's Material.
     */
    public Object setGetItemMethod() throws InvocationTargetException, IllegalAccessException {
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
        return nmsItemStack;
    }

    /*
     * Sets method to get destroy speed of block when broken with certain item.
     */
    public void setGetDestroySpeedMethod(Object nmsItemStack) throws InvocationTargetException, IllegalAccessException {
        Object nmsItem = getItem.invoke(nmsItemStack);
        Class itemClass = nmsItem.getClass();
        item = getSuperClass(itemClass);
        Method[] itemClassMethods = item.getDeclaredMethods();
        for (Method method : itemClassMethods) {
            if (method.getName().equals("getDestroySpeed")) {
                getDestroySpeed = method;
                break;
            } else {
                if (method.getReturnType() == float.class && method.getParameterCount() == 2) {
                    Class<?>[] parameters = { nmsItemStack.getClass(), getNMSBlock.getReturnType() };
                    Class<?>[] methodParams = method.getParameterTypes();
                    if (Arrays.equals(methodParams, parameters)) {
                        getDestroySpeed = method;
                        break;
                    }
                }
            }
        }
        getDestroySpeed.setAccessible(true);
    }

    /*
     * Sets player's held item slot to what is best to break the block.
     * Only calculates hotbar slots.
     */
    public void autoTool(Player entity, Block block) {
        PlayerInventory inventory = entity.getInventory();
        float highest = 0;
        int highestSlot = -1;
        //pretend hand item as stone to avoid errors when its air or null
        ItemStack handItem = (inventory.getItemInHand() == null || inventory.getItemInHand().getType() == Material.AIR) ? new ItemStack(Material.STONE) : inventory.getItemInHand();
        float handItemSpeed = getDestroySpeed(handItem, block);

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = inventory.getItem(i);
            if (itemStack == null || itemStack.getType() == Material.AIR || block.getDrops(itemStack).isEmpty()) continue;

            float destroySpeed = getDestroySpeed(itemStack, block);
            if (destroySpeed > highest) {
                highest = destroySpeed;
                highestSlot = i;
            }
        }

        if (highestSlot == -1 || highest <= handItemSpeed) return;
        inventory.setHeldItemSlot(highestSlot);
    }

    public float getDestroySpeed(ItemStack itemStack, Block block) {
        float destroySpeed = -1;
        try {
            Object nmsItemStack = asNMSCopy.invoke(craftItemStack, itemStack);
            Object nmsItem = getItem.invoke(nmsItemStack); //nms material

            Object nmsBlockData = getNMSBlock.invoke(block);
            destroySpeed = (float) getDestroySpeed.invoke(nmsItem, nmsItemStack, nmsBlockData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return destroySpeed;
    }

    /*
     * Returns very last superclass of a class.
     */
    public Class getSuperClass(Object obj) {
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
