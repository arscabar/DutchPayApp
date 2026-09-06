package kr.dutchpay;

import android.content.Context;
import android.graphics.Bitmap;

final class NumericEnglish implements AutoCloseable {
    private final Context context;private KoreanModel model;
    NumericEnglish(Context context){this.context=context;}
    static boolean canCompare(String other){return NumericRecovery.number(other)==null && !other.contains(" | ");}
    static boolean candidate(String old,KoreanModel.Reading reading){
        Long before=NumericRecovery.number(old),after=NumericRecovery.number(reading.text);
        return before!=null && before>=1 && before<=9999 && after!=null && after>=1 && after<=9999
            && !before.equals(after) && reading.confidence>=.95;
    }
    KoreanModel.Reading read(Bitmap crop,String old,KoreanModel.Reading reading) throws Exception {
        if(!candidate(old,reading))return null;
        if(model==null)model=new KoreanModel(context,"models/english.onnx","models/english-characters.json");
        return model.readWithConfidence(crop);
    }
    static boolean agrees(KoreanModel.Reading a,KoreanModel.Reading b){
        Long value=NumericRecovery.number(a.text);
        return b!=null && a.confidence>=.95 && b.confidence>=.95 && value!=null && value>=1 && value<=9999
            && value.equals(NumericRecovery.number(b.text));
    }
    public void close() throws Exception {if(model!=null)model.close();}
}
