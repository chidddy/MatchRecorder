package tc.oc.occ.matchrecorder;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.WoolMatchModule;

@SuppressWarnings("unused")
public class SideBarGenerator {
  private final Replay recorder;
  private Match match = null;
  private String baseString = null;
  private @Nullable Future<?> renderTask;

  private static final int MAX_ROWS = 16; // Max rows on the scoreboard
  private static final int MAX_LENGTH = 30; // Max characters per line allowed
  private static final int MAX_TEAM = 16; // Max characters per team name
  private static final int MAX_TITLE = 32; // Max characters allowed in title
  private final List<String> rows = new ArrayList<String>(MAX_ROWS);
  private final Map<Goal<?>, BlinkTask> blinkingGoals = new HashMap<>();

  public SideBarGenerator(Replay recorder, Match match) {
    this.recorder = recorder;
    this.baseString = "sb-" + UUID.randomUUID().toString().substring(0, 8);
    this.match = match;
  }

  public void createSidebar() {
    recorder.addPacket(
        PacketBuilder.createScoreboardObjectivePacket_Create(
            this.baseString,
            LegacyComponentSerializer.legacySection()
                .serialize(
                    TextTranslations.translate(
                        this.constructHeader(PGM.get().getConfiguration(), this.match.getMap())))));
    recorder.addPacket(PacketBuilder.createScoreboardDisplayObjectivePacket(this.baseString));
    IntStream.range(0, MAX_ROWS)
        .forEach(
            idx -> {
              rows.add(this.baseString + ":" + idx);
            });
  }

  public void renderSidebarDebounce() {
    if (this.renderTask == null || renderTask.isDone()) {
      this.renderTask =
          match
              .getExecutor(MatchScope.LOADED)
              .submit(
                  () -> {
                    this.renderTask = null;
                    this.displayUpdatedSidebar(this.constructSidebar());
                  });
    }
  }

