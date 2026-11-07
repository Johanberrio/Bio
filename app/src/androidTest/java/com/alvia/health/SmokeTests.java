package com.alvia.health;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import org.json.JSONObject;
import java.io.*;
import java.util.Arrays;

/** Self-contained emulator smoke suite; no external testing dependency or clinical data. */
public class SmokeTests extends Instrumentation {
    int checks=0;
    void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();String id=null,docId=null;Vault vault=null;MainActivity activity=null;
        try {
            AuthSession.unlock();Context target=getTargetContext();vault=new Vault(target);
            PipelineChecks.run(vault,this);
            ProfileSourceChecks.run(vault,this);
            MailChecks.run(this);
            JSONObject record=Vault.item().put("kind","Síntoma").put("title","Prueba técnica").put("system",0).put("notes","Dato sintético de prueba");id=record.getString("id");
            vault.put("records",record);check(new Vault(target).list("records").stream().anyMatch(r->r.optString("id").equals(record.optString("id"))),"persist across reopen");
            record.put("notes","Modificado");vault.put("records",record);check(new Vault(target).list("records").stream().filter(r->r.optString("id").equals(record.optString("id"))).count()==1,"update without duplication");
            byte[] plain="private-test-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);byte[] encrypted=vault.encrypt(plain);check(!Arrays.equals(plain,encrypted),"encrypted at rest");check(Arrays.equals(plain,vault.decrypt(encrypted)),"decryption roundtrip");encrypted[encrypted.length-1]^=1;boolean rejected=false;try{vault.decrypt(encrypted);}catch(Exception e){rejected=true;}check(rejected,"authentication tag rejects tampering");
            JSONObject doc=Vault.item().put("name","test.pdf").put("mime","application/pdf").put("size",plain.length);docId=doc.getString("id");vault.storeDocument(docId,plain);vault.put("documents",doc);check(Arrays.equals(plain,vault.readDocument(docId)),"document roundtrip");
            try(android.os.ParcelFileDescriptor pfd=target.getContentResolver().openFileDescriptor(android.net.Uri.parse("content://com.alvia.health.documents/"+docId),"r");FileInputStream in=new FileInputStream(pfd.getFileDescriptor())){check(in.read()==plain[0],"document provider readable");}
            vault.remove("documents",docId);vault.deleteDocument(docId);docId=null;vault.remove("records",id);id=null;
            activity=(MainActivity)startActivitySync(new Intent(target,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();MainActivity app=activity;
            for(int screen=0;screen<5;screen++){int tab=screen;final Throwable[] failure={null};runOnMainSync(()->{try{app.go(tab);View decor=app.getWindow().getDecorView();check(decor!=null,"screen "+tab+" renders");}catch(Throwable t){failure[0]=t;}});if(failure[0]!=null)throw new RuntimeException(failure[0]);waitForIdleSync();
                runOnMainSync(()->{try{View view=app.getWindow().getDecorView();Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);bitmap.setHasAlpha(false);view.draw(new Canvas(bitmap));try(FileOutputStream out=new FileOutputStream(new File(app.getExternalFilesDir(null),"screen-"+tab+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}catch(Exception e){throw new RuntimeException(e);}});
            }
            runOnMainSync(()->{app.go(2);app.selectSystem(7);app.selectSystem(8);app.go(3);app.go(4);app.go(0);});waitForIdleSync();checks++;
            AlertDialog[] editor={null};
            runOnMainSync(()->editor[0]=app.recordForm(null));waitForIdleSync();
            runOnMainSync(()->{
                java.util.List<android.widget.EditText> fields=new java.util.ArrayList<>();
                collect(editor[0].getWindow().getDecorView(),android.widget.EditText.class,fields);
                check(fields.size()==2,"symptom form asks for description and details");
                java.util.List<android.widget.Spinner> choices=new java.util.ArrayList<>();
                collect(editor[0].getWindow().getDecorView(),android.widget.Spinner.class,choices);
                check(choices.size()==2&&choices.stream().noneMatch(v->v.getAdapter().getCount()==9),"no anatomical system selector in form");
                fields.get(0).setText("Dolor en rodilla izquierda");fields.get(1).setText("Prueba UI automática temporal");
                editor[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });waitForIdleSync();
            check(!editor[0].isShowing(),"symptom form saves successfully");
            JSONObject uiRecord=null;
            for(JSONObject item:new Vault(target).list("records"))if("Prueba UI automática temporal".equals(item.optString("notes")))uiRecord=item;
            check(uiRecord!=null,"UI record persists");id=uiRecord.getString("id");
            check(uiRecord.getJSONArray("systems").length()==2&&!uiRecord.has("system"),"UI save associates systems without manual input");
            JSONObject editable=uiRecord;
            runOnMainSync(()->editor[0]=app.recordForm(editable));waitForIdleSync();
            runOnMainSync(()->{
                java.util.List<android.widget.CheckBox> boxes=new java.util.ArrayList<>();
                collect(editor[0].getWindow().getDecorView(),android.widget.CheckBox.class,boxes);
                boxes.get(0).setChecked(false);editor[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });waitForIdleSync();
            for(JSONObject item:new Vault(target).list("records"))if(item.optString("id").equals(id))check(!RecordAssociations.active(item),"UI resolves symptom and persists state");
            new Vault(target).remove("records",id);id=null;
            JSONObject knee=Vault.item().put("kind","Síntoma").put("title","Dolor en rodilla izquierda").put("notes","Registro sintético").put("active",true);
            RecordAssociations.attach(knee);check(knee.getJSONArray("systems").length()==2,"automatic multi-system storage");
            check(!knee.has("system"),"no manual system stored");
            JSONObject urinary=Vault.item().put("kind","Síntoma").put("title","Incomodidad al orinar").put("active",true);
            RecordAssociations.attach(urinary);
            JSONObject legacy=Vault.item().put("kind","Síntoma").put("title","Dolor de estómago").put("system",0);
            check(RecordAssociations.systems(legacy).contains(5)&&!RecordAssociations.systems(legacy).contains(0),"legacy symptom is re-associated from text");
            JSONObject unknown=Vault.item().put("kind","Síntoma").put("title","Me siento raro").put("system",0);
            check(RecordAssociations.systems(unknown).isEmpty(),"unknown legacy symptom does not default to nervous system");
            BodyView[] map={null};
            runOnMainSync(()->{map[0]=new BodyView(app);map[0].selected.clear();map[0].selected.addAll(Arrays.asList(3,6,8));map[0].records(Arrays.asList(knee,urinary));app.setContentView(map[0]);});
            waitForIdleSync();
            check(map[0].getContentDescription().toString().contains("dolor"),"pain marker has text alternative");
            check(map[0].getContentDescription().toString().contains("incomodidad"),"discomfort marker has text alternative");
            java.lang.reflect.Field pulseField=BodyView.class.getDeclaredField("pulse");pulseField.setAccessible(true);
            float[] phase={0,0};runOnMainSync(()->{try{phase[0]=pulseField.getFloat(map[0]);}catch(Exception e){throw new RuntimeException(e);}});
            Thread.sleep(400);
            runOnMainSync(()->{try{phase[1]=pulseField.getFloat(map[0]);}catch(Exception e){throw new RuntimeException(e);}});
            check(Math.abs(phase[0]-phase[1])>.01f,"indicator animation changes opacity");
            runOnMainSync(()->map[0].setAnimated(false));
            for(String fieldName:new String[]{"discomfortOverlay","painOverlay"}){
                java.lang.reflect.Field overlayField=BodyView.class.getDeclaredField(fieldName);overlayField.setAccessible(true);
                Bitmap overlay=(Bitmap)overlayField.get(map[0]);int colored=0;
                for(int y=0;y<overlay.getHeight();y++)for(int x=0;x<overlay.getWidth();x++){
                    int c=overlay.getPixel(x,y);if(Color.alpha(c)==0)continue;colored++;
                    if(fieldName.equals("discomfortOverlay")){
                        if(!(Color.green(c)>Color.red(c)*.8f&&Color.blue(c)<50))throw new AssertionError("discomfort must be yellow");
                    }else if(!(Color.red(c)>Color.green(c)*3))throw new AssertionError("pain must be red");
                }
                check(colored>100,"tissue illumination is visible: "+fieldName);
                check(Color.alpha(overlay.getPixel(0,0))==0,"illumination does not cover background");
            }
            capture(app,map[0],"body-associations.png");
            knee.put("active",false);urinary.put("active",false);
            runOnMainSync(()->map[0].records(Arrays.asList(knee,urinary)));
            check(!map[0].getContentDescription().toString().contains("dolor"),"resolved pain no longer highlighted");
            java.lang.reflect.Field animationField=BodyView.class.getDeclaredField("animator");animationField.setAccessible(true);
            check(animationField.get(map[0])==null,"animation stops without active symptoms");
            knee.put("active",true);
            runOnMainSync(()->{map[0].records(Arrays.asList(knee));map[0].setAnimated(false);});
            check(animationField.get(map[0])==null,"motion preference stops pulse");
            runOnMainSync(()->{BodyView all=new BodyView(app);all.selected.clear();for(int i=0;i<9;i++)all.selected.add(i);all.setAnimated(false);app.setContentView(all);map[0]=all;});
            waitForIdleSync();capture(app,map[0],"body-all-systems.png");
            java.lang.reflect.Field imagesField=BodyView.class.getDeclaredField("images");imagesField.setAccessible(true);
            check(((java.util.Map<?,?>)imagesField.get(map[0])).size()==9,"all nine system views have loaded illustrations");
            runOnMainSync(()->app.go(0));waitForIdleSync();
            runOnMainSync(()->{
                app.go(2);
                java.util.List<android.widget.TextView> labels=new java.util.ArrayList<>();collect(app.getWindow().getDecorView(),android.widget.TextView.class,labels);
                check(labels.stream().anyMatch(v->"Tu mapa personal".contentEquals(v.getText())&&Math.abs(v.getTextSize()/app.getResources().getDisplayMetrics().scaledDensity-32)<1),"body title is the large heading");
                check(labels.stream().anyMatch(v->"CONOCE TU CUERPO".contentEquals(v.getText())),"body section has its contextual eyebrow");
                check(labels.stream().noneMatch(v->"Escucha a tu cuerpo".contentEquals(v.getText())),"old body title removed");
                app.go(0);
            });waitForIdleSync();capture(app,app.getWindow().getDecorView(),"home-light.png");
            check(AppointmentType.type(new JSONObject().put("title","Control con neurólogo"))==1,"legacy neurology appointment icon");
            check(AppointmentType.type(new JSONObject().put("title","Examen de sangre"))==4,"laboratory appointment icon");
            check(AppointmentType.type(new JSONObject().put("title","Cardiólogo").put("appointmentType",5))==5,"explicit appointment type overrides automatic detection");
            java.lang.reflect.Field monthField=MainActivity.class.getDeclaredField("agendaMonth");monthField.setAccessible(true);
            runOnMainSync(()->{try{monthField.set(app,java.time.YearMonth.of(2028,2));app.go(1);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
            runOnMainSync(()->{java.util.List<View> cells=new java.util.ArrayList<>();collect(app.getWindow().getDecorView(),View.class,cells);check(cells.stream().anyMatch(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("2028-02-29,")),"leap day shown in full month");});
            capture(app,app.getWindow().getDecorView(),"calendar-leap-month.png");
            java.lang.reflect.Field agendaVaultField=MainActivity.class.getDeclaredField("vault");agendaVaultField.setAccessible(true);Vault agendaVault=(Vault)agendaVaultField.get(app);
            String[] sampleIds={"calendar-test-neuro","calendar-test-cardio","calendar-test-dental"};
            try{
                String[] titles={"Control neurólogo","Cardiólogo","Dentista"};
                for(int i=0;i<sampleIds.length;i++)agendaVault.put("appointments",new JSONObject().put("id",sampleIds[i]).put("title",titles[i]).put("time",java.time.LocalDate.of(2028,2,i==2?10:29).atTime(10+i,0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()));
                runOnMainSync(()->app.go(1));waitForIdleSync();
                runOnMainSync(()->{java.util.List<View> cells=new java.util.ArrayList<>();collect(app.getWindow().getDecorView(),View.class,cells);check(cells.stream().anyMatch(v->v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("2028-02-29, 2 citas")),"calendar groups multiple appointments on their date");});
                capture(app,app.getWindow().getDecorView(),"calendar-with-appointments.png");
            }finally{for(String sampleId:sampleIds)agendaVault.remove("appointments",sampleId);}

            runOnMainSync(()->{try{monthField.set(app,java.time.YearMonth.of(2028,12));app.moveMonth(1);check(monthField.get(app).equals(java.time.YearMonth.of(2029,1)),"December navigates to following year");app.moveMonth(-1);check(monthField.get(app).equals(java.time.YearMonth.of(2028,12)),"January navigates to previous year");}catch(Exception e){throw new RuntimeException(e);}app.go(0);});
            JSONObject analyzed=Vault.item().put("name","scan123.pdf").put("mime","application/pdf");
            JSONObject extraction=new JSONObject().put("readable",true).put("name","Hemograma").put("clinical_date","2024-02-29").put("date_evidence","29/02/2024");
            ClinicalDocuments.apply(analyzed,extraction);
            check(analyzed.getString("name").startsWith("Hemograma"),"document renamed from analysis");
            check(analyzed.getString("originalName").equals("scan123.pdf"),"original document name preserved");
            JSONObject unknownDate=Vault.item().put("name","unknown.pdf").put("mime","application/pdf");
            ClinicalDocuments.apply(unknownDate,new JSONObject().put("readable",false).put("name",JSONObject.NULL).put("clinical_date",JSONObject.NULL));
            check(unknownDate.getString("name").equals("unknown.pdf")&&ClinicalDocuments.clinicalDate(unknownDate)==null,"unreadable document does not invent name or date");
            java.util.List<JSONObject> ordered=new java.util.ArrayList<>(Arrays.asList(unknownDate,analyzed));ClinicalDocuments.sort(ordered);
            check(ordered.get(0)==analyzed,"clinical dates precede unknown dates");
            check(ClinicalDocuments.category(unknownDate).equals("exam"),"legacy document remains in exams");
            java.lang.reflect.Field healthTabField=MainActivity.class.getDeclaredField("healthTab");healthTabField.setAccessible(true);
            runOnMainSync(()->{try{healthTabField.setInt(app,1);app.go(3);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
            runOnMainSync(()->{java.util.List<android.widget.TextView> labels=new java.util.ArrayList<>();collect(app.getWindow().getDecorView(),android.widget.TextView.class,labels);check(labels.stream().anyMatch(v->"+ Subir historia clínica".contentEquals(v.getText())),"history is a document upload screen");check(labels.stream().noneMatch(v->"+  Síntoma o diagnóstico".contentEquals(v.getText())),"history no longer adds manual diagnoses");});
            capture(app,app.getWindow().getDecorView(),"clinical-history.png");
            runOnMainSync(()->{try{healthTabField.setInt(app,2);app.go(3);}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();capture(app,app.getWindow().getDecorView(),"clinical-exams.png");
            runOnMainSync(()->app.go(0));waitForIdleSync();
            ActivityMonitor themeMonitor=addMonitor(MainActivity.class.getName(),null,false);
            runOnMainSync(()->{
                java.util.List<android.widget.TextView> buttons=new java.util.ArrayList<>();collect(app.getWindow().getDecorView(),android.widget.TextView.class,buttons);
                check(buttons.stream().noneMatch(v->"PREMIUM".contentEquals(v.getText())),"premium badge removed");
                buttons.stream().filter(v->"Activar modo oscuro".equals(v.getContentDescription())).findFirst().orElseThrow().performClick();
            });
            MainActivity night=(MainActivity)themeMonitor.waitForActivityWithTimeout(10000);removeMonitor(themeMonitor);
            check(night!=null,"appearance button recreates activity");waitForIdleSync();
            check(night.getPreferences(Context.MODE_PRIVATE).getBoolean("darkMode",false),"dark preference persisted");
            for(int tab=0;tab<5;tab++){int selectedTab=tab;runOnMainSync(()->night.go(selectedTab));waitForIdleSync();capture(night,night.getWindow().getDecorView(),"dark-"+tab+".png");}
            runOnMainSync(()->editor[0]=night.recordForm(null));waitForIdleSync();capture(night,editor[0].getWindow().getDecorView(),"dark-form.png");
            runOnMainSync(()->{editor[0].dismiss();night.getPreferences(Context.MODE_PRIVATE).edit().putBoolean("darkMode",false).commit();night.finish();});
            result.putString("stream","PASS: "+checks+" Android checks (vault, tamper rejection, documents, five screens).\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable e){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}
        finally {try{if(vault!=null){if(id!=null)vault.remove("records",id);if(docId!=null){vault.remove("documents",docId);vault.deleteDocument(docId);}}}catch(Exception ignored){}}
    }
    private void capture(MainActivity app,View view,String name){
        runOnMainSync(()->{try{Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);bitmap.setHasAlpha(false);view.draw(new Canvas(bitmap));try(FileOutputStream output=new FileOutputStream(new File(app.getExternalFilesDir(null),name))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();}catch(Exception e){throw new RuntimeException(e);}});
    }
    private <T extends View> void collect(View root,Class<T> type,java.util.List<T> found){
        if(type.isInstance(root))found.add(type.cast(root));
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)collect(group.getChildAt(i),type,found);}
    }
}
