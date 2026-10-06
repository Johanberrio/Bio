package com.alvia.health;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Offline, conservative text associations. These are NOT diagnoses or a clinical model. */
final class SymptomAssociator {
    static final int VERSION = 1;
    static final String[] SYSTEMS = {"Nervioso", "Circulatorio", "Respiratorio", "Urinario",
            "Endocrino", "Digestivo", "Óseo", "Reproductivo", "Muscular"};
    static final int OTHER = 0, DISCOMFORT = 1, PAIN = 2;

    static final class Signal {
        final int system, sensation;
        final float x, y;
        final String region, reason;
        Signal(int system, int sensation, float x, float y, String region, String reason) {
            this.system=system; this.sensation=sensation; this.x=x; this.y=y;
            this.region=region; this.reason=reason;
        }
    }
    static final class Result {
        final List<Signal> signals = new ArrayList<>();
        boolean urgent;
        boolean incomplete;
        int matches;
        Set<Integer> systems() {
            Set<Integer> ids = new LinkedHashSet<>();
            for (Signal signal : signals) ids.add(signal.system);
            return ids;
        }
        String labels() {
            List<String> labels = new ArrayList<>();
            for (int id : systems()) labels.add(SYSTEMS[id]);
            return labels.isEmpty() ? "Necesita más detalles" : String.join(" · ", labels);
        }
        void add(int system, int sensation, float x, float y, String region, String reason) {
            matches++;
            for (Signal s : signals) if (s.system==system && s.region.equals(region) && s.sensation==sensation && s.x==x && s.y==y) return;
            signals.add(new Signal(system, sensation, x, y, region, reason));
        }
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
    static boolean has(String text, String expression) {
        return Pattern.compile("\\b(?:"+expression+")\\b").matcher(text).find();
    }
    static String affirmed(String text) {
        // Negation is scoped to a clause; contrast and repeated sensation words form new clauses.
        return text.replaceAll("\\b(?:no|sin|niego|nunca)\\b[^,;.!?]*", " ");
    }
    static Result associate(String title, String notes, String sensationOverride) {
        Result out = new Result();
        String input = normalize(title + ". " + notes);
        input = input.replaceAll("\\b(?:pero|aunque|sin embargo)\\b", ";")
                .replaceAll("\\by\\s+(?=(?:dolor|dolores|molestia|molestias|incomodidad|me duele|tengo|siento|no|sin)\\b)", "; ");
        String[] clauses = input.split("[,;.!?\\n]+");
        int inheritedSensation = OTHER;
        for (String raw : clauses) {
            String clause = affirmed(raw).trim();
            if (clause.isEmpty()) continue;
            // Explicitly historical, resolved and family statements should not create active markers.
            if (has(clause,"resuelto|desaparecio|desaparecieron|se quito|mi madre|mi padre|mi hermano|mi hermana|antecedente familiar")) continue;
            int sensation = has(clause,"dolor|dolores|duele|duelen|doloroso|dolorosa|punzada|punzadas") ? PAIN
                    : has(clause,"incomodidad|incomodo|incomoda|molestia|molestias|malestar|ardor|picor|picazon") ? DISCOMFORT : OTHER;
            if (sensation != OTHER) inheritedSensation = sensation;
            else if (has(clause,"cabeza|pecho|abdomen|estomago|rodilla|brazo|pierna|espalda|cuello|pelvis|hombro|mano|pie")) sensation=inheritedSensation;
            if ("Dolor".equals(sensationOverride)) sensation=PAIN;
            else if ("Incomodidad".equals(sensationOverride)) sensation=DISCOMFORT;
            else if ("Otro síntoma".equals(sensationOverride)) sensation=OTHER;

            int before=out.matches;
            // Body's left appears on the right of a frontal illustration.
            float side=has(clause,"izquierdo|izquierda") ? 1f : has(clause,"derecho|derecha") ? -1f : 0f;
            boolean chest=has(clause,"pecho|torax|toracico|toracica|costilla|costillas");
            boolean urinary=has(clause,"orinar|orino|orina|miccion|vejiga|uretra|rinon|rinones|renal|cistitis");
            boolean respiratory=has(clause,"tos|toser|respirar|respiracion|falta de aire|disnea|pulmon|pulmones|asma|bronquitis|silbido|silbidos");
            boolean digestive=has(clause,"estomago|gastrico|gastrica|gastritis|reflujo|acidez|nausea|nauseas|vomito|vomitos|diarrea|estrenimiento|intestino|intestinal|comer|comida|digestion");
            boolean cardiac=has(clause,"palpitacion|palpitaciones|corazon|cardiaco|cardiaca|taquicardia|hipertension|vena|venas|varices");
            boolean nerve=has(clause,"hormigueo|entumecimiento|adormecimiento|neuropatia|ciatica|nervio|nervios");
            boolean muscle=has(clause,"musculo|musculos|muscular|contractura|calambre|calambres|desgarro|agujetas");
            boolean bone=has(clause,"hueso|huesos|fractura|articulacion|articulaciones|artritis|artrosis|osteoporosis");
            boolean reproductive=has(clause,"menstruacion|menstrual|regla|ovario|ovarios|utero|vagina|vaginal|testiculo|testiculos|pene|prostata|prostatico|prostatica|eyaculacion");
            boolean endocrine=has(clause,"tiroides|tiroideo|tiroidea|hipotiroidismo|hipertiroidismo|diabetes|insulina|hormona|hormonas|endocrino|endocrina|suprarrenal");

            String limb=null;float lx=.5f,ly=.5f;
            if(has(clause,"rodilla|rodillas")){limb="Rodilla";lx=.5f+side*.10f;ly=.68f;}
            else if(has(clause,"tobillo|tobillos|pie|pies|talon")){limb="Pie / tobillo";lx=.5f+side*.11f;ly=.93f;}
            else if(has(clause,"pierna|piernas|muslo|pantorrilla")){limb="Pierna";lx=.5f+side*.10f;ly=.76f;}
            else if(has(clause,"mano|manos|muneca|munecas|dedo|dedos")){limb="Mano / muñeca";lx=.5f+side*.29f;ly=.54f;}
            else if(has(clause,"codo|codos|brazo|brazos|antebrazo")){limb="Brazo";lx=.5f+side*.22f;ly=.37f;}
            else if(has(clause,"hombro|hombros")){limb="Hombro";lx=.5f+side*.17f;ly=.21f;}
            else if(has(clause,"espalda|lumbar|columna")){limb="Espalda (proyección anterior)";lx=.5f;ly=.38f;}
            else if(has(clause,"cuello|cervical")){limb="Cuello";lx=.5f;ly=.16f;}
            if(limb!=null) {
                String suffix=side>0?" izquierdo":side<0?" derecho":" (lado no especificado)";
                // Do not invent laterality: show both candidate locations for paired body parts.
                float[] xs=side==0 && !limb.startsWith("Espalda") && !limb.equals("Cuello") ?
                        new float[]{limb.startsWith("Mano")?.21f:limb.equals("Brazo")?.28f:limb.equals("Hombro")?.33f:.40f,
                                limb.startsWith("Mano")?.79f:limb.equals("Brazo")?.72f:limb.equals("Hombro")?.67f:.60f}:new float[]{lx};
                for(float x:xs){
                    if(nerve)out.add(0,sensation,x,ly,limb+suffix,"Sensación nerviosa descrita; asociación orientativa.");
                    if(cardiac)out.add(1,sensation,x,ly,limb+suffix,"Referencia vascular explícita en el texto.");
                    if(bone || (!nerve&&!muscle&&!cardiac))out.add(6,sensation,x,ly,limb+suffix,"La zona incluye articulaciones y huesos; no identifica la causa.");
                    if(muscle || (!nerve&&!bone&&!cardiac))out.add(8,sensation,x,ly,limb+suffix,"La zona incluye tejidos musculares; no identifica la causa.");
                }
            }
            if(has(clause,"cabeza|cefalea|migrana")) out.add(0,sensation,.5f,.065f,"Cabeza","Ubicación descrita; no confirma un origen neurológico.");
            if(urinary){
                boolean kidney=has(clause,"rinon|rinones|renal");
                if(kidney && side==0){out.add(3,sensation,.43f,.365f,"Zona renal (lado no especificado)","Referencia renal; falta precisar el lado.");out.add(3,sensation,.57f,.365f,"Zona renal (lado no especificado)","Referencia renal; falta precisar el lado.");}
                else out.add(3,sensation,kidney?.5f+side*.07f:.5f,kidney?.365f:.49f,kidney?"Zona renal":"Zona urinaria","Referencia a micción o vías urinarias.");
            }
            if(respiratory)out.add(2,sensation,.5f,.265f,"Tórax / respiración","Referencia respiratoria en la descripción.");
            if(digestive)out.add(5,sensation,.5f,.40f,"Abdomen / digestión","Referencia digestiva en la descripción.");
            if(cardiac && limb==null)out.add(1,sensation,.53f,.27f,"Tórax / circulación","Referencia cardiovascular explícita.");
            if(reproductive)out.add(7,sensation,.5f,.53f,"Pelvis / reproducción","Referencia reproductiva en la descripción.");
            if(endocrine)out.add(4,sensation,.5f,has(clause,"tiroides|tiroideo|tiroidea|hipotiroidismo|hipertiroidismo")?.155f:.38f,"Zona endocrina aproximada","Referencia hormonal explícita; no implica que el síntoma tenga ese origen.");
            if(muscle&&limb==null){out.add(8,sensation,chest?.5f:-1,chest?.25f:-1,chest?"Tórax":"Zona muscular no precisada","Músculo mencionado; falta precisar la zona.");if(!chest)out.incomplete=true;}
            if(bone&&limb==null){out.add(6,sensation,chest?.5f:-1,chest?.25f:-1,chest?"Tórax":"Zona ósea no precisada","Hueso o articulación mencionado; falta precisar la zona.");if(!chest)out.incomplete=true;}
            if(nerve&&limb==null){out.add(0,sensation,-1,-1,"Zona nerviosa no precisada","Sensación nerviosa descrita; falta precisar la zona.");out.incomplete=true;}
            if(chest&&!respiratory&&!cardiac&&!muscle&&!bone&&!digestive){
                for(int s:new int[]{1,2,5,6,8})out.add(s,sensation,.5f,.255f,"Tórax (origen sin determinar)","El pecho contiene tejidos de varios sistemas; se necesita valoración.");
            }
            if(has(clause,"abdomen|abdominal|barriga|vientre")&&!digestive&&!urinary&&!reproductive){
                for(int s:new int[]{5,3,7})out.add(s,sensation,.5f,.43f,"Abdomen (origen sin determinar)","La ubicación puede relacionarse con varios sistemas.");
            }
            if(has(clause,"pelvis|pelvico|pelvica|ingle")&&!urinary&&!reproductive){
                for(int s:new int[]{3,7,6,8})out.add(s,sensation,.5f,.49f,"Pelvis (origen sin determinar)","La región pélvica comprende varios sistemas.");
            }
            if(has(clause,"garganta|nariz|nasal|faringe"))out.add(2,sensation,.5f,.135f,"Vías respiratorias superiores","Ubicación descrita; asociación orientativa.");
            if((chest&&sensation>OTHER)||has(clause,"no puedo respirar|falta de aire|desmayo|desmaye|debilidad repentina|vomito sangre|vomitos con sangre"))out.urgent=true;
            if(out.matches==before && sensation>OTHER)out.incomplete=true;
        }
        // Negation of "puedo respirar" expresses a positive red flag, not absence of a symptom.
        if(has(input,"no puedo respirar")){out.urgent=true;out.add(2,OTHER,.5f,.265f,"Respiración","Dificultad respiratoria descrita.");}
        if(out.signals.isEmpty())out.incomplete=true;
        return out;
    }
}
