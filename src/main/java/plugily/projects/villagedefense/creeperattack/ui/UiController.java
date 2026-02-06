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

package plugily.projects.villagedefense.creeperattack.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages UI elements: Scoreboard, Title, Chat messages.
 */
public class UiController {

  private final Main plugin;
  private final ConfigService configService;
  private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
  private final Map<UUID, Integer> playerKills = new HashMap<>();

  public UiController(Main plugin, ConfigService configService) {
    this.plugin = plugin;
    this.configService = configService;
  }

  /**
   * Update scoreboard for all players in the arena.
   */
  public void updateScoreboards(Arena arena, ArenaContext context) {
    if (!configService.isScoreboardEnabled()) return;
    
    for (Player player : arena.getPlayers()) {
      updateScoreboard(player, arena, context);
    }
  }

  /**
   * Update scoreboard for a specific player.
   */
  public void updateScoreboard(Player player, Arena arena, ArenaContext context) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager == null) return;
    
    Scoreboard scoreboard = playerScoreboards.computeIfAbsent(player.getUniqueId(), 
        k -> manager.getNewScoreboard());
    
    // Clear existing objective
    Objective existing = scoreboard.getObjective("ca_main");
    if (existing != null) {
      existing.unregister();
    }
    
    // Create new objective
    String title = ChatColor.translateAlternateColorCodes('&', configService.getScoreboardTitle());
    // 1.8.8 API: registerNewObjective(name, criteria)
    Objective objective = scoreboard.registerNewObjective("ca_main", "dummy");
    objective.setDisplayName(title);
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    
    // Get and format lines
    List<String> lines = configService.getScoreboardLines();
    int score = lines.size();
    
    for (String line : lines) {
      String formatted = formatLine(line, player, arena, context);
      // Ensure unique entries (add invisible chars if needed)
      formatted = makeUnique(formatted, score, scoreboard);
      objective.getScore(formatted).setScore(score);
      score--;
    }
    
    player.setScoreboard(scoreboard);
  }

  /**
   * Format a scoreboard line with placeholders.
   */
  private String formatLine(String line, Player player, Arena arena, ArenaContext context) {
    line = line.replace("%wave%", String.valueOf(context.getWaveIndex()));
    line = line.replace("%maxwave%", String.valueOf(context.getWaveMax()));
    line = line.replace("%trader_hp%", String.valueOf(context.getTraderCurrentHp()));
    line = line.replace("%trader_maxhp%", String.valueOf(context.getTraderMaxHp()));
    line = line.replace("%coins%", String.valueOf(context.getPlayerCoins(player.getUniqueId())));
    line = line.replace("%kills%", String.valueOf(playerKills.getOrDefault(player.getUniqueId(), 0)));
    line = line.replace("%creepers%", String.valueOf(context.getCreepersAlive() + context.getCreepersToSpawn()));
    line = line.replace("%players%", String.valueOf(arena.getPlayers().size()));
    
    // HP bar
    if (line.contains("%trader_hp_bar%")) {
      line = line.replace("%trader_hp_bar%", createHpBar(context));
    }
    
    return ChatColor.translateAlternateColorCodes('&', line);
  }

  /**
   * Create HP bar for scoreboard.
   */
  private String createHpBar(ArenaContext context) {
    int percent = (context.getTraderCurrentHp() * 100) / context.getTraderMaxHp();
    int bars = 10;
    int filled = (percent * bars) / 100;
    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < bars; i++) {
      if (i < filled) {
        if (percent > 50) {
          sb.append("§a█");
        } else if (percent > 25) {
          sb.append("§e█");
        } else {
          sb.append("§c█");
        }
      } else {
        sb.append("§7█");
      }
    }
    return sb.toString();
  }

  /**
   * Make scoreboard entry unique by adding invisible characters.
   */
  private String makeUnique(String text, int score, Scoreboard scoreboard) {
    // Add invisible characters to ensure uniqueness
    StringBuilder suffix = new StringBuilder();
    for (int i = 0; i < score; i++) {
      suffix.append(ChatColor.RESET);
    }
    return text + suffix.toString();
  }

  /**
   * Send wave start title.
   */
  public void sendWaveStartTitle(Arena arena, ArenaContext context) {
    if (!configService.isTitleEnabled()) return;
    
    String title = "§a§l第 " + context.getWaveIndex() + " 波";
    String subtitle = "§7消灭所有苦力怕!";
    
    for (Player player : arena.getPlayers()) {
      VersionUtils.sendTitle(player, title, 10, 40, 10);
      VersionUtils.sendSubTitle(player, subtitle, 10, 40, 10);
    }
  }

  /**
   * Send wave end title.
   */
  public void sendWaveEndTitle(Arena arena, ArenaContext context, int secondsUntilNext) {
    if (!configService.isTitleEnabled()) return;
    
    String title = "§e§l波次结束!";
    String subtitle = "§7下一波将在 §a" + secondsUntilNext + "§7 秒后开始";
    
    for (Player player : arena.getPlayers()) {
      VersionUtils.sendTitle(player, title, 10, 40, 10);
      VersionUtils.sendSubTitle(player, subtitle, 10, 40, 10);
    }
  }

  /**
   * Send game win title.
   */
  public void sendWinTitle(Arena arena) {
    if (!configService.isTitleEnabled()) return;
    
    for (Player player : arena.getPlayers()) {
      VersionUtils.sendTitle(player, "§a§l胜利!", 10, 60, 20);
      VersionUtils.sendSubTitle(player, "§7你成功抵御了所有波次!", 10, 60, 20);
    }
  }

  /**
   * Send game lose title.
   */
  public void sendLoseTitle(Arena arena) {
    if (!configService.isTitleEnabled()) return;
    
    for (Player player : arena.getPlayers()) {
      VersionUtils.sendTitle(player, "§c§l失败!", 10, 60, 20);
      VersionUtils.sendSubTitle(player, "§7商人已死亡!", 10, 60, 20);
    }
  }

  /**
   * Send Trader low HP warning.
   */
  public void sendTraderLowHpWarning(Arena arena, ArenaContext context) {
    if (!configService.isChatEnabled()) return;
    
    String msg = configService.getMessage("trader_low_hp")
        .replace("%hp%", String.valueOf(context.getTraderCurrentHp()))
        .replace("%maxhp%", String.valueOf(context.getTraderMaxHp()));
    
    for (Player player : arena.getPlayers()) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
  }

  /**
   * Send countdown warning for Creeper explosion.
   */
  public void sendCreeperCountdownWarning(Arena arena, int seconds) {
    if (!configService.isChatEnabled()) return;
    
    String msg = configService.getRawMessage("creeper_countdown")
        .replace("%seconds%", String.valueOf(seconds));
    
    for (Player player : arena.getPlayers()) {
      VersionUtils.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', msg));
    }
  }

  /**
   * Send kill reward message.
   */
  public void sendKillReward(Player player, int coins) {
    if (!configService.isChatEnabled()) return;
    
    String msg = configService.getRawMessage("kill_reward")
        .replace("%coins%", String.valueOf(coins));
    VersionUtils.sendActionBar(player, ChatColor.translateAlternateColorCodes('&', msg));
  }

  /**
   * Increment kill count for player.
   */
  public void incrementKills(UUID playerId) {
    playerKills.merge(playerId, 1, Integer::sum);
  }

  /**
   * Get kills for player.
   */
  public int getKills(UUID playerId) {
    return playerKills.getOrDefault(playerId, 0);
  }

  /**
   * Clear player data.
   */
  public void clearPlayer(UUID playerId) {
    playerScoreboards.remove(playerId);
    playerKills.remove(playerId);
  }

  /**
   * Reset scoreboard for player.
   */
  public void resetScoreboard(Player player) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    if (manager != null) {
      player.setScoreboard(manager.getMainScoreboard());
    }
    clearPlayer(player.getUniqueId());
  }
}
