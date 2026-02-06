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

package plugily.projects.villagedefense.creeperattack;

import org.bukkit.Bukkit;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.command.CACommandExecutor;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.listener.CreeperProximityListener;
import plugily.projects.villagedefense.creeperattack.listener.MobDeathListener;
import plugily.projects.villagedefense.creeperattack.listener.PlayerDeathListener;
import plugily.projects.villagedefense.creeperattack.listener.TraderDamageBlockListener;
import plugily.projects.villagedefense.creeperattack.shop.effect.EffectRegistry;

import java.util.logging.Level;

/**
 * Entry point for Creeper Attack mode.
 * Initializes all sub-systems and registers listeners/commands.
 */
public class CreeperAttackMode {

  private final Main plugin;
  private final ConfigService configService;
  private final CAArenaManager arenaManager;
  private final EffectRegistry effectRegistry;

  public CreeperAttackMode(Main plugin) {
    this.plugin = plugin;

    if (plugin.getServer().getPluginManager().getPlugin("Citizens") == null) {
      plugin.getLogger().warning("[CreeperAttack] Citizens not found. NPC-based behavior will fallback to vanilla entities.");
    }
    
    // Initialize config service and generate default config
    this.configService = new ConfigService(plugin);
    this.configService.loadOrCreate();
    
    // Initialize effect registry for shop effects
    this.effectRegistry = new EffectRegistry(plugin, configService);
    
    // Initialize arena manager
    this.arenaManager = new CAArenaManager(plugin, configService, effectRegistry);
    
    // Register event listeners
    registerListeners();
    
    // Register command executor
    registerCommands();
    
    plugin.getLogger().log(Level.INFO, "[CreeperAttack] Mode initialized successfully");
  }

  private void registerListeners() {
    Bukkit.getPluginManager().registerEvents(new CreeperProximityListener(plugin, arenaManager, configService), plugin);
    Bukkit.getPluginManager().registerEvents(new MobDeathListener(plugin, arenaManager), plugin);
    Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(plugin, arenaManager, configService), plugin);
    Bukkit.getPluginManager().registerEvents(new TraderDamageBlockListener(plugin, arenaManager), plugin);
  }

  private void registerCommands() {
    CACommandExecutor executor = new CACommandExecutor(plugin, configService, arenaManager);
    if (plugin.getCommand("ca") != null) {
      plugin.getCommand("ca").setExecutor(executor);
      plugin.getCommand("ca").setTabCompleter(executor);
    }
  }

  public void reload() {
    configService.reload();
    arenaManager.reloadAllContexts();
    plugin.getLogger().log(Level.INFO, "[CreeperAttack] Configuration reloaded");
  }

  public void shutdown() {
    arenaManager.shutdownAll();
    plugin.getLogger().log(Level.INFO, "[CreeperAttack] Mode shut down");
  }

  public ConfigService getConfigService() {
    return configService;
  }

  public CAArenaManager getArenaManager() {
    return arenaManager;
  }

  public EffectRegistry getEffectRegistry() {
    return effectRegistry;
  }
}
