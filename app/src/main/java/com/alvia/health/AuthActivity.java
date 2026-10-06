package com.alvia.health;

import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import androidx.fragment.app.FragmentActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import org.json.JSONObject;
import java.util.concurrent.*;
import javax.crypto.Cipher;

/** Email account first; hardware-backed biometrics unlock a previously enrolled account. */
public class AuthActivity extends FragmentActivity {
    private FirebaseCloud cloud;private Vault vault;private BiometricAccess access;
    private LinearLayout content;private EditText email,password,confirm;private TextView status;
    private CheckBox biometric;private boolean register,busy,resumed,enrolling;
    private String biometricOwner;private BiometricPrompt prompt;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final int purple=0xff7550b8,ink=0xff282335,muted=0xff80778e;
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        try{vault=new Vault(this);cloud=FirebaseCloud.create(this);access=new BiometricAccess(this);}catch(Exception e){TextView error=new TextView(this);error.setText("No se pudo abrir Bio. Cierra y vuelve a intentarlo. Tus datos se conservan.");setContentView(error);return;}
        register=cloud.anonymous();
        prompt=new BiometricPrompt(this,getMainExecutorCompat(),new BiometricPrompt.AuthenticationCallback(){
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){try{
                if(!resumed||!cloud.account()||!cloud.owner().equals(biometricOwner)||result.getCryptoObject()==null)throw new IllegalStateException();
                access.complete(result.getCryptoObject().getCipher(),enrolling,biometricOwner);enter();
            }catch(Exception e){access.clear();busy=false;status.setText("Vuelve a entrar con tu contraseña para activar la biometría.");}}
            @Override public void onAuthenticationError(int code,CharSequence message){busy=false;status.setText("Puedes continuar con tu correo y contraseña. "+message);}
            @Override public void onAuthenticationFailed(){status.setText("No se reconoció la huella o el rostro. Inténtalo de nuevo.");}
        });
        draw();
    }
    private java.util.concurrent.Executor getMainExecutorCompat(){return command->new Handler(Looper.getMainLooper()).post(command);}
    @Override protected void onResume(){super.onResume();resumed=true;}
    @Override protected void onPause(){resumed=false;super.onPause();}
    @Override protected void onStop(){if(prompt!=null)prompt.cancelAuthentication();super.onStop();}
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(8),0,dp(8));return v;}
    void add(View v){content.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    EditText field(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setTextColor(ink);e.setHintTextColor(muted);e.setInputType(type);e.setSingleLine(true);if((type&InputType.TYPE_TEXT_VARIATION_PASSWORD)!=0)e.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());e.setMinHeight(dp(56));e.setSaveEnabled(false);add(e);return e;}
    void button(String text,Runnable action,boolean primary){TextView b=label(text,15,primary?Color.WHITE:purple);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(52));b.setPadding(dp(12),dp(14),dp(12),dp(14));GradientDrawable bg=new GradientDrawable();bg.setColor(primary?purple:0xfff2edf9);bg.setCornerRadius(dp(16));b.setBackground(bg);b.setFocusable(true);b.setOnClickListener(v->{if(!busy)action.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);content.addView(b,p);}
    private void draw(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.WHITE);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(28),dp(40),dp(28),dp(28));scroll.addView(content);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.bio_header_transparent);logo.setContentDescription("Bio");logo.setScaleType(ImageView.ScaleType.FIT_CENTER);content.addView(logo,new LinearLayout.LayoutParams(dp(104),dp(88)));
        TextView heading=label(register?"Tu salud, contigo":"Cuida tu salud",30,ink);heading.setTypeface(null,Typeface.BOLD);add(heading);
        add(label(register?"Crea tu cuenta y conserva tus documentos.":"Inicia sesión para acceder a tu espacio personal.",15,muted));
        if(cloud.anonymous())add(label("Al crear tu cuenta, los documentos que ya tienes seguirán contigo.",14,0xff37795e));
        email=field("Correo electrónico",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);email.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);
        if(cloud.account())email.setText(cloud.email());
        password=field("Contraseña",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);password.setAutofillHints(register?"newPassword":View.AUTOFILL_HINT_PASSWORD);
        confirm=register?field("Repite la contraseña",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD):null;
        biometric=new CheckBox(this);biometric.setText("Activar acceso con huella o rostro");biometric.setTextColor(ink);biometric.setChecked(getIntent().getBooleanExtra("biometricSetup",false));add(biometric);
        int available=BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        biometric.setEnabled(available==BiometricManager.BIOMETRIC_SUCCESS);
        if(!biometric.isEnabled()){biometric.setChecked(false);add(label("La biometría requiere una huella o un rostro seguro registrado en los ajustes del teléfono. Puedes usar tu contraseña.",12,muted));}
        status=label("",13,0xffa12e46);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);add(status);
        button(register?"Crear cuenta":"Iniciar sesión",this::submit,true);
        try{if(cloud.account()&&access.enabled(cloud.owner()))button("Entrar con huella o rostro",()->biometric(false),false);}catch(Exception ignored){}
        button(register?"Ya tengo una cuenta":"Crear una cuenta",()->{register=!register;draw();},false);
        button("Olvidé mi contraseña",this::reset,false);
        add(label("Tu huella y tu rostro los verifica Android. Bio no recibe ni almacena tus datos biométricos.",12,muted));
    }
    private String address(){String value=email.getText().toString().trim();if(!android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches())throw new IllegalArgumentException("Escribe un correo válido.");return value;}
    void submit(){try{
        String mail=address(),secret=password.getText().toString();if(secret.isEmpty())throw new IllegalArgumentException("Escribe tu contraseña.");
        if(register&&(secret.length()<8||!secret.equals(confirm.getText().toString())))throw new IllegalArgumentException("Usa al menos 8 caracteres y repite la misma contraseña.");
        boolean create=register,enable=biometric.isChecked();busy=true;status.setText("Conectando de forma segura…");
        worker.execute(()->{try{cloud.login(vault,mail,secret,create);try{cloud.syncProfile(vault);}catch(Exception ignored){}runOnUiThread(()->{if(isDestroyed())return;password.setText("");if(confirm!=null)confirm.setText("");busy=false;if(!resumed){status.setText("Vuelve a iniciar sesión para continuar.");return;}if(enable)biometric(true);else enter();});}
            catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){busy=false;status.setText(error(e));}});}});
    }catch(IllegalArgumentException e){status.setText(e.getMessage());}}
    private void reset(){try{String mail=address();busy=true;status.setText("Preparando recuperación…");worker.execute(()->{String message;try{cloud.resetPassword(mail);message="Si el correo corresponde a una cuenta, recibirás instrucciones para restablecer la contraseña.";}catch(Exception e){message=error(e);}String text=message;runOnUiThread(()->{if(!isDestroyed()){busy=false;status.setText(text);}});});}catch(IllegalArgumentException e){status.setText(e.getMessage());}}
    private void biometric(boolean enroll){try{
        if(BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)!=BiometricManager.BIOMETRIC_SUCCESS){status.setText("La biometría no está disponible. Usa tu contraseña.");return;}
        enrolling=enroll;biometricOwner=cloud.owner();Cipher cipher=access.prepare(enroll);busy=true;
        prompt.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle(enroll?"Activar acceso biométrico":"Entrar a Bio").setSubtitle("Verifica tu huella o rostro con Android").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).setNegativeButtonText("Usar contraseña").build(),new BiometricPrompt.CryptoObject(cipher));
    }catch(Exception e){busy=false;access.clear();status.setText("La biometría cambió o no está disponible. Inicia sesión con tu contraseña y vuelve a activarla.");}}
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){super.onActivityResult(request,result,data);if(request==91){AuthSession.profileAccess=false;if(result==RESULT_OK&&cloud.account()&&HealthProfile.complete(vault)){AuthSession.unlock();AuthSession.restore=true;setResult(RESULT_OK);finish();}else{status.setText("Completa tu perfil para continuar.");}}}
    private void enter(){if(!resumed||!cloud.account())return;if(!HealthProfile.complete(vault)){AuthSession.profileAccess=true;startActivityForResult(new android.content.Intent(this,ProfileActivity.class),91);return;}AuthSession.unlock();AuthSession.restore=true;setResult(RESULT_OK);finish();}
    static String error(Exception e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();
        if(cause instanceof java.io.IOException&&!(cause instanceof java.net.SocketException))return cause.getMessage()==null?"No se pudo conectar. Inténtalo de nuevo.":cause.getMessage();
        if(cause instanceof com.google.firebase.FirebaseNetworkException)return "Sin conexión. Comprueba internet y vuelve a intentarlo.";
        if(cause instanceof com.google.firebase.FirebaseTooManyRequestsException)return "Demasiados intentos. Espera unos minutos y vuelve a intentarlo.";
        if(cause instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException)return "Ese correo ya tiene una cuenta. Usa Iniciar sesión u otro correo para vincular tus documentos actuales.";
        return "No se pudo iniciar sesión. Revisa el correo y la contraseña o usa Recuperar contraseña.";
    }
}

