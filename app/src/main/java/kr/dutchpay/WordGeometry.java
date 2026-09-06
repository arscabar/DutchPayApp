package kr.dutchpay;

import android.graphics.*;
import com.google.mlkit.vision.text.Text;
import java.util.*;

final class WordGeometry {
    static List<OcrWord> from(Text text) {
        return from(text,OcrWord.words(text));
    }
    static List<OcrWord> from(Text text,List<OcrWord> words) {
        List<Double> slopes=new ArrayList<>();
        for(var b:text.getTextBlocks())for(var l:b.getLines()) {
            Point[] p=l.getCornerPoints();
            if(p!=null && p[1].x-p[0].x>80)slopes.add((p[1].y-p[0].y)/(double)(p[1].x-p[0].x));
        }
        Collections.sort(slopes);
        return flatten(words,slopes.isEmpty()?0:slopes.get(slopes.size()/2));
    }
    static List<OcrWord> flatten(List<OcrWord> words,double slope) {
        List<OcrWord> out=new ArrayList<>();
        for(var w:words) {
            int cy=(int)Math.round(w.y()-slope*w.x());
            int height=Math.max(2,(int)Math.round(w.box.height()-Math.abs(slope)*w.box.width()));
            out.add(new OcrWord(w.text,new Rect(w.box.left,cy-height/2,w.box.right,cy+(height+1)/2)));
        }
        out.sort(Comparator.comparingDouble(OcrWord::y));return out;
    }
}
