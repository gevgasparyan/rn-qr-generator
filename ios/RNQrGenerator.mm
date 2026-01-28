#import "RNQrGenerator.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("RCTConvert.h")
#import "RCTConvert.h"
#else
#import "React/RCTConvert.h"
#endif

#import <UIKit/UIKit.h>
#import <ZXingObjC/ZXingObjC.h>

@implementation RNQrGenerator

RCT_EXPORT_MODULE()

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_METHOD(generate:(NSDictionary *)options
                  failureCallback:(RCTResponseErrorBlock)failureCallback
                  successCallback:(RCTResponseSenderBlock)successCallback)
{
  NSString *qrData = [RCTConvert NSString:options[@"value"]];
  NSString *level = [RCTConvert NSString:options[@"correctionLevel"]];
  NSString *fileName = [RCTConvert NSString:options[@"fileName"]];
  level = [self getCorrectionLevel:level];
  float width = [RCTConvert float:options[@"width"]];
  float height = [RCTConvert float:options[@"height"]];
  bool base64 = [RCTConvert BOOL:options[@"base64"]];
  UIColor *backgroundColor = [RCTConvert UIColor:options[@"backgroundColor"]];
  UIColor *color = [RCTConvert UIColor:options[@"color"]];
  NSDictionary *padding = [RCTConvert NSDictionary:options[@"padding"]];
  float top = [RCTConvert float:padding[@"top"]];
  float left = [RCTConvert float:padding[@"left"]];
  float bottom = [RCTConvert float:padding[@"bottom"]];
  float right = [RCTConvert float:padding[@"right"]];
  UIEdgeInsets insets = UIEdgeInsetsMake(top, left, bottom, right);
  width = width - insets.left - insets.right;
  height = height - insets.top - insets.bottom;

  if (qrData) {
    NSData *stringData = [qrData dataUsingEncoding: NSUTF8StringEncoding];
    CIFilter *qrFilter = [CIFilter filterWithName:@"CIQRCodeGenerator"];
    CIFilter *colorFilter = [CIFilter filterWithName:@"CIFalseColor"];
    [qrFilter setValue:stringData forKey:@"inputMessage"];
    [qrFilter setValue:level forKey:@"inputCorrectionLevel"];

    CIColor *background = [[CIColor alloc] initWithColor:backgroundColor];
    CIColor *foreground = [[CIColor alloc] initWithColor:color];

    [colorFilter setValue:qrFilter.outputImage forKey:kCIInputImageKey];
    [colorFilter setValue:background forKey:@"inputColor1"];
    [colorFilter setValue:foreground forKey:@"inputColor0"];
    CIImage *qrImage = colorFilter.outputImage;

    float scaleX = 1;
    float scaleY = 1;
    if (height) {
      scaleY = height / qrImage.extent.size.height;
    }
    if (width) {
      scaleX = width / qrImage.extent.size.width;
    }
    qrImage = [qrImage imageByApplyingTransform:CGAffineTransformMakeScale(scaleX, scaleY)];
    CIContext *context = [CIContext contextWithOptions:nil];
    CGImageRef cgImage = [context createCGImage:qrImage fromRect:[qrImage extent]];
    UIImage *image = [UIImage imageWithCGImage:cgImage];
    CGImageRelease(cgImage);
    if (insets.top != 0 || insets.left != 0 || insets.bottom != 0 || insets.right != 0) {
      CGFloat width = image.size.width + insets.left + insets.right;
      CGFloat height = image.size.height + insets.top + insets.bottom;
      UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), NO, 1.0);
      CGContextRef context = UIGraphicsGetCurrentContext();
      CGContextSetFillColorWithColor(context, [backgroundColor CGColor]);
      CGContextFillRect(context, CGRectMake(0, 0, width, height));
      UIGraphicsPushContext(context);

      CGPoint origin = CGPointMake(insets.left, insets.top);
      [image drawAtPoint:origin];

      UIGraphicsPopContext();
      UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
      UIGraphicsEndImageContext();
      image = newImage;
    }

    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
    NSData *qrImageData = UIImagePNGRepresentation(image);

    NSString *directory = [[self cacheDirectoryPath] stringByAppendingPathComponent:@"QRCode"];
    NSString *path = [self generatePathInDirectory:directory fileName:fileName withExtension:@".png"];
    response[@"uri"] = [self writeImage:qrImageData toPath:path];

    response[@"width"] = @(image.size.width);
    response[@"height"] = @(image.size.height);

    if (base64) {
      response[@"base64"] = [qrImageData base64EncodedStringWithOptions:0];
    }
    successCallback(@[response]);
  } else {
    NSString *errorMessage = @"QRCode value is missing";
    NSDictionary *userInfo = @{NSLocalizedFailureReasonErrorKey: NSLocalizedString(errorMessage, nil)};
    NSError *error = [NSError errorWithDomain:@"com.rnqrcode" code:1 userInfo:userInfo];
    failureCallback(error);
    RCTLogError(@"key 'value' missing in options");
  }
}

