package kr.dutchpay;

import android.graphics.*;
import org.json.*;
import java.util.*;
import java.util.regex.*;

final class PaddleTrim {
    private static final Pattern NUMBERS=Pattern.compile("[-−]?\\d[\\d,]*");
    static final class Result {
        final List<PaddleLine> lines;
        final List<JSONObject> diagnostics=new ArrayList<>();
        Result(List<PaddleLine> source){lines=new ArrayList<>(source);}
    }
    static Result read(Bitmap page,List<PaddleLine> source,KoreanModel model) throws Exception {
        Result out=new Result(source);List<OcrWord> words=new ArrayList<>();List<Double> slopes=new ArrayList<>();
        for(var l:source){words.add(l.word);var a=l.points.get(0);var b=l.points.get(1);
            if(b.x-a.x>80)slopes.add((double)(b.y-a.y)/(b.x-a.x));}
        Collections.sort(slopes);
        Receipt r=ColumnReceipt.parse(WordGeometry.flatten(words,slopes.isEmpty()?0:slopes.get(slopes.size()/2)),
            String.join("\n",PaddleLine.rows(source)));
        if(r==null)return out;
        List<String> names=new ArrayList<>();for(Item i:r.items)if(i.baseAmount()>0)names.add(compact(i.name));
        int tried=0;
        for(PaddleLine line:source){
            String old=line.word.text;
            if(!old.matches(".*[가-힣].*") || names.stream().noneMatch(n->n.contains(compact(old))))continue;
            if(tried++==6)break;
            JSONObject d=new JSONObject().put("old",old).put("base","").put("new","")
                .put("baseConfidence",JSONObject.NULL).put("newConfidence",JSONObject.NULL).put("accepted",false);
            out.diagnostics.add(d);Point[] p=new Point[4];
            for(int i=0;i<4;i++){PointF q=line.points.get(i);p[i]=new Point(Math.round(q.x),Math.round(q.y));}
            try(TextPatch patch=TextPatch.create(page,p)){
                if(patch==null){d.put("note","patch rejected");continue;}
                KoreanModel.Reading base=model.readWithConfidence(patch.image);
                d.put("base",base.text).put("baseConfidence",base.confidence);
                if(!old.equals(base.text)){d.put("note","base differs from original: confidence is not comparable");continue;}
                Rect b=InkBounds.trim(patch.image);
                if(b.width()>patch.image.getWidth()*.85 && b.height()>patch.image.getHeight()*.85){
                    d.put("note","trim below threshold");continue;}
                if(base.confidence>=.95){d.put("note","base confidence already high");continue;}
                Bitmap crop=Bitmap.createBitmap(patch.image,b.left,b.top,b.width(),b.height());
                KoreanModel.Reading next;
                try{next=model.readWithConfidence(crop);}finally{if(crop!=patch.image)crop.recycle();}
                boolean accepted=accepts(base,next);
                d.put("new",next.text).put("newConfidence",next.confidence).put("accepted",accepted)
                    .put("note","CTC mean score is not accuracy probability");
                if(accepted)out.lines.set(out.lines.indexOf(line),new PaddleLine(next.text,line.points));
            }catch(Exception e){d.put("note","retained original: "+e.getClass().getSimpleName());}
        }
        return out;
    }
    static boolean accepts(KoreanModel.Reading base,KoreanModel.Reading next){
        return base.confidence>=0 && base.confidence<.95 && next.confidence<=1
            && next.confidence>=base.confidence+.03 && next.text.matches(".*[가-힣].*")
            && compact(next.text).length()>=compact(base.text).length()
            && numericTokens(base.text).equals(numericTokens(next.text));
    }
    private static List<String> numericTokens(String text){
        List<String> tokens=new ArrayList<>();Matcher found=NUMBERS.matcher(text);
        while(found.find())tokens.add(found.group());return tokens;
    }
    static String compact(String s){return s.replaceAll("\\s","");}
}
