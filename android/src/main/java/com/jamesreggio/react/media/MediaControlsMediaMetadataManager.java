//XXX support additionals fields

package com.jamesreggio.react.media;

import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

public class MediaControlsMediaMetadataManager {

  private final MediaControlsModule module;

  private MediaMetadataCompat.Builder builder;

  /**
   * Constructors
   */

  MediaControlsMediaMetadataManager(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.module = module;
    this.builder = new MediaMetadataCompat.Builder();
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
    // No-op.
  }

  public void resetDetails() {
    this.builder = new MediaMetadataCompat.Builder();
    this.render();
  }

  public void updateDetails(final MediaControlsDetails details) {
    final MediaMetadataCompat.Builder builder = this.builder;
    builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, details.getAlbum());
    builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, details.getArtist());
    builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, details.getTitle());
    builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, details.getDuration());
    this.render();
  }

  public void updateArtwork(final Bitmap artwork) {
    this.builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, artwork);
    this.render();
  }

  /**
   * Rendering
   */

  private void render() {
    this.module.getSession().setMetadata(this.builder.build());
  }

}
