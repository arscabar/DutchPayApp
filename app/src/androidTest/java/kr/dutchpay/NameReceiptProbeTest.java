package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import org.json.*;

public class NameReceiptProbeTest extends InstrumentationTestCase {
    public void testNameRegions() throws Exception {
        var ctx=getInstrumentation().getTargetContext();var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        String file=args.getString("file","03.jpg");File copy=new File(ctx.getCacheDir(),"name-review.jpg");
        try(var in=file.startsWith("Scan_")?new FileInputStream(new File(ctx.getExternalFilesDir(null),"scans/"+file)):
            getInstrumentation().getContext().getAssets().open("receipts/"+file);var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
        Bitmap page=InputImage.fromFilePath(ctx,Uri.fromFile(copy)).getBitmapInternal();long start=System.currentTimeMillis();
        try(var engine=new FullPaddle(ctx,true)){
            var original=engine.read(page,1600);var updated=PaddleRetry.read(page,original,engine.korean);
            var trimmed=PaddleTrim.read(page,updated,engine.korean);var lines=QuantityRecovery.read(page,trimmed.lines);
            var receipt=PaddleReceipt.receipt(lines);long total=receipt.itemSum();
            var diagnostic=NameReview.apply(ctx,page,lines,receipt,engine.korean);JSONArray items=new JSONArray();
            for(Item i:receipt.items)items.put(new JSONObject().put("name",i.name).put("unit",i.unit)
                .put("count",i.count).put("printedTotal",i.printedTotal).put("baseTotal",i.baseAmount())
                .put("originalName",i.originalName).put("nameCandidates",new JSONArray(i.nameCandidates)));
            JSONObject data=new JSONObject().put("file",file).put("items",items).put("total",receipt.total)
                .put("diagnostics",diagnostic).put("milliseconds",System.currentTimeMillis()-start);
            try(var out=new FileWriter(new File(ctx.getExternalFilesDir(null),"name-review-preview.json"))){out.write(data.toString(2));}
            assertEquals(total,receipt.itemSum());assertTrue(diagnostic.length()>0);
        }finally{page.recycle();copy.delete();}
    }
}
