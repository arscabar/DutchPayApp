package kr.dutchpay;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

final class NameSheet {
    static List<String> read(List<Bitmap> crops) throws Exception {
        if(crops.isEmpty())return Collections.emptyList();int width=1;
        for(Bitmap b:crops)width=Math.max(width,b.getWidth());
        Bitmap sheet=Bitmap.createBitmap(width,crops.size()*100,Bitmap.Config.ARGB_8888);
        TextRecognizer engine=Ocr.create();Task<Text> task=null;
        try {
            Canvas c=new Canvas(sheet);c.drawColor(Color.WHITE);
            for(int i=0;i<crops.size();i++)c.drawBitmap(crops.get(i),0,i*100+10,null);
            task=engine.process(InputImage.fromBitmap(sheet,0));
            Text text=Tasks.await(task,30,TimeUnit.SECONDS);
            List<String> names=new ArrayList<>(Collections.nCopies(crops.size(),""));
            for(var block:text.getTextBlocks())for(var line:block.getLines()){
                Rect b=line.getBoundingBox();if(b==null)continue;int i=(int)b.exactCenterY()/100;
                if(i<0 || i>=crops.size() || b.top<i*100 || b.bottom>(i+1)*100)continue;
                names.set(i,(names.get(i)+" "+line.getText()).trim());
            }
            return names;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
        finally {
            if(task!=null && !task.isComplete())task.addOnCompleteListener(Runnable::run,t->{try{engine.close();}finally{sheet.recycle();}});
            else {try{engine.close();}finally{sheet.recycle();}}
        }
    }
}
