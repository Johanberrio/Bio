package com.alvia.health

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import org.json.JSONObject

class SourcesActivity: FragmentActivity() {
    private lateinit var vault: Vault
    private lateinit var cloud: FirebaseCloud
    private lateinit var form: LinearLayout
    private lateinit var status: TextView
    private lateinit var healthStatus: TextView
    private lateinit var page: LinearLayout
    private var dark=false
    private val purple get()=if(dark) 0xffc5a6ff.toInt() else 0xff7550b8.toInt()
    private val ink get()=if(dark) 0xfff4effa.toInt() else 0xff282335.toInt()
    private val muted get()=if(dark) 0xffb8aec6.toInt() else 0xff80778e.toInt()
    private val surface get()=if(dark) 0xff201b28.toInt() else Color.WHITE
    private val background get()=if(dark) 0xff141119.toInt() else Color.WHITE
    private val lilac get()=if(dark) 0xff2e253f.toInt() else 0xfff2edf9.toInt()
    private var category="exam"
    private var running=false
    private var requesting=false
    private var checking=false
    private var healthRunning=false
    private var healthText="Pulsa Conectar para comprobar el acceso a tus mediciones."
    private val permission = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        requesting=false
        if(!::status.isInitialized || !AuthSession.unlocked()) return@registerForActivityResult
        inspectPermissions(true)
    }
    private val backgroundPermission = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        requesting=false
        if(!::status.isInitialized || !AuthSession.unlocked()) return@registerForActivityResult
        if(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted){SourceSync.schedule(this);message("Lectura en segundo plano autorizada.")}
        else message("Puedes sincronizar al abrir Bio aunque no autorices la lectura en segundo plano.")
    }
    override fun onCreate(state: Bundle?) {
        dark=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("darkMode",false)
        setTheme(if(dark) R.style.Theme_Bio_Dark else R.style.Theme_Bio)
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(!AuthSession.unlocked()){finish();return}
        try{vault=Vault(this);cloud=FirebaseCloud.create(this);if(!cloud.account()){finish();return}}
        catch(e:Exception){finish();return}
        category=state?.getString("category")?:"exam"
        healthText=state?.getString("healthText")?:healthText
        render()
    }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,radius:Int=20)=GradientDrawable().also{it.setColor(color);it.cornerRadius=dp(radius).toFloat()}
    private fun text(value:String, size:Float=14f):TextView=TextView(this).also {
        it.text=value;it.textSize=size;it.setTextColor(if(size>=19f)ink else muted)
        it.typeface=Typeface.create(if(size>=19f)"sans-serif-medium" else "sans-serif",Typeface.NORMAL)
        it.setLineSpacing(dp(3).toFloat(),1f);it.setPadding(0,dp(6),0,dp(10));form.addView(it)
    }
    private fun button(value:String,primary:Boolean=false,action:()->Unit){TextView(this).also {
        it.text=value;it.textSize=14f;it.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        it.setTextColor(if(primary){if(dark)0xff21132f.toInt() else Color.WHITE}else purple)
        it.gravity=Gravity.CENTER;it.minHeight=dp(48);it.setPadding(dp(16),dp(13),dp(16),dp(13))
        it.background=rounded(if(primary)purple else lilac,16);it.isFocusable=true;it.isClickable=true
        it.accessibilityDelegate=object:android.view.View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:android.view.View,info:android.view.accessibility.AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"}}
        form.addView(it,LinearLayout.LayoutParams(-1,-2).also{p->p.topMargin=dp(8)})
        it.setOnClickListener {action()}
    }}
    private fun card(label:String,title:String){
        form=LinearLayout(this);form.orientation=LinearLayout.VERTICAL;form.setPadding(dp(20),dp(18),dp(20),dp(20))
        form.background=rounded(surface).also{it.setStroke(dp(1),if(dark)0xff3b3248.toInt() else 0xffeeeaf2.toInt())}
        page.addView(form,LinearLayout.LayoutParams(-1,-2).also{it.bottomMargin=dp(18)})
        text(label,11f).also{it.setTextColor(purple);it.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)}
        text(title,21f)
    }
    private fun render(){
        val scroll=ScrollView(this);scroll.setBackgroundColor(background)
        page=AdaptiveColumn(this);page.orientation=LinearLayout.VERTICAL;page.setPadding(dp(24),dp(24),dp(24),dp(24));scroll.addView(page);setContentView(scroll);form=page
        if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener {v,i -> val b=i.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);i}
        else scroll.fitsSystemWindows=true
        scroll.requestApplyInsets()
        val header=LinearLayout(this);header.gravity=Gravity.CENTER_VERTICAL
        header.addView(ImageView(this).also{it.setImageResource(R.drawable.bio_header_transparent);it.scaleType=ImageView.ScaleType.FIT_CENTER;it.contentDescription="Bio"},LinearLayout.LayoutParams(dp(76),dp(60)))
        header.addView(Space(this),LinearLayout.LayoutParams(0,1,1f))
        header.addView(TextView(this).also{it.text="‹  Volver";it.textSize=14f;it.setTextColor(purple);it.gravity=Gravity.CENTER;it.minHeight=dp(48);it.setPadding(dp(16),0,dp(16),0);it.background=rounded(lilac,16);it.isFocusable=true;it.setOnClickListener{finish()}})
        page.addView(header)
        text("TU SALUD, CONECTADA",11f).setTextColor(purple)
        text("Fuentes y dispositivos",32f)
        text("Reúne tus mediciones y documentos en un solo lugar.").setPadding(0,0,0,dp(24))
        card("MEDICIONES","Signos vitales")
        text("Conecta Health Connect para recibir mediciones que comparta la aplicación de tu reloj, pulsera o dispositivo médico. Debes vincular el dispositivo en su aplicación original. Bio importa los últimos 30 días y conserva la hora y la fuente; no mide con el teléfono. Autoriza Peso para actualizar automáticamente tu perfil e Inicio, y la lectura en segundo plano para sincronizar con la app cerrada.")
        healthStatus=text(healthText).also {it.setTextColor(if(dark)0xff85d8ae.toInt() else 0xff37795e.toInt());it.accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE}
        button("Conectar / revisar permisos",true) {connect()}
        button("Abrir ajustes de Health Connect") {healthSettings()}
        if(SourceSync.setting(vault,"health").optBoolean("enabled")) {
            button("Sincronizar mediciones") {syncHealth()}
            button("Permitir sincronización en segundo plano") {connectBackground()}
            button("Desconectar dispositivos de Bio"){disable("health");render()}
        }
        card("TUS ARCHIVOS","Documentos clínicos")
        text("Elige una carpeta dedicada a documentos tuyos. Se importarán los PDF, JPG y PNG existentes y los nuevos (hasta 10 MB), se analizarán y se respaldarán en tu cuenta de Bio. No se recorren subcarpetas. La revisión en segundo plano depende de Android y de la conexión.")
        button("Elegir carpeta de exámenes"){choose("exam")}
        button("Elegir carpeta de historia clínica"){choose("history")}
        for(source in vault.list("sources").filter {it.optString("id").startsWith("folder-")&&it.optBoolean("enabled")}){
            text((if(source.optString("category")=="history")"Historia clínica" else "Exámenes")+" · "+source.optString("label")+"\n"+source.optString("status","Esperando primera revisión"))
            button("Desconectar esta carpeta"){disable(source.getString("id"));render()}
        }
        card("IMPORTACIÓN AUTOMÁTICA","Correo electrónico")
        text("Conecta Gmail para recibir tus documentos médicos en Bio. Gestiona tus cuentas y consulta el estado de la sincronización.")
        button("Conectar y gestionar correos",true){startActivity(Intent(this,MailActivity::class.java))}
        form=page
        status=text("Las mediciones manuales siguen disponibles en Salud.")
        status.accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
        button("Revisar fuentes ahora",true){sync()}
    }
    private fun disable(id:String){val s=SourceSync.setting(vault,id);s.put("enabled",false);vault.put("sources",s)}
    private fun healthMessage(text:String){healthText=text;if(::healthStatus.isInitialized)healthStatus.text=text}
    private fun message(text:String){if(isFinishing||isDestroyed)return;healthMessage(text);AlertDialog.Builder(this).setTitle("Conexión de dispositivos").setMessage(text).setPositiveButton("Entendido",null).show()}
    private fun launchExternal(action:()->Unit){AuthSession.beginExternalFlow();try{action()}catch(e:Exception){requesting=false;AuthSession.cancelExternalFlow();message("Android no pudo abrir esta pantalla. Revisa Health Connect en los ajustes del teléfono e inténtalo de nuevo.")}}
    private fun connect(){if(requesting||checking){healthMessage("Comprobando permisos de Health Connect…");return}
        try{when(HealthBridge.availability(this)){
            HealthConnectClient.SDK_AVAILABLE -> inspectPermissions(false)
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> if(android.os.Build.VERSION.SDK_INT>=34)healthSettings() else open("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            else -> message("Health Connect no está disponible en este dispositivo o perfil de Android. Revisa sus actualizaciones y usa el perfil personal del teléfono. Puedes seguir registrando mediciones manualmente.")
        }}catch(e:Exception){message("No se pudo consultar Health Connect. Revisa la actualización del sistema de Google Play en los ajustes de Android y vuelve a intentarlo.")}
    }
    private fun inspectPermissions(afterRequest:Boolean){if(checking)return;checking=true;healthMessage("Comprobando permisos de Health Connect…")
        Thread {
            try{val granted=HealthBridge.granted(applicationContext);val allowed=granted.intersect(HealthBridge.permissions())
                runOnUiThread {checking=false;if(isFinishing||isDestroyed||!AuthSession.unlocked())return@runOnUiThread
                    try{if(allowed.size==HealthBridge.permissions().size || (afterRequest&&allowed.isNotEmpty())){
                        vault.put("sources",JSONObject().put("id","health").put("enabled",true).put("owner",cloud.owner()))
                        SourceSync.schedule(this);healthText="Health Connect autorizado · ${allowed.size} de ${HealthBridge.permissions().size} tipos de medición.";render()
                        AlertDialog.Builder(this).setTitle("Health Connect autorizado").setMessage("${healthText}\n\nPuedes sincronizar ahora o revisar los permisos en los ajustes de Health Connect.")
                            .setPositiveButton("Sincronizar ahora"){_,_->syncHealth()}.setNeutralButton("Revisar permisos"){_,_->healthSettings()}.setNegativeButton("Cerrar",null).show()
                    }else if(afterRequest){message("No se autorizaron mediciones. Revisa los permisos en los ajustes de Health Connect.")}
                    else {requesting=true;launchExternal {permission.launch(HealthBridge.permissions()-granted)}}
                    }catch(e:Exception){message("No se pudo guardar la conexión. Vuelve a intentarlo.")}
                }
            }catch(e:Exception){runOnUiThread {checking=false;message("No se pudieron comprobar los permisos. Abre los ajustes de Health Connect y vuelve a intentarlo.")}}
        }.start()
    }
    private fun syncHealth(){if(healthRunning){healthMessage("Sincronizando mediciones…");return};healthRunning=true;healthMessage("Sincronizando mediciones…")
        Thread {val result=try{val count=HealthBridge.sync(applicationContext,false)
            val weightStatus=SourceSync.setting(vault,"health").optString("weightStatus")
            weightStatus+"\n"+(
            if(count>0)"Sincronización completada: $count mediciones nuevas o actualizadas. Puedes verlas en Salud."
            else "Sincronización completada: no hay mediciones nuevas en los últimos 30 días. Si esperabas datos, revisa que Samsung Health o la app de tu dispositivo los comparta con Health Connect.")
        }catch(e:Exception){"No se completó la sincronización. Revisa los permisos de medición y vuelve a intentarlo."}
            runOnUiThread {healthRunning=false;if(!isDestroyed&&!isFinishing)healthMessage(result)}
        }.start()
    }
    private fun connectBackground(){if(requesting)return
        try{if(!HealthBridge.backgroundAvailable(this)){message("Tu versión de Health Connect no permite lectura en segundo plano. Bio puede sincronizar mientras está abierta.");return}
            requesting=true;launchExternal {backgroundPermission.launch(setOf(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND))}
        }catch(e:Exception){requesting=false;message("No se pudo solicitar lectura en segundo plano. Puedes seguir sincronizando desde Bio.")}
    }
    private fun healthSettings(){launchExternal {startActivity(Intent(if(android.os.Build.VERSION.SDK_INT>=34)"android.health.connect.action.HEALTH_HOME_SETTINGS" else "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"))}}
    private fun open(url:String){launchExternal {startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}
    private fun choose(kind:String){category=kind;launchExternal {startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),42)}}
    override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data)
        if(request!=42||result!=RESULT_OK||data?.data==null)return
        try{val uri=data.data!!;contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val id="folder-"+SourceSync.hash(uri.toString().toByteArray())
            val label=androidx.documentfile.provider.DocumentFile.fromTreeUri(this,uri)?.name?:"Carpeta clínica"
            vault.put("sources",JSONObject().put("id",id).put("uri",uri.toString()).put("category",category).put("label",label).put("enabled",true).put("owner",cloud.owner()))
            SourceSync.schedule(this);render();sync()
        }catch(e:Exception){status.text="No se pudo conservar el permiso. Vuelve a elegir la carpeta."}
    }
    private fun sync(){if(running)return;running=true;status.text="Revisando fuentes y analizando documentos…"
        Thread {val result=try{SourceSync.run(applicationContext,false)}catch(e:Exception){"No se completó la revisión. Comprueba los permisos y la conexión."}
            runOnUiThread {running=false;if(!isDestroyed)status.text=result}
        }.start()
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("category",category);out.putString("healthText",healthText);super.onSaveInstanceState(out)}
    override fun onResume(){super.onResume();requesting=false;if(!AuthSession.unlocked())finish()}
}
