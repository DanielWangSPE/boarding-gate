package com.boardinggate.auth.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 与 sm-crypto@0.4.0 同一组 key/iv 下，Hutool 能还原 Node 产出的 123456 密文。
 */
class Sm4CompatNodeTest {

  @Test
  void node_sm_crypto_cipher_decrypts_to_plain_123456() {
    // 与 node 中 keyHex='0001..0e0f' / ivHex='0f0e0d..0100' 一致
    byte[] key = new byte[16];
    for (int i = 0; i < 16; i++) {
      key[i] = (byte) i;
    }
    byte[] iv = new byte[] {
      0x0f, 0x0e, 0x0d, 0x0c, 0x0b, 0x0a, 0x09, 0x08,
      0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00
    };
    // node: sm4.encrypt('123456', keyHex, { mode:'cbc', iv:ivHex, padding:'pkcs#7', output:'string' })
    String nodeCipherHex = "8b4902d83f44fe11c3de3b03955ac079";
    String plain = Sm4Util.decryptFromHex(nodeCipherHex, key, iv);
    assertEquals("123456", plain);
  }
}
