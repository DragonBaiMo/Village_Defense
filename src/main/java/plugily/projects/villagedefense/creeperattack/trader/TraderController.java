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

package plugily.projects.villagedefense.creeperattack.trader;

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;

/**
 * Manages the Trader entity: spawning, HP tracking, damage/heal operations.
 */
public class TraderController {

  private final Main plugin;
  private final ConfigService configService;
  private final CitizensHook citizensHook;

  public TraderController(Main plugin, ConfigService configService, CitizensHook citizensHook) {
    this.plugin = plugin;
    this.configService = configService;
    this.citizensHook = citizensHook;
  }

  /**
   * Spawn the Trader entity at the configured location.
   * @param context The arena context
   * @return true if spawn successful, false otherwise
   */
  public boolean spawnTrader(ArenaContext context) {
    Location location = configService.getTraderLocation();
    if (location == null || location.getWorld() == null) {
      plugin.getLogger().warning("[CreeperAttack] Cannot spawn Trader: location not configured");
      return false;
    }

    // Prefer Citizens NPC when available.
    if(citizensHook.isAvailable()) {
      NPC traderNpc = citizensHook.createNpc(org.bukkit.entity.EntityType.VILLAGER, "§6§l商人");
      if(traderNpc != null && traderNpc.spawn(location)) {
        traderNpc.setUseMinecraftAI(false);
        traderNpc.setProtected(configService.isTraderInvulnerable());
        traderNpc.data().set(NPC.SILENT_METADATA, true);
        traderNpc.data().set(NPC.COLLIDABLE_METADATA, false);
        // TODO(1.8.8/Citizens): old Citizens build may not expose fluid push metadata constant.

        if(traderNpc.getEntity() instanceof Villager) {
          Villager villager = (Villager) traderNpc.getEntity();
          villager.setCustomNameVisible(true);
          villager.setRemoveWhenFarAway(false);
          VersionUtils.setMaxHealth(villager, 20.0);
          villager.setHealth(20.0);
          context.setTraderEntity(villager);
        } else {
          plugin.getLogger().warning("[CreeperAttack] Citizens trader entity is not a Villager");
          citizensHook.safeDestroy(traderNpc);
          return false;
        }

        context.setTraderNpc(traderNpc);
        context.setTraderLocation(location);
        context.setTraderMaxHp(configService.getTraderMaxHealth());
        context.setTraderCurrentHp(configService.getTraderMaxHealth());
        return true;
      }
      plugin.getLogger().warning("[CreeperAttack] Failed to spawn Citizens trader NPC, fallback to Bukkit villager");
    }

    // Fallback: Spawn villager directly
    Villager villager = location.getWorld().spawn(location, Villager.class);
    
    // Configure villager
    villager.setCustomName("§6§l商人");
    villager.setCustomNameVisible(true);
    villager.setRemoveWhenFarAway(false);
    
    // 1.8 Compatibility: setAI, setInvulnerable, setSilent are 1.9+ API
    if (ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
      // TODO(1.8.8): implement AI/invulnerable/silent via NMS or packets (APIs do not exist in 1.8).
    } else {
      // 1.8 fallback: Apply high slowness to prevent movement
      villager.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 255, false, false));
      villager.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 128, false, false));
      // Invulnerability is handled by TraderDamageBlockListener
    }
    
    // Set max health for visual purposes (actual HP tracked in context)
    VersionUtils.setMaxHealth(villager, 20.0);
    villager.setHealth(20.0);
    
    // Store in context
    context.setTraderEntity(villager);
    context.setTraderNpc(null);
    context.setTraderLocation(location);
    context.setTraderMaxHp(configService.getTraderMaxHealth());
    context.setTraderCurrentHp(configService.getTraderMaxHealth());
    
    return true;
  }

  /**
   * Apply explosion damage to the Trader.
   * @param context The arena context
   * @return The new HP value
   */
  public int applyExplosionDamage(ArenaContext context) {
    int damage = (context.getTraderMaxHp() * configService.getExplosionDamagePercent()) / 100;
    context.damageTrader(damage);
    updateTraderVisual(context);
    return context.getTraderCurrentHp();
  }

  /**
   * Heal the Trader by a percentage of max HP.
   * @param context The arena context
   * @param percent Heal percentage (0-100)
   * @return The new HP value
   */
  public int healTrader(ArenaContext context, int percent) {
    int heal = (context.getTraderMaxHp() * percent) / 100;
    context.healTrader(heal);
    updateTraderVisual(context);
    return context.getTraderCurrentHp();
  }

  /**
   * Update the visual display of the Trader (name with HP bar).
   */
  public void updateTraderVisual(ArenaContext context) {
    Villager trader = context.getTraderEntity();
    if (trader == null || trader.isDead()) return;
    
    int current = context.getTraderCurrentHp();
    int max = context.getTraderMaxHp();
    int percent = (current * 100) / max;
    
    // Create HP bar
    String hpBar = createHpBar(percent);
    
    // Update name
    trader.setCustomName("§6§l商人 " + hpBar + " §c" + current + "§7/§c" + max);
  }

  /**
   * Create a visual HP bar string.
   */
  private String createHpBar(int percent) {
    int bars = 10;
    int filled = (percent * bars) / 100;
    
    StringBuilder sb = new StringBuilder("§8[");
    for (int i = 0; i < bars; i++) {
      if (i < filled) {
        if (percent > 50) {
          sb.append("§a|");
        } else if (percent > 25) {
          sb.append("§e|");
        } else {
          sb.append("§c|");
        }
      } else {
        sb.append("§7|");
      }
    }
    sb.append("§8]");
    return sb.toString();
  }

  /**
   * Remove the Trader entity.
   */
  public void removeTrader(ArenaContext context) {
    Villager trader = context.getTraderEntity();
    if (trader != null && !trader.isDead()) {
      trader.remove();
    }

    if(context.getTraderNpc() != null) {
      citizensHook.safeDestroy(context.getTraderNpc());
    }
    context.setTraderEntity(null);
    context.setTraderNpc(null);
  }

  /**
   * Check if the Trader entity still exists.
   */
  public boolean isTraderAlive(ArenaContext context) {
    Villager trader = context.getTraderEntity();
    return trader != null && !trader.isDead() && context.getTraderCurrentHp() > 0;
  }
}
