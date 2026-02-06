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

package plugily.projects.villagedefense.creeperattack.config;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.creeperattack.arena.Lane;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Centralized configuration service for Creeper Attack mode.
 * Handles loading, saving, validation, and default generation.
 */
public class ConfigService {

  private final Main plugin;
  private File configFile;
  private FileConfiguration config;

  // Cached config values
  private int traderMaxHealth = 100;
  private int explosionDamagePercent = 10;
  private double triggerRadius = 3.0;
  private int countdownSeconds = 3;
  private boolean traderInvulnerable = true;
  
  private int maxWaves = 30;
  private int waveIntervalSeconds = 10;
  private int spawnPerWaveBase = 8;
  private int spawnPerWaveIncrease = 2;
  private int creeperPercent = 100;
  private int batchSpawnSize = 4;
  private int batchSpawnPeriodTicks = 20;
  
  private int killRewardCreeper = 10;
  private int deathPenaltyPercent = 33;
  
  private boolean shopEnabled = true;
  private boolean shopBetweenWavesOnly = false;
  
  private Location traderLocation;
  private final List<Lane> lanes = new ArrayList<>(4);

  public ConfigService(Main plugin) {
    this.plugin = plugin;
    for (int i = 1; i <= 4; i++) {
      lanes.add(new Lane(i));
    }
  }

  /**
   * Load or create the creeperattack.yml configuration file.
   */
  public void loadOrCreate() {
    configFile = new File(plugin.getDataFolder(), "creeperattack.yml");
    
    if (!configFile.exists()) {
      createDefaultConfig();
    }
    
    config = YamlConfiguration.loadConfiguration(configFile);
    loadValues();
  }

  /**
   * Reload configuration from disk.
   */
  public void reload() {
    if (configFile == null) {
      loadOrCreate();
      return;
    }
    config = YamlConfiguration.loadConfiguration(configFile);
    loadValues();
  }

