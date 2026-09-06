package kr.dutchpay;

import com.google.mlkit.vision.text.Text;
import java.util.*;

final class TableParser {
    static Receipt parse(Text text){
        return parseWords(OcrWord.words(text),String.join("\n",Layout.rows(text)));
    }
    static Receipt parseWords(List<OcrWord> words,String raw){
        TableColumns c=TableColumns.find(words);
        if(c==null)return null;
        List<OcrWord> names=c.nameRows(); if(names.isEmpty())return null;
        Receipt r=new Receipt(); r.raw=raw;
        Set<OcrWord> used=new HashSet<>(); int found=0;
        for(int i=0;i<names.size();i++){
            OcrWord name=names.get(i);
            float start=name.y()-name.box.height()*.25f;
            float end=i+1<names.size()?names.get(i+1).y()-names.get(i+1).box.height()*.25f:c.end.y();
            OcrWord price=null;
            for(OcrWord p:c.prices)if(p.y()>=start && p.y()<end){price=p;break;}
            if(price==null){ r.warnings.add("단가 미인식: "+name.text); continue; }
            found++; long unit=price.number(); OcrWord count=null;
            float expected=price.y()+c.quantity.y()-c.price.y();
            for(OcrWord q:c.counts)if(!used.contains(q) && q.number()>0 && q.number()<1000
                && Math.abs(q.y()-expected)<price.box.height()*.65
                && (count==null || Math.abs(q.y()-expected)<Math.abs(count.y()-expected)))count=q;
            int quantity=count==null?1:count.number().intValue(); if(count!=null)used.add(count);
            Item item=new Item(name.text.replaceFirst("^[▶►]+\\s*",""),unit,quantity,unit*quantity);
            if(count==null)item.warning="수량 미인식: 1로 표시, 확인 필요";
            if(price.text.matches(".*[gqOoIl].*"))item.warning="숫자의 문자 혼동을 보정했습니다. 확인 필요";
            r.items.add(item);
        }
        if(found<names.size()*.7)return null;
        Long subtotal=nearAmount(words,c,c.end);
        if(subtotal!=null){
            long discount=0;
            for(OcrWord w:words)if(w.y()>c.end.y() && w.text.equals("할인")){
                Long value=nearAmount(words,c,w); if(value!=null){discount=Math.abs(value);break;}
            }
            if(discount>0)r.items.add(new Item("할인",-discount,1,-discount));
            r.total=subtotal-discount;
        }
        r.validate(); return r;
    }
    static Long nearAmount(List<OcrWord> words,TableColumns c,OcrWord label){
        return words.stream().filter(w -> w.x()>c.quantity.x() && w.number()!=null
            && w.y()<=label.y()+label.box.height()*.4 && w.y()>label.y()-label.box.height()*1.5)
            .min(Comparator.comparingDouble(w -> Math.abs(w.y()-label.y()-c.amount.y()+c.price.y())))
            .map(OcrWord::number).orElse(null);
    }
}
