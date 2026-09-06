package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class NumericAreas {
    final List<PaddleLine> originals=new ArrayList<>();
    final List<List<Rect>> boxes=new ArrayList<>();
    final Set<PaddleLine> quantities=new HashSet<>();
    static NumericAreas find(Bitmap page,List<PaddleLine> lines){
        NumericAreas out=new NumericAreas();List<OcrWord> labels=new ArrayList<>();
        for(var line:lines){ColumnRows row=new ColumnRows();row.words.add(line.word);labels.addAll(ColumnRows.headers(row));}
        for(OcrWord p:labels)if(p.text.equals("단가"))for(OcrWord q:labels)if(q.text.startsWith("수") && q.x()>p.x()
            && Math.abs(q.y()-p.y())<2*Math.max(p.box.height(),q.box.height()))
            for(OcrWord a:labels)if(a.text.equals("금액") && a.x()>q.x()
                && Math.abs(a.y()-q.y())<2*Math.max(a.box.height(),q.box.height())){
                float top=Math.max(p.box.bottom,Math.max(q.box.bottom,a.box.bottom)),end=page.getHeight();
                for(var line:lines)if(line.word.y()>top && ColumnRows.footer(line.word.text))end=Math.min(end,line.word.box.top);
                if(end==page.getHeight())continue;
                int split=Math.round((p.x()+q.x())/2);
                for(var line:lines){OcrWord w=line.word;Long n=ColumnRows.number(w);
                    if(w.y()<=top || w.y()>=end || !w.text.matches("[0-9][0-9,.\\s]*"))continue;
                    List<Rect> areas=new ArrayList<>();
                    if(n==null && w.box.left<split && w.box.right>q.x() && w.box.right<(q.x()+a.x())/2){
                        areas.add(bounds(page,w.box,w.box.left,split));areas.add(bounds(page,w.box,split,w.box.right));
                    }else if(n!=null && n>=0 && n<100 && Math.abs(w.x()-a.x())<(a.x()-q.x())*.4)
                        areas.add(bounds(page,w.box,w.box.left,w.box.right));
                    if(!areas.isEmpty()){out.originals.add(line);out.boxes.add(areas);}
                    if(out.originals.size()==24)break;
                }
                return out;
            }
        return out;
    }
    static Rect bounds(Bitmap page,Rect word,int left,int right){
        return NumericPatch.trim(page,new Rect(Math.max(0,left-2),Math.max(0,word.top-3),
            Math.min(page.getWidth(),right+2),Math.min(page.getHeight(),word.bottom+3)));
    }
}
