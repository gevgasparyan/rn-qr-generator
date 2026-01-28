
# rn-qr-generator

https://www.npmjs.com/package/rn-qr-generator

React Native QR Code generator and reader library supporting both the **New Architecture (TurboModules/Fabric)** and the legacy architecture.

## Version Compatibility

| Version | React Native | Architecture Support |
|---------|--------------|---------------------|
| 2.x.x   | >= 0.71.0    | New Architecture (TurboModules) + Legacy |
| 1.x.x   | >= 0.55.0    | Legacy Architecture only |

> **Note:** If you're using React Native < 0.71.0 or need to stay on the legacy architecture, use version 1.4.5:
> ```bash
> npm install rn-qr-generator@1.4.5
> ```

## Getting started

```bash
npm install rn-qr-generator --save
# or
yarn add rn-qr-generator
```

### iOS Installation

```bash
cd ios && pod install && cd ../
```

### New Architecture (React Native 0.71+)

This library automatically supports the New Architecture when enabled in your project. No additional configuration is required.

**For iOS:** Make sure you have `RCT_NEW_ARCH_ENABLED=1` in your Podfile or environment.

**For Android:** Make sure you have `newArchEnabled=true` in your `gradle.properties`.

## Manual installation (Legacy)


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `rn-qr-generator` and add `RNQrGenerator.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNQrGenerator.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.gevorg.reactlibrary.RNQrGeneratorPackage;` to the imports at the top of the file
  - Add `new RNQrGeneratorPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
    ```diff
    rootProject.name = 'MyApp'
    include ':app'

  	+ include ':rn-qr-generator'
  	+ project(':rn-qr-generator').projectDir = new File(rootProject.projectDir, 	'../node_modules/rn-qr-generator/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
    ```diff
    dependencies {
    + implementation project(':rn-qr-generator')
    }
  	```

## Usage

```javascript
import RNQRGenerator from 'rn-qr-generator';

// Generate QR Code
RNQRGenerator.generate({
  value: 'https://github.com/gevgasparyan/rn-qr-generator',
  height: 100,
  width: 100,
})
  .then(response => {
    const { uri, width, height, base64 } = response;
    this.setState({ imageUri: uri });
  })
  .catch(error => console.log('Cannot create QR code', error));

// Detect QR code in image
RNQRGenerator.detect({
  uri: PATH_TO_IMAGE
})
  .then(response => {
    const { values } = response; // Array of detected QR code values. Empty if nothing found.
  })
  .catch(error => console.log('Cannot detect QR code in image', error));
```

### TypeScript Support

This library includes TypeScript definitions out of the box:

```typescript
import RNQRGenerator, { 
  QRCodeGenerateOptions, 
  QRCodeGenerateResult,
  QRCodeDetectOptions,
  QRCodeScanResult 
} from 'rn-qr-generator';

const options: QRCodeGenerateOptions = {
  value: 'Hello World',
  width: 200,
  height: 200,
  correctionLevel: 'H',
};

const result: QRCodeGenerateResult = await RNQRGenerator.generate(options);
```

## API Reference

### generate

Generates a QR code image from the provided value.

#### Input Properties

| Property | Type | Description |
| :------------- | :------: | :----------- |
| **`value`** | string | Text value to be converted into QR image. (required) |
| **`width`** | number | Width of the QR image to be generated. (required) |
| **`height`** | number | Height of the QR image to be generated. (required) |
| backgroundColor | string | Background color of the image. (white by default) |
| color | string | Color of the image. (black by default) |
| fileName | string | Name of the image file to store in FileSystem. (optional) |
| correctionLevel | string | Data restoration rate for total codewords. Available values are 'L', 'M', 'Q' and 'H'. ('H' by default) |
| base64 | boolean | If true will return base64 representation of the image. (optional, false by default) |
| padding | Object | Padding params for the image to be generated: `{top: number, left: number, bottom: number, right: number}`. (default no padding) |

#### Response

| Property | Type | Description |
| :------------- | :------: | :----------- |
| uri | string | Path of the generated image. |
| width | number | Width of the generated image. |
| height | number | Height of the generated image. |
| base64 | string | Base64 encoded string of the image (if requested). |


### detect

Detects QR codes in an image.

#### Input Properties

| Property | Type | Description |
| :------------- | :------: | :----------- |
| **`uri`** | string | Local path of the image. Can be skipped if base64 is passed. |
| **`base64`** | string | Base64 representation of the image to be scanned. If uri is passed this option will be skipped. |

#### Response

| Property | Type | Description |
| :------------- | :------: | :----------- |
| values | string[] | Array of detected QR code values. Empty if nothing found. |
| type | string | Type of detected code. |

### Supported Barcode Types

The following barcode types are currently supported for decoding:

* UPC-A and UPC-E
* EAN-8 and EAN-13
* Code 39
* Code 93
* Code 128
* ITF
* Codabar
* RSS-14 (all variants)
* QR Code
* Data Matrix
* Maxicode
* Aztec ('beta' quality)
* PDF 417 ('beta' quality)


## Example

![example](https://user-images.githubusercontent.com/13519034/104821872-50268480-5858-11eb-9e5b-77190f9da71d.gif)


### 2FA QR Code Example

Example of 2FA QR code with Time Based (TOTP) or Counter Based (HOTP):

```javascript
RNQRGenerator.generate({
  ...
  value: 'otpauth://totp/Example:google@google.com?secret=HKSWY3RNEHPK3PXP&issuer=Issuer',
})
```

More information about TOTP can be found [here](https://github.com/google/google-authenticator/wiki/Key-Uri-Format).

## Migration from 1.x to 2.x

Version 2.0.0 adds support for React Native's New Architecture (TurboModules). The JavaScript API remains the same, so migration should be seamless:

1. Update the package:
   ```bash
   npm install rn-qr-generator@latest
   # or
   yarn upgrade rn-qr-generator
   ```

2. For iOS, run:
   ```bash
   cd ios && pod install && cd ../
   ```

3. For Android, rebuild your app.

**Breaking Changes:**
- Minimum React Native version is now 0.71.0
- Minimum iOS version is now 13.0
- Minimum Android SDK version is now 21

## Dependencies

This module uses `ZXing` library for encoding and decoding codes:
- iOS: [ZXingObjC](https://github.com/zxingify/zxingify-objc)
- Android: [zxing-core](https://github.com/zxing/zxing)

## Troubleshooting

### Simulator Issues
Some simulators may not generate QR codes properly. Use a real device if you get an error.

### New Architecture Issues
If you encounter issues with the New Architecture:
1. Make sure your React Native version is 0.71.0 or higher
2. Verify that the New Architecture is properly enabled in your project
3. Clean and rebuild your project:
   ```bash
   # iOS
   cd ios && pod install && cd ..
   npx react-native run-ios
   
   # Android
   cd android && ./gradlew clean && cd ..
   npx react-native run-android
   ```

## License

MIT
