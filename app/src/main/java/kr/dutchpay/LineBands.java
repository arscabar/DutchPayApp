package kr.dutchpay;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.util.*;

final class LineBands {
    // Bright paper/dark ink; callers must rectify the crop before splitting.
    static List<Rect> split(Bitmap image) {
        return split(image,0);
    }

    static List<Rect> split(Bitmap image,double expectedLineHeight) {
        int w=image.getWidth(), h=image.getHeight();
        List<Rect> whole=Collections.singletonList(new Rect(0,0,w,h));
        if(h<16 || w<8) return whole;
        int threshold=threshold(image); if(threshold<0) return whole;
        int[] row=new int[w], counts=new int[h];
        for(int y=0;y<h;y++) {
            image.getPixels(row,0,w,0,y,w,1);
            for(int pixel:row) if(gray(pixel)<=threshold) counts[y]++;
        }
        // ponytail: conservative projection; overlapping/curved lines need a detector.
        int ink=Math.max(2,w/100), gap=Math.max(3,h/16), min=Math.max(6,h/7);
        if(expectedLineHeight>0 && Double.isFinite(expectedLineHeight)) {
            double size=Math.min(h,expectedLineHeight);
            ink=Math.max(1,w/100);
            gap=Math.max(3,(int)Math.round(size*.12)); min=Math.max(4,(int)Math.ceil(size*.35));
        }
        List<Rect> bands=new ArrayList<>(); int start=-1, last=-1;
        for(int y=0;y<=h+gap;y++) {
            if(y<h && counts[y]>=ink) { if(start<0) start=y; last=y; }
            else if(start>=0 && y-last>gap) {
                if(last-start+1>=min) bands.add(new Rect(0,start,w,last+1));
                start=-1;
            }
        }
        if(bands.size()<2) return whole;
        int max=0;
        for(Rect band:bands) max=Math.max(max,band.height());
        for(Rect band:bands) if(band.height()*2<max) return whole;
        List<Rect> out=new ArrayList<>(); int top=0;
        for(int i=0;i<bands.size()-1;i++) {
            int cut=(bands.get(i).bottom+bands.get(i+1).top)/2;
            out.add(new Rect(0,top,w,cut)); top=cut;
        }
        out.add(new Rect(0,top,w,h)); return out;
    }

    static int threshold(Bitmap image) {
        int w=image.getWidth(), h=image.getHeight();
        int[] row=new int[w], histogram=new int[256];
        for(int y=0;y<h;y++) {
            image.getPixels(row,0,w,0,y,w,1);
            for(int pixel:row) histogram[gray(pixel)]++;
        }
        long total=(long)w*h, dark=0, sum=0, partial=0;
        for(int i=0;i<256;i++) sum+=(long)i*histogram[i];
        double best=0; int threshold=-1;
        for(int i=0;i<255;i++) {
            dark+=histogram[i]; partial+=(long)i*histogram[i];
            if(dark==0 || dark==total) continue;
            double delta=partial/(double)dark-(sum-partial)/(double)(total-dark);
            double score=dark*(double)(total-dark)*delta*delta;
            if(score>best) { best=score; threshold=i; }
        }
        return threshold;
    }

    static int gray(int pixel) {
        return (77*((pixel>>16)&255)+150*((pixel>>8)&255)+29*(pixel&255))>>8;
    }
}
