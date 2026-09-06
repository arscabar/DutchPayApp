package kr.dutchpay;

import android.content.Context;
import android.graphics.Bitmap;
import java.util.*;
import org.json.*;

final class EnglishReview implements AutoCloseable {
    static final class Result {
        final String text;final JSONArray diagnostics;
        Result(String text,JSONArray diagnostics){this.text=text;this.diagnostics=diagnostics;}
    }
    private final Context context;private KoreanModel english;
    EnglishReview(Context context){this.context=context.getApplicationContext();}
    Result read(Bitmap source,String original,KoreanModel korean){
        JSONArray log=new JSONArray();String result=original;
        if(!EnglishSpans.eligible(original) || source.getWidth()*(long)source.getHeight()>2000000)return new Result(original,log);
        try{
            var line=EnglishSpans.read(source,korean);var tokens=EnglishSpans.tokens(original);
            if(!EnglishSpans.body(original).equals(EnglishSpans.body(line.text)) || tokens.size()!=line.spans.size())return new Result(original,log);
            int tried=0;
            for(int i=tokens.size()-1;i>=0 && tried<3;i--){
                var token=tokens.get(i);var span=line.spans.get(i);
                if(!EnglishSpans.eligible(span.text) || span.text.trim().length()<2)continue;
                tried++;
                if(english==null)english=new KoreanModel(context,"models/english.onnx","models/english-characters.json");
                List<KoreanModel.Reading> readings=new ArrayList<>();JSONArray views=new JSONArray();Set<String> bounds=new HashSet<>();
                for(double factor:new double[]{0,.1,.2}){
                    int pad=(int)Math.round(source.getHeight()*factor),l=Math.max(0,span.left-pad),r=Math.min(source.getWidth(),span.right+pad);
                    if(!bounds.add(l+":"+r))continue;
                    Bitmap crop=Bitmap.createBitmap(source,l,0,r-l,source.getHeight());
                    try{
                        var reading=english.readWithConfidence(crop);readings.add(reading);
                        views.put(new JSONObject().put("text",reading.text).put("score",reading.confidence).put("left",l).put("right",r));
                    }finally{if(crop!=source)crop.recycle();}
                }
                String next=EnglishChoice.choose(token.text,readings);
                log.put(new JSONObject().put("old",token.text).put("new",next).put("views",views).put("accepted",!next.equals(token.text)));
                result=result.substring(0,token.start)+next+result.substring(token.end);
            }
            if(!EnglishSpans.body(original).equals(EnglishSpans.body(result)))throw new IllegalStateException("Protected text changed");
        }catch(Exception failure){
            try{for(int i=0;i<log.length();i++)log.getJSONObject(i).put("accepted",false).put("rolledBack",true);}
            catch(JSONException ignored){}
            return new Result(original,log);
        }
        return new Result(result,log);
    }
    public void close() throws Exception {if(english!=null)english.close();}
}
