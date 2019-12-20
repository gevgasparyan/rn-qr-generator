import {NativeModules, processColor} from 'react-native';

const {RNQrGenerator} = NativeModules;

export default {
  generate: options => {
    const {value, backgroundColor, color} = options;
    if (!value) {
      return Promise.reject('Property "value" is missing');
    }
    const qrOptions = {
      ...options,
      backgroundColor: processColor(backgroundColor || 'white'),
      color: processColor(color || 'black'),
    };
    return new Promise((resolve, reject) => {
      RNQrGenerator.generate(
        qrOptions,
        error => reject(error),
        data => resolve(data),
      );
    });
  },
};
