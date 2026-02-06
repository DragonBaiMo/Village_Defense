/*
 *  Village Defense - Protect villagers from hordes of zombies
 *  Copyright (c) 2023 Plugily Projects
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

package plugily.projects.villagedefense.arena.managers;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.managers.PluginScoreboardManager;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.villagedefense.arena.Arena;

import java.util.ArrayList;
import java.util.List;

/**
 * Village Defense scoreboard lines provider.
 */
public class ScoreboardManager extends PluginScoreboardManager {

  private final PluginArena arena;

  public ScoreboardManager(PluginArena arena) {
    super(arena);
    this.arena = arena;
  }

  @Override
  public List<String> getScoreboardLines(Player player) {
    List<String> lines;

    IArenaState state = arena.getArenaState();
    if(state == IArenaState.FULL_GAME) {
      lines = arena.getPlugin().getLanguageManager().getLanguageList("Scoreboard.Content.Starting");
    } else if(state == IArenaState.IN_GAME) {
      String key = "Scoreboard.Content." + state.getFormattedName();
      if(arena instanceof Arena && !((Arena) arena).isFighting()) {
        key += "-Waiting";
      }
      lines = arena.getPlugin().getLanguageManager().getLanguageList(key);
    } else {
      lines = arena.getPlugin().getLanguageManager().getLanguageList("Scoreboard.Content." + state.getFormattedName());
    }

    // Apply message placeholders.
    List<String> built = new ArrayList<>(lines.size());
    for(String line : lines) {
      built.add(new MessageBuilder(line).player(player).arena(arena).build());
    }
    return built;
  }
}
