import {NativeModules, NativeEventEmitter} from 'react-native';

const NativeControls = NativeModules.RNMediaControls;
const NativeEvents = new NativeEventEmitter(NativeControls);

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
