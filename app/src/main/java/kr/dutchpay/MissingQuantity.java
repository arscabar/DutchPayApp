package kr.dutchpay;

import android.graphics.*;
import android.os.Looper;
import java.util.*;
import org.json.*;

final class MissingQuantity {
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> source,KoreanModel model,JSONArray diagnostics){
        if(Looper.myLooper()==Looper.getMainLooper())return source;List<PaddleLine> result=null;
        try(var direct=new MissingQuantityDirect(model)){
            for(var area:MissingQuantityAreas.find(page,source)){
                Rect b=area.quantity;var detected=NumericStrip.read(page,area.context);
                List<PaddleLine> candidates=MissingQuantityMatch.centered(detected,b);
                JSONObject log=new JSONObject().put("box",box(b)).put("context",box(area.context))
                    .put("printedAmount",area.amount.text).put("accepted",false);
                log.put("contextMlkit",json(detected));
                if(candidates.isEmpty()){
                    var retry=NumericStrip.read(page,new Rect(area.context.left,b.top,area.context.right,b.bottom));
                    log.put("retryMlkit",json(retry));candidates=MissingQuantityMatch.centered(retry,b);
                }
                JSONArray readings=new JSONArray();for(var line:candidates)readings.put(line.json());
                log.put("mlkit",readings);diagnostics.put(log);
                if(candidates.isEmpty()){
                    var found=direct.read(page,b,source,log);
                    if(found!=null)result=apply(result,source,found,log);
                    continue;
                }
                if(candidates.size()!=1)continue;PaddleLine found=candidates.get(0);
                Rect cropBox=NumericAreas.bounds(page,found.word.box,found.word.box.left,found.word.box.right);
                Bitmap crop=Bitmap.createBitmap(page,cropBox.left,cropBox.top,cropBox.width(),cropBox.height()),padded=null;
                try{
                    padded=NamePatches.pad(crop);var reading=model.readWithConfidence(padded);
                    log.put("korean",reading.text).put("confidence",reading.confidence);
                    Long a=NumericRecovery.number(found.word.text),z=NumericRecovery.number(reading.text);
                    if(a!=null && a.equals(z) && !(reading.confidence>=.95)){
                        var plain=model.readWithConfidence(crop);log.put("directKorean",plain.text).put("directConfidence",plain.confidence);
                        if(a.equals(NumericRecovery.number(plain.text)))reading=plain;
                    }
                    if(a==null || a<1 || a>9999 || !a.equals(z) || !(reading.confidence>=.95))continue;
                    result=apply(result,source,new PaddleLine(Long.toString(a),found.points,reading.confidence),log);
                }finally{if(padded!=null)padded.recycle();if(crop!=page)crop.recycle();}
            }
        }catch(InterruptedException failure){Thread.currentThread().interrupt();}
        catch(Exception failure){try{diagnostics.put(new JSONObject().put("error",failure.getClass().getSimpleName()));}catch(JSONException ignored){}}
        return result==null?source:result;
    }
    private static List<PaddleLine> apply(List<PaddleLine> result,List<PaddleLine> source,PaddleLine found,JSONObject log) throws Exception {
        if(result==null)result=new ArrayList<>(source);JSONArray replaced=new JSONArray();
        result.removeIf(old->{if(!MissingQuantityMatch.replaced(old,found))return false;replaced.put(old.word.text);return true;});
        log.put("replacedFragments",replaced).put("accepted",true);result.add(found);return result;
    }
    private static JSONArray box(Rect b){return new JSONArray(Arrays.asList(b.left,b.top,b.right,b.bottom));}
    private static JSONArray json(List<PaddleLine> lines) throws Exception {
        JSONArray out=new JSONArray();for(var line:lines)out.put(line.json());return out;
    }
}
