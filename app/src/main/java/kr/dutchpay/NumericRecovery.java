package kr.dutchpay;

import android.graphics.*;
import java.util.*;
import org.json.*;

final class NumericRecovery {
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> source,KoreanModel model){
        return read(page,source,model,new JSONArray());
    }
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> source,KoreanModel model,JSONArray diagnostics){
        NumericAreas areas=NumericAreas.find(page,source);
        if(areas.originals.isEmpty())areas=NumericQuantities.find(page,source);
        return read(page,source,model,areas,diagnostics);
    }
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> source,KoreanModel model,NumericAreas areas,JSONArray diagnostics){
        List<Bitmap> crops=new ArrayList<>();List<PaddleLine> result=null;
        try(var english=new NumericEnglish(model.context)){
            for(var row:areas.boxes)for(Rect b:row){
                Bitmap crop=Bitmap.createBitmap(page,b.left,b.top,b.width(),b.height());
                try{crops.add(NamePatches.pad(crop));}finally{if(crop!=page)crop.recycle();}
            }
            List<String> others=areas.quantities.isEmpty()?NameSheet.read(crops):NumericQuantities.read(page,areas);int index=0;
            for(int i=0;i<areas.originals.size();i++){
                List<PaddleLine> replacement=new ArrayList<>();List<JSONObject> regions=new ArrayList<>();boolean valid=true;int part=0;
                for(Rect b:areas.boxes.get(i)){
                    Bitmap crop=crops.get(index);var reading=model.readWithConfidence(crop);String other=others.get(index++);
                    Long value=number(reading.text),second=number(other);
                    boolean quantity=areas.quantities.contains(areas.originals.get(i)) || areas.boxes.get(i).size()==2 && part==1;
                    boolean agrees=value!=null && value.equals(second) && reading.confidence>=.9
                        && (!quantity || value>=1 && value<=9999);
                    var original=areas.originals.get(i);KoreanModel.Reading extra=null;
                    if(NumericEnglish.canCompare(other) && areas.quantities.contains(original) && source.contains(original)){
                        extra=english.read(crop,original.word.text,reading);
                        agrees=NumericEnglish.agrees(reading,extra);
                    }
                    valid&=agrees;JSONObject region=new JSONObject().put("old",areas.originals.get(i).word.text)
                        .put("box",new JSONArray(new int[]{b.left,b.top,b.right,b.bottom}))
                        .put("paddle",reading.text).put("confidence",reading.confidence).put("mlkit",other)
                        .put("valid",agrees).put("accepted",false);regions.add(region);diagnostics.put(region);
                    if(extra!=null)region.put("english",extra.text).put("englishConfidence",extra.confidence);
                    replacement.add(new PaddleLine(reading.text.trim(),Arrays.asList(new PointF(b.left,b.top),
                        new PointF(b.right,b.top),new PointF(b.right,b.bottom),new PointF(b.left,b.bottom))));part++;
                }
                if(valid && !(replacement.size()==1 && Objects.equals(number(replacement.get(0).word.text),
                    number(areas.originals.get(i).word.text)))){
                    if(result==null)result=new ArrayList<>(source);
                    result.remove(areas.originals.get(i));result.addAll(replacement);
                    for(JSONObject region:regions)region.put("accepted",true);
                }
            }
        }catch(InterruptedException failure){Thread.currentThread().interrupt();}
        catch(Exception failure){/* Keep only independently validated regions already applied. */}
        finally{for(Bitmap crop:crops)crop.recycle();}
        return result==null?source:result;
    }
    static Long number(String text){
        String s=text.replaceAll("\\s","");
        if(!s.matches("(?:[0-9]{1,9}|[0-9]{1,3}(?:,[0-9]{3}){1,2})"))return null;
        return Long.parseLong(s.replace(",",""));
    }
}
