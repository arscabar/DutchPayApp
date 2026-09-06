package kr.dutchpay;

import android.graphics.Bitmap;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import java.util.concurrent.TimeUnit;

final class NameSingle implements AutoCloseable {
    private TextRecognizer engine;
    private Task<Text> pending;
    String read(Bitmap source) throws Exception {
        if(engine==null)engine=Ocr.create();
        Bitmap copy=source.copy(Bitmap.Config.ARGB_8888,false);
        try{
            pending=engine.process(InputImage.fromBitmap(copy,0));
            return Tasks.await(pending,30,TimeUnit.SECONDS).getText();
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
        finally{
            if(pending!=null && !pending.isComplete())pending.addOnCompleteListener(Runnable::run,t->copy.recycle());
            else copy.recycle();
        }
    }
    public void close(){
        if(engine==null)return;
        if(pending!=null && !pending.isComplete())pending.addOnCompleteListener(Runnable::run,t->engine.close());
        else engine.close();
    }
}
