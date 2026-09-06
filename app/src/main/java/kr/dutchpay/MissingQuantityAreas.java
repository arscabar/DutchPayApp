package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class MissingQuantityAreas {
    static final class Area {
        final Rect quantity,context;final OcrWord amount;
        Area(Rect quantity,Rect context,OcrWord amount){this.quantity=quantity;this.context=context;this.amount=amount;}
    }
    static List<Area> find(Bitmap page,List<PaddleLine> source){
        List<OcrWord> words=new ArrayList<>();for(var line:source)words.add(line.word);
        ColumnLayout c=ColumnLayout.find(words);List<Area> out=new ArrayList<>();
        if(c==null || c.price!=null || c.start>=c.rows.size())return out;
        float top=page.getHeight(),end=page.getHeight();
        for(var w:c.rows.get(c.start).words)top=Math.min(top,w.box.top);
        for(int i=c.start;i<c.rows.size();i++)if(ColumnRows.footer(c.rows.get(i).text())){
            for(var w:c.rows.get(i).words)end=Math.min(end,w.box.top);break;
        }
        if(end==page.getHeight())return out;
        List<OcrWord> body=new ArrayList<>(),known=new ArrayList<>();
        for(var w:words)if(w.y()>=top && w.y()<end){
            body.add(w);Long n=ColumnRows.number(w);
            if(c.column(w.x())==1 && n!=null && n>=1 && n<=9999)known.add(w);
        }
        float x=c.quantity,half=(c.amount-c.quantity)*.35f;
        if(known.size()>=2){
            known.sort(Comparator.comparingDouble(OcrWord::x));x=known.get(known.size()/2).x();
            List<Integer> widths=new ArrayList<>();for(var w:known)widths.add(w.box.width());Collections.sort(widths);
            half=Math.min(half,widths.get(widths.size()/2)*.75f+4);
        }
        for(var amount:body){Long value=ColumnRows.number(amount);
            if(value==null || value<=0 || c.column(amount.x())!=2 || Math.abs(amount.x()-c.amount)>half*3)continue;
            Area area=MissingQuantityRow.area(page,c,body,amount,x,half,top,end);
            if(area!=null)out.add(area);if(out.size()==24)break;
        }
        Set<Area> ambiguous=new HashSet<>();
        for(Area a:out)for(Area b:out)if(a!=b && Rect.intersects(a.quantity,b.quantity)){ambiguous.add(a);ambiguous.add(b);}
        out.removeAll(ambiguous);return out;
    }
}
