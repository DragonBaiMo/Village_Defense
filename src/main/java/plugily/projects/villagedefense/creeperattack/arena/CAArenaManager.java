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

package plugily.projects.villagedefense.creeperattack.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;
import plugily.projects.villagedefense.creeperattack.economy.EconomyService;
import plugily.projects.villagedefense.creeperattack.shop.ShopController;
import plugily.projects.villagedefense.creeperattack.shop.effect.EffectRegistry;
import plugily.projects.villagedefense.creeperattack.trader.TraderController;
import plugily.projects.villagedefense.creeperattack.ui.UiController;
import plugily.projects.villagedefense.creeperattack.wave.WaveController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages Creeper Attack arenas: context lifecycle, game loop, win/lose conditions.
 */
public class CAArenaManager {

  private final Main plugin;
  private final ConfigService configService;
  private final EffectRegistry effectRegistry;
  private final CitizensHook citizensHook;
  
  private final TraderController traderController;
  private final WaveController waveController;
  private final EconomyService economyService;
  private final UiController uiController;
  private ShopController shopController;
  
  private final Map<String, ArenaContext> contexts = new HashMap<>();
  private final Map<String, BukkitTask> gameTasks = new HashMap<>();
  private final Map<String, BukkitTask> scoreboardTasks = new HashMap<>();
  
  // Track warned HP thresholds to avoid spam
  private final Map<String, Set<Integer>> warnedThresholds = new HashMap<>();

  public CAArenaManager(Main plugin, ConfigService configService, EffectRegistry effectRegistry) {
    this.plugin = plugin;
    this.configService = configService;
    this.effectRegistry = effectRegistry;
    this.citizensHook = new CitizensHook(plugin);
    
    this.traderController = new TraderController(plugin, configService, citizensHook);
    this.waveController = new WaveController(plugin, configService, citizensHook);
    this.economyService = new EconomyService(plugin, configService);
    this.uiController = new UiController(plugin, configService);
    
    // Initialize effect registry with trader controller
    effectRegistry.initWithTraderController(plugin, configService, traderController);
    
    // Shop controller needs this manager, so initialize after
    this.shopController = new ShopController(plugin, configService, this, economyService, effectRegistry);
  }

  /**
   * Get or create context for an arena.
   */
  public ArenaContext getOrCreateContext(String arenaId) {
    return contexts.computeIfAbsent(arenaId, ArenaContext::new);
  }

  /**
   * Get context for an arena.
   */
  public ArenaContext getContext(String arenaId) {
    return contexts.get(arenaId);
  }

