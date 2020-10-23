
package com.gevorg.reactlibrary;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class RNQrGeneratorModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;
  private final static String SCHEME_CONTENT = "content";

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
    String fileName = options.hasKey("fileName") ? options.getString("fileName") : null;
    String correctionLevel = options.hasKey("correctionLevel") ? options.getString("correctionLevel") : "H";
    Double width = options.hasKey("width") ? options.getDouble("width") : 100;
    Double height = options.hasKey("height") ? options.getDouble("height") : 100;
    int backgroundColor = options.hasKey("backgroundColor") ? options.getInt("backgroundColor") : Color.WHITE;
    int color = options.hasKey("color") ? options.getInt("color") : Color.BLACK;
    ReadableMap padding = options.hasKey("padding") ? options.getMap("padding") : Arguments.createMap();

    Double top = padding.hasKey("top") ? padding.getDouble("top") : 0;
    Double left = padding.hasKey("left") ? padding.getDouble("left") : 0;
    Double bottom = padding.hasKey("bottom") ? padding.getDouble("bottom") : 0;
    Double right = padding.hasKey("right") ? padding.getDouble("right"):  0;
    width = width - left - right;
    height = height - top - bottom;
    boolean base64 = options.hasKey("base64") ? options.getBoolean("base64") : false;

    try {
      Bitmap bitmap = generateQrCode(value, width.intValue(), height.intValue(), backgroundColor, color, correctionLevel);
      if (top != 0 || left != 0 || bottom != 0 || right != 0) {
        int newWidth = bitmap.getWidth() + left.intValue() + right.intValue();
        int newHeight = bitmap.getHeight() + top.intValue() + bottom.intValue();
        Bitmap output = Bitmap.createBitmap(
                newWidth,
                newHeight,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(output);
        canvas.drawColor(backgroundColor);
        canvas.drawBitmap(bitmap, left.floatValue(), top.floatValue(), null);
        bitmap = output;
      }

      WritableMap response = Arguments.createMap();
      response.putDouble("width", bitmap.getWidth());
      response.putDouble("height", bitmap.getHeight());

      try {
        File cacheDirectory = this.reactContext.getCacheDir();
        File imageFile = new File(getOutputFilePath(cacheDirectory, fileName, ".png"));
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

  @ReactMethod
  public void detect(final ReadableMap options, @Nullable Callback failureCallback, @Nullable Callback successCallback) {
    String path = options.hasKey("uri") ? options.getString("uri") : "";
    String base64 = options.hasKey("base64") ? options.getString("base64") : "";

    Bitmap bitmap = null;
    if (path == "" && base64 != "") {
      try {
        bitmap = getBitmapFromSource(path, base64);
      } catch (Exception e) {
        failureCallback.invoke("IMAGE_NOT_FOUND");
        return;
      }
    }
    try {
      String decoded = scanQRImage(bitmap);
      onDetectResult(decoded, successCallback);
    } catch (Exception e) {
      e.printStackTrace();
      onDetectResult("", successCallback);
    }
  }

  private void onDetectResult(String result, Callback successCallback) {
    WritableArray values = Arguments.createArray();
    if (result != "") {
      values.pushString(result);
    }
    WritableMap response = Arguments.createMap();
    response.putArray("values", values);
    successCallback.invoke(response);
  }

  public static Bitmap generateQrCode(String myCodeText, int qrWidth, int qrHeight, int backgroundColor, int color, String correctionLevel) throws WriterException {
    /**
     * Allow the zxing engine use the default argument for the margin variable
     */
    int MARGIN_AUTOMATIC = -1;

    /**
     * Set no margin to be added to the QR code by the zxing engine
     */
    int MARGIN_NONE = 0;
    int marginSize = MARGIN_NONE;

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    ErrorCorrectionLevel level = ErrorCorrectionLevel.H;
    if (correctionLevel == "M") {
      level = ErrorCorrectionLevel.M;
    } else if (correctionLevel == "L") {
      level = ErrorCorrectionLevel.L;
    } else if (correctionLevel == "Q") {
      level = ErrorCorrectionLevel.Q;
    }
    hints.put(EncodeHintType.ERROR_CORRECTION, level);
    if (marginSize != MARGIN_AUTOMATIC) {
      // We want to generate with a custom margin size
      hints.put(EncodeHintType.MARGIN, marginSize);
    }

    MultiFormatWriter writer = new MultiFormatWriter();
    BitMatrix result = writer.encode(myCodeText, BarcodeFormat.QR_CODE, qrWidth, qrHeight, hints);

    final int width = result.getWidth();
    final int height = result.getHeight();
    int[] pixels = new int[width * height];
    for (int y = 0; y < height; y++) {
      int offset = y * width;
      for (int x = 0; x < width; x++) {
        pixels[offset + x] = result.get(x, y) ? color : backgroundColor;
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    return bitmap;
  }

  public static String scanQRImage(Bitmap bMap) throws Exception {
    String contents = null;

    int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
    //copy pixel data from the Bitmap into the 'intArray' array
    bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());

    LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

    Reader reader = new MultiFormatReader();
    try {
      Result result = reader.decode(bitmap);
      contents = result.getText();
    }
    catch (Exception e) {
      throw e;
    }
    return contents;
  }

  public static File ensureDirExists(File dir) throws IOException {
    if (!(dir.isDirectory() || dir.mkdirs())) {
      throw new IOException("Couldn't create directory '" + dir + "'");
    }
    return dir;
  }

  public static String getOutputFilePath(File directory, String fileName, String extension) throws IOException {
    ensureDirExists(directory);
    String name = (fileName != null) ? fileName : UUID.randomUUID().toString();
    return directory + File.separator + name + extension;
  }

  public Bitmap getBitmapFromSource(String path, String base64) throws IOException {
    Bitmap sourceImage = null;
    if (!path.isEmpty()) {
      Uri imageUri = Uri.parse(path);
      sourceImage = loadBitmapFromFile(imageUri);
      FileOutputStream out = new FileOutputStream(imageUri.getPath());
      sourceImage.compress(Bitmap.CompressFormat.JPEG, 100, out); //100-best quality
      out.close();
    } else if (!base64.isEmpty()) {
      sourceImage = convertToBitmap(base64);
    }
    return  sourceImage;
  }

  private Bitmap loadBitmapFromFile(Uri imageUri) throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    loadBitmap(imageUri, options);

    options.inSampleSize = 1;
    options.inJustDecodeBounds = false;
    return loadBitmap(imageUri, options);
  }


  public Bitmap convertToBitmap(String base64Str) throws IllegalArgumentException {
    byte[] decodedBytes = Base64.decode(
            base64Str.substring(base64Str.indexOf(",")  + 1),
            Base64.DEFAULT
    );
    return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
  }

  private Bitmap loadBitmap(Uri imageUri, BitmapFactory.Options options) throws IOException {
    Bitmap sourceImage = null;
    String imageUriScheme = imageUri.getScheme();
    if (imageUriScheme == null || !imageUriScheme.equalsIgnoreCase(SCHEME_CONTENT)) {
      try {
        sourceImage = BitmapFactory.decodeFile(imageUri.getPath(), options);
      } catch (Exception e) {
        e.printStackTrace();
        throw new IOException("Error decoding image file");
      }
    } else {
      ContentResolver cr = this.reactContext.getContentResolver();
      InputStream input = cr.openInputStream(imageUri);
      if (input != null) {
        sourceImage = BitmapFactory.decodeStream(input, null, options);
        input.close();
      }
    }
    return sourceImage;
  }
}