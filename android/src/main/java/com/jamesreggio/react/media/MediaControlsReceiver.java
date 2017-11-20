//XXX emit special event for ACTION_DISMISS_NOTIFICATION

package com.jamesreggio.react.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.LifecycleState;

public class MediaControlsReceiver extends BroadcastReceiver {

  private final ReactApplicationContext context;
  private final MediaControlsModule module;

  private BroadcastReceiver.PendingResult pendingResult;

  MediaControlsReceiver(
    final ReactApplicationContext context,
    final MediaControlsModule module
  ) {
    this.context = context;
    this.module = module;
  }

  private boolean isThisPackage(final Intent intent) {
    if (intent.hasExtra(MediaControlsNotificationManager.PACKAGE_NAME)) {
      final String packageName = intent.getStringExtra(
        MediaControlsNotificationManager.PACKAGE_NAME
      );

      return this.context.getPackageName().equals(packageName);
    }

    return true;
  }

  private boolean isAppInForeground() {
    return this.context.getLifecycleState() == LifecycleState.RESUMED;
  }

  public void didUpdateDetails() {
    if (this.pendingResult != null) {
      this.pendingResult.finish();
      this.pendingResult = null;
    }
  }

  @Override
  public void onReceive(final Context context, final Intent intent) {
    if (!this.isThisPackage(intent)) {
      return;
    }

    final String action = intent.getAction();

    if (action == null) {
      return;
    }

    if (!isAppInForeground()) {
      final Intent serviceIntent = new Intent(context, MediaControlsUpdateService.class);
      context.startService(serviceIntent);
      HeadlessJsTaskService.acquireWakeLockNow(context);
    }

    if (this.pendingResult != null) {
      this.pendingResult.finish();
    }

    this.pendingResult = this.goAsync();

    switch (action) {
      case MediaControlsNotificationManager.ACTION_DISMISS_NOTIFICATION: {
        this.module.emit("stop");
        break;
      }

      case AudioManager.ACTION_AUDIO_BECOMING_NOISY: {
        WritableMap body = Arguments.createMap();
        body.putString("reason", "OLD_DEVICE_UNAVAILABLE");
        this.module.emit("routeChange", body);
        break;
      }

      case Intent.ACTION_MEDIA_BUTTON:
      case MediaControlsNotificationManager.ACTION_MEDIA_BUTTON: {
        if (!intent.hasExtra(Intent.EXTRA_KEY_EVENT)) {
          break;
        }

        KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        this.module.getSession().getController().dispatchMediaButtonEvent(keyEvent);
        break;
      }
    }
  }

}
