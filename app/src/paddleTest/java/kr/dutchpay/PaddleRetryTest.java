package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;
import java.util.*;

public class PaddleRetryTest extends InstrumentationTestCase {
    public void testSavedRegions() throws Exception {
        var target=getInstrumentation().getTargetContext();File folder=target.getExternalFilesDir(null);
        JSONArray source;
        try(var in=new FileInputStream(new File(folder,"document-scans-paddle-v6.json"))){
            source=new JSONArray(new String(KoreanModel.bytes(in),java.nio.charset.StandardCharsets.UTF_8));}
        assertEquals(63,source.length());JSONArray results=new JSONArray();
        try(KoreanModel model=new KoreanModel(getInstrumentation().getContext())){
            for(int n=0;n<source.length();n++){
                JSONObject saved=source.getJSONObject(n);String file=saved.getString("file");
                List<PaddleLine> lines=new ArrayList<>();JSONArray raw=saved.getJSONArray("lines");
                for(int j=0;j<raw.length();j++){
                    JSONObject x=raw.getJSONObject(j);JSONArray q=x.getJSONArray("quad");List<PointF> points=new ArrayList<>();
                    for(int k=0;k<4;k++)points.add(new PointF((float)q.getJSONArray(k).getDouble(0),(float)q.getJSONArray(k).getDouble(1)));
                    lines.add(new PaddleLine(x.getString("text"),points));
                }
                Bitmap page=InputImage.fromFilePath(target,Uri.fromFile(new File(folder,"scans/"+file))).getBitmapInternal();
                try {
                    List<PaddleLine> updated=PaddleRetry.read(page,lines,model);
                    PaddleTrim.Result trimmed=PaddleTrim.read(page,updated,model);
                    results.put(PaddleReceipt.parse(trimmed.lines).put("file",file).put("originalLines",raw)
                        .put("trimDiagnostics",new JSONArray(trimmed.diagnostics)));
                } finally {page.recycle();}
            }
        }
        try(var out=new OutputStreamWriter(new FileOutputStream(new File(folder,"document-scans-paddle-v6-retry.json")),"UTF-8")){
            out.write(results.toString(2));}
        assertEquals(63,results.length());
    }
}
