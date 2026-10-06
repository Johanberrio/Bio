package com.alvia.health;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.util.*;

/** Local encrypted vault. A failed read never silently replaces the user's records. */
final class Vault {
    private final Context context;
    private JSONObject data;
    private static final Object FILE_LOCK=new Object();
    private void refresh() throws Exception {File file=new File(context.getFilesDir(),"vault.enc");if(file.exists()||new File(file+".bak").exists())data=new JSONObject(new String(decrypt(new AtomicFile(file).readFully()),java.nio.charset.StandardCharsets.UTF_8));}
    Vault(Context context) throws Exception {
        this.context = context.getApplicationContext();
        synchronized(FILE_LOCK){
        File file = new File(context.getFilesDir(), "vault.enc");
        AtomicFile atomic = new AtomicFile(file);
        if (file.exists() || new File(file + ".bak").exists()) data = new JSONObject(new String(decrypt(atomic.readFully()), java.nio.charset.StandardCharsets.UTF_8));
        else { data = new JSONObject(); for (String key : new String[]{"appointments","vitals","records","documents"}) data.put(key, new JSONArray()); }
        }
    }
    private javax.crypto.SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        String alias = "alvia.vault.v1";
        if (!ks.containsAlias(alias)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            generator.generateKey();
        }
        return (javax.crypto.SecretKey) ks.getKey(alias, null);
    }
    byte[] encrypt(byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(cipher.getIV()); out.write(cipher.doFinal(bytes)); return out.toByteArray();
    }
    byte[] decrypt(byte[] bytes) throws Exception {
        if (bytes.length < 28) throw new IOException("Archivo cifrado incompleto");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, bytes, 0, 12));
        return cipher.doFinal(bytes, 12, bytes.length - 12);
    }
    synchronized List<JSONObject> list(String name) {
        synchronized(FILE_LOCK){try{refresh();}catch(Exception e){throw new IllegalStateException("No se pudieron leer los datos guardados",e);}
        List<JSONObject> out = new ArrayList<>(); JSONArray arr = data.optJSONArray(name);
        if (arr != null) for (int i=0;i<arr.length();i++) out.add(arr.optJSONObject(i));
        return out;}
    }
    synchronized void put(String collection, JSONObject object) throws Exception {
        synchronized(FILE_LOCK){refresh();JSONObject next = new JSONObject(data.toString()); JSONArray arr = next.optJSONArray(collection);
        if (arr == null) { arr = new JSONArray(); next.put(collection, arr); }
        boolean replaced = false;
        for (int i=0;i<arr.length();i++) if (arr.getJSONObject(i).optString("id").equals(object.optString("id"))) { arr.put(i,object); replaced=true; break; }
        if (!replaced) arr.put(object);
        save(next);}
    }
    synchronized void remove(String collection, String id) throws Exception {
        synchronized(FILE_LOCK){refresh();JSONObject next = new JSONObject(data.toString()); JSONArray arr = next.getJSONArray(collection);
        for (int i=arr.length()-1;i>=0;i--) if (arr.getJSONObject(i).optString("id").equals(id)) arr.remove(i);
        save(next);}
    }
    synchronized void putAll(String collection,List<JSONObject> objects)throws Exception {
        synchronized(FILE_LOCK){refresh();JSONObject next=new JSONObject(data.toString());LinkedHashMap<String,JSONObject> items=new LinkedHashMap<>();JSONArray old=next.optJSONArray(collection);
        if(old!=null)for(int i=0;i<old.length();i++){JSONObject o=old.getJSONObject(i);items.put(o.getString("id"),o);}
        for(JSONObject o:objects)items.put(o.getString("id"),o);next.put(collection,new JSONArray(items.values()));save(next);}
    }
    private void save(JSONObject next) throws Exception {
        byte[] bytes = encrypt(next.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        AtomicFile file = new AtomicFile(new File(context.getFilesDir(), "vault.enc")); FileOutputStream out = null;
        try { out=file.startWrite(); out.write(bytes); file.finishWrite(out); data=next; }
        catch(Exception e) { if(out!=null) file.failWrite(out); throw e; }
    }
    void storeDocument(String id, byte[] bytes) throws Exception {
        try (FileOutputStream out = context.openFileOutput(id+".enc", Context.MODE_PRIVATE)) { out.write(encrypt(bytes)); }
    }
    byte[] readDocument(String id) throws Exception {
        if (!id.matches("[a-f0-9-]{36}")) throw new IOException("ID inválido");
        try (FileInputStream in = context.openFileInput(id+".enc")) { return decrypt(MainActivity.readLimited(in, 11*1024*1024)); }
    }
    void deleteDocument(String id) { context.deleteFile(id+".enc"); }
    static JSONObject item() throws JSONException { return new JSONObject().put("id",UUID.randomUUID().toString()).put("created",System.currentTimeMillis()); }
}
