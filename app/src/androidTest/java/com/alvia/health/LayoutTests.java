package com.alvia.health;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

/** Isolated layout measurements: no account, network or clinical records. */
public class LayoutTests extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();final Throwable[] failure={null};final int[] checks={0};
        runOnMainSync(()->{try{
            for(int width:new int[]{280,320,360,411,600,840})for(float scale:new float[]{1f,1.3f,2f}){
                Configuration config=new Configuration(getTargetContext().getResources().getConfiguration());config.fontScale=scale;
                Context c=getTargetContext().createConfigurationContext(config);float density=c.getResources().getDisplayMetrics().density;
                AdaptiveColumn page=new AdaptiveColumn(c);AdaptiveRow row=new AdaptiveRow(c);
                for(String name:new String[]{"Frecuencia cardíaca","Oxígeno en sangre"}){TextView text=new TextView(c);text.setText(name);text.setTextSize(28);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));}
                page.addView(row,new LinearLayout.LayoutParams(-1,-2));
                page.measure(View.MeasureSpec.makeMeasureSpec((int)(width*density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));page.layout(0,0,page.getMeasuredWidth(),page.getMeasuredHeight());
                if(row.getWidth()>page.getWidth()-page.getPaddingLeft()-page.getPaddingRight())throw new AssertionError("Horizontal overflow");
                if(scale>=1.3f&&row.getOrientation()!=LinearLayout.VERTICAL)throw new AssertionError("Large text not stacked");
                for(int i=0;i<row.getChildCount();i++){TextView child=(TextView)row.getChildAt(i);if(child.getLayout()==null||child.getHeight()<child.getLayout().getHeight())throw new AssertionError("Clipped text");}
                if(width>=840&&(page.getWidth()-2*page.getPaddingLeft())/density>721)throw new AssertionError("Unbounded tablet content");checks[0]++;
            }
        }catch(Throwable t){failure[0]=t;}});
        if(failure[0]!=null){result.putString("error",failure[0].toString());finish(Activity.RESULT_CANCELED,result);}
        else {result.putString("result",checks[0]+" viewport/font combinations passed");finish(Activity.RESULT_OK,result);}
    }
}
