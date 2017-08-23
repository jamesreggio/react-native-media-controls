#import "RNMediaControls.h"
#import <React/RCTConvert.h>

@import AVFoundation;
@import MediaPlayer;

@implementation RNMediaControls
{
  NSString *_artwork;
}

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

#pragma mark - Details

#define DETAIL_STRING_KEYS @{ \
  @"album": MPMediaItemPropertyAlbumTitle, \
  @"artist": MPMediaItemPropertyArtist, \
  @"title": MPMediaItemPropertyTitle, \
}

#define DETAIL_NUMBER_KEYS @{ \
  @"duration": MPMediaItemPropertyPlaybackDuration, \
  @"position": MPNowPlayingInfoPropertyElapsedPlaybackTime, \
  @"speed": MPNowPlayingInfoPropertyPlaybackRate, \
}

RCT_EXPORT_METHOD(updateDetails:(NSDictionary *)details
                       resolver:(RCTPromiseResolveBlock)resolve
                       rejecter:(RCTPromiseRejectBlock)reject)
{
  MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];

  NSMutableDictionary *nextDetails;
  if (center.nowPlayingInfo == nil) {
    nextDetails = [NSMutableDictionary dictionary];
  } else {
    nextDetails = [NSMutableDictionary dictionaryWithDictionary:center.nowPlayingInfo];
  }

  for (NSString *key in DETAIL_STRING_KEYS) {
    if (details[key] != nil) {
      NSObject *value = [RCTConvert NSString:details[key]];
      [nextDetails setValue:value forKey:DETAIL_STRING_KEYS[key]];
    }
  }

  for (NSString *key in DETAIL_NUMBER_KEYS) {
    if (details[key] != nil) {
      NSObject *value = [RCTConvert NSNumber:details[key]];
      [nextDetails setValue:value forKey:DETAIL_NUMBER_KEYS[key]];
    }
  }

  BOOL updateArtwork = (details[@"artwork"] != nil && ![details[@"artwork"] isEqual:_artwork]);
  if (updateArtwork) {
    [nextDetails removeObjectForKey:MPMediaItemPropertyArtwork];
  }

  center.nowPlayingInfo = nextDetails;
  NSDictionary *updatedDetails = center.nowPlayingInfo;
  BOOL updateSuccessful = [nextDetails isEqualToDictionary:updatedDetails];

  if (updateArtwork && updateSuccessful) {
    _artwork = details[@"artwork"];
    [self updateArtwork];
  }

  resolve(@(updateSuccessful));
}

RCT_REMAP_METHOD(resetDetails,
                 resetDetailsWithResolver:(RCTPromiseResolveBlock)resolve
                                 rejecter:(RCTPromiseRejectBlock)reject)
{
  MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];
  center.nowPlayingInfo = nil;
  _artwork = nil;

  NSDictionary *updatedDetails = center.nowPlayingInfo;
  resolve(@(updatedDetails == nil || updatedDetails.count == 0));
}

- (void)updateArtwork
{
  [self updateArtwork:0];
}

