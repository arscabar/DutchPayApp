package kr.dutchpay;

import android.graphics.Bitmap;
import org.json.*;

/** Worker-thread helper: preserve the final name unless two pixel views support ML's glyph. */
final class RegionRetry {
    static final class Result {
        final String text;final JSONObject diagnostics;
        Result(String text,JSONObject diagnostics){this.text=text;this.diagnostics=diagnostics;}
    }
    static Result read(Bitmap source,Bitmap padded,String original,KoreanModel model,NameSingle single){
        JSONObject log=new JSONObject();JSONArray edits=new JSONArray();Bitmap center=null;
        try{
            log.put("accepted",false);
            var base=RegionRetryCtc.read(model.infer(source),model.chars);
            if(!RegionRetryChoice.eligible(original,base,null))return new Result(original,log);
            String ml=single.read(padded);log.put("other",ml);
            if(!RegionRetryChoice.eligible(original,base,ml))return new Result(original,log);
            int margin=Math.max(1,Math.round(source.getHeight()*.1f));
            if(source.getHeight()<=margin*2)return new Result(original,log);
            center=Bitmap.createBitmap(source,0,margin,source.getWidth(),source.getHeight()-margin*2);
            var narrow=RegionRetryCtc.read(model.infer(center),model.chars);
            String next=RegionRetryChoice.select(original,base,narrow,ml,edits);
            log.put("edits",edits).put("accepted",!next.equals(original));return new Result(next,log);
        }catch(Exception failure){
            try{log.put("accepted",false).put("error",failure.getClass().getSimpleName()).put("edits",new JSONArray());}
            catch(JSONException ignored){}
            return new Result(original,log);
        }finally{if(center!=null && center!=source)center.recycle();}
    }
}
