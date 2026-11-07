package com.alvia.health;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.WindowInsets;
import android.widget.*;
import com.android.billingclient.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Play owns checkout. Only the authenticated server can grant access. */
public class PremiumActivity extends Activity {
    private BillingClient billing;
    private FirebaseCloud cloud;
    private TextView status, buy, monthlyPrice, usage;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private String owner, accountHash;
    private volatile boolean enabled=false;
    private boolean connecting;
    private int purple, ink, buttonBackground;
    @Override public void onCreate(Bundle state){
        boolean dark=getSharedPreferences("MainActivity",MODE_PRIVATE).getBoolean("darkMode",false);
        setTheme(dark?R.style.Theme_Bio_Dark:R.style.Theme_Bio);super.onCreate(state);
        if(!AuthSession.unlocked()){finish();return;}
        try{cloud=FirebaseCloud.create(this);if(!cloud.account()){finish();return;}owner=cloud.owner();accountHash=SourceSync.hash(owner.getBytes(StandardCharsets.UTF_8));}catch(Exception e){finish();return;}
        buttonBackground=dark?0xff2e253f:0xfff2edf9;purple=dark?0xffc5a6ff:0xff7550b8;ink=dark?0xfff4effa:0xff282335;
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(dark?0xff141119:Color.WHITE);
        LinearLayout layout=new AdaptiveColumn(this);layout.setPadding(24,24,24,24);scroll.addView(layout);setContentView(scroll);
        if(android.os.Build.VERSION.SDK_INT>=30)scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets b=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(b.left,b.top,b.right,b.bottom);return i;});else scroll.setFitsSystemWindows(true);
        button(layout,"‹ Volver",()->finish());
        label(layout,"MÁS APOYO PARA CUIDARTE",12,purple);
        label(layout,"Bio Premium",30,ink);
        label(layout,"Normal: organiza tu salud y guarda tus documentos sin IA.\n\nPremium: acceso al análisis y la investigación con IA.",16,ink);
        monthlyPrice=label(layout,"Consultando precio en Google Play…",18,purple);
        label(layout,"Incluye 30 análisis de documentos y 20 consultas de IA por mes calendario (UTC), por cuenta, sin acumulación. Los adjuntos de Gmail usan el mismo cupo de análisis. Los análisis completados cuentan aunque el archivo resulte ilegible o de otro titular. Los errores del servicio no consumen cupo.",15,ink);
        label(layout,"Renovación mensual automática. Cancela cuando quieras desde Google Play.",15,ink);
        status=label(layout,"Consultando tu suscripción…",14,purple);
        usage=label(layout,"",14,ink);
        button(layout,"Consultar suscripción",this::consult);
        buy=button(layout,"Suscribirme a Premium",this::loadProduct);setPurchaseAvailable(false);
        button(layout,"Restaurar compras",this::restore);
        button(layout,"Gestionar en Google Play",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/account/subscriptions?sku=bio_premium&package=com.alvia.health")));}catch(Exception e){message("No hay navegador disponible.");}});
        billing=BillingClient.newBuilder(this).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).enableAutoServiceReconnection().setListener((result,purchases)->{
            if(result.getResponseCode()==BillingClient.BillingResponseCode.OK&&purchases!=null)for(Purchase p:purchases)verify(p);
            else if(result.getResponseCode()==BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)restore();
            else if(result.getResponseCode()!=BillingClient.BillingResponseCode.USER_CANCELED)message("No se pudo completar la compra. Inténtalo de nuevo.");
        }).build();
        connectBilling();
        refresh();
    }
    TextView label(LinearLayout parent,String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);v.setPadding(0,14,0,14);parent.addView(v,new LinearLayout.LayoutParams(-1,-2));return v;}
    TextView button(LinearLayout parent,String value,Runnable action){TextView v=label(parent,value,16,purple);BioButton.style(v,buttonBackground,purple);v.setOnClickListener(w->action.run());return v;}
    boolean sameAccount(){try{return cloud.account()&&owner.equals(cloud.owner());}catch(Exception e){return false;}}
    boolean active(){return !isFinishing()&&!isDestroyed()&&sameAccount();}
    void setPurchaseAvailable(boolean available){buy.setEnabled(available);buy.setAlpha(available?1f:0.45f);}
    void message(String value){runOnUiThread(()->{if(active())status.setText(value);});}
    void consult(){
        message("Consultando tu suscripción…");
        refresh();
        if(billing!=null&&billing.isReady()){loadProduct(false);restore();}else connectBilling();
    }
    void connectBilling(){
        if(!active()||billing==null||connecting)return;
        connecting=true;
        message("Conectando con Google Play…");
        billing.startConnection(new BillingClientStateListener(){
            public void onBillingSetupFinished(BillingResult r){runOnUiThread(()->{
                connecting=false;if(!active())return;
                if(r.getResponseCode()==BillingClient.BillingResponseCode.OK){loadProduct(false);restore();}
                else{priceUnavailable();message("No se pudo conectar con Google Play. Pulsa Consultar suscripción para reintentar.");}
            });}
            public void onBillingServiceDisconnected(){runOnUiThread(()->{connecting=false;});}
        });
    }
    void refresh(){refresh(false);}
    void showUsage(JSONObject response){
        JSONObject data=response.optJSONObject("usage");
        if(!response.optBoolean("premium")||data==null){usage.setText("");return;}
        JSONObject analyses=data.optJSONObject("analysis"),queries=data.optJSONObject("research");
        if(analyses==null||queries==null)return;
        String reset=data.optString("resetAt");if(reset.length()>=10)reset=reset.substring(0,10);
        usage.setText("Disponibles este mes: "+analyses.optInt("remaining")+" análisis y "+queries.optInt("remaining")+" consultas.\nSe renuevan el "+reset+" (UTC).");
    }
    void refresh(boolean restoredEmpty){if(!active()||worker.isShutdown())return;worker.execute(()->{try{JSONObject r=new MailApi(this,cloud).post("/billing/status",new JSONObject());if(!sameAccount())return;enabled=r.optBoolean("purchasesEnabled");runOnUiThread(()->{if(!active())return;setPurchaseAvailable(enabled&&!r.optBoolean("premium"));showUsage(r);status.setText(r.optBoolean("premium")?"Tu suscripción Premium está activa.":restoredEmpty?"Plan actual: Normal. Google Play no encontró una suscripción activa para restaurar. Revisa la cuenta y el estado en Gestionar en Google Play.":enabled?"Plan actual: Normal":"Plan actual: Normal. Las compras no están habilitadas para esta cuenta. Puedes consultar o restaurar una suscripción existente.");});}catch(Exception e){message("No se pudo consultar tu suscripción. Comprueba la conexión y pulsa Consultar suscripción para reintentar.");}});}
    void restore(){if(!active())return;if(billing==null||!billing.isReady()){connectBilling();return;}message("Buscando tus compras en Google Play…");billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),(r,list)->{if(!active())return;if(r.getResponseCode()!=BillingClient.BillingResponseCode.OK){message("No se pudieron restaurar las compras. Comprueba tu conexión y vuelve a intentarlo.");return;}boolean found=false;for(Purchase p:list)if(p.getProducts().contains("bio_premium")){found=true;verify(p);}if(!found)refresh(true);});}
    void verify(Purchase purchase){
        if(isFinishing()||isDestroyed()||worker.isShutdown())return;
        if(!purchase.getProducts().contains("bio_premium"))return;
        if(purchase.getPurchaseState()!=Purchase.PurchaseState.PURCHASED){message("Pago pendiente. Premium se activa cuando Google Play confirme el pago.");return;}
        message("Verificando tu compra con Google Play…");
        worker.execute(()->{try{if(!sameAccount())return;JSONObject r=new MailApi(this,cloud).post("/billing/verify",new JSONObject().put("purchaseToken",purchase.getPurchaseToken()));if(!sameAccount())return;enabled=r.optBoolean("purchasesEnabled");runOnUiThread(()->{if(!active())return;setPurchaseAvailable(enabled&&!r.optBoolean("premium"));showUsage(r);status.setText(r.optBoolean("premium")?"Premium activado. Ya puedes volver a Bio.":"La suscripción no está activa.");});}catch(Exception e){if(sameAccount())message("No se pudo verificar la compra. Pulsa Restaurar compras para reintentarlo. No necesitas comprar de nuevo.");}});
    }
    void loadProduct(){
        loadProduct(true);
    }
    void priceUnavailable(){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())monthlyPrice.setText("Precio no disponible. Pulsa Consultar suscripción para reintentar.");});}
    void loadProduct(boolean checkout){
        if((checkout&&!enabled)||!sameAccount()||billing==null||!billing.isReady()){if(checkout)message("La suscripción todavía no está disponible.");return;}
        billing.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(QueryProductDetailsParams.Product.newBuilder().setProductId("bio_premium").setProductType(BillingClient.ProductType.SUBS).build())).build(),(r,result)->{
            if(r.getResponseCode()!=BillingClient.BillingResponseCode.OK){priceUnavailable();if(checkout)message("No se pudo consultar el precio en Google Play.");return;}
            for(ProductDetails product:result.getProductDetailsList())if(product.getSubscriptionOfferDetails()!=null)for(ProductDetails.SubscriptionOfferDetails offer:product.getSubscriptionOfferDetails()){
                if(!"mensual".equals(offer.getBasePlanId())||offer.getOfferId()!=null)continue;
                List<ProductDetails.PricingPhase> phases=offer.getPricingPhases().getPricingPhaseList();if(phases.size()!=1||!"P1M".equals(phases.get(0).getBillingPeriod()))continue;
                String price=phases.get(0).getFormattedPrice();
                runOnUiThread(()->{if(!sameAccount()||isFinishing()||isDestroyed())return;monthlyPrice.setText(price+" al mes");if(!checkout)return;new android.app.AlertDialog.Builder(this).setTitle("Bio Premium").setMessage(price+" al mes. Renovación automática. Puedes cancelar desde Google Play.").setNegativeButton("Ahora no",null).setPositiveButton("Continuar",(d,w)->{
                    if(!sameAccount())return;
                    BillingFlowParams params=BillingFlowParams.newBuilder().setObfuscatedAccountId(accountHash).setProductDetailsParamsList(Collections.singletonList(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.getOfferToken()).build())).build();
                    if(billing.launchBillingFlow(this,params).getResponseCode()!=BillingClient.BillingResponseCode.OK)message("No se pudo abrir el pago de Google Play.");
                }).show();});return;
            }
            priceUnavailable();if(checkout)message("El plan mensual aún no está disponible en Google Play.");
        });
    }
    @Override public void onResume(){super.onResume();if(billing!=null&&billing.isReady())restore();}
    @Override public void onDestroy(){if(billing!=null)billing.endConnection();worker.shutdown();super.onDestroy();}
}
