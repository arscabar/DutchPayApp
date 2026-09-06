package kr.dutchpay;

import android.graphics.*;
import android.os.Looper;
import java.util.*;

final class QuantityRecovery {
    private static final class Area {
        final Rect box;final float x;final double median;
        Area(Rect box,float x,double median){this.box=box;this.x=x;this.median=median;}
    }

    // Called by PaddleScan's worker. Never block the UI or derive quantities from prices.
    static List<PaddleLine> read(Bitmap page,List<PaddleLine> source) {
        if(Looper.myLooper()==Looper.getMainLooper())return source;
        try {
            Area area=find(page,source);if(area==null)return source;Rect b=area.box;
            List<PaddleLine> found=NumericStrip.read(page,b);
            List<PaddleLine> result=merge(source,found,b,area.x,area.median);
            Rect tight=narrow(found,b,area.x,area.median);
            if(tight!=null)try{return fill(result,NumericStrip.read(page,tight),b,area.x,area.median);}
                catch(Exception failure){if(failure instanceof InterruptedException)Thread.currentThread().interrupt();}
            return result;
        } catch(InterruptedException failure) {Thread.currentThread().interrupt();return source;}
        catch(Exception failure) {return source;}
    }

    static Rect narrow(List<PaddleLine> found,Rect strip,float x,double median){
        int left=strip.right,right=strip.left,count=0;
        for(var line:found){OcrWord w=line.word;if(!quantity(w) || !inside(w,strip,x) || w.box.height()<median*.35)continue;
            left=Math.min(left,w.box.left);right=Math.max(right,w.box.right);count++;}
        int margin=Math.max(3,(int)Math.ceil(median*.15));
        if(count<4 || right-left+2*margin>=strip.width()*.75)return null;
        return new Rect(Math.max(strip.left,left-margin),strip.top,Math.min(strip.right,right+margin),strip.bottom);
    }

    static List<PaddleLine> fill(List<PaddleLine> source,List<PaddleLine> found,Rect strip,float x,double median){
        List<PaddleLine> missing=new ArrayList<>();
        for(var line:found)if(source.stream().noneMatch(old->quantity(old.word) && inside(old.word,strip,x)
            && line.word.box.bottom>=old.word.box.top-median*.15
            && line.word.box.top<=old.word.box.bottom+median*.15))missing.add(line);
        return merge(source,missing,strip,x,median);
    }

    private static Area find(Bitmap page,List<PaddleLine> source) {
        List<OcrWord> labels=new ArrayList<>();
        for(var line:source){ColumnRows row=new ColumnRows();row.words.add(line.word);labels.addAll(ColumnRows.headers(row));}
        for(OcrWord p:labels)if(p.text.equals("단가"))for(OcrWord q:labels)if(q.text.startsWith("수")) {
            if(q.x()<=p.x() || Math.abs(q.y()-p.y())>Math.max(p.box.height(),q.box.height())*2)continue;
            OcrWord a=null;
            for(OcrWord label:labels)if(label.text.equals("금액") && label.x()>q.x()
                && Math.abs(label.y()-q.y())<=Math.max(q.box.height(),label.box.height())*2
                && (a==null || Math.abs(label.y()-q.y())<Math.abs(a.y()-q.y())))a=label;
            if(a==null)continue;
            if(Math.max(p.y(),Math.max(q.y(),a.y()))-Math.min(p.y(),Math.min(q.y(),a.y()))
                >2*Math.max(p.box.height(),Math.max(q.box.height(),a.box.height())))continue;
            int top=q.box.bottom,end=page.getHeight();
            for(var line:source)if(line.word.box.top>top && ColumnRows.footer(line.word.text))end=Math.min(end,line.word.box.top);
            if(end==page.getHeight())continue;
            Rect b=new Rect(Math.max(0,Math.round((p.x()+q.x())/2)),Math.max(0,top),
                Math.min(page.getWidth(),Math.round((q.x()+a.x())/2)),Math.min(page.getHeight(),end));
            if(b.isEmpty() || b.width()>512 || b.height()>2048 || (long)b.width()*b.height()*16>8_000_000)continue;
            List<Integer> heights=new ArrayList<>();List<OcrWord> amounts=new ArrayList<>();OcrWord price=null;int quantities=0;
            for(var line:source){OcrWord w=line.word;if(w.y()<b.top || w.y()>=b.bottom)continue;
                if(w.x()>=p.x()-(q.x()-p.x())*.75 && w.x()<b.left && w.text.matches("\\d[\\d,]*")){
                    heights.add(w.box.height());price=w;
                }
                if(w.x()>b.right && Math.abs(w.x()-a.x())<(a.x()-q.x())*.75 && w.text.matches("\\d[\\d,]*"))amounts.add(w);
                if(quantity(w) && inside(w,b,q.x()))quantities++;
            }
            boolean single=heights.size()==1 && amounts.size()==1
                && Math.abs(price.y()-amounts.get(0).y())<=Math.max(price.box.height(),amounts.get(0).box.height())*.75;
            if(heights.isEmpty() || heights.size()<4 && !single || quantities>=heights.size()*.8)continue;
            Collections.sort(heights);return new Area(b,q.x(),heights.get(heights.size()/2));
        }
        return null;
    }

    static List<PaddleLine> merge(List<PaddleLine> source,List<PaddleLine> found,Rect strip,float x,double median) {
        List<PaddleLine> result=null;
        for(PaddleLine line:found){OcrWord w=line.word;
            if(!quantity(w) || !inside(w,strip,x) || w.box.height()<median*.35)continue;
            if(result==null)result=new ArrayList<>(source);
            result.removeIf(old -> quantity(old.word) && inside(old.word,strip,x)
                && Math.abs(old.word.x()-w.x())<=Math.max(4,median*.6)
                && w.y()>=old.word.box.top-median*.15 && w.y()<=old.word.box.bottom+median*.15);
            result.add(line);
        }
        return result==null?source:result;
    }

    private static boolean quantity(OcrWord w) {
        String s=w.text.trim();return s.matches("\\d{1,4}") && Integer.parseInt(s)>0;
    }
    private static boolean inside(OcrWord w,Rect b,float x) {
        return w.y()>=b.top && w.y()<b.bottom && w.x()>b.left && w.x()<b.right
            && Math.abs(w.x()-x)<=b.width()*.3;
    }
}
