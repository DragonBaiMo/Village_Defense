/*
 *  Village Defense - Protect villagers from hordes of zombies
 *  Copyright (c) 2023 Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugily.projects.villagedefense.creeperattack.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for /ca commands.
 */
public class CACommandExecutor implements CommandExecutor, TabCompleter {

  private static final String PERMISSION_ADMIN = "creeperattack.admin";
  
  private final Main plugin;
  private final ConfigService configService;
  private final CAArenaManager arenaManager;

  public CACommandExecutor(Main plugin, ConfigService configService, CAArenaManager arenaManager) {
    this.plugin = plugin;
    this.configService = configService;
    this.arenaManager = arenaManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      sendHelp(sender);
      return true;
    }
    
    String subCommand = args[0].toLowerCase();
    
    switch (subCommand) {
      case "help":
        sendHelp(sender);
        break;
      case "reload":
        handleReload(sender);
        break;
      case "settrader":
        handleSetTrader(sender);
        break;
      case "setlane":
        handleSetLane(sender, args);
        break;
      case "forcestart":
        handleForceStart(sender);
        break;
      case "forcestop":
        handleForceStop(sender);
        break;
      case "info":
        handleInfo(sender);
        break;
      default:
        sender.sendMessage(ChatColor.RED + "Unknown command. Use /ca help.");
        break;
    }
    
