package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;
import java.util.*;

public class QuantityProbeTest extends InstrumentationTestCase {
    public void testQuantityCrops() throws Exception {
        var target=getInstrumentation().getTargetContext();
        File file=File.createTempFile("quantity-",".jpg",target.getCacheDir());
        try(var in=getInstrumentation().getContext().getAssets().open("receipts/03.jpg");
            var out=new FileOutputStream(file)){out.write(KoreanModel.bytes(in));}
        Bitmap page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
        JSONArray results=new JSONArray();
        try(var engine=new FullPaddle(target,true)) {
            var lines=engine.read(page,1600);OcrWord price=null,quantity=null;
            for(var line:lines){String s=ColumnRows.compact(line.word.text);
                if(s.equals("단가"))price=line.word;if(s.equals("수량"))quantity=line.word;}
            assertNotNull(price);assertNotNull(quantity);
            float end=page.getHeight();
            for(var line:lines)if(line.word.y()>price.y()+50 &&
                ColumnRows.footer(line.word.text))end=Math.min(end,line.word.y());
            for(var line:lines){var w=line.word;
                if(ColumnRows.number(w)==null || w.x()<price.x()-50 || w.x()>quantity.x()-10
                    || w.y()<price.y()+30 || w.y()>end)continue;
                float x=w.x()+quantity.x()-price.x(),y=w.y()+quantity.y()-price.y();
                int h=Math.max(12,Math.round(w.box.height()*.75f));
                Rect box=new Rect(Math.max(0,Math.round(x)-18),Math.max(0,Math.round(y)-h/2),
                    Math.min(page.getWidth(),Math.round(x)+18),Math.min(page.getHeight(),Math.round(y)+(h+1)/2));
                Bitmap crop=Bitmap.createBitmap(page,box.left,box.top,box.width(),box.height());
                try{var read=engine.korean.readWithConfidence(crop);
                    results.put(new JSONObject().put("unit",w.text).put("x",x).put("y",y)
                        .put("quantity",read.text).put("confidence",read.confidence));
                }finally{crop.recycle();}
            }
            try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"structure-quantity-probe.json"))){
                out.write(results.toString(2));}
            assertTrue(results.length()>0);
        }finally{page.recycle();file.delete();}
    }
}
