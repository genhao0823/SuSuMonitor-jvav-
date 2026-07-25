package com.susumonitor.server.ssh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import net.schmizz.sshj.common.Buffer;
import org.junit.jupiter.api.Test;

/**
 * 验证 sshj 主机密钥 verifier 的算法选择和指纹校验行为，不建立网络连接。
 */
class SshConnectionTesterTests {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 2223;

    /** 验证空算法列表会让 sshj 使用默认主机密钥算法协商，不改变当前行为。 */
    @Test
    void findExistingAlgorithmsShouldKeepDefaultNegotiation() {
        SshConnectionTester.CapturingHostKeyVerifier verifier =
                new SshConnectionTester.CapturingHostKeyVerifier("SHA256:unused", null);

        assertEquals(List.of(), verifier.findExistingAlgorithms(HOST, PORT));
    }

    /** 验证正确的 RSA 公钥指纹和算法可以通过主机身份校验。 */
    @Test
    void verifyShouldAcceptMatchingRsaFingerprintAndAlgorithm() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String fingerprint = sha256Fingerprint(keyPair.getPublic());
        SshConnectionTester.CapturingHostKeyVerifier verifier =
                new SshConnectionTester.CapturingHostKeyVerifier(fingerprint, "ssh-rsa");

        assertTrue(verifier.verify(HOST, PORT, keyPair.getPublic()));
        assertTrue(verifier.matched());
        assertEquals("ssh-rsa", verifier.observedAlgorithm());
    }

    /** 验证错误指纹不能通过，即使公钥算法登记正确。 */
    @Test
    void verifyShouldRejectWrongFingerprint() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        SshConnectionTester.CapturingHostKeyVerifier verifier =
                new SshConnectionTester.CapturingHostKeyVerifier(
                        "SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "ssh-rsa");

        assertFalse(verifier.verify(HOST, PORT, keyPair.getPublic()));
        assertFalse(verifier.matched());
        assertEquals("ssh-rsa", verifier.observedAlgorithm());
    }

    /** 验证指纹正确但算法登记不匹配时仍拒绝主机身份。 */
    @Test
    void verifyShouldRejectAlgorithmMismatch() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        SshConnectionTester.CapturingHostKeyVerifier verifier =
                new SshConnectionTester.CapturingHostKeyVerifier(sha256Fingerprint(keyPair.getPublic()), "ED25519");

        assertFalse(verifier.verify(HOST, PORT, keyPair.getPublic()));
        assertFalse(verifier.matched());
    }

    /** 使用 SSH 公钥 blob 计算与 OpenSSH 和 sshj 一致的无填充 SHA-256 指纹。 */
    private String sha256Fingerprint(PublicKey key) throws Exception {
        byte[] keyBlob = new Buffer.PlainBuffer().putPublicKey(key).getCompactData();
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(keyBlob);
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest);
    }

    /** 生成测试专用 RSA 公钥，不写入磁盘或测试资源。 */
    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
