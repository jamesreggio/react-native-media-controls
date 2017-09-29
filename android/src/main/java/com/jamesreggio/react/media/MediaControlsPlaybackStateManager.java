//XXX support additional fields

package com.jamesreggio.react.media;

import android.graphics.Bitmap;
import android.support.v4.media.session.PlaybackStateCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

import java.util.HashMap;
import java.util.Map;

public class MediaControlsPlaybackStateManager {

  private final MediaControlsModule module;
  private final Map<String, Boolean> actionsEnabled = new HashMap<>();

  private PlaybackStateCompat.Builder builder;

  /**
   * Constructors
   */

  MediaControlsPlaybackStateManager(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.module = module;
    this.builder = new PlaybackStateCompat.Builder();
  }

  public void destroy() { }

  /**
   * Public interface
   */

  public void toggleAction(
    final String name,
    final boolean enabled,
    final ReadableMap options
  ) {
    this.actionsEnabled.put(name, enabled);
    this.render();
  }

  public void resetDetails() {
    this.builder = new PlaybackStateCompat.Builder();
    this.render();
  }

  public void updateDetails(final MediaControlsDetails details) {
    int state = PlaybackStateCompat.STATE_NONE;

    if (details.getPlaying() != null) {
      state = details.getPlaying() ?
        PlaybackStateCompat.STATE_PLAYING :
        PlaybackStateCompat.STATE_PAUSED;
    }

    this.builder.setState(state, details.getPosition(), details.getSpeed());
    this.render();
  }

  public void updateArtwork(final Bitmap artwork) {
    // No-op.
  }

  /**
   * Rendering
   */

  private void render() {
    long actions = 0;

    for (Map.Entry<String, Boolean> entry : this.actionsEnabled.entrySet()) {
      final Boolean value = entry.getValue();

      if (value != null && value) {
        actions |= this.toAction(entry.getKey());
      }
    }

    this.builder.setActions(actions);
    this.module.getSession().setPlaybackState(this.builder.build());
  }

  /**
   * Mappers
   */

  private long toAction(final String actionName) {
    switch (actionName) {
      case "play":
        return PlaybackStateCompat.ACTION_PLAY;
      case "pause":
        return PlaybackStateCompat.ACTION_PAUSE;
      case "toggle":
        return PlaybackStateCompat.ACTION_PLAY_PAUSE;
      case "stop":
        return PlaybackStateCompat.ACTION_STOP;
      case "nextTrack":
        return PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
      case "previousTrack":
        return PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
      case "skipForward":
        return PlaybackStateCompat.ACTION_FAST_FORWARD;
      case "skipBackward":
        return PlaybackStateCompat.ACTION_REWIND;
      default:
        return 0;
    }
  }

}
