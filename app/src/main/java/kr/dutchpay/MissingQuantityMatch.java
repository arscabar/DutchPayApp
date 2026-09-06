package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class MissingQuantityMatch {
    static List<PaddleLine> centered(List<PaddleLine> lines,Rect box){
        List<PaddleLine> out=new ArrayList<>();
        for(var line:lines)if(box.contains(Math.round(line.word.x()),Math.round(line.word.y()))
            && Math.abs(line.word.y()-box.exactCenterY())<=box.height()*.35)out.add(line);
        return out;
    }
    static boolean replaced(PaddleLine old,PaddleLine found){
        if(ColumnRows.number(old.word)!=null)return false;
        Rect a=old.word.box,b=found.word.box,overlap=new Rect(a);
        if(!overlap.intersect(b))return false;
        long shared=overlap.width()*(long)overlap.height();
        boolean punctuated=old.word.text.matches("[\\p{P}\\p{S}\\s]*[0-9]{1,4}[\\p{P}\\p{S}\\s]*")
            && old.word.text.replaceAll("[^0-9]","").equals(found.word.text);
        return shared>=b.width()*(long)b.height()*.8
            && (shared>=a.width()*(long)a.height()*.6 || punctuated);
    }
}
