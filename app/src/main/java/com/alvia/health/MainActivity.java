package com.alvia.health;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    int PURPLE=0xff7550b8, INK=0xff282335, MUTED=0xff80778e, GREEN=0xff37795e, LILAC=0xfff2edf9, BORDER=0xffeeeaf2;
    private java.time.LocalDate agendaDay=java.time.LocalDate.now();
    private java.time.YearMonth agendaMonth=java.time.YearMonth.now();
    private boolean darkMode;
    private int BACKGROUND=Color.WHITE, SURFACE=Color.WHITE;
    private Vault vault;
    private FirebaseCloud cloud;
    private String cloudStatus="";
    private LinearLayout root,content,nav;
    private int screen=0,healthTab=0,metric=0;
    private boolean combined=false,busy=false,automaticRunning=false;
    private final Handler documentHandler=new Handler(Looper.getMainLooper());
    private final Runnable documentRetry=new Runnable(){public void run(){automaticDocuments();documentHandler.postDelayed(this,60000);}};
    private boolean authOpening;
    private void requireLogin(){if(!authOpening){authOpening=true;startActivityForResult(new Intent(this,AuthActivity.class),90);}}
    @Override protected void onResume(){super.onResume();if(!AuthSession.unlocked()){requireLogin();return;}if(vault!=null){render();if(!vault.list("sources").isEmpty()){SourceSync.schedule(this);executor.execute(()->{try{SourceSync.run(getApplicationContext(),false,()->runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&AuthSession.unlocked())render();}));}catch(Exception ignored){}});}}documentHandler.postDelayed(documentRetry,1000);}
    @Override protected void onPause(){documentHandler.removeCallbacks(documentRetry);if(root!=null)root.setVisibility(View.INVISIBLE);super.onPause();}
    private final Set<Integer> systems=new LinkedHashSet<>(Collections.singleton(0));
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private String aiResult="",aiStatus="", aiQuery="",aiCity="";
    private JSONArray aiSources=new JSONArray();
    private static final String[] METRICS={"Frecuencia cardíaca","Presión sistólica","Presión diastólica","Oxígeno en sangre","Temperatura","Frecuencia respiratoria","Glucosa"};
    private static final String[] UNITS={"lpm","mmHg","mmHg","%","°C","rpm","mg/dL"};
    private static final double[] MAX_VALUES={350,350,250,100,50,120,1500};

    @Override public void onCreate(Bundle state){
        darkMode=getPreferences(MODE_PRIVATE).getBoolean("darkMode",false);
        setTheme(darkMode?R.style.Theme_Bio_Dark:R.style.Theme_Bio);
        if(darkMode){PURPLE=0xffc5a6ff;INK=0xfff4effa;MUTED=0xffb8aec6;GREEN=0xff85d8ae;LILAC=0xff2e253f;BORDER=0xff3b3248;BACKGROUND=0xff141119;SURFACE=0xff201b28;}
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        try {vault=new Vault(this);HealthProfile.migrateWeights(vault);}catch(Exception e){new AlertDialog.Builder(this).setTitle("No pudimos abrir tus datos").setMessage("El almacén cifrado no está disponible. Tus archivos se conservarán. Cierra y vuelve a abrir Bio.").setCancelable(false).setPositiveButton("Cerrar",(d,w)->finish()).show();return;}
        try{cloud=FirebaseCloud.create(this);}catch(Exception ignored){cloud=null;}
        screen=state==null?getIntent().getIntExtra("screen",0):state.getInt("screen");
        if(state!=null){agendaDay=java.time.LocalDate.parse(state.getString("agendaDay",agendaDay.toString()));agendaMonth=java.time.YearMonth.parse(state.getString("agendaMonth",agendaMonth.toString()));healthTab=state.getInt("healthTab");metric=state.getInt("metric");aiQuery=state.getString("query","");aiCity=state.getString("city","");combined=state.getBoolean("combined");int[] saved=state.getIntArray("systems");if(saved!=null){systems.clear();for(int id:saved)systems.add(id);}}
        try{Reminders.restore(this);}catch(Exception e){toast("No se pudieron restaurar los recordatorios. Revisa tu agenda.");}render();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("screen",screen);out.putString("agendaDay",agendaDay.toString());out.putString("agendaMonth",agendaMonth.toString());out.putInt("healthTab",healthTab);out.putInt("metric",metric);out.putString("query",aiQuery);out.putString("city",aiCity);out.putBoolean("combined",combined);out.putIntArray("systems",systems.stream().mapToInt(Integer::intValue).toArray());}
    @Override protected void onDestroy(){executor.shutdownNow();super.onDestroy();}
    @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);if(vault!=null)render();}
    int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    int surfaceColor(int color){if(!darkMode)return color;if(color==Color.WHITE)return SURFACE;if(color==0xffedf6f0)return 0xff19372c;if(color==0xfff8f7fa||color==0xfff6f3f9)return SURFACE;return color;}
    void toggleAppearance(){getPreferences(MODE_PRIVATE).edit().putBoolean("darkMode",!darkMode).apply();recreate();}
    GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(surfaceColor(color));d.setCornerRadius(dp(radius));return d;}
    TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(darkMode&&color==0xff887000?0xffffe16d:darkMode&&(color==0xffc42b43||color==0xffad263e)?0xffff8ca0:color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(3),1);return t;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void gap(LinearLayout layout,int h){Space s=new Space(this);layout.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    void add(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    TextView action(String label,Runnable onClick,boolean primary){TextView b=new TextView(this);b.setText(label);BioButton.style(b,primary?PURPLE:LILAC,primary?(darkMode?0xff21132f:Color.WHITE):PURPLE);b.setOnClickListener(v->onClick.run());return b;}
    LinearLayout card(LinearLayout parent,int color){LinearLayout l=column();l.setPadding(dp(20),dp(20),dp(20),dp(20));GradientDrawable d=bg(color,24);if(color==Color.WHITE)d.setStroke(dp(1),BORDER);l.setBackground(d);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(l,lp);return l;}
    void title(String eyebrow,String title,String subtitle){add(content,text(eyebrow.toUpperCase(new Locale("es")),11,PURPLE,true));gap(content,8);add(content,text(title,32,INK,true));gap(content,8);add(content,text(subtitle,14,MUTED,false));gap(content,24);}
    void section(String name,String link,Runnable callback){LinearLayout r=row();TextView t=text(name,19,INK,true);r.addView(t,new LinearLayout.LayoutParams(0,-2,1));if(link!=null){TextView a=text(link,13,PURPLE,true);a.setPadding(dp(8),dp(12),0,dp(12));a.setMinHeight(dp(48));a.setOnClickListener(v->callback.run());a.setFocusable(true);r.addView(a);}add(content,r);gap(content,12);}
    void empty(LinearLayout l,String heading,String desc){add(l,text(heading,17,INK,true));gap(l,7);add(l,text(desc,14,MUTED,false));}
    void go(int target){screen=target;render();}
    void render(){if(isFinishing()||isDestroyed())return;if(!AuthSession.unlocked()){setContentView(new android.widget.FrameLayout(this));return;}root=column();root.setBackgroundColor(BACKGROUND);if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,insets.getInsets(WindowInsets.Type.ime()).bottom));return insets;});
        // API 26–29 use legacy system insets.
        if(Build.VERSION.SDK_INT<30)root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setFocusableInTouchMode(true);scroll.requestFocus();content=new AdaptiveColumn(this);content.setPadding(dp(24),dp(16),dp(24),dp(20));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout brand=row();ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.bio_header_transparent);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Bio");brand.addView(logo,new LinearLayout.LayoutParams(dp(76),dp(60)));Space brandSpace=new Space(this);brand.addView(brandSpace,new LinearLayout.LayoutParams(0,1,1));TextView appearance=action(getResources().getConfiguration().fontScale>=1.3f?(darkMode?"☀":"☾"):(darkMode?"☀ Claro":"☾ Oscuro"),()->toggleAppearance(),false);appearance.setTextSize(12);appearance.setContentDescription(darkMode?"Activar modo claro":"Activar modo oscuro");brand.addView(appearance);TextView settings=text("  ⚙",23,MUTED,false);settings.setContentDescription("Cuenta y acceso biométrico");settings.setMinHeight(dp(48));settings.setGravity(Gravity.CENTER);settings.setOnClickListener(v->settings());brand.addView(settings);add(content,brand);gap(content,28);
        switch(screen){case 0:home();break;case 1:appointments();break;case 2:body();break;case 3:health();break;default:research();}
        nav=row();nav.setPadding(dp(10),dp(9),dp(10),dp(8));nav.setBackgroundColor(SURFACE);String[] labels={"Inicio","Citas","Mi cuerpo","Mi salud","IA"};String[] icons={"⌂","▦","◎","♡","✧"};for(int i=0;i<5;i++){final int index=i;LinearLayout item=column();item.setGravity(Gravity.CENTER);item.setMinimumHeight(dp(62));item.setPadding(dp(2),dp(6),dp(2),dp(5));if(screen==i)item.setBackground(bg(LILAC,16));TextView icon=text(icons[i],23,screen==i?PURPLE:MUTED,true);icon.setGravity(Gravity.CENTER);add(item,icon);TextView label=text(labels[i],10,screen==i?PURPLE:MUTED,screen==i);label.setGravity(Gravity.CENTER);add(item,label);item.setContentDescription(labels[i]);item.setFocusable(true);item.setOnClickListener(v->go(index));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));}root.addView(nav);setContentView(root);root.requestApplyInsets();}

    static String measurement(JSONObject profile,String field,String unit){
        double value=profile.optDouble(field,Double.NaN);
        if(!Double.isFinite(value)||value<=0)return "Sin registrar";
        java.text.NumberFormat format=java.text.NumberFormat.getNumberInstance(new Locale("es","CO"));
        format.setMaximumFractionDigits(1);return format.format(value)+" "+unit;
    }
    void homeMeasurements(LinearLayout hero){
        JSONObject profile=HealthProfile.current(vault);LinearLayout values=row();
        values.setGravity(Gravity.TOP);
        for(String[] item:new String[][]{{"PESO",measurement(profile,"weightKg","kg")},{"ESTATURA",measurement(profile,"heightCm","cm")},{"SANGRE",profile.optString("bloodGroup","No lo sé")}}){
            LinearLayout cell=column();cell.setPadding(dp(4),dp(12),dp(4),dp(12));
            TextView label=text(item[0],10,PURPLE,true);label.setGravity(Gravity.CENTER);add(cell,label);gap(cell,4);
            TextView value=text(item[1].trim().isEmpty()?"No lo sé":item[1],17,INK,true);value.setGravity(Gravity.CENTER);add(cell,value);
            if(item[0].equals("SANGRE"))cell.setContentDescription("Grupo sanguíneo: "+value.getText());
            values.addView(cell,new LinearLayout.LayoutParams(0,-2,1f));
        }
        add(hero,values);
        gap(hero,8);TextView change=action("Cambiar perfil",()->{AuthSession.profileAccess=true;startActivity(new Intent(this,ProfileActivity.class));},false);BioButton.style(change,SURFACE,PURPLE);add(hero,change);
    }
    void home(){title("Tu espacio de bienestar","Tu salud, en armonía.","Todo lo que necesitas para cuidarte, en un solo lugar.");
        LinearLayout hero=card(content,LILAC);add(hero,text("CONTIGO, CADA DÍA",10,PURPLE,true));gap(hero,10);add(hero,text("Conoce tu cuerpo.\nConecta con tu salud.",25,INK,true));gap(hero,10);add(hero,text("Explora tus sistemas y reúne lo que sabes de ti.",14,MUTED,false));ImageView mini=new ImageView(this);mini.setImageResource(HealthProfile.female(HealthProfile.current(vault))?R.drawable.bio_home_body_female:R.drawable.bio_home_body);mini.setScaleType(ImageView.ScaleType.FIT_CENTER);mini.setAdjustViewBounds(true);mini.setContentDescription("Ilustración de los sistemas del cuerpo. Explorar mi cuerpo");mini.setOnClickListener(v->go(2));addSized(hero,mini,380);homeMeasurements(hero);gap(hero,12);add(hero,action("Explorar mi cuerpo  →",()->go(2),true));
        add(content,action("Mi plan · Bio Premium",()->startActivity(new Intent(this,PremiumActivity.class)),false));gap(content,16);
        section("Tu próxima cita","Ver agenda",()->go(1));List<JSONObject> upcoming=upcoming();if(upcoming.isEmpty()){LinearLayout c=card(content,Color.WHITE);empty(c,"Un espacio para cuidarte","Organiza tu primera cita y activa sus recordatorios.");gap(c,14);add(c,action("+  Agendar cita",()->appointmentForm(null),false));}else appointmentCard(upcoming.get(0),false);
        section("Tus mediciones","Ver todas",()->{healthTab=0;go(3);});LinearLayout vitalRow=new AdaptiveRow(this);for(int i:new int[]{0,3}){List<JSONObject> values=vitals(i);LinearLayout c=column();c.setPadding(dp(16),dp(18),dp(12),dp(18));c.setBackground(bg(i==0?LILAC:0xffedf6f0,20));add(c,text(i==0?"♡  Pulso":"◌  Oxígeno",13,i==0?PURPLE:GREEN,true));gap(c,10);add(c,text(values.isEmpty()?"—":values.get(values.size()-1).optString("value"),28,INK,true));add(c,text(values.isEmpty()?"Sin mediciones":UNITS[i]+" · "+values.get(values.size()-1).optString("origin","Registro manual"),11,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(i==0)lp.rightMargin=dp(10);vitalRow.addView(c,lp);}add(content,vitalRow);gap(content,22);
        LinearLayout privacy=card(content,0xfff8f7fa);add(privacy,text("Tu historia, siempre contigo",16,INK,true));gap(privacy,8);add(privacy,text(vault.list("records").size()+" registros · "+vault.list("documents").size()+" documentos\nGuardados y cifrados en este dispositivo.",13,MUTED,false));}
    void addSized(LinearLayout l,View view,int h){
        l.addView(view,new LinearLayout.LayoutParams(-1,dp(h)));
        if(view instanceof ImageView||view instanceof BodyView)view.addOnLayoutChangeListener((v,left,top,right,bottom,ol,ot,or,ob)->{
            int target=Math.min(dp(600),Math.round((right-left)*1.5f));
            if(target>0&&v.getLayoutParams().height!=target){v.getLayoutParams().height=target;v.requestLayout();}
        });
    }
    String date(long time,String pattern){return new SimpleDateFormat(pattern,new Locale("es","CO")).format(new Date(time));}
    List<JSONObject> upcoming(){List<JSONObject> items=vault.list("appointments");items.removeIf(a->a.optLong("time")<System.currentTimeMillis());items.sort(Comparator.comparingLong(a->a.optLong("time")));return items;}
    java.time.LocalDate appointmentDay(JSONObject a){return java.time.Instant.ofEpochMilli(a.optLong("time")).atZone(java.time.ZoneId.systemDefault()).toLocalDate();}
    void moveMonth(int delta){java.time.YearMonth next=agendaMonth.plusMonths(delta);if(next.getYear()<1||next.getYear()>9999)return;agendaMonth=next;agendaDay=next.atDay(1);render();}
    void chooseMonth(){
        LinearLayout f=form();Spinner month=new Spinner(this);String[] months={"Enero","Febrero","Marzo","Abril","Mayo","Junio","Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        month.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,months));month.setSelection(agendaMonth.getMonthValue()-1);add(f,month);
        EditText year=field(f,"Año (1–9999)",String.valueOf(agendaMonth.getYear()),InputType.TYPE_CLASS_NUMBER);
        dialog("Ir a mes y año",f,()->{int y;try{y=Integer.parseInt(required(year));}catch(NumberFormatException e){throw new IllegalArgumentException("Escribe un año entre 1 y 9999.");}if(y<1||y>9999)throw new IllegalArgumentException("Escribe un año entre 1 y 9999.");agendaMonth=java.time.YearMonth.of(y,month.getSelectedItemPosition()+1);agendaDay=agendaMonth.atDay(1);});
    }
    void appointments(){
        title("Tu tiempo, organizado","Tus citas","Consulta cada mes y toca un día para ver sus citas.");
        List<JSONObject> all=vault.list("appointments");all.sort(Comparator.comparingLong(a->a.optLong("time")));
        LinearLayout calendar=card(content,Color.WHITE);calendar.setPadding(dp(8),dp(12),dp(8),dp(12));
        LinearLayout header=row();TextView previous=action("‹",()->moveMonth(-1),false);previous.setContentDescription("Mes anterior");header.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));
        String monthTitle=agendaMonth.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy",new Locale("es")));
        TextView heading=action(monthTitle,()->chooseMonth(),false);heading.setContentDescription("Elegir mes y año: "+monthTitle);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        TextView next=action("›",()->moveMonth(1),false);next.setContentDescription("Mes siguiente");header.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));add(calendar,header);gap(calendar,8);
        LinearLayout week=row();for(String day:new String[]{"L","M","X","J","V","S","D"}){TextView label=text(day,12,MUTED,true);label.setGravity(Gravity.CENTER);week.addView(label,new LinearLayout.LayoutParams(0,dp(28),1));}add(calendar,week);
        int offset=agendaMonth.atDay(1).getDayOfWeek().getValue()-1;
        for(int r=0;r<6;r++){LinearLayout days=row();for(int col=0;col<7;col++){
            int number=r*7+col-offset+1;LinearLayout cell=column();cell.setGravity(Gravity.CENTER);cell.setMinimumHeight(dp(60));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(dp(1),dp(1),dp(1),dp(1));days.addView(cell,lp);
            if(number<1||number>agendaMonth.lengthOfMonth())continue;
            java.time.LocalDate day=agendaMonth.atDay(number);List<JSONObject> dayItems=new ArrayList<>();for(JSONObject item:all)if(appointmentDay(item).equals(day))dayItems.add(item);
            boolean selected=day.equals(agendaDay);GradientDrawable background=bg(selected?LILAC:Color.WHITE,10);if(day.equals(java.time.LocalDate.now()))background.setStroke(dp(2),PURPLE);cell.setBackground(background);
            TextView label=text(String.valueOf(number),15,selected?PURPLE:INK,selected);label.setGravity(Gravity.CENTER);add(cell,label);
            String icons=dayItems.isEmpty()?"":AppointmentType.icon(dayItems.get(0));if(dayItems.size()>1)icons+=" +"+(dayItems.size()-1);
            TextView mark=text(icons,12,PURPLE,true);mark.setGravity(Gravity.CENTER);add(cell,mark);
            StringBuilder description=new StringBuilder(day.toString()+", "+dayItems.size()+" citas");for(JSONObject item:dayItems)description.append(". ").append(AppointmentType.NAMES[AppointmentType.type(item)]).append(": ").append(item.optString("title"));
            cell.setContentDescription(description);cell.setFocusable(true);cell.setOnClickListener(v->{agendaDay=day;render();});
        }add(calendar,days);}
        add(calendar,action("Hoy",()->{agendaDay=java.time.LocalDate.now();agendaMonth=java.time.YearMonth.from(agendaDay);render();},false));
        add(content,action("+ Agendar cita",()->appointmentForm(null),true));gap(content,16);
        section(agendaDay.format(java.time.format.DateTimeFormatter.ofPattern("d 'de' MMMM",new Locale("es"))),null,null);
        boolean any=false;for(JSONObject item:all)if(appointmentDay(item).equals(agendaDay)){appointmentCard(item,true);any=true;}
        if(!any)empty(card(content,Color.WHITE),"Día sin citas","Selecciona otro día o agenda una cita para esta fecha.");
        boolean allowed=getSystemService(NotificationManager.class).areNotificationsEnabled();LinearLayout note=card(content,0xfff8f7fa);
        add(note,text(allowed?"Recordatorios activados":"Activa tus recordatorios",15,INK,true));gap(note,7);
        add(note,text("Día anterior a las 18:00 · mismo día a las 08:00 · una hora antes. Android puede retrasar los avisos según batería.",12,MUTED,false));if(!allowed)add(note,action("Permitir notificaciones",()->enableNotifications(),false));
    }
    void appointmentCard(JSONObject a,boolean editable){LinearLayout c=card(content,Color.WHITE);add(c,text(date(a.optLong("time"),"EEE, d MMM · HH:mm"),12,PURPLE,true));gap(c,9);add(c,text(AppointmentType.icon(a)+"  "+a.optString("title"),19,INK,true));add(c,text(AppointmentType.NAMES[AppointmentType.type(a)],12,MUTED,false));gap(c,6);if(!a.optString("doctor").isEmpty())add(c,text(a.optString("doctor"),14,MUTED,false));if(!a.optString("place").isEmpty())add(c,text(a.optString("place"),13,MUTED,false));if(!a.optString("notes").isEmpty()){gap(c,8);add(c,text(a.optString("notes"),13,MUTED,false));}if(editable){gap(c,12);LinearLayout r=new AdaptiveRow(this);r.addView(action("Editar",()->appointmentForm(a),false),new LinearLayout.LayoutParams(0,-2,1));r.addView(action("Eliminar",()->confirmDelete("appointments",a),false),new LinearLayout.LayoutParams(0,-2,1));add(c,r);}}
    void enableNotifications(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);else startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()));}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==20){render();if(g.length>0&&g[0]!=PackageManager.PERMISSION_GRANTED)toast("Puedes activarlas en Ajustes de Android → Notificaciones.");}}
    LinearLayout form(){LinearLayout f=new AdaptiveColumn(this);f.setPadding(dp(24),dp(12),dp(24),dp(16));return f;}
    EditText field(LinearLayout f,String label,String value,int type){add(f,text(label,12,MUTED,true));EditText e=new EditText(this);e.setHintTextColor(MUTED);e.setTextSize(16);e.setTextColor(INK);e.setSingleLine((type&InputType.TYPE_TEXT_FLAG_MULTI_LINE)==0);e.setInputType(type);e.setText(value);e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(bg(0xfff6f3f9,10));e.setContentDescription(label);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(6);lp.bottomMargin=dp(16);f.addView(e,lp);return e;}
    interface Save {void run() throws Exception;}
    AlertDialog dialog(String title,LinearLayout f,Save save){ScrollView s=new ScrollView(this);s.addView(f);AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(s).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{save.run();d.dismiss();render();}catch(Exception e){toast(e.getMessage()==null?"No se pudo guardar. Intenta de nuevo.":e.getMessage());}}));d.show();return d;}
    String required(EditText e){String value=e.getText().toString().trim();if(value.isEmpty())throw new IllegalArgumentException("Completa los campos obligatorios.");return value;}
    void appointmentForm(JSONObject old){LinearLayout f=form();Spinner type=new Spinner(this);add(f,text("Tipo de cita",12,MUTED,true));type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,AppointmentType.NAMES));type.setSelection(old==null?0:old.optInt("appointmentType",0));add(f,type);gap(f,12);EditText title=field(f,"Motivo de la cita *",old==null?"":old.optString("title"),InputType.TYPE_CLASS_TEXT);EditText doctor=field(f,"Profesional",old==null?"":old.optString("doctor"),InputType.TYPE_CLASS_TEXT);EditText place=field(f,"Centro médico o dirección",old==null?"":old.optString("place"),InputType.TYPE_CLASS_TEXT);Calendar selected=Calendar.getInstance();selected.setTimeInMillis(old==null?agendaDay.atTime(9,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli():old.optLong("time"));selected.set(Calendar.SECOND,0);selected.set(Calendar.MILLISECOND,0);TextView day=action(date(selected.getTimeInMillis(),"dd MMM yyyy"),()->{},false);day.setOnClickListener(v->new DatePickerDialog(this,(picker,y,m,d)->{selected.set(y,m,d);day.setText(date(selected.getTimeInMillis(),"dd MMM yyyy"));},selected.get(Calendar.YEAR),selected.get(Calendar.MONTH),selected.get(Calendar.DAY_OF_MONTH)).show());add(f,day);gap(f,8);TextView hour=action(date(selected.getTimeInMillis(),"HH:mm"),()->{},false);hour.setOnClickListener(v->new TimePickerDialog(this,(picker,h,m)->{selected.set(Calendar.HOUR_OF_DAY,h);selected.set(Calendar.MINUTE,m);hour.setText(date(selected.getTimeInMillis(),"HH:mm"));},selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());add(f,hour);gap(f,12);EditText notes=field(f,"Preparación o notas",old==null?"":old.optString("notes"),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        dialog(old==null?"Nueva cita":"Editar cita",f,()->{if(selected.getTimeInMillis()<=System.currentTimeMillis())throw new IllegalArgumentException("Elige una fecha y hora futuras.");JSONObject a=old==null?Vault.item():new JSONObject(old.toString());a.put("appointmentType",type.getSelectedItemPosition()).put("title",required(title)).put("doctor",doctor.getText().toString().trim()).put("place",place.getText().toString().trim()).put("notes",notes.getText().toString().trim()).put("time",selected.getTimeInMillis());vault.put("appointments",a);agendaDay=appointmentDay(a);agendaMonth=java.time.YearMonth.from(agendaDay);Reminders.schedule(this,a);toast("Cita guardada");});}

    void body() {
        add(content,text("CONOCE TU CUERPO",11,PURPLE,true));gap(content,8);
        add(content,text("Tu mapa personal",32,INK,true));gap(content,24);
        LinearLayout controls=card(content,LILAC);
        Switch toggle=new Switch(this);
        toggle.setText("Combinar sistemas");toggle.setTextColor(INK);toggle.setTextSize(14);
        toggle.setMinHeight(dp(48));toggle.setChecked(combined);
        toggle.setOnCheckedChangeListener((button,value)->{
            combined=value;
            if(!value && systems.size()>1){int first=systems.iterator().next();systems.clear();systems.add(first);}
            render();
        });add(controls,toggle);
        add(controls,text("Selecciona las vistas que quieres explorar. Bio asocia los registros automáticamente.",12,MUTED,false));
        gap(controls,10);
        HorizontalScrollView systemScroll=new HorizontalScrollView(this);systemScroll.setHorizontalScrollBarEnabled(true);
        LinearLayout chips=row();
        for(int i=0;i<BodyView.SYSTEMS.length;i++){
            final int system=i;
            TextView chip=action((systems.contains(system)?"●  ":"○  ")+BodyView.SYSTEMS[system],()->selectSystem(system),systems.contains(system));
            chip.setTextSize(13);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.rightMargin=dp(8);chips.addView(chip,lp);
        }
        systemScroll.addView(chips);add(controls,systemScroll);gap(controls,8);
        add(controls,text("Desliza para ver los nueve sistemas.",11,MUTED,false));
        add(content,text("Amarillo · Incomodidad     Rojo · Dolor",13,PURPLE,true));gap(content,10);
        LinearLayout legend=column();
        add(legend,text("Amarillo · Incomodidad",14,0xff887000,true));
        add(legend,text("Rojo · Dolor",14,0xffc42b43,true));
        add(legend,text("La zona del cuerpo se ilumina intermitentemente. El color indica la sensación, no la gravedad. Los colores originales de las imágenes son decorativos.",12,MUTED,false));
        boolean animation=getPreferences(MODE_PRIVATE).getBoolean("bodyAnimation",true);
        Switch motion=new Switch(this);motion.setText("Animar iluminación");motion.setTextSize(13);motion.setTextColor(INK);
        motion.setMinHeight(dp(48));motion.setChecked(animation);add(legend,motion);
        boolean female=HealthProfile.female(HealthProfile.current(vault));
        BodyView body=new BodyView(this,female);body.selected.clear();body.selected.addAll(systems);
        body.setAnimated(animation);body.records(vault.list("records"));
        body.listener=system->{combined=false;systems.clear();systems.add(system);render();};
        motion.setOnCheckedChangeListener((button,value)->{getPreferences(MODE_PRIVATE).edit().putBoolean("bodyAnimation",value).apply();body.setAnimated(value);});
        addSized(content,body,520);gap(content,12);add(content,legend);
        gap(content,10);
        add(content,text("Ilustraciones de referencia, no estudios médicos personales. Asociaciones orientativas a partir del texto; pueden requerir corrección con más detalles.",12,MUTED,false));
        if(female&&systems.contains(6)){gap(content,6);add(content,text("La ilustración femenina del sistema óseo está pendiente y no se añade a la combinación.",11,MUTED,false));}
        gap(content,18);section("Registros asociados","+ Añadir",()->recordForm(null));
        boolean any=false;
        for(JSONObject record:vault.list("records")){
            if(!Collections.disjoint(systems,RecordAssociations.systems(record))){recordCard(record);any=true;}
        }
        if(!any)empty(card(content,Color.WHITE),"Aún no hay registros asociados","Describe lo que sientes. Bio propondrá los sistemas relacionados sin que tengas que elegirlos.");
        List<JSONObject> pending=new ArrayList<>();
        for(JSONObject record:vault.list("records"))if(RecordAssociations.systems(record).isEmpty())pending.add(record);
        if(!pending.isEmpty()){
            section("Necesitan más detalles",null,null);
            add(content,text("Indica en qué zona lo sientes, de qué lado, desde cuándo y qué lo provoca. No asignamos una zona cuando el texto no permite hacerlo.",13,MUTED,false));gap(content,12);
            for(JSONObject record:pending)recordCard(record);
        }
    }
    void selectSystem(int s){if(!combined){systems.clear();systems.add(s);}else if(systems.contains(s)){if(systems.size()>1)systems.remove(s);}else systems.add(s);render();}
    void showAssociatedSystems(JSONObject record){
        Set<Integer> related=RecordAssociations.systems(record);
        if(related.isEmpty()){toast("Añade más detalles para ubicar este registro.");return;}
        systems.clear();systems.addAll(related);combined=systems.size()>1;go(2);
    }
    void tabs(String[] labels,int selected,java.util.function.IntConsumer callback){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout r=row();for(int i=0;i<labels.length;i++){final int index=i;TextView chip=action(labels[i],()->callback.accept(index),i==selected);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.rightMargin=dp(8);r.addView(chip,p);}scroll.addView(r);add(content,scroll);gap(content,20);}
    void health(){title("Tu historia, conectada","Mi salud","Tus mediciones y documentos clínicos, organizados.");tabs(new String[]{"Signos vitales","Historia","Exámenes"},healthTab,i->{healthTab=i;render();});if(healthTab==0)vitalScreen();else documents(healthTab==1?"history":"exam");}

    List<JSONObject> vitals(int which){List<JSONObject> values=vault.list("vitals");values.removeIf(a->a.optInt("metric")!=which);values.sort(Comparator.comparingLong(a->a.optLong("created")));return values;}
    void vitalScreen(){metric=Math.max(0,Math.min(metric,METRICS.length-1));add(content,action("Conectar dispositivos",()->startActivity(new Intent(this,SourcesActivity.class)),false));gap(content,10);add(content,action("+  Registrar medición",()->vitalForm(),true));gap(content,18);Spinner select=new Spinner(this);select.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,METRICS));select.setSelection(metric);select.setMinimumHeight(dp(48));select.setContentDescription("Tipo de medición");select.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int pos,long id){if(metric!=pos){metric=pos;render();}}});add(content,select);gap(content,12);List<JSONObject> values=vitals(metric);LinearLayout chart=card(content,LILAC);add(chart,text(METRICS[metric],15,PURPLE,true));gap(chart,12);add(chart,text(values.isEmpty()?"—":values.get(values.size()-1).optString("value")+" "+UNITS[metric],34,INK,true));add(chart,text(values.isEmpty()?"Tu primera medición aparecerá aquí.":"Última: "+date(values.get(values.size()-1).optLong("created"),"dd MMM · HH:mm"),12,MUTED,false));if(!values.isEmpty()){ArrayList<Double> data=new ArrayList<>();for(JSONObject a:values.subList(Math.max(0,values.size()-12),values.size()))data.add(a.optDouble("value"));gap(chart,18);addSized(chart,new TrendView(this,data),100);gap(chart,10);add(chart,text("Últimos "+data.size()+" registros · orden cronológico\nEscala relativa; no indica rangos clínicos.",11,MUTED,false));}
        add(content,text("Registros manuales y mediciones autorizadas de Health Connect. La app no mide signos vitales con el teléfono.",12,MUTED,false));gap(content,18);section("Historial de mediciones",null,null);Collections.reverse(values);if(values.size()>200)add(content,text("Mostrando las 200 mediciones más recientes.",12,MUTED,false));for(JSONObject a:values.subList(0,Math.min(200,values.size()))){LinearLayout c=card(content,Color.WHITE);add(c,text(a.optString("value")+" "+UNITS[metric],19,INK,true));add(c,text(date(a.optLong("created"),"dd MMM yyyy · HH:mm")+" · "+a.optString("origin","Registro manual")+(a.has("sourcePackage")?"\n"+a.optString("sourcePackage"):""),12,MUTED,false));gap(c,8);add(c,action("Eliminar registro",()->confirmDelete("vitals",a),false));}}
    void vitalForm(){LinearLayout f=form();Spinner select=new Spinner(this);select.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,METRICS));select.setSelection(metric);add(f,select);gap(f,12);EditText value=field(f,"Valor *", "",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);TextView unit=text("Unidad: "+UNITS[metric],13,MUTED,true);add(f,unit);select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){unit.setText("Unidad: "+UNITS[pos]);}});gap(f,12);add(f,text("Introduce la lectura de tu dispositivo. Se guardará con la fecha y hora actuales.",13,MUTED,false));dialog("Nueva medición",f,()->{int m=select.getSelectedItemPosition();double n;try{n=Double.parseDouble(required(value).replace(',','.'));}catch(NumberFormatException e){throw new IllegalArgumentException("Introduce un número válido.");}if(!Double.isFinite(n)||n<=0||n>MAX_VALUES[m])throw new IllegalArgumentException("Revisa el valor y la unidad "+UNITS[m]+".");vault.put("vitals",Vault.item().put("metric",m).put("value",n).put("origin","Registro manual"));metric=m;});}
    AlertDialog recordForm(JSONObject old) {
        LinearLayout f=form();
        String[] kinds=old==null?new String[]{"Síntoma"}:new String[]{"Síntoma","Diagnóstico confirmado","Antecedente","Tratamiento"};
        Spinner kind=new Spinner(this);kind.setContentDescription("Tipo de registro");
        kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,kinds));
        if(old!=null)kind.setSelection(Math.max(0,Arrays.asList(kinds).indexOf(old.optString("kind"))));
        add(f,kind);gap(f,12);
        EditText title=field(f,"¿Qué sientes o quieres registrar? *",old==null?"":old.optString("title"),InputType.TYPE_CLASS_TEXT);
        title.setHint("Ej.: dolor en la rodilla izquierda");title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(200)});
        EditText notes=field(f,"Detalles: zona, lado, inicio y qué lo provoca",old==null?"":old.optString("notes"),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notes.setHint("Describe una sensación por registro para ubicarla mejor.");notes.setMinLines(3);notes.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3000)});
        LinearLayout symptomOptions=column();add(f,symptomOptions);
        add(symptomOptions,text("Sensación (Bio también puede leerla en tu descripción)",12,MUTED,true));
        String[] sensations={"Según descripción","Incomodidad","Dolor","Otro síntoma"};
        Spinner sensation=new Spinner(this);sensation.setContentDescription("Sensación descrita");
        sensation.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,sensations));
        if(old!=null)sensation.setSelection(Math.max(0,Arrays.asList(sensations).indexOf(old.optString("sensation","Según descripción"))));
        add(symptomOptions,sensation);
        CheckBox active=new CheckBox(this);active.setText("El síntoma sigue presente");active.setTextColor(INK);active.setChecked(old==null||old.optBoolean("active",true));add(symptomOptions,active);
        gap(f,12);TextView preview=text("",13,PURPLE,true);add(f,preview);
        gap(f,8);add(f,text("Asociación automática local y orientativa. No determina la causa ni confirma daño en órganos. Para cambiar una asociación, precisa la descripción.",12,MUTED,false));
        Runnable refresh=()->{
            boolean symptom=kind.getSelectedItemPosition()==0;symptomOptions.setVisibility(symptom?View.VISIBLE:View.GONE);
            SymptomAssociator.Result association=SymptomAssociator.associate(title.getText().toString(),notes.getText().toString(),symptom?sensations[sensation.getSelectedItemPosition()]:"Otro síntoma");
            String message="Sistemas posiblemente relacionados:\n"+association.labels();
            if(association.incomplete)message+="\nAñade ubicación y contexto para mejorar la asociación.";
            if(symptom&&association.urgent&&active.isChecked())message+="\nSi el dolor de pecho es nuevo o inexplicable, o hay dificultad para respirar, busca atención urgente. No esperes al mapa.";
            preview.setText(message);
        };
        title.addTextChangedListener(watcher(value->refresh.run()));notes.addTextChangedListener(watcher(value->refresh.run()));
        AdapterView.OnItemSelectedListener selection=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){refresh.run();}};
        kind.setOnItemSelectedListener(selection);sensation.setOnItemSelectedListener(selection);active.setOnCheckedChangeListener((button,value)->refresh.run());refresh.run();
        return dialog(old==null?"Nuevo registro":"Editar registro",f,()->{
            JSONObject record=old==null?Vault.item():new JSONObject(old.toString());
            record.put("title",required(title)).put("kind",kinds[kind.getSelectedItemPosition()]).put("notes",notes.getText().toString().trim());
            record.put("sensation",kind.getSelectedItemPosition()==0?sensations[sensation.getSelectedItemPosition()]:"Otro síntoma");
            record.put("active",kind.getSelectedItemPosition()==0&&active.isChecked());
            RecordAssociations.attach(record);vault.put("records",record);
            if(record.optBoolean("associationNeedsDetails"))toast("Registro guardado. Añade más detalles para ubicarlo mejor.");
            else toast("Registro guardado y asociado automáticamente.");
        });
    }
    void recordCard(JSONObject record) {
        LinearLayout c=card(content,Color.WHITE);
        add(c,text(record.optString("kind")+" · "+RecordAssociations.labels(record),11,PURPLE,true));gap(c,8);
        add(c,text(record.optString("title"),18,INK,true));
        if(!record.optString("notes").isEmpty()){gap(c,6);add(c,text(record.optString("notes"),13,MUTED,false));}
        SymptomAssociator.Result association=RecordAssociations.result(record);
        if("Síntoma".equals(record.optString("kind"))){
            gap(c,9);add(c,text(RecordAssociations.active(record)?"● Síntoma activo":"✓ Síntoma resuelto",12,RecordAssociations.active(record)?PURPLE:GREEN,true));
            Set<String> descriptions=new LinkedHashSet<>();
            for(SymptomAssociator.Signal signal:association.signals)descriptions.add((signal.sensation==SymptomAssociator.PAIN?"D · Dolor":signal.sensation==SymptomAssociator.DISCOMFORT?"I · Incomodidad":"Otro síntoma")+" · "+signal.region);
            for(String description:descriptions)add(c,text(description,12,MUTED,false));
            if(association.urgent&&RecordAssociations.active(record)){gap(c,8);add(c,text("Si el dolor de pecho es nuevo o inexplicable, o hay dificultad para respirar, busca atención urgente. El mapa no descarta una urgencia.",13,0xffad263e,true));}
        }
        gap(c,7);add(c,text("Asociación orientativa · "+date(record.optLong("created"),"dd MMM yyyy"),11,MUTED,false));
        if(association.incomplete){gap(c,6);add(c,text("Faltan detalles de ubicación o contexto. Edita la descripción para precisarlos.",12,MUTED,false));}
        gap(c,10);LinearLayout r=row();
        r.addView(action("Editar",()->recordForm(record),false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(action("Eliminar",()->confirmDelete("records",record),false),new LinearLayout.LayoutParams(0,-2,1));add(c,r);
        if(!RecordAssociations.systems(record).isEmpty()){gap(c,8);add(c,action("Ver en mi cuerpo",()->showAssociatedSystems(record),false));}
    }
    void chooseDocument(String category,boolean analyze){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/pdf","image/jpeg","image/png"});
        startActivityForResult(i,30+("history".equals(category)?1:0)+(analyze?2:0));
    }
    void importDocument(String category){if(!busy)chooseDocument(category,true);}
    DocumentPipeline pipeline(){return new DocumentPipeline(vault,(doc,bytes)->{
        if(cloud==null)throw new IOException("Servicio no disponible");
        return DocumentPipeline.request(connection().getString("url"),cloud.idToken(),doc,bytes,HealthProfile.clinical(vault));
    },doc->{if(cloud==null)throw new IOException("Servicio no disponible");cloud.upload(vault,doc);});}
    void documents(String category){
        add(content,action("Fuentes de documentos",()->startActivity(new Intent(this,SourcesActivity.class)),false));gap(content,10);
        boolean history="history".equals(category);
        add(content,action(busy?"Procesando…":history?"+ Subir historia clínica":"+ Subir examen",()->importDocument(category),true));gap(content,12);
        add(content,text("PDF, JPG o PNG · hasta 10 MB. Orden: fecha clínica, de más reciente a más antigua. Los documentos sin fecha identificada aparecen al final.",12,MUTED,false));
        if(!cloudStatus.isEmpty())add(content,text(cloudStatus,12,MUTED,false));gap(content,12);
        List<JSONObject> all=vault.list("documents");all.removeIf(doc->!ClinicalDocuments.category(doc).equals(category));ClinicalDocuments.sort(all);
        if(all.isEmpty())empty(card(content,Color.WHITE),history?"Tu historia clínica":"Tus exámenes",history?"Sube informes de consultas, epicrisis e historias clínicas.":"Sube resultados de laboratorio e informes de estudios.");
        for(JSONObject a:all){LinearLayout c=card(content,Color.WHITE);
            if(DocumentPipeline.pending(a)){add(c,text("Preparando documento",17,INK,true));gap(c,8);add(c,text(a.optString("importError","Identificando el contenido, nombre y fecha…"),13,MUTED,false));gap(c,8);add(c,action("Reintentar carga",()->retryImport(a),false));add(c,action("Eliminar",()->confirmDelete("documents",a),false));continue;}
            add(c,text(history?"HISTORIA CLÍNICA":"EXAMEN",10,PURPLE,true));gap(c,8);add(c,text(a.optString("name"),17,INK,true));
            java.time.LocalDate clinical=ClinicalDocuments.clinicalDate(a);add(c,text(clinical==null?"Fecha clínica no identificada":clinical.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy",new Locale("es"))),12,PURPLE,true));
            add(c,text("Cargado: "+date(a.optLong("created"),"dd MMM yyyy")+" · "+String.format(Locale.US,"%.1f MB",a.optLong("size")/1048576.0),11,MUTED,false));
            add(c,text("synced".equals(a.optString("cloudState"))?"Guardado":"Guardado en este dispositivo · respaldo pendiente",11,MUTED,false));JSONObject analysis=a.optJSONObject("analysis");
            if(analysis!=null){documentAnalysis(c,analysis);
            }else add(c,text("pending".equals(a.optString("analysisState","pending"))?"Pendiente de análisis":"El análisis no se completó. El original está guardado.",12,MUTED,false));
            gap(c,10);add(c,action("Abrir original",()->openDocument(a),false));gap(c,6);add(c,action("Corregir nombre, fecha o carpeta",()->editDocument(a),false));gap(c,6);add(c,action("Eliminar",()->confirmDelete("documents",a),false));
        }
    }
    void analysisHeading(LinearLayout parent,String title){
        gap(parent,18);TextView heading=text(title,16,PURPLE,true);
        if(Build.VERSION.SDK_INT>=28)heading.setAccessibilityHeading(true);
        add(parent,heading);gap(parent,8);
    }
    void documentAnalysis(LinearLayout parent,JSONObject analysis){
        analysisHeading(parent,"Resumen");
        String summary=analysis.optString("summary","").trim();
        add(parent,text(summary.isEmpty()?"No se generó un resumen para este documento.":summary,14,INK,false));

        analysisHeading(parent,"Resultados");
        JSONArray findings=analysis.optJSONArray("findings");int count=0;
        if(findings!=null)for(int n=0;n<findings.length();n++){
            JSONObject finding=findings.optJSONObject(n);if(finding==null)continue;count++;
            LinearLayout item=column();item.setPadding(dp(14),dp(12),dp(14),dp(12));item.setBackground(bg(LILAC,14));
            String label=finding.optString("label","").trim();
            add(item,text(count+". "+(label.isEmpty()?"Hallazgo del documento":label),14,INK,true));
            String state=finding.optString("status");
            String qualifier="suspected".equals(state)?"Sospecha documentada":"negated".equals(state)?"Negado en el documento":"family_history".equals(state)?"Antecedente familiar":"past_history".equals(state)?"Antecedente personal":"";
            if(!qualifier.isEmpty()){gap(item,4);add(item,text(qualifier,12,PURPLE,true));}
            String quote=finding.optString("quote","").trim();
            if(!quote.isEmpty()){gap(item,6);add(item,text("“"+quote+"”",13,INK,false));}
            int page=finding.optInt("page",0);if(page>0){gap(item,6);add(item,text("Página "+page+" del original",11,MUTED,false));}
            add(parent,item);gap(parent,8);
        }
        if(count==0)add(parent,text("No se identificaron hallazgos explícitos en el documento.",13,MUTED,false));
        JSONArray possible=analysis.optJSONArray("possible_interpretations");
        if(possible!=null&&possible.length()>0){
            gap(parent,6);add(parent,text("Interpretación orientativa",14,PURPLE,true));gap(parent,4);
            add(parent,text("Posibilidades para revisar con un profesional; no son diagnósticos confirmados.",12,MUTED,false));
            for(int n=0;n<possible.length();n++){String value=possible.optString(n,"").trim();if(value.isEmpty())continue;gap(parent,8);add(parent,text(value,13,INK,false));}
        }
        String limitations=AnalysisText.uniqueParagraphs(analysis.optString("limitations",""));
        if(!limitations.isEmpty()){
            analysisHeading(parent,"Limitaciones");
            TextView details=text(limitations,13,MUTED,false);
            // Keep the complete source text available without filling the card with repeated notices.
            boolean longText=limitations.length()>240;
            if(longText){
                add(parent,text("Consulta las condiciones y los límites de este análisis.",12,MUTED,false));
                details.setVisibility(View.GONE);
                TextView toggle=action("Ver limitaciones",()->{},false);
                toggle.setOnClickListener(v->{boolean show=details.getVisibility()!=View.VISIBLE;details.setVisibility(show?View.VISIBLE:View.GONE);toggle.setText(show?"Ocultar limitaciones":"Ver limitaciones");});
                gap(parent,8);add(parent,toggle);gap(parent,8);
            }
            add(parent,details);
        }
    }
    void uploadPendingDocument(JSONObject doc){try{if(cloud==null)throw new IOException();cloud.upload(vault,doc);}catch(Exception e){cloudStatus="Respaldo pendiente. Se reintentará automáticamente.";}}
    void deleteClinicalDocument(JSONObject doc){if(busy||automaticRunning||SourceSync.running){toast("Espera a que termine la carga en curso.");return;}new AlertDialog.Builder(this).setTitle("Eliminar documento").setMessage("Se eliminará el original y su análisis de este dispositivo y de Firebase si estaba sincronizado.").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{busy=true;render();executor.execute(()->{String message;try{if(!doc.optString("cloudOwner").isEmpty()){if(cloud==null)throw new IOException();cloud.ensureSession();cloud.delete(vault,doc);}vault.remove("documents",doc.getString("id"));vault.deleteDocument(doc.getString("id"));message="Documento eliminado";}catch(Exception e){message="No se pudo eliminar. Revisa la conexión y reintenta.";}String status=message;runOnUiThread(()->{busy=false;toast(status);render();});});}).show();}
    void editDocument(JSONObject original){if(busy||automaticRunning||SourceSync.running){toast("Espera a que termine la carga en curso.");return;}LinearLayout f=form();EditText name=field(f,"Nombre",original.optString("name"),InputType.TYPE_CLASS_TEXT);EditText day=field(f,"Fecha clínica (AAAA-MM-DD, vacía si desconocida)",original.optString("clinicalDate",""),InputType.TYPE_CLASS_TEXT);Spinner folder=new Spinner(this);folder.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Exámenes","Historia clínica"}));folder.setSelection("history".equals(ClinicalDocuments.category(original))?1:0);add(f,folder);
        dialog("Revisar documento",f,()->{String value=day.getText().toString().trim();if(!value.isEmpty()){try{java.time.LocalDate parsed=java.time.LocalDate.parse(value);if(parsed.isAfter(java.time.LocalDate.now()))throw new IllegalArgumentException();}catch(Exception e){throw new IllegalArgumentException("Usa una fecha válida AAAA-MM-DD que no sea futura.");}}JSONObject updated=new JSONObject(original.toString());updated.put("name",required(name)).put("clinicalDate",value).put("category",folder.getSelectedItemPosition()==1?"history":"exam").put("metadataReviewed",true).put("cloudState","pending");vault.put("documents",updated);});
    }
    void retryImport(JSONObject doc){if(busy||automaticRunning)return;try{doc.put("retryAt",0);doc.remove("importError");vault.put("documents",doc);automaticDocuments();}catch(Exception e){toast("No se pudo reanudar la carga.");}}
    void automaticDocuments(){
        if(cloud==null||!cloud.account())return;
        if(vault==null||busy||automaticRunning||!AuthSession.unlocked()||isFinishing()||isDestroyed())return;
        List<JSONObject> docs=vault.list("documents");long now=System.currentTimeMillis();
        if(!AuthSession.restore&&!"pending".equals(HealthProfile.current(vault).optString("cloudState"))&&docs.stream().noneMatch(d->DocumentPipeline.pending(d)?d.optLong("retryAt")<=now:!"synced".equals(d.optString("cloudState"))))return;
        automaticRunning=true;
        executor.execute(()->{try{if((AuthSession.restore||"pending".equals(HealthProfile.current(vault).optString("cloudState")))&&cloud!=null){try{cloud.syncProfile(vault);cloud.sync(vault);AuthSession.restore=false;}catch(Exception ignored){cloudStatus="No se completó el respaldo. Se reintentará automáticamente.";}}for(JSONObject doc:vault.list("documents")){
            if(Thread.currentThread().isInterrupted())break;
            if(DocumentPipeline.pending(doc)&&doc.optLong("retryAt")<=System.currentTimeMillis()){try{pipeline().process(doc);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}catch(Exception failure){/* Error persisted on the pending card. */}}
            else if(!DocumentPipeline.pending(doc)&&!"synced".equals(doc.optString("cloudState")))uploadPendingDocument(doc);
        }}finally{runOnUiThread(()->{automaticRunning=false;if(!isDestroyed()){render();}});}});
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(request==90){authOpening=false;if(result!=RESULT_OK||!AuthSession.unlocked())finish();return;}if(request<30||request>33||result!=RESULT_OK||intent==null||intent.getData()==null)return;Uri uri=intent.getData();String category=request%2==1?"history":"exam";busy=true;cloudStatus="Identificando el contenido y preparando el documento…";render();executor.execute(()->{String message;try{
        String mime=getContentResolver().getType(uri);String name="Documento";try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())name=c.getString(0);}byte[] bytes;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("No se pudo leer el archivo");bytes=readLimited(in,10*1024*1024);}
        JSONObject doc=pipeline().stage(name,category,mime,bytes);
        runOnUiThread(()->{if(!isDestroyed())render();});
        try{JSONObject ready=pipeline().process(doc);message="Documento cargado: "+ready.optString("name");}catch(Exception e){message=DocumentPipeline.message(e);}
        }catch(Exception e){message="No se pudo importar el archivo. Comprueba que sea un PDF, JPG o PNG de hasta 10 MB.";}String status=message;runOnUiThread(()->{busy=false;cloudStatus="";if(!isDestroyed()){toast(status);render();}});});}
    static byte[] readLimited(InputStream in,int limit) throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("El archivo o respuesta supera el tamaño permitido");out.write(buffer,0,n);}return out.toByteArray();}
    void openDocument(JSONObject a){Uri uri=Uri.parse("content://com.alvia.health.documents/"+a.optString("id"));try{Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,a.optString("mime")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);view.setClipData(ClipData.newRawUri("Examen",uri));startActivity(view);}catch(ActivityNotFoundException e){toast("Instala un lector compatible para abrir este examen.");}}
    void confirmDelete(String collection,JSONObject a){if("documents".equals(collection)){deleteClinicalDocument(a);return;}new AlertDialog.Builder(this).setTitle("¿Eliminar este registro?").setMessage("Se eliminará de este dispositivo. Esta acción no se puede deshacer.").setNegativeButton("Conservar",null).setPositiveButton("Eliminar",(d,w)->{try{if(collection.equals("vitals")&&a.optString("id").startsWith("hc-"))vault.put("deletedVitals",new JSONObject().put("id",a.getString("id")));vault.remove(collection,a.optString("id"));if(collection.equals("appointments"))Reminders.cancel(this,a.optString("id"));if(collection.equals("documents"))vault.deleteDocument(a.optString("id"));render();}catch(Exception e){toast("No se pudo eliminar el registro.");}}).show();}

    void research(){title("Información para cuidarte","Una mirada más clara","Investiga tu salud con evidencia y prepara tu próxima consulta.");LinearLayout c=card(content,LILAC);add(c,text("✧  INVESTIGACIÓN CON IA",11,PURPLE,true));gap(c,10);add(c,text("Mejores preguntas.\nDecisiones informadas.",24,INK,true));gap(c,10);add(c,text("Explora tratamientos, evidencia reciente y centros de atención. Las respuestas incluyen fuentes y requieren revisión profesional.",14,MUTED,false));
        EditText query=field(content,"¿Qué quieres investigar? *",aiQuery,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);query.setHint("Ej.: opciones de tratamiento para mi diagnóstico");EditText city=field(content,"Ciudad y país para buscar atención",aiCity,InputType.TYPE_CLASS_TEXT);city.setHint("Ej.: Bogotá, Colombia");
        query.addTextChangedListener(watcher(s->aiQuery=s));city.addTextChangedListener(watcher(s->aiCity=s));CheckBox include=new CheckBox(this);include.setText("Incluir mis registros y mediciones");include.setTextSize(13);include.setTextColor(INK);add(content,include);CheckBox files=new CheckBox(this);files.setText("Incluir exámenes e historia clínica (máximo 10 MB)");files.setTextSize(13);files.setTextColor(INK);add(content,files);gap(content,12);add(content,action(busy?"Investigando…":"✧  Investigar con fuentes",()->{if(busy)return;String q=query.getText().toString().trim();if(q.isEmpty()){toast("Escribe qué deseas investigar.");return;}beginResearch(q,city.getText().toString().trim(),include.isChecked(),files.isChecked());},true));gap(content,12);add(content,text("La IA puede equivocarse. No confirma diagnósticos ni garantiza curas. No uses esta función para urgencias.",12,MUTED,false));gap(content,18);if(!aiStatus.isEmpty())add(content,text(aiStatus,14,PURPLE,true));if(!aiResult.isEmpty()){LinearLayout answer=card(content,Color.WHITE);add(answer,text("Resultado de la investigación",19,INK,true));gap(answer,12);TextView body=text(aiResult,15,INK,false);body.setTextIsSelectable(true);Linkify.addLinks(body,Linkify.WEB_URLS);body.setMovementMethod(LinkMovementMethod.getInstance());add(answer,body);gap(answer,16);add(answer,text("Fuentes consultadas",14,PURPLE,true));for(int i=0;i<aiSources.length();i++){JSONObject source=aiSources.optJSONObject(i);if(source==null)continue;String url=source.optString("url");if(!url.startsWith("https://"))continue;gap(answer,8);add(answer,action(source.optString("title",url),()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("No hay navegador disponible.");}},false));}}}
    android.text.TextWatcher watcher(java.util.function.Consumer<String> change){return new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){change.accept(s.toString());}public void afterTextChanged(android.text.Editable e){}};}
    void beginResearch(String query,String city,boolean include,boolean files){
        if(busy)return;busy=true;
        executor.execute(()->{try{
            JSONObject plan=new MailApi(this,cloud).post("/billing/status",new JSONObject());
            runOnUiThread(()->{busy=false;if(isFinishing()||isDestroyed())return;if(plan.optBoolean("premium"))beginPremiumResearch(query,city,include,files);else startActivity(new Intent(this,PremiumActivity.class));});
        }catch(Exception e){runOnUiThread(()->{busy=false;toast("No se pudo verificar tu suscripción. Inténtalo de nuevo.");});}});
    }
    void beginPremiumResearch(String query,String city,boolean include,boolean files){
        JSONObject settings=connection();String endpoint=settings.optString("url");if(endpoint.isEmpty()){settings();return;}
        String summary="Se enviarán a tu servidor de IA y a "+aiProvider()+":\n• Tu pregunta, ubicación escrita y perfil de salud (edad, estatura, peso y grupo sanguíneo)"+(include?"\n• "+vault.list("records").size()+" registros y "+vault.list("vitals").size()+" mediciones":"")+(files?"\n• "+vault.list("documents").size()+" documentos clínicos completos":"")+"\n\nLos datos clínicos incluidos también pueden influir en las búsquedas web. Evita nombres, identificaciones y otros datos innecesarios.\n\nDestino: "+endpoint;
        new AlertDialog.Builder(this).setTitle("Revisar envío de datos").setMessage(summary).setNegativeButton("Cancelar",null).setPositiveButton("Enviar e investigar",(d,w)->runResearch(query,city,include,files,settings)).show();}
    JSONObject connection(){try(InputStream in=getAssets().open("backend-config.json")){return new JSONObject(new String(readLimited(in,16384),StandardCharsets.UTF_8));}catch(Exception absent){try{List<JSONObject> list=vault.list("connection");return list.isEmpty()?new JSONObject():list.get(0);}catch(Exception e){return new JSONObject();}}}
    String aiToken(JSONObject config) throws Exception {if("firebase".equals(config.optString("auth"))){if(cloud==null)throw new IOException("No se pudo preparar la conexión de Bio.");return cloud.idToken();}return config.optString("token");}
    String aiProvider(){return "firebase".equals(connection().optString("auth"))?"Google Cloud (Gemini)":"OpenAI";}
    void runResearch(String query,String city,boolean include,boolean files,JSONObject settings){busy=true;aiStatus="Buscando evidencia. Puede tardar unos minutos…";aiResult="";aiSources=new JSONArray();render();executor.execute(()->{String result="",status;JSONArray sources=new JSONArray();HttpURLConnection http=null;try{JSONObject request=new JSONObject().put("query",query).put("location",city).put("profile",HealthProfile.clinical(vault));if(include){request.put("records",new JSONArray(vault.list("records")));java.util.List<JSONObject> selectedVitals=vault.list("vitals");selectedVitals.sort(Comparator.comparingLong(v->v.optLong("created")));request.put("vitals",new JSONArray(selectedVitals.subList(Math.max(0,selectedVitals.size()-2000),selectedVitals.size())));}if(files){JSONArray docs=new JSONArray();long total=0;for(JSONObject doc:vault.list("documents")){total+=doc.optLong("size");if(total>10*1024*1024)throw new IOException("Los exámenes superan 10 MB en total. Envía la consulta sin adjuntos.");byte[] bytes=vault.readDocument(doc.optString("id"));docs.put(new JSONObject().put("name",doc.optString("name")).put("mime",doc.optString("mime")).put("data",android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP)));}request.put("documents",docs);}
            URL endpoint=new URL(settings.getString("url")+"/research");http=(HttpURLConnection)endpoint.openConnection();http.setConnectTimeout(20000);http.setReadTimeout(240000);http.setInstanceFollowRedirects(false);http.setRequestMethod("POST");http.setRequestProperty("Content-Type","application/json");http.setRequestProperty("Authorization","Bearer "+aiToken(settings));http.setDoOutput(true);byte[] payload=request.toString().getBytes(StandardCharsets.UTF_8);http.setFixedLengthStreamingMode(payload.length);try(OutputStream out=http.getOutputStream()){out.write(payload);}int code=http.getResponseCode();if(code==429||code==413){String error="La solicitud supera el cupo o la extensión permitida.";try(InputStream in=http.getErrorStream()){if(in!=null)error=new JSONObject(new String(readLimited(in,65536),StandardCharsets.UTF_8)).optString("error",error);}catch(Exception ignored){}throw new IOException(error);}if(code!=200)throw new IOException(code==402?"La IA requiere una suscripción Bio Premium activa.":code==401?"Tu sesión no es válida. Vuelve a iniciar sesión en Bio.":code==503?"El servidor no tiene la IA configurada.":"El servicio no completó la solicitud ("+code+"). Intenta de nuevo.");JSONObject response;try(InputStream in=http.getInputStream()){response=new JSONObject(new String(readLimited(in,2*1024*1024),StandardCharsets.UTF_8));}result=response.optString("answer");sources=response.optJSONArray("sources");if(result.trim().isEmpty())throw new IOException("La IA no devolvió un informe. Intenta de nuevo.");status="Investigación completada · "+response.optString("model","IA");
        }catch(Exception e){status=e instanceof IOException?e.getMessage():"No se pudo completar la investigación. Revisa la conexión.";}finally{if(http!=null)http.disconnect();}String answer=result,finalStatus=status;JSONArray finalSources=sources==null?new JSONArray():sources;runOnUiThread(()->{busy=false;aiResult=answer;aiStatus=finalStatus;aiSources=finalSources;render();});});}
    AlertDialog settings(){return new AlertDialog.Builder(this).setTitle("Tu cuenta").setItems(new String[]{"Activar huella o rostro","Desactivar acceso biométrico","Cerrar sesión","Mi perfil de salud","Fuentes y dispositivos","Política de privacidad","Eliminar cuenta"},(dialog,which)->{
        if(busy||automaticRunning||SourceSync.running){toast("Espera a que termine la carga en curso.");return;}
        if(which==5){try{AuthSession.beginExternalFlow();startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(AccountDeletionActivity.PRIVACY)));}catch(Exception e){toast("No hay navegador disponible.");}return;}
        if(which==6){startActivity(new Intent(this,AccountDeletionActivity.class));return;}
        if(which==3){AuthSession.profileAccess=true;startActivity(new Intent(this,ProfileActivity.class));return;}if(which==4){startActivity(new Intent(this,SourcesActivity.class));return;}if(which==1){new BiometricAccess(this).clear();toast("Acceso biométrico desactivado");return;}
        if(which==2){new BiometricAccess(this).clear();if(cloud!=null)cloud.signOut();AuthSession.restore=false;}
        AuthSession.lock();render();authOpening=true;startActivityForResult(new Intent(this,AuthActivity.class).putExtra("biometricSetup",which==0),90);
    }).setNegativeButton("Cerrar",null).show();}
    void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
