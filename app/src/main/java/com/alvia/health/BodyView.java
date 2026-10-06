package com.alvia.health;



import android.animation.ValueAnimator;

import android.content.Context;

import android.graphics.*;

import android.util.LruCache;

import android.view.*;

import java.util.*;

import org.json.JSONObject;



/** One anatomical canvas with tissue-masked illumination of reported sensations. */

final class BodyView extends View {

    static final String[] SYSTEMS=SymptomAssociator.SYSTEMS;

    static final int YELLOW=0xffffe12b, RED=0xffff303f;

    private static final int[] IMAGES={R.drawable.system_nervous,R.drawable.system_circulatory,

            R.drawable.system_respiratory,R.drawable.system_urinary,R.drawable.system_endocrine,

            R.drawable.system_digestive,R.drawable.system_skeletal,R.drawable.system_reproductive,

            R.drawable.system_muscular};

    private static final LruCache<Integer,Bitmap> CACHE=new LruCache<Integer,Bitmap>(16*1024*1024) {

        @Override protected int sizeOf(Integer key,Bitmap image){return image.getAllocationByteCount();}

    };

    private static final int[] FEMALE_IMAGES={R.drawable.system_female_nervous,
            R.drawable.system_female_circulatory,R.drawable.system_female_respiratory,
            R.drawable.system_female_urinary,R.drawable.system_female_endocrine,
            R.drawable.system_female_digestive,0,R.drawable.system_female_reproductive,
            R.drawable.system_female_muscular};
    private final boolean female;
    static int resourceFor(int system,boolean female){return (female?FEMALE_IMAGES:IMAGES)[system];}

    final Set<Integer> selected=new LinkedHashSet<>();

    interface Listener { void select(int system); }

    Listener listener;

    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

    private final RectF imageRect=new RectF(), tileRect=new RectF();

    private final List<SymptomAssociator.Signal> signals=new ArrayList<>();

    private final Map<Integer,Bitmap> images=new HashMap<>();

    private final Map<Integer,Bitmap> layers=new HashMap<>();

    private Bitmap discomfortOverlay,painOverlay;

    private final int[] counts=new int[SYSTEMS.length];

    private ValueAnimator animator;

    private float pulse=.8f;

    private boolean animated=true;

    private static final Typeface LABEL_FONT=Typeface.create("sans-serif-medium",Typeface.NORMAL);



    BodyView(Context context){this(context,false);}
    BodyView(Context context,boolean female){super(context);this.female=female;selected.add(0);setFocusable(true);

        setContentDescription("Ilustraciones corporales. Amarillo: incomodidad; rojo: dolor. Asociaciones orientativas.");}

    void records(List<JSONObject> records){

        signals.clear();Arrays.fill(counts,0);

        StringBuilder accessible=new StringBuilder("Mapa corporal. ");

        for(JSONObject record:records){

            for(int system:RecordAssociations.systems(record))counts[system]++;

            if(!RecordAssociations.active(record))continue;

            for(SymptomAssociator.Signal signal:RecordAssociations.result(record).signals){

                if(signal.sensation==SymptomAssociator.OTHER)continue;

                signals.add(signal);

                if(selected.contains(signal.system))accessible.append(SYSTEMS[signal.system]).append(": ")

                        .append(signal.sensation==SymptomAssociator.PAIN?"dolor":"incomodidad")

                        .append(" en ").append(signal.region).append(". ");

            }

        }

        setContentDescription(accessible.toString());rebuildOverlays();updateAnimator();invalidate();

    }

    void setAnimated(boolean value){animated=value;updateAnimator();invalidate();}

    private void prepareImages(){

        for(int system:selected){

            if(system<0||system>=IMAGES.length)continue;

            int resource=resourceFor(system,female);if(resource==0)continue;
            Bitmap image=CACHE.get(resource);

            if(image==null){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;

                image=BitmapFactory.decodeResource(getResources(),resource,options);

                if(image!=null){if(image.getWidth()!=512||image.getHeight()!=768){Bitmap original=image;image=Bitmap.createScaledBitmap(original,512,768,true);original.recycle();}CACHE.put(resource,image);}}

            if(image!=null){images.put(system,image);layers.put(system,anatomyLayer(image,system));}

        }

    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();prepareImages();rebuildOverlays();updateAnimator();}

