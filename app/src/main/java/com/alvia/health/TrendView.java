package com.alvia.health;
import android.view.View;
import android.content.Context;
import android.graphics.*;
import java.util.*;
final class TrendView extends View {
    private final java.util.List<Double> values;
    private final Paint p=new Paint(3);
    TrendView(Context c,java.util.List<Double> v){super(c);values=v;setContentDescription("Tendencia de las últimas "+v.size()+" mediciones, ordenadas por registro. Valores: "+v);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(values.isEmpty())return;float w=getWidth(),h=getHeight();double min=Collections.min(values),max=Collections.max(values),range=Math.max(1,max-min);p.setColor(0xffeeeaf4);p.setStrokeWidth(1);for(int i=1;i<4;i++)c.drawLine(8,h*i/4,w-8,h*i/4,p);Path line=new Path();for(int i=0;i<values.size();i++){float x=values.size()==1?w/2:12+(w-24)*i/(values.size()-1);float y=(float)(h-12-(values.get(i)-min)/range*(h-24));if(i==0)line.moveTo(x,y);else line.lineTo(x,y);p.setColor(0xff7550b8);c.drawCircle(x,y,4,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(0xff7550b8);c.drawPath(line,p);p.setStyle(Paint.Style.FILL);}
}
