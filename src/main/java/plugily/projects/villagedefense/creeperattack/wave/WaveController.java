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

package plugily.projects.villagedefense.creeperattack.wave;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.Lane;
import plugily.projects.villagedefense.creeperattack.config.ConfigService;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;

import java.util.List;
import java.util.Random;

/**
 * Controls wave progression: starting, spawning, ending waves.
 */
public class WaveController {

  private static final String METADATA_ARENA_ID = "ca_arena_id";
  private static final String METADATA_WAVE = "ca_wave";
  
  private final Main plugin;
  private final ConfigService configService;
  private final CitizensHook citizensHook;
  private final Random random = new Random();

  public WaveController(Main plugin, ConfigService configService, CitizensHook citizensHook) {
    this.plugin = plugin;
    this.configService = configService;
    this.citizensHook = citizensHook;
  }

  /**
   * Start a new wave.
   */
  public void startWave(ArenaContext context, Arena arena) {
    context.incrementWave();
    int wave = context.getWaveIndex();
    
    // Calculate creepers to spawn this wave
    int baseCount = configService.getSpawnPerWaveBase();
    int increase = configService.getSpawnPerWaveIncrease();
    int total = baseCount + (wave - 1) * increase;
    
    // Apply player count scaling
    int playerCount = arena.getPlayers().size();
    total = (int) Math.ceil(total * (0.5 + 0.5 * playerCount));
    
    context.setCreepersToSpawn(total);
    context.setCreepersSpawnedThisWave(0);
    context.setFighting(true);
    
    plugin.getLogger().info("[CreeperAttack] Wave " + wave + " started. Spawning " + total + " creepers.");
  }

  /**
   * End the current wave.
   */
  public void endWave(ArenaContext context, Arena arena) {
    context.setFighting(false);
    
    // Schedule next wave
    long nextWaveAt = System.currentTimeMillis() + (configService.getWaveIntervalSeconds() * 1000L);
    context.setNextWaveAt(nextWaveAt);
    
    plugin.getLogger().info("[CreeperAttack] Wave " + context.getWaveIndex() + " ended.");
  }

  /**
   * Spawn a batch of creepers.
   * @return Number of creepers spawned this call
   */
  public int spawnBatch(ArenaContext context, Arena arena) {
    int toSpawn = Math.min(configService.getBatchSpawnSize(), context.getCreepersToSpawn());
    if (toSpawn <= 0) return 0;
    
    List<Lane> lanes = configService.getLanes();
    int spawned = 0;
    
    for (int i = 0; i < toSpawn; i++) {
      // Pick a random lane
      Lane lane = lanes.get(random.nextInt(lanes.size()));
      Location spawnLoc = lane.getSpawn();
      
      if (spawnLoc == null || spawnLoc.getWorld() == null) continue;
      
      Creeper creeper = spawnCreeperEntity(spawnLoc, context);
      if (creeper == null) continue;
      configureCreeper(creeper, context, arena, lane);
      
      context.addCreeper(creeper);
      context.decrementCreepersToSpawn();
      context.incrementCreepersSpawned();
      spawned++;
    }
    
    return spawned;
  }

  private Creeper spawnCreeperEntity(Location spawnLoc, ArenaContext context) {
    if(citizensHook.isAvailable()) {
      NPC npc = citizensHook.createNpc(EntityType.CREEPER, "");
      if(npc != null && npc.spawn(spawnLoc) && npc.getEntity() instanceof Creeper) {
        npc.setUseMinecraftAI(false);
        npc.setProtected(false);
        npc.data().set(NPC.COLLIDABLE_METADATA, true);
        // TODO(1.8.8/Citizens): old Citizens build may not expose fluid push metadata constant.
        LookClose lookClose = npc.getOrAddTrait(LookClose.class);
        lookClose.lookClose(true);
        lookClose.setRange(10.0);
        Creeper creeper = (Creeper) npc.getEntity();
        context.addCreeperNpc(creeper.getUniqueId(), npc);
        return creeper;
      }
      if(npc != null) {
        citizensHook.safeDestroy(npc);
      }
    }

    return (Creeper) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.CREEPER);
  }

  /**
   * Configure a newly spawned Creeper.
   */
  private void configureCreeper(Creeper creeper, ArenaContext context, Arena arena, Lane lane) {
    // Metadata for tracking
    creeper.setMetadata(METADATA_ARENA_ID, new FixedMetadataValue(plugin, context.getArenaId()));
    creeper.setMetadata(METADATA_WAVE, new FixedMetadataValue(plugin, context.getWaveIndex()));
    
    // Prevent natural explosion
    // TODO(1.8.8): no setExplosionRadius API; if needed, cancel explosion event and handle custom damage.
    
    // Apply knockback resistance via slowness removal compensation
    // For 1.8 compatibility, we use behavior-based knockback compensation
    // instead of NMS attributes
    creeper.setRemoveWhenFarAway(false);
    
    // Apply speed buff based on wave
    int speedLevel = Math.min(context.getWaveIndex() / 10, 2);
    if (speedLevel > 0) {
      creeper.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedLevel - 1, false, false));
    }
    
    // Set target towards the lane end / trader
    Location target = lane.getEnd();
    if (target != null && creeper.getLocation().getWorld().equals(target.getWorld())) {
      // Use pathfinding to make creeper walk towards target
      setPathfindingTarget(creeper, target);
    }
  }

  /**
   * Set the pathfinding target for a creeper.
   * For 1.8 compatibility, we use a simple approach.
   */
  private void setPathfindingTarget(Creeper creeper, Location target) {
    // Find nearest player or use trader as attraction
    // The CreeperProximityListener will handle the actual targeting behavior
    // Here we just store the target location as metadata
    creeper.setMetadata("ca_target_x", new FixedMetadataValue(plugin, target.getX()));
    creeper.setMetadata("ca_target_y", new FixedMetadataValue(plugin, target.getY()));
    creeper.setMetadata("ca_target_z", new FixedMetadataValue(plugin, target.getZ()));
  }

  /**
   * Check if current wave is complete.
   */
  public boolean isWaveComplete(ArenaContext context) {
    return context.getCreepersToSpawn() <= 0 && context.getCreepersAlive() <= 0;
  }

  /**
   * Check if all waves are complete (victory condition).
   */
  public boolean isAllWavesComplete(ArenaContext context) {
    return context.getWaveIndex() >= context.getWaveMax() && isWaveComplete(context);
  }

  /**
   * Check if it's time to start the next wave.
   */
  public boolean shouldStartNextWave(ArenaContext context) {
    if (context.isFighting()) return false;
    return System.currentTimeMillis() >= context.getNextWaveAt();
  }

  /**
   * Get remaining time until next wave in seconds.
   */
  public int getSecondsUntilNextWave(ArenaContext context) {
    long remaining = context.getNextWaveAt() - System.currentTimeMillis();
    return (int) Math.max(0, remaining / 1000);
  }
}
