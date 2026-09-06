package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;
import java.io.*;
import org.json.*;

public class MenuViewProbeTest extends InstrumentationTestCase {
    public void testIndependentNameViews() throws Exception {
        var ctx=getInstrumentation().getTargetContext();
        var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        boolean paddleOnly="true".equals(args.getString("paddleOnly"));
        String[] files=args.getString("files","03.jpg").split(",");JSONArray result=new JSONArray();
        try(var engine=new FullPaddle(ctx,true);var ml=Ocr.create()){
            for(String file:files){
                File copy=new File(ctx.getCacheDir(),"menu-probe.jpg");
                try(var in=file.startsWith("Scan_")?new FileInputStream(new File(ctx.getExternalFilesDir(null),"scans/"+file)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+file);var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
                Bitmap page=InputImage.fromFilePath(ctx,Uri.fromFile(copy)).getBitmapInternal();
                try{
                    var lines=PaddleTrim.read(page,PaddleRetry.read(page,engine.read(page,1600),engine.korean),engine.korean).lines;
                    lines=QuantityRecovery.read(page,lines);var receipt=PaddleReceipt.receipt(lines);
                    try(var patches=NamePatches.create(page,lines,receipt)){
                        JSONArray names=new JSONArray();
                        for(int n=0;n<patches.items.size();n++){
                            JSONArray views=new JSONArray();Bitmap source=patches.padded.get(n);
                            for(float scale:paddleOnly?new float[]{}:new float[]{.7f,1f,1.5f}){
                                Bitmap view=Bitmap.createScaledBitmap(source,Math.max(1,Math.round(source.getWidth()*scale)),Math.max(1,Math.round(source.getHeight()*scale)),true);
                                try{
                                    var text=Tasks.await(ml.process(InputImage.fromBitmap(view,0)),30,TimeUnit.SECONDS);
                                    views.put(new JSONObject().put("scale",scale).put("text",text.getText()));
                                }finally{if(view!=source)view.recycle();}
                            }
                            JSONArray paddles=new JSONArray();
                            if(paddleOnly){
                                var readings=MenuViews.read(patches.originals.get(n),engine.korean);
                                readings.add(0,engine.korean.readWithConfidence(patches.originals.get(n)));
                                readings.add(engine.korean.readWithConfidence(source));
                                for(var r:readings)paddles.put(new JSONObject().put("text",r.text).put("score",r.confidence));
                            }
                            names.put(new JSONObject().put("old",patches.items.get(n).name).put("views",views)
                                .put("paddle",paddles).put("originalScore",Double.isFinite(patches.scores.get(n))?patches.scores.get(n):JSONObject.NULL));
                        }
                        result.put(new JSONObject().put("file",file).put("names",names));
                    }
                }finally{page.recycle();copy.delete();}
                try(var out=new FileWriter(new File(ctx.getExternalFilesDir(null),paddleOnly?"menu-paddle-views.json":"menu-view-probe.json"))){out.write(result.toString(2));}
            }
        }
        assertEquals(files.length,result.length());
    }
}
