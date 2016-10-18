package com.mvbarcodedemo.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public class CustomStatusLayout extends View {

    private boolean isStatusOK = false;
    private String code = "";

    public CustomStatusLayout(Context context) {
        super(context);
    }

    public CustomStatusLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        if (isStatusOK)
            paint.setColor(Color.parseColor("#00c853"));
        else
            paint.setColor(Color.parseColor("#f44336"));

        //float left, float top, float right, float bottom, Paint paint
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        paint.setColor(Color.GRAY);
        canvas.drawRect(getWidth() - 2, 0, getWidth(), getHeight(), paint);

        if (!TextUtils.isEmpty(code)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.parseColor("#0277bd"));
            paint.setTextSize(30);
            canvas.drawText(code, getWidth() / 3, getHeight() / 2, paint);
        }
    }

    public void setStatusOK(boolean statusOK) {
        isStatusOK = statusOK;
        invalidate();
    }

    public void setCode(String barcode) {
        code = barcode;
        invalidate();
    }
}
