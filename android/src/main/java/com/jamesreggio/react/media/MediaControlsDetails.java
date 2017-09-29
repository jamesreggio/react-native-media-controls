//XXX support first-class playback state
//XXX support additional fields

package com.jamesreggio.react.media;

import com.facebook.react.bridge.ReadableMap;

public class MediaControlsDetails {

  private String album;
  private String artist;
  private String title;
  private Integer duration;
  private Integer position;
  private Float speed;

  public void update(final ReadableMap details) {
    if (details.hasKey("album")) {
      this.album = details.getString("album");
    }

    if (details.hasKey("artist")) {
      this.artist = details.getString("artist");
    }

    if (details.hasKey("title")) {
      this.title = details.getString("title");
    }

    if (details.hasKey("duration")) {
      this.duration = details.isNull("duration") ?
        null : new Integer((int)details.getDouble("duration"));
    }

    if (details.hasKey("position")) {
      this.position = details.isNull("position") ?
        null : new Integer((int)details.getDouble("position"));
    }

    if (details.hasKey("speed")) {
      this.speed = details.isNull("speed") ?
        null : new Float(details.getDouble("speed"));
    }
  }

  public String getAlbum() {
    return this.album;
  }

  public String getArtist() {
    return this.artist;
  }

  public String getTitle() {
    return this.title;
  }

  public Integer getDuration() {
    return this.duration;
  }

  public Integer getPosition() {
    return this.position;
  }

  public Float getSpeed() {
    return this.speed;
  }

  public Boolean getPlaying() {
    if (this.speed == null) {
      return null;
    }

    return new Boolean(this.speed > 0.0f ? true : false);
  }

}
