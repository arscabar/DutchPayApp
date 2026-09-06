package kr.dutchpay;

import android.graphics.*;

final class TextPatch implements AutoCloseable {
    final Bitmap image;
    private final Matrix toCrop, toPage;
    private final int width, height;

    private TextPatch(Bitmap image,Matrix toCrop,Matrix toPage,Bitmap page) {
        this.image=image; this.toCrop=toCrop; this.toPage=toPage;
        width=page.getWidth(); height=page.getHeight();
    }

    static TextPatch create(Bitmap page,Point[] p) {
        return create(page,p,false);
    }

    static TextPatch create(Bitmap page,Point[] p,boolean allowTall) {
        if(p==null || p.length!=4) return null;
        for(Point point:p) if(point==null) return null;
        int w=(int)Math.ceil(Math.max(distance(p[0],p[1]),distance(p[3],p[2])));
        int h=(int)Math.ceil(Math.max(distance(p[0],p[3]),distance(p[1],p[2])));
        if(w<8 || h<16 || (!allowTall && h>w*1.4) || (long)w*h>4_000_000) return null;
        float[] from=new float[8];
        for(int i=0;i<4;i++) { from[2*i]=p[i].x; from[2*i+1]=p[i].y; }
        Matrix forward=new Matrix(), backward=new Matrix();
        if(!forward.setPolyToPoly(from,0,new float[]{0,0,w,0,w,h,0,h},0,4)
            || !forward.invert(backward)) return null;
        Bitmap crop=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas=new Canvas(crop); canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(page,forward,new Paint(Paint.FILTER_BITMAP_FLAG));
        } catch(RuntimeException failure) { crop.recycle(); throw failure; }
        return new TextPatch(crop,forward,backward,page);
    }

    Rect map(Rect box,int offsetY) {
        RectF mapped=new RectF(box); mapped.offset(0,offsetY); toPage.mapRect(mapped);
        if(!Float.isFinite(mapped.left) || !Float.isFinite(mapped.top)
            || !Float.isFinite(mapped.right) || !Float.isFinite(mapped.bottom)) return null;
        Rect result=new Rect(Math.max(0,(int)Math.floor(mapped.left)),
            Math.max(0,(int)Math.floor(mapped.top)),Math.min(width,(int)Math.ceil(mapped.right)),
            Math.min(height,(int)Math.ceil(mapped.bottom)));
        return result.isEmpty()?null:result;
    }

    float[] mapPoints(float[] points) {
        float[] mapped=points.clone(); toPage.mapPoints(mapped); return mapped;
    }

    boolean contains(OcrWord word) {
        float[] center={word.x(),word.y()}; toCrop.mapPoints(center);
        return center[0]>=0 && center[1]>=0 && center[0]<image.getWidth()
            && center[1]<image.getHeight();
    }

    private static double distance(Point a,Point b) {
        return Math.hypot((double)b.x-a.x,(double)b.y-a.y);
    }

    public void close() { image.recycle(); }
}
