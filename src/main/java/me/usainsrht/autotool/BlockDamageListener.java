package me.usainsrht.autotool;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

public class BlockDamageListener implements Listener {

    private AutoTool plugin;

     public BlockDamageListener(AutoTool plugin) {
         this.plugin = plugin;
     }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if (e.getInstaBreak()) return;
        plugin.autoTool(e.getPlayer(), e.getBlock());
    }
}
