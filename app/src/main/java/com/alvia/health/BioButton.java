package com.alvia.health;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

final class BioButton {
    static void style(TextView view,int background,int foreground){
        float density=view.getResources().getDisplayMetrics().density;
        view.setTextSize(14);view.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        view.setTextColor(foreground);view.setGravity(Gravity.CENTER);
        view.setPadding((int)(16*density),(int)(13*density),(int)(16*density),(int)(13*density));
        view.setMinHeight((int)(48*density));view.setLineSpacing(3*density,1);
        view.setBackgroundTintList(null);
        GradientDrawable shape=new GradientDrawable();shape.setColor(background);shape.setCornerRadius(16*density);view.setBackground(shape);
        view.setFocusable(true);view.setClickable(true);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName("android.widget.Button");}});
    }
}
