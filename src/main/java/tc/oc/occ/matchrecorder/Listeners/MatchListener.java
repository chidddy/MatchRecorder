package tc.oc.occ.matchrecorder.Listeners;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.occ.matchrecorder.MatchRecorder;
import tc.oc.occ.matchrecorder.PacketBuilder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.Dropped;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

public class MatchListener implements Listener {
  private final Recorder recorder;

  public MatchListener(Recorder recorder) {
    this.recorder = recorder;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    recorder.startRecording(event.getMatch());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    Bukkit.getScheduler()
        .runTaskLater(
            MatchRecorder.get(),
            () -> {
              recorder.stopRecording(event.getMatch());
            },
            60);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void playerWoolPlace(final PlayerWoolPlaceEvent event) {
    if (!recorder.isRecording()) return;
    if (!event.getWool().hasShowOption(ShowOption.SHOW_MESSAGES)) return;
    Component message =
        TextTranslations.translate(
            translatable(
                "wool.complete.owned",
                event.getPlayer().getName(NameStyle.COLOR),
                event.getWool().getComponentName(),
                event.getPlayer().getParty().getName()),
            Locale.ENGLISH);
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void coreLeak(final CoreLeakEvent event) {
    if (!recorder.isRecording()) return;
    final Core core = event.getCore();
    if (!core.hasShowOption(ShowOption.SHOW_MESSAGES)) return;

    Component message =
        TextTranslations.translate(
            translatable(
                "core.complete.owned",
                formatContributions(core.getContributions(), false),
                core.getComponentName(),
                core.getOwner().getName()),
            Locale.ENGLISH);
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  // TODO:
  // ! [ ] handle scorebox messages (these likely arent possible with the current implementation of
  // scoreboxes)
  @EventHandler(priority = EventPriority.MONITOR)
  public void goalTouch(GoalTouchEvent event) {
    if (!recorder.isRecording()) return;
    if (event.getGoal().isCompleted()) return;
    Component message = null;
    if (event.getGoal() instanceof MonumentWool) {
      message =
          TextTranslations.translate(
              translatable(
                  "destroyable.touch.owned.player",
                  event.getPlayer().getName(NameStyle.COLOR),
                  event.getGoal().getComponentName(),
                  event.getGoal().getOwner().getName()),
              Locale.ENGLISH);
    } else if (event.getGoal() instanceof Flag) {
      message =
          TextTranslations.translate(
              translatable(
                  "flag.touch.player",
                  event.getGoal().getComponentName(),
                  event.getPlayer().getName(NameStyle.COLOR)),
              Locale.ENGLISH);
    } else {
      message =
          TextTranslations.translate(
              translatable(
                  "destroyable.touch.owned.player",
                  event.getPlayer().getName(NameStyle.COLOR),
                  event.getGoal().getComponentName(),
                  event.getGoal().getOwner().getName()),
              Locale.ENGLISH);
    }
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void destroyableDestroyed(final DestroyableDestroyedEvent event) {
    if (!recorder.isRecording()) ;
    Destroyable destroyable = event.getDestroyable();
    if (!destroyable.hasShowOption(ShowOption.SHOW_MESSAGES)) return;

    Component message =
        TextTranslations.translate(
            translatable(
                "destroyable.complete.owned",
                formatContributions(event.getDestroyable().getContributions(), true),
                destroyable.getComponentName(),
                destroyable.getOwner().getName()),
            Locale.ENGLISH);
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onModeSwitch(ObjectiveModeChangeEvent event) {
    if (!recorder.isRecording()) return;
    if (!event.isVisible()) return;
    Component message =
        text()
            .append(text("> > > > ", NamedTextColor.DARK_AQUA))
            .append(text(event.getName(), NamedTextColor.DARK_RED))
            .append(text(" < < < <", NamedTextColor.DARK_AQUA))
            .build();
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onControlPointCapture(ControllerChangeEvent event) {
    if (!recorder.isRecording()) return;
    if (event.getNewController() != null) {
      recorder.addPacket(
          PacketBuilder.createChatPacket(
              text()
                  .append(event.getNewController().getName())
                  .append(
                      text(" captured ", NamedTextColor.GRAY)
                          .append(
                              text(
                                  event.getControlPoint().getName(),
                                  TextFormatter.convert(event.getNewController().getColor()))))
                  .build()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagStateChange(FlagStateChangeEvent event) {
    if (!recorder.isRecording()) return;
    Component message = null;
    if (event.getNewState() instanceof Dropped) {
      message =
          TextTranslations.translate(
              translatable("flag.drop", event.getFlag().getComponentName()), Locale.ENGLISH);
    } else if (event.getNewState() instanceof Returned) {
      message =
          TextTranslations.translate(
              translatable("flag.respawn", event.getFlag().getComponentName()), Locale.ENGLISH);
    } else {
      return;
    }
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onFlagCapture(FlagCaptureEvent event) {
    if (!recorder.isRecording()) return;
    Component message =
        TextTranslations.translate(
            translatable(
                "flag.capture.player",
                event.getGoal().getComponentName(),
                event.getCarrier().getName(NameStyle.COLOR)),
            Locale.ENGLISH);
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  // TODO:
  // ! move function in PGM into textformatter and make public
  private Component formatContributions(
      Collection<? extends Contribution> contributions, boolean showPercentage) {
    List<? extends Contribution> sorted = new ArrayList<>(contributions);
    sorted.sort(
        (o1, o2) -> {
          return Double.compare(o2.getPercentage(), o1.getPercentage()); // reverse
        });

    List<Component> contributors = new ArrayList<>();
    boolean someExcluded = false;
    for (Contribution entry : sorted) {
      if (entry.getPercentage() > 0.2) { // 20% necessary to be included
        if (showPercentage) {
          contributors.add(
              translatable(
                  "objective.credit.percentage",
                  entry.getPlayerState().getName(NameStyle.COLOR),
                  text(Math.round(entry.getPercentage() * 100), NamedTextColor.AQUA)));
        } else {
          contributors.add(entry.getPlayerState().getName(NameStyle.COLOR));
        }
      } else {
        someExcluded = true;
      }
    }

    final Component credit;
    if (contributors.isEmpty()) {
      credit = translatable(someExcluded ? "objective.credit.many" : "objective.credit.unknown");
    } else {
      if (someExcluded) {
        contributors.add(translatable("objective.credit.etc")); // Some contributors < 20%
      }
      credit = TextFormatter.list(contributors, NamedTextColor.WHITE);
    }

    return credit;
  }
}
