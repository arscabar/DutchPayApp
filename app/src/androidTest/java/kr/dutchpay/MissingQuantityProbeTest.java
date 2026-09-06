package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.util.*;
import org.json.*;

public class MissingQuantityProbeTest extends InstrumentationTestCase {
    public void testActualMissingQuantityCells() throws Exception {
        var test=getInstrumentation().getContext();var target=getInstrumentation().getTargetContext();JSONArray results=new JSONArray();
        JSONArray cases;try(var in=test.getAssets().open("receipts/missing-quantity-cases.json")){cases=new JSONArray(new String(KoreanModel.bytes(in),"UTF-8"));}
        boolean money=true,known=true,correct=true,controls=true;
        try(FullPaddle engine=new FullPaddle(target,true)){
            for(int k=0;k<cases.length();k++){
                var spec=cases.getJSONObject(k);String name=spec.getString("file");File file=File.createTempFile("missing-qty-",".jpg",target.getCacheDir());Bitmap page=null;
                try(var in=name.startsWith("Scan_")?new FileInputStream(new File(target.getExternalFilesDir(null),"scans/"+name)):
                    test.getAssets().open("receipts/"+name);var out=new FileOutputStream(file)){out.write(KoreanModel.bytes(in));}
                try{
                    page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
                    var trim=PaddleTrim.read(page,PaddleRetry.read(page,engine.read(page,1600),engine.korean),engine.korean);
                    var source=NumericRecovery.read(page,QuantityRecovery.read(page,trim.lines),engine.korean);
                    Receipt before=PaddleReceipt.receipt(source);JSONArray log=new JSONArray();
                    var lines=MissingQuantity.read(page,source,engine.korean,log);Receipt after=PaddleReceipt.receipt(lines);
                    var result=PaddleReceipt.parse(lines).put("file",name).put("missingQuantity",log).put("before",PaddleReceipt.parse(source));
                    results.put(result);money&=Objects.equals(before.total,after.total) && before.items.size()==after.items.size();
                    for(int i=0;i<Math.min(before.items.size(),after.items.size());i++){
                        Item a=before.items.get(i),b=after.items.get(i);money&=a.name.equals(b.name) && a.printedTotal==b.printedTotal && a.includedDiscount==b.includedDiscount;
                        if(a.quantityKnown)known&=b.quantityKnown && a.count==b.count && a.unit==b.unit;
                    }
                    var rows=spec.optJSONObject("rows");if(rows==null)controls&=lines==source;
                    else for(var keys=rows.keys();keys.hasNext();){String row=keys.next();int index=Integer.parseInt(row)-1;
                        correct&=index<after.items.size() && after.items.get(index).quantityKnown && after.items.get(index).count==rows.getInt(row);
                    }
                }finally{if(page!=null)page.recycle();file.delete();}
                try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"missing-quantity-probe.json"))){out.write(results.toString(2));}
            }
        }
        assertTrue("Names, printed amounts and totals preserved",money);assertTrue("Known quantities preserved",known);
        assertTrue("Controls unchanged",controls);assertTrue("All reviewed printed quantities recovered",correct);
    }
}
