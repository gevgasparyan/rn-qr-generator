import {NativeModules, processColor} from 'react-native';

const {RNQrGenerator} = NativeModules;

export type QRCodeGenerateOptions = {
  value: string,
  backgroundColor?: string,
  color?: string,
  width?: number,
  height?: number,
  base64?: boolean,
};

export type QRCodeDetectOptions = {
  uri?: string,
  base64?: string,
};

export default {
  generate: (options: QRCodeGenerateOptions) => {
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
  detect: (options: QRCodeDetectOptions) => {
    const {uri, base64} = options;
    if (!uri && !base64) {
      return Promise.reject('Property "uri" or "base64" are missing');
    }
    const qrOptions = {
      ...options,
    };
    return new Promise((resolve, reject) => {
      RNQrGenerator.detect(
        qrOptions,
        error => reject(error),
        data => resolve(data),
      );
    });
  },
};
