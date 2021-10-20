import {NativeModules, Platform, processColor} from 'react-native';

const {RNQrGenerator} = NativeModules;

if (!NativeModules.RNQrGenerator) {
  const errorMessage =
    Platform.OS === 'ios'
      ? `Could not find module "RNQrGenerator".\nDid you forget to run "pod install" ?`
      : `Could not find module "RNQrGenerator".`;
  console.error(errorMessage);
}

export type Padding = {
  top?: number,
  left?: number,
  bottom?: number,
  right?: number,
};

export type QRCodeGenerateOptions = {
  value: string,
  backgroundColor?: string,
  color?: string,
  width?: number,
  height?: number,
  base64?: boolean,
  padding?: Padding,
  fileName?: string,
  correctionLevel: 'L' | 'M' | 'Q' | 'H',
};

export type QRCodeGenerateResult = {
  uri: string,
  width: number,
  height: number,
  base64?: string,
};

export type QRCodeDetectOptions = {
  uri?: string,
  base64?: string,
};

export type CodeType = 'Aztec' | 'Codabar' | 'Code39' | 'Code93' | 'Code128' |
  'DataMatrix' | 'Ean8' | 'Ean13' | 'ITF' | 'MaxiCode' | 'PDF417' | 'QRCode' |
  'RSS14' | 'RSSExpanded' | 'UPCA' | 'UPCE' | 'UPCEANExtension';

export type QRCodeScanResult = {
  values: string[],
  type: CodeType
};

export default {
  generate: (options: QRCodeGenerateOptions): Promise<QRCodeGenerateResult> => {
    const {value, backgroundColor, color} = options;
    if (!value) {
      return Promise.reject('Property "value" is missing');
    }
    const qrOptions = {
      correctionLevel: 'H',
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
  detect: (options: QRCodeDetectOptions): Promise<QRCodeScanResult> => {
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
