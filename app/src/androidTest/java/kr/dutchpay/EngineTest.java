package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class EngineTest extends InstrumentationTestCase {
    public void testKoreanRecognition() throws Exception {
        var test=getInstrumentation().getContext();
        var target=getInstrumentation().getTargetContext();
        JSONArray results=new JSONArray();
        boolean padded="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("padded"));
        try(var paddle=new KoreanModel(test); var ml=Ocr.create()) {
            for(String file:test.getAssets().list("receipts")) {
                long start=System.currentTimeMillis();
                File copy=new File(target.getCacheDir(),file);
                try(var in=test.getAssets().open("receipts/"+file);var out=new FileOutputStream(copy)) {
                    out.write(KoreanModel.bytes(in));
                }
                var image=InputImage.fromFilePath(target,Uri.fromFile(copy));
                var first=Tasks.await(ml.process(image),120,TimeUnit.SECONDS);
                Bitmap page=ReceiptCrop.create(image.getBitmapInternal(),first);
                image.getBitmapInternal().recycle(); copy.delete();
                JSONArray lines=new JSONArray();
                if(page!=null) {
                    var text=Tasks.await(ml.process(InputImage.fromBitmap(page,0)),120,TimeUnit.SECONDS);
                    lines=EngineLines.read(page,text,ml,paddle,padded);
                    page.recycle();
                }
                results.put(new JSONObject().put("file",file).put("lines",lines)
                    .put("milliseconds",System.currentTimeMillis()-start));
                try(var out=new FileWriter(new File(target.getExternalFilesDir(null),padded?"padded.json":"engines.json"))) {
                    out.write(results.toString(2));
                }
            }
        }
        assertEquals(11,results.length());
        assertTrue(results.getJSONObject(0).getJSONArray("lines").length()>0);
    }
}
