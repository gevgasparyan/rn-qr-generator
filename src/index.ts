import { NativeModules, Platform, processColor } from "react-native";

const LINKING_ERROR =
  `The package 'rn-qr-generator' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: "" }) +
  "- You rebuilt the app after installing the package\n" +
  "- You are not using Expo Go\n";

// Try to get TurboModule first, then fall back to legacy
const isTurboModuleEnabled = (globalThis as any).__turboModuleProxy != null;

const RNQrGeneratorModule = isTurboModuleEnabled
  ? require("./NativeRNQrGenerator").default
  : NativeModules.RNQrGenerator;

const RNQrGenerator = RNQrGeneratorModule
  ? RNQrGeneratorModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      },
    );

export type Padding = {
  top?: number;
  left?: number;
  bottom?: number;
  right?: number;
};

export type QRCodeGenerateOptions = {
  value: string;
  backgroundColor?: string;
  color?: string;
  width?: number;
  height?: number;
  base64?: boolean;
  padding?: Padding;
  fileName?: string;
  correctionLevel?: "L" | "M" | "Q" | "H";
};

export type QRCodeGenerateResult = {
  uri: string;
  width: number;
  height: number;
  base64?: string;
};

export type QRCodeDetectOptions = {
  uri?: string;
  base64?: string;
};

export type CodeType =
  | "Aztec"
  | "Codabar"
  | "Code39"
  | "Code93"
  | "Code128"
  | "DataMatrix"
  | "Ean8"
  | "Ean13"
  | "ITF"
  | "MaxiCode"
  | "PDF417"
  | "QRCode"
  | "RSS14"
  | "RSSExpanded"
  | "UPCA"
  | "UPCE"
  | "UPCEANExtension";

export type QRCodeScanResult = {
  values: string[];
  type: CodeType;
};

export default {
  generate: (options: QRCodeGenerateOptions): Promise<QRCodeGenerateResult> => {
    const { value, backgroundColor, color } = options;
    if (!value) {
      return Promise.reject('Property "value" is missing');
    }
    const qrOptions = {
      correctionLevel: "H",
      ...options,
      backgroundColor: processColor(backgroundColor || "white") as number,
      color: processColor(color || "black") as number,
    };
    return new Promise((resolve, reject) => {
      RNQrGenerator.generate(
        qrOptions,
        (error: string) => reject(error),
        (data: QRCodeGenerateResult) => resolve(data),
      );
    });
  },
  detect: (options: QRCodeDetectOptions): Promise<QRCodeScanResult> => {
    const { uri, base64 } = options;
    if (!uri && !base64) {
      return Promise.reject('Property "uri" or "base64" are missing');
    }
    const qrOptions = {
      ...options,
    };
    return new Promise((resolve, reject) => {
      RNQrGenerator.detect(
        qrOptions,
        (error: string) => reject(error),
        (data: QRCodeScanResult) => resolve(data),
      );
    });
  },
};