RCT_EXPORT_METHOD(detect:(NSDictionary *)options
                  failureCallback:(RCTResponseErrorBlock)failureCallback
                  successCallback:(RCTResponseSenderBlock)successCallback)
{
    NSString *uri = [RCTConvert NSString:options[@"uri"]];
    UIImage *image = [self imageFromPath:uri];
    if (!image) {
        NSString *base64 = [RCTConvert NSString:options[@"base64"]];
        image = [self imageFromBase64:base64];
    }

  if (!image) {
      NSString *errorMessage = @"QRCode uri or base64 are missing";
      NSDictionary *userInfo = @{NSLocalizedFailureReasonErrorKey: NSLocalizedString(errorMessage, nil)};
      NSError *error = [NSError errorWithDomain:@"com.rnqrcode" code:1 userInfo:userInfo];
      failureCallback(error);
      RCTLogWarn(@"key 'uri' or 'base64' are missing in options");
      return;
  }
    ZXDecodeHints *hints = [self createDecodeHints];
    ZXResult *result = [self decodeImage:image hints:hints];

    if (!result) {
        NSArray<NSValue *> *relativeInsets = @[
            [NSValue valueWithUIEdgeInsets:UIEdgeInsetsMake(0.15f, 0.05f, 0.35f, 0.05f)],
            [NSValue valueWithUIEdgeInsets:UIEdgeInsetsMake(0.25f, 0.05f, 0.25f, 0.05f)],
            [NSValue valueWithUIEdgeInsets:UIEdgeInsetsMake(0.35f, 0.05f, 0.15f, 0.05f)]
        ];

        NSArray<NSNumber *> *zoomFactors = @[@1.2f, @1.5f, @2.0f];
        BOOL didDecode = NO;

        for (NSValue *value in relativeInsets) {
            if (didDecode) {
                break;
            }
            UIEdgeInsets insets = [value UIEdgeInsetsValue];
            UIImage *croppedImage = [self cropImage:image withRelativeInsets:insets];
            if (!croppedImage) {
                continue;
            }

            result = [self decodeImage:croppedImage hints:hints];
            if (result) {
                didDecode = YES;
                break;
            }

            for (NSNumber *factor in zoomFactors) {
                if (didDecode) {
                    break;
                }
                CGFloat zoomFactor = [factor floatValue];
                if (zoomFactor <= 1.0f) {
                    continue;
                }
                UIImage *zoomedImage = [self scaleImage:croppedImage byFactor:zoomFactor];
                result = [self decodeImage:zoomedImage hints:hints];
                if (result) {
                    didDecode = YES;
                }
            }
        }
    }

    NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
    if (result) {
      NSString *contents = result.text;
        ZXBarcodeFormat format = result.barcodeFormat;
        response[@"values"] = @[contents];
        response[@"type"] = [self getCodeType:format];
        successCallback(@[response]);
    } else {
        CIImage* ciImage = [[CIImage alloc] initWithImage:image];
        NSMutableDictionary* detectorOptions = [[NSMutableDictionary alloc] init];
        detectorOptions[CIDetectorAccuracy] = CIDetectorAccuracyHigh;
        CIDetector* qrDetector = [CIDetector detectorOfType:CIDetectorTypeQRCode
                                                    context:NULL
                                                    options:detectorOptions];
        if ([[ciImage properties] valueForKey:(NSString*) kCGImagePropertyOrientation] == nil) {
            detectorOptions[CIDetectorImageOrientation] = @1;
        } else {
            id orientation = [[ciImage properties] valueForKey:(NSString*) kCGImagePropertyOrientation];
            detectorOptions[CIDetectorImageOrientation] = orientation;
        }

        NSArray * features = [qrDetector featuresInImage:ciImage
                                      options:detectorOptions];
        NSMutableArray *rawValues = [NSMutableArray array];
        [features enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            [rawValues addObject: [obj messageString]];
        }];
        NSMutableDictionary *finalResponse = [[NSMutableDictionary alloc] init];
        finalResponse[@"values"] = rawValues;
        finalResponse[@"type"] = @"QRCode";
        successCallback(@[finalResponse]);
    }
}

