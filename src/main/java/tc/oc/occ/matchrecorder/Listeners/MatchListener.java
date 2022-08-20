package tc.oc.occ.matchrecorder.Listeners;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import tc.oc.occ.matchrecorder.MatchRecorder;
import tc.oc.occ.matchrecorder.PacketBuilder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchScope;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.ParticipantState;
import tc.oc.pgm.controlpoint.events.ControllerChangeEvent;
import tc.oc.pgm.core.Core;
import tc.oc.pgm.core.CoreLeakEvent;
import tc.oc.pgm.countdowns.Countdown;
import tc.oc.pgm.countdowns.MatchCountdown;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.destroyable.DestroyableDestroyedEvent;
import tc.oc.pgm.events.CountdownCancelEvent;
import tc.oc.pgm.events.CountdownEndEvent;
import tc.oc.pgm.events.CountdownStartEvent;
import tc.oc.pgm.flag.Flag;
import tc.oc.pgm.flag.event.FlagCaptureEvent;
import tc.oc.pgm.flag.event.FlagStateChangeEvent;
import tc.oc.pgm.flag.state.BaseState;
import tc.oc.pgm.flag.state.Carried;
import tc.oc.pgm.flag.state.Dropped;
import tc.oc.pgm.flag.state.Respawning;
import tc.oc.pgm.flag.state.Returned;
import tc.oc.pgm.goals.ShowOption;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.listeners.FormattingListener;
import tc.oc.pgm.modes.ObjectiveModeChangeEvent;
import tc.oc.pgm.score.ScoreBox;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.util.TimeUtils;
import tc.oc.pgm.util.event.PlayerCoarseMoveEvent;
import tc.oc.pgm.util.event.PlayerItemTransferEvent;
import tc.oc.pgm.util.material.matcher.SingleMaterialMatcher;
import tc.oc.pgm.util.named.NameStyle;
import tc.oc.pgm.util.text.TemporalComponent;
import tc.oc.pgm.util.text.TextFormatter;
import tc.oc.pgm.util.text.TextTranslations;
import tc.oc.pgm.wool.MonumentWool;
import tc.oc.pgm.wool.PlayerWoolPlaceEvent;

@SuppressWarnings("deprecation")
public class MatchListener implements Listener {
  private final Recorder recorder;
  private final HashMap<Countdown, Integer> countdowns = new HashMap<Countdown, Integer>();

