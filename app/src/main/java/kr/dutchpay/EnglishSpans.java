package kr.dutchpay;

import android.graphics.Bitmap;
import java.util.*;
import java.util.regex.*;

final class EnglishSpans {
    static final Pattern TOKEN=Pattern.compile("[A-Za-z0-9][A-Za-z0-9 .%/+_()−-]*");
    static final class Span {
        final int start,end,left,right;final String text;
        Span(Matcher m,int l,int r){start=m.start();end=m.end();text=m.group();left=l;right=r;}
    }
    static final class Line {
        final String text;final List<Span> spans;
        Line(String t,List<Span> s){text=t;spans=s;}
    }
    static List<Span> tokens(String text){
        List<Span> spans=new ArrayList<>();Matcher m=TOKEN.matcher(text);
        while(m.find())spans.add(new Span(m,0,0));return spans;
    }
    static String body(String text){
        Matcher m=TOKEN.matcher(text);StringBuffer out=new StringBuffer();
        while(m.find())m.appendReplacement(out,Matcher.quoteReplacement(eligible(m.group())?"":m.group()));
        m.appendTail(out);return out.toString().replaceAll("\\s","");
    }
    static boolean eligible(String text){return text.matches(".*[A-Za-z].*");}
    static Line read(Bitmap source,KoreanModel model) throws Exception {
        float[][] values=model.infer(source);StringBuilder text=new StringBuilder();
        List<Integer> begin=new ArrayList<>(),end=new ArrayList<>();int previous=0,from=0;
        for(int t=0;t<values.length;t++){
            float[] row=values[t];if(row.length!=model.chars.length())throw new IllegalStateException("Character table mismatch");
            int best=0;for(int k=1;k<row.length;k++)if(row[k]>row[best])best=k;
            if(best>0 && best!=previous){
                from=text.length();String word=model.chars.getString(best);text.append(word);
                for(int k=0;k<word.length();k++){begin.add(t);end.add(t);}
            }else if(best>0)for(int k=from;k<end.size();k++)end.set(k,t);
            previous=best;
        }
        double width=Math.max(320,Math.ceil(source.getWidth()*48.0/source.getHeight()));
        double unit=width*source.getHeight()/48/values.length;
        double[] centers=new double[begin.size()];
        for(int n=0;n<centers.length;n++)centers[n]=(begin.get(n)+end.get(n)+1)/2.0;
        List<Span> spans=new ArrayList<>();Matcher m=TOKEN.matcher(text);
        while(m.find()){
            int a=m.start(),b=m.end();
            int l=a==0?0:(int)Math.floor((centers[a-1]+centers[a])/2*unit);
            int r=b==centers.length?source.getWidth():(int)Math.ceil((centers[b-1]+centers[b])/2*unit);
            l=Math.max(0,Math.min(source.getWidth()-1,l));r=Math.max(l+1,Math.min(source.getWidth(),r));
            spans.add(new Span(m,l,r));
        }
        return new Line(text.toString(),spans);
    }
}