- (void)updateArtwork:(uint)attempt
{
  if (attempt >= 3) {
    return;
  }

  uint delayInSeconds = 1.5; // Delay necessary to prevent `nowPlayingInfo` assignment from being a no-op.
  dispatch_time_t dispatchTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_BACKGROUND, 0), ^{
    NSString *url = _artwork;
    UIImage *image = nil;
    if (![url isEqual:@""]) {
      if ([url.lowercaseString hasPrefix:@"http://"] || [url.lowercaseString hasPrefix:@"https://"]) {
        NSURL *imageURL = [NSURL URLWithString:url];
        NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
        image = [UIImage imageWithData:imageData];
      } else {
        BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:url];
        if (fileExists) {
          image = [UIImage imageNamed:url];
        }
      }
    }

    if (image == nil) {
      return;
    }

    CIImage *cim = [image CIImage];
    CGImageRef cgref = [image CGImage];
    if (cim != nil || cgref != NULL) {
      dispatch_after(dispatchTime, dispatch_get_main_queue(), ^{
        if ([url isEqual:_artwork]) {
          MPNowPlayingInfoCenter *center = [MPNowPlayingInfoCenter defaultCenter];

          NSMutableDictionary *nextDetails;
          if (center.nowPlayingInfo == nil) {
            nextDetails = [NSMutableDictionary dictionary];
          } else {
            nextDetails = [NSMutableDictionary dictionaryWithDictionary:center.nowPlayingInfo];
          }

          MPMediaItemArtwork *artwork = [[MPMediaItemArtwork alloc] initWithImage:image];
          [nextDetails setValue:artwork forKey:MPMediaItemPropertyArtwork];
          center.nowPlayingInfo = nextDetails;

          if (!center.nowPlayingInfo[MPMediaItemPropertyArtwork]) {
            [self updateArtwork:(attempt + 1)];
          }
        }
      });
    }
  });
}

#pragma mark - Route

RCT_EXPORT_METHOD(showRoutePicker)
{
  // This is an absurdly hacky way to display the AirPlay selection popover.
  // https://stackoverflow.com/a/15583062

  UIViewController *presentedController = RCTPresentedViewController();
  UIView *presentedView = presentedController.view;

  MPVolumeView *volumeView = [[MPVolumeView alloc] initWithFrame:CGRectZero];
  volumeView.hidden = YES;
  [presentedView addSubview:volumeView];

  for (UIButton *button in volumeView.subviews) {
    if ([button isKindOfClass:[UIButton class]]) {
      [button sendActionsForControlEvents:UIControlEventTouchUpInside];
      break;
    }
  }

  [volumeView removeFromSuperview];
}

RCT_REMAP_METHOD(getOutputRoutes,
                 getOutputRoutesWithResolver:(RCTPromiseResolveBlock)resolve
                                   rejecter:(RCTPromiseRejectBlock)reject)
{
  AVAudioSession *session = [AVAudioSession sharedInstance];
  NSArray<AVAudioSessionPortDescription *> *descriptions = session.currentRoute.outputs;
  NSMutableArray<NSDictionary *> *routes = [[NSMutableArray alloc] initWithCapacity:descriptions.count];
  for (NSUInteger i = 0; i < descriptions.count; i++) {
    AVAudioSessionPortDescription *description = descriptions[i];
    NSMutableDictionary *route = [NSMutableDictionary dictionary];
    route[@"name"] = description.portName;

    NSString *portType = description.portType;
    if ([portType isEqualToString:AVAudioSessionPortLineOut]) {
      route[@"type"] = @"LINE_OUT";
    } else if ([portType isEqualToString:AVAudioSessionPortHeadphones]) {
      route[@"type"] = @"HEADPHONES";
    } else if ([portType isEqualToString:AVAudioSessionPortBuiltInReceiver]) {
      route[@"type"] = @"BUILTIN_RECEIVER";
    } else if ([portType isEqualToString:AVAudioSessionPortBuiltInSpeaker]) {
      route[@"type"] = @"BUILTIN_SPEAKER";
    } else if ([portType isEqualToString:AVAudioSessionPortHDMI]) {
      route[@"type"] = @"HDMI";
    } else if ([portType isEqualToString:AVAudioSessionPortAirPlay]) {
      route[@"type"] = @"AIRPLAY";
    } else if ([portType isEqualToString:AVAudioSessionPortBluetoothLE]) {
      route[@"type"] = @"BLUETOOTH_LE";
    } else if ([portType isEqualToString:AVAudioSessionPortBluetoothA2DP]) {
      route[@"type"] = @"BLUETOOTH_A2DP";
    } else if ([portType isEqualToString:AVAudioSessionPortBluetoothHFP]) {
      route[@"type"] = @"BLUETOOTH_HFP";
    } else if ([portType isEqualToString:AVAudioSessionPortUSBAudio]) {
      route[@"type"] = @"USB";
    } else if ([portType isEqualToString:AVAudioSessionPortCarAudio]) {
      route[@"type"] = @"CARPLAY";
    } else {
      route[@"type"] = @"UNKNOWN";
    }

    routes[i] = route;
  }

  resolve(routes);
}

