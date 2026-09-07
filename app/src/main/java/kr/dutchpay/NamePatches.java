package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class NamePatches implements AutoCloseable {
    final List<Item> items=new ArrayList<>();
    final List<Double> scores=new ArrayList<>();
    final List<Bitmap> originals=new ArrayList<>(),padded=new ArrayList<>();
    static NamePatches create(Bitmap page,List<PaddleLine> lines,Receipt receipt){
        NamePatches out=new NamePatches();var groups=PaddleLine.groups(lines);Set<Integer> used=new HashSet<>();
        try {
            for(Item item:receipt.items){
                if(item.baseAmount()<0 || item.includedDiscount)continue;
                String name=NameDecision.key(item.name);List<PaddleLine> match=null;int found=-1;
                for(int j=0;j<groups.size();j++){
                    List<PaddleLine> parts=new ArrayList<>();StringBuilder joined=new StringBuilder();
                    for(var line:groups.get(j)){
                        String s=NameDecision.key(line.word.text);
                        if(s.isEmpty() || !s.matches(".*[가-힣A-Za-z].*") || !name.contains(s))continue;
                        joined.append(s);parts.add(line);
                    }
                    if(parts.isEmpty() || !name.contentEquals(joined))continue;
                    // Repeated names can include cancellations or options under different parents.
                    if(match!=null){match=null;break;}
                    match=parts;found=j;
                }
                if(match==null || used.contains(found))continue;
                Bitmap crop=crop(page,match);if(crop==null)continue;

                // Extract high-quality receipt crop for item view
                Bitmap rowCrop = cropRow(page, groups.get(found));
                if (rowCrop != null) {
                    item.crop = rowCrop;
                } else {
                    item.crop = crop.copy(Bitmap.Config.ARGB_8888, false);
                }

                used.add(found);
                // ponytail: cap costly OCR review at 24 name regions; larger receipts retain originals.
                if(out.items.size()<24){
                    out.originals.add(crop);out.padded.add(pad(crop));out.items.add(item);
                    out.scores.add(match.stream().mapToDouble(l->l.confidence).max().orElse(Double.NaN));
                } else {
                    crop.recycle();
                }
            }

            // Fallback pass: find crops for remaining items that have no crop yet
            for(Item item:receipt.items){
                if(item.crop!=null || item.name==null || item.name.trim().isEmpty())continue;
                String key=NameDecision.key(item.name);
                for(int j=0;j<groups.size();j++){
                    if(used.contains(j))continue;
                    StringBuilder sb=new StringBuilder();
                    for(var l:groups.get(j))sb.append(NameDecision.key(l.word.text));
                    String rowText=sb.toString();
                    if((!key.isEmpty() && (rowText.contains(key) || key.contains(rowText)))
                        || (item.printedTotal>0 && rowText.contains(String.valueOf(item.printedTotal)))) {
                        Bitmap rc=cropRow(page,groups.get(j));
                        if(rc!=null){item.crop=rc;used.add(j);break;}
                    }
                }
            }
            return out;
        }catch(RuntimeException e){out.close();throw e;}
    }
    static Bitmap cropRow(Bitmap page,List<PaddleLine> parts){
        if(page==null || parts==null || parts.isEmpty())return null;
        List<PointF> firstCorners=parts.get(0).points;
        var a=firstCorners.get(0);var b=firstCorners.get(1);
        float slope=Math.abs(b.x-a.x)>1?(b.y-a.y)/(b.x-a.x):0;
        RectF r=new RectF(Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);
        for(var line:parts){
            for(var p:line.points){
                float y=p.y-slope*p.x;
                r.left=Math.min(r.left,p.x);r.right=Math.max(r.right,p.x);
                r.top=Math.min(r.top,y);r.bottom=Math.max(r.bottom,y);
            }
        }
        float padY=Math.max(4f,(r.bottom-r.top)*0.15f);
        float padX=Math.max(8f,padY);
        r.left=Math.max(0,r.left-padX);
        r.right=Math.min(page.getWidth(),r.right+padX);
        r.top=Math.max(0,r.top-padY);
        r.bottom=Math.min(page.getHeight(),r.bottom+padY);

        List<PointF> corners=Arrays.asList(
            new PointF(r.left,r.top+slope*r.left),
            new PointF(r.right,r.top+slope*r.right),
            new PointF(r.right,r.bottom+slope*r.right),
            new PointF(r.left,r.bottom+slope*r.left)
        );
        Point[] p=new Point[4];
        for(int k=0;k<4;k++)p[k]=new Point(Math.round(corners.get(k).x),Math.round(corners.get(k).y));
        try(var patch=TextPatch.create(page,p,false)){
            return patch==null?null:patch.image.copy(Bitmap.Config.ARGB_8888,false);
        }catch(Exception e){return null;}
    }
    static Bitmap crop(Bitmap page,List<PaddleLine> parts){
        List<PointF> corners=parts.get(0).points;
        if(parts.size()>1){
            var a=corners.get(0);var b=corners.get(1);float slope=(b.y-a.y)/Math.max(1,b.x-a.x);
            RectF r=new RectF(Float.MAX_VALUE,Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE);
            for(var line:parts)for(var p:line.points){float y=p.y-slope*p.x;
                r.left=Math.min(r.left,p.x);r.right=Math.max(r.right,p.x);r.top=Math.min(r.top,y);r.bottom=Math.max(r.bottom,y);}
            corners=Arrays.asList(new PointF(r.left,r.top+slope*r.left),new PointF(r.right,r.top+slope*r.right),
                new PointF(r.right,r.bottom+slope*r.right),new PointF(r.left,r.bottom+slope*r.left));
        }
        Point[] p=new Point[4];for(int k=0;k<4;k++)p[k]=new Point(Math.round(corners.get(k).x),Math.round(corners.get(k).y));
        try(var patch=TextPatch.create(page,p)){return patch==null?null:patch.image.copy(Bitmap.Config.ARGB_8888,false);}
    }
    static Bitmap pad(Bitmap source){
        Rect r=InkBounds.trim(source);int h=64,w=Math.max(1,Math.round((float)r.width()*h/r.height()));
        if(w>1200){h=Math.max(8,Math.round(h*1200f/w));w=1200;}
        Bitmap b=Bitmap.createBitmap(w+16,h+16,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        c.drawBitmap(source,r,new Rect(8,8,w+8,h+8),new Paint(Paint.FILTER_BITMAP_FLAG));return b;
    }
    public void close(){for(Bitmap b:originals)b.recycle();for(Bitmap b:padded)b.recycle();}
}
