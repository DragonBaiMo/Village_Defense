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
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Effect handler that freezes all Creepers in the arena.
 * Uses slowness and position lock as 1.8-compatible freeze mechanism.
 */
public class FreezeCreeperEffect implements ShopEffectHandler {

  private final Main plugin;
  private final ConfigService configService;

  public FreezeCreeperEffect(Main plugin, ConfigService configService) {
    this.plugin = plugin;
    this.configService = configService;
  }

  @Override
  public String getEffectType() {
    return "FREEZE_CREEPERS";
  }

  @Override
  public boolean apply(Player player, Arena arena, ArenaContext context, ConfigurationSection effectConfig) {
    int durationSeconds = effectConfig.getInt("duration_seconds", 5);
    int durationTicks = durationSeconds * 20;
    
    List<Creeper> creepers = new ArrayList<>(context.getCreepers());
    if (creepers.isEmpty()) {
      player.sendMessage(ChatColor.YELLOW + "No creepers to freeze!");
      return false;
    }
    
    // Store original positions for position lock
    Map<UUID, Location> originalPositions = new HashMap<>();
    
    for (Creeper creeper : creepers) {
      if (creeper == null || creeper.isDead()) continue;
      
      // Store original position
      originalPositions.put(creeper.getUniqueId(), creeper.getLocation().clone());
      
      // Apply high-level slowness to simulate freeze
      creeper.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationTicks, 255, false, false));
      
      // Visual effect - Glowing is 1.9+ only
      if (ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
        // TODO(1.8.8): no PotionEffectType.GLOWING; consider particles or nameplate as replacement.
      }
    }
    
    // Position lock task (fallback for 1.8 where slowness might not fully stop movement)
    new BukkitRunnable() {
      int ticks = 0;
      
      @Override
      public void run() {
        if (ticks >= durationTicks) {
          cancel();
          return;
        }
        
        for (Creeper creeper : context.getCreepers()) {
          if (creeper == null || creeper.isDead()) continue;
          
          Location original = originalPositions.get(creeper.getUniqueId());
          if (original != null && original.getWorld().equals(creeper.getWorld())) {
            // Teleport back to original position if moved
            if (creeper.getLocation().distanceSquared(original) > 0.25) {
              creeper.teleport(original);
            }
          }
        }
        
        ticks += 5;
      }
    }.runTaskTimer(plugin, 0L, 5L);
    
    // Broadcast to arena
    String msg = configService.getRawMessage("freeze_activated")
        .replace("%seconds%", String.valueOf(durationSeconds));
    for (Player p : arena.getPlayers()) {
      p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }
    
    return true;
  }
}
