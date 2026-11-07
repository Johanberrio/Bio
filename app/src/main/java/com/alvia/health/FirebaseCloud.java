package com.alvia.health;
import android.content.Context;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import com.google.android.gms.tasks.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
/** SDK-based authenticated storage. Public project config only; never service account keys. */
final class FirebaseCloud {
    private static FirebaseCloud cached;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final String project;
    static synchronized FirebaseCloud create(Context context) throws Exception {
        if(cached!=null)return cached;
        JSONObject config;
        try(InputStream in=context.getAssets().open("firebase-config.json")){config=new JSONObject(new String(MainActivity.readLimited(in,16384),StandardCharsets.UTF_8));}
        FirebaseOptions options=new FirebaseOptions.Builder().setProjectId(config.getString("projectId")).setApplicationId(config.getString("appId")).setApiKey(config.getString("apiKey")).setStorageBucket(config.getString("storageBucket")).build();
        FirebaseApp app;try{app=FirebaseApp.getInstance("bio");}catch(IllegalStateException e){app=FirebaseApp.initializeApp(context,options,"bio");}
        cached=new FirebaseCloud(app);return cached;
    }
    private FirebaseCloud(FirebaseApp app){project=app.getOptions().getProjectId();auth=FirebaseAuth.getInstance(app);db=FirebaseFirestore.getInstance(app);db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());storage=FirebaseStorage.getInstance(app);storage.setMaxUploadRetryTimeMillis(60000);storage.setMaxDownloadRetryTimeMillis(60000);}
    boolean signedIn(){return auth.getCurrentUser()!=null;}
    boolean account(){return signedIn()&&!auth.getCurrentUser().isAnonymous();}
    boolean anonymous(){return signedIn()&&auth.getCurrentUser().isAnonymous();}
    void login(Vault vault,String email,String password,boolean register)throws Exception{
        AuthCredential credential=EmailAuthProvider.getCredential(email,password);
        if(register){
            if(anonymous()){bind(vault);waitFor(auth.getCurrentUser().linkWithCredential(credential));}
            else if(account())throw new IOException("Ya existe una cuenta en este dispositivo. Inicia sesión con ella.");
            else {if(!vault.list("cloudBinding").isEmpty())throw new IOException("Inicia sesión con la cuenta que guardó estos documentos.");waitFor(auth.createUserWithEmailAndPassword(email,password));}
        }else{
            if(anonymous()&&(!vault.list("cloudBinding").isEmpty()||!vault.list("documents").isEmpty()))throw new IOException("Para conservar tus documentos actuales, usa Crear cuenta y vincúlalos a un correo nuevo.");
            if(account()){
                if(!email.equalsIgnoreCase(email()))throw new IOException("Usa la cuenta de este dispositivo: "+email());
                waitFor(auth.getCurrentUser().reauthenticate(credential));
            }else waitFor(auth.signInWithEmailAndPassword(email,password));
        }
        try{bind(vault);}catch(Exception e){auth.signOut();throw e;}
    }
    void resetPassword(String email)throws Exception{waitFor(auth.sendPasswordResetEmail(email));}
    void confirmDeletionPassword(String password)throws Exception{
        if(!account())throw new IOException("Inicia sesión con tu cuenta de Bio.");
        FirebaseUser user=auth.getCurrentUser();String owner=owner();
        waitFor(user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(),password)));
        waitFor(user.getIdToken(true));
        if(!account()||!owner.equals(owner()))throw new IOException("La sesión cambió. Vuelve a intentarlo.");
    }
    void syncProfile(Vault vault)throws Exception{
        if(!account())return;bind(vault);DocumentReference ref=db.collection("users").document(uid()).collection("profile").document("main");JSONObject local=HealthProfile.current(vault);
        if(HealthProfile.complete(vault)&&"pending".equals(local.optString("cloudState"))){
            if(!owner().equals(local.optString("owner")))throw new IOException("El perfil pertenece a otra cuenta.");
            JSONObject saved=new JSONObject(local.toString()).put("cloudState","synced");Map<String,Object> data=new HashMap<>();data.put("payload",saved.toString());data.put("updatedAt",FieldValue.serverTimestamp());waitFor(ref.set(data));if(HealthProfile.current(vault).optLong("updatedAt")==local.optLong("updatedAt"))vault.put("profile",saved);
        }else if(!HealthProfile.complete(vault)){DocumentSnapshot remote=waitFor(ref.get(Source.SERVER));if(remote.exists()){JSONObject p=new JSONObject(remote.getString("payload"));HealthProfile.validate(p);if(!owner().equals(p.optString("owner")))throw new IOException("Perfil inválido");vault.put("profile",p);}}
    }
    synchronized void ensureSession() throws Exception {if(!signedIn())waitFor(auth.signInAnonymously());}
    String idToken() throws Exception {ensureSession();return waitFor(auth.getCurrentUser().getIdToken(false)).getToken();}
    String email(){return signedIn()?auth.getCurrentUser().getEmail():"";}
    String owner() throws IOException {if(!signedIn())throw new IOException("Inicia sesión en Firebase");return project+":"+auth.getCurrentUser().getUid();}
    void signIn(String email,String password,boolean register) throws Exception {if(register)waitFor(auth.createUserWithEmailAndPassword(email,password));else waitFor(auth.signInWithEmailAndPassword(email,password));}
    void signOut(){auth.signOut();}
    private static <T>T waitFor(Task<T> task)throws Exception{return Tasks.await(task,90,TimeUnit.SECONDS);}
    private String uid()throws Exception{owner();return auth.getCurrentUser().getUid();}
    private void bind(Vault vault)throws Exception{String owner=owner();List<JSONObject> bindings=vault.list("cloudBinding");if(!bindings.isEmpty()&&!owner.equals(bindings.get(0).optString("owner")))throw new IOException("Estos datos locales pertenecen a otra cuenta. No se sincronizaron.");if(bindings.isEmpty())vault.put("cloudBinding",new JSONObject().put("id","owner").put("owner",owner));}
    void upload(Vault vault,JSONObject source)throws Exception{
        ensureSession();bind(vault);String id=source.getString("id");JSONObject doc=new JSONObject(source.toString());
        StorageReference file=storage.getReference().child("users/"+uid()+"/documents/"+id);
        if(!owner().equals(doc.optString("cloudOwner")))waitFor(file.putBytes(vault.readDocument(id),new StorageMetadata.Builder().setContentType(doc.getString("mime")).build()));
        doc.put("cloudOwner",owner()).put("cloudState","pending");
        JSONObject checkpoint=new JSONObject(doc.toString());for(JSONObject stored:vault.list("documents"))if(id.equals(stored.optString("id"))&&DocumentPipeline.pending(stored))checkpoint.put("importPending",true);
        vault.put("documents",checkpoint);
        doc.put("cloudState","synced");
        Map<String,Object> metadata=new HashMap<>();metadata.put("payload",doc.toString());metadata.put("updatedAt",FieldValue.serverTimestamp());
        waitFor(db.collection("users").document(uid()).collection("documents").document(id).set(metadata));vault.put("documents",doc);
    }
    void sync(Vault vault)throws Exception{
        ensureSession();bind(vault);
        // Explicit synchronization sends pending local files; remote originals are restored if absent.
        for(JSONObject doc:vault.list("documents"))if(!DocumentPipeline.pending(doc)&&!"synced".equals(doc.optString("cloudState")))upload(vault,doc);
        Set<String> local=new HashSet<>();for(JSONObject doc:vault.list("documents"))local.add(doc.getString("id"));
        QuerySnapshot snapshot=waitFor(db.collection("users").document(uid()).collection("documents").get(Source.SERVER));
        for(DocumentSnapshot remote:snapshot.getDocuments()){
            if(local.contains(remote.getId()))continue;
            JSONObject doc=new JSONObject(remote.getString("payload"));if(!remote.getId().equals(doc.optString("id")))throw new IOException("Documento remoto inválido");
            byte[] bytes=waitFor(storage.getReference().child("users/"+uid()+"/documents/"+remote.getId()).getBytes(10*1024*1024));
            vault.storeDocument(remote.getId(),bytes);try{doc.put("cloudOwner",owner()).put("cloudState","synced");vault.put("documents",doc);}catch(Exception e){vault.deleteDocument(remote.getId());throw e;}
        }
    }
    void delete(Vault vault,JSONObject doc)throws Exception{bind(vault);if(!owner().equals(doc.optString("cloudOwner")))throw new IOException("La cuenta no corresponde al documento");String id=doc.getString("id");try{waitFor(storage.getReference().child("users/"+uid()+"/documents/"+id).delete());}catch(Exception e){Throwable cause=e.getCause();if(!(cause instanceof StorageException)||((StorageException)cause).getErrorCode()!=StorageException.ERROR_OBJECT_NOT_FOUND)throw e;}waitFor(db.collection("users").document(uid()).collection("documents").document(id).delete());}
}
