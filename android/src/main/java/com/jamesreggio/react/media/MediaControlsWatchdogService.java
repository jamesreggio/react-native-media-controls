package com.jamesreggio.react.media;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MediaControlsWatchdogService extends Service {

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_NOT_STICKY;
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    this.stopSelf();
  }

}
