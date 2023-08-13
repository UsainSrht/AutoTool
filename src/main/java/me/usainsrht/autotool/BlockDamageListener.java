package me.usainsrht.autotool;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;

public class BlockDamageListener implements Listener {

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if (e.getInstaBreak()) return;
        AutoTool.autoTool(e.getPlayer(), e.getBlock());
    }
}
