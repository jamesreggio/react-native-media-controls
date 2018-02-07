//XXX define interface for managers
//XXX define interface for destroyability
//XXX initialize lazily and jettison on low memory

package com.jamesreggio.react.media;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread;
import java.net.URL;
import java.net.URLConnection;

import javax.annotation.Nullable;

public class MediaControlsModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext context;

  private boolean initialized = false;
  private MediaSessionCompat session;
  private MediaControlsReceiver receiver;
  private MediaControlsFocusManager focusManager;
  private MediaControlsNotificationManager notificationManager;
  private MediaControlsPlaybackStateManager playbackStateManager;
  private MediaControlsMediaMetadataManager mediaMetadataManager;

  private String artwork;
  private Thread artworkThread;
  private MediaControlsDetails details;

  /**
   * Constructors
   */

  public MediaControlsModule(final ReactApplicationContext context) {
    super(context);
    this.context = context;

    // Initialization must occur on the main thread.

    final MediaControlsModule module = this;
    final Handler mainHandler = new Handler(context.getMainLooper());

    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        module.init();
      }
    });
  }

  private void init() {
    final MediaControlsModule module = this;
    final ReactApplicationContext context = this.context;

    // Initialize session.

    this.session = new MediaSessionCompat(
      context,
      "MediaControlsModule",
      new ComponentName(context, MediaControlsReceiver.class),
      null
    );

    this.session.setFlags(
      MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
      MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
    );

    this.session.setCallback(new MediaControlsListener(context, module));
    this.session.setPlaybackToLocal(AudioManager.STREAM_MUSIC);

    // Initialize managers.

    this.focusManager = new MediaControlsFocusManager(context, this);
    this.notificationManager = new MediaControlsNotificationManager(context, this);
    this.playbackStateManager = new MediaControlsPlaybackStateManager(context, this);
    this.mediaMetadataManager = new MediaControlsMediaMetadataManager(context, this);

    // Initialize details.

    this.details = new MediaControlsDetails();

    // Initialize receiver.

    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_BUTTON);
    filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    filter.addAction(MediaControlsNotificationManager.ACTION_MEDIA_BUTTON);
    filter.addAction(MediaControlsNotificationManager.ACTION_DISMISS_NOTIFICATION);

    this.receiver = new MediaControlsReceiver(context, this);
    context.registerReceiver(this.receiver, filter);

    // Initialize watchdog service and lifecycle listeners.
    // The watchdog is necessary to ensure the lifecycle listeners are invoked.

    context.startService(new Intent(context, MediaControlsWatchdogService.class));

    context.addLifecycleEventListener(new LifecycleEventListener() {
      @Override
      public void onHostResume() { }

      @Override
      public void onHostPause() { }

      @Override
      public void onHostDestroy() {
        module.destroy();
      }
    });

    this.initialized = true;
  }

  public void destroy() {
    if (!this.initialized) {
      return;
    }

    this.initialized = false;
    this.interruptArtwork();
    this.context.unregisterReceiver(this.receiver);
    this.focusManager.destroy();
    this.mediaMetadataManager.destroy();
    this.playbackStateManager.destroy();
    this.notificationManager.destroy();
    this.session.release();
  }

  /**
   * Public interface
   */

  @Override
  public String getName() {
    return "RNMediaControls";
  }

  @ReactMethod
  public void toggleAction(
    final String name,
    final boolean enabled,
    final ReadableMap options,
    final Promise promise
  ) {
    if (!this.initialized) {
      promise.resolve(false);
      return;
    }

    try {
      this.focusManager.toggleAction(name, enabled, options);
      this.notificationManager.toggleAction(name, enabled, options);
      this.playbackStateManager.toggleAction(name, enabled, options);
      this.mediaMetadataManager.toggleAction(name, enabled, options);
      promise.resolve(true);
    } catch (Exception ex) {
      Log.w("MediaControlsModule", "Error in toggleAction", ex);
      promise.resolve(false);
    }
  }

  @ReactMethod
  public void resetDetails(final Promise promise) {
    if (!this.initialized) {
      promise.resolve(false);
      return;
    }

    try {
      this.interruptArtwork();
      this.artwork = null;

      this.session.setActive(false);
      this.focusManager.resetDetails();
      this.notificationManager.resetDetails();
      this.playbackStateManager.resetDetails();
      this.mediaMetadataManager.resetDetails();
      this.details = new MediaControlsDetails();

      promise.resolve(true);
    } catch (Exception ex) {
      Log.w("MediaControlsModule", "Error in resetDetails", ex);
      promise.resolve(false);
    }
  }

  @ReactMethod
  public void updateDetails(final ReadableMap details, final Promise promise) {
    if (!this.initialized) {
      promise.resolve(false);
      return;
    }

    try {
      this.details.update(details);
      this.focusManager.updateDetails(this.details);
      this.mediaMetadataManager.updateDetails(this.details);
      this.playbackStateManager.updateDetails(this.details);
      this.notificationManager.updateDetails(this.details);
      this.receiver.didUpdateDetails();
      this.session.setActive(true);

      final Activity activity = this.context.getCurrentActivity();
      if (activity != null) {
        activity.setVolumeControlStream(AudioManager.STREAM_MUSIC);
      }

      if (details.hasKey("artwork")) {
        final String artwork = details.getString("artwork");
        if (artwork == null || !artwork.equals(this.artwork)) {
          this.focusManager.updateArtwork(null);
          this.mediaMetadataManager.updateArtwork(null);
          this.playbackStateManager.updateArtwork(null);
          this.notificationManager.updateArtwork(null);
          this.artwork = artwork;
          this.updateArtwork();
        }
      }

      promise.resolve(true);
    } catch (Exception ex) {
      Log.w("MediaControlsModule", "Error in updateDetails", ex);
      promise.resolve(false);
    }
  }

  /**
   * Events
   */

  private interface RCTDeviceEventEmitter extends JavaScriptModule {
    void emit(String eventName, @Nullable Object data);
  }

  protected void emit(final String name) {
    this.emit(name, null);
  }

  protected void emit(final String name, final Object body) {
    this.context
      .getJSModule(MediaControlsModule.RCTDeviceEventEmitter.class)
      .emit(name, body);
  }

  /**
   * Session
   */

  protected MediaSessionCompat getSession() {
    return this.session;
  }

  /**
   * Artwork
   */

  private void interruptArtwork() {
    if (this.artworkThread != null && this.artworkThread.isAlive()) {
      this.artworkThread.interrupt();
      this.artworkThread = null;
    }
  }

  private void updateArtwork() {
    final MediaControlsModule module = this;
    final String artwork = this.artwork;

    this.interruptArtwork();
    this.artworkThread = new Thread(new Runnable() {
      @Override
      public void run() {
        final Bitmap bitmap = this.loadArtwork(artwork);
        module.focusManager.updateArtwork(bitmap);
        module.mediaMetadataManager.updateArtwork(bitmap);
        module.playbackStateManager.updateArtwork(bitmap);
        module.notificationManager.updateArtwork(bitmap);
        module.artworkThread = null;
      }

      private Bitmap loadArtwork(final String artwork) {
        Bitmap bitmap = null;

        try {
          final URLConnection connection = new URL(artwork).openConnection();
          connection.connect();
          final InputStream input = connection.getInputStream();
          bitmap = BitmapFactory.decodeStream(input);
          input.close();
        } catch(IOException ex) {
          Log.w(
            "RNMediaControls",
            String.format("Failed to load artwork: %s", artwork),
            ex
          );
        }

        return bitmap;
      }
    });

    this.artworkThread.start();
  }

}
