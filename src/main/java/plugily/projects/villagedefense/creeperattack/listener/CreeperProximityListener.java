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

import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Creeper proximity to Trader and explosion countdown.
 * Also handles Creeper knockback immunity via position restoration.
 */
public class CreeperProximityListener implements Listener {

  private final Main plugin;
  private final CAArenaManager arenaManager;
  private final ConfigService configService;
  private final CitizensHook citizensHook;
  
  // Track creeper positions for knockback compensation
  private final Map<UUID, Location> creeperLastPositions = new HashMap<>();
  private final Map<UUID, Long> creeperLastDamageTime = new HashMap<>();
  
  private static final long KNOCKBACK_COMPENSATION_WINDOW = 500; // 500ms
  private static final double CREEPER_SPEED = 0.15; // Movement speed per tick

  public CreeperProximityListener(Main plugin, CAArenaManager arenaManager, ConfigService configService) {
    this.plugin = plugin;
    this.arenaManager = arenaManager;
    this.configService = configService;
    this.citizensHook = new CitizensHook(plugin);
    
    // Start proximity check task
    startProximityCheckTask();
    
    // Start knockback compensation task
    startKnockbackCompensationTask();
    
    // Start creeper movement task - makes creepers walk toward trader
    startCreeperMovementTask();
  }

  /**
   * Start the proximity check task (runs every 5 ticks).
   */
  private void startProximityCheckTask() {
    new BukkitRunnable() {
      @Override
      public void run() {
        for (Arena arena : plugin.getArenaRegistry().getPluginArenas()) {
          if (arena.getArenaState() != IArenaState.IN_GAME) continue;
          
          ArenaContext context = arenaManager.getContext(arena.getId());
          if (context == null || !context.isFighting()) continue;
          
          checkProximity(arena, context);
          processCountdowns(arena, context);
        }
      }
    }.runTaskTimer(plugin, 0L, 5L);
  }

