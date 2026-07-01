package me.usainsrht.autotool;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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
            plugin.setAutoToolEnabled(player, !currentState);
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString(currentState ? "messages.autotool_off" : "messages.autotool_on", currentState ? "off" : "on")));

            return true;
        }

        return false;
    }
}
