import type { TurboModule } from "react-native";
import { TurboModuleRegistry } from "react-native";

export interface Padding {
  top?: number;
  left?: number;
  bottom?: number;
  right?: number;
}

export interface GenerateOptions {
  value: string;
  backgroundColor?: number;
  color?: number;
  width?: number;
  height?: number;
  base64?: boolean;
  padding?: Padding;
  fileName?: string;
  correctionLevel?: string;
}

export interface GenerateResult {
  uri: string;
  width: number;
  height: number;
  base64?: string;
}

export interface DetectOptions {
  uri?: string;
  base64?: string;
}

export interface DetectResult {
  values: string[];
  type: string;
}

export interface Spec extends TurboModule {
  generate(
    options: GenerateOptions,
    errorCallback: (error: string) => void,
    successCallback: (result: GenerateResult) => void,
  ): void;
  detect(
    options: DetectOptions,
    errorCallback: (error: string) => void,
    successCallback: (result: DetectResult) => void,
  ): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>("RNQrGenerator");
