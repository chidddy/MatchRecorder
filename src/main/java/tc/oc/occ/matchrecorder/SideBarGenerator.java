package tc.oc.occ.matchrecorder;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import tc.oc.pgm.api.Config;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.map.MapTag;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.blitz.BlitzMatchModule;
import tc.oc.pgm.ffa.Tribute;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.GoalMatchModule;
import tc.oc.pgm.goals.ProximityGoal;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

/*
 * contrust all the necesarry rows (16)
 * from fb-hash:0-15
 * just update the display name of fb-hash:0-15
 *
 *
 *
 */
@SuppressWarnings("unused")
public class SideBarGenerator {
  private final Replay recorder;
  private Match match = null;
  private String baseString = null;

  private static final int MAX_ROWS = 16; // Max rows on the scoreboard
  private static final int MAX_LENGTH = 30; // Max characters per line allowed
  private static final int MAX_TEAM = 16; // Max characters per team name
  private static final int MAX_TITLE = 32; // Max characters allowed in title

  // use this as a cache so that:
  // for any generated string that != the string in here
  // update it
  private final List<String> rows = new ArrayList<String>(MAX_ROWS);
  private final Map<Goal, BlinkTask> blinkingGoals = new HashMap<>();

  public SideBarGenerator(Replay recorder, Match match) {
    this.recorder = recorder;
    this.baseString = "sb-" + UUID.randomUUID().toString().substring(0, 8);
    this.match = match;
  }

  public void createSidebar() {
    // header
    recorder.addPacket(
        PacketBuilder.createScoreboardObjectivePacket_Create(
            this.baseString,
            LegacyComponentSerializer.legacySection()
                .serialize(
                    TextTranslations.translate(
                        this.constructHeader(PGM.get().getConfiguration(), this.match.getMap()),
                        Locale.ENGLISH))));
    recorder.addPacket(PacketBuilder.createScoreboardDisplayObjectivePacket(this.baseString));
    IntStream.range(0, MAX_ROWS)
        .forEach(
            idx -> {
              rows.add(this.baseString + ":" + idx);
            });
  }

  public void displayUpdatedSidebar(List<String> new_rows) {
    for (int i = 0; i < MAX_ROWS; i++) {
      String cached_row = this.rows.get(i);
      String row = new_rows.get(i);
      if (cached_row.equals(row)) continue;
      this.rows.set(i, row);
      if (row == this.baseString + ":" + i) {
        removeTeamPacket(i);
      } else {
        createTeamPacket(row, i, cached_row.equals(this.baseString + ":" + i));
      }
    }
  }

  private void removeTeamPacket(int index) {
    recorder.addPacket(PacketBuilder.createScoreboardScorePacket_Remove(this.baseString, index));
    recorder.addPacket(
        PacketBuilder.createScoreboardTeamPacket_Remove(this.baseString + ":" + index));
  }

  private void createTeamPacket(String text, int idx, boolean create) {
    String prefix = "";
    String suffix = "";
    if (text == "") {
      prefix = "§" + idx + "§r";
    } else if (text.length() <= MAX_TEAM) {
      prefix = text;
    } else {
      int index = text.charAt(MAX_TEAM - 1) == ChatColor.COLOR_CHAR ? (MAX_TEAM - 1) : MAX_TEAM;
      prefix = text.substring(0, index);
      String suffixTmp = text.substring(index);
      ChatColor chatColor = null;

      if (suffixTmp.length() >= 2 && suffixTmp.charAt(0) == ChatColor.COLOR_CHAR) {
        chatColor = ChatColor.getByChar(suffixTmp.charAt(1));
      }

      String color = ChatColor.getLastColors(prefix);
      boolean addColor = chatColor == null || chatColor.isFormat();

      suffix = (addColor ? (color.isEmpty() ? ChatColor.RESET.toString() : color) : "") + suffixTmp;
    }

    if (prefix.length() > MAX_TEAM || (suffix != null && suffix.length() > MAX_TEAM)) {
      // Something went wrong, just cut to prevent client crash/kick
      prefix = prefix.substring(0, MAX_TEAM);
      suffix = (suffix != null) ? suffix.substring(0, MAX_TEAM) : "";
    }
    if (create) {
      recorder.addPacket(PacketBuilder.createScoreboardScorePacket_Order(this.baseString, idx));
      recorder.addPacket(
          PacketBuilder.createScoreboardTeamPacket_Create(
              this.baseString + ":" + idx, prefix, suffix));
    } else {
      recorder.addPacket(
          PacketBuilder.createScoreboardTeamPacket_Modify(
              this.baseString + ":" + idx, prefix, suffix));
    }
  }