  /**
   * Start the Creeper Attack game for an arena.
   */
  public boolean startGame(Arena arena) {
    String arenaId = arena.getId();
    
    // Validate configuration
    ConfigService.ValidationResult validation = configService.validate();
    if (!validation.isOk()) {
      String missing = String.join(", ", validation.getMissingFields());
      for (Player player : arena.getPlayers()) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            configService.getMessage("config_missing").replace("%missing%", missing)));
      }
      return false;
    }
    
    // Create/reset context
    ArenaContext context = getOrCreateContext(arenaId);
    context.reset();
    
    // Copy lane configuration from config service
    List<Lane> configLanes = configService.getLanes();
    for (int i = 0; i < 4; i++) {
      Lane src = configLanes.get(i);
      Lane dst = context.getLane(i + 1);
      if (dst != null && src != null) {
        dst.setSpawn(src.getSpawn());
        dst.setEnd(src.getEnd());
      }
    }
    
    // Set wave max from config
    context.setWaveMax(configService.getMaxWaves());
    
    // Spawn trader
    if (!traderController.spawnTrader(context)) {
      for (Player player : arena.getPlayers()) {
        player.sendMessage(ChatColor.RED + "[CreeperAttack] 无法生成商人!");
      }
      return false;
    }
    
    // Initialize player economy
    for (Player player : arena.getPlayers()) {
      economyService.initPlayer(context, player.getUniqueId());
    }
    
    // Clear warned thresholds
    warnedThresholds.put(arenaId, new HashSet<>());
    
    // Start game loop
    startGameLoop(arena, context);
    
    // Start scoreboard update task
    startScoreboardTask(arena, context);
    
    // Send initial message
    for (Player player : arena.getPlayers()) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&',
          configService.getMessage("forcestart_success")));
    }
    
    // Start first wave after interval
    context.setNextWaveAt(System.currentTimeMillis() + 5000); // 5 second initial delay
    
    plugin.getLogger().info("[CreeperAttack] Game started for arena: " + arenaId);
    return true;
  }

  /**
   * Start the main game loop task.
   */
  private void startGameLoop(Arena arena, ArenaContext context) {
    String arenaId = arena.getId();
    
    // Cancel existing task
    BukkitTask existingTask = gameTasks.get(arenaId);
    if (existingTask != null) {
      existingTask.cancel();
    }
    
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      gameLoopTick(arena, context);
    }, 0L, configService.getBatchSpawnPeriodTicks());
    
    gameTasks.put(arenaId, task);
    context.setMainTaskId(task.getTaskId());
  }

  /**
   * Start the scoreboard update task.
   */
  private void startScoreboardTask(Arena arena, ArenaContext context) {
    String arenaId = arena.getId();
    
    // Cancel existing task
    BukkitTask existingTask = scoreboardTasks.get(arenaId);
    if (existingTask != null) {
      existingTask.cancel();
    }
    
    int refreshTicks = configService.getScoreboardRefreshSeconds() * 20;
    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      uiController.updateScoreboards(arena, context);
    }, 0L, refreshTicks);
    
    scoreboardTasks.put(arenaId, task);
    context.setScoreboardTaskId(task.getTaskId());
  }

  /**
   * Main game loop tick.
   */
  private void gameLoopTick(Arena arena, ArenaContext context) {
    // Check if arena is still in game
    if (arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    
    // Check lose condition: Trader dead
    if (context.isTraderDead()) {
      handleGameLose(arena, context);
      return;
    }
    
    // Check win condition: All waves complete
    if (waveController.isAllWavesComplete(context)) {
      handleGameWin(arena, context);
      return;
    }
    
    // Handle fighting state
    if (context.isFighting()) {
      // Spawn batch
      if (context.getCreepersToSpawn() > 0) {
        waveController.spawnBatch(context, arena);
      }
      
      // Check wave complete
      if (waveController.isWaveComplete(context)) {
        int seconds = configService.getWaveIntervalSeconds();
        waveController.endWave(context, arena);
        uiController.sendWaveEndTitle(arena, context, seconds);
        
        // Chat message
        if (configService.isChatEnabled()) {
          String msg = configService.getMessage("wave_end")
              .replace("%wave%", String.valueOf(context.getWaveIndex()))
              .replace("%seconds%", String.valueOf(seconds));
          for (Player player : arena.getPlayers()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
          }
        }
      }
    } else {
      // Check if should start next wave
      if (waveController.shouldStartNextWave(context)) {
        waveController.startWave(context, arena);
        uiController.sendWaveStartTitle(arena, context);
        
        // Chat message
        if (configService.isChatEnabled()) {
          String msg = configService.getMessage("wave_start")
              .replace("%wave%", String.valueOf(context.getWaveIndex()));
          for (Player player : arena.getPlayers()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
          }
        }
      }
    }
    
    // Check Trader HP warnings
    checkTraderHpWarnings(arena, context);
  }

  /**
   * Check and send Trader low HP warnings.
   */
  private void checkTraderHpWarnings(Arena arena, ArenaContext context) {
    int percent = (context.getTraderCurrentHp() * 100) / context.getTraderMaxHp();
    Set<Integer> warned = warnedThresholds.computeIfAbsent(arena.getId(), k -> new HashSet<>());
    
    for (int threshold : configService.getTraderLowHpThresholds()) {
      if (percent <= threshold && !warned.contains(threshold)) {
        warned.add(threshold);
        uiController.sendTraderLowHpWarning(arena, context);
        break;
      }
    }
  }

  /**
   * Handle game win.
   */
  private void handleGameWin(Arena arena, ArenaContext context) {
    uiController.sendWinTitle(arena);
    
    if (configService.isChatEnabled()) {
      for (Player player : arena.getPlayers()) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            configService.getMessage("game_win")));
      }
    }
    
    stopGame(arena, context);
  }

  /**
   * Handle game lose.
   */
  private void handleGameLose(Arena arena, ArenaContext context) {
    uiController.sendLoseTitle(arena);
    
    if (configService.isChatEnabled()) {
      for (Player player : arena.getPlayers()) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            configService.getMessage("game_lose")));
      }
    }
    
    stopGame(arena, context);
  }

  /**
   * Stop the game and clean up.
   */
  public void stopGame(Arena arena, ArenaContext context) {
    String arenaId = arena.getId();
    
    // Cancel tasks
    BukkitTask gameTask = gameTasks.remove(arenaId);
    if (gameTask != null) {
      gameTask.cancel();
    }
    
    BukkitTask scoreboardTask = scoreboardTasks.remove(arenaId);
    if (scoreboardTask != null) {
      scoreboardTask.cancel();
    }
    
    // Remove all creepers
    for (Creeper creeper : context.getCreepers()) {
      if (creeper != null && !creeper.isDead()) {
        if(context.getCreeperNpc(creeper.getUniqueId()) != null) {
          citizensHook.safeDestroy(context.getCreeperNpc(creeper.getUniqueId()));
          context.removeCreeperNpc(creeper.getUniqueId());
        }
        creeper.remove();
      }
    }
    context.getCreepers().clear();
    
    // Remove trader
    traderController.removeTrader(context);
    
    // Reset player scoreboards
    for (Player player : arena.getPlayers()) {
      uiController.resetScoreboard(player);
    }
    
    // Clear context
    context.reset();
    warnedThresholds.remove(arenaId);
    
    plugin.getLogger().info("[CreeperAttack] Game stopped for arena: " + arenaId);
    
    // Trigger arena manager stop
    plugin.getArenaManager().stopGame(false, arena);
  }

  /**
   * Force stop the game.
   */
  public void forceStop(Arena arena) {
    ArenaContext context = getContext(arena.getId());
    if (context != null) {
      stopGame(arena, context);
    }
    
    for (Player player : arena.getPlayers()) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&',
          configService.getMessage("forcestop_success")));
    }
  }

  /**
   * Handle Creeper explosion (called by proximity listener).
   */
  public void handleCreeperExplosion(Arena arena, ArenaContext context, Creeper creeper) {
    // Apply damage to trader
    traderController.applyExplosionDamage(context);
    
    // Create explosion effect (no block damage)
    // 1.8.8 API: createExplosion(x, y, z, power, setFire, breakBlocks)
    Location loc = creeper.getLocation();
    creeper.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0F, false, false);
    
    // Remove creeper
    context.removeCreeper(creeper);
    if(context.getCreeperNpc(creeper.getUniqueId()) != null) {
      citizensHook.safeDestroy(context.getCreeperNpc(creeper.getUniqueId()));
      context.removeCreeperNpc(creeper.getUniqueId());
    }
    creeper.remove();
    
    // Clear countdown
    context.clearCountdown(creeper.getUniqueId());
  }

  /**
   * Handle Creeper kill (called by death listener).
   */
  public void handleCreeperKill(Arena arena, ArenaContext context, Creeper creeper, Player killer) {
    // Award coins
    if (killer != null) {
      int reward = economyService.awardKillReward(context, killer);
      uiController.sendKillReward(killer, reward);
      uiController.incrementKills(killer.getUniqueId());
    }
    
    // Remove from tracking
    context.removeCreeper(creeper);
    if(context.getCreeperNpc(creeper.getUniqueId()) != null) {
      citizensHook.safeDestroy(context.getCreeperNpc(creeper.getUniqueId()));
      context.removeCreeperNpc(creeper.getUniqueId());
    }
    context.clearCountdown(creeper.getUniqueId());
  }

  /**
   * Handle player death (called by death listener).
   */
  public void handlePlayerDeath(Arena arena, ArenaContext context, Player player) {
    int penalty = economyService.applyDeathPenalty(context, player);
    
    if (configService.isChatEnabled() && penalty > 0) {
      String msg = configService.getMessage("death_penalty")
          .replace("%percent%", String.valueOf(configService.getDeathPenaltyPercent()));
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
  }

  /**
   * Open shop for player.
   */
  public void openShop(Player player, Arena arena) {
    ArenaContext context = getContext(arena.getId());
    if (context == null) return;
    
    shopController.openShop(player, arena, context);
  }

  /**
   * Reload all contexts (on config reload).
   */
  public void reloadAllContexts() {
    // Just reload config; active games will use old settings until restart
    configService.reload();
  }

  /**
   * Shutdown all games.
   */
  public void shutdownAll() {
    for (String arenaId : new HashSet<>(contexts.keySet())) {
      Arena arena = plugin.getArenaRegistry().getArena(arenaId);
      if (arena != null) {
        ArenaContext context = contexts.get(arenaId);
        if (context != null) {
          stopGame(arena, context);
        }
      }
    }
    contexts.clear();
  }

  // Getters
  public TraderController getTraderController() { return traderController; }
  public WaveController getWaveController() { return waveController; }
  public EconomyService getEconomyService() { return economyService; }
  public UiController getUiController() { return uiController; }
  public ShopController getShopController() { return shopController; }
}
