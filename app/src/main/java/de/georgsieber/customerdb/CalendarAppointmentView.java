package de.georgsieber.customerdb;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.georgsieber.customerdb.tools.ColorControl;

public class CalendarAppointmentView extends LinearLayout {
    public CalendarAppointmentView(Context context) {
        super(context);
    }
    public CalendarAppointmentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CalendarAppointmentView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setValues(String text, String subtitle, int backgroundColor) {
        TextView textViewTitle = findViewById(R.id.textViewAppointmentTitle);
        textViewTitle.setText(text);

        TextView textViewSubtitle = findViewById(R.id.textViewAppointmentSubtitle);
        textViewSubtitle.setText(subtitle);

        if(ColorControl.isColorDark(backgroundColor)) {
            textViewTitle.setTextColor(Color.WHITE);
            textViewSubtitle.setTextColor(Color.argb(180, 255, 255, 255));
        } else {
            textViewTitle.setTextColor(Color.BLACK);
            textViewSubtitle.setTextColor(Color.argb(180, 0, 0, 0));
        }

        if(ColorControl.isColorDark(backgroundColor)) {
            textViewTitle.setTextColor(Color.WHITE);
        } else {
            textViewTitle.setTextColor(Color.BLACK);
        }

        GradientDrawable border = new GradientDrawable();
        border.setCornerRadius(12);
        border.setColor(backgroundColor);
        border.setStroke((int)getResources().getDimension(R.dimen.hour_divider_height), getResources().getColor(R.color.divider_color));
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(border);
        } else {
            setBackground(border);
        }
    }
}
