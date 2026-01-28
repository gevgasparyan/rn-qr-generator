#ifdef RCT_NEW_ARCH_ENABLED
#import <RNQrGeneratorSpec/RNQrGeneratorSpec.h>

@interface RNQrGenerator : NSObject <NativeRNQrGeneratorSpec>
#else
#if __has_include(<React/RCTBridgeModule.h>)
#import <React/RCTBridgeModule.h>
#elif __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import "React/RCTBridgeModule.h"
#endif

@interface RNQrGenerator : NSObject <RCTBridgeModule>
#endif

@end
