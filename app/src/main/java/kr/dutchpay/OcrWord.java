package kr.dutchpay;

import com.google.mlkit.vision.text.Text;
import android.graphics.Rect;
import java.util.*;

final class OcrWord {
    String text; final Rect box;
    OcrWord(String text,Rect box){this.text=text;this.box=box;}
    float x(){return box.exactCenterX();}
    float y(){return box.exactCenterY();}
    boolean join(OcrWord next){
        if(!text.matches("-?\\d+[,.]") || !next.text.matches("\\d{3}")
            || next.box.left<box.right-box.height()*.2
            || Math.abs(y()-next.y())>Math.min(box.height(),next.box.height())*.5)return false;
        text+=next.text; box.union(next.box); return true;
    }
    static List<OcrWord> words(Text text){
        List<OcrWord> words=new ArrayList<>();
        for(Text.TextBlock b:text.getTextBlocks()) for(Text.Line l:b.getLines()) {
            OcrWord previous=null;
            for(Text.Element e:l.getElements()) if(e.getBoundingBox()!=null){
                OcrWord w=new OcrWord(e.getText(),e.getBoundingBox());
                if(previous!=null && previous.join(w))continue;
                boolean duplicate=words.stream().anyMatch(p -> p.text.equals(w.text)
                    && Math.abs(p.x()-w.x())<w.box.width()*.6 && Math.abs(p.y()-w.y())<w.box.height()*.5);
                if(!duplicate){words.add(w); previous=w;}
            }
        }
        words.sort(Comparator.comparingDouble(OcrWord::y)); return words;
    }
    Long number(){
        String s=Numbers.clean(text).replace(" ","");
        if(!s.matches("-?[0-9gqOoIl]+(?:[,][0-9gqOoIl]{3})*"))return null;
        s=s.replace('g','9').replace('q','9').replace('O','0').replace('o','0')
            .replace('I','1').replace('l','1').replace(",","");
        try{return Long.parseLong(s);}catch(NumberFormatException e){return null;}
    }
}
