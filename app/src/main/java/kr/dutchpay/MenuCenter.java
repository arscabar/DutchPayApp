package kr.dutchpay;

import android.graphics.Bitmap;
import org.json.*;

final class MenuCenter {
    static KoreanModel.Reading read(Bitmap source,double original,KoreanModel.Reading base,
            String name,KoreanModel model,JSONArray diagnostic) throws Exception {
        if(!Double.isFinite(original) || Math.max(original,base.confidence)>=.9)return null;
        KoreanModel.Reading a=center(source,.6,model),b=center(source,.7,model);
        boolean accept=NameDecision.sameText(a.text,b.text)
            && NameDecision.candidate(name,a.text) && NameDecision.sameNumbers(name,a.text)
            && Math.min(a.confidence,b.confidence)>=.9 && Math.max(a.confidence,b.confidence)>=.97;
        diagnostic.put(new JSONObject().put("old",name).put("center60",a.text).put("center70",b.text)
            .put("score60",a.confidence).put("score70",b.confidence).put("accepted",accept));
        return accept?a:null;
    }
    private static KoreanModel.Reading center(Bitmap source,double ratio,KoreanModel model) throws Exception {
        int h=Math.max(1,(int)Math.round(source.getHeight()*ratio));
        Bitmap crop=Bitmap.createBitmap(source,0,(source.getHeight()-h)/2,source.getWidth(),h);
        try{return model.readWithConfidence(crop);}finally{if(crop!=source)crop.recycle();}
    }
}
