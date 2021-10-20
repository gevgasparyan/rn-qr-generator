require 'json'
  
package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = "RNQrGenerator"
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']

  s.authors      = package['author']
  s.homepage     = package['homepage']

  s.platform     = :ios, "7.0"
  s.tvos.deployment_target = '13.0'
  s.source       = { :git => "https://github.com/gevgasparyan/rn-qr-generator.git", :tag => "master" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
  s.dependency "ZXingObjC"
end

  