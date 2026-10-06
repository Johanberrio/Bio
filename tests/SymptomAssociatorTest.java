package com.alvia.health;
import java.util.*;

public class SymptomAssociatorTest {
    static int checks=0;
    static void check(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    static SymptomAssociator.Result read(String text){return SymptomAssociator.associate(text,"","Según descripción");}
    static void systems(String text,Integer... expected){check(read(text).systems().equals(new LinkedHashSet<>(Arrays.asList(expected))),text+" -> "+read(text).labels());}
    public static void main(String[] args){
        systems("Dolor en rodilla izquierda",6,8);
        SymptomAssociator.Result knee=read("Dolor en rodilla izquierda");
        check(knee.signals.stream().allMatch(s->s.x>.5f&&s.sensation==2),"left knee mirrors on frontal view and is red");
        check(read("Dolor en rodilla derecha").signals.stream().allMatch(s->s.x<.5f),"right knee mirrors on frontal view");
        check(read("Dolor en rodillas").signals.size()==4,"unspecified laterality preserves both candidate positions");
        systems("Incomodidad al orinar",3);
        check(read("Incomodidad al orinar").signals.get(0).sensation==1,"discomfort orange");
        systems("Dolor al orinar",3);
        systems("Dolor de estómago después de comer",5);
        systems("Tos al respirar",2);
        systems("Dolor de cabeza",0);
        systems("Palpitaciones",1);
        systems("Molestia en la tiroides",4);
        systems("Dolor menstrual",7);
        systems("Dolor muscular en brazo derecho",8);
        systems("Dolor de huesos en brazo izquierdo",6);
        systems("Hormigueo en mano izquierda",0);
        systems("Sin dolor de cabeza");
        systems("No tengo dolor en rodilla derecha");
        systems("Sin dolor de cabeza pero molestias al orinar",3);
        systems("Sin dolor de cabeza y molestias al orinar",3);
        systems("No tengo dolor de pecho ni de estómago");
        systems("Mi madre tiene dolor de rodilla");
        systems("Me siento raro");
        check(read("Me siento raro").incomplete,"unknown symptom requests details");
        check(read("Dolor").signals.isEmpty(),"no invented location for unspecified pain");
        check(read("Dolor muscular").signals.get(0).x<0,"unlocalized symptom shown as a badge");
        check(read("Dolor de pecho").systems().size()>1,"chest ambiguity preserved");
        check(read("Dolor de pecho").urgent,"chest pain does not just create a pretty marker");
        check(!read("Sin dolor de pecho").urgent,"negated chest pain does not create alert");
        check(read("No puedo respirar").urgent&&read("No puedo respirar").systems().contains(2),"breathing red flag is not erased by negation");
        check(!read("Sin falta de aire").urgent,"negated breathlessness");
        SymptomAssociator.Result mixed=read("Dolor en rodilla izquierda y molestia al orinar");
        check(mixed.signals.stream().anyMatch(s->s.system==3&&s.sensation==1),"orange symptom alongside red symptom");
        check(mixed.signals.stream().anyMatch(s->s.system==6&&s.sensation==2),"red symptom retains its own region");
        check(read("Dolor en riñón izquierdo").signals.get(0).x>.5f,"renal laterality");
        check(SymptomAssociator.associate("Rodilla izquierda","","Incomodidad").signals.stream().allMatch(s->s.sensation==1),"explicit sensation without system selection");
        check(SymptomAssociator.associate("Dolor en rodilla","","Otro síntoma").signals.stream().allMatch(s->s.sensation==0),"user sensation override");
        check(!SymptomAssociator.associate("Dolor en rodilla izquierda","Dolor en rodilla izquierda","Según descripción").incomplete,"repeated detail is not unknown");
        System.out.println("PASS: "+checks+" symptom association checks");
    }
}
