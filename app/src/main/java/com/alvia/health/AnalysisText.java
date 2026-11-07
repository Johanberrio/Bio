package com.alvia.health;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Presentation only: preserve distinct cautions and never rewrite clinical evidence. */
final class AnalysisText {
    static String uniqueParagraphs(String value) {
        Map<String,String> paragraphs=new LinkedHashMap<>();
        for(String line:value.split("\\R+")) {
            String text=line.trim();
            if(text.isEmpty())continue;
            String key=text.replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
            paragraphs.putIfAbsent(key,text);
        }
        return String.join("\n\n",paragraphs.values());
    }
}
