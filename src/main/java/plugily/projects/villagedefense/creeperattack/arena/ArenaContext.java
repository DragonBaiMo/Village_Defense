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

package plugily.projects.villagedefense.creeperattack.arena;

import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.Nullable;
import net.citizensnpcs.api.npc.NPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores all Creeper Attack specific data for an arena.
 * This class centralizes game state to avoid polluting the base Arena class.
 */
public class ArenaContext {

  private final String arenaId;
  
  // Lane configuration (4 lanes)
  private final List<Lane> lanes = new ArrayList<>(4);
  
  // Trader state
  private Location traderLocation;
  private Villager traderEntity;
  private NPC traderNpc;
  private int traderMaxHp;
  private int traderCurrentHp;
  
  // Wave state
  private int waveIndex = 0;
  private int waveMax = 30;
  private boolean fighting = false;
  private int creepersToSpawn = 0;
  private int creepersSpawnedThisWave = 0;
  private long nextWaveAt = 0;
  
  // Creeper countdown tracking (key = creeper UUID, value = explosion tick)
  private final Map<UUID, Long> creeperCountdownEnd = new HashMap<>();
  
  // Economy (per-player coins)
  private final Map<UUID, Integer> coins = new HashMap<>();
  
  // Shop cooldowns (key = playerUUID:itemId, value = cooldown end time in millis)
  private final Map<String, Long> shopCooldowns = new HashMap<>();
  
  // Active creepers in this arena
  private final List<Creeper> creepers = new ArrayList<>();
  private final Map<UUID, NPC> creeperNpcs = new HashMap<>();
  
  // Task IDs for cleanup
  private int mainTaskId = -1;
  private int scoreboardTaskId = -1;

  public ArenaContext(String arenaId) {
    this.arenaId = arenaId;
    // Initialize 4 lanes
    for (int i = 1; i <= 4; i++) {
      lanes.add(new Lane(i));
    }
  }

  public String getArenaId() {
    return arenaId;
  }

  // Lane methods
  public List<Lane> getLanes() {
    return lanes;
  }

  @Nullable
  public Lane getLane(int laneId) {
    if (laneId < 1 || laneId > 4) return null;
    return lanes.get(laneId - 1);
  }

  public boolean areLanesValid() {
    for (Lane lane : lanes) {
      if (!lane.isValid()) return false;
    }
    return true;
  }

  public List<String> getMissingLaneInfo() {
    List<String> missing = new ArrayList<>();
    for (Lane lane : lanes) {
      if (lane.getSpawn() == null) {
        missing.add("lane" + lane.getLaneId() + ".spawn");
      }
      if (lane.getEnd() == null) {
        missing.add("lane" + lane.getLaneId() + ".end");
      }
    }
    return missing;
  }

  // Trader methods
  @Nullable
  public Location getTraderLocation() {
    return traderLocation;
  }

  public void setTraderLocation(Location location) {
    this.traderLocation = location;
  }

  @Nullable
  public Villager getTraderEntity() {
    return traderEntity;
  }

  public void setTraderEntity(Villager entity) {
    this.traderEntity = entity;
  }

  @Nullable
  public NPC getTraderNpc() {
    return traderNpc;
  }

  public void setTraderNpc(NPC traderNpc) {
    this.traderNpc = traderNpc;
  }

  public int getTraderMaxHp() {
    return traderMaxHp;
  }

  public void setTraderMaxHp(int maxHp) {
    this.traderMaxHp = maxHp;
  }

  public int getTraderCurrentHp() {
    return traderCurrentHp;
  }

  public void setTraderCurrentHp(int hp) {
    this.traderCurrentHp = Math.max(0, Math.min(hp, traderMaxHp));
  }

  public void damageTrader(int amount) {
    setTraderCurrentHp(traderCurrentHp - amount);
  }

  public void healTrader(int amount) {
    setTraderCurrentHp(traderCurrentHp + amount);
  }

  public boolean isTraderDead() {
    return traderCurrentHp <= 0;
  }

  // Wave methods
  public int getWaveIndex() {
    return waveIndex;
  }

  public void setWaveIndex(int wave) {
    this.waveIndex = wave;
  }

  public void incrementWave() {
    this.waveIndex++;
  }

  public int getWaveMax() {
    return waveMax;
  }

  public void setWaveMax(int max) {
    this.waveMax = max;
  }

  public boolean isFighting() {
    return fighting;
  }

  public void setFighting(boolean fighting) {
    this.fighting = fighting;
  }

  public int getCreepersToSpawn() {
    return creepersToSpawn;
  }

  public void setCreepersToSpawn(int count) {
    this.creepersToSpawn = count;
  }

