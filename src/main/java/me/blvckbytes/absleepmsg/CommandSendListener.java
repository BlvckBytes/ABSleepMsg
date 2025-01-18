package me.blvckbytes.absleepmsg;

import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CommandSendListener implements Listener {

  private final String lowerPluginName;
  private final Command pluginCommand;

  public CommandSendListener(JavaPlugin plugin) {
    this.lowerPluginName = plugin.getName().toLowerCase();
    this.pluginCommand = Objects.requireNonNull(plugin.getCommand("absleepmsg"));
  }

  @EventHandler
  public void onCommandSend(PlayerCommandSendEvent event) {
    var player = event.getPlayer();

    if (player.hasPermission("absleepmsg.reload"))
      return;

    event.getCommands().remove(pluginCommand.getName());
    event.getCommands().remove(lowerPluginName + ":" + pluginCommand.getName());

    for (var alias : pluginCommand.getAliases()) {
      event.getCommands().remove(alias);
      event.getCommands().remove(lowerPluginName + ":" + alias);
    }
  }
}