#pragma mark - Events

- (NSArray<NSString *> *)supportedEvents
{
  return @[
    @"pause",
    @"play",
    @"stop",
    @"toggle",
    @"nextTrack",
    @"previousTrack",
    @"seek",
    @"seekForward",
    @"seekBackward",
    @"skipForward",
    @"skipBackward",
    @"routeChange",
    @"systemReset",
    @"interrupt",
  ];
}

RCT_EXPORT_METHOD(toggleCommand:(NSString *)name
                        enabled:(BOOL)enabled
                        options:(NSDictionary *)options)
{
  MPRemoteCommandCenter *commandCenter = [MPRemoteCommandCenter sharedCommandCenter];

  if ([name isEqual: @"pause"]) {
    [self toggleCommandHandler:commandCenter.pauseCommand enabled:enabled selector:@selector(onPause:)];
  } else if ([name isEqual: @"play"]) {
    [self toggleCommandHandler:commandCenter.playCommand enabled:enabled selector:@selector(onPlay:)];
  } else if ([name isEqual: @"stop"]) {
    [self toggleCommandHandler:commandCenter.stopCommand enabled:enabled selector:@selector(onStop:)];
  } else if ([name isEqual: @"toggle"]) {
    [self toggleCommandHandler:commandCenter.togglePlayPauseCommand enabled:enabled selector:@selector(onToggle:)];
  } else if ([name isEqual: @"nextTrack"]) {
    [self toggleCommandHandler:commandCenter.nextTrackCommand enabled:enabled selector:@selector(onNextTrack:)];
  } else if ([name isEqual: @"previousTrack"]) {
    [self toggleCommandHandler:commandCenter.previousTrackCommand enabled:enabled selector:@selector(onPreviousTrack:)];
  } else if ([name isEqual: @"seek"]) {
    [self toggleCommandHandler:commandCenter.changePlaybackPositionCommand enabled:enabled selector:@selector(onSeek:)];
  } else if ([name isEqual: @"seekForward"]) {
    [self toggleCommandHandler:commandCenter.seekForwardCommand enabled:enabled selector:@selector(onSeekForward:)];
  } else if ([name isEqual: @"seekBackward"]) {
    [self toggleCommandHandler:commandCenter.seekBackwardCommand enabled:enabled selector:@selector(onSeekBackward:)];
  } else if ([name isEqual:@"skipForward"]) {
    MPSkipIntervalCommand *command = commandCenter.skipForwardCommand;
    if (options[@"interval"]) {
      NSNumber *interval = [RCTConvert NSNumber:options[@"interval"]];
      command.preferredIntervals = @[interval];
    }
    [self toggleCommandHandler:command enabled:enabled selector:@selector(onSkipForward:)];
  } else if ([name isEqual:@"skipBackward"]) {
    MPSkipIntervalCommand *command = commandCenter.skipBackwardCommand;
    if (options[@"interval"]) {
      NSNumber *interval = [RCTConvert NSNumber:options[@"interval"]];
      command.preferredIntervals = @[interval];
    }
    [self toggleCommandHandler:command enabled:enabled selector:@selector(onSkipBackward:)];
  } else if ([name isEqual: @"routeChange"]) {
    [self toggleNotificationHandler:AVAudioSessionRouteChangeNotification enabled:enabled selector:@selector(onRouteChange:)];
  } else if ([name isEqual: @"systemReset"]) {
    [self toggleNotificationHandler:AVAudioSessionMediaServicesWereResetNotification enabled:enabled selector:@selector(onSystemReset:)];
  } else if ([name isEqual: @"interrupt"]) {
    [self toggleNotificationHandler:AVAudioSessionInterruptionNotification enabled:enabled selector:@selector(onInterrupt:)];
  }
}

