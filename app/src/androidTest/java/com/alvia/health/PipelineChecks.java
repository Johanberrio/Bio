package com.alvia.health;
import org.json.*;
import java.io.IOException;
final class PipelineChecks {
    static void run(Vault vault,SmokeTests test)throws Exception{
        int[] analyzed={0},uploaded={0};boolean[] fail={true};
        DocumentPipeline flow=new DocumentPipeline(vault,(doc,bytes)->{analyzed[0]++;return new JSONObject().put("readable",true).put("name","Hemograma completo").put("clinical_date","2026-09-01").put("date_evidence","01/09/2026");},doc->{uploaded[0]++;test.check(doc.optString("name").startsWith("Hemograma completo"),"Inferred name reaches uploader");if(fail[0])throw new IOException("synthetic offline");vault.put("documents",new JSONObject(doc.toString()).put("cloudOwner","test:owner"));});
        JSONObject doc=flow.stage("scan.pdf","exam","application/pdf",new byte[]{1,2,3});String id=doc.getString("id");
        try{
            test.check(DocumentPipeline.pending(doc),"Staged file is not completed");
            try{flow.process(doc);throw new AssertionError("Expected offline failure");}catch(IOException expected){}
            final String stagedId=id;JSONObject pending=vault.list("documents").stream().filter(d->stagedId.equals(d.optString("id"))).findFirst().get();
            test.check(DocumentPipeline.pending(pending)&&pending.has("importError"),"Upload failure remains visibly pending");
            test.check(vault.readDocument(id).length==3,"Failure preserves encrypted original");
            fail[0]=false;JSONObject ready=flow.process(pending);
            test.check(analyzed[0]==1&&uploaded[0]==2,"Retry reuses completed analysis");
            test.check(!DocumentPipeline.pending(ready)&&!ready.has("importError"),"Successful retry clears pending error");
            test.check("test:owner".equals(ready.optString("cloudOwner")),"Successful import retains cloud owner");
        }finally{vault.remove("documents",id);vault.deleteDocument(id);}
        DocumentPipeline broken=new DocumentPipeline(vault,(d,b)->{throw new IOException("synthetic analysis failure");},d->{throw new AssertionError("Must not upload before analysis");});
        doc=broken.stage("scan.pdf","history","application/pdf",new byte[]{4});id=doc.getString("id");
        try{try{broken.process(doc);throw new AssertionError("Expected analysis failure");}catch(IOException expected){}test.check(DocumentPipeline.pending(vault.list("documents").get(0)),"Analysis failure never publishes original filename");}
        finally{vault.remove("documents",id);vault.deleteDocument(id);}
    }
}
