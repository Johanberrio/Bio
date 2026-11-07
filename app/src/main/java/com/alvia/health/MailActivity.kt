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
import org.json.JSONObject

class MailActivity: FragmentActivity() {
    private lateinit var vault: Vault
    private lateinit var cloud: FirebaseCloud
    private lateinit var form: LinearLayout
    private lateinit var status: TextView
    private lateinit var page: LinearLayout
    private var dark=false
    private val purple get()=if(dark)0xffc5a6ff.toInt() else 0xff7550b8.toInt()
    private val ink get()=if(dark)0xfff4effa.toInt() else 0xff282335.toInt()
    private val muted get()=if(dark)0xffb8aec6.toInt() else 0xff80778e.toInt()
    private val surface get()=if(dark)0xff201b28.toInt() else Color.WHITE
    private val lilac get()=if(dark)0xff2e253f.toInt() else 0xfff2edf9.toInt()
    private var capabilities=JSONObject()
    private var busy=false
    private var loaded=false
    private var returning=false
    private var message="Comprobando disponibilidad…"
    override fun onCreate(saved:Bundle?){
        dark=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("darkMode",false)
        setTheme(if(dark)R.style.Theme_Bio_Dark else R.style.Theme_Bio)
        super.onCreate(saved);window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(!AuthSession.unlocked()){finish();return}
        try{vault=Vault(this);cloud=FirebaseCloud.create(this);if(!cloud.account()){finish();return}}catch(e:Exception){finish();return}
        render();refresh()
    }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun rounded(color:Int,radius:Int=20)=GradientDrawable().also{it.setColor(color);it.cornerRadius=dp(radius).toFloat()}
    private fun text(value:String,size:Float=14f)=TextView(this).also {
        it.text=value;it.textSize=size;it.setTextColor(if(size>=19f)ink else muted)
        it.typeface=Typeface.create(if(size>=19f)"sans-serif-medium" else "sans-serif",Typeface.NORMAL)
        it.setLineSpacing(dp(3).toFloat(),1f);it.setPadding(0,dp(6),0,dp(10));form.addView(it)
    }
    private fun button(label:String,enabled:Boolean=true,primary:Boolean=false,action:()->Unit){TextView(this).also {
        it.text=label;it.textSize=14f;it.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
        it.setTextColor(if(primary){if(dark)0xff21132f.toInt() else Color.WHITE}else purple)
        it.gravity=Gravity.CENTER;it.minHeight=dp(48);it.setPadding(dp(16),dp(13),dp(16),dp(13))
        it.background=rounded(if(primary)purple else lilac,16);it.isFocusable=true;it.isEnabled=enabled&&!busy;it.alpha=if(it.isEnabled)1f else 0.45f
        it.accessibilityDelegate=object:android.view.View.AccessibilityDelegate(){override fun onInitializeAccessibilityNodeInfo(host:android.view.View,info:android.view.accessibility.AccessibilityNodeInfo){super.onInitializeAccessibilityNodeInfo(host,info);info.className="android.widget.Button"}}
        form.addView(it,LinearLayout.LayoutParams(-1,-2).also{p->p.topMargin=dp(8)});it.setOnClickListener{action()}
    }}
    private fun card(label:String,title:String){
        form=LinearLayout(this);form.orientation=LinearLayout.VERTICAL;form.setPadding(dp(20),dp(18),dp(20),dp(20))
        form.background=rounded(surface).also{it.setStroke(dp(1),if(dark)0xff3b3248.toInt() else 0xffeeeaf2.toInt())}
        page.addView(form,LinearLayout.LayoutParams(-1,-2).also{it.bottomMargin=dp(18)})
        text(label,11f).also{it.setTextColor(purple);it.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)}
        text(title,21f)
    }
    private fun render(){
        val scroll=ScrollView(this);scroll.setBackgroundColor(if(dark)0xff141119.toInt() else Color.WHITE)
        page=AdaptiveColumn(this);page.orientation=LinearLayout.VERTICAL;page.setPadding(dp(24),dp(24),dp(24),dp(24));scroll.addView(page);setContentView(scroll);form=page
        if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener{v,i->val b=i.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout() or android.view.WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);i}
        else scroll.fitsSystemWindows=true
        scroll.requestApplyInsets()
        val header=LinearLayout(this);header.gravity=Gravity.CENTER_VERTICAL
        header.addView(ImageView(this).also{it.setImageResource(R.drawable.bio_header_transparent);it.scaleType=ImageView.ScaleType.FIT_CENTER;it.contentDescription="Bio"},LinearLayout.LayoutParams(dp(76),dp(60)))
        header.addView(Space(this),LinearLayout.LayoutParams(0,1,1f))
        header.addView(TextView(this).also{it.text="‹  Volver";it.textSize=14f;it.setTextColor(purple);it.gravity=Gravity.CENTER;it.minHeight=dp(48);it.setPadding(dp(16),0,dp(16),0);it.background=rounded(lilac,16);it.isFocusable=true;it.setOnClickListener{finish()}})
        page.addView(header)
        text("TUS DOCUMENTOS, CONECTADOS",11f).setTextColor(purple)
        text("Tu correo en Bio",32f)
        text("Tus documentos médicos, más cerca de ti.").setPadding(0,0,0,dp(24))
        card("IMPORTACIÓN AUTOMÁTICA","Conecta tu correo")
        text("Conecta una o varias cuentas para importar tus exámenes e historia clínica. Bio selecciona adjuntos por palabras de su nombre como historia, ayudas diagnósticas, resultados o exámenes, reconociendo tildes y separadores; analiza los PDF, JPG y PNG de hasta 10 MB y guarda solo los que identifica como tuyos.")
        text("El análisis usa el nombre completo y la fecha de nacimiento de tu perfil para comprobar el titular. Los archivos aceptados se nombran según su contenido y se ordenan por fecha clínica. No se envían ni se borran correos.")
        for((provider,label) in listOf("google" to "Gmail / Google Workspace","microsoft" to "Outlook / Hotmail / Microsoft 365")){
            val available=capabilities.optBoolean(provider)
            button("Conectar $label",loaded&&available,primary=provider=="google"){connect(provider)}
            if(loaded&&!available)text("$label: pendiente de habilitación del proveedor para Bio.",13f)
        }
        card("AL DÍA","Sincronización")
        status=text(message).also{it.setTextColor(if(dark)0xff85d8ae.toInt() else 0xff37795e.toInt());it.accessibilityLiveRegion=android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE}
        button("Actualizar conexiones"){refresh()}
        button("Sincronizar ahora",loaded,primary=true){sync()}
        for(s in vault.list("sources").filter{it.optString("id").startsWith(MailSync.PREFIX)&&it.optString("owner")==cloud.owner()&&!it.optBoolean("remoteDisconnected")}){
            card("CUENTA CONECTADA",s.optString("email"));text(s.optString("status","Conectado · esperando primera revisión"))
            button("Desconectar ${s.optString("email")}"){task("Desconectando…"){MailSync.disconnect(this,vault,cloud,s);"Cuenta desconectada. Los documentos ya guardados se conservan."}}
        }
        val reviews=MailReviews.pending(vault,cloud.owner())
        if(reviews.isNotEmpty()){
            card("AVISOS RECIENTES","Adjuntos no importados")
            text("Estos archivos ya se revisaron; no están en espera de importación. Los avisos se ocultan después de siete días o cuando el mismo archivo se importa. Descartarlos conserva el registro de revisión y no borra correos.",13f)
            button("Descartar todos los avisos (${reviews.size})"){task("Retirando avisos…"){MailReviews.dismiss(vault,reviews);"Avisos descartados. Tus documentos y correos se conservan."}}
            for(r in reviews.take(10)){
                text(r.optString("name")+"\n"+r.optString("status"),13f)
                button("Descartar aviso: ${r.optString("name")}"){task("Retirando aviso…"){MailReviews.dismiss(vault,listOf(r));"Aviso descartado."}}
            }
        }
        form=page
        text("Primera revisión: últimos 30 días. Después se revisan correos nuevos por lotes. Android programa la revisión en segundo plano a partir de 15 minutos y puede retrasarla. La cuota de análisis puede dejar archivos pendientes. Los enlaces a portales, archivos protegidos y formatos no admitidos requieren carga manual.",13f)
    }
    private fun task(progress:String,work:()->String){if(busy)return;busy=true;message=progress;render();Thread{val result=try{work()}catch(e:Exception){if(e is java.io.IOException)e.message?:"No se completó la conexión." else "No se completó la conexión. Vuelve a intentarlo."};runOnUiThread{busy=false;message=result;if(!isDestroyed&&!isFinishing)render()}}.start()}
    private fun refresh(){task("Comprobando conexiones…"){val response=MailSync.refresh(this,vault,cloud);capabilities=response.getJSONObject("providers");loaded=true;"${response.getJSONArray("accounts").length()} cuentas conectadas."}}
    private fun sync(){task("Revisando correo y analizando adjuntos…"){
        MailSync.refresh(this,vault,cloud)
        val result=SourceSync.run(applicationContext,false)
        val details=vault.list("sources").filter{it.optString("id").startsWith(MailSync.PREFIX)&&it.optBoolean("enabled")&&it.optString("owner")==cloud.owner()}
            .joinToString("\n\n"){it.optString("email")+"\n"+it.optString("status","Pendiente de revisión")}
        if(details.isEmpty()) "No hay correos conectados. Pulsa Actualizar conexiones y conecta Gmail." else result+"\n\n"+details
    }}
    private fun connect(provider:String){
        AlertDialog.Builder(this).setTitle("Conectar correo").setMessage("Autoriza a Bio a leer esta cuenta para buscar documentos médicos. Los adjuntos candidatos se envían al servidor de Bio para su análisis; solo los identificados como tuyos se incorporan a Exámenes o Historia. Puedes desconectar cuando quieras.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Continuar"){_,_->
                task("Preparando autorización…"){
                    val result=MailApi(this,cloud).post("/mail/connect",JSONObject().put("provider",provider));val uri=Uri.parse(result.getString("url"))
                    if(uri.scheme!="https"||uri.host !in listOf("accounts.google.com","login.microsoftonline.com"))throw java.io.IOException("Dirección de autorización inválida.")
                    runOnUiThread{if(!isDestroyed&&!isFinishing&&AuthSession.unlocked()){try{AuthSession.beginExternalFlow();returning=true;startActivity(Intent(Intent.ACTION_VIEW,uri))}catch(e:Exception){returning=false;AuthSession.cancelExternalFlow();message="No se pudo abrir el navegador.";render()}}}
                    "Completa la autorización en el navegador y vuelve a Bio."
                }
            }.show()
    }
    override fun onResume(){super.onResume();if(!AuthSession.unlocked()){finish();return};if(returning&&::vault.isInitialized){returning=false;refresh()}}
}
