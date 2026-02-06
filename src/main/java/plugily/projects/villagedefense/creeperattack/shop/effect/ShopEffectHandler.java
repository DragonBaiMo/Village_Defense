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

package plugily.projects.villagedefense.creeperattack.shop.effect;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;

/**
 * Interface for shop effect handlers.
 * Implementations handle specific effect types like GIVE_ITEM, HEAL_TRADER, FREEZE_CREEPERS.
 */
public interface ShopEffectHandler {

  /**
   * Get the effect type this handler supports.
   */
  String getEffectType();

  /**
   * Apply the effect to the player/arena.
   * @param player The player who purchased
   * @param arena The arena instance
   * @param context The arena context
   * @param effectConfig The effect configuration section
   * @return true if effect applied successfully
   */
  boolean apply(Player player, Arena arena, ArenaContext context, ConfigurationSection effectConfig);
}