  public void decrementCreepersToSpawn() {
    if (creepersToSpawn > 0) creepersToSpawn--;
  }

  public int getCreepersSpawnedThisWave() {
    return creepersSpawnedThisWave;
  }

  public void setCreepersSpawnedThisWave(int count) {
    this.creepersSpawnedThisWave = count;
  }

  public void incrementCreepersSpawned() {
    this.creepersSpawnedThisWave++;
  }

  public long getNextWaveAt() {
    return nextWaveAt;
  }

  public void setNextWaveAt(long tick) {
    this.nextWaveAt = tick;
  }

  // Creeper countdown methods
  public Map<UUID, Long> getCreeperCountdownEnd() {
    return creeperCountdownEnd;
  }

  public void startCountdown(UUID creeperId, long endTick) {
    creeperCountdownEnd.put(creeperId, endTick);
  }

  public void clearCountdown(UUID creeperId) {
    creeperCountdownEnd.remove(creeperId);
  }

  public boolean hasCountdown(UUID creeperId) {
    return creeperCountdownEnd.containsKey(creeperId);
  }

  @Nullable
  public Long getCountdownEnd(UUID creeperId) {
    return creeperCountdownEnd.get(creeperId);
  }

  // Economy methods
  public Map<UUID, Integer> getCoins() {
    return coins;
  }

  public int getPlayerCoins(UUID playerId) {
    return coins.getOrDefault(playerId, 0);
  }

  public void setPlayerCoins(UUID playerId, int amount) {
    coins.put(playerId, Math.max(0, amount));
  }

  public void addPlayerCoins(UUID playerId, int amount) {
    setPlayerCoins(playerId, getPlayerCoins(playerId) + amount);
  }

  public boolean subtractPlayerCoins(UUID playerId, int amount) {
    int current = getPlayerCoins(playerId);
    if (current < amount) return false;
    setPlayerCoins(playerId, current - amount);
    return true;
  }

  public void clearPlayerCoins(UUID playerId) {
    coins.remove(playerId);
  }

  // Shop cooldown methods
  public Map<String, Long> getShopCooldowns() {
    return shopCooldowns;
  }

  public boolean isOnCooldown(UUID playerId, String itemId, long currentTimeMillis) {
    String key = playerId.toString() + ":" + itemId;
    Long end = shopCooldowns.get(key);
    return end != null && currentTimeMillis < end;
  }

  public void setCooldown(UUID playerId, String itemId, long endTimeMillis) {
    String key = playerId.toString() + ":" + itemId;
    shopCooldowns.put(key, endTimeMillis);
  }

  // Creeper list methods
  public List<Creeper> getCreepers() {
    return creepers;
  }

  public void addCreeper(Creeper creeper) {
    creepers.add(creeper);
  }

  public void addCreeperNpc(UUID creeperId, NPC npc) {
    creeperNpcs.put(creeperId, npc);
  }

  public void removeCreeper(Creeper creeper) {
    creepers.remove(creeper);
    if(creeper != null) {
      creeperNpcs.remove(creeper.getUniqueId());
    }
  }

  @Nullable
  public NPC getCreeperNpc(UUID creeperId) {
    return creeperNpcs.get(creeperId);
  }

  public void removeCreeperNpc(UUID creeperId) {
    creeperNpcs.remove(creeperId);
  }

  public int getCreepersAlive() {
    // Clean up dead creepers
    creepers.removeIf(c -> c == null || c.isDead());
    return creepers.size();
  }

  // Task ID methods
  public int getMainTaskId() {
    return mainTaskId;
  }

  public void setMainTaskId(int id) {
    this.mainTaskId = id;
  }

  public int getScoreboardTaskId() {
    return scoreboardTaskId;
  }

  public void setScoreboardTaskId(int id) {
    this.scoreboardTaskId = id;
  }

  /**
   * Reset the context for a new game.
   */
  public void reset() {
    waveIndex = 0;
    fighting = false;
    creepersToSpawn = 0;
    creepersSpawnedThisWave = 0;
    nextWaveAt = 0;
    traderCurrentHp = traderMaxHp;
    creeperCountdownEnd.clear();
    coins.clear();
    shopCooldowns.clear();
    
    // Remove all creepers
    for (Creeper creeper : new ArrayList<>(creepers)) {
      if (creeper != null && !creeper.isDead()) {
        creeper.remove();
      }
    }
    creepers.clear();
    creeperNpcs.clear();
    
    // Remove trader entity
    if (traderEntity != null && !traderEntity.isDead()) {
      traderEntity.remove();
    }
    traderEntity = null;
    traderNpc = null;
  }
}
