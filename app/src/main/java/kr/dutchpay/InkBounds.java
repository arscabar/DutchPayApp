package kr.dutchpay;

import android.graphics.Bitmap;
import android.graphics.Rect;

final class InkBounds {
    // Bright paper/dark ink. Whole ink extent preserves punctuation and neighboring ink.
    static Rect trim(Bitmap image) {
        int w=image.getWidth(), h=image.getHeight(), threshold=LineBands.threshold(image);
        if(threshold<0) return new Rect(0,0,w,h);
        int left=w, top=h, right=0, bottom=0; int[] row=new int[w];
        for(int y=0;y<h;y++) {
            image.getPixels(row,0,w,0,y,w,1);
            for(int x=0;x<w;x++) if(LineBands.gray(row[x])<=threshold) {
                left=Math.min(left,x); top=Math.min(top,y);
                right=Math.max(right,x+1); bottom=Math.max(bottom,y+1);
            }
        }
        if(right<=left || bottom<=top) return new Rect(0,0,w,h);
        return new Rect(Math.max(0,left-2),Math.max(0,top-2),
            Math.min(w,right+2),Math.min(h,bottom+2));
    }
}
