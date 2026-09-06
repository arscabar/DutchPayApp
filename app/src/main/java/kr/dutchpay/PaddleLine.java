package kr.dutchpay;

import android.graphics.*;
import org.json.*;
import java.util.*;

final class PaddleLine {
    final OcrWord word;
    final List<PointF> points;
    final double confidence;
    PaddleLine(String text,List<PointF> points) {
        this(text,points,Double.NaN);
    }
    PaddleLine(String text,List<PointF> points,double confidence) {
        this.points=points;this.confidence=confidence;
        RectF r=new RectF(points.get(0).x,points.get(0).y,points.get(0).x,points.get(0).y);
        for(PointF p:points){r.left=Math.min(r.left,p.x);r.top=Math.min(r.top,p.y);
            r.right=Math.max(r.right,p.x);r.bottom=Math.max(r.bottom,p.y);}
        Rect box=new Rect();r.roundOut(box);word=new OcrWord(text,box);
    }
    JSONObject json() throws Exception {
        JSONArray quad=new JSONArray();
        for(PointF p:points)quad.put(new JSONArray().put(p.x).put(p.y));
        return new JSONObject().put("text",word.text).put("quad",quad);
    }
    double y(double slope){return word.y()-word.x()*slope;}
    static List<List<PaddleLine>> groups(List<PaddleLine> lines) {
        List<Double> slopes=new ArrayList<>();
        for(var l:lines){var a=l.points.get(0);var b=l.points.get(1);
            if(b.x-a.x>80)slopes.add((double)(b.y-a.y)/(b.x-a.x));}
        Collections.sort(slopes); double slope=slopes.isEmpty()?0:slopes.get(slopes.size()/2);
        var sorted=new ArrayList<>(lines); sorted.sort(Comparator.comparingDouble(l->l.y(slope)));
        List<List<PaddleLine>> groups=new ArrayList<>();
        for(var l:sorted) {
            var last=groups.isEmpty()?null:groups.get(groups.size()-1);
            if(last==null || Math.abs(l.y(slope)-last.get(0).y(slope))>
                Math.min(l.word.box.height(),last.get(0).word.box.height())*.55){
                last=new ArrayList<>();groups.add(last);}
            last.add(l);
        }
        for(var g:groups)g.sort(Comparator.comparingDouble(l->l.word.x()));
        return groups;
    }
    static List<String> rows(List<PaddleLine> lines) {
        List<String> rows=new ArrayList<>();
        for(var g:groups(lines)){
            List<String> s=new ArrayList<>();for(var l:g)s.add(l.word.text);rows.add(String.join("\t",s));}
        return rows;
    }
}
