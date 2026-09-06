package kr.dutchpay;

import android.graphics.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.util.*;
import org.json.*;

public class EnglishReviewTest extends InstrumentationTestCase {
    KoreanModel.Reading r(String text,double score){return new KoreanModel.Reading(text,score);}
    public void testAcceptanceGuards(){
        assertEquals("All New)",EnglishChoice.choose("A11New)",Arrays.asList(r("All New)",.98),r("All New)",.97))));
        assertEquals("A11New)",EnglishChoice.choose("A11New)",Arrays.asList(r("All New)",.94),r("All New)",.99))));
        assertEquals("355m7",EnglishChoice.choose("355m7",Arrays.asList(r("350ml",.99),r("350ml",.99))));
        assertEquals("355m7",EnglishChoice.choose("355m7",Arrays.asList(r("355m1",.99),r("355m1",.99))));
        assertEquals("355ml",EnglishChoice.choose("355m7",Arrays.asList(r("355ml",.99),r("355ml",.99))));
        assertFalse(EnglishChoice.numberSafe("A111","All"));
        assertEquals("A11New)",EnglishChoice.choose("A11New)",Arrays.asList(r("All New",.99),r("All New",.99))));
        assertEquals("-1,000",EnglishChoice.choose("-1,000",Arrays.asList(r("1000",.99),r("1000",.99))));
        assertEquals("메뉴2개-1,000원",EnglishSpans.body("All New)메뉴 2개 -1,000원"));
    }
    public void testActualLatinRegions() throws Exception {
        var ctx=getInstrumentation().getTargetContext();
        var args=((android.test.InstrumentationTestRunner)getInstrumentation()).getArguments();
        String[] files=args.getString("files","Scan_20260629_144710.jpg,Scan_20260715_130133.jpg,Scan_20260723_124307.jpg,Scan_20260730_125509.jpg").split(",");
        JSONArray output=new JSONArray();
        try(var paddle=new FullPaddle(ctx,true);var english=new EnglishReview(ctx)){
            for(String file:files){
                File source=new File(ctx.getExternalFilesDir(null),"scans/"+file);
                Bitmap page=InputImage.fromFilePath(ctx,Uri.fromFile(source)).getBitmapInternal();
                try{
                    var lines=paddle.read(page,1600);var receipt=PaddleReceipt.receipt(lines);
                    try(var patches=NamePatches.create(page,lines,receipt)){
                        JSONArray names=new JSONArray();
                        for(int n=0;n<patches.items.size();n++){
                            String old=patches.items.get(n).name;if(!EnglishSpans.eligible(old))continue;
                            Bitmap crop=patches.originals.get(n);long start=System.currentTimeMillis();
                            var next=english.read(crop,old,paddle.korean);
                            assertFalse(crop.isRecycled());
                            assertEquals(EnglishSpans.body(old),EnglishSpans.body(next.text));
                            names.put(new JSONObject().put("old",old).put("new",next.text).put("diagnostics",next.diagnostics)
                                .put("milliseconds",System.currentTimeMillis()-start));
                        }
                        output.put(new JSONObject().put("file",file).put("names",names));
                    }
                }finally{page.recycle();}
            }
        }
        try(var out=new FileWriter(new File(ctx.getExternalFilesDir(null),"english-review.json"))){out.write(output.toString(2));}
        assertEquals(files.length,output.length());
    }
}
