package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.text.TextRecognizer;
import org.json.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class BatchTest extends InstrumentationTestCase {
    public void testReceipts() throws Exception {
        Context test = getInstrumentation().getContext();
        Context target = getInstrumentation().getTargetContext();
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(InputStream apk=new FileInputStream(target.getApplicationInfo().sourceDir)){
            byte[] buffer=new byte[65536];int n;
            while((n=apk.read(buffer))!=-1)digest.update(buffer,0,n);
        }
        StringBuilder apkHash=new StringBuilder();
        for(byte b:digest.digest())apkHash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        TextRecognizer engine = Ocr.create();
        JSONArray results = new JSONArray();
        boolean scans = "true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("scans"));
        boolean paddle="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("paddle"));
        boolean structure="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("structure"));
        boolean names="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("names"));
        boolean menus="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("menus"));
        boolean recovery="true".equals(((android.test.InstrumentationTestRunner)getInstrumentation())
            .getArguments().getString("recovery"));
        File folder = new File(target.getExternalFilesDir(null), "scans");
        String[] files = scans ? folder.list((d,n)->n.endsWith(".jpg")) : test.getAssets().list("receipts");
        assertNotNull(files);
        files=java.util.Arrays.stream(files).filter(n->n.endsWith(".jpg")).toArray(String[]::new);
        java.util.Arrays.sort(files);
        for (String file : files) {
            long start = System.currentTimeMillis();
            JSONObject result = new JSONObject().put("file", file).put("apkSHA256",apkHash.toString());
            try (InputStream stream = scans ? new FileInputStream(new File(folder,file))
                    : test.getAssets().open("receipts/" + file)) {
                File copy = new File(target.getCacheDir(), file);
                try (OutputStream out = new FileOutputStream(copy)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = stream.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                Receipt r = Tasks.await(paddle?PaddleScan.read(target,android.net.Uri.fromFile(copy)):
                    Scan.read(target,android.net.Uri.fromFile(copy),engine),
                    120, TimeUnit.SECONDS);
                JSONArray items = new JSONArray();
                for (Item i : r.items) items.put(new JSONObject().put("name", i.name)
                    .put("unit", i.unit).put("count", i.count).put("quantityKnown",i.quantityKnown).put("printedTotal", i.printedTotal)
                    .put("warning", i.warning).put("baseTotal",i.baseAmount())
                    .put("amountBased",i.amountBased).put("includedDiscount",i.includedDiscount)
                    .put("originalName",i.originalName).put("nameCandidates",new JSONArray(i.nameCandidates)));
                result.put("originalRows", r.originalRaw).put("method",r.method).put("rows", r.raw).put("items", items)
                    .put("total", r.total).put("warnings", new JSONArray(r.warnings));
                if(names || menus || recovery)result.put("nameDiagnostics",new JSONArray(r.nameDiagnostics));
                if(menus || recovery)result.put("numericDiagnostics",new JSONArray(r.numericDiagnostics));
                copy.delete();
            } catch (Exception e) { result.put("error", e.toString()); }
            result.put("milliseconds", System.currentTimeMillis()-start);
            results.put(result);
            try (Writer out = new OutputStreamWriter(new FileOutputStream(
                    new File(target.getExternalFilesDir(null),recovery?(scans?"recovery-app63.json":"recovery-app11.json"):
                        menus?(scans?"menus-app63.json":"menus-app11.json"):
                        names?(scans?"names-app63.json":"names-app11.json"):
                        structure?(scans?"structure-app63.json":"structure-app11.json"):
                        paddle?(scans?"steps-app63.json":"steps-app11.json"):
                        scans ? "document-scans-ml.json" : "batch.json")), "UTF-8")) {
                out.write(results.toString(2));
            }
        }
        engine.close();
        assertEquals(scans ? 63 : 11, results.length());
        for (int i = 0; i < results.length(); i++) assertFalse(results.getJSONObject(i).has("error"));
    }
}
