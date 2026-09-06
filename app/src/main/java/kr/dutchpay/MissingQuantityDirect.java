package kr.dutchpay;

import android.graphics.*;
import java.util.*;
import org.json.*;

final class MissingQuantityDirect implements AutoCloseable {
    private final KoreanModel korean;private KoreanModel english;
    MissingQuantityDirect(KoreanModel korean){this.korean=korean;}
    PaddleLine read(Bitmap page,Rect area,List<PaddleLine> source,JSONObject log) throws Exception {
        for(var line:source)if(area.contains(Math.round(line.word.x()),Math.round(line.word.y()))
            && line.word.text.matches(".*[가-힣A-Za-z].*"))return null;
        Bitmap crop=Bitmap.createBitmap(page,area.left,area.top,area.width(),area.height()),padded=null;
        try{
            Rect ink=inkBox(crop);log.put("directInk",ink!=null);
            if(ink==null)return null;
            padded=NamePatches.pad(crop);var first=korean.readWithConfidence(padded);
            log.put("directKorean",first.text).put("directConfidence",first.confidence);
            Long value=NumericRecovery.number(first.text);
            if(value==null || value<1 || value>9999 || !(first.confidence>=.95))return null;
            if(english==null)english=new KoreanModel(korean.context,"models/english.onnx","models/english-characters.json");
            var second=english.readWithConfidence(padded);
            log.put("directEnglish",second.text).put("directEnglishConfidence",second.confidence);
            if(!NumericEnglish.agrees(first,second))return null;
            ink.offset(area.left,area.top);
            log.put("directBox",new JSONArray(Arrays.asList(ink.left,ink.top,ink.right,ink.bottom)));
            return new PaddleLine(Long.toString(value),Arrays.asList(new PointF(ink.left,ink.top),
                new PointF(ink.right,ink.top),new PointF(ink.right,ink.bottom),new PointF(ink.left,ink.bottom)),first.confidence);
        }finally{if(padded!=null)padded.recycle();if(crop!=page)crop.recycle();}
    }
    static Rect inkBox(Bitmap image){
        int threshold=LineBands.threshold(image);if(threshold<0)return null;
        int w=image.getWidth(),h=image.getHeight(),count=0;long dark=0,light=0;
        int[] row=new int[w];
        for(int y=0;y<h;y++){
            image.getPixels(row,0,w,0,y,w,1);
            for(int pixel:row){int gray=LineBands.gray(pixel);
                if(gray<=threshold){dark+=gray;count++;}else light+=gray;}
        }
        long total=(long)w*h;
        if(count<total*.01 || count>total*.5 || light/(double)(total-count)-dark/(double)count<35)return null;
        Rect r=InkBounds.trim(image);
        if(r.height()<h*.5 || r.width()<r.height()*.12 || r.width()>r.height()*4.2
            || LineBands.split(image,h).size()!=1)return null;
        return r;
    }
    public void close() throws Exception {if(english!=null)english.close();}
}
