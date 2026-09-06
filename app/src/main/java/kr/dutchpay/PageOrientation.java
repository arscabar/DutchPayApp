package kr.dutchpay;

import android.graphics.*;
import java.util.*;

final class PageOrientation implements AutoCloseable {
    final Bitmap page;final List<PaddleLine> lines;final int degrees;
    private PageOrientation(Bitmap page,List<PaddleLine> lines,int degrees){this.page=page;this.lines=lines;this.degrees=degrees;}
    static PageOrientation read(Bitmap source,List<PaddleLine> original,FullPaddle engine){
        PageOrientation best=new PageOrientation(source,original,0);
        if(!vertical(original))return best;int score=score(original);
        for(int degrees:new int[]{90,270}){
            Bitmap rotated=null;
            try{
                Matrix rotation=new Matrix();rotation.postRotate(degrees);
                rotated=Bitmap.createBitmap(source,0,0,source.getWidth(),source.getHeight(),rotation,true);
                var lines=engine.read(rotated,1600);int candidate=score(lines);
                if(!vertical(lines) && candidate>=200 && candidate>score){
                    best.close();best=new PageOrientation(rotated,lines,degrees);rotated=null;score=candidate;
                }
            }catch(Exception failure){/* Preserve the previous measured OCR result. */}
            finally{if(rotated!=null && rotated!=source)rotated.recycle();}
        }
        return best;
    }
    static boolean vertical(List<PaddleLine> lines){
        int tall=0,wide=0;
        for(var line:lines)if(line.word.text.replaceAll("\\s","").length()>=3){
            Rect b=line.word.box;if(b.height()>b.width()*1.8)tall++;
            if(b.width()>b.height()*1.8)wide++;
        }
        return tall>=6 && tall>wide*2;
    }
    static int score(List<PaddleLine> lines){
        int words=0,letters=0;
        for(var line:lines){Rect b=line.word.box;if(b.width()<b.height()*1.3)continue;
            String s=ColumnRows.compact(line.word.text);
            if(s.matches(".*(영수증|합계|결제|금액|단가|수량|카드|상호|부가세).*"))words++;
            letters+=s.replaceAll("[^가-힣]","").length();
        }
        return words*100+Math.min(99,letters);
    }
    public void close(){if(degrees!=0)page.recycle();}
}
