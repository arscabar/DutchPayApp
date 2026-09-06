package kr.dutchpay;

import android.graphics.Bitmap;
import java.util.*;

final class MenuViews {
    static KoreanModel.Reading center(Bitmap source,KoreanModel model) throws Exception {
        int margin=Math.max(1,Math.round(source.getHeight()*.1f));
        if(source.getHeight()<=margin*2)return new KoreanModel.Reading("",0);
        Bitmap crop=Bitmap.createBitmap(source,0,margin,source.getWidth(),source.getHeight()-margin*2);
        try{return model.readWithConfidence(crop);}finally{if(crop!=source)crop.recycle();}
    }
    static List<KoreanModel.Reading> read(Bitmap source,KoreanModel model) throws Exception {
        List<KoreanModel.Reading> out=new ArrayList<>();
        out.add(center(source,model));
        for(float factor:new float[]{.8f,1.2f}){
            Bitmap scaled=Bitmap.createScaledBitmap(source,Math.max(1,Math.round(source.getWidth()*factor)),source.getHeight(),true);
            try{out.add(model.readWithConfidence(scaled));}finally{if(scaled!=source)scaled.recycle();}
        }
        return out;
    }
}
