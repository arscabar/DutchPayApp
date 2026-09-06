package kr.dutchpay;

import android.graphics.*;

final class NumericPatch {
    // Numeric boxes may touch letters from the rows above/below. Keep the central ink band.
    static Rect trim(Bitmap page,Rect area){
        Bitmap crop=Bitmap.createBitmap(page,area.left,area.top,area.width(),area.height());
        try{
            int w=crop.getWidth(),h=crop.getHeight(),threshold=LineBands.threshold(crop);
            if(threshold<0)return area;
            int[] pixels=new int[w],ink=new int[h];
            for(int y=0;y<h;y++){
                crop.getPixels(pixels,0,w,0,y,w,1);
                for(int pixel:pixels)if(LineBands.gray(pixel)<=threshold)ink[y]++;
            }
            int start=-1,last=-1,gap=Math.max(1,h/40);
            for(int y=0;y<=h+gap;y++){
                if(y<h && ink[y]>=Math.max(1,w/200)){if(start<0)start=y;last=y;}
                else if(start>=0 && y-last>gap){
                    if(start<=h/2 && last>=h/2 && last-start+1>=Math.max(4,h/4))
                        return new Rect(area.left,area.top+Math.max(0,start-1),area.right,
                            area.top+Math.min(h,last+2));
                    start=-1;
                }
            }
            return area;
        }finally{if(crop!=page)crop.recycle();}
    }
}