  /**
   * Create default configuration file with all structure intact.
   */
  private void createDefaultConfig() {
    try {
      if (!plugin.getDataFolder().exists()) {
        plugin.getDataFolder().mkdirs();
      }
      configFile.createNewFile();
      
      FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
      
      // Trader section
      defaultConfig.set("trader.location", "");
      defaultConfig.set("trader.max_health", 100);
      defaultConfig.set("trader.explosion_damage_percent", 10);
      defaultConfig.set("trader.trigger_radius", 3.0);
      defaultConfig.set("trader.countdown_seconds", 3);
      defaultConfig.set("trader.invulnerable", true);
      
      // Lanes section (4 lanes)
      for (int i = 1; i <= 4; i++) {
        defaultConfig.set("lanes.lane" + i + ".spawn", "");
        defaultConfig.set("lanes.lane" + i + ".end", "");
      }
      
      // Waves section
      defaultConfig.set("waves.max_waves", 30);
      defaultConfig.set("waves.interval_seconds", 10);
      defaultConfig.set("waves.spawn_per_wave.base", 8);
      defaultConfig.set("waves.spawn_per_wave.increase", 2);
      defaultConfig.set("waves.mob_mix.creeper_percent", 100);
      defaultConfig.set("waves.batch_spawn.size", 4);
      defaultConfig.set("waves.batch_spawn.period_ticks", 20);
      
      // Economy section
      defaultConfig.set("economy.kill_reward.creeper", 10);
      defaultConfig.set("economy.death_penalty_percent", 33);
      defaultConfig.set("economy.enabled", true);
      
      // Shop section
      defaultConfig.set("shop.enabled", true);
      defaultConfig.set("shop.open_between_waves_only", false);
      
      // Default shop items
      defaultConfig.set("shop.items.wooden_sword.type", "WEAPON");
      defaultConfig.set("shop.items.wooden_sword.price", 10);
      defaultConfig.set("shop.items.wooden_sword.material", "WOOD_SWORD");
      defaultConfig.set("shop.items.wooden_sword.name", "&eWooden Sword");
      List<String> woodenSwordLore = new ArrayList<>();
      woodenSwordLore.add("&7Basic weapon");
      woodenSwordLore.add("&aPrice: 10 coins");
      defaultConfig.set("shop.items.wooden_sword.lore", woodenSwordLore);
      defaultConfig.set("shop.items.wooden_sword.effect.type", "GIVE_ITEM");
      
      defaultConfig.set("shop.items.iron_sword.type", "WEAPON");
      defaultConfig.set("shop.items.iron_sword.price", 30);
      defaultConfig.set("shop.items.iron_sword.material", "IRON_SWORD");
      defaultConfig.set("shop.items.iron_sword.name", "&fIron Sword");
      List<String> ironSwordLore = new ArrayList<>();
      ironSwordLore.add("&7Upgraded weapon");
      ironSwordLore.add("&aPrice: 30 coins");
      defaultConfig.set("shop.items.iron_sword.lore", ironSwordLore);
      defaultConfig.set("shop.items.iron_sword.effect.type", "GIVE_ITEM");
      
      defaultConfig.set("shop.items.diamond_sword.type", "WEAPON");
      defaultConfig.set("shop.items.diamond_sword.price", 80);
      defaultConfig.set("shop.items.diamond_sword.material", "DIAMOND_SWORD");
      defaultConfig.set("shop.items.diamond_sword.name", "&bDiamond Sword");
      List<String> diamondSwordLore = new ArrayList<>();
      diamondSwordLore.add("&7High tier weapon");
      diamondSwordLore.add("&aPrice: 80 coins");
      defaultConfig.set("shop.items.diamond_sword.lore", diamondSwordLore);
      defaultConfig.set("shop.items.diamond_sword.effect.type", "GIVE_ITEM");
      
      defaultConfig.set("shop.items.bow.type", "WEAPON");
      defaultConfig.set("shop.items.bow.price", 25);
      defaultConfig.set("shop.items.bow.material", "BOW");
      defaultConfig.set("shop.items.bow.name", "&6Bow");
      List<String> bowLore = new ArrayList<>();
      bowLore.add("&7Ranged weapon");
      bowLore.add("&aPrice: 25 coins");
      defaultConfig.set("shop.items.bow.lore", bowLore);
      defaultConfig.set("shop.items.bow.effect.type", "GIVE_ITEM");
      
      defaultConfig.set("shop.items.arrows.type", "CONSUMABLE");
      defaultConfig.set("shop.items.arrows.price", 5);
      defaultConfig.set("shop.items.arrows.material", "ARROW");
      defaultConfig.set("shop.items.arrows.amount", 16);
      defaultConfig.set("shop.items.arrows.name", "&7Arrows x16");
      List<String> arrowsLore = new ArrayList<>();
      arrowsLore.add("&7Ammo for bow");
      arrowsLore.add("&aPrice: 5 coins");
      defaultConfig.set("shop.items.arrows.lore", arrowsLore);
      defaultConfig.set("shop.items.arrows.effect.type", "GIVE_ITEM");
      
      defaultConfig.set("shop.items.golden_apple.type", "CONSUMABLE");
      defaultConfig.set("shop.items.golden_apple.price", 20);
      defaultConfig.set("shop.items.golden_apple.material", "GOLDEN_APPLE");
      defaultConfig.set("shop.items.golden_apple.name", "&6Golden Apple");
      List<String> goldenAppleLore = new ArrayList<>();
      goldenAppleLore.add("&7Heal yourself");
      goldenAppleLore.add("&aPrice: 20 coins");
      defaultConfig.set("shop.items.golden_apple.lore", goldenAppleLore);
      defaultConfig.set("shop.items.golden_apple.effect.type", "GIVE_ITEM");
      
      // Special skills
      defaultConfig.set("shop.items.freeze_creepers.type", "SPECIAL");
      defaultConfig.set("shop.items.freeze_creepers.price", 50);
      defaultConfig.set("shop.items.freeze_creepers.material", "SNOW_BALL");
      defaultConfig.set("shop.items.freeze_creepers.name", "&bFreeze Creepers");
      List<String> freezeLore = new ArrayList<>();
      freezeLore.add("&7Freeze creepers for 5s");
      freezeLore.add("&aPrice: 50 coins");
      freezeLore.add("&cCooldown: 30s");
      defaultConfig.set("shop.items.freeze_creepers.lore", freezeLore);
      defaultConfig.set("shop.items.freeze_creepers.cooldown_seconds", 30);
      defaultConfig.set("shop.items.freeze_creepers.effect.type", "FREEZE_CREEPERS");
      defaultConfig.set("shop.items.freeze_creepers.effect.duration_seconds", 5);
      defaultConfig.set("shop.items.freeze_creepers.effect.radius", 50);
      
      defaultConfig.set("shop.items.heal_trader.type", "SPECIAL");
      defaultConfig.set("shop.items.heal_trader.price", 40);
      defaultConfig.set("shop.items.heal_trader.material", "RED_DYE");
      defaultConfig.set("shop.items.heal_trader.name", "&cHeal Trader");
      List<String> healLore = new ArrayList<>();
      healLore.add("&7Heal trader for 20% max HP");
      healLore.add("&aPrice: 40 coins");
      healLore.add("&cCooldown: 20s");
      defaultConfig.set("shop.items.heal_trader.lore", healLore);
      defaultConfig.set("shop.items.heal_trader.cooldown_seconds", 20);
      defaultConfig.set("shop.items.heal_trader.effect.type", "HEAL_TRADER");
      defaultConfig.set("shop.items.heal_trader.effect.heal_percent", 20);
      
      // UI section
      defaultConfig.set("ui.scoreboard.enabled", true);
      defaultConfig.set("ui.scoreboard.refresh_seconds", 1);
      defaultConfig.set("ui.title.enabled", true);
      defaultConfig.set("ui.chat.enabled", true);
      List<Integer> lowHpThresholds = new ArrayList<>();
      lowHpThresholds.add(25);
      lowHpThresholds.add(10);
      defaultConfig.set("ui.trader_lowhp_thresholds", lowHpThresholds);
      
      // Messages section
      defaultConfig.set("messages.prefix", "&8[&6CreeperAttack&8] ");
      defaultConfig.set("messages.wave_start", "&aWave %wave% started!");
      defaultConfig.set("messages.wave_end", "&eWave %wave% cleared! Next wave in %seconds% seconds");
      defaultConfig.set("messages.game_win", "&a&lVictory! You survived all waves!");
      defaultConfig.set("messages.game_lose", "&c&lDefeat! Trader died!");
      defaultConfig.set("messages.trader_low_hp", "&c&lWarning! Trader low HP (%hp%/%maxhp%)");
      defaultConfig.set("messages.creeper_countdown", "&cCreeper will explode in %seconds%s!");
      defaultConfig.set("messages.kill_reward", "&a+%coins% coins");
      defaultConfig.set("messages.death_penalty", "&cDeath! Lost %percent%%% coins");
      defaultConfig.set("messages.shop_not_enough", "&cNot enough coins!");
      defaultConfig.set("messages.shop_cooldown", "&cOn cooldown! %seconds%s left");
      defaultConfig.set("messages.shop_success", "&aPurchased!");
      defaultConfig.set("messages.freeze_activated", "&bFreeze activated! %seconds%s");
      defaultConfig.set("messages.heal_activated", "&aHeal activated! +%percent%%% HP");
      defaultConfig.set("messages.config_missing", "&cMissing config: %missing%");
      defaultConfig.set("messages.forcestart_success", "&aGame started.");
      defaultConfig.set("messages.forcestop_success", "&cGame stopped.");
      defaultConfig.set("messages.reload_success", "&aReloaded.");
      defaultConfig.set("messages.set_trader_success", "&aTrader location set.");
      defaultConfig.set("messages.set_lane_success", "&aLane %lane% %type% set.");
      
      // Scoreboard lines
      defaultConfig.set("scoreboard.title", "&6&lCreeperAttack");
      List<String> scoreboardLines = new ArrayList<>();
      scoreboardLines.add("&7━━━━━━━━━━━━━━");
      scoreboardLines.add("&fWave: &a%wave%/%maxwave%");
      scoreboardLines.add("");
      scoreboardLines.add("&fTrader HP:");
      scoreboardLines.add("%trader_hp_bar%");
      scoreboardLines.add("&c%trader_hp%&7/&c%trader_maxhp%");
      scoreboardLines.add("");
      scoreboardLines.add("&fCoins: &e%coins%");
      scoreboardLines.add("&fKills: &a%kills%");
      scoreboardLines.add("");
      scoreboardLines.add("&fCreepers left: &c%creepers%");
      scoreboardLines.add("&7━━━━━━━━━━━━━━");
      defaultConfig.set("scoreboard.lines", scoreboardLines);
      
      defaultConfig.save(configFile);
      plugin.getLogger().log(Level.INFO, "[CreeperAttack] Default configuration created: creeperattack.yml");
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "[CreeperAttack] Failed to create default config!", e);
    }
  }

  /**
   * Load values from configuration into cached fields.
   */
  private void loadValues() {
    // Trader config
    traderMaxHealth = config.getInt("trader.max_health", 100);
    explosionDamagePercent = config.getInt("trader.explosion_damage_percent", 10);
    triggerRadius = config.getDouble("trader.trigger_radius", 3.0);
    countdownSeconds = config.getInt("trader.countdown_seconds", 3);
    traderInvulnerable = config.getBoolean("trader.invulnerable", true);
    traderLocation = parseLocation(config.getString("trader.location", ""));
    
    // Lanes config
    for (int i = 1; i <= 4; i++) {
      Lane lane = lanes.get(i - 1);
      lane.setSpawn(parseLocation(config.getString("lanes.lane" + i + ".spawn", "")));
      lane.setEnd(parseLocation(config.getString("lanes.lane" + i + ".end", "")));
    }
    
    // Waves config
    maxWaves = config.getInt("waves.max_waves", 30);
    waveIntervalSeconds = config.getInt("waves.interval_seconds", 10);
    spawnPerWaveBase = config.getInt("waves.spawn_per_wave.base", 8);
    spawnPerWaveIncrease = config.getInt("waves.spawn_per_wave.increase", 2);
    creeperPercent = config.getInt("waves.mob_mix.creeper_percent", 100);
    batchSpawnSize = config.getInt("waves.batch_spawn.size", 4);
    batchSpawnPeriodTicks = config.getInt("waves.batch_spawn.period_ticks", 20);
    
    // Economy config
    killRewardCreeper = config.getInt("economy.kill_reward.creeper", 10);
    deathPenaltyPercent = Math.max(0, Math.min(100, config.getInt("economy.death_penalty_percent", 33)));
    
    // Shop config
    shopEnabled = config.getBoolean("shop.enabled", true);
    shopBetweenWavesOnly = config.getBoolean("shop.open_between_waves_only", false);
  }

  /**
   * Parse a location string in format "world,x,y,z" or "world,x,y,z,yaw,pitch".
   */
  private Location parseLocation(String str) {
    if (str == null || str.isEmpty()) return null;
    
    String[] parts = str.split(",");
    if (parts.length < 4) return null;
    
    try {
      World world = plugin.getServer().getWorld(parts[0].trim());
      if (world == null) return null;
      
      double x = Double.parseDouble(parts[1].trim());
      double y = Double.parseDouble(parts[2].trim());
      double z = Double.parseDouble(parts[3].trim());
      
      float yaw = 0, pitch = 0;
      if (parts.length >= 6) {
        yaw = Float.parseFloat(parts[4].trim());
        pitch = Float.parseFloat(parts[5].trim());
      }
      
      return new Location(world, x, y, z, yaw, pitch);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Convert a location to string format.
   */
  public String locationToString(Location loc) {
    if (loc == null || loc.getWorld() == null) return "";
    return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ()
        + "," + loc.getYaw() + "," + loc.getPitch();
  }

  /**
   * Save trader location to config.
   */
  public void saveTraderLocation(Location location) {
    traderLocation = location;
    config.set("trader.location", locationToString(location));
    saveConfig();
  }

  /**
   * Save lane spawn/end location to config.
   */
  public void saveLaneLocation(int laneId, String type, Location location) {
    if (laneId < 1 || laneId > 4) return;
    
    Lane lane = lanes.get(laneId - 1);
    if ("spawn".equalsIgnoreCase(type)) {
      lane.setSpawn(location);
      config.set("lanes.lane" + laneId + ".spawn", locationToString(location));
    } else if ("end".equalsIgnoreCase(type)) {
      lane.setEnd(location);
      config.set("lanes.lane" + laneId + ".end", locationToString(location));
    }
    saveConfig();
  }

  private void saveConfig() {
    try {
      config.save(configFile);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "[CreeperAttack] Failed to save config!", e);
    }
  }

  /**
   * Validate that all required configuration is present.
   * @return ValidationResult with ok=true if valid, or list of missing fields
   */
  public ValidationResult validate() {
    List<String> missing = new ArrayList<>();
    
    if (traderLocation == null) {
      missing.add("trader.location");
    }
    
    for (int i = 1; i <= 4; i++) {
      Lane lane = lanes.get(i - 1);
      if (lane.getSpawn() == null) {
        missing.add("lanes.lane" + i + ".spawn");
      }
      if (lane.getEnd() == null) {
        missing.add("lanes.lane" + i + ".end");
      }
    }
    
    return new ValidationResult(missing.isEmpty(), missing);
  }

  /**
   * Get a message from config with prefix.
   */
  public String getMessage(String key) {
    String prefix = config.getString("messages.prefix", "&8[&6苦力怕进攻&8] ");
    String msg = config.getString("messages." + key, "&cMissing message: " + key);
    return prefix + msg;
  }

  /**
   * Get a raw message without prefix.
   */
  public String getRawMessage(String key) {
    return config.getString("messages." + key, "&cMissing message: " + key);
  }

  /**
   * Get scoreboard title.
   */
  public String getScoreboardTitle() {
    return config.getString("scoreboard.title", "&6&l苦力怕进攻");
  }

  /**
   * Get scoreboard lines.
   */
  public List<String> getScoreboardLines() {
    return config.getStringList("scoreboard.lines");
  }

  /**
   * Get shop items configuration section.
   */
  public ConfigurationSection getShopItemsSection() {
    return config.getConfigurationSection("shop.items");
  }

  /**
   * Get low HP thresholds for trader warning.
   */
  public List<Integer> getTraderLowHpThresholds() {
    return config.getIntegerList("ui.trader_lowhp_thresholds");
  }

  // Getters for cached values
  public int getTraderMaxHealth() { return traderMaxHealth; }
  public int getExplosionDamagePercent() { return explosionDamagePercent; }
  public double getTriggerRadius() { return triggerRadius; }
  public int getCountdownSeconds() { return countdownSeconds; }
  public boolean isTraderInvulnerable() { return traderInvulnerable; }
  public Location getTraderLocation() { return traderLocation; }
  public List<Lane> getLanes() { return lanes; }
  
  public int getMaxWaves() { return maxWaves; }
  public int getWaveIntervalSeconds() { return waveIntervalSeconds; }
  public int getSpawnPerWaveBase() { return spawnPerWaveBase; }
  public int getSpawnPerWaveIncrease() { return spawnPerWaveIncrease; }
  public int getCreeperPercent() { return creeperPercent; }
  public int getBatchSpawnSize() { return batchSpawnSize; }
  public int getBatchSpawnPeriodTicks() { return batchSpawnPeriodTicks; }
  
  public int getKillRewardCreeper() { return killRewardCreeper; }
  public int getDeathPenaltyPercent() { return deathPenaltyPercent; }
  
  public boolean isShopEnabled() { return shopEnabled; }
  public boolean isShopBetweenWavesOnly() { return shopBetweenWavesOnly; }
  
  public boolean isScoreboardEnabled() { return config.getBoolean("ui.scoreboard.enabled", true); }
  public int getScoreboardRefreshSeconds() { return config.getInt("ui.scoreboard.refresh_seconds", 1); }
  public boolean isTitleEnabled() { return config.getBoolean("ui.title.enabled", true); }
  public boolean isChatEnabled() { return config.getBoolean("ui.chat.enabled", true); }

  public FileConfiguration getConfig() { return config; }

  /**
   * Validation result holder.
   */
  public static class ValidationResult {
    private final boolean ok;
    private final List<String> missingFields;

    public ValidationResult(boolean ok, List<String> missingFields) {
      this.ok = ok;
      this.missingFields = missingFields;
    }

    public boolean isOk() { return ok; }
    public List<String> getMissingFields() { return missingFields; }
  }
}
