package com.alvia.health;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Equal-width cards become a vertical list when text no longer fits comfortably. */
final class AdaptiveRow extends LinearLayout {
    AdaptiveRow(Context context){super(context);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        float density=getResources().getDisplayMetrics().density;
        boolean stack=MeasureSpec.getSize(widthSpec)/density<340||getResources().getConfiguration().fontScale>=1.3f;
        setOrientation(stack?VERTICAL:HORIZONTAL);
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);LayoutParams p=(LayoutParams)child.getLayoutParams();
            p.width=stack?LayoutParams.MATCH_PARENT:0;p.height=LayoutParams.WRAP_CONTENT;p.weight=stack?0:1;
            p.rightMargin=!stack&&i<getChildCount()-1?(int)(10*density):0;
            p.bottomMargin=stack&&i<getChildCount()-1?(int)(10*density):0;
        }
        super.onMeasure(widthSpec,heightSpec);
    }
}