  /**
   * Start knockback compensation task.
   */
  private void startKnockbackCompensationTask() {
    new BukkitRunnable() {
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        
        for (Arena arena : plugin.getArenaRegistry().getPluginArenas()) {
          if (arena.getArenaState() != IArenaState.IN_GAME) continue;
          
          ArenaContext context = arenaManager.getContext(arena.getId());
          if (context == null) continue;
          
          for (Creeper creeper : context.getCreepers()) {
            if (creeper == null || creeper.isDead()) continue;
            
            UUID id = creeper.getUniqueId();
            Long lastDamage = creeperLastDamageTime.get(id);
            
            // If recently damaged, restore position
            if (lastDamage != null && now - lastDamage < KNOCKBACK_COMPENSATION_WINDOW) {
              Location lastPos = creeperLastPositions.get(id);
              if (lastPos != null && lastPos.getWorld().equals(creeper.getWorld())) {
                // Restore position but allow forward movement toward trader
                Location current = creeper.getLocation();
                Location traderLoc = context.getTraderLocation();
                
                if (traderLoc != null) {
                  double lastDist = lastPos.distanceSquared(traderLoc);
                  double currentDist = current.distanceSquared(traderLoc);
                  
                  // Only restore if creeper was pushed away from trader
                  if (currentDist > lastDist) {
                    creeper.teleport(lastPos);
                  }
                }
              }
            } else {
              // Update last known position
              creeperLastPositions.put(id, creeper.getLocation().clone());
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 2L);
  }

  /**
   * Start creeper movement task - makes creepers walk toward the trader.
   * This is necessary because Creepers don't naturally target Villagers.
   */
  private void startCreeperMovementTask() {
    new BukkitRunnable() {
      @Override
      public void run() {
        for (Arena arena : plugin.getArenaRegistry().getPluginArenas()) {
          if (arena.getArenaState() != IArenaState.IN_GAME) continue;
          
          ArenaContext context = arenaManager.getContext(arena.getId());
          if (context == null || !context.isFighting()) continue;
          
          Location traderLoc = context.getTraderLocation();
          if (traderLoc == null) continue;
          
          for (Creeper creeper : context.getCreepers()) {
            if (creeper == null || creeper.isDead()) continue;
            if (!creeper.getWorld().equals(traderLoc.getWorld())) continue;
            
            // Skip if frozen (has high slowness)
            if (creeper.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW)) {
              org.bukkit.potion.PotionEffect effect = null;
              for(org.bukkit.potion.PotionEffect active : creeper.getActivePotionEffects()) {
                if(active.getType().equals(org.bukkit.potion.PotionEffectType.SLOW)) {
                  effect = active;
                  break;
                }
              }
              if (effect != null && effect.getAmplifier() > 200) {
                continue; // Frozen creeper, skip movement
              }
            }
            
            // Citizens NPC pathfinding if available
            if(citizensHook.isAvailable()) {
              NPC npc = context.getCreeperNpc(creeper.getUniqueId());
              Villager trader = context.getTraderEntity();
              if(npc != null && trader != null && !trader.isDead()) {
                npc.getNavigator().setTarget(trader, true);
                continue;
              }
            }

            // Fallback manual movement toward trader
            Location creeperLoc = creeper.getLocation();
            double dx = traderLoc.getX() - creeperLoc.getX();
            double dz = traderLoc.getZ() - creeperLoc.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            if (dist > 0.5) { // Only move if not already very close
              // Normalize direction and apply speed
              double speed = CREEPER_SPEED;
              double vx = (dx / dist) * speed;
              double vz = (dz / dist) * speed;
              
              // Get current velocity and modify horizontal components
              Vector velocity = creeper.getVelocity();
              velocity.setX(vx);
              velocity.setZ(vz);
              
              creeper.setVelocity(velocity);
              
              // Make creeper look toward trader (1.9+ only - setRotation doesn't exist in 1.8)
              if (ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
                Location lookAt = creeperLoc.clone();
                lookAt.setDirection(new Vector(dx, 0, dz).normalize());
            // TODO(1.8.8): no setRotation API; if needed, use NMS yaw/pitch update.
              }
              // On 1.8, we skip rotation update to avoid NoSuchMethodError
            }
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 4L); // Run every 4 ticks (5 times per second)
  }

  /**
   * Check proximity of all creepers to the trader.
   */
  private void checkProximity(Arena arena, ArenaContext context) {
    Location traderLoc = context.getTraderLocation();
    if (traderLoc == null) return;
    
    double triggerRadius = configService.getTriggerRadius();
    double triggerRadiusSq = triggerRadius * triggerRadius;
    long currentTime = System.currentTimeMillis();
    int countdownMs = configService.getCountdownSeconds() * 1000;
    
    for (Creeper creeper : context.getCreepers()) {
      if (creeper == null || creeper.isDead()) continue;
      if (!creeper.getWorld().equals(traderLoc.getWorld())) continue;
      
      double distSq = creeper.getLocation().distanceSquared(traderLoc);
      UUID creeperId = creeper.getUniqueId();
      
      if (distSq <= triggerRadiusSq) {
        // Within trigger radius
        if (!context.hasCountdown(creeperId)) {
          // Start countdown
          long endTime = currentTime + countdownMs;
          context.startCountdown(creeperId, endTime);
          
          // Visual/audio feedback
          creeper.setCustomName("§c§l" + configService.getCountdownSeconds());
          creeper.setCustomNameVisible(true);
        }
      }
    }
  }

  /**
   * Process active countdowns and trigger explosions.
   */
  private void processCountdowns(Arena arena, ArenaContext context) {
    long currentTime = System.currentTimeMillis();
    
    for (Creeper creeper : context.getCreepers().toArray(new Creeper[0])) {
      if (creeper == null || creeper.isDead()) {
        context.clearCountdown(creeper != null ? creeper.getUniqueId() : null);
        continue;
      }
      
      UUID creeperId = creeper.getUniqueId();
      Long endTime = context.getCountdownEnd(creeperId);
      
      if (endTime == null) continue;
      
      int secondsLeft = (int) Math.ceil((endTime - currentTime) / 1000.0);
      
      if (secondsLeft <= 0) {
        // Explosion!
        arenaManager.handleCreeperExplosion(arena, context, creeper);
      } else {
        // Update countdown display
        creeper.setCustomName("§c§l" + secondsLeft);
        
        // Send warning to players
        if (secondsLeft <= 3) {
          arenaManager.getUiController().sendCreeperCountdownWarning(arena, secondsLeft);
        }
      }
    }
  }

  /**
   * Track creeper damage for knockback compensation.
   */
  @EventHandler
  public void onCreeperDamage(EntityDamageByEntityEvent event) {
    if (event.getEntityType() != EntityType.CREEPER) return;
    
    Creeper creeper = (Creeper) event.getEntity();
    
    // Check if this is our managed creeper
    List<MetadataValue> metadata = creeper.getMetadata("ca_arena_id");
    if (metadata.isEmpty() && !(citizensHook.isAvailable() && citizensHook.isNpc(creeper))) return;
    
    // Record damage time and position for knockback compensation
    UUID id = creeper.getUniqueId();
    creeperLastPositions.put(id, creeper.getLocation().clone());
    creeperLastDamageTime.put(id, System.currentTimeMillis());
  }

  /**
   * Prevent creeper from targeting players (they should walk toward trader).
   */
  @EventHandler
  public void onCreeperTarget(EntityTargetEvent event) {
    if (event.getEntityType() != EntityType.CREEPER) return;
    
    Creeper creeper = (Creeper) event.getEntity();
    
    // Check if this is our managed creeper
    List<MetadataValue> metadata = creeper.getMetadata("ca_arena_id");
    if (metadata.isEmpty() && !(citizensHook.isAvailable() && citizensHook.isNpc(creeper))) return;
    
    // Cancel targeting of players - creepers should walk toward trader
    Entity target = event.getTarget();
    if (target != null && target.getType() != EntityType.VILLAGER) {
      event.setCancelled(true);
    }
  }

  /**
   * Clean up tracking data when creeper is removed.
   */
  public void cleanupCreeper(UUID creeperId) {
    creeperLastPositions.remove(creeperId);
    creeperLastDamageTime.remove(creeperId);
  }
}
