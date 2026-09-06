package kr.dutchpay;

import android.graphics.*;
import com.google.mlkit.vision.text.Text;

final class LineCrop {
    static Bitmap create(Bitmap page, Text.Line line, boolean straighten) {
        Rect r=line.getBoundingBox();
        Point[] p=line.getCornerPoints();
        if(!straighten || p==null || p.length!=4) {
            int l=Math.max(0,r.left), t=Math.max(0,r.top);
            return Bitmap.createBitmap(page,l,t,Math.min(page.getWidth(),r.right)-l,
                Math.min(page.getHeight(),r.bottom)-t);
        }
        int w=Math.max(1,(int)Math.hypot(p[1].x-p[0].x,p[1].y-p[0].y));
        int h=Math.max(1,(int)Math.hypot(p[3].x-p[0].x,p[3].y-p[0].y));
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        float[] from=new float[8];
        for(int j=0;j<4;j++){from[j*2]=p[j].x;from[j*2+1]=p[j].y;}
        Matrix m=new Matrix(); m.setPolyToPoly(from,0,new float[]{0,0,w,0,w,h,0,h},0,4);
        Canvas c=new Canvas(out); c.drawColor(Color.WHITE);
        c.drawBitmap(page,m,new Paint(Paint.FILTER_BITMAP_FLAG)); return out;
    }
}
