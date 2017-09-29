//XXX include default resources
//XXX support configuration of actions in small view
//XXX support configuration of notifications dismissability
//XXX show intent should send additional info (and activate playback screen)

package com.jamesreggio.react.media;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;

import java.util.HashMap;
import java.util.Map;

public class MediaControlsNotificationManager {

  // Resources
  public static final String RESOURCE_SMALL_ICON = "media_controls_small_icon";
  public static final String RESOURCE_NOTIFICATION_COLOR = "media_controls_notification_color";
  public static final String RESOURCE_PLAY = "media_controls_play";
  public static final String RESOURCE_PAUSE = "media_controls_pause";
  public static final String RESOURCE_TOGGLE = "media_controls_toggle";
  public static final String RESOURCE_STOP = "media_controls_stop";
  public static final String RESOURCE_NEXT_TRACK = "media_controls_next_track";
  public static final String RESOURCE_PREVIOUS_TRACK = "media_controls_previous_track";
  public static final String RESOURCE_SKIP_FORWARD = "media_controls_skip_forward";
  public static final String RESOURCE_SKIP_BACKWARD = "media_controls_skip_backward";

  // Intents
  protected static final String ACTION_MEDIA_BUTTON = "MEDIA_CONTROLS_MEDIA_BUTTON";
  protected static final String ACTION_DISMISS_NOTIFICATION = "MEDIA_CONTROLS_REMOVE_NOTIFICATION";

  // Intent extras
  protected static final String PACKAGE_NAME = "MEDIA_CONTROLS_PACKAGE_NAME";

  // Notification tags
  private static final String NOTIFICATION_KEY = "MediaControlsNotification";

  private final Map<String, NotificationCompat.Action> actions = new HashMap<>();
  private final Map<String, Boolean> actionsEnabled = new HashMap<>();

  private final ReactApplicationContext context;
  private final NotificationCompat.Builder builder;

  private boolean visible = false;
  private boolean playing = false;

  /**
   * Constructors
   */

  MediaControlsNotificationManager(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.context = context;

    // Initialize builder.

    final MediaSessionCompat session = module.getSession();
    final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
    this.builder = builder;

    builder
      .setStyle(
        new NotificationCompat
          .MediaStyle()
          .setMediaSession(session.getSessionToken())
          .setShowActionsInCompactView(0, 1, 2) //XXX
      );

    // Initialize notification color.

    final String packageName = context.getPackageName();
    final Resources resources = context.getResources();

    final int colorId = resources.getIdentifier(
      RESOURCE_NOTIFICATION_COLOR, "string", packageName
    );

    if (colorId != 0) {
      builder.setColor(Color.parseColor(resources.getString(colorId)));
    }

    // Initialize small icon.

    final int iconId = resources.getIdentifier(
      RESOURCE_SMALL_ICON, "drawable", packageName
    );

    if (iconId == 0) {
      Log.w(
        "MediaControlsModule",
        "Missing small icon, which is required to show notification."
      );
    }

    builder.setSmallIcon(iconId);

    // Initialize intent for notification tap.

    final Intent launch = context
      .getPackageManager()
      .getLaunchIntentForPackage(packageName);

    builder.setContentIntent(PendingIntent.getActivity(context, 0, launch, 0));

    // Initialize intent for notification dismissal.

    final Intent dismiss = new Intent(ACTION_DISMISS_NOTIFICATION);
    dismiss.putExtra(PACKAGE_NAME, packageName);

    builder.setDeleteIntent(
      PendingIntent.getBroadcast(
        context,
        0,
        dismiss,
        PendingIntent.FLAG_UPDATE_CURRENT
      )
    );
  }

  public void destroy() {
    this.hide();
  }

  /**
   * Public interface
   */

  public void toggleAction(
    final String name,
    final boolean enabled,
    final ReadableMap options
  ) {
    this.actionsEnabled.put(name, enabled);
    this.buildAction(name, options);
    this.render();
  }

  public void resetDetails() {
    this.visible = false;

    this.builder
      .setContentTitle(null)
      .setContentText(null)
      .setContentInfo(null)
      .setLargeIcon(null);

    this.render();
  }

  public void updateDetails(final MediaControlsDetails details) {
    if (details.getPlaying() == null) {
      return;
    }

    this.visible = true;
    this.playing = details.getPlaying();

    this.builder
      .setContentTitle(details.getTitle())
      .setContentText(details.getArtist())
      .setContentInfo(details.getAlbum());

    this.render();
  }

