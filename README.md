
# rn-qr-generator

https://www.npmjs.com/package/rn-qr-generator

## Getting started

`$ npm install rn-qr-generator --save`

## Mostly automatic installation

`$ react-native link rn-qr-generator`

---

#### Important:
Linking is not needed anymore. ``react-native@0.60.0+`` supports dependencies auto linking.
For iOS you also need additional step to install auto linked Pods (Cocoapods should be installed):
``` 
cd ios && pod install && cd ../
```
___

## Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `rn-qr-generator` and add `RNQrGenerator.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNQrGenerator.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

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
    + compile project(':rn-qr-generator')
    }
  	```

## Usage
```javascript
import RNQRGenerator from 'rn-qr-generator';

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

#### generate

input properties

|    Property    | Type     | Description  |
| :------------- | :------: | :----------- |
| **`value`**    | string   | Text value to be converted into QR image. (required)
| **`width`**    | number   | Width of the QR image to be generated. (required)
| **`height`**   | number   | Height of the QR image to be generated. (required)
| backgroundColor| string   | Background color of the image. (white by default)
| color          | string   | Color of the image. (black by default)
| fileName       | string   | Name of the image file to store in FileSystem. (optional)
| correctionLevel| string   | Data restoration rate for total codewords. Available values are 'L', 'M', 'Q' and 'H'. ('H' by default)
| base64         | boolean  | If true will return base64 representation of the image. (optional, false by default)
| padding        | Object   | Padding params for the image to be generated: {top: number, left: number, bottom: number, right: number}. (default no padding)

payload

|    Property    | Type     | Description  |
| :------------- | :------: | :----------- |
| uri            | string   | Path of the generated image.
| width          | number   | Width of the generated image.
| height         | number   | Height of the generated image.
| base64         | boolean  | Base64 encoded string of the image.


#### detect

input properties

|    Property    | Type     | Description  |
| :------------- | :------: | :----------- |
| **`uri`**      | string   | Local path of the image. Can be skipped if base64 is passed.
| **`base64`**   | string   | Base64 representation of the image to be scanned. If uri is passed this option will be skipped.

payload

|    Property    | Type     | Description  |
| :------------- | :------: | :----------- |
| values         | string[]   | Array of detected QR code values. Empty if nothing found.
| type           | string     | Type of detected code.

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



![example](https://user-images.githubusercontent.com/13519034/104821872-50268480-5858-11eb-9e5b-77190f9da71d.gif)


Example of 2FA QR code with Time Based (TOTP) or Counter Based (HOTP)

```
RNQRGenerator.generate({
  ...
  value: 'otpauth://totp/Example:google@google.com?secret=HKSWY3RNEHPK3PXP&issuer=Issuer',
})
```

More information about totp can be found [here](https://github.com/google/google-authenticator/wiki/Key-Uri-Format).


This module uses `Zxing` library for encoding and decoding codes ( [ios](https://github.com/zxingify/zxingify-objc), [Android](https://github.com/journeyapps/zxing-android-embedded)).
# Note
Some simulators may not generate qr code properly. Use real device if you get an error.
