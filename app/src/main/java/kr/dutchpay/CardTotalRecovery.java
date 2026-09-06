package kr.dutchpay;

import java.util.*;

final class CardTotalRecovery {
    static Long read(List<OcrWord> words,String raw){
        String clean=ReceiptTotalText.clean(raw);
        if(!clean.startsWith("신용카드전표") || clean.contains("상품명")
            || clean.split("신용[승숭]인",-1).length>2)return null;
        List<OcrWord> numbers=new ArrayList<>();
        for(OcrWord w:words)if(w.number()!=null && w.number()>=0 && w.number()<=1000000000L)numbers.add(w);
        numbers.sort(Comparator.comparingDouble(OcrWord::y));
        Map<Long,Integer> supported=new HashMap<>();
        for(int i=2;i<numbers.size();i++){
            OcrWord total=numbers.get(i),vat=numbers.get(i-1);int j=i-2;
            if(vat.number()==0 && j>=1){vat=numbers.get(j);j--;}
            OcrWord supply=numbers.get(j);
            if(!total.text.matches(".*[,원].*") || supply.number()<=0 || vat.number()<=0)continue;
            float h=Math.max(supply.box.height(),total.box.height());
            if(total.y()-supply.y()>h*5 || Math.abs(total.box.right-supply.box.right)>h
                || Math.abs(total.box.right-vat.box.right)>h)continue;
            if(supply.number()+vat.number()==total.number())supported.merge(total.number(),1,Integer::sum);
        }
        Set<Long> values=new HashSet<>();for(var entry:supported.entrySet())if(entry.getValue()>=2)values.add(entry.getKey());
        // Recover an already printed number twice corroborated by receipt structure, never synthesize one.
        return values.size()==1?values.iterator().next():null;
    }
}
