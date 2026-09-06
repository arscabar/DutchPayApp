package kr.dutchpay;

import android.content.Context;
import android.graphics.*;
import com.paddle.ocr.*;
import com.paddle.ocr.engine.*;
import com.paddle.ocr.postprocess.QuadTextCrop;
import com.paddle.ocr.util.*;
import org.json.*;
import java.util.*;

final class FullPaddle implements AutoCloseable {
    final ORTSessionManager manager;
    final KoreanModel korean;
    final boolean v6;
    long detectionMs, recognitionMs;
    FullPaddle(Context context,boolean v6) throws Exception {
        this.v6=v6;
        if(!OpenCVUtils.INSTANCE.init(context))throw new IllegalStateException("OpenCV initialization failed");
        manager=new ORTSessionManager(context,new EngineConfig(2));
        try {
            manager.loadModels(v6?"models/detector-v6.onnx":"models/detector.onnx","models/korean.onnx");
            korean=new KoreanModel(context);
        } catch(Exception | LinkageError failure) {manager.release();throw failure;}
    }
    List<PaddleLine> read(Bitmap page,int limit) throws Exception {
        var config=new PaddleOCRConfig("BGR",limit,"max",4000,v6?.2f:.3f,v6?.45f:.6f,v6?1.4f:1.5f,3000,false,"fast","quad",0,1);
        var source=BitmapUtils.INSTANCE.bitmapToBGRMat(page);
        try {
            var detected=new DetectionEngine(manager,config).detect(source);
            detectionMs=detected.getTimeMs(); long start=System.currentTimeMillis();
            List<PaddleLine> lines=new ArrayList<>();
            for(var box:detected.getBoxes()) {
                var crop=QuadTextCrop.INSTANCE.crop(source,box);
                Bitmap bitmap=null;
                try {
                    bitmap=BitmapUtils.INSTANCE.bgrMatToBitmap(crop);
                    var reading=korean.readWithConfidence(bitmap);
                    lines.add(new PaddleLine(reading.text,box.getPoints(),reading.confidence));
                } finally {if(bitmap!=null)bitmap.recycle();crop.release();}
            }
            recognitionMs=System.currentTimeMillis()-start; return lines;
        } finally {source.release();}
    }
    public void close() throws Exception { try{korean.close();}finally{manager.release();} }
}
