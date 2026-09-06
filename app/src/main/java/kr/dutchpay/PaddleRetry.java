package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class PaddleRetry {
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> original,KoreanModel model) {
        List<PaddleLine> result=new ArrayList<>(original);
        List<Double> heights=new ArrayList<>();
        for(var l:original)heights.add(height(l));
        if(heights.isEmpty())return result;
        Collections.sort(heights);double median=heights.get(heights.size()/2);int tried=0,usedBands=0;
        for(var line:original) {
            if(tried>=6 || usedBands>=20)break;
            PointF a=line.points.get(0),b=line.points.get(1);
            double h=height(line),w=Math.hypot(a.x-b.x,a.y-b.y);
            if(h<median*1.6 && (h<=w*1.4 || h<median*1.2))continue;
            Point[] corners=new Point[4];for(int i=0;i<4;i++){
                PointF p=line.points.get(i);corners[i]=new Point(Math.round(p.x),Math.round(p.y));}
            try(TextPatch patch=TextPatch.create(page,corners,true)) {
                if(patch==null)continue;
                List<Rect> bands=LineBands.split(patch.image,median);
                if(bands.size()<2 || usedBands+bands.size()>20)continue;
                tried++;usedBands+=bands.size();List<PaddleLine> added=new ArrayList<>();int count=0;
                for(Rect band:bands) {
                    Bitmap crop=Bitmap.createBitmap(patch.image,0,band.top,band.width(),band.height());
                    String text;
                    try{text=model.read(crop);}finally{crop.recycle();}
                    if(text.trim().isEmpty()){added.clear();break;}
                    float[] q=patch.mapPoints(new float[]{0,band.top,band.right,band.top,
                        band.right,band.bottom,0,band.bottom});
                    boolean valid=true;for(float v:q)if(!Float.isFinite(v))valid=false;
                    if(!valid){added.clear();break;}
                    List<PointF> points=new ArrayList<>();for(int i=0;i<8;i+=2)points.add(new PointF(q[i],q[i+1]));
                    added.add(new PaddleLine(text,points));count+=text.replaceAll("\\s","").length();
                }
                if(added.size()==bands.size() && count>=line.word.text.replaceAll("\\s","").length()){
                    result.remove(line);result.addAll(added);
                }
            } catch(Exception failure) { /* Keep the original OCR candidate. */ }
        }
        return result;
    }
    static double height(PaddleLine l) {
        PointF a=l.points.get(0),b=l.points.get(3);return Math.hypot(a.x-b.x,a.y-b.y);
    }
}