    return true;
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(ChatColor.GOLD + "===== Creeper Attack Help =====");
    sender.sendMessage(ChatColor.YELLOW + "/ca help" + ChatColor.GRAY + " - Show help");
    sender.sendMessage(ChatColor.YELLOW + "/ca reload" + ChatColor.GRAY + " - Reload config");
    sender.sendMessage(ChatColor.YELLOW + "/ca settrader" + ChatColor.GRAY + " - Set trader location");
    sender.sendMessage(ChatColor.YELLOW + "/ca setlane <1-4> <spawn|end>" + ChatColor.GRAY + " - Set lane points");
    sender.sendMessage(ChatColor.YELLOW + "/ca forcestart" + ChatColor.GRAY + " - Force start game");
    sender.sendMessage(ChatColor.YELLOW + "/ca forcestop" + ChatColor.GRAY + " - Force stop game");
    sender.sendMessage(ChatColor.YELLOW + "/ca info" + ChatColor.GRAY + " - Show config status");
  }

  private void handleReload(CommandSender sender) {
    if (!checkPermission(sender)) return;
    
    configService.reload();
    arenaManager.reloadAllContexts();
    
    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
        configService.getMessage("reload_success")));
  }

  private void handleSetTrader(CommandSender sender) {
    if (!checkPermission(sender)) return;
    if (!checkPlayer(sender)) return;
    
    Player player = (Player) sender;
    Location location = player.getLocation();
    
    configService.saveTraderLocation(location);
    
    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
        configService.getMessage("set_trader_success")));
  }

  private void handleSetLane(CommandSender sender, String[] args) {
    if (!checkPermission(sender)) return;
    if (!checkPlayer(sender)) return;
    
    if (args.length < 3) {
      sender.sendMessage(ChatColor.RED + "Usage: /ca setlane <1-4> <spawn|end>");
      return;
    }
    
    int laneId;
    try {
      laneId = Integer.parseInt(args[1]);
    } catch (NumberFormatException e) {
      sender.sendMessage(ChatColor.RED + "Lane id must be number 1-4!");
      return;
    }
    
    if (laneId < 1 || laneId > 4) {
      sender.sendMessage(ChatColor.RED + "Lane id must be 1-4!");
      return;
    }
    
    String type = args[2].toLowerCase();
    if (!type.equals("spawn") && !type.equals("end")) {
      sender.sendMessage(ChatColor.RED + "Type must be spawn or end!");
      return;
    }
    
    Player player = (Player) sender;
    Location location = player.getLocation();
    
    configService.saveLaneLocation(laneId, type, location);
    
    String msg = configService.getMessage("set_lane_success")
        .replace("%lane%", String.valueOf(laneId))
        .replace("%type%", type);
    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
  }

  private void handleForceStart(CommandSender sender) {
    if (!checkPermission(sender)) return;
    if (!checkPlayer(sender)) return;
    
    Player player = (Player) sender;
    Arena arena = plugin.getArenaRegistry().getArena(player);
    
    if (arena == null) {
      sender.sendMessage(ChatColor.RED + "You must be in an arena to use this command!");
      return;
    }
    
    if (arena.getArenaState() != IArenaState.WAITING_FOR_PLAYERS 
        && arena.getArenaState() != IArenaState.STARTING) {
      sender.sendMessage(ChatColor.RED + "You can only forcestart while waiting/starting!");
      return;
    }
    
    // Validate configuration
    ConfigService.ValidationResult validation = configService.validate();
    if (!validation.isOk()) {
      String missing = String.join(", ", validation.getMissingFields());
      String msg = configService.getMessage("config_missing").replace("%missing%", missing);
      sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
      return;
    }
    
    // Force arena to IN_GAME state
    arena.setArenaState(IArenaState.IN_GAME);
    
    // Start Creeper Attack game
    arenaManager.startGame(arena);
  }

  private void handleForceStop(CommandSender sender) {
    if (!checkPermission(sender)) return;
    if (!checkPlayer(sender)) return;
    
    Player player = (Player) sender;
    Arena arena = plugin.getArenaRegistry().getArena(player);
    
    if (arena == null) {
      sender.sendMessage(ChatColor.RED + "You must be in an arena to use this command!");
      return;
    }
    
    if (arena.getArenaState() != IArenaState.IN_GAME) {
      sender.sendMessage(ChatColor.RED + "Game is not running!");
      return;
    }
    
    arenaManager.forceStop(arena);
  }

  private void handleInfo(CommandSender sender) {
    if (!checkPermission(sender)) return;
    
    sender.sendMessage(ChatColor.GOLD + "===== Creeper Attack Status =====");
    
    ConfigService.ValidationResult validation = configService.validate();
    
    if (validation.isOk()) {
      sender.sendMessage(ChatColor.GREEN + "OK: config complete");
    } else {
      sender.sendMessage(ChatColor.RED + "Missing config:");
      for (String missing : validation.getMissingFields()) {
        sender.sendMessage(ChatColor.RED + "  - " + missing);
      }
    }
    
    sender.sendMessage(ChatColor.GRAY + "Trader: " +
        (configService.getTraderLocation() != null ?
            formatLocation(configService.getTraderLocation()) : ChatColor.RED + "Not set"));
    
    for (int i = 1; i <= 4; i++) {
      plugily.projects.villagedefense.creeperattack.arena.Lane lane = configService.getLanes().get(i - 1);
      sender.sendMessage(ChatColor.GRAY + "Lane " + i + " spawn: " +
          (lane.getSpawn() != null ? formatLocation(lane.getSpawn()) : ChatColor.RED + "Not set"));
      sender.sendMessage(ChatColor.GRAY + "Lane " + i + " end: " +
          (lane.getEnd() != null ? formatLocation(lane.getEnd()) : ChatColor.RED + "Not set"));
    }
  }

  private String formatLocation(Location loc) {
    return loc.getWorld().getName() + " (" + 
        String.format("%.1f", loc.getX()) + ", " +
        String.format("%.1f", loc.getY()) + ", " +
        String.format("%.1f", loc.getZ()) + ")";
  }

  private boolean checkPermission(CommandSender sender) {
    if (!sender.hasPermission(PERMISSION_ADMIN)) {
      sender.sendMessage(ChatColor.RED + "No permission!");
      return false;
    }
    return true;
  }

  private boolean checkPlayer(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Players only!");
      return false;
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    
    if (args.length == 1) {
      completions.addAll(Arrays.asList("help", "reload", "settrader", "setlane", "forcestart", "forcestop", "info"));
    } else if (args.length == 2 && args[0].equalsIgnoreCase("setlane")) {
      completions.addAll(Arrays.asList("1", "2", "3", "4"));
    } else if (args.length == 3 && args[0].equalsIgnoreCase("setlane")) {
      completions.addAll(Arrays.asList("spawn", "end"));
    }
    
    String prefix = args[args.length - 1].toLowerCase();
    return completions.stream()
        .filter(s -> s.toLowerCase().startsWith(prefix))
        .collect(Collectors.toList());
  }
}