#pragma mark - TurboModule Methods

#ifdef RCT_NEW_ARCH_ENABLED
- (void)generate:(JS::NativeRNQrGenerator::GenerateOptions &)options
   errorCallback:(RCTResponseSenderBlock)errorCallback
 successCallback:(RCTResponseSenderBlock)successCallback
{
    NSMutableDictionary *opts = [[NSMutableDictionary alloc] init];
    opts[@"value"] = options.value();
    if (options.backgroundColor().has_value()) {
        opts[@"backgroundColor"] = @(options.backgroundColor().value());
    }
    if (options.color().has_value()) {
        opts[@"color"] = @(options.color().value());
    }
    if (options.width().has_value()) {
        opts[@"width"] = @(options.width().value());
    }
    if (options.height().has_value()) {
        opts[@"height"] = @(options.height().value());
    }
    if (options.base64().has_value()) {
        opts[@"base64"] = @(options.base64().value());
    }
    
    NSString *fileName = options.fileName();
    if (fileName != nil) {
        opts[@"fileName"] = fileName;
    }
    NSString *correctionLevel = options.correctionLevel();
    if (correctionLevel != nil) {
        opts[@"correctionLevel"] = correctionLevel;
    }
    if (options.padding().has_value()) {
        NSMutableDictionary *paddingDict = [[NSMutableDictionary alloc] init];
        auto padding = options.padding().value();
        if (padding.top().has_value()) {
            paddingDict[@"top"] = @(padding.top().value());
        }
        if (padding.left().has_value()) {
            paddingDict[@"left"] = @(padding.left().value());
        }
        if (padding.bottom().has_value()) {
            paddingDict[@"bottom"] = @(padding.bottom().value());
        }
        if (padding.right().has_value()) {
            paddingDict[@"right"] = @(padding.right().value());
        }
        opts[@"padding"] = paddingDict;
    }

    [self generate:opts failureCallback:^(NSError *error) {
        errorCallback(@[error.localizedDescription ?: @"Unknown error"]);
    } successCallback:successCallback];
}

- (void)detect:(JS::NativeRNQrGenerator::DetectOptions &)options
 errorCallback:(RCTResponseSenderBlock)errorCallback
successCallback:(RCTResponseSenderBlock)successCallback
{
    NSMutableDictionary *opts = [[NSMutableDictionary alloc] init];
    NSString *uri = options.uri();
    if (uri != nil) {
        opts[@"uri"] = uri;
    }
    NSString *base64 = options.base64();
    if (base64 != nil) {
        opts[@"base64"] = base64;
    }

    [self detect:opts failureCallback:^(NSError *error) {
        errorCallback(@[error.localizedDescription ?: @"Unknown error"]);
    } successCallback:successCallback];
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeRNQrGeneratorSpecJSI>(params);
}
#endif

