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
  fileName?: string,
  correctionLevel: 'L' | 'M' | 'Q' | 'H',
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

export type CodeType = 'Aztec' | 'Codabar' | 'Code39' | 'Code93' | 'Code128' |
  'DataMatrix' | 'Ean8' | 'Ean13' | 'ITF' | 'MaxiCode' | 'PDF417' | 'QRCode' |
  'RSS14' | 'RSSExpanded' | 'UPCA' | 'UPCE' | 'UPCEANExtension';

export interface QRCodeScanResult {
  values: Array<string>;
  type: CodeType;
};

declare namespace RNQRGenerator {
  function generate(options: QRCodeGenerateOptions): Promise<QRCodeGenerateResult>;
  function detect(options: QRCodeDetectOptions): Promise<QRCodeScanResult>;
}

export default RNQRGenerator;
