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
    UIImage *image = [UIImage imageWithCIImage:qrImage];
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

- (NSString *)generatePathInDirectory:(NSString *)directory withExtension:(NSString *)extension
{
    NSString *fileName = [[[NSUUID UUID] UUIDString] stringByAppendingString:extension];
    [self ensureDirExistsWithPath:directory];
    return [directory stringByAppendingPathComponent:fileName];
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
