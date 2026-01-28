package com.gevorg.reactlibrary;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
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
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ReactModule(name = RNQrGeneratorModule.NAME)
public class RNQrGeneratorModule extends NativeRNQrGeneratorSpec {

  public static final String NAME = "RNQrGenerator";
  private final ReactApplicationContext reactContext;
  private final static String SCHEME_CONTENT = "content";
  private final String TAG = "RNQRGenerator";

  public RNQrGeneratorModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @Override
  @ReactMethod
  public void generate(ReadableMap options, Callback failureCallback, Callback successCallback) {
    String value = options.hasKey("value") ? options.getString("value") : "";
    String fileName = options.hasKey("fileName") ? options.getString("fileName") : null;
    String correctionLevel = options.hasKey("correctionLevel") ? options.getString("correctionLevel") : "H";
    Double width = options.hasKey("width") ? options.getDouble("width") : 100;
    Double height = options.hasKey("height") ? options.getDouble("height") : 100;
    int backgroundColor = options.hasKey("backgroundColor") ? options.getInt("backgroundColor") : Color.WHITE;
    int color = options.hasKey("color") ? options.getInt("color") : Color.BLACK;
    ReadableMap padding = options.hasKey("padding") ? options.getMap("padding") : Arguments.createMap();

    Double top = padding != null && padding.hasKey("top") ? padding.getDouble("top") : 0;
    Double left = padding != null && padding.hasKey("left") ? padding.getDouble("left") : 0;
    Double bottom = padding != null && padding.hasKey("bottom") ? padding.getDouble("bottom") : 0;
    Double right = padding != null && padding.hasKey("right") ? padding.getDouble("right") : 0;
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
    } catch (WriterException e) {
      e.printStackTrace();
      failureCallback.invoke(e.getMessage());
    }
  }

  @Override
  @ReactMethod
  public void detect(ReadableMap options, Callback failureCallback, Callback successCallback) {
    String path = options.hasKey("uri") ? options.getString("uri") : "";
    String base64 = options.hasKey("base64") ? options.getString("base64") : "";

    Bitmap bitmap = null;
    if (path != null && !path.isEmpty() || base64 != null && !base64.isEmpty()) {
      try {
        bitmap = getBitmapFromSource(path != null ? path : "", base64 != null ? base64 : "");
      } catch (Exception e) {
        failureCallback.invoke("IMAGE_NOT_FOUND");
        return;
      }
    }
    final int MAX_RETRIES = 5;

    try {
      Result[] results = tryToScanQrImage(MAX_RETRIES, bitmap);
      BarcodeFormat format = results[0].getBarcodeFormat();
      String codeType = getCodeType(format);
      String[] texts = new String[results.length];

      for (int i=0;i<results.length;i++) {
        texts[i]= results[i].getText();
      }
      onDetectResult(texts, codeType, successCallback);
    } catch (Exception e) {
      e.printStackTrace();
      String[] texts = {};
      onDetectResult(texts, "", successCallback);
    }
  }

  private Result[] tryToScanQrImage(int MAX_RETRIES, Bitmap bitmap) throws Exception {
    int attemptCounterAndScale = 1;

    do{
      try {
        Result[] results = scanQRImage(scaleBitmap(bitmap,attemptCounterAndScale));
        return results;
      } catch (Exception e) {
        if(attemptCounterAndScale == MAX_RETRIES){
          throw e;
        }
        attemptCounterAndScale++;
      }
    }while(attemptCounterAndScale <= MAX_RETRIES);

    throw new Exception();
  }

  private Bitmap scaleBitmap(Bitmap bitmap, int scale){
    if(scale == 1 ){
      return bitmap;
    }
    int width = bitmap.getWidth()/scale;
    int height = bitmap.getHeight()/scale;

    return Bitmap.createScaledBitmap(bitmap,width,height,true);
  }

  private void onDetectResult(String[] results, String type, Callback successCallback) {
    WritableArray values = Arguments.createArray();
    for (int i=0;i<results.length;i++) {
      values.pushString(results[i]);
    }
    WritableMap response = Arguments.createMap();
    response.putArray("values", values);
    response.putString("type", type);
    successCallback.invoke(response);
  }

  private String getCodeType(BarcodeFormat format) {

    switch (format) {
      case AZTEC:
        return "Aztec";
      case CODABAR:
        return "Codabar";
      case CODE_39:
        return "Code39";
      case CODE_93:
        return "Code93";
      case CODE_128:
        return "Code128";
      case DATA_MATRIX:
        return "DataMatrix";
      case EAN_8:
        return "Ean8";
      case EAN_13:
        return "Ean13";
      case ITF:
        return "ITF";
      case MAXICODE:
        return "MaxiCode";
      case PDF_417:
        return "PDF417";
      case QR_CODE:
        return "QRCode";
      case RSS_14:
        return "RSS14";
      case RSS_EXPANDED:
        return "RSSExpanded";
      case UPC_A:
        return "UPCA";
      case UPC_E:
        return "UPCE";
      case UPC_EAN_EXTENSION:
        return "UPCEANExtension";
      default:
        return "";
    }
  }

  public static Bitmap generateQrCode(String myCodeText, int qrWidth, int qrHeight, int backgroundColor, int color, String correctionLevel) throws WriterException {
    int MARGIN_AUTOMATIC = -1;
    int MARGIN_NONE = 0;
    int marginSize = MARGIN_NONE;

    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    ErrorCorrectionLevel level = ErrorCorrectionLevel.H;
    if ("M".equals(correctionLevel)) {
      level = ErrorCorrectionLevel.M;
    } else if ("L".equals(correctionLevel)) {
      level = ErrorCorrectionLevel.L;
    } else if ("Q".equals(correctionLevel)) {
      level = ErrorCorrectionLevel.Q;
    }
    hints.put(EncodeHintType.ERROR_CORRECTION, level);
    hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
    if (marginSize != MARGIN_AUTOMATIC) {
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

  public static Result[] scanQRImage(Bitmap _bMap) throws Exception {
    try {
      Result[] result = scanBitmap(_bMap);
      return result;
    } catch (Exception e) {
      Log.e("RNQRGenerator", "Decode Failed:", e);
      Bitmap BWBitmap = createBlackAndWhite(_bMap);
      Bitmap bMap = invertBitmap(BWBitmap);
      return scanBitmap(bMap);
    }
  }

  public static Result[] scanBitmap(Bitmap bMap) throws Exception {
    Map<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
    hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

    Exception lastError = null;

    try {
      return decodeBitmapUsingBinarizers(bMap, hints);
    } catch (Exception e) {
      lastError = e;
      Log.e("RNQRGenerator", "Decode failed on original bitmap", e);
    }

    float[][] cropConfigs = new float[][]{
      {0.15f, 0.05f, 0.35f, 0.05f},
      {0.25f, 0.05f, 0.25f, 0.05f},
      {0.35f, 0.05f, 0.15f, 0.05f}
    };
    float[] zoomFactors = new float[]{1.2f, 1.5f, 2.0f};

    for (float[] config : cropConfigs) {
      Bitmap cropped = cropBitmapRelative(bMap, config[0], config[1], config[2], config[3]);
      if (cropped == null) {
        continue;
      }
      try {
        return decodeBitmapUsingBinarizers(cropped, hints);
      } catch (Exception cropError) {
        lastError = cropError;
        Log.e("RNQRGenerator", "Decode failed on cropped bitmap", cropError);
      }

      for (float zoom : zoomFactors) {
        if (zoom <= 1.0f) {
          continue;
        }
        Bitmap zoomed = scaleBitmapByFactor(cropped, zoom);
        if (zoomed == null) {
          continue;
        }
        try {
          return decodeBitmapUsingBinarizers(zoomed, hints);
        } catch (Exception zoomError) {
          lastError = zoomError;
          Log.e("RNQRGenerator", "Decode failed on zoomed bitmap", zoomError);
        }
      }
    }

    if (lastError != null) {
      throw lastError;
    }
    throw new Exception("Decode failed");
  }

  private static Result[] decodeBitmapUsingBinarizers(Bitmap bitmap, Map<DecodeHintType, Object> hints) throws Exception {
    LuminanceSource source = createLuminanceSource(bitmap);
    BinaryBitmap hybridBitmap = new BinaryBitmap(new HybridBinarizer(source));
    return decodeBinaryBitmap(new MultiFormatReader(), new QRCodeMultiReader(), hybridBitmap, hints);
  }

  private static Result[] decodeBinaryBitmap(Reader reader, QRCodeMultiReader readerMulti, BinaryBitmap bitmap, Map<DecodeHintType, Object> hints) throws Exception {
    try {
      Result result = reader.decode(bitmap, hints);
      if (!result.getText().isEmpty()) {
        Result[] results = new Result[1];
        results[0] = result;
        return results;
      }
      Result[] results = readerMulti.decodeMultiple(bitmap, hints);
      return results;
    } catch (Exception e) {
      Log.e("RNQRGenerator", "Decode Failed:", e);
      throw e;
    } finally {
      reader.reset();
      readerMulti.reset();
    }
  }

  private static LuminanceSource createLuminanceSource(Bitmap bitmap) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int[] pixels = new int[width * height];
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    return new RGBLuminanceSource(width, height, pixels);
  }

  private static Bitmap cropBitmapRelative(Bitmap bitmap, float topInset, float leftInset, float bottomInset, float rightInset) {
    if (bitmap == null) {
      return null;
    }
    if (topInset + bottomInset >= 1.0f || leftInset + rightInset >= 1.0f) {
      return null;
    }
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();

    int left = Math.max(0, Math.round(leftInset * width));
    int top = Math.max(0, Math.round(topInset * height));
    int right = Math.max(0, Math.round(rightInset * width));
    int bottom = Math.max(0, Math.round(bottomInset * height));

    int cropWidth = width - left - right;
    int cropHeight = height - top - bottom;

    if (cropWidth <= 0 || cropHeight <= 0) {
      return null;
    }

    return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight);
  }

  private static Bitmap scaleBitmapByFactor(Bitmap bitmap, float factor) {
    if (bitmap == null || factor <= 1.0f) {
      return bitmap;
    }
    int width = Math.max(1, Math.round(bitmap.getWidth() * factor));
    int height = Math.max(1, Math.round(bitmap.getHeight() * factor));
    return Bitmap.createScaledBitmap(bitmap, width, height, true);
  }


  public static Bitmap invertBitmap(Bitmap src)
  {
    int height = src.getHeight();
    int width = src.getWidth();

    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint();

    ColorMatrix matrixGrayscale = new ColorMatrix();
    matrixGrayscale.setSaturation(0);

    ColorMatrix matrixInvert = new ColorMatrix();
    matrixInvert.set(new float[]
            {
                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            });
    matrixInvert.preConcat(matrixGrayscale);

    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
    paint.setColorFilter(filter);

    canvas.drawBitmap(src, 0, 0, paint);

    return bitmap;
  }

  public static Bitmap createBlackAndWhite(Bitmap src) {
    int width, height;
    height = src.getHeight();
    width = src.getWidth();

    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bmpGrayscale);
    Paint paint = new Paint();
    ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0);
    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
    paint.setColorFilter(f);
    c.drawBitmap(src, 0, 0, paint);
    return bmpGrayscale;
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
      int imageScale = getRightScale(imageUri.getPath());
      sourceImage = loadBitmapFromFile(imageUri,imageScale);
    } else if (!base64.isEmpty()) {
      sourceImage = convertToBitmap(base64);
    }
    return sourceImage;
  }

  private Bitmap loadBitmapFromFile(Uri imageUri,int scale) throws IOException {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = scale;

    try {
      return loadBitmap(imageUri,options);
    } catch (Error e) {
      throw new IOException("Error loading image file");
    }
  }

  private int getRightScale(String imagePath){
    int scale = 1;
    while (!canBitmapFitInMemory(imagePath, scale)) {
      scale = scale << 1;
    }
    return scale;
  }

  public Bitmap convertToBitmap(String base64Str) throws IllegalArgumentException {
    byte[] decodedBytes = Base64.decode(
      base64Str.substring(base64Str.indexOf(",") + 1),
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


  private BitmapFactory.Options getBitmapOpts(String url, Boolean decode, int scale) {
    BitmapFactory.Options opts = new BitmapFactory.Options();
    opts.inJustDecodeBounds = !decode;
    opts.inSampleSize = scale;
    BitmapFactory.decodeFile(url, opts);
    return opts;
  }

  private int getBitmapSize(String url, Boolean decode, int scale) {
    BitmapFactory.Options opts = getBitmapOpts(url, decode, scale);
    return opts.outHeight * opts.outWidth * 32 / (1024 * 1024 * 8);
  }

  private boolean canBitmapFitInMemory(String path, int scale) {
    double availableMemory = availableMemory();
    availableMemory = availableMemory / 2;
    long size = getBitmapSize(path, false, scale);
    return size <= availableMemory;
  }

  private long availableMemory() {
    Runtime runtime = Runtime.getRuntime();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    return runtime.maxMemory() - usedMemory;
  }
}
