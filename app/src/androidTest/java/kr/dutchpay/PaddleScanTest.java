package kr.dutchpay;

import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.android.gms.tasks.Tasks;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class PaddleScanTest extends InstrumentationTestCase {
    public void testPackagedPaddlePhoto() throws Exception {
        var target=getInstrumentation().getTargetContext();
        File copy=File.createTempFile("paddle-app-",".jpg",target.getCacheDir());
        try {
            try(var in=getInstrumentation().getContext().getAssets().open("receipts/01.jpg");
                var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
            Receipt r=Tasks.await(PaddleScan.read(target,Uri.fromFile(copy)),90,TimeUnit.SECONDS);
            assertTrue(r.method.startsWith("Paddle 한국어 OCR"));
            assertFalse(r.items.isEmpty());assertEquals(Long.valueOf(58000),r.total);
        } finally {assertTrue(copy.delete());}
    }
    public void testMissingImageFails() throws Exception {
        var task=PaddleScan.read(getInstrumentation().getTargetContext(),null);
        assertTrue(task.isComplete());assertFalse(task.isSuccessful());
    }
}
