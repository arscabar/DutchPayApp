package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

public class NameProbeTest extends InstrumentationTestCase {
    public void testVariants() throws Exception {
        var ctx=getInstrumentation().getTargetContext();var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        String file=args.getString("file","03.jpg");File copy=new File(ctx.getCacheDir(),"name-probe.jpg");
        try(var in=file.startsWith("Scan_")?new FileInputStream(new File(ctx.getExternalFilesDir(null),"scans/"+file)):
            getInstrumentation().getContext().getAssets().open("receipts/"+file);var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
        Bitmap page=InputImage.fromFilePath(ctx,Uri.fromFile(copy)).getBitmapInternal();JSONArray results=new JSONArray();
        try(var engine=new FullPaddle(ctx,true);var ml=Ocr.create()) {
            var lines=PaddleRetry.read(page,engine.read(page,1600),engine.korean);
            var receipt=PaddleReceipt.receipt(QuantityRecovery.read(page,lines));
            List<String> names=new ArrayList<>();for(Item i:receipt.items)names.add(PaddleTrim.compact(i.name));
            for(var line:lines){String old=line.word.text;
                if(!old.matches(".*[가-힣].*") || names.stream().noneMatch(n->n.contains(PaddleTrim.compact(old))))continue;
                Point[] points=new Point[4];for(int k=0;k<4;k++){var p=line.points.get(k);points[k]=new Point(Math.round(p.x),Math.round(p.y));}
                try(var patch=TextPatch.create(page,points)){
                    if(patch==null)continue;JSONObject row=line.json();JSONArray variants=new JSONArray();
                    for(String mode:new String[]{"base","trim","pad","contrast"}){
                        Bitmap b=NameProbeImages.make(patch.image,mode);
                        try{var reading=engine.korean.readWithConfidence(b);variants.put(new JSONObject().put("mode",mode)
                            .put("text",reading.text).put("score",reading.confidence));
                            if(mode.equals("pad")){var text=Tasks.await(ml.process(InputImage.fromBitmap(b,0)),30,TimeUnit.SECONDS);
                                row.put("ml",text.getText());}
                        }finally{if(b!=patch.image)b.recycle();}
                    }
                    results.put(row.put("variants",variants));
                }
            }
            try(var out=new FileWriter(new File(ctx.getExternalFilesDir(null),"name-probe.json"))){out.write(new JSONObject().put("file",file).put("rows",results).toString(2));}
            assertTrue(results.length()>0);
        }finally{page.recycle();copy.delete();}
    }
}
