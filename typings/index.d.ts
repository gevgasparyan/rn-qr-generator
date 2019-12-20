// Type definitions for rn-qr-generator 1.0
// Project: https://github.com/gevorg94/rn-qr-generator#readme
// Definitions by: Gevorg Gasparyan <https://github.com/gevorg94>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped
// TypeScript Version: 2.1

declare namespace RNQRGenerator {
  function generate(options: QROptions): Promise<QRResponse>;
}

export default Share;

interface QRResponse {
  uri: string;
  width: number;
  height: number;
  base64?: boolean;
}

interface QROptions {
  value: string;
  width: number;
  height: number;
  base64?: boolean;
  color?: string;
  backgroundColor?: string;
}