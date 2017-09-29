package com.jamesreggio.react.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

public class MediaControlsFocusManager implements AudioManager.OnAudioFocusChangeListener {

  private final ReactApplicationContext context;
  private final MediaControlsModule module;
  private final AudioManager manager;

  private boolean focused = false;
  private boolean shouldResume = false;

  /**
   * Constructors
   */

  MediaControlsFocusManager(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.context = context;
    this.module = module;
    this.manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
  }

  public void destroy() {
    if (this.focused) {
      this.manager.abandonAudioFocus(this);
    }
  }

  /**
   * Public interface
   */

  public void toggleAction(
    final String name,
    final boolean enabled,
    final ReadableMap options
  ) {
    // No-op.
  }

  public void resetDetails() {
    if (!this.focused) {
      return;
    }

    int result = this.manager.abandonAudioFocus(this);

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      this.focused = false;
    } else {
      Log.i(
        "MediaControlsModule",
        String.format("Unable to abandon audio focus: %d", result)
      );
    }
  }

  public void updateDetails(final MediaControlsDetails details) {
    if (this.focused) {
      return;
    }

    int result = this.manager.requestAudioFocus(
      this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
    );

    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      this.focused = true;
    } else {
      Log.i(
        "MediaControlsModule",
        String.format("Unable to acquire audio focus: %d", result)
      );
    }
  }

  public void updateArtwork(final Bitmap artwork) {
    // No-op.
  }

  /**
   * Events
   */

  @Override
  public void onAudioFocusChange(int focusChange) {
    WritableMap body = Arguments.createMap();

    switch (focusChange) {
      case AudioManager.AUDIOFOCUS_GAIN: {
        body.putString("type", "ENDED");
        body.putBoolean("shouldResume", this.shouldResume);
        this.module.emit("interrupt", body);
        break;
      }

      case AudioManager.AUDIOFOCUS_LOSS: {
        this.shouldResume = false;
        body.putString("type", "BEGAN");
        body.putBoolean("wasSuspended", false);
        this.module.emit("interrupt", body);
        break;
      }

      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
      case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
        this.shouldResume = true;
        body.putString("type", "BEGAN");
        body.putBoolean("wasSuspended", false);
        this.module.emit("interrupt", body);
        break;
      }
    }
  }

}
