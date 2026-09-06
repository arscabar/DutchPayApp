package kr.dutchpay;

import android.graphics.Rect;
import java.util.*;

final class QuantityRows {
    // Only attach orphan cells; preserve all already grouped quantities and raw OCR boxes.
    static List<OcrWord> align(List<OcrWord> words,ColumnLayout c,OcrWord q,OcrWord a){
        List<OcrWord> anchors=new ArrayList<>(),cells=new ArrayList<>();
        for(int k=c.start;k<c.rows.size();k++){
            ColumnRows row=c.rows.get(k);if(ColumnRows.footer(row.text()))break;
            if(c.value(row,c.amount)!=null){
                if(c.quantityValue(row)!=null)continue;
                List<OcrWord> amounts=new ArrayList<>();
                for(OcrWord w:row.words)if(c.column(w.x())==2 && ColumnRows.number(w)!=null)amounts.add(w);
                if(amounts.size()==1)anchors.add(amounts.get(0));
            }else for(OcrWord w:row.words){
                Long n=ColumnRows.quantity(w);
                if(n!=null && n!=0 && n>=-9999 && n<=9999 && c.column(w.x())==1
                    && w.box.left<q.box.right && w.box.right>q.box.left)cells.add(w);
            }
        }
        float shift=a.y()-q.y();Map<OcrWord,OcrWord> matched=NumericAlignment.match(anchors,cells,shift);
        Map<OcrWord,OcrWord> unique=new HashMap<>();
        for(var pair:matched.entrySet()){
            OcrWord cell=pair.getKey(),anchor=pair.getValue();
            if(!near(anchor,cell,shift))continue;
            if(anchors.stream().filter(w->near(w,cell,shift)).count()!=1
                || cells.stream().filter(w->near(anchor,w,shift)).count()!=1)continue;
            unique.put(cell,anchor);
        }
        if(unique.isEmpty())return words;
        List<OcrWord> out=new ArrayList<>();
        for(OcrWord w:words){
            OcrWord anchor=unique.get(w);
            if(anchor==null){out.add(w);continue;}
            Rect box=new Rect(w.box);box.offset(0,Math.round(anchor.y()-w.y()));
            out.add(new OcrWord(w.text,box));
        }
        return out;
    }
    private static boolean near(OcrWord anchor,OcrWord cell,float shift){
        return Math.abs(anchor.y()-cell.y()-shift)<=Math.max(anchor.box.height(),cell.box.height())*.45;
    }
}
