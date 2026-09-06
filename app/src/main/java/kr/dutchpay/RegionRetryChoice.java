package kr.dutchpay;

import java.util.*;
import org.json.*;

final class RegionRetryChoice {
    static boolean hangul(char c){return c>='가' && c<='힣';}
    static boolean singleDifference(String original,String other){
        if(original.length()!=other.length())return false;
        int changed=0;
        for(int i=0;i<original.length();i++)if(original.charAt(i)!=other.charAt(i)){
            if(!hangul(original.charAt(i)) || !hangul(other.charAt(i)))return false;
            changed++;
        }
        return changed==1;
    }
    static boolean candidate(char original,RegionRetryCtc.Glyph g,String other,int index){
        return g.text.charAt(0)==original && hangul(original) && g.alternative.length()==1
            && hangul(g.alternative.charAt(0)) && g.values[g.best]<.95 && g.values[g.second]>=.08
            && (other==null || other.charAt(index)==g.alternative.charAt(0));
    }
    static boolean eligible(String original,RegionRetryCtc base,String ml){
        String text=NameDecision.evidenceText(original),other=ml==null?null:NameDecision.evidenceText(ml);
        if(text.length()!=base.text.length() || (other!=null && !singleDifference(text,other)))return false;
        for(int i=0;i<text.length();i++)if(candidate(text.charAt(i),base.glyphs.get(i),other,i))return true;
        return false;
    }
    static String select(String original,RegionRetryCtc base,RegionRetryCtc center,String ml,JSONArray edits) throws Exception {
        String text=NameDecision.evidenceText(original),other=NameDecision.evidenceText(ml);
        if(!eligible(original,base,ml) || text.length()!=center.text.length())return original;
        String compact=original.replaceAll("\\s","");int skip=compact.length()-text.length();
        if(skip<0 || !compact.substring(skip).equals(text))return original;
        List<Integer> positions=new ArrayList<>();
        for(int i=0;i<original.length();i++)if(!original.substring(i,i+1).matches("\\s"))positions.add(i);
        StringBuilder out=new StringBuilder(original);
        for(int i=0;i<text.length();i++){
            char old=text.charAt(i),next=other.charAt(i);var first=base.glyphs.get(i);var second=center.glyphs.get(i);
            if(!candidate(old,first,other,i) || (center.text.charAt(i)!=old && center.text.charAt(i)!=next))continue;
            float p=first.values[first.second],q=second.values[first.second];
            float before=first.values[first.best],after=second.values[first.best];
            if(!(q>p && after<before))continue;
            out.setCharAt(positions.get(i+skip),next);
            edits.put(new JSONObject().put("index",i).put("old",String.valueOf(old)).put("new",String.valueOf(next))
                .put("baseOld",before).put("baseNew",p).put("centerOld",after).put("centerNew",q));
        }
        return out.toString();
    }
}
