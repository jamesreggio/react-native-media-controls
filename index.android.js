import {NativeModules, NativeEventEmitter} from 'react-native';

const NativeControls = NativeModules.RNMediaControls;
const NativeEvents = new NativeEventEmitter(NativeControls);

export const resetDetails = () => (
  NativeControls.resetDetails()
);

export const updateDetails = (details) => (
  NativeControls.updateDetails(details)
);

export const showRoutePicker = () => null;

export const getOutputRoutes = () => null;

export const toggleCommand = (name, enabled, options) => (
  NativeControls.toggleAction(name, enabled, options)
);

export const addListener = (name, callback) => (
  NativeEvents.addListener(name, callback)
);

export const removeListener = (subscription) => (
  NativeEvents.removeListener(subscription)
);
