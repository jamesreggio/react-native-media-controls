//XXX test onSeekTo
//XXX supply interval with skipForward/Backward
//XXX support additional callbacks

package com.jamesreggio.react.media;

import android.support.v4.media.session.MediaSessionCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;

public class MediaControlsListener extends MediaSessionCompat.Callback {

  private final MediaControlsModule module;

  MediaControlsListener(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.module = module;
  }

  @Override
  public void onPlay() {
    this.module.emit("play");
  }

  @Override
  public void onPause() {
    this.module.emit("pause");
  }

  @Override
  public void onStop() {
    this.module.emit("stop");
  }

  @Override
  public void onSkipToNext() {
    this.module.emit("nextTrack");
  }

  @Override
  public void onSkipToPrevious() {
    this.module.emit("previousTrack");
  }

  @Override
  public void onSeekTo(final long position) {
    WritableMap body = Arguments.createMap();
    body.putDouble("position", position / 1000D);
    this.module.emit("seek", body);
  }

  @Override
  public void onFastForward() {
    this.module.emit("skipForward");
  }

  @Override
  public void onRewind() {
    this.module.emit("skipBackward");
  }

}
