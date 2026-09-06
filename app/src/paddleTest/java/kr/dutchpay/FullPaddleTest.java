package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;

public class FullPaddleTest extends InstrumentationTestCase {
    public void testReceipts() throws Exception {
        var test=getInstrumentation().getContext();var target=getInstrumentation().getTargetContext();
        var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        int limit=Integer.parseInt(args.getString("limit","960"));
        assertTrue(limit==960 || limit==1600 || limit==2400);
        boolean scans="true".equals(args.getString("scans"));
        boolean v6="true".equals(args.getString("v6"));
        File folder=new File(target.getExternalFilesDir(null),"scans");
        String[] files=scans?folder.list((d,n)->n.endsWith(".jpg")):test.getAssets().list("receipts");
        assertNotNull(files);java.util.Arrays.sort(files);
        JSONArray results=new JSONArray();long loading=System.currentTimeMillis();
        try(var engine=new FullPaddle(test,v6)) {
            loading=System.currentTimeMillis()-loading;
            for(String file:files) {
                File copy=new File(target.getCacheDir(),file);
                try(var in=scans?new FileInputStream(new File(folder,file)):test.getAssets().open("receipts/"+file);
                    var out=new FileOutputStream(copy)) {
                    out.write(KoreanModel.bytes(in));
                }
                long start=System.currentTimeMillis();
                var image=InputImage.fromFilePath(target,Uri.fromFile(copy));
                var page=image.getBitmapInternal();
                try {
                    var lines=engine.read(page,limit);
                    JSONObject r=PaddleReceipt.parse(lines).put("file",file)
                        .put("milliseconds",System.currentTimeMillis()-start).put("detectionMs",engine.detectionMs)
                        .put("recognitionMs",engine.recognitionMs).put("coldLoadMs",loading).put("limit",limit);
                    results.put(r);
                    String output=scans?"document-scans-paddle"+(limit==1600?"":"-"+limit):"full-"+limit;
                    output+=(v6?"-v6":"")+".json";
                    try(var out=new FileWriter(new File(target.getExternalFilesDir(null),output))) {
                        out.write(results.toString(2));
                    }
                } finally {page.recycle();copy.delete();}
            }
        }
        assertEquals(scans?63:11,results.length());
        assertTrue(results.getJSONObject(0).getJSONArray("lines").length()>0);
    }
    public void testRowOrder() {
        var a=new PaddleLine("상품",java.util.Arrays.asList(new android.graphics.PointF(0,0),
            new android.graphics.PointF(100,0),new android.graphics.PointF(100,20),new android.graphics.PointF(0,20)));
        var b=new PaddleLine("9000",java.util.Arrays.asList(new android.graphics.PointF(150,0),
            new android.graphics.PointF(210,0),new android.graphics.PointF(210,20),new android.graphics.PointF(150,20)));
        assertEquals("상품\t9000",PaddleLine.rows(java.util.Arrays.asList(b,a)).get(0));
    }
}
