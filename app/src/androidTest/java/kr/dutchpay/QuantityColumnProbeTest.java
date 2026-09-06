package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.*;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import org.json.*;
import java.io.*;
import java.util.concurrent.TimeUnit;

public class QuantityColumnProbeTest extends InstrumentationTestCase {
    public void testQuantityColumn() throws Exception {
        var target=getInstrumentation().getTargetContext();
        File file=File.createTempFile("quantity-column-",".jpg",target.getCacheDir());
        Bitmap page=null,crop=null,large=null;
        try(var recognizer=Ocr.create()) {
            try(var in=getInstrumentation().getContext().getAssets().open("receipts/03.jpg");
                var out=new FileOutputStream(file)){out.write(KoreanModel.bytes(in));}
            page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
            java.util.List<PaddleLine> lines;
            try(var engine=new FullPaddle(target,true)){lines=engine.read(page,1600);}
            OcrWord price=null,quantity=null,amount=null;
            for(var line:lines){String s=ColumnRows.compact(line.word.text);
                if(s.equals("단가"))price=line.word;if(s.equals("수량"))quantity=line.word;}
            assertNotNull(price);assertNotNull(quantity);
            for(var line:lines)if(ColumnRows.compact(line.word.text).equals("금액")
                && line.word.x()>quantity.x() && (amount==null ||
                Math.abs(line.word.y()-quantity.y())<Math.abs(amount.y()-quantity.y())))amount=line.word;
            assertNotNull(amount);int end=page.getHeight();
            for(var line:lines)if(line.word.box.top>quantity.box.bottom && ColumnRows.footer(line.word.text))
                end=Math.min(end,line.word.box.top);
            assertTrue("No footer label",end<page.getHeight());
            Rect b=new Rect(Math.max(0,Math.round((price.x()+quantity.x())/2)),quantity.box.bottom,
                Math.min(page.getWidth(),Math.round((quantity.x()+amount.x())/2)),end);
            assertFalse(b.isEmpty());crop=Bitmap.createBitmap(page,b.left,b.top,b.width(),b.height());
            large=Bitmap.createScaledBitmap(crop,crop.getWidth()*4,crop.getHeight()*4,true);
            var text=Tasks.await(recognizer.process(InputImage.fromBitmap(large,0)),90,TimeUnit.SECONDS);
            JSONArray words=new JSONArray();
            for(var block:text.getTextBlocks())for(var line:block.getLines())for(var element:line.getElements()){
                Rect r=element.getBoundingBox();if(r==null)continue;
                JSONArray quad=new JSONArray();
                for(int[] p:new int[][]{{r.left,r.top},{r.right,r.top},{r.right,r.bottom},{r.left,r.bottom}})
                    quad.put(new JSONArray().put(b.left+p[0]/4.0).put(b.top+p[1]/4.0));
                words.put(new JSONObject().put("text",element.getText()).put("quad",quad));
            }
            JSONObject result=new JSONObject().put("file","03.jpg").put("scale",4)
                .put("crop",new JSONArray(new int[]{b.left,b.top,b.right,b.bottom}))
                .put("raw",text.getText()).put("elements",words);
            try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"structure-quantity-column-probe.json"))){
                out.write(result.toString(2));}
            assertTrue(words.length()>0);
        } finally {
            if(large!=null && large!=crop)large.recycle();
            if(crop!=null && crop!=page)crop.recycle();if(page!=null)page.recycle();file.delete();
        }
    }
}
