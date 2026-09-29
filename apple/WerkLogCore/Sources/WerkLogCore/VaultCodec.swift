import Foundation
import CryptoKit
import CommonCrypto

/// Android-compatible cryptographic envelopes. No network, keychain or UI side effects.
public enum VaultCodec {
    public enum Failure: Error { case invalidEnvelope, keyDerivation, invalidReference }
    public static let metadataLimit = 32 * 1024 * 1024
    public static func derive(password: String, salt: Data) throws -> SymmetricKey {
        guard salt.count == 16 else { throw Failure.invalidEnvelope }
        let passwordBytes = Array(password.utf8)
        var output = [UInt8](repeating: 0, count: 32)
        let status = passwordBytes.withUnsafeBytes { p in salt.withUnsafeBytes { s in
            CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2), p.bindMemory(to: Int8.self).baseAddress, passwordBytes.count,
                s.bindMemory(to: UInt8.self).baseAddress, salt.count, CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256), 310_000, &output, 32)
        } }
        guard status == kCCSuccess else { throw Failure.keyDerivation }
        defer { output.withUnsafeMutableBytes { $0.initializeMemory(as: UInt8.self, repeating: 0) } }
        return SymmetricKey(data: output)
    }
    public static func openMetadata(_ bytes: Data, password: String) throws -> Data {
        guard bytes.count >= 52, bytes.count <= metadataLimit, bytes.prefix(8) == Data("WRKLOG01".utf8) else { throw Failure.invalidEnvelope }
        let key = try derive(password: password, salt: bytes.subdata(in: 8..<24))
        let box = try AES.GCM.SealedBox(combined: bytes.subdata(in: 24..<bytes.count))
        return try AES.GCM.open(box, using: key, authenticating: Data("WRKLOG01".utf8))
    }
    public static func sealMetadata(_ bytes: Data, key: SymmetricKey, salt: Data) throws -> Data {
        guard bytes.count < metadataLimit - 64, salt.count == 16 else { throw Failure.invalidEnvelope }
        let box = try AES.GCM.seal(bytes, using: key, authenticating: Data("WRKLOG01".utf8))
        guard let combined = box.combined else { throw Failure.invalidEnvelope }
        return Data("WRKLOG01".utf8) + salt + combined
    }
    public static func openImage(_ bytes: Data, key: SymmetricKey, reference: String) throws -> Data {
        guard reference.range(of: "^img:[a-f0-9-]{36}$", options: .regularExpression) != nil else { throw Failure.invalidReference }
        guard (28...(512 * 1024 + 28)).contains(bytes.count) else { throw Failure.invalidEnvelope }
        return try AES.GCM.open(AES.GCM.SealedBox(combined: bytes), using: key, authenticating: Data(("WRKIMG01:" + reference).utf8))
    }
}