  public MatchListener(Recorder recorder) {
    this.recorder = recorder;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchStart(MatchStartEvent event) {
    recorder.startRecording(event.getMatch());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchEnd(MatchFinishEvent event) {
    Component message = null;
    if (event.getWinner() == null) {
      message = TextTranslations.translate(translatable("broadcast.gameOver"), Locale.ENGLISH);
    } else {
      message =
          translatable(
              event.getWinner().isNamePlural()
                  ? "broadcast.gameOver.teamWinners"
                  : "broadcast.gameOver.teamWinner",
              event.getWinner().getName());
    }
    recorder.addPacket(PacketBuilder.createChatPacket(message));
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
                FormattingListener.formatContributions(core.getContributions(), false),
                core.getComponentName(),
                core.getOwner().getName()),
            Locale.ENGLISH);
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

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
                FormattingListener.formatContributions(
                    event.getDestroyable().getContributions(), true),
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
    BaseState state = (BaseState) event.getNewState();
    recorder.stopBlinkingGoal(event.getFlag());
    if (state instanceof Dropped) {
      message =
          TextTranslations.translate(
              translatable("flag.drop", event.getFlag().getComponentName()), Locale.ENGLISH);
      if (TimeUtils.isInfinite(state.getDuration())) {
        this.recorder.blinkGoal(event.getFlag(), 2, null);
      }
    } else if (state instanceof Returned) {
      message =
          TextTranslations.translate(
              event.getOldState() instanceof Dropped
                  ? translatable("flag.respawn", event.getFlag().getComponentName())
                  : translatable("flag.return", event.getFlag().getComponentName()),
              Locale.ENGLISH);
    } else if (state instanceof Respawning) {
      TranslatableComponent.Builder time =
          TemporalComponent.duration(state.getDuration(), NamedTextColor.AQUA);
      String postName = state.getPost().getPostName();
      message =
          TextTranslations.translate(
              postName != null
                  ? translatable(
                      "flag.willRespawn.named",
                      event.getFlag().getComponentName(),
                      text(postName, NamedTextColor.AQUA),
                      time)
                  : translatable("flag.willRespawn", event.getFlag().getComponentName(), time),
              Locale.ENGLISH);
    } else if (state instanceof Carried) {
      recorder.blinkGoal(event.getGoal(), 2, null);
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

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void playerEnterBox(PlayerCoarseMoveEvent event) {
    if (!recorder.isRecording()) return;
    Match match = PGM.get().getMatchManager().getMatch(event.getPlayer().getWorld());
    ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
    if (smm == null) return;
    MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    ParticipantState playerState = player.getParticipantState();
    Vector from = event.getBlockFrom().toVector();
    Vector to = event.getBlockTo().toVector();

    for (ScoreBox box : smm.getScoreboxes()) {
      if (box.getRegion().enters(from, to) && box.canScore(playerState)) {
        if (!box.isCoolingDown(playerState)) {
          this.playerScore(box, player, box.getScore() + redeemItems(box, player.getInventory()));
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  public void playerAcquireRedeemableInBox(PlayerItemTransferEvent event) {
    if (!recorder.isRecording()) return;
    Match match = PGM.get().getMatchManager().getMatch(event.getPlayer().getWorld());
    ScoreMatchModule smm = match.getModule(ScoreMatchModule.class);
    if (smm == null) return;
    if (!event.isAcquiring()) return;
    final MatchPlayer player = match.getPlayer(event.getPlayer());
    if (player == null || !player.canInteract() || player.getBukkit().isDead()) return;

    for (final ScoreBox box : smm.getScoreboxes()) {
      if (!box.getRedeemables().isEmpty()
          && box.getRegion().contains(player.getBukkit())
          && box.canScore(player.getParticipantState())) {
        match
            .getExecutor(MatchScope.RUNNING)
            .execute(
                () -> {
                  if (player.getBukkit().isOnline()) {
                    double points = redeemItems(box, player.getInventory());
                    this.playerScore(box, player, points);
                  }
                });
      }
    }
  }

  private double redeemItems(ScoreBox box, ItemStack stack) {
    if (stack == null) return 0;
    double points = 0;
    for (Entry<SingleMaterialMatcher, Double> entry : box.getRedeemables().entrySet()) {
      if (entry.getKey().matches(stack.getData())) {
        points += entry.getValue() * stack.getAmount();
        // stack.setAmount(0);
      }
    }
    return points;
  }

  private double redeemItems(ScoreBox box, ItemStack[] stacks) {
    double total = 0;
    for (int i = 0; i < stacks.length; i++) {
      double points = redeemItems(box, stacks[i]);
      if (points != 0) stacks[i] = null;
      total += points;
    }
    return total;
  }

  private double redeemItems(ScoreBox box, PlayerInventory inventory) {
    ItemStack[] notArmor = inventory.getContents();
    ItemStack[] armor = inventory.getArmorContents();

    double points = redeemItems(box, notArmor) + redeemItems(box, armor);

    return points;
  }

  private void playerScore(ScoreBox box, MatchPlayer player, double points) {
    if (!player.isParticipating()) return;

    if (points == 0) return;

    int wholePoints = (int) points;
    if (wholePoints < 1 || box.isSilent()) return;

    recorder.addPacket(
        PacketBuilder.createChatPacket(
            TextTranslations.translate(
                translatable(
                    "scorebox.scored",
                    player.getName(NameStyle.COLOR),
                    translatable(
                        wholePoints == 1 ? "misc.point" : "misc.points",
                        text(wholePoints, NamedTextColor.DARK_AQUA)),
                    player.getParty().getName()),
                Locale.ENGLISH)));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountDownStart(CountdownStartEvent event) {
    if (!recorder.isRecording()) return;
    if (!(event.getCountdown() instanceof MatchCountdown)) return;
    MatchCountdown countdown = (MatchCountdown) event.getCountdown();
    int task =
        Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(
                MatchRecorder.get(),
                () -> {
                  if (countdown.showChat() && recorder.isRecording()) {
                    recorder.addPacket(
                        PacketBuilder.createChatPacket(
                            TextTranslations.translate(countdown.formatText(), Locale.ENGLISH)));
                  }
                },
                0,
                20);
    this.countdowns.put(event.getCountdown(), task);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountDownEnd(CountdownEndEvent event) {
    if (!recorder.isRecording()) return;
    if (!(event.getCountdown() instanceof MatchCountdown)) return;
    if (!this.countdowns.containsKey(event.getCountdown())) return;
    Bukkit.getScheduler().cancelTask(this.countdowns.get(event.getCountdown()));
    this.countdowns.remove(event.getCountdown());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onCountDownCancel(CountdownCancelEvent event) {
    if (!recorder.isRecording()) return;
    if (!(event.getCountdown() instanceof MatchCountdown)) return;
    if (!this.countdowns.containsKey(event.getCountdown())) return;
    Bukkit.getScheduler().cancelTask(this.countdowns.get(event.getCountdown()));
    this.countdowns.remove(event.getCountdown());
  }
}