  private boolean hasScores() {
    return match.getModule(ScoreMatchModule.class) != null;
  }

  private boolean isBlitz() {
    return match.getModule(BlitzMatchModule.class) != null;
  }

  // Determines if wool objectives should be given their own rows, or all shown on
  // 1 row.
  private boolean isCompactWool() {
    WoolMatchModule wmm = match.getModule(WoolMatchModule.class);
    return wmm != null
        && !(wmm.getWools().keySet().size() * 2 - 1 + wmm.getWools().values().size() < MAX_ROWS);
  }

  // Determines if all the map objectives can fit onto the scoreboard with empty
  // rows in between.
  private boolean isSuperCompact(Set<Competitor> competitorsWithGoals) {
    int rowsUsed = competitorsWithGoals.size() * 2 - 1;

    if (isCompactWool()) {
      WoolMatchModule wmm = match.needModule(WoolMatchModule.class);
      rowsUsed += wmm.getWools().keySet().size();
    } else {
      GoalMatchModule gmm = match.needModule(GoalMatchModule.class);
      rowsUsed += gmm.getGoals().size();
    }

    return !(rowsUsed < MAX_ROWS);
  }

  private Component constructHeader(final Config config, final MapInfo map) {
    final Component header = config.getMatchHeader();
    if (header != null) {
      return header.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Component gamemode = map.getGamemode();
    if (gamemode != null) {
      return gamemode.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Collection<Gamemode> gamemodes = map.getGamemodes();
    if (!gamemodes.isEmpty()) {
      String suffix = gamemodes.size() <= 1 ? ".name" : ".acronym";
      List<Component> gmComponents =
          gamemodes.stream()
              .map(gm -> translatable("gamemode." + gm.getId() + suffix))
              .collect(Collectors.toList());
      return TextFormatter.list(gmComponents, NamedTextColor.AQUA);
    }

    final List<Component> games = new LinkedList<>();

    // First, find a primary game mode
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || tag.isAuxiliary()) continue;

      if (games.isEmpty()) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
        continue;
      }

      // When there are multiple, primary game modes
      games.set(0, translatable("gamemode.generic.name", NamedTextColor.AQUA));
      break;
    }

    // Second, append auxiliary game modes
    for (final MapTag tag : map.getTags()) {
      if (!tag.isGamemode() || !tag.isAuxiliary()) continue;

      // There can only be 2 game modes
      if (games.size() < 2) {
        games.add(tag.getName().color(NamedTextColor.AQUA));
      } else {
        break;
      }
    }