    @Override protected void onDetachedFromWindow(){stopAnimator();super.onDetachedFromWindow();}

    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);updateAnimator();}

    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);if(visible)updateAnimator();else stopAnimator();}

    private void stopAnimator(){if(animator!=null){animator.cancel();animator=null;}}

    private void updateAnimator(){

        boolean hasMarker=false;

        for(SymptomAssociator.Signal signal:signals)if(selected.contains(signal.system)){hasMarker=true;break;}

        if(!animated||!hasMarker||!isAttachedToWindow()||getWindowVisibility()!=VISIBLE||!ValueAnimator.areAnimatorsEnabled()){

            stopAnimator();pulse=.85f;return;}

        if(animator==null){animator=ValueAnimator.ofFloat(.22f,.95f);animator.setDuration(900);

            animator.setRepeatCount(ValueAnimator.INFINITE);animator.setRepeatMode(ValueAnimator.REVERSE);

            animator.addUpdateListener(value->{pulse=(float)value.getAnimatedValue();invalidate();});animator.start();}

    }

    private float dp(float value){return getResources().getDisplayMetrics().density*value;}

    // Extract the illustrated tissue in memory. Original imported resources stay unchanged.

    // Warm tissue contrasts with the common blue reference body; the skeleton is ivory.

    private Bitmap anatomyLayer(Bitmap source,int system){

        int w=source.getWidth(),h=source.getHeight();int[] pixels=new int[w*h];

        source.getPixels(pixels,0,w,0,0,w,h);

        for(int y=0;y<h;y++)for(int x=0;x<w;x++){

            int i=y*w+x,c=pixels[i],r=Color.red(c),g=Color.green(c),b=Color.blue(c);

            float nx=x/(float)w,ny=y/(float)h;

            boolean tissue=system==6?r>b*.92f&&g>b*.87f&&r>95:r>g*1.10f&&r>b*1.12f&&r>85;

            if(system==0||system==4)tissue=r>100&&g>65&&r>b*1.2f;

            // Gate the muscular illustration's warm background to the actual body silhouette.

            boolean inside=insideBody(nx,ny);

            if(system==3)inside &= ny>.32f&&ny<.53f;

            if(system==7)inside &= female?ny>.43f&&ny<.55f:ny>.40f&&ny<.60f;

            pixels[i]=tissue&&inside?c:Color.TRANSPARENT;

        }

        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);

    }

    private boolean insideBody(float x,float y){

        if(y<.005f||y>.989f)return false;

        float[] ys={0,.04f,.12f,.16f,.20f,.28f,.36f,.44f,.50f,.56f,.60f,.68f,.80f,.90f,.95f,.99f};

        float[] half=female?new float[]{.04f,.083f,.083f,.07f,.20f,.22f,.26f,.32f,.38f,.41f,.34f,.18f,.17f,.15f,.20f,.19f}:new float[]{.038f,.080f,.079f,.065f,.205f,.222f,.267f,.302f,.354f,.350f,.295f,.176f,.161f,.145f,.199f,.180f};

        float extent=half[half.length-1];

        for(int i=1;i<ys.length;i++)if(y<=ys[i]){float t=(y-ys[i-1])/(ys[i]-ys[i-1]);extent=half[i-1]+t*(half[i]-half[i-1]);break;}

        if(Math.abs(x-.5f)>extent)return false;

        if(y>.60f&&Math.abs(x-.5f)<.025f)return false;

        // Spaces between the arms and trunk, then between hands and legs.

        if(y>.32f&&y<.59f){float d=Math.abs(x-.5f);float inner=y<.48f?.135f:.16f;float outer=.19f+(y-.32f)*.34f;if(d>inner&&d<outer)return false;}

        return true;

    }

    private void rebuildOverlays(){

        if(layers.isEmpty())return;

        Bitmap reference=layers.values().iterator().next();int w=reference.getWidth(),h=reference.getHeight();

        int[] yellow=new int[w*h],red=new int[w*h];

        for(SymptomAssociator.Signal signal:signals){

            Bitmap layer=layers.get(signal.system);if(layer==null||!selected.contains(signal.system)||signal.x<0||signal.y<0)continue;

            int[] tissue=new int[w*h];layer.getPixels(tissue,0,w,0,0,w,h);

            float rx=.09f,ry=.055f;

            String region=signal.region;

            if(region.startsWith("Pierna")){rx=.065f;ry=.16f;}

            else if(region.startsWith("Brazo")){rx=.08f;ry=.13f;}

            else if(region.startsWith("Tórax")){rx=.17f;ry=.12f;}

            else if(region.startsWith("Abdomen")){rx=.15f;ry=.12f;}

            else if(region.startsWith("Cabeza")){rx=.10f;ry=.075f;}

            else if(region.startsWith("Espalda")){rx=.075f;ry=.18f;}

            else if(region.startsWith("Zona urinaria")){rx=.065f;ry=.06f;}

            int[] target=signal.sensation==SymptomAssociator.PAIN?red:yellow;

            int tint=signal.sensation==SymptomAssociator.PAIN?RED:YELLOW;

            for(int y=0;y<h;y++)for(int x=0;x<w;x++){

                int i=y*w+x;if(Color.alpha(tissue[i])==0)continue;

                float edge=Math.min((rx-Math.abs(x/(float)w-signal.x))/.015f,(ry-Math.abs(y/(float)h-signal.y))/.015f);

                if(edge<=0)continue;

                int alpha=(int)(255*Math.min(1,edge));

                // Preserve tissue texture in the illuminated surface.

                int c=tissue[i];float light=.65f+.35f*Math.max(Color.red(c),Math.max(Color.green(c),Color.blue(c)))/255f;

                if(alpha>Color.alpha(target[i]))target[i]=Color.argb(alpha,(int)(Color.red(tint)*light),(int)(Color.green(tint)*light),(int)(Color.blue(tint)*light));

            }

        }

        if(discomfortOverlay!=null)discomfortOverlay.recycle();if(painOverlay!=null)painOverlay.recycle();

        discomfortOverlay=Bitmap.createBitmap(yellow,w,h,Bitmap.Config.ARGB_8888);

        painOverlay=Bitmap.createBitmap(red,w,h,Bitmap.Config.ARGB_8888);

    }

    @Override protected void onDraw(Canvas canvas){

        super.onDraw(canvas);paint.setStyle(Paint.Style.FILL);paint.setColor(0xff07111e);paint.setAlpha(255);

        tileRect.set(0,0,getWidth(),getHeight());canvas.drawRoundRect(tileRect,dp(16),dp(16),paint);

        if(images.isEmpty()){
            paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(dp(14));
            canvas.drawText("Ilustración femenina del sistema óseo",getWidth()/2f,getHeight()/2f,paint);
            canvas.drawText("pendiente de añadir",getWidth()/2f,getHeight()/2f+dp(24),paint);return;
        }

        Bitmap base=images.get(selected.iterator().next());if(base==null)base=images.values().iterator().next();

        float availableHeight=getHeight()-dp(42),scale=Math.min(getWidth()/(float)base.getWidth(),availableHeight/base.getHeight());

        float w=base.getWidth()*scale,h=base.getHeight()*scale;

        imageRect.set((getWidth()-w)/2,dp(32)+(availableHeight-h)/2,(getWidth()+w)/2,dp(32)+(availableHeight+h)/2);

        paint.setColor(Color.WHITE);

        if(selected.size()==1)canvas.drawBitmap(base,null,imageRect,paint);

        else {

            // One coordinate frame for every layer: never tiles or separate figures.

            paint.setAlpha(65);canvas.drawBitmap(base,null,imageRect,paint);

            // Muscle and bone provide context behind the smaller organ systems.

            for(int system:new int[]{8,6,0,1,2,5,4,3,7})if(selected.contains(system)){

                Bitmap layer=layers.get(system);if(layer==null)continue;

                paint.setAlpha(system==8?120:system==6?165:235);canvas.drawBitmap(layer,null,imageRect,paint);

            }

        }

        paint.setAlpha((int)(255*pulse));

        if(discomfortOverlay!=null)canvas.drawBitmap(discomfortOverlay,null,imageRect,paint);

        if(painOverlay!=null)canvas.drawBitmap(painOverlay,null,imageRect,paint);

        paint.setAlpha(255);paint.setColor(Color.WHITE);paint.setTextAlign(Paint.Align.CENTER);

        paint.setTypeface(LABEL_FONT);paint.setTextSize(dp(14));

        canvas.drawText(selected.size()==1?SYSTEMS[selected.iterator().next()]:"Sistemas combinados",getWidth()/2f,dp(22),paint);

    }

    @Override public boolean performClick(){super.performClick();return true;}

}

