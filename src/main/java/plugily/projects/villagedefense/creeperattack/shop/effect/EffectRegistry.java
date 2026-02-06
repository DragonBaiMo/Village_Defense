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
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.trader.TraderController;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for shop effect handlers.
 */
public class EffectRegistry {

  private final Map<String, ShopEffectHandler> handlers = new HashMap<>();

  public EffectRegistry(Main plugin, ConfigService configService) {
    // Register built-in handlers
    register(new GiveItemEffect(plugin));
    
    // TraderController will be set later via setTraderController
  }

  /**
   * Initialize handlers that require TraderController.
   */
  public void initWithTraderController(Main plugin, ConfigService configService, TraderController traderController) {
    register(new HealTraderEffect(plugin, configService, traderController));
    register(new FreezeCreeperEffect(plugin, configService));
  }

  /**
   * Register an effect handler.
   */
  public void register(ShopEffectHandler handler) {
    handlers.put(handler.getEffectType().toUpperCase(), handler);
  }

  /**
   * Get handler for effect type.
   */
  public ShopEffectHandler getHandler(String effectType) {
    return handlers.get(effectType.toUpperCase());
  }

  /**
   * Apply an effect using the appropriate handler.
   */
  public boolean applyEffect(String effectType, Player player, Arena arena, 
                             ArenaContext context, ConfigurationSection effectConfig) {
    ShopEffectHandler handler = getHandler(effectType);
    if (handler == null) {
      return false;
    }
    return handler.apply(player, arena, context, effectConfig);
  }
}
