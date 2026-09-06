package kr.dutchpay;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.test.InstrumentationTestCase;
import org.json.*;
import java.io.File;
import java.nio.file.Files;

/** Uses cached automatic crop PNGs; no labels or repeat receipt detection. */
public class RegionRetryViewProbeTest extends InstrumentationTestCase {
    public void testCenterCtcOnExistingPixels() throws Exception {
        var context=getInstrumentation().getTargetContext();
        File dir=new File(context.getExternalFilesDir(null),"region-probe");
        JSONArray source=new JSONArray(new String(Files.readAllBytes(new File(dir,"region-probe.json").toPath()),"UTF-8"));
        JSONArray output=new JSONArray();
        try(var model=new KoreanModel(context)){
            for(int r=0;r<source.length();r++){
                JSONObject receipt=source.getJSONObject(r);JSONArray names=receipt.getJSONArray("names");
                for(int i=0;i<names.length();i++){
                    JSONObject name=names.getJSONObject(i);String png=name.getString("png");
                    Bitmap bitmap=BitmapFactory.decodeFile(new File(dir,png).getAbsolutePath());assertNotNull(bitmap);
                    int margin=Math.max(1,Math.round(bitmap.getHeight()*.1f));
                    Bitmap center=Bitmap.createBitmap(bitmap,0,margin,bitmap.getWidth(),bitmap.getHeight()-margin*2);
                    try{
                        long start=System.currentTimeMillis();float[][] values=model.infer(center);
                        output.put(new JSONObject().put("file",receipt.getString("file")).put("png",png)
                            .put("old",name.getString("old")).put("tokens",RegionRetryTrace.tokens(values,model.chars))
                            .put("milliseconds",System.currentTimeMillis()-start));
                    }finally{if(center!=bitmap)center.recycle();bitmap.recycle();}
                }
                Files.write(new File(dir,"region-center.json").toPath(),output.toString(2).getBytes("UTF-8"));
            }
        }
        assertTrue(output.length()>0);
    }
}
