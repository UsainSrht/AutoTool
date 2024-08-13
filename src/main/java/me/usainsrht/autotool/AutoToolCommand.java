package me.usainsrht.autotool;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AutoToolCommand implements CommandExecutor {

    private AutoTool plugin;

    public AutoToolCommand(AutoTool plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if ((commandSender.hasPermission("autotool.reload") && args.length > 0 && args[0].equalsIgnoreCase("reload")) || commandSender instanceof ConsoleCommandSender) {
            plugin.reloadConfig();
            commandSender.sendMessage(ChatColor.GREEN + "AutoTool Reloaded!");
            return true;
        }
        if (commandSender instanceof Player) {
            Player player = (Player) commandSender;

            boolean currentState = plugin.isAutoToolOn(player);

            List<String> disabledList = plugin.getConfig().getStringList("autotool_disabled");

            if (currentState) {
                disabledList.add(player.getUniqueId().toString());
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.autotool_off", "off")));
            } else {
                disabledList.remove(player.getUniqueId().toString());
                commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.autotool_on", "on")));
            }

            plugin.getConfig().set("autotool_disabled", disabledList);
            plugin.saveConfig();

            return true;
        }

        return false;
    }
}
