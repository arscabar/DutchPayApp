package kr.dutchpay;

import android.graphics.Rect;
import java.util.*;

final class ReceiptTotals {
    static void apply(Receipt receipt,List<OcrWord> words,String raw){
        List<ReceiptTotalText.Candidate> values=ReceiptTotalText.candidates(raw);
        List<OcrWord> expanded=new ArrayList<>();
        List<Integer> heights=new ArrayList<>();for(OcrWord w:words)if(w.number()!=null)heights.add(w.box.height());
        Collections.sort(heights);int height=heights.isEmpty()?0:heights.get(heights.size()/2);
        for(OcrWord w:words){
            String s=w.text.replaceAll("[^가-힣]","");
            if(height>0 && s.length()>=2 && s.length()<=8 && w.box.height()>height*2
                && w.box.height()>w.box.width()){
                for(int i=0;i<s.length();i++)expanded.add(new OcrWord(s.substring(i,i+1),new Rect(w.box.left,
                    w.box.top+w.box.height()*i/s.length(),w.box.right,w.box.top+w.box.height()*(i+1)/s.length())));
            }else expanded.add(w);
        }
        List<String> rows=new ArrayList<>();for(ColumnRows row:ColumnRows.group(expanded))rows.add(row.text());
        List<ReceiptTotalText.Candidate> geometry=ReceiptTotalText.candidates(String.join("\n",rows));
        Long text=ReceiptTotalText.choose(values);values.addAll(geometry);
        receipt.total=ReceiptTotalText.choose(values);
        if(receipt.total!=null && !receipt.total.equals(text))
            warn(receipt,"좌표로 연결한 전체 금액입니다. 원본 확인 필요");
        if(receipt.total==null && !values.isEmpty())warn(receipt,"서로 다른 전체 금액 후보가 있습니다. 원본 확인 필요");
        if(values.isEmpty()){
            receipt.total=CardTotalRecovery.read(words,raw);
            if(receipt.total!=null)warn(receipt,"카드전표의 반복 인쇄 금액·세금 금액열 구조로 복구했습니다. 원본 확인 필요");
        }
        receipt.validate();
    }
    static void warn(Receipt r,String message){if(!r.warnings.contains(message))r.warnings.add(message);}
}
