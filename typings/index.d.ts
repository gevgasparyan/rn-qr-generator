// Type definitions for rn-qr-generator 1.0
// Project: https://github.com/gevgasparyan/rn-qr-generator#readme
// Definitions by: Gevorg Gasparyan <https://github.com/gevgasparyan>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

export interface Padding {
  top?: number;
  left?: number;
  bottom?: number;
  right?: number;
};

export interface QRCodeGenerateOptions  {
  value: string;
  backgroundColor?: string;
  color?: string;
  width?: number;
  height?: number;
  base64?: boolean;
  padding?: Padding;
};

export interface QRCodeGenerateResult {
  uri: string;
  width: number;
  height: number;
  base64?: string;
};

export interface QRCodeDetectOptions {
  uri?: string;
  base64?: string;
};

export interface QRCodeScanResult {
  values: Array<string>;
};

declare namespace RNQRGenerator {
  function generate(options: QRCodeGenerateOptions): Promise<QRCodeGenerateResult>;
  function detect(options: QRCodeDetectOptions): Promise<QRCodeScanResult>;
}

export default RNQRGenerator;
