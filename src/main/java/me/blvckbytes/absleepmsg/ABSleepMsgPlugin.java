package me.blvckbytes.absleepmsg;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class ABSleepMsgPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

  private YamlConfiguration configuration;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(new CommandSendListener(this), this);
    Objects.requireNonNull(getCommand("absleepmsg")).setExecutor(this);

    saveDefaultConfig();
    configuration = loadConfiguration();

    Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
      for (var world : Bukkit.getWorlds()) {
        var worldMembers = world.getPlayers();

        var sleepCandidateCount = 0;
        var actuallySleepingCount = 0;

        for (var worldMember : worldMembers) {
          if (worldMember.isSleepingIgnored())
            continue;

          ++sleepCandidateCount;

          if (worldMember.isSleeping())
            ++actuallySleepingCount;
        }

        if (sleepCandidateCount == 0 || actuallySleepingCount == 0)
          continue;

        var reachedThreshold = actuallySleepingCount >= (sleepCandidateCount + 1) / 2;
        var configuredMessage = accessConfigValue(
          "actionBar." + (reachedThreshold ? "thresholdReached" : "thresholdNotYetReached")
        );

        var parameterizedMessage = TextComponent.fromLegacyText(
          configuredMessage
          .replace("{sleeping_count}", String.valueOf(actuallySleepingCount))
          .replace("{candidate_count}", String.valueOf(sleepCandidateCount))
        );

        for (var worldMember : worldMembers) {
          if (worldMember.isSleeping())
            worldMember.spigot().sendMessage(ChatMessageType.ACTION_BAR, parameterizedMessage);
        }
      }
    }, 0, 5);
  }

  private YamlConfiguration loadConfiguration() {
    var configuration = new YamlConfiguration();

    try {
      configuration.load(new File(getDataFolder(), "config.yml"));
    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Could not load the configuration-file", e);
    }

    return configuration;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!sender.hasPermission("absleepmsg.reload")) {
      sender.sendMessage(accessConfigValue("chat.noCommandPermission"));
      return true;
    }

    if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
      sender.sendMessage(
        accessConfigValue("chat.commandUsage")
          .replace("{command_label}", label)
      );
      return true;
    }

    configuration = loadConfiguration();
    sender.sendMessage(accessConfigValue("chat.configurationReloaded"));
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
    if (!sender.hasPermission("absleepmsg.reload") || args.length != 1)
      return List.of();

    return List.of("reload");
  }

  private String accessConfigValue(String path) {
    var value = configuration.getString(path);

    if (value == null)
      return "§cUndefined config-value at " + path;

    return enableColors(value);
  }

  private static boolean isColorChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= '0' && c <= '9') || (c >= 'k' && c <= 'o') || c == 'r';
  }

  private static boolean isHexChar(char c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }

  private static String enableColors(String input) {
    var inputLength = input.length();
    var result = new StringBuilder(inputLength);

    for (var charIndex = 0; charIndex < inputLength; ++charIndex) {
      var currentChar = input.charAt(charIndex);
      var remainingChars = inputLength - 1 - charIndex;

      if (currentChar != '&' || remainingChars == 0) {
        result.append(currentChar);
        continue;
      }

      var nextChar = input.charAt(++charIndex);

      // Possible hex-sequence of format &#RRGGBB
      if (nextChar == '#' && remainingChars >= 6 + 1) {
        var r1 = input.charAt(charIndex + 1);
        var r2 = input.charAt(charIndex + 2);
        var g1 = input.charAt(charIndex + 3);
        var g2 = input.charAt(charIndex + 4);
        var b1 = input.charAt(charIndex + 5);
        var b2 = input.charAt(charIndex + 6);

        if (
          isHexChar(r1) && isHexChar(r2)
            && isHexChar(g1) && isHexChar(g2)
            && isHexChar(b1) && isHexChar(b2)
        ) {
          result
            .append('§').append('x')
            .append('§').append(r1)
            .append('§').append(r2)
            .append('§').append(g1)
            .append('§').append(g2)
            .append('§').append(b1)
            .append('§').append(b2);

          charIndex += 6;
          continue;
        }
      }

      // Vanilla color-sequence
      if (isColorChar(nextChar)) {
        result.append('§').append(nextChar);
        continue;
      }

      // Wasn't a color-sequence, store as-is
      result.append(currentChar).append(nextChar);
    }

    return result.toString();
  }
}
