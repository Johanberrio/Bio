package com.alvia.health;
import java.util.Locale;
import org.json.JSONObject;
final class AppointmentType {
    static final String[] NAMES={"Detectar según el motivo", "Neurología", "Cardiología", "Medicina general", "Examen / laboratorio", "Odontología", "Oftalmología", "Traumatología", "Psicología", "Pediatría", "Otra consulta"};
    static final String[] ICONS={"✚","🧠","♥","✚","💉","🦷","👁","🦴","💬","🧸","✚"};
    static int type(JSONObject a){
        int explicit=a.optInt("appointmentType",0);if(explicit>0&&explicit<NAMES.length)return explicit;
        String text=SymptomAssociator.normalize(a.optString("title")+" "+a.optString("doctor"));
        String[] patterns={"neuro","cardio|corazon","general|medicina familiar","examen|laboratorio|sangre|vacuna|analisis|ecografia|resonancia|radiografia","dent|odonto|muela","oftal|optom|ojo","trauma|ortop|fractura","psico|terapia","pediatr"};
        for(int i=0;i<patterns.length;i++)if(java.util.regex.Pattern.compile(patterns[i]).matcher(text).find())return i+1;
        return 10;
    }
    static String icon(JSONObject a){return ICONS[type(a)];}
}
