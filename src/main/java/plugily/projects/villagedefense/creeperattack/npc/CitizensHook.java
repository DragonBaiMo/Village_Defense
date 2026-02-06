package plugily.projects.villagedefense.creeperattack.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import plugily.projects.villagedefense.Main;

/**
 * Minimal Citizens integration layer for 1.8.8 build.
 */
public class CitizensHook {

  private final Main plugin;
  private final boolean available;

  public CitizensHook(Main plugin) {
    this.plugin = plugin;
    Plugin citizens = plugin.getServer().getPluginManager().getPlugin("Citizens");
    this.available = citizens != null && citizens.isEnabled() && CitizensAPI.hasImplementation();
  }

  public boolean isAvailable() {
    return available;
  }

  public NPCRegistry getRegistry() {
    return CitizensAPI.getNPCRegistry();
  }

  public NPC createNpc(EntityType type, String name) {
    if(!available) {
      return null;
    }
    return getRegistry().createNPC(type, name);
  }

  public boolean isNpc(Entity entity) {
    return available && entity != null && getRegistry().isNPC(entity);
  }

  public NPC getNpc(Entity entity) {
    if(!available || entity == null) {
      return null;
    }
    return getRegistry().getNPC(entity);
  }

  public void safeDestroy(NPC npc) {
    if(npc == null) {
      return;
    }
    try {
      npc.destroy();
    } catch(Exception ignored) {
      // best effort
    }
  }
}
