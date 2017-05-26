import {NativeModules, NativeEventEmitter} from 'react-native';

const {RNMediaControls: NativeControls} = NativeModules;
const NativeEvents = new NativeEventEmitter(NativeControls);

export const STATE_ERROR = 'ERROR';
export const STATE_LOADING = 'LOADING';
export const STATE_PLAYING = 'PLAYING';
export const STATE_PAUSED = 'PAUSED';
export const STATE_STOPPED = 'STOPPED';

export const resetDetails = () => (
  NativeControls.resetDetails()
);

export const updateDetails = (details) => (
  NativeControls.updateDetails(details)
);

export const toggleCommand = (name, enabled, options) => (
  NativeControls.toggleCommand(name, enabled, options)
);

export const on = (name, callback) => {
  const subscription = NativeEvents.addListener(name, callback);
  return () => NativeEvents.removeListener(subscription);
};