  public void updateArtwork(final Bitmap artwork) {
    this.builder.setLargeIcon(artwork);
    this.render();
  }

  /**
   * Rendering
   */

  private void render() {
    if (!this.visible) {
      this.hide();
      return;
    }

    final NotificationCompat.Builder builder = this.builder;

    // Build actions (ordering matters).

    builder.mActions.clear();

    this.addAction(builder, "previousTrack");
    this.addAction(builder, "skipBackward");

    if (this.playing) {
      this.addAction(builder, "pause");
    } else {
      this.addAction(builder, "play");
    }

    this.addAction(builder, "skipForward");
    this.addAction(builder, "nextTrack");

    // Configure whether notification can be dismissed.

    builder.setOngoing(this.playing);

    this.show();
  }

  private void show() {
    NotificationManagerCompat
      .from(this.context)
      .notify(NOTIFICATION_KEY, 0, this.builder.build());
  }

  private void hide() {
    NotificationManagerCompat
      .from(this.context)
      .cancel(NOTIFICATION_KEY, 0);
  }

  /**
   * Actions
   */

  private void addAction(final NotificationCompat.Builder builder, final String actionName) {
    final Boolean enabled = this.actionsEnabled.get(actionName);
    final NotificationCompat.Action action = this.actions.get(actionName);

    if (enabled != null && action != null && enabled) {
      builder.addAction(action);
    }
  }

  private void buildAction(final String actionName, final ReadableMap options) {
    if (this.actions.containsKey(actionName) && options == null) {
      return;
    }

    final String packageName = this.context.getPackageName();
    final Resources resources = this.context.getResources();
    final String resourceName = this.toResourceName(actionName);

    if (resourceName == null) {
      return;
    }

    // Load icon.

    final int iconId = resources.getIdentifier(
      resourceName, "drawable", packageName
    );

    if (iconId == 0) {
      Log.w(
        "MediaControlsModule",
        String.format("Missing icon for action: %s", actionName)
      );

      return;
    }

    // Load label.

    final int labelId = resources.getIdentifier(
      resourceName, "string", packageName
    );

    if (labelId == 0) {
      Log.w(
        "MediaControlsModule",
        String.format("Missing label for action: %s", actionName)
      );

      return;
    }

    final String label = resources.getString(labelId);

    // Build intent.

    final int keyCode = this.toKeyCode(actionName);
    final Intent intent = new Intent(ACTION_MEDIA_BUTTON);
    intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    intent.putExtra(PACKAGE_NAME, packageName);

    final PendingIntent pendingIntent = PendingIntent.getBroadcast(
      this.context, keyCode, intent, PendingIntent.FLAG_UPDATE_CURRENT
    );

    // Build action.

    final NotificationCompat.Action action = new NotificationCompat.Action(
      iconId, label, pendingIntent
    );

    this.actions.put(actionName, action);
  }

  /**
   * Mappers
   */

  private String toResourceName(final String actionName) {
    switch (actionName) {
      case "play":
        return RESOURCE_PLAY;
      case "pause":
        return RESOURCE_PAUSE;
      case "toggle":
        return RESOURCE_TOGGLE;
      case "stop":
        return RESOURCE_STOP;
      case "nextTrack":
        return RESOURCE_NEXT_TRACK;
      case "previousTrack":
        return RESOURCE_PREVIOUS_TRACK;
      case "skipForward":
        return RESOURCE_SKIP_FORWARD;
      case "skipBackward":
        return RESOURCE_SKIP_BACKWARD;
      default:
        return null;
    }
  }

  private int toKeyCode(final String actionName) {
    switch (actionName) {
      case "play":
        return KeyEvent.KEYCODE_MEDIA_PLAY;
      case "pause":
        return KeyEvent.KEYCODE_MEDIA_PAUSE;
      case "toggle":
        return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
      case "stop":
        return KeyEvent.KEYCODE_MEDIA_STOP;
      case "nextTrack":
        return KeyEvent.KEYCODE_MEDIA_NEXT;
      case "previousTrack":
        return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
      case "skipForward":
        return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
      case "skipBackward":
        return KeyEvent.KEYCODE_MEDIA_REWIND;
      default:
        return KeyEvent.KEYCODE_UNKNOWN;
    }
  }

}
