package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class NumericQuantities {
    static NumericAreas find(Bitmap page,List<PaddleLine> lines){
        NumericAreas out=new NumericAreas();List<OcrWord> words=new ArrayList<>();
        for(var line:lines)words.add(line.word);
        ColumnLayout c=ColumnLayout.find(words);
        if(c==null || c.price!=null || c.start>=c.rows.size())return out;
        float top=c.rows.get(c.start).y(),end=page.getHeight();
        for(int i=c.start;i<c.rows.size();i++)if(ColumnRows.footer(c.rows.get(i).text())){
            for(OcrWord w:c.rows.get(i).words)end=Math.min(end,w.box.top);break;
        }
        for(var line:lines){OcrWord w=line.word;Long n=NumericRecovery.number(w.text);
            if(w.y()<top-w.box.height()*.5 || w.y()>=end || n==null || n<1 || n>9999
                || c.column(w.x())!=1 || Math.abs(w.x()-c.quantity)>(c.amount-c.quantity)*.25)continue;
            out.originals.add(line);out.quantities.add(line);
            out.boxes.add(Collections.singletonList(NumericAreas.bounds(page,w.box,w.box.left,w.box.right)));
            if(out.originals.size()==24)break;
        }
        return out.originals.size()>=2?out:new NumericAreas();
    }
    static List<String> read(Bitmap page,NumericAreas areas) throws Exception {
        Rect strip=new Rect();for(var row:areas.boxes)for(Rect box:row)strip.union(box);
        int margin=Math.max(8,strip.width());
        strip.set(Math.max(0,strip.left-margin),Math.max(0,strip.top-8),
            Math.min(page.getWidth(),strip.right+margin),Math.min(page.getHeight(),strip.bottom+8));
        return match(areas.boxes,NumericStrip.read(page,strip));
    }
    static List<String> match(List<List<Rect>> boxes,List<PaddleLine> found){
        List<String> out=new ArrayList<>();Map<PaddleLine,Integer> used=new IdentityHashMap<>();
        for(var row:boxes)for(Rect box:row){String value="";int matches=0;boolean shared=false;
            for(var line:found){OcrWord w=line.word;
                if(Math.abs(w.y()-box.exactCenterY())<box.height()*.5
                    && w.x()>box.left-box.width()*.5 && w.x()<box.right+box.width()*.5){
                    value=matches++==0?w.text:value+" | "+w.text;
                    Integer previous=used.putIfAbsent(line,out.size());
                    if(previous!=null && previous<out.size()){
                        shared=true;out.set(previous,out.get(previous)+" | shared");
                    }
                }
            }
            out.add(shared?value+" | shared":value);
        }
        return out;
    }
}
