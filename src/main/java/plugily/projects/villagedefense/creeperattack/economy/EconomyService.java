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

package plugily.projects.villagedefense.creeperattack.economy;

import org.bukkit.entity.Player;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;

import java.util.UUID;

/**
 * Manages the in-game economy: coins, kill rewards, death penalties.
 */
public class EconomyService {

  private final Main plugin;
  private final ConfigService configService;

  public EconomyService(Main plugin, ConfigService configService) {
    this.plugin = plugin;
    this.configService = configService;
  }

  /**
   * Initialize player coins when they join the game.
   */
  public void initPlayer(ArenaContext context, UUID playerId) {
    context.setPlayerCoins(playerId, 0);
  }

  /**
   * Award kill reward to a player.
   * @return The amount of coins awarded
   */
  public int awardKillReward(ArenaContext context, Player player) {
    int reward = configService.getKillRewardCreeper();
    context.addPlayerCoins(player.getUniqueId(), reward);
    return reward;
  }

  /**
   * Apply death penalty to a player.
   * @return The amount of coins lost
   */
  public int applyDeathPenalty(ArenaContext context, Player player) {
    UUID playerId = player.getUniqueId();
    int current = context.getPlayerCoins(playerId);
    int penalty = (current * configService.getDeathPenaltyPercent()) / 100;
    context.subtractPlayerCoins(playerId, penalty);
    return penalty;
  }

  /**
   * Get player's current coins.
   */
  public int getCoins(ArenaContext context, Player player) {
    return context.getPlayerCoins(player.getUniqueId());
  }

  /**
   * Attempt to spend coins.
   * @return true if successful, false if insufficient funds
   */
  public boolean spend(ArenaContext context, Player player, int amount) {
    return context.subtractPlayerCoins(player.getUniqueId(), amount);
  }

  /**
   * Add coins to a player.
   */
  public void addCoins(ArenaContext context, Player player, int amount) {
    context.addPlayerCoins(player.getUniqueId(), amount);
  }

  /**
   * Clear player's coins (on leave/game end).
   */
  public void clearPlayer(ArenaContext context, UUID playerId) {
    context.clearPlayerCoins(playerId);
  }
}