#pragma mark - Helper Methods

- (NSString *)generatePathInDirectory:(NSString *)directory fileName:(NSString *)name withExtension:(NSString *)extension
{
    NSString *fileName = name ? name : [[NSUUID UUID] UUIDString];
    fileName = [fileName stringByAppendingString:extension];
    [self ensureDirExistsWithPath:directory];
    return [directory stringByAppendingPathComponent:fileName];
}

- (UIImage *)imageFromPath:(NSString *)path
{
    NSURL *imageURL = [NSURL URLWithString:path];
    NSData *imageData = [NSData dataWithContentsOfURL:imageURL];
    return [UIImage imageWithData:imageData];
}

- (UIImage *)imageFromBase64:(NSString *)base64String
{
    NSData *imageData = [[NSData alloc]initWithBase64EncodedString:base64String options:NSDataBase64DecodingIgnoreUnknownCharacters];
    return [UIImage imageWithData:imageData];
}

- (ZXResult *)decodeImage:(UIImage *)image hints:(ZXDecodeHints *)hints
{
    if (!image || !image.CGImage) {
        return nil;
    }

    ZXResult *result = nil;
    NSError *error = nil;
    ZXCGImageLuminanceSource *source = [[ZXCGImageLuminanceSource alloc] initWithCGImage:image.CGImage];
    ZXBinaryBitmap *hybridBitmap = [ZXBinaryBitmap binaryBitmapWithBinarizer:[ZXHybridBinarizer binarizerWithSource:source]];
    ZXMultiFormatReader *reader = [ZXMultiFormatReader reader];

    @try {
        result = [reader decode:hybridBitmap hints:hints error:&error];
    } @catch (NSException *exception) {
        result = nil;
    }

    [reader reset];
    return result;
}

- (UIImage *)cropImage:(UIImage *)image withRelativeInsets:(UIEdgeInsets)insets
{
    if (!image || !image.CGImage) {
        return nil;
    }
    if (insets.top + insets.bottom >= 1.0f || insets.left + insets.right >= 1.0f) {
        return nil;
    }

    CGFloat scale = image.scale;
    CGFloat pixelWidth = image.size.width * scale;
    CGFloat pixelHeight = image.size.height * scale;

    CGRect cropRect = CGRectMake(insets.left * pixelWidth,
                                 insets.top * pixelHeight,
                                 pixelWidth * (1.0f - insets.left - insets.right),
                                 pixelHeight * (1.0f - insets.top - insets.bottom));
    cropRect = CGRectIntersection(CGRectMake(0, 0, pixelWidth, pixelHeight), cropRect);
    cropRect = CGRectIntegral(cropRect);

    if (CGRectIsEmpty(cropRect) || cropRect.size.width <= 0 || cropRect.size.height <= 0) {
        return nil;
    }

    CGImageRef croppedRef = CGImageCreateWithImageInRect(image.CGImage, cropRect);
    if (!croppedRef) {
        return nil;
    }

    UIImage *croppedImage = [UIImage imageWithCGImage:croppedRef scale:image.scale orientation:image.imageOrientation];
    CGImageRelease(croppedRef);
    return croppedImage;
}

