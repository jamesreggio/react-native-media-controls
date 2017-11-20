package com.jamesreggio.react.media;

import android.content.Intent;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class MediaControlsUpdateService extends HeadlessJsTaskService {

  private static final int SERVICE_TIMEOUT = 10000;

  @Override
  protected HeadlessJsTaskConfig getTaskConfig(final Intent intent) {
    return new HeadlessJsTaskConfig(
      "MediaControlsUpdateTask",
      null,
      SERVICE_TIMEOUT
    );
  }

}
