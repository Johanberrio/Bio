package com.alvia.health;

import android.content.Context;
import android.widget.LinearLayout;
import android.view.View;

/** Re-measures against the actual window, including split-screen and rotation. */
final class AdaptiveColumn extends LinearLayout {
    AdaptiveColumn(Context context){super(context);setOrientation(VERTICAL);}
    static int gutter(int width){return Math.max(width<360?12:24,(width-720)/2);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        float density=getResources().getDisplayMetrics().density;
        int width=MeasureSpec.getSize(widthSpec);int side=(int)(gutter((int)(width/density))*density);
        setPadding(side,getPaddingTop(),side,getPaddingBottom());
        super.onMeasure(widthSpec,heightSpec);
    }
}
