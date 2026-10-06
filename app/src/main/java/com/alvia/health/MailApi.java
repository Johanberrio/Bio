package com.alvia.health;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** Only Bio's authenticated HTTPS service receives requests; OAuth secrets stay server-side. */
final class MailApi {
    final FirebaseCloud cloud; final String endpoint;
    MailApi(Context c,FirebaseCloud cloud)throws Exception{
        this.cloud=cloud;
        try(InputStream in=c.getAssets().open("backend-config.json")){endpoint=new JSONObject(new String(MainActivity.readLimited(in,16384),StandardCharsets.UTF_8)).getString("url");}
        if(!endpoint.startsWith("https://"))throw new IOException("El servidor requiere HTTPS.");
    }
    JSONObject post(String path,JSONObject payload)throws Exception{
        if(!cloud.account())throw new IOException("Inicia sesión en Bio.");
        String owner=cloud.owner();
        HttpURLConnection h=(HttpURLConnection)new URL(endpoint+path).openConnection();
        try{
            h.setConnectTimeout(20000);h.setReadTimeout(path.equals("/analyze-document")?290000:180000);h.setInstanceFollowRedirects(false);
            h.setRequestMethod("POST");h.setRequestProperty("Content-Type","application/json");h.setRequestProperty("Authorization","Bearer "+cloud.idToken());h.setDoOutput(true);
            byte[] body=payload.toString().getBytes(StandardCharsets.UTF_8);h.setFixedLengthStreamingMode(body.length);try(OutputStream out=h.getOutputStream()){out.write(body);}
            int code=h.getResponseCode();if(code==429)throw new IOException("Límite diario de análisis alcanzado. Se reintentará mañana.");
            InputStream stream=code==200?h.getInputStream():h.getErrorStream();JSONObject result;
            if(stream==null)throw new IOException("El servicio de correo no respondió ("+code+").");
            try(InputStream in=stream){result=new JSONObject(new String(MainActivity.readLimited(in,code==200?15*1024*1024:32768),StandardCharsets.UTF_8));}
            if(!cloud.account()||!owner.equals(cloud.owner()))throw new IOException("La sesión cambió. Vuelve a intentarlo.");
            if(code!=200)throw new IOException(result.optString("error","No se completó la sincronización de correo."));
            return result;
        }finally{h.disconnect();}
    }
}