- (void)toggleCommandHandler:(MPRemoteCommand *)command enabled:(BOOL)enabled selector:(SEL)selector
{
  [command removeTarget:self action:selector];
  if (enabled) {
    [command addTarget:self action:selector];
  }
  command.enabled = enabled;
}

- (void)toggleNotificationHandler:(NSNotificationName)notification enabled:(BOOL)enabled selector:(SEL)selector
{
  NSNotificationCenter *notificationCenter = [NSNotificationCenter defaultCenter];
  if (enabled) {
    [notificationCenter addObserver:self selector:selector name:notification object:nil];
  } else {
    [notificationCenter removeObserver:self name:notification object:nil];
  }
}

- (void)onPause:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"pause" body:nil]; }
- (void)onPlay:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"play" body:nil]; }
- (void)onStop:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"stop" body:nil]; }
- (void)onToggle:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"toggle" body:nil]; }
- (void)onNextTrack:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"nextTrack" body:nil]; }
- (void)onPreviousTrack:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"previousTrack" body:nil]; }
- (void)onSeek:(MPChangePlaybackPositionCommandEvent*)event { [self sendEventWithName:@"seek" body:@{@"position": @(event.positionTime)}]; }
- (void)onSeekForward:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"seekForward" body:nil]; }
- (void)onSeekBackward:(MPRemoteCommandEvent*)event { [self sendEventWithName:@"seekBackward" body:nil]; }
- (void)onSkipForward:(MPSkipIntervalCommandEvent*)event { [self sendEventWithName:@"skipForward" body:@{@"interval": @(event.interval)}]; }
- (void)onSkipBackward:(MPSkipIntervalCommandEvent*)event { [self sendEventWithName:@"skipBackward" body:@{@"interval": @(event.interval)}]; }

- (void)onRouteChange:(NSNotification*)notification {
  NSDictionary *userInfo = notification.userInfo;
  NSMutableDictionary *body = [NSMutableDictionary dictionary];

  AVAudioSessionRouteChangeReason reason = [userInfo[AVAudioSessionRouteChangeReasonKey] intValue];
  if (reason == AVAudioSessionRouteChangeReasonNewDeviceAvailable) {
    body[@"reason"] = @"NEW_DEVICE_AVAILABLE";
  } else if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable) {
    body[@"reason"] = @"OLD_DEVICE_UNAVAILABLE";
  } else {
    body[@"reason"] = @"UNIMPLEMENTED";
  }

  [self sendEventWithName:@"routeChange" body:body];
}

- (void)onSystemReset:(NSNotification*)notification {
  [self sendEventWithName:@"systemReset" body:nil];
}

- (void)onInterrupt:(NSNotification*)notification {
  NSDictionary *userInfo = notification.userInfo;
  AVAudioSessionInterruptionType type = [userInfo[AVAudioSessionInterruptionTypeKey] intValue];

  NSMutableDictionary *body = [NSMutableDictionary dictionary];
  if (type == AVAudioSessionInterruptionTypeBegan) {
    body[@"type"] = @"BEGAN";

    BOOL wasSuspended = [userInfo[AVAudioSessionInterruptionWasSuspendedKey] boolValue];
    body[@"wasSuspended"] = @(wasSuspended);
  } else if (type == AVAudioSessionInterruptionTypeEnded) {
    body[@"type"] = @"ENDED";

    AVAudioSessionInterruptionOptions option = [userInfo[AVAudioSessionInterruptionOptionKey] intValue];
    body[@"shouldResume"] = @(option == AVAudioSessionInterruptionOptionShouldResume);
  }

  [self sendEventWithName:@"interrupt" body:body];
}

@end
