package com.alvia.health;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
/** Per-use authenticated key. No password or biometric templates are stored. */
final class BiometricAccess {
    private static final String ALIAS="bio.biometric.v1";
    private final SharedPreferences prefs;
    BiometricAccess(Context context){prefs=context.getSharedPreferences("biometric_access",Context.MODE_PRIVATE);}
    boolean enabled(String owner){return owner.equals(prefs.getString("owner",""))&&prefs.contains("ciphertext");}
    void clear(){prefs.edit().clear().commit();try{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);k.deleteEntry(ALIAS);}catch(Exception ignored){}}
    Cipher prepare(boolean enroll)throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(enroll){clear();KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(true).build());generator.generateKey();}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        if(enroll)cipher.init(Cipher.ENCRYPT_MODE,store.getKey(ALIAS,null));
        else cipher.init(Cipher.DECRYPT_MODE,store.getKey(ALIAS,null),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
        return cipher;
    }
    void complete(Cipher cipher,boolean enroll,String owner)throws Exception{
        byte[] expected=("Bio access:"+owner).getBytes(StandardCharsets.UTF_8);
        if(enroll){byte[] encrypted=cipher.doFinal(expected);if(!prefs.edit().putString("owner",owner).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString("ciphertext",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit())throw new java.io.IOException();}
        else if(!MessageDigest.isEqual(expected,cipher.doFinal(Base64.decode(prefs.getString("ciphertext",""),Base64.NO_WRAP))))throw new GeneralSecurityException("Invalid biometric binding");
    }
}
