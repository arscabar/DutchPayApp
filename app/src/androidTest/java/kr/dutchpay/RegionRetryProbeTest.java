package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;
import java.io.*;
import org.json.*;

public class RegionRetryProbeTest extends InstrumentationTestCase {
    public void testActualNamePixelsAndCtc() throws Exception {
        var ctx=getInstrumentation().getTargetContext();
        var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        String[] files=args.getString("files","03.jpg").split(",");JSONArray result=new JSONArray();
        File dir=new File(ctx.getExternalFilesDir(null),"region-probe");dir.mkdirs();
        try(var engine=new FullPaddle(ctx,true);var ml=Ocr.create()){
            for(String file:files){
                File copy=new File(ctx.getCacheDir(),"region-probe.jpg");
                try(var in=file.startsWith("Scan_")?new FileInputStream(new File(ctx.getExternalFilesDir(null),"scans/"+file)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+file);var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
                Bitmap page=InputImage.fromFilePath(ctx,Uri.fromFile(copy)).getBitmapInternal();
                try(var direction=PageOrientation.read(page,engine.read(page,1600),engine)){
                    var lines=PaddleTrim.read(direction.page,PaddleRetry.read(direction.page,direction.lines,engine.korean),engine.korean).lines;
                    lines=QuantityRecovery.read(direction.page,lines);
                    lines=NumericRecovery.read(direction.page,lines,engine.korean,new JSONArray());
                    var receipt=PaddleReceipt.receipt(lines);JSONArray names=new JSONArray();
                    try(var patches=NamePatches.create(direction.page,lines,receipt)){
                        for(int n=0;n<patches.items.size();n++){
                            Bitmap crop=patches.originals.get(n);String png=file+"-"+n+".png";
                            try(var out=new FileOutputStream(new File(dir,png))){crop.compress(Bitmap.CompressFormat.PNG,100,out);}
                            float[][] values=engine.korean.infer(crop);
                            var other=Tasks.await(ml.process(InputImage.fromBitmap(patches.padded.get(n),0)),30,TimeUnit.SECONDS);
                            names.put(new JSONObject().put("old",patches.items.get(n).name).put("png",png)
                                .put("width",crop.getWidth()).put("height",crop.getHeight()).put("steps",values.length)
                                .put("tokens",RegionRetryTrace.tokens(values,engine.korean.chars)).put("ml",other.getText()));
                        }
                    }
                    result.put(new JSONObject().put("file",file).put("names",names));
                }finally{page.recycle();copy.delete();}
                try(var out=new FileWriter(new File(dir,"region-probe.json"))){out.write(result.toString(2));}
            }
        }
        assertEquals(files.length,result.length());
    }
}
