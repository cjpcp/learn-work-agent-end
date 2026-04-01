package com.example.learnworkagent.common.util;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * RSA加密工具类
 */
@Slf4j
@Component
public class RsaUtil {

    // RSA私钥（用于后端解密）- 生产环境应从配置文件或密钥管理服务获取
    private static final String PRIVATE_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDWd4o2pWIRD6Kv
            3M39IbcuRLvieRc3zrNlk6Pvjahx5xUWw1FCkG4LDQodxbZ87vRz59OzbQkUHV7z
            HIkEArQNB4eRUqme8Ulre3Z07eGWL7W/YHRg+KfF5uBDOm92Q0r+EifKcjUDqEKX
            nYHaAtDLdKk7iEPoOmzgVGld+HE2dTaeDsP5aW5URbmTAoaD+obG+O+SdLFcQtjg
            42WiUsIxBddTZb9864oWenlV0lTErWSRjI5MTgUgaZbJPXB/RcDeXdGgBhNADfMz
            eLn13gbKnznaLIf4yg5m0saU72JGNIyPVq7ZJNSizHTg/93I8ZDbmMbJCxbwpyxW
            PhI7WGYnAgMBAAECggEAJlXOyE7OlTMgxuOmlFkq96PEm8o+SrTxXzZBRugg1F9U
            ZzG5fDb53JLTnJfDM1i5LGCjeD6EsWpAlx8iJKvhCDUw19qZRDZoA8TZWzRMLv7P
            M2qn0s+PmbHXo1y2IMMNjBWttCOu8zznzlcSp2f6b2umdmQ4kzGiVYTxIqFuhyef
            l89RCixWNQU2ZfCtCzvYBJmI8QuQoomFBI8HXSxm9yT6L8NhQdoxyfg8jkyWapwK
            0BpwZPiXRqHeOgTErp+JHYChv5l/CuyJpGd8H16NouXa8JEQ+96kB7RlOp0TWvnf
            u8r8nglX2qCDLGVa2PX2mHfASnhyW5AojqxVkxC+cQKBgQD4R7CtP39+gsT8GmLR
            T7yjiA2K8vX41gmEjditr7ir5eiiek/8nBkY3Kv1U6nhPSBweeKk3Y0J3cKwLpBi
            u3eo5FZcIu2v2uCQJ9xRzsvhAZRpPzjiUJatSeTFucAsIBwwp84n5NupPyfhnEFy
            EwInBQuDEfdsgf09qNv0N9/2EQKBgQDdIrKODmEdvDVp6XNbY58/1tpmuBzQc47R
            cYl9sYGTW8zgpy5/qdcdpPl6hflGvKeo7MsFA27MAjyAF9GcIVqmCzuxlH6ojTXw
            5Wif/aqCJjEjWrbGE+bZ9wQTmyiczakypE7QjjXJvRSLeAsyZMPyMWj7gdN/VmJt
            A7/tiMeAtwKBgGBx6M3D5tDMF50e2tgYM10LEsexDZ+19UiKmWsO4ZvU2YOUI9Ir
            CQzBXAMWlt0qE5ndnw8QCSOWA3TRAcF4tUjkOi/cWZyAV89nzIvy2vvy0yX3Ky+u
            wnlaRQYR2/bIGmtEJC8XFcUvBVz8h+e6PpHNweUOa6C49hinqZm6wsMhAoGAfJ3F
            CZDI4bziTOEmBOZLI+qsAR7X/hBg916IILEbWDNvbVpJNeA8PZRuksFVDKvbv7JR
            Zm3czlKkTXsewGF7d/70kMoh5lJNh4eAQkjtAZMMNeQ1A8LLYSF2xqW1aUfshYFa
            eOTrItjO7xmjFa77TUzS4Ij06tl/dfea/P0LkFkCgYEAqMsFrzvDRNrhARFw1dpL
            Kh4iZFzd1ZpXdKbkMZHYEAZYFqFhPRv3vKfokUWEIKk4YpW70bVTfBFt6tzNJ3QS
            Ze5D1/8G3SPxM5g+tYuqX+RBggKwpRzHaRofaU2v0hQEgeVCWDJOEztrajr0Ozx/
            2vfoPkZN0IMq3aKc8T5mbKY=
            -----END PRIVATE KEY-----
            """;

    private static PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            privateKey = loadPrivateKey();
            log.info("RSA私钥加载成功");
        } catch (Exception e) {
            log.error("RSA私钥加载失败", e);
        }
    }

    /**
     * 加载私钥
     */
    private PrivateKey loadPrivateKey() throws Exception {
        String privateKeyPEM = RsaUtil.PRIVATE_KEY
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA", "BC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * RSA解密
     *
     * @param encryptedData Base64编码的加密数据
     * @return 解密后的明文
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }

        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("RSA解密失败", e);
            // 如果解密失败，可能是明文传输（兼容旧版本），直接返回原值
            return encryptedData;
        }
    }

}
