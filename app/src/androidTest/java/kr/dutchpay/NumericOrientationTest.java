package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.*;
import android.net.Uri;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import org.json.*;

public class NumericOrientationTest extends InstrumentationTestCase {
    public void testActualNumericRegionsAndRotation() throws Exception {
        var target=getInstrumentation().getTargetContext();JSONArray results=new JSONArray();
        String[] files={"03.jpg","Scan_20260729_191830.jpg","Scan_20260901_130311.jpg","01.jpg"};
        boolean sauce=false,split=false,rotated=false,upright=false,quantitiesRead=false;
        try(FullPaddle engine=new FullPaddle(target,true)){
            for(String name:files){
                File file=File.createTempFile("numeric-",".jpg",target.getCacheDir());Bitmap page=null;
                try(InputStream in=name.startsWith("Scan_")?new FileInputStream(new File(target.getExternalFilesDir(null),"scans/"+name)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+name);
                    OutputStream out=new FileOutputStream(file)){out.write(KoreanModel.bytes(in));}
                try{
                    page=InputImage.fromFilePath(target,Uri.fromFile(file)).getBitmapInternal();
                    var original=engine.read(page,1600);
                    try(var direction=PageOrientation.read(page,original,engine)){
                        var retried=PaddleRetry.read(direction.page,direction.lines,engine.korean);
                        var trimmed=PaddleTrim.read(direction.page,retried,engine.korean);
                        var quantities=QuantityRecovery.read(direction.page,trimmed.lines);JSONArray diagnostics=new JSONArray();
                        var recovered=NumericRecovery.read(direction.page,quantities,engine.korean,diagnostics);
                        Receipt r=PaddleReceipt.receipt(recovered);
                        results.put(PaddleReceipt.parse(recovered).put("file",name).put("degrees",direction.degrees).put("numeric",diagnostics));
                        if(name.equals("03.jpg")){
                            sauce=r.items.size()==20 && r.items.get(1).printedTotal==0;
                            quantitiesRead=r.items.stream().noneMatch(i->i.warning.contains("수량 미인식"));
                        }
                        if(name.contains("0729"))split=r.items.stream().anyMatch(i->i.unit==2400 && i.count==2 && i.printedTotal==4800);
                        if(name.contains("0901"))rotated=direction.degrees!=0 && Long.valueOf(118300).equals(r.total);
                        if(name.equals("01.jpg"))upright=direction.degrees==0 && Long.valueOf(58000).equals(r.total);
                    }
                }finally{if(page!=null)page.recycle();file.delete();}
                try(var out=new FileWriter(new File(target.getExternalFilesDir(null),"numeric-orientation-probe.json"))){out.write(results.toString(2));}
            }
        }
        assertTrue("Free sauce amount",sauce);assertTrue("Merged price/quantity",split);
        assertTrue("90 degree payment",rotated);assertTrue("Upright control",upright);
        assertTrue("All printed quantities read",quantitiesRead);
    }
}
