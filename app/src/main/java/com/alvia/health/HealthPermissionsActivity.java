package com.alvia.health;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
/** Public permission rationale contains no patient information. */
public class HealthPermissionsActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);TextView t=new TextView(this);t.setTextSize(18);t.setPadding(32,64,32,32);t.setText("Health Connect y Bio\n\nBio lee únicamente las mediciones que autorices: pulso, presión arterial, oxígeno, temperatura, respiración, glucosa y peso. Se guardan cifradas en tu teléfono con su fecha y procedencia. El peso más reciente actualiza tu perfil de salud automáticamente. Se incluyen en investigaciones de IA solo cuando eliges incluir tus mediciones.\n\nSi autorizas lectura en segundo plano, Android puede sincronizarlas periódicamente. Puedes desconectar la fuente en Bio y revocar permisos en Health Connect. Los datos ya importados permanecen hasta que los elimines. Bio no escribe datos en Health Connect.");setContentView(t);}
}
