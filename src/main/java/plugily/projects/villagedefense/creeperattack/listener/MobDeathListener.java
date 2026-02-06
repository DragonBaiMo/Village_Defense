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
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.MetadataValue;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;

import java.util.List;

/**
 * Handles mob death events for Creeper Attack mode.
 */
public class MobDeathListener implements Listener {

  private final Main plugin;
  private final CAArenaManager arenaManager;
  private final CitizensHook citizensHook;

  public MobDeathListener(Main plugin, CAArenaManager arenaManager) {
    this.plugin = plugin;
    this.arenaManager = arenaManager;
    this.citizensHook = new CitizensHook(plugin);
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    if (event.getEntityType() != EntityType.CREEPER) return;
    
    Creeper creeper = (Creeper) event.getEntity();
    
    // Check if this is our managed creeper
    List<MetadataValue> metadata = creeper.getMetadata("ca_arena_id");
    if (metadata.isEmpty() && !(citizensHook.isAvailable() && citizensHook.isNpc(creeper))) return;

    String arenaId = null;
    if(!metadata.isEmpty()) {
      arenaId = metadata.get(0).asString();
    } else if(citizensHook.isAvailable()) {
      NPC npc = citizensHook.getNpc(creeper);
      if(npc != null) {
        // fallback: resolve arena from loaded contexts by entity list
        for (Arena a : plugin.getArenaRegistry().getPluginArenas()) {
          ArenaContext ctx = arenaManager.getContext(a.getId());
          if(ctx != null && ctx.getCreepers().contains(creeper)) {
            arenaId = a.getId();
            break;
          }
        }
      }
    }

    if(arenaId == null) return;
    Arena arena = plugin.getArenaRegistry().getArena(arenaId);
    if (arena == null || arena.getArenaState() != IArenaState.IN_GAME) return;
    
    ArenaContext context = arenaManager.getContext(arenaId);
    if (context == null) return;
    
    // Get killer
    Player killer = creeper.getKiller();
    
    // Handle the kill
    arenaManager.handleCreeperKill(arena, context, creeper, killer);
    
    // Clear drops (no rotten flesh from creepers)
    event.getDrops().clear();
    event.setDroppedExp(0);
  }
}
