// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "WerkLogCore", platforms: [.iOS(.v16), .macOS(.v13)], products: [.library(name: "WerkLogCore", targets: ["WerkLogCore"])], targets: [.target(name: "WerkLogCore"), .testTarget(name: "WerkLogCoreTests", dependencies: ["WerkLogCore"], resources: [.copy("Fixtures")])])
