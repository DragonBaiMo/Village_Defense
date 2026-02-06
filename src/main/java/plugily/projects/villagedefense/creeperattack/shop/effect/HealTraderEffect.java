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

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.trader.TraderController;

/**
 * Effect handler that heals the Trader.
 */
public class HealTraderEffect implements ShopEffectHandler {

  private final Main plugin;
  private final ConfigService configService;
  private final TraderController traderController;

  public HealTraderEffect(Main plugin, ConfigService configService, TraderController traderController) {
    this.plugin = plugin;
    this.configService = configService;
    this.traderController = traderController;
  }

  @Override
  public String getEffectType() {
    return "HEAL_TRADER";
  }

  @Override
  public boolean apply(Player player, Arena arena, ArenaContext context, ConfigurationSection effectConfig) {
    int healPercent = effectConfig.getInt("heal_percent", 20);
    
    // Check if trader is at max HP
    if (context.getTraderCurrentHp() >= context.getTraderMaxHp()) {
      player.sendMessage(ChatColor.YELLOW + "Trader is already at max HP!");
      return false;
    }
    
    int newHp = traderController.healTrader(context, healPercent);
    
    // Broadcast to arena
    String msg = configService.getRawMessage("heal_activated")
        .replace("%percent%", String.valueOf(healPercent));
    for (Player p : arena.getPlayers()) {
      p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
    
    return true;
  }
}
