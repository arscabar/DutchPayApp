package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import org.json.*;

public class NumericQuantityProbeTest extends InstrumentationTestCase {
    public void testObservedQuantityColumns() throws Exception {
        var target=getInstrumentation().getTargetContext();JSONArray results=new JSONArray();
        String[] files={"05.jpg","Scan_20260904_122258.jpg","06.jpg","03.jpg"};
        boolean corrected=true,preserved=true,footerExcluded=true,corroborated=true;
        try(FullPaddle engine=new FullPaddle(target,true)){
            for(int index=0;index<files.length;index++){
                String name=files[index];File file=File.createTempFile("numeric-qty-",".jpg",target.getCacheDir());Bitmap page=null;
                try(InputStream in=name.startsWith("Scan_")?new FileInputStream(new File(target.getExternalFilesDir(null),"scans/"+name)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+name);
                    OutputStream out=new FileOutputStream(file)){out.write(KoreanModel.bytes(in));}
                try{
                    page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
                    var original=engine.read(page,1600);
                    var retried=PaddleRetry.read(page,original,engine.korean);
                    var trimmed=PaddleTrim.read(page,retried,engine.korean);
                    var source=QuantityRecovery.read(page,trimmed.lines);JSONArray diagnostics=new JSONArray();
                    var areas=NumericQuantities.find(page,source);
                    var recovered=NumericRecovery.read(page,source,engine.korean,areas,diagnostics);
                    Receipt before=PaddleReceipt.receipt(source),after=PaddleReceipt.receipt(recovered);
                    results.put(PaddleReceipt.parse(recovered).put("file",name).put("numeric",diagnostics));
                    if(index<2){corrected&=after.items.size()==5 && after.items.get(3).count==1
                        && after.items.get(3).printedTotal==3900 && Long.valueOf(9700).equals(after.total);
                        footerExcluded&=areas.originals.size()==4;
                        boolean agreed=false;for(int k=0;k<diagnostics.length();k++){
                            var d=diagnostics.getJSONObject(k);if(d.optBoolean("accepted"))agreed|=d.optString("paddle").equals("1")
                                && d.optString("english").equals("1") && d.optDouble("confidence")>=.95 && d.optDouble("englishConfidence")>=.95;
                        }corroborated&=agreed;
                    }
                    else{
                        preserved&=recovered==source && before.items.size()==after.items.size();
                        for(int i=0;i<Math.min(before.items.size(),after.items.size());i++){
                            Item a=before.items.get(i),b=after.items.get(i);
                            preserved&=a.unit==b.unit && a.count==b.count && a.printedTotal==b.printedTotal;
                        }
                    }
                }finally{if(page!=null)page.recycle();file.delete();}
                try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"numeric-quantity-probe.json"))){out.write(results.toString(2));}
            }
        }
        assertTrue("Printed CU quantity is one",corrected);assertTrue("Control fields unchanged",preserved);
        assertTrue("Subtotal quantity excluded",footerExcluded);
        assertTrue("Two trained models read the printed quantity",corroborated);
    }
}
