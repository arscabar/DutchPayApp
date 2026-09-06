package kr.dutchpay;

import android.graphics.PointF;
import android.test.InstrumentationTestCase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReplayPaddleTest extends InstrumentationTestCase {
    public void testReplay() throws Exception {
        File folder=getInstrumentation().getTargetContext().getExternalFilesDir(null);
        assertNotNull(folder);
        var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        boolean retry="true".equals(args.getString("retry"));
        File input=new File(folder,retry?"document-scans-paddle-v6-retry.json":"document-scans-paddle-v6.json");
        File output=new File(folder,"true".equals(args.getString("structure"))?"structure-replay63.json":
            retry?"steps-retry.json":"steps-layout.json");
        assertFalse(input.getCanonicalFile().equals(output.getCanonicalFile()));
        JSONArray source;
        try(var in=new FileInputStream(input)) {
            source=new JSONArray(new String(KoreanModel.bytes(in),StandardCharsets.UTF_8));
        }
        assertEquals(63,source.length());
        JSONArray results=new JSONArray(); Set<String> files=new HashSet<>();
        for(int i=0;i<source.length();i++) {
            JSONObject receipt=source.getJSONObject(i);
            String file=receipt.getString("file"); assertTrue(files.add(file));
            JSONArray raw=receipt.getJSONArray("lines"); List<PaddleLine> lines=new ArrayList<>();
            for(int j=0;j<raw.length();j++) {
                JSONObject line=raw.getJSONObject(j); JSONArray quad=line.getJSONArray("quad");
                assertEquals(4,quad.length()); List<PointF> points=new ArrayList<>();
                for(int k=0;k<quad.length();k++) {
                    JSONArray point=quad.getJSONArray(k); assertEquals(2,point.length());
                    float x=(float)point.getDouble(0), y=(float)point.getDouble(1);
                    assertTrue(Float.isFinite(x) && Float.isFinite(y));
                    points.add(new PointF(x,y));
                }
                lines.add(new PaddleLine(line.getString("text"),points));
            }
            results.put(PaddleReceipt.parse(lines).put("file",file));
        }
        assertEquals(63,results.length());
        try(var out=new OutputStreamWriter(new FileOutputStream(output),StandardCharsets.UTF_8)) {
            out.write(results.toString(2));
        }
    }
}
