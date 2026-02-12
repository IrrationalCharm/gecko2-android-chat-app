package com.dominik.Gecko2Chat.utils;

import android.content.Context;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

public class CryptoManager {

    private static final String KEYSET_NAME = "auth_keyset";
    private static final String PREF_FILE = "secure_keys";
    private static final String MASTER_KEY_URI = "android-keystore://auth_master_key";

    private final Aead aead;

    public CryptoManager(Context context) throws GeneralSecurityException, IOException {
        AeadConfig.register();

        KeysetHandle keysetHandle = new AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, PREF_FILE)
                .withKeyTemplate(com.google.crypto.tink.aead.AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .getKeysetHandle();

        aead = keysetHandle.getPrimitive(Aead.class);
    }

    public String encrypt(String plaintext) throws Exception {
        byte[] encrypted = aead.encrypt(
                plaintext.getBytes(StandardCharsets.UTF_8),
                null
        );
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String ciphertext) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        byte[] decrypted = aead.decrypt(decoded, null);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