    return TextFormatter.list(games, NamedTextColor.AQUA);
  }

  private String renderScore(Competitor competitor) {
    ScoreMatchModule smm = competitor.getMatch().needModule(ScoreMatchModule.class);
    String text = ChatColor.WHITE.toString() + (int) smm.getScore(competitor);
    if (smm.hasScoreLimit()) {
      text += ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + smm.getScoreLimit();
    }
    return text;
  }

  private String renderBlitz(Competitor competitor) {
    BlitzMatchModule bmm = competitor.getMatch().needModule(BlitzMatchModule.class);
    if (competitor instanceof tc.oc.pgm.teams.Team) {
      return ChatColor.WHITE.toString() + bmm.getRemainingPlayers(competitor);
    } else if (competitor instanceof Tribute && bmm.getConfig().getNumLives() > 1) {
      return ChatColor.WHITE.toString()
          + bmm.getNumOfLives(competitor.getPlayers().iterator().next().getId());
    } else {
      return "";
    }
  }

  private String renderTeam(Competitor competitor) {
    return LegacyComponentSerializer.legacySection()
        .serialize(TextTranslations.translate(competitor.getName(), Locale.ENGLISH));
  }

  private String renderGoal(Goal<?> goal, @Nullable Competitor competitor) {
    StringBuilder sb = new StringBuilder(" ");

    BlinkTask blinkTask = this.blinkingGoals.get(goal);
    if (blinkTask != null && blinkTask.isDark()) {
      sb.append(ChatColor.BLACK);
    } else {
      sb.append(goal.renderSidebarStatusColor(competitor, this.match.getDefaultParty()));
    }
    sb.append(goal.renderSidebarStatusText(competitor, this.match.getDefaultParty()));

    if (goal instanceof ProximityGoal) {
      // Show teams their own proximity on shared goals
      String proximity =
          ((ProximityGoal) goal).renderProximity(competitor, this.match.getDefaultParty());

      if (!proximity.isEmpty()) sb.append(" ").append(proximity);
    }
    sb.append(" ");
    final TextColor color =
        TextFormatter.convert(
            goal.renderSidebarLabelColor(competitor, this.match.getDefaultParty()));
    sb.append(
        LegacyComponentSerializer.legacySection()
            .serialize(
                TextTranslations.translate(
                    goal.renderSidebarLabelText(competitor, this.match.getDefaultParty())
                        .color(color),
                    Locale.ENGLISH)));

    return sb.toString();
  }

  public List<String> constructSidebar() {
    final boolean hasScores = hasScores();
    final boolean isBlitz = isBlitz();
    final boolean isCompactWool = isCompactWool();
    final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

    Set<Competitor> competitorsWithGoals = new HashSet<>();
    List<Goal<?>> sharedGoals = new ArrayList<>();

    // Count the rows used for goals
    for (Goal<?> goal : gmm.getGoals()) {
      if (goal.hasShowOption(ShowOption.SHOW_SIDEBAR)) {
        if (goal.isShared()) {
          sharedGoals.add(goal);
        } else {
          competitorsWithGoals.addAll(gmm.getCompetitors(goal));
        }
      }
    }
    final boolean isSuperCompact = isSuperCompact(competitorsWithGoals);

    final List<String> rows = new ArrayList<>(MAX_ROWS);
    // Scores/Blitz
    if (hasScores || isBlitz) {
      for (Competitor competitor : match.getSortedCompetitors()) {
        String text;
        if (hasScores) {
          text = renderScore(competitor);
        } else {
          text = renderBlitz(competitor);
        }
        if (text.length() != 0) text += " ";
        rows.add(
            text
                + LegacyComponentSerializer.legacySection()
                    .serialize(TextTranslations.translate(competitor.getName(), Locale.ENGLISH)));

        // No point rendering more scores, usually seen in FFA
        if (rows.size() >= MAX_ROWS) break;
      }

      if (!competitorsWithGoals.isEmpty() || !sharedGoals.isEmpty()) {
        // Blank row between scores and goals
        rows.add("");
      }
    }

    boolean firstTeam = true;

    // Shared goals i.e. not grouped under a specific team
    for (Goal goal : sharedGoals) {
      firstTeam = false;
      rows.add(this.renderGoal(goal, null));
    }

    // Team-specific goals
    List<Competitor> sortedCompetitors = new ArrayList<>(match.getSortedCompetitors());
    sortedCompetitors.retainAll(competitorsWithGoals);
    for (Competitor competitor : sortedCompetitors) {
      // Prevent team name from showing if there isn't space for at least 1 row of its
      // objectives
      if (!(rows.size() + 2 < MAX_ROWS)) break;

      if (!(firstTeam || isSuperCompact)) {
        // Add a blank row between teams
        rows.add("");
      }
      firstTeam = false;

      // Add a row for the team name
      rows.add(this.renderTeam(competitor));

      if (isCompactWool) {
        boolean firstWool = true;

        List<Goal> sortedWools = new ArrayList<>(gmm.getGoals(competitor));
        Collections.sort(sortedWools, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Calculate whether having three spaces between each wool would fit on the
        // scoreboard.
        boolean horizontalCompact =
            MAX_LENGTH < (3 * sortedWools.size()) + (3 * (sortedWools.size() - 1)) + 1;
        String woolText = "";
        if (!horizontalCompact) {
          // If there is extra room, add another space to the left of the wools to make
          // them
          // appear more centered.
          woolText += " ";
        }

        for (Goal<?> goal : sortedWools) {
          if (goal instanceof MonumentWool && goal.hasShowOption(ShowOption.SHOW_SIDEBAR)) {
            MonumentWool wool = (MonumentWool) goal;
            woolText += " ";
            if (!firstWool && !horizontalCompact) woolText += "  ";
            firstWool = false;
            woolText += wool.renderSidebarStatusColor(competitor, this.match.getDefaultParty());
            woolText += wool.renderSidebarStatusText(competitor, this.match.getDefaultParty());
          }
        }
        // Add a row for the compact wools
        rows.add(woolText);

      } else {
        // Not compact; add a row for each of this team's goals
        for (Goal goal : gmm.getGoals()) {
          if (!goal.isShared()
              && goal.canComplete(competitor)
              && goal.hasShowOption(ShowOption.SHOW_SIDEBAR)) {
            rows.add(this.renderGoal(goal, competitor));
          }
        }
      }
    }
    final Component footer = PGM.get().getConfiguration().getMatchFooter();
    if (footer != null) {
      // Only shows footer if there are one or two rows available
      if (rows.size() < MAX_ROWS - 2) {
        rows.add("");
      }
      rows.add(
          LegacyComponentSerializer.legacySection()
              .serialize(TextTranslations.translate(footer, Locale.ENGLISH)));
    }

    Collections.reverse(rows);
    for (int i = rows.size(); i < MAX_ROWS; i++) {
      rows.add(this.baseString + ":" + i);
    }
    return rows;
  }

  public void blinkGoal(Goal goal, float rateHz, @Nullable Duration duration) {
    BlinkTask task = this.blinkingGoals.get(goal);
    if (task != null) {
      task.reset(duration);
    } else {
      this.blinkingGoals.put(goal, new BlinkTask(goal, rateHz, duration));
    }
  }

  public void stopBlinkingGoal(Goal goal) {
    BlinkTask task = this.blinkingGoals.remove(goal);
    if (task != null) task.stop();
  }

  private class BlinkTask implements Runnable {

    private final Future<?> task;
    private final Goal goal;
    private final long intervalTicks;

    private boolean dark;
    private Long ticksRemaining;

    private BlinkTask(Goal goal, float rateHz, @Nullable Duration duration) {
      this.goal = goal;
      this.intervalTicks = (long) (10f / rateHz);
      this.task =
          match
              .getExecutor(MatchScope.RUNNING)
              .scheduleWithFixedDelay(
                  this, 0, intervalTicks * TimeUtils.TICK, TimeUnit.MILLISECONDS);

      this.reset(duration);
    }

    public void reset(@Nullable Duration duration) {
      this.ticksRemaining = duration == null ? null : TimeUtils.toTicks(duration);
    }

    public void stop() {
      this.task.cancel(true);
      SideBarGenerator.this.blinkingGoals.remove(this.goal);
      // renderSidebarDebounce();
      displayUpdatedSidebar(constructSidebar());
    }

    public boolean isDark() {
      return this.dark;
    }

    @Override
    public void run() {
      if (this.ticksRemaining != null) {
        this.ticksRemaining -= this.intervalTicks;
        if (this.ticksRemaining <= 0) {
          this.task.cancel(true);
          SideBarGenerator.this.blinkingGoals.remove(this.goal);
        }
      }

      this.dark = !this.dark;
      // renderSidebarDebounce();
      displayUpdatedSidebar(constructSidebar());
    }
  }
}
