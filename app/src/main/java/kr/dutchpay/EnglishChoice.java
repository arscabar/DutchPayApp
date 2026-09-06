package kr.dutchpay;

import java.util.*;
import java.util.regex.*;

final class EnglishChoice {
    static String compact(String s){return s.replaceAll("\\s","");}
    static String punctuation(String s){return s.replaceAll("[A-Za-z0-9\\s]","");}
    static String digits(String s){Matcher m=Pattern.compile("^\\d+").matcher(s);return m.find()?m.group():"";}
    static boolean numberSafe(String original,String next){
        String a=compact(original),b=compact(next);
        if(a.length()!=b.length())return a.replaceAll("\\D","").equals(b.replaceAll("\\D",""));
        for(int i=0;i<a.length();i++){
            char x=a.charAt(i),y=b.charAt(i);if(x==y)continue;
            boolean dx=Character.isDigit(x),dy=Character.isDigit(y);
            if(!dx && !dy)continue;
            if(dx && dy)return false;
            char digit=dx?x:y,letter=dx?y:x;
            if(!(digit=='1' && (letter=='l' || letter=='I') || digit=='0' && (letter=='O' || letter=='o') || digit=='7' && letter=='l'))return false;
        }
        return true;
    }
    static String choose(String original,List<KoreanModel.Reading> readings){
        Map<String,List<KoreanModel.Reading>> groups=new HashMap<>();
        for(var r:readings)if(r.confidence>=.95 && EnglishSpans.eligible(r.text)
            && EnglishSpans.TOKEN.matcher(r.text.trim()).matches())
            groups.computeIfAbsent(compact(r.text),k->new ArrayList<>()).add(r);
        List<KoreanModel.Reading> chosen=null;
        for(var group:groups.values())if(group.size()>=2){if(chosen!=null)return original;chosen=group;}
        if(chosen==null)return original;
        String text=chosen.stream().max(Comparator.comparingDouble(r->r.confidence)).get().text.trim();
        if(!punctuation(original).equals(punctuation(text)) || !digits(original).equals(digits(text)) || !numberSafe(original,text))return original;
        return compact(original).equals(compact(text))?original:text;
    }
}
