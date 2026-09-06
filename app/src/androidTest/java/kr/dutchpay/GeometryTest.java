package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import org.json.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class GeometryTest extends InstrumentationTestCase {
    public void testGeometry() throws Exception {
        android.content.Context c=getInstrumentation().getTargetContext();
        File file=new File(c.getCacheDir(),"geometry.jpg");
        try(InputStream in=getInstrumentation().getContext().getAssets().open("receipts/03.jpg");
            OutputStream out=new FileOutputStream(file)) {
            byte[] b=new byte[8192]; int n; while((n=in.read(b))!=-1)out.write(b,0,n);
        }
        com.google.mlkit.vision.text.TextRecognizer engine=Ocr.create();
        Tasks.await(Scan.read(c,Uri.fromFile(file),engine,text -> {
            JSONArray arr=new JSONArray();
            try {
                for(com.google.mlkit.vision.text.Text.TextBlock b:text.getTextBlocks())
                    for(com.google.mlkit.vision.text.Text.Line l:b.getLines())
                        for(com.google.mlkit.vision.text.Text.Element e:l.getElements()) {
                            android.graphics.Rect r=e.getBoundingBox();
                            arr.put(new JSONObject().put("text",e.getText()).put("x",r.centerX())
                                .put("y",r.centerY()).put("w",r.width()).put("h",r.height()));
                        }
                try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(c.getExternalFilesDir(null),"geometry.json")),"UTF-8")){
                    out.write(arr.toString(2));
                }
            }catch(Exception e){throw new RuntimeException(e);}
        }),120,TimeUnit.SECONDS); engine.close(); file.delete();
    }
}
