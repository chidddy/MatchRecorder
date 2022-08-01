package tc.oc.occ.matchrecorder.Listeners;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.occ.matchrecorder.PacketCreator;
import tc.oc.occ.matchrecorder.Replay;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.goals.Contribution;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

public class MatchListener implements Listener {

  // TODO:
  // ! [x] - start recording
  // ! [x] - add all players
  // ! [x] - create team packets
  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    Replay.startRecording(event.getMatch());
  }

  // TODO:
  // ! [x] - stop recording
  // ! [x] - remove all players
  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    Replay.stopRecording(event.getMatch());
  }

  // TODO:
  // ! [ ] - on wool touch message too
  @EventHandler(priority = EventPriority.MONITOR)
  public void playerWoolPlace(final PlayerWoolPlaceEvent event) {
    if (!event.getWool().hasShowOption(ShowOption.SHOW_MESSAGES)) return;
    String message =
        LegacyComponentSerializer.legacySection()
            .serialize(
                translatable(
                    "wool.complete.owned",
                    event.getPlayer().getName(NameStyle.COLOR),
                    event.getWool().getComponentName(),
                    event.getPlayer().getParty().getName()));
    Replay.addPacket(PacketCreator.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void coreLeak(final CoreLeakEvent event) {
    final Core core = event.getCore();
    if (!core.hasShowOption(ShowOption.SHOW_MESSAGES)) return;

    String message =
        LegacyComponentSerializer.legacySection()
            .serialize(
                translatable(
                    "core.complete.owned",
                    formatContributions(core.getContributions(), false),
                    core.getComponentName(),
                    core.getOwner().getName()));
    Replay.addPacket(PacketCreator.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void destroyableDestroyed(final DestroyableDestroyedEvent event) {
    Destroyable destroyable = event.getDestroyable();
    if (!destroyable.hasShowOption(ShowOption.SHOW_MESSAGES)) return;

    String message =
        LegacyComponentSerializer.legacySection()
            .serialize(
                translatable(
                    "core.complete.owned",
                    formatContributions(event.getDestroyable().getContributions(), true),
                    destroyable.getComponentName(),
                    destroyable.getOwner().getName()));
    Replay.addPacket(PacketCreator.createChatPacket(message));
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
