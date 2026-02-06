/*
 *  Village Defense - Protect villagers from hordes of zombies
 *
 *  1.8.8-only source set: this interface must NOT reference 1.9+ Bukkit APIs
 *  (e.g. org.bukkit.attribute.Attribute).
 */

package plugily.projects.villagedefense.creatures;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;

/**
 * Version-safe creature initializer contract.
 */
public interface BaseCreatureInitializer {
  Villager spawnVillager(Location location);

  Wolf spawnWolf(Location location);

  IronGolem spawnGolem(Location location);

  Creature spawnFastZombie(Location location);

  Creature spawnBabyZombie(Location location);

  Creature spawnHardZombie(Location location);

  Creature spawnPlayerBuster(Location location);

  Creature spawnGolemBuster(Location location);

  Creature spawnVillagerBuster(Location location);

  Creature spawnKnockbackResistantZombies(Location location);

  Creature spawnVillagerSlayer(Location location);

  /**
   * 1.8 fallback: best-effort no-op.
   */
  default void applyFollowRange(Creature zombie) {
    // 1.8 does not expose Attribute API; handled in v1_8_R3 NMS implementation.
  }

  /**
   * 1.8 fallback: best-effort no-op.
   */
  default void applyDamageModifier(LivingEntity entity, double value) {
    // handled in NMS implementation
  }

  /**
   * 1.8 fallback: best-effort no-op.
   */
  default void applySpeedModifier(LivingEntity entity, double value) {
    // handled in NMS implementation
  }
}
