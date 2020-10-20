#import "RNQrGenerator.h"

#if __has_include(<React/RCTConvert.h>)
#import <React/RCTConvert.h>
#elif __has_include("RCTConvert.h")
#import "RCTConvert.h"
#else
#import "React/RCTConvert.h"   // Required when used as a Pod in a Swift project
#endif

@implementation RNQrGenerator

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(generate:(NSDictionary *)options
                  failureCallback:(RCTResponseErrorBlock)failureCallback
                  successCallback:(RCTResponseSenderBlock)successCallback)
{

  NSString *qrData = [RCTConvert NSString:options[@"value"]];
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
    [qrFilter setValue:@"H" forKey:@"inputCorrectionLevel"];

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
    if (!UIEdgeInsetsEqualToEdgeInsets(insets, UIEdgeInsetsZero)) {
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
    NSData *qrData = UIImagePNGRepresentation(image);

    NSString *directory = [[self cacheDirectoryPath] stringByAppendingPathComponent:@"QRCode"];
    NSString *path = [self generatePathInDirectory:directory withExtension:@".png"];
    response[@"uri"] = [self writeImage:qrData toPath:path];

    response[@"width"] = @(image.size.width);
    response[@"height"] = @(image.size.height);

    if (base64) {
      response[@"base64"] = [qrData base64EncodedStringWithOptions:0];
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
    CIImage* ciImage = [[CIImage alloc] initWithImage:image];

    NSMutableDictionary* detectorOptions;
    detectorOptions[CIDetectorAccuracy] = CIDetectorAccuracyHigh;
//    detectorOptions[CIDetectorAccuracy] = CIDetectorAccuracyLow;  // Fast but superficial

  if (@available(iOS 8.0, *)) {
      CIDetector* qrDetector = [CIDetector detectorOfType:CIDetectorTypeQRCode
                                                  context:NULL
                                                  options:options];
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
      NSMutableDictionary *response = [[NSMutableDictionary alloc] init];
      response[@"values"] = rawValues;
      successCallback(@[response]);

  } else {
      NSString *errorMessage = @"QRCode iOS 8+ required";
      NSDictionary *userInfo = @{NSLocalizedFailureReasonErrorKey: NSLocalizedString(errorMessage, nil)};
      NSError *error = [NSError errorWithDomain:@"com.rnqrcode" code:1 userInfo:userInfo];
      failureCallback(error);
      RCTLogWarn(@"Required iOS 8 or later");
  }
}

- (NSString *)generatePathInDirectory:(NSString *)directory withExtension:(NSString *)extension
{
    NSString *fileName = [[[NSUUID UUID] UUIDString] stringByAppendingString:extension];
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

- (NSString *)writeImage:(NSData *)image toPath:(NSString *)path
{
    [image writeToFile:path atomically:YES];
    NSURL *fileURL = [NSURL fileURLWithPath:path];
    return [fileURL absoluteString];
}
@end
