package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.*;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class VariantsTest extends InstrumentationTestCase {
    public void testScales() throws Exception {
        android.content.Context c=getInstrumentation().getTargetContext();
        com.google.mlkit.vision.text.TextRecognizer engine=Ocr.create();
        JSONArray results=new JSONArray();
        for(String name:new String[]{"01.jpg","05.jpg","06.jpg"}) {
            File file=new File(c.getCacheDir(),name);
            try(InputStream in=getInstrumentation().getContext().getAssets().open("receipts/"+name);
                OutputStream out=new FileOutputStream(file)) {
                byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n);
            }
            InputImage original=InputImage.fromFilePath(c,Uri.fromFile(file));
            com.google.mlkit.vision.text.Text first=Tasks.await(engine.process(original),90,TimeUnit.SECONDS);
            Bitmap crop=ReceiptCrop.create(original.getBitmapInternal(),first);
            for(int width:new int[]{800,1100,2000}) {
                Bitmap scaled=Bitmap.createScaledBitmap(crop,width,Math.round(crop.getHeight()*width/1600f),true);
                com.google.mlkit.vision.text.Text text=Tasks.await(engine.process(InputImage.fromBitmap(scaled,0)),90,TimeUnit.SECONDS);
                results.put(new JSONObject().put("file",name).put("width",width).put("rows",String.join("\n",Layout.rows(text))));
                scaled.recycle();
                try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(c.getExternalFilesDir(null),"variants.json")),"UTF-8")){
                    out.write(results.toString(2));
                }
            }
            crop.recycle(); original.getBitmapInternal().recycle(); file.delete();
        }
        engine.close();
    }
}
