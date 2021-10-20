/**
 * @flow strict-local
 */

import React, {useState} from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Image,
  Dimensions,
  Text,
  StatusBar,
  TextInput,
  Button,
  View,
  TouchableOpacity,
} from 'react-native';
import RNQRGenerator from 'rn-qr-generator';
import {launchImageLibrary} from 'react-native-image-picker';

const {height} = Dimensions.get('screen');

const options = {
  title: 'photoUpload',
  takePhotoButtonTitle: 'photoTake',
  chooseFromLibraryButtonTitle: 'photoLibrary',
  cancelButtonTitle: 'cancel',
  quality: 0.7,
  base64: true,
  maxWidth: 728,
};

const App: () => React$Node = () => {
  const [imageUri, setImageUri] = useState(null);
  const [detectImageUri, setDetectImageUri] = useState(null);
  const [detectedValues, setDetectedValues] = useState([]);
  const [text, setText] = useState('');
  const [error, setError] = useState('');

  const generate = () => {
    if (!text.trim()) {
      setError('value cannot be empty');
      setText('');
      return;
    }
    RNQRGenerator.generate({
      // value: 'otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example', // required
      value: text,
      height: 300,
      width: 300,
      base64: true,
      backgroundColor: 'white',
      color: 'black',
      correctionLevel: 'M',
      // padding: {
      //   top: 0,
      //   left: 0,
      //   bottom: 0,
      //   right: 0,
      // }
    })
      .then((response) => {
        console.log('Response:', response);
        setImageUri({uri: response.uri});
      })
      .catch((err) => console.log('Cannot create QR code', err));
  };

  const onPick = () => {
    launchImageLibrary(options, (response) => {
      setDetectImageUri({uri: response.uri});
      RNQRGenerator.detect({uri: response.uri})
        .then((res) => {
          console.log('Detected', res);
          if (res.values.length === 0) {
            setDetectedValues(['Code not found']);
          } else {
            setDetectedValues(res.values);
          }
        })
        .catch((err) => {
          console.log('Cannot detect', err);
        });
    });
  };

  const handleChange = (val) => {
    setError('');
    setText(val);
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <Text style={styles.title}>Enter text to generate QR code</Text>
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.input}
          placeholderTextColor={error ? 'rgba(255,0,0,0.5)' : undefined}
          placeholder={error || 'value'}
          onChangeText={handleChange}
          value={text}
        />
        <TouchableOpacity style={styles.button} onPress={generate}>
          <Text style={styles.buttonText}>Generate</Text>
        </TouchableOpacity>
      </View>
      <Image style={styles.image} source={imageUri} />
      <Button title="Pick from library" onPress={onPick} />
      <Text style={styles.detectedValues}>{detectedValues.join(', ')}</Text>
      <Image style={styles.image} source={detectImageUri} />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
  },
  error: {
    color: 'red',
  },
  button: {
    maxWidth: 100,
    backgroundColor: '#2ea3f2',
    padding: 4,
    borderRadius: 4,
    marginLeft: 8,
  },
  buttonText: {
    color: 'white',
    padding: 2,
  },
  image: {
    backgroundColor: '#F3F3F3',
    width: height / 3,
    height: height / 3,
    borderWidth: StyleSheet.hairlineWidth,
    marginBottom: 16,
  },
  title: {
    fontSize: 18,
    paddingBottom: 10,
    textAlign: 'center',
  },
  inputContainer: {
    flexDirection: 'row',
    paddingHorizontal: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  input: {
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
    padding: 4,
    borderRadius: 4,
    height: 30,
  },
  detectedValues: {
    marginBottom: 4,
  },
});

export default App;
