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

package plugily.projects.villagedefense.creeperattack.shop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.economy.EconomyService;
import plugily.projects.villagedefense.creeperattack.shop.effect.EffectRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the Creeper Attack shop GUI.
 */
public class ShopController implements Listener {

  private static final String SHOP_TITLE = "§6§lCreeperAttack - Shop";
  
  private final Main plugin;
  private final ConfigService configService;
  private final CAArenaManager arenaManager;
  private final EconomyService economyService;
  private final EffectRegistry effectRegistry;
  
  // Mapping of slot -> itemId
  private final Map<Integer, String> slotToItemId = new HashMap<>();

  public ShopController(Main plugin, ConfigService configService, CAArenaManager arenaManager,
                        EconomyService economyService, EffectRegistry effectRegistry) {
    this.plugin = plugin;
    this.configService = configService;
    this.arenaManager = arenaManager;
    this.economyService = economyService;
    this.effectRegistry = effectRegistry;
    
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  /**
   * Open the shop GUI for a player.
   */
  public void openShop(Player player, Arena arena, ArenaContext context) {
    if (!configService.isShopEnabled()) {
      player.sendMessage(ChatColor.RED + "Shop is disabled");
      return;
    }
    
    // Check if shop is only available between waves
    if (configService.isShopBetweenWavesOnly() && context.isFighting()) {
      player.sendMessage(ChatColor.RED + "Shop is only available between waves!");
      return;
    }
    
    Inventory inv = Bukkit.createInventory(null, 27, SHOP_TITLE);
    slotToItemId.clear();
    
    ConfigurationSection items = configService.getShopItemsSection();
    if (items == null) {
      player.sendMessage(ChatColor.RED + "Shop config error!");
      return;
    }
    
    int slot = 0;
    for (String itemId : items.getKeys(false)) {
      if (slot >= 27) break;
      
      ConfigurationSection itemConfig = items.getConfigurationSection(itemId);
      if (itemConfig == null) continue;
      
      ItemStack displayItem = createDisplayItem(itemConfig, player, context);
      if (displayItem != null) {
        inv.setItem(slot, displayItem);
        slotToItemId.put(slot, itemId);
        slot++;
      }
    }
    
    player.openInventory(inv);
  }

  /**
   * Create a display item for the shop.
   */
  @SuppressWarnings("deprecation")
  private ItemStack createDisplayItem(ConfigurationSection itemConfig, Player player, ArenaContext context) {
    String materialName = itemConfig.getString("material", "STONE");
    Material material = parseMaterial(materialName);
    if (material == null) material = Material.STONE;
    
    int amount = itemConfig.getInt("amount", 1);
    ItemStack item = new ItemStack(material, amount);
    
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      // Name
      String name = itemConfig.getString("name", materialName);
      meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
      
      // Lore
      List<String> lore = new ArrayList<>();
      for (String line : itemConfig.getStringList("lore")) {
        lore.add(ChatColor.translateAlternateColorCodes('&', line));
      }
      
      // Add price and cooldown info
      int price = itemConfig.getInt("price", 0);
      lore.add("");
      lore.add("§7价格: §e" + price + " 金币");
      lore.add("§7你的金币: §e" + context.getPlayerCoins(player.getUniqueId()));
      
      // Check cooldown
      int cooldownSeconds = itemConfig.getInt("cooldown_seconds", 0);
      if (cooldownSeconds > 0) {
        String itemId = itemConfig.getName();
        long currentTick = System.currentTimeMillis();
        if (context.isOnCooldown(player.getUniqueId(), itemId, currentTick)) {
          Long endTick = context.getShopCooldowns().get(player.getUniqueId().toString() + ":" + itemId);
          int remaining = (int) Math.max(0, (endTick - currentTick) / 1000);
          lore.add("§c冷却中: " + remaining + " 秒");
        } else {
          lore.add("§7冷却: " + cooldownSeconds + " 秒");
        }
      }
      
      meta.setLore(lore);
      item.setItemMeta(meta);
    }
    
    return item;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) return;
    
    String title = event.getView().getTitle();
    if (!SHOP_TITLE.equals(title)) return;
    
    event.setCancelled(true);
    
    Player player = (Player) event.getWhoClicked();
    int slot = event.getRawSlot();
    
    String itemId = slotToItemId.get(slot);
    if (itemId == null) return;
    
    // Get arena context
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if (arena == null) return;
    
    ArenaContext context = arenaManager.getContext(arena.getId());
    if (context == null) return;
    
    // Process purchase
    processPurchase(player, arena, context, itemId);
    
    // Refresh the inventory
    player.closeInventory();
    openShop(player, arena, context);
  }

  /**
   * Process a shop purchase.
   */
  private void processPurchase(Player player, Arena arena, ArenaContext context, String itemId) {
    ConfigurationSection items = configService.getShopItemsSection();
    if (items == null) return;
    
    ConfigurationSection itemConfig = items.getConfigurationSection(itemId);
    if (itemConfig == null) return;
    
    int price = itemConfig.getInt("price", 0);
    int cooldownSeconds = itemConfig.getInt("cooldown_seconds", 0);
    
    // Check cooldown
    long currentTick = System.currentTimeMillis();
    if (context.isOnCooldown(player.getUniqueId(), itemId, currentTick)) {
      String msg = configService.getMessage("shop_cooldown");
      Long endTick = context.getShopCooldowns().get(player.getUniqueId().toString() + ":" + itemId);
      int remaining = (int) Math.max(0, (endTick - currentTick) / 1000);
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
          msg.replace("%seconds%", String.valueOf(remaining))));
      return;
    }
    
    // Check funds
    if (!economyService.spend(context, player, price)) {
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
          configService.getMessage("shop_not_enough")));
      return;
    }
    
    // Apply effect
    ConfigurationSection effectConfig = itemConfig.getConfigurationSection("effect");
    if (effectConfig != null) {
      String effectType = effectConfig.getString("type", "GIVE_ITEM");
      boolean success = effectRegistry.applyEffect(effectType, player, arena, context, effectConfig);
      
      if (!success) {
        // Refund on failure
        economyService.addCoins(context, player, price);
        return;
      }
    }
    
    // Set cooldown
    if (cooldownSeconds > 0) {
      long endTick = currentTick + (cooldownSeconds * 1000L);
      context.setCooldown(player.getUniqueId(), itemId, endTick);
    }
    
    // Success message
    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
        configService.getMessage("shop_success")));
  }

  /**
   * Parse material name with 1.8 compatibility.
   */
  @SuppressWarnings("deprecation")
  private Material parseMaterial(String name) {
    // Handle legacy material names
    switch (name.toUpperCase()) {
      case "WOOD_SWORD":
        return getMaterialSafe("WOOD_SWORD", "WOODEN_SWORD");
      case "GOLD_SWORD":
        return getMaterialSafe("GOLD_SWORD", "GOLDEN_SWORD");
      case "RED_DYE":
        return getMaterialSafe("INK_SACK", "RED_DYE");
      case "SNOW_BALL":
        return getMaterialSafe("SNOW_BALL", "SNOWBALL");
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
