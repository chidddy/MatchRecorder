package tc.oc.occ.matchrecorder.Listeners;

import java.time.Duration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.match.event.MatchVictoryChangeEvent;
import tc.oc.pgm.api.party.event.CompetitorScoreChangeEvent;
import tc.oc.pgm.api.party.event.PartyRenameEvent;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.destroyable.Destroyable;
import tc.oc.pgm.events.FeatureChangeEvent;
import tc.oc.pgm.events.PlayerJoinMatchEvent;
import tc.oc.pgm.events.PlayerLeaveMatchEvent;
import tc.oc.pgm.events.PlayerPartyChangeEvent;
import tc.oc.pgm.goals.Goal;
import tc.oc.pgm.goals.events.GoalCompleteEvent;
import tc.oc.pgm.goals.events.GoalProximityChangeEvent;
import tc.oc.pgm.goals.events.GoalStatusChangeEvent;
import tc.oc.pgm.goals.events.GoalTouchEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.teams.events.TeamRespawnsChangeEvent;

public class SidebarListener implements Listener {
  private final Recorder recorder;

  public SidebarListener(Recorder recorder) {
    this.recorder = recorder;
  }

  @EventHandler
  public void addPlayer(PlayerJoinMatchEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler
  public void removePlayer(PlayerLeaveMatchEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPartyChange(PlayerPartyChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDeath(MatchPlayerDeathEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSpawn(ParticipantSpawnEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPartyRename(final PartyRenameEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void scoreChange(final CompetitorScoreChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalTouch(final GoalTouchEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalStatusChange(final GoalStatusChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    if (event.getGoal() instanceof Destroyable
        && ((Destroyable) event.getGoal()).getShowProgress()) {
      this.recorder.blinkGoal(event.getGoal(), 3, Duration.ofSeconds(1));
    } else {
      this.recorder.updateSidebar();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalProximityChange(final GoalProximityChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    if (PGM.get().getConfiguration().showProximity()) {
      this.recorder.updateSidebar();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalComplete(final GoalCompleteEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void goalChange(final FeatureChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    if (event.getFeature() instanceof Goal) {
      this.recorder.updateSidebar();
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void updateRespawnLimit(final TeamRespawnsChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void resultChange(MatchVictoryChangeEvent event) {
    if (!this.recorder.isRecording()) return;
    this.recorder.updateSidebar();
  }
}