- (UIImage *)scaleImage:(UIImage *)image byFactor:(CGFloat)factor
{
    if (!image || factor <= 1.0f) {
        return image;
    }

    CGSize newSize = CGSizeMake(image.size.width * factor, image.size.height * factor);
    UIGraphicsBeginImageContextWithOptions(newSize, NO, image.scale);
    [image drawInRect:CGRectMake(0, 0, newSize.width, newSize.height)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();

    return scaledImage ?: image;
}

- (ZXDecodeHints *)createDecodeHints {
    ZXDecodeHints *hints = [ZXDecodeHints hints];
    [hints setTryHarder:TRUE];

    [hints addPossibleFormat:kBarcodeFormatAztec];
    [hints addPossibleFormat:kBarcodeFormatCodabar];
    [hints addPossibleFormat:kBarcodeFormatCode39];
    [hints addPossibleFormat:kBarcodeFormatCode93];
    [hints addPossibleFormat:kBarcodeFormatCode128];
    [hints addPossibleFormat:kBarcodeFormatDataMatrix];
    [hints addPossibleFormat:kBarcodeFormatEan8];
    [hints addPossibleFormat:kBarcodeFormatEan13];
    [hints addPossibleFormat:kBarcodeFormatITF];
    [hints addPossibleFormat:kBarcodeFormatMaxiCode];
    [hints addPossibleFormat:kBarcodeFormatPDF417];
    [hints addPossibleFormat:kBarcodeFormatQRCode];
    [hints addPossibleFormat:kBarcodeFormatRSS14];
    [hints addPossibleFormat:kBarcodeFormatRSSExpanded];
    [hints addPossibleFormat:kBarcodeFormatUPCA];
    [hints addPossibleFormat:kBarcodeFormatUPCE];
    [hints addPossibleFormat:kBarcodeFormatUPCEANExtension];

    return hints;
}

- (NSString*) getCodeType:(ZXBarcodeFormat) format {
    switch (format) {
        case kBarcodeFormatAztec:
            return @"Aztec";
        case kBarcodeFormatCodabar:
            return @"Codabar";
        case kBarcodeFormatCode39:
            return @"Code39";
        case kBarcodeFormatCode93:
            return @"Code93";
        case kBarcodeFormatCode128:
            return @"Code128";
        case kBarcodeFormatDataMatrix:
            return @"DataMatrix";
        case kBarcodeFormatEan8:
            return @"Ean8";
        case kBarcodeFormatEan13:
            return @"Ean13";
        case kBarcodeFormatITF:
            return @"ITF";
        case kBarcodeFormatMaxiCode:
            return @"MaxiCode";
        case kBarcodeFormatPDF417:
            return @"PDF417";
        case kBarcodeFormatQRCode:
            return @"QRCode";
        case kBarcodeFormatRSS14:
            return @"RSS14";
        case kBarcodeFormatRSSExpanded:
            return @"RSSExpanded";
        case kBarcodeFormatUPCA:
            return @"UPCA";
        case kBarcodeFormatUPCE:
            return @"UPCE";
        case kBarcodeFormatUPCEANExtension:
            return @"UPCEANExtension";
        default:
            return @"";
    }
}

- (BOOL)ensureDirExistsWithPath:(NSString *)path
{
    BOOL isDir = NO;
    NSError *error;
    BOOL exists = [[NSFileManager defaultManager] fileExistsAtPath:path isDirectory:&isDir];
    if (!(exists && isDir)) {
        [[NSFileManager defaultManager] createDirectoryAtPath:path withIntermediateDirectories:YES attributes:nil error:&error];
        if (error) {
            return NO;
        }
    }
    return YES;
}

- (NSString *)cacheDirectoryPath
{
    NSArray *array = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    return [array objectAtIndex:0];
}

- (NSString *)getCorrectionLevel:(NSString *)level
{
    NSString *correctionLevel = @"H";
    if ([level isEqualToString:@"L"]) {
        correctionLevel = @"L";
    } else if ([level isEqualToString:@"M"]) {
        correctionLevel = @"M";
    } else if ([level isEqualToString:@"Q"]) {
        correctionLevel = @"Q";
    }
    return correctionLevel;
}

- (NSString *)writeImage:(NSData *)image toPath:(NSString *)path
{
    [image writeToFile:path atomically:YES];
    NSURL *fileURL = [NSURL fileURLWithPath:path];
    return [fileURL absoluteString];
}

@end
