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

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;

/**
 * Effect handler that gives items to the player.
 */
public class GiveItemEffect implements ShopEffectHandler {

  private final Main plugin;

  public GiveItemEffect(Main plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getEffectType() {
    return "GIVE_ITEM";
  }

  @Override
  public boolean apply(Player player, Arena arena, ArenaContext context, ConfigurationSection effectConfig) {
    // Get parent section (the item config)
    ConfigurationSection itemConfig = effectConfig.getParent();
    if (itemConfig == null) return false;
    
    String materialName = itemConfig.getString("material", "STONE");
    int amount = itemConfig.getInt("amount", 1);
    
    Material material = parseMaterial(materialName);
    if (material == null) {
      plugin.getLogger().warning("[CreeperAttack] Invalid material: " + materialName);
      return false;
    }
    
    ItemStack item = new ItemStack(material, amount);
    player.getInventory().addItem(item);
    return true;
  }

  /**
   * Parse material name with 1.8 compatibility.
   */
  @SuppressWarnings("deprecation")
  private Material parseMaterial(String name) {
    // Handle legacy material names for 1.8
    switch (name.toUpperCase()) {
      case "WOOD_SWORD":
        return getMaterialSafe("WOOD_SWORD", "WOODEN_SWORD");
      case "GOLD_SWORD":
        return getMaterialSafe("GOLD_SWORD", "GOLDEN_SWORD");
      case "RED_DYE":
        return getMaterialSafe("INK_SACK", "RED_DYE"); // 1.8 uses INK_SACK with data
      default:
        try {
          return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
          return null;
        }
    }
  }

  private Material getMaterialSafe(String legacyName, String modernName) {
    try {
      return Material.valueOf(modernName);
    } catch (IllegalArgumentException e) {
      try {
        return Material.valueOf(legacyName);
      } catch (IllegalArgumentException e2) {
        return null;
      }
    }
  }
}
