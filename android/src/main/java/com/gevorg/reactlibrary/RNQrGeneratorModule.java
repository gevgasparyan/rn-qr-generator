
package com.gevorg.reactlibrary;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.UUID;

public class RNQrGeneratorModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNQrGeneratorModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNQrGenerator";
  }

  @ReactMethod
  public void generate(final ReadableMap options, @Nullable Callback failureCallback, @Nullable Callback successCallback) {
    String value = options.hasKey("value") ? options.getString("value") : "";
    Double width = options.hasKey("width") ? options.getDouble("width") : 100;
    Double height = options.hasKey("height") ? options.getDouble("height") : 100;
    int backgroundColor = options.hasKey("backgroundColor") ? options.getInt("backgroundColor") : Color.WHITE;
    int color = options.hasKey("color") ? options.getInt("color") : Color.BLACK;

    boolean base64 = options.hasKey("base64") ? options.getBoolean("base64") : false;

    try {
      Bitmap bitmap = generateQrCode(value, width.intValue(), height.intValue(), backgroundColor, color);

      WritableMap response = Arguments.createMap();
      response.putDouble("width", bitmap.getWidth());
      response.putDouble("height", bitmap.getHeight());

      try {
        File cacheDirectory = this.reactContext.getCacheDir();
        File imageFile = new File(getOutputFilePath(cacheDirectory, ".png"));
        imageFile.createNewFile();
        FileOutputStream fOut = new FileOutputStream(imageFile);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();

        fOut.write(byteArray);
        fOut.flush();
        fOut.close();
        String fileUri = Uri.fromFile(imageFile).toString();
        response.putString("uri", fileUri);

        if (base64) {
          response.putString("base64", Base64.encodeToString(byteArray, Base64.NO_WRAP));
        }
        successCallback.invoke(response);
      } catch (IOException e) {
        failureCallback.invoke(e.getMessage());
      }
    }
    catch (WriterException e) {
      e.printStackTrace();
      failureCallback.invoke(e.getMessage());
    }
  }

  public static Bitmap generateQrCode(String myCodeText, int qrWidth, int qrHeight, int backgroundColor, int color) throws WriterException {
    Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% damage

    QRCodeWriter qrCodeWriter = new QRCodeWriter();

    BitMatrix bitMatrix = qrCodeWriter.encode(myCodeText,BarcodeFormat.QR_CODE, qrWidth, qrHeight, hintMap);
    int width = bitMatrix.getWidth();
    Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < width; y++) {
        bmp.setPixel(y, x, bitMatrix.get(x, y) ? color : backgroundColor);
      }
    }
    return bmp;
  }

  public static File ensureDirExists(File dir) throws IOException {
    if (!(dir.isDirectory() || dir.mkdirs())) {
      throw new IOException("Couldn't create directory '" + dir + "'");
    }
    return dir;
  }

  public static String getOutputFilePath(File directory, String extension) throws IOException {
    ensureDirExists(directory);
    String filename = UUID.randomUUID().toString();
    return directory + File.separator + filename + extension;
  }
}