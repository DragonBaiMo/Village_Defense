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
import org.jetbrains.annotations.Nullable;

/**
 * Represents a spawn lane for Creepers.
 * Each lane has a spawn point and an end point (usually near the Trader).
 */
public class Lane {

  private final int laneId;
  private Location spawn;
  private Location end;

  public Lane(int laneId) {
    this.laneId = laneId;
  }

  public Lane(int laneId, @Nullable Location spawn, @Nullable Location end) {
    this.laneId = laneId;
    this.spawn = spawn;
    this.end = end;
  }

  public int getLaneId() {
    return laneId;
  }

  @Nullable
  public Location getSpawn() {
    return spawn;
  }

  public void setSpawn(Location spawn) {
    this.spawn = spawn;
  }

  @Nullable
  public Location getEnd() {
    return end;
  }

  public void setEnd(Location end) {
    this.end = end;
  }

  /**
   * Check if this lane has valid spawn and end locations.
   */
  public boolean isValid() {
    return spawn != null && spawn.getWorld() != null
        && end != null && end.getWorld() != null;
  }

  @Override
  public String toString() {
    return "Lane{id=" + laneId + ", spawn=" + (spawn != null ? spawn.toString() : "null")
        + ", end=" + (end != null ? end.toString() : "null") + "}";
  }
}
