package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import org.json.*;

public class SingleQuantityTest extends InstrumentationTestCase {
    public void testPrintedSingleQuantityAndControls() throws Exception {
        var target=getInstrumentation().getTargetContext();JSONArray results=new JSONArray();
        String[] files={"Scan_20260518_132851.jpg","08.jpg","01.jpg"};
        long[][] units={{10000},{10000},{9000,9000,2000}},amounts={{70000},{120000},{18000,36000,4000}};
        int[][] counts={{7},{12},{2,4,2}};boolean correct=true,controls=true;
        try(FullPaddle engine=new FullPaddle(target,true)){
            for(int index=0;index<files.length;index++){
                String name=files[index];File file=File.createTempFile("single-qty-",".jpg",target.getCacheDir());Bitmap page=null;
                try(InputStream in=name.startsWith("Scan_")?new FileInputStream(new File(target.getExternalFilesDir(null),"scans/"+name)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+name);OutputStream out=new FileOutputStream(file)){
                    out.write(KoreanModel.bytes(in));
                }
                try{
                    page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
                    var source=PaddleTrim.read(page,PaddleRetry.read(page,engine.read(page,1600),engine.korean),engine.korean).lines;
                    var recovered=QuantityRecovery.read(page,source);
                    var after=NumericRecovery.read(page,recovered,engine.korean);Receipt r=PaddleReceipt.receipt(after);
                    results.put(PaddleReceipt.parse(after).put("file",name).put("quantityChanged",recovered!=source));
                    correct&=r.items.size()==counts[index].length;
                    for(int k=0;k<Math.min(r.items.size(),counts[index].length);k++){
                        Item item=r.items.get(k);correct&=item.unit==units[index][k] && item.count==counts[index][k]
                            && item.printedTotal==amounts[index][k] && !item.warning.contains("수량 미인식");
                    }
                    if(index>0)controls&=recovered==source;
                }finally{if(page!=null)page.recycle();file.delete();}
                try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"single-quantity-probe.json"))){out.write(results.toString(2));}
            }
        }
        assertTrue("Printed units, quantities and row amounts",correct);assertTrue("Read quantities preserved",controls);
    }
}
