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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.villagedefense.Main;
import plugily.projects.villagedefense.arena.Arena;
import plugily.projects.villagedefense.creeperattack.arena.ArenaContext;
import plugily.projects.villagedefense.creeperattack.arena.CAArenaManager;
import plugily.projects.villagedefense.creeperattack.npc.CitizensHook;

/**
 * Blocks damage to the Trader and handles shop interaction.
 */
public class TraderDamageBlockListener implements Listener {

  private final Main plugin;
  private final CAArenaManager arenaManager;
  private final CitizensHook citizensHook;

  public TraderDamageBlockListener(Main plugin, CAArenaManager arenaManager) {
    this.plugin = plugin;
    this.arenaManager = arenaManager;
    this.citizensHook = new CitizensHook(plugin);
  }

  /**
   * Block all damage to the Trader entity.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTraderDamage(EntityDamageEvent event) {
    if (event.getEntityType() != EntityType.VILLAGER) return;

    if(citizensHook.isAvailable()) {
      NPC npc = citizensHook.getNpc(event.getEntity());
      if(npc != null) {
        // trader protection is managed by Citizens metadata/protected flag.
        event.setCancelled(true);
        return;
      }
    }
    
    Villager villager = (Villager) event.getEntity();
    
    // Check if this is a Trader in any arena
    for (Arena arena : plugin.getArenaRegistry().getPluginArenas()) {
      if (arena.getArenaState() != IArenaState.IN_GAME) continue;
      
      ArenaContext context = arenaManager.getContext(arena.getId());
      if (context == null) continue;
      
      Villager trader = context.getTraderEntity();
      if (trader != null && trader.equals(villager)) {
        // Cancel all damage - Trader HP is managed separately
        event.setCancelled(true);
        return;
      }
    }
  }

  /**
   * Block player attacks on the Trader.
   */
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTraderAttack(EntityDamageByEntityEvent event) {
    if (event.getEntityType() != EntityType.VILLAGER) return;

    if(citizensHook.isAvailable()) {
      NPC npc = citizensHook.getNpc(event.getEntity());
      if(npc != null) {
        event.setCancelled(true);
        return;
      }
    }
    
    Villager villager = (Villager) event.getEntity();
    
    // Check if this is a Trader in any arena
    for (Arena arena : plugin.getArenaRegistry().getPluginArenas()) {
      if (arena.getArenaState() != IArenaState.IN_GAME) continue;
      
      ArenaContext context = arenaManager.getContext(arena.getId());
      if (context == null) continue;
      
      Villager trader = context.getTraderEntity();
      if (trader != null && trader.equals(villager)) {
        event.setCancelled(true);
        return;
      }
    }
  }

  /**
   * Handle right-click on Trader to open shop.
   */
  @EventHandler
  public void onTraderInteract(PlayerInteractEntityEvent event) {
    if (event.getRightClicked().getType() != EntityType.VILLAGER) return;
    
    Player player = event.getPlayer();
    Villager villager = (Villager) event.getRightClicked();

    if(citizensHook.isAvailable()) {
      NPC npc = citizensHook.getNpc(villager);
      if(npc == null) {
        return;
      }
    }
    
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if (arena == null || arena.getArenaState() != IArenaState.IN_GAME) return;
    
    ArenaContext context = arenaManager.getContext(arena.getId());
    if (context == null) return;
    
    Villager trader = context.getTraderEntity();
    if (trader != null && trader.equals(villager)) {
      event.setCancelled(true);
      arenaManager.openShop(player, arena);
    }
  }
}
