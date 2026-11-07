package com.alvia.health;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Password-verified request; server performs durable erasure independently of the phone. */
public class AccountDeletionActivity extends Activity {
    static final String PRIVACY="https://us-east1-bio-project-15d15.cloudfunctions.net/bio_api/privacy";
    private FirebaseCloud cloud;
    private String owner;
    private EditText password;
    private CheckBox confirm;
    private TextView status, submit;
    private LinearLayout layout;
    private int ink,purple,surface;
    private boolean busy;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state){
        boolean dark=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("darkMode",false);
        setTheme(dark?R.style.Theme_Bio_Dark:R.style.Theme_Bio);super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if(!AuthSession.unlocked()){finish();return;}
        try{cloud=FirebaseCloud.create(this);if(!cloud.account()){finish();return;}owner=cloud.owner();}catch(Exception e){finish();return;}
        ink=dark?0xfff4effa:0xff282335;purple=dark?0xffc5a6ff:0xff7550b8;surface=dark?0xff2e253f:0xfff2edf9;
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(dark?0xff141119:Color.WHITE);
        layout=new AdaptiveColumn(this);layout.setPadding(dp(24),dp(24),dp(24),dp(24));scroll.addView(layout);setContentView(scroll);
        if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});else scroll.setFitsSystemWindows(true);
        button("‹ Volver",()->{if(!busy)finish();});label("CUENTA Y PRIVACIDAD",12,purple);label("Eliminar cuenta",30,ink);
        label("Se eliminarán tu cuenta, perfil, documentos y análisis de Bio, conexiones de correo y contadores de uso. Es irreversible. La limpieza del servidor normalmente comienza durante las siguientes 24 horas y reintenta si hay fallos.",16,ink);
        label("Este teléfono se limpiará al aceptar la solicitud. Para retirar copias locales de otros dispositivos, borra los datos de Bio en sus ajustes de Android. Los originales en Gmail, Health Connect o fuera de Bio no se eliminan.",15,ink);
        label("Eliminar tu cuenta no cancela la suscripción. Gestiona su renovación por separado en Google Play.",16,ink);
        button("Gestionar suscripción en Google Play",()->open("https://play.google.com/store/account/subscriptions?sku=bio_premium&package=com.alvia.health"));
        button("Política de privacidad y conservación",()->open(PRIVACY));
        label("Cuenta: "+cloud.email(),15,ink);
        label("Contraseña de Bio",14,ink);
        password=new EditText(this);password.setHint("Confirma tu contraseña");password.setTextColor(ink);password.setHintTextColor(purple);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setSingleLine(true);password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());password.setSaveEnabled(false);password.setMinHeight(dp(56));layout.addView(password,new LinearLayout.LayoutParams(-1,-2));
        confirm=new CheckBox(this);confirm.setText("Entiendo que se eliminarán definitivamente mi cuenta y sus datos.");confirm.setTextColor(ink);layout.addView(confirm,new LinearLayout.LayoutParams(-1,-2));
        status=label("",14,purple);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        submit=button("Solicitar eliminación definitiva",this::request);
    }
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(10),0,dp(10));layout.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    TextView button(String value,Runnable action){TextView v=label(value,16,purple);BioButton.style(v,surface,purple);v.setOnClickListener(w->{if(!busy)action.run();});return v;}
    void open(String url){try{AuthSession.beginExternalFlow();startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){status.setText("No hay navegador disponible.");}}
    boolean sameAccount(){try{return cloud.account()&&owner.equals(cloud.owner());}catch(Exception e){return false;}}
    void request(){
        if(busy)return;
        if(!AuthSession.unlocked()||!sameAccount()){status.setText("Vuelve a iniciar sesión para continuar.");return;}
        if(!confirm.isChecked()||password.length()==0){status.setText("Introduce tu contraseña y confirma la eliminación.");return;}
        String secret=password.getText().toString();password.setText("");busy=true;submit.setEnabled(false);status.setText("Verificando tu contraseña y registrando la solicitud…");
        worker.execute(()->{
            String error="No se pudo verificar la contraseña. Vuelve a intentarlo.";
            try{
                cloud.confirmDeletionPassword(secret);
                error="No se pudo registrar la eliminación. Comprueba tu conexión y vuelve a intentarlo.";
                JSONObject response=new MailApi(this,cloud).post("/account/deletion-request",new JSONObject().put("confirm",true));
                if(!response.optBoolean("requested")||!sameAccount())throw new IllegalStateException();
                // Persist first: if the process stops now, next launch completes local cleanup.
                getSharedPreferences("accountDeletion",MODE_PRIVATE).edit().putBoolean("accepted",true).commit();
                AuthSession.lock();new BiometricAccess(this).clear();cloud.signOut();
                runOnUiThread(()->{Toast.makeText(this,"Eliminación solicitada. Se limpiarán los datos de Bio en este teléfono.",Toast.LENGTH_LONG).show();clearLocal();});return;
            }catch(Exception e){if(e instanceof java.io.IOException)error=e.getMessage();}
            String message=error;runOnUiThread(()->{if(!isDestroyed()){busy=false;submit.setEnabled(true);status.setText(message);}});
        });
    }
    void clearLocal(){
        ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        if(manager==null||!manager.clearApplicationUserData()){status.setText("La solicitud está registrada. Borra los datos de Bio desde Ajustes de Android → Aplicaciones → Bio → Almacenamiento.");}
    }
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
