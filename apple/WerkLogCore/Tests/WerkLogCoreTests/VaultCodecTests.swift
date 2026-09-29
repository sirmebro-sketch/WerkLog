import XCTest
import Foundation
@testable import WerkLogCore
final class VaultCodecTests: XCTestCase {
    func testSharedAndroidVectorAndTampering() throws {
        let url = Bundle.module.url(forResource: "interop", withExtension: "json", subdirectory: "Fixtures")!
        let vector = try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as! [String: String]
        func bytes(_ name: String) -> Data { Data(base64Encoded: vector[name]!)! }
        let opened = try VaultCodec.openMetadata(bytes("metadata"), password: vector["password"]!)
        XCTAssertEqual(opened, bytes("plaintext"))
        XCTAssertThrowsError(try VaultCodec.openMetadata(bytes("metadata"), password: "falsch"))
        let key = try VaultCodec.derive(password: vector["password"]!, salt: bytes("salt"))
        XCTAssertEqual(try VaultCodec.openImage(bytes("encryptedImage"), key: key, reference: vector["reference"]!), bytes("image"))
        var changed = bytes("encryptedImage"); changed[changed.count - 1] ^= 1
        XCTAssertThrowsError(try VaultCodec.openImage(changed, key: key, reference: vector["reference"]!))
        let sealed = try VaultCodec.sealMetadata(opened, key: key, salt: bytes("salt"))
        XCTAssertEqual(try VaultCodec.openMetadata(sealed, password: vector["password"]!), opened)
    }
}
