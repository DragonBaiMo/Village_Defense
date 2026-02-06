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

package plugily.projects.villagedefense.creeperattack.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;

/**
 * Handles player death events for Creeper Attack mode.
 */
public class PlayerDeathListener implements Listener {

  private final Main plugin;
  private final CAArenaManager arenaManager;
  private final ConfigService configService;

  public PlayerDeathListener(Main plugin, CAArenaManager arenaManager, ConfigService configService) {
    this.plugin = plugin;
    this.arenaManager = arenaManager;
    this.configService = configService;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    
    if (arena == null || arena.getArenaState() != IArenaState.IN_GAME) return;
    
    ArenaContext context = arenaManager.getContext(arena.getId());
    if (context == null) return;
    
    // Apply death penalty
    arenaManager.handlePlayerDeath(arena, context, player);
  }
}
