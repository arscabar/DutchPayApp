package kr.dutchpay;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class NumericStrip {
    static List<PaddleLine> read(Bitmap page,Rect b) throws Exception {
        if(b.isEmpty() || b.left<0 || b.top<0 || b.right>page.getWidth() || b.bottom>page.getHeight()
            || 16L*b.width()*b.height()>8000000)return Collections.emptyList();
        Bitmap crop=null,large=null;TextRecognizer engine=null;Task<Text> pending=null;
        try{
            crop=Bitmap.createBitmap(page,b.left,b.top,b.width(),b.height());
            large=Bitmap.createScaledBitmap(crop,b.width()*4,b.height()*4,true);
            engine=Ocr.create();pending=engine.process(InputImage.fromBitmap(large,0));
            Text text=Tasks.await(pending,30,TimeUnit.SECONDS);List<PaddleLine> found=new ArrayList<>();
            for(var block:text.getTextBlocks())for(var line:block.getLines())for(var element:line.getElements()){
                Rect r=element.getBoundingBox();if(r==null)continue;List<PointF> points=new ArrayList<>();
                for(int[] p:new int[][]{{r.left,r.top},{r.right,r.top},{r.right,r.bottom},{r.left,r.bottom}})
                    points.add(new PointF(b.left+p[0]/4f,b.top+p[1]/4f));
                found.add(new PaddleLine(element.getText(),points));
            }
            return found;
        }finally{
            if(pending!=null && !pending.isComplete()){
                Bitmap retained=large;TextRecognizer retainedEngine=engine;
                pending.addOnCompleteListener(Runnable::run,t->{try{retainedEngine.close();}finally{retained.recycle();}});
                large=null;engine=null;
            }
            try{if(engine!=null)engine.close();}catch(RuntimeException ignored){}
            finally{if(large!=null && large!=crop)large.recycle();if(crop!=null && crop!=page)crop.recycle();}
        }
    }
}
