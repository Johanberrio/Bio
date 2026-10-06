package com.alvia.health;
import org.json.JSONObject;

final class MailChecks {
    static void run(SmokeTests tests)throws Exception{
        JSONObject result=new JSONObject().put("readable",true).put("document_kind","exam").put("patient_match","matches");
        tests.check(MailSync.admissible(result),"Matching patient and medical exam can be auto-imported");
        result.put("patient_match","unknown");tests.check(!MailSync.admissible(result),"Unidentified patient must not be automatically imported");
        result.put("patient_match","other");tests.check(!MailSync.admissible(result),"Other person's medical record must not be imported");
        result.put("patient_match","matches").put("document_kind","other");tests.check(!MailSync.admissible(result),"Invoices and nonclinical attachments excluded");
        result.put("document_kind","history").put("readable",false);tests.check(!MailSync.admissible(result),"Unreadable history is not automatically imported");
        result.put("readable",true);tests.check(MailSync.admissible(result),"Matching clinical history accepted");
        long now=System.currentTimeMillis();
        java.util.Set<String> sources=java.util.Collections.singleton("mail-test");
        java.util.Set<String> imported=java.util.Collections.singleton("saved");
        JSONObject notice=new JSONObject().put("id","dedup-key").put("source","mail-test").put("created",now).put("status","No importado: titular");
        tests.check(MailReviews.visible(notice,sources,imported,now),"Recent legacy notice is reviewable");
        notice.put("dismissed",true);
        tests.check(!MailReviews.visible(notice,sources,imported,now)&&notice.getString("id").equals("dedup-key"),"Dismissed notice preserves dedup identity");
        notice.put("dismissed",false).put("fingerprint","saved");
        tests.check(!MailReviews.visible(notice,sources,imported,now),"Imported fingerprint resolves rejection");
        notice.put("fingerprint","different").put("created",now-MailReviews.MAX_AGE);
        tests.check(!MailReviews.visible(notice,sources,imported,now),"Expired notices are hidden");
        notice.put("created",now).put("source","other-owner");
        tests.check(!MailReviews.visible(notice,sources,imported,now),"Other account notices are excluded");
        tests.check(MainActivity.measurement(new JSONObject(),"weightKg","kg").equals("Sin registrar"),"Missing weight is not zero");
        tests.check(MainActivity.measurement(new JSONObject().put("heightCm",170),"heightCm","cm").equals("170 cm"),"Height retains units");
        tests.check(BodyView.resourceFor(7,true)==R.drawable.system_female_reproductive&&BodyView.resourceFor(7,false)==R.drawable.system_reproductive,"Dedicated reproductive resources");
    }
}