  public void displayUpdatedSidebar(List<Component> new_rows) {
    for (int i = 0; i < MAX_ROWS; i++) {
      String cached_row = this.rows.get(i);
      String row = LegacyComponentSerializer.legacySection().serialize(new_rows.get(i));
      if (cached_row.equalsIgnoreCase(row)) continue;
      this.rows.set(i, row);
      if (row.equalsIgnoreCase(this.baseString + ":" + i)) {
        removeTeamPacket(i);
      } else {
        createTeamPacket(row, i, cached_row.equalsIgnoreCase(this.baseString + ":" + i));
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
      prefix = PacketBuilder.COLOR_CODES[idx] + "§r";
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

  private boolean isCompactWool() {
    WoolMatchModule wmm = match.getModule(WoolMatchModule.class);
    return wmm != null
        && !(wmm.getWools().keySet().size() * 2 - 1 + wmm.getWools().values().size() < MAX_ROWS);
  }

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
    final Component header = PGM.get().getConfiguration().getMatchHeader();
    if (header != null) {
      return header.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Component gamemode = map.getGamemode();
    if (gamemode != null) {
      return gamemode.colorIfAbsent(NamedTextColor.AQUA);
    }

    final Collection<Gamemode> gamemodes = map.getGamemodes();
    if (!gamemodes.isEmpty()) {
      boolean acronyms = gamemodes.size() > 1;
      List<Component> gmComponents =
          gamemodes.stream()
              .map(gm -> text(acronyms ? gm.getAcronym() : gm.getFullName()))
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

    // Display "Blitz: Rage" rather than "Blitz and Rage"
    if (games.size() == 2
        && Stream.of("blitz", "rage")
            .allMatch(id -> map.getTags().stream().anyMatch(mt -> mt.getId().equals(id)))) {
      games.clear();
      games.add(text(Gamemode.BLITZ_RAGE.getFullName(), NamedTextColor.AQUA));
    }

    return TextFormatter.list(games, NamedTextColor.AQUA);
  }

  private Component renderScore(Competitor competitor) {
    ScoreMatchModule smm = match.needModule(ScoreMatchModule.class);
    if (!smm.getScoreboardFilter().response(competitor)) {
      return null;
    }
    Component score = text((int) smm.getScore(competitor), NamedTextColor.WHITE);
    if (!smm.hasScoreLimit()) {
      return score;
    }
    return text()
        .append(score)
        .append(text("/", NamedTextColor.DARK_GRAY))
        .append(text(smm.getScoreLimit(), NamedTextColor.GRAY))
        .build();
  }

  private Component renderBlitz(Competitor competitor) {
    BlitzMatchModule bmm = this.match.getMatch().needModule(BlitzMatchModule.class);
    if (!bmm.getConfig().getScoreboardFilter().response(competitor)) {
      return null;
    } else if (competitor instanceof tc.oc.pgm.teams.Team) {
      return text(bmm.getRemainingPlayers(competitor), NamedTextColor.WHITE);
    } else if (competitor instanceof Tribute && bmm.getConfig().getNumLives() > 1) {
      final UUID id = competitor.getPlayers().iterator().next().getId();
      return text(bmm.getNumOfLives(id), NamedTextColor.WHITE);
    } else {
      return empty();
    }
  }

  private String renderTeam(Competitor competitor) {
    return LegacyComponentSerializer.legacySection()
        .serialize(TextTranslations.translate(competitor.getName()));
  }

  private Component renderGoal(Goal<?> goal, @Nullable Competitor competitor) {
    final BlinkTask blinkTask = this.blinkingGoals.get(goal);
    final TextComponent.Builder line = text();

    line.append(space());
    line.append(
        goal.renderSidebarStatusText(competitor, this.match.getDefaultParty())
            .color(
                blinkTask != null && blinkTask.isDark()
                    ? NamedTextColor.BLACK
                    : goal.renderSidebarStatusColor(competitor, this.match.getDefaultParty())));

    if (goal instanceof ProximityGoal) {
      final ProximityGoal<?> proximity = (ProximityGoal<?>) goal;
      if (proximity.shouldShowProximity(competitor, this.match.getDefaultParty())) {
        line.append(space());
        line.append(proximity.renderProximity(competitor, this.match.getDefaultParty()));
      }
    }

    line.append(space());
    line.append(
        goal.renderSidebarLabelText(competitor, this.match.getDefaultParty())
            .color(goal.renderSidebarLabelColor(competitor, this.match.getDefaultParty())));

    return line.build();
  }

  public List<Component> constructSidebar() {
    final boolean hasScores = match.getModule(ScoreMatchModule.class) != null;
    final boolean isBlitz = match.getModule(BlitzMatchModule.class) != null;
    final boolean isCompactWool = isCompactWool();
    final GoalMatchModule gmm = match.needModule(GoalMatchModule.class);

    Set<Competitor> competitorsWithGoals = new HashSet<>();
    List<Goal<?>> sharedGoals = new ArrayList<>();

    for (Goal<?> goal : gmm.getGoals()) {
      if (goal.hasShowOption(ShowOption.SHOW_SIDEBAR)
          && goal.getScoreboardFilter().response(match)) {
        if (goal.isShared()) {
          sharedGoals.add(goal);
        } else {
          competitorsWithGoals.addAll(gmm.getCompetitors(goal));
        }
      }
    }
    final boolean isSuperCompact = isSuperCompact(competitorsWithGoals);

    final List<Component> rows = new ArrayList<>(MAX_ROWS);
    if (hasScores || isBlitz) {
      for (Competitor competitor : match.getSortedCompetitors()) {
        Component text;
        if (hasScores) {
          text = renderScore(competitor);
        } else {
          text = renderBlitz(competitor);
        }
        if (text != null) {
          if (text != empty()) {
            text = text.append(space());
          }
          rows.add(text.append(competitor.getName(NameStyle.SIMPLE_COLOR)));

          if (rows.size() >= MAX_ROWS) break;
        }
      }

      if (!competitorsWithGoals.isEmpty() || !sharedGoals.isEmpty()) {
        rows.add(empty());
      }
    }

    boolean firstTeam = true;

    for (Goal<?> goal : sharedGoals) {
      firstTeam = false;
      rows.add(this.renderGoal(goal, null));
    }

    List<Competitor> sortedCompetitors = new ArrayList<>(match.getSortedCompetitors());
    sortedCompetitors.retainAll(competitorsWithGoals);
    for (Competitor competitor : sortedCompetitors) {
      if ((rows.size() + 2) >= MAX_ROWS) break;

      if (!(firstTeam || isSuperCompact)) {
        rows.add(empty());
      }
      firstTeam = false;

      rows.add(competitor.getName());

      if (isCompactWool) {
        boolean firstWool = true;

        List<Goal> sortedWools = new ArrayList<>(gmm.getGoals(competitor));
        sortedWools.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        boolean horizontalCompact =
            MAX_LENGTH < (3 * sortedWools.size()) + (3 * (sortedWools.size() - 1)) + 1;
        TextComponent.Builder woolText = text();
        for (Goal<?> goal : sortedWools) {
          if (goal instanceof MonumentWool && goal.hasShowOption(ShowOption.SHOW_SIDEBAR)) {
            MonumentWool wool = (MonumentWool) goal;
            TextComponent spacer = space();
            if (!firstWool && !horizontalCompact) {
              spacer = spacer.append(space()).append(space());
            }
            firstWool = false;
            woolText.append(
                spacer
                    .append(wool.renderSidebarStatusText(competitor, this.match.getDefaultParty()))
                    .color(
                        wool.renderSidebarStatusColor(competitor, this.match.getDefaultParty())));
          }
        }
        rows.add(woolText.build());

      } else {
        for (Goal<?> goal : gmm.getGoals()) {
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
      if (rows.size() < MAX_ROWS - 2) {
        rows.add(empty());
      }
      rows.add(footer);
    }

    return rows;
  }

  public void blinkGoal(Goal<?> goal, float rateHz, @Nullable Duration duration) {
    BlinkTask task = this.blinkingGoals.get(goal);
    if (task != null) {
      task.reset(duration);
    } else {
      this.blinkingGoals.put(goal, new BlinkTask(goal, rateHz, duration));
    }
  }

  public void stopBlinkingGoal(Goal<?> goal) {
    BlinkTask task = this.blinkingGoals.remove(goal);
    if (task != null) task.stop();
  }

  private class BlinkTask implements Runnable {

    private final Future<?> task;
    private final Goal<?> goal;
    private final long intervalTicks;

    private boolean dark;
    private Long ticksRemaining;

    private BlinkTask(Goal<?> goal, float rateHz, @Nullable Duration duration) {
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
      renderSidebarDebounce();
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
      renderSidebarDebounce();
    }
  }
}
