import {AppRegistry, NativeModules, NativeEventEmitter} from 'react-native';

const NativeControls = NativeModules.RNMediaControls;
const NativeEvents = new NativeEventEmitter(NativeControls);

let onUpdate = null;

export const resetDetails = () => (
  NativeControls.resetDetails()
);

export const updateDetails = async (details) => {
  const result = await NativeControls.updateDetails(details);

  if (onUpdate) {
    onUpdate();
    onUpdate = null;
  }

  return result;
};

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

AppRegistry.registerHeadlessTask(
  'MediaControlsUpdateTask',
  () => () => {
    if (onUpdate) {
      onUpdate();
      onUpdate = null;
    }

    return new Promise(resolve => {
      onUpdate = resolve;
    });
  },
);
