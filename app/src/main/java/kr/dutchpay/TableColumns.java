package kr.dutchpay;

import java.util.*;

final class TableColumns {
    OcrWord price, quantity, amount, end;
    final List<OcrWord> names=new ArrayList<>(), prices=new ArrayList<>(), counts=new ArrayList<>();
    static TableColumns find(List<OcrWord> words){
        TableColumns c=new TableColumns();
        for(OcrWord w:words){
            String s=w.text.replaceAll("\\s","");
            if(c.price==null && s.matches("[단딘]가"))c.price=w;
            if(c.quantity==null && s.matches("수[량랑람당]"))c.quantity=w;
            if(c.amount==null && s.equals("금액"))c.amount=w;
        }
        if(c.price==null || c.quantity==null || c.amount==null
            || c.price.x()>=c.quantity.x() || c.quantity.x()>=c.amount.x())return null;
        float top=Math.max(c.price.y(),Math.max(c.quantity.y(),c.amount.y()));
        for(OcrWord w:words) if(w.y()>top && w.text.replaceAll("\\s","").matches(".*(합계|함계|과세).*")){c.end=w;break;}
        if(c.end==null)return null;
        float split=(c.price.x()+c.quantity.x())/2;
        for(OcrWord w:words){
            if(w.y()<=top+10 || w.y()>=c.end.y()-c.end.box.height())continue;
            if(w.number()!=null){
                if(w.x()>c.price.x()-c.price.box.width() && w.x()<split)c.prices.add(w);
                else if(w.x()>=split && w.x()<(c.quantity.x()+c.amount.x())/2)c.counts.add(w);
            }else if(w.text.matches(".*[가-힣].*") && w.x()<split)c.names.add(w);
        }
        return c;
    }
    List<OcrWord> nameRows(){
        List<OcrWord> rows=new ArrayList<>();
        for(OcrWord w:names){
            OcrWord last=rows.isEmpty()?null:rows.get(rows.size()-1);
            if(last!=null && Math.abs(last.y()-w.y())<Math.min(last.box.height(),w.box.height())*.5){
                last.text+=" "+w.text; last.box.union(w.box);
            }else rows.add(new OcrWord(w.text,new android.graphics.Rect(w.box)));
        }
        return rows;
    }
}
