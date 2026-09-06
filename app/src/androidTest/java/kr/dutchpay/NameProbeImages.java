package kr.dutchpay;

import android.graphics.*;

final class NameProbeImages {
    static Bitmap make(Bitmap source,String mode){
        if(mode.equals("base"))return source;
        Rect r=InkBounds.trim(source);
        if(mode.equals("trim"))return Bitmap.createBitmap(source,r.left,r.top,r.width(),r.height());
        int h=64,w=Math.max(1,Math.round((float)r.width()*h/r.height())),pad=8;
        Bitmap out=Bitmap.createBitmap(w+pad*2,h+pad*2,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(out);c.drawColor(Color.WHITE);Paint p=new Paint(Paint.FILTER_BITMAP_FLAG);
        if(mode.equals("contrast"))p.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            1.4f,0,0,0,-40,0,1.4f,0,0,-40,0,0,1.4f,0,-40,0,0,0,1,0})));
        c.drawBitmap(source,r,new Rect(pad,pad,w+pad,h+pad),p);return out;
    }
}
