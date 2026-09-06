package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class MissingQuantityRow {
    static MissingQuantityAreas.Area area(Bitmap page,ColumnLayout c,List<OcrWord> body,OcrWord amount,
            float x,float half,float top,float end){
        OcrWord anchor=null;double best=Double.MAX_VALUE;boolean named=false;
        for(var w:body)if(w.x()<x-half && w!=amount){
            double distance=Math.abs(w.y()-amount.y()),height=Math.max(w.box.height(),amount.box.height());
            if(distance>height*1.5)continue;
            boolean barcode=w.text.trim().matches("\\*?[0-9]{8,14}");
            boolean name=w.text.matches(".*[가-힣A-Za-z].*") && !ColumnRows.footer(w.text);
            named|=barcode || name;
            if(!barcode && !name && ColumnRows.number(w)==null)continue;
            double score=distance+(barcode?0:ColumnRows.number(w)!=null?height*.25:height*.6);
            if(score<best){anchor=w;best=score;}
        }
        if(anchor==null || !named || amount.x()<=anchor.x())return null;
        float y=anchor.y()+(amount.y()-anchor.y())*(x-anchor.x())/(amount.x()-anchor.x());
        float height=Math.max(anchor.box.height(),amount.box.height());
        float cropEnd=Math.min(page.getHeight(),Math.max(end,Math.max(anchor.box.bottom,amount.box.bottom)+3));
        for(var w:body)if(c.column(w.x())==1 && Math.abs(w.x()-x)<half*1.5
            && Math.abs(w.y()-y)<Math.max(height,w.box.height())*.65){
            ColumnRows row=new ColumnRows();row.words.add(w);
            if(c.quantityValue(row)!=null)return null;
        }
        Rect box=new Rect(Math.max(0,Math.round(x-half)),Math.max((int)top,Math.round(y-height*.6f)),
            Math.min(page.getWidth(),Math.round(x+half)),Math.min((int)cropEnd,Math.round(y+height*.6f)));
        if(box.isEmpty() || box.width()>512 || 16L*box.width()*box.height()>8000000)return null;
        box=NumericPatch.trim(page,box);
        Rect context=new Rect(box.left,Math.max((int)top,Math.min(box.top,amount.box.top)-3),
            Math.min(page.getWidth(),amount.box.right+4),Math.min((int)cropEnd,Math.max(box.bottom,amount.box.bottom)+3));
        return new MissingQuantityAreas.Area(box,context,amount);
    }
}
