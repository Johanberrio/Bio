package com.alvia.health;
import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.util.*;
public class ProfileActivity extends Activity {
    private boolean dark;private int purple,ink,muted,surface,lilac,border;
    private Vault vault;private FirebaseCloud cloud;private String birth="";private boolean saving;
    @Override public void onCreate(Bundle state){
        dark=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("darkMode",false);
        setTheme(dark?R.style.Theme_Bio_Dark:R.style.Theme_Bio);
        purple=dark?0xffc5a6ff:0xff7550b8;ink=dark?0xfff4effa:0xff282335;muted=dark?0xffb8aec6:0xff80778e;
        surface=dark?0xff201b28:Color.WHITE;lilac=dark?0xff2e253f:0xfff2edf9;border=dark?0xff3b3248:0xffeeeaf2;
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if(!AuthSession.unlocked()&&!AuthSession.profileAccess){finish();return;}
        try{vault=new Vault(this);cloud=FirebaseCloud.create(this);if(!cloud.account()){finish();return;}}catch(Exception e){finish();return;}
        AuthSession.profileAccess=false;
        JSONObject previous=HealthProfile.current(vault);birth=previous.optString("birthDate");
        ScrollView scroll=new ScrollView(this);LinearLayout form=new AdaptiveColumn(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(24),dp(24),dp(24),dp(24));scroll.setBackgroundColor(dark?0xff141119:Color.WHITE);scroll.addView(form);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});
        else scroll.setFitsSystemWindows(true);
        scroll.requestApplyInsets();
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.bio_header_transparent);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("Bio");header.addView(logo,new LinearLayout.LayoutParams(dp(76),dp(60)));
        header.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1f));
        TextView back=new TextView(this);back.setText("‹  Volver");styleButton(back,false);back.setOnClickListener(v->finish());header.addView(back);form.addView(header);
        label(form,"HECHO A TU MEDIDA",11,purple);
        label(form,"Tu perfil de salud",32,ink);
        label(form,"Tus datos, para conocerte mejor y acompañar tu cuidado.",15,muted);
        LinearLayout identity=card(form,"SOBRE TI","Tus datos personales");
        EditText name=field(identity,"Nombre completo",previous.optString("fullName"),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        label(identity,"Ilustraciones de mi cuerpo",14,ink);
        Spinner bodyModel=new Spinner(this);bodyModel.setContentDescription("Ilustraciones de mi cuerpo");bodyModel.setAdapter(choices(HealthProfile.BODY_LABELS));bodyModel.setSelection(Math.max(0,Arrays.asList(HealthProfile.BODY_VALUES).indexOf(previous.optString("bodyModel"))));styleSpinner(bodyModel);identity.addView(bodyModel);
        label(identity,"Esta elección personaliza las ilustraciones del Inicio y tu mapa corporal.",13,muted);
        TextView date=new TextView(this);date.setText(birth.isEmpty()?"Fecha de nacimiento":birth);styleButton(date,false);identity.addView(date,new LinearLayout.LayoutParams(-1,-2));date.setOnClickListener(v->{LocalDate d=birth.isEmpty()?LocalDate.of(2000,1,1):LocalDate.parse(birth);DatePickerDialog picker=new DatePickerDialog(this,(view,y,m,day)->{birth=LocalDate.of(y,m+1,day).toString();date.setText(birth);},d.getYear(),d.getMonthValue()-1,d.getDayOfMonth());picker.getDatePicker().setMaxDate(System.currentTimeMillis());picker.show();});
        LinearLayout measures=card(form,"TU CUERPO","Peso y estatura");
        EditText height=field(measures,"Estatura (cm)",previous.has("heightCm")?previous.optString("heightCm"):"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText weight=field(measures,"Peso (kg)",previous.has("weightKg")?previous.optString("weightKg"):"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        label(measures,"El peso se actualiza automáticamente con las mediciones autorizadas de Health Connect. Al sincronizar, la última lectura disponible de Health Connect sustituye el peso manual.",13,muted);
        LinearLayout medical=card(form,"DATOS DE SALUD","Grupo sanguíneo");
        label(medical,"Si lo desconoces, elige No lo sé.",13,muted);Spinner blood=new Spinner(this);blood.setAdapter(choices(HealthProfile.BLOOD));blood.setSelection(Math.max(0,Arrays.asList(HealthProfile.BLOOD).indexOf(previous.optString("bloodGroup"))));styleSpinner(blood);medical.addView(blood);
        TextView error=new TextView(this);error.setTextColor(0xffa12e46);form.addView(error);TextView save=new TextView(this);save.setText("Guardar y continuar");styleButton(save,true);form.addView(save,new LinearLayout.LayoutParams(-1,-2));
        save.setOnClickListener(v->{if(saving)return;try{JSONObject p=new JSONObject().put("id","main").put("fullName",name.getText().toString().trim()).put("bodyModel",HealthProfile.BODY_VALUES[bodyModel.getSelectedItemPosition()]).put("birthDate",birth).put("heightCm",Double.parseDouble(height.getText().toString().replace(',','.'))).put("weightKg",Double.parseDouble(weight.getText().toString().replace(',','.'))).put("bloodGroup",blood.getSelectedItem().toString()).put("owner",cloud.owner()).put("updatedAt",System.currentTimeMillis()).put("cloudState","pending");HealthProfile.validate(p);
            synchronized(SourceSync.LOCK){
                JSONObject latest=HealthProfile.current(vault);
                if(Double.compare(p.optDouble("weightKg"),previous.optDouble("weightKg"))!=0){p.put("weightMeasuredAt",System.currentTimeMillis()).put("weightOrigin","Registro manual");}
                else {for(String key:new String[]{"weightKg","weightMeasuredAt","weightOrigin","weightRecord","weightPackage"})if(latest.has(key))p.put(key,latest.get(key));}
                vault.put("profile",p);
            }saving=true;save.setEnabled(false);error.setText("Guardando perfil…");new Thread(()->{try{cloud.syncProfile(vault);}catch(Exception ignored){}runOnUiThread(()->{if(!isDestroyed()&&!isFinishing()){setResult(RESULT_OK);finish();}});},"profile-save").start();}catch(NumberFormatException e){error.setText("Introduce estatura y peso con números válidos.");}catch(Exception e){error.setText(e.getMessage());}});
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable rounded(int color){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(18));bg.setStroke(dp(1),border);return bg;}
    private TextView label(LinearLayout parent,String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(size>=20?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setPadding(0,dp(8),0,dp(10));parent.addView(t);return t;}
    private LinearLayout card(LinearLayout parent,String eyebrow,String title){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(20),dp(14),dp(20),dp(20));card.setBackground(rounded(surface));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(18);lp.bottomMargin=dp(8);parent.addView(card,lp);label(card,eyebrow,11,purple);label(card,title,21,ink);return card;}
    private void styleButton(TextView b,boolean primary){BioButton.style(b,primary?purple:lilac,primary?(dark?0xff21132f:Color.WHITE):purple);}
    private ArrayAdapter<String> choices(String[] values){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,values){
        private View styled(View view,boolean dropdown){TextView label=(TextView)view;label.setTextColor(ink);label.setTextSize(14);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));label.setMinHeight(dp(48));label.setGravity(Gravity.CENTER_VERTICAL);label.setPadding(dp(12),dp(10),dp(12),dp(10));if(dropdown)label.setBackgroundColor(surface);return label;}
        @Override public View getView(int position,View convert,ViewGroup parent){return styled(super.getView(position,convert,parent),false);}
        @Override public View getDropDownView(int position,View convert,ViewGroup parent){return styled(super.getDropDownView(position,convert,parent),true);}
    };}
    private void styleSpinner(Spinner spinner){spinner.setBackgroundTintList(null);spinner.setMinimumHeight(dp(48));spinner.setBackground(rounded(lilac));spinner.setPadding(dp(12),dp(6),dp(12),dp(6));}
    private EditText field(LinearLayout form,String hint,String value,int type){label(form,hint,14,ink);EditText e=new EditText(this);e.setContentDescription(hint);e.setInputType(type);e.setText(value);e.setTextColor(ink);e.setHintTextColor(muted);e.setSingleLine(true);e.setMinHeight(dp(48));e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setBackgroundTintList(null);e.setBackground(rounded(lilac));form.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
    @Override protected void onStop(){super.onStop();if(!isChangingConfigurations())finish();}
}
