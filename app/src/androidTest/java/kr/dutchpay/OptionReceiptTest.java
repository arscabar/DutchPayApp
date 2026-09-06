package kr.dutchpay;

import android.net.Uri;
import android.graphics.Rect;
import android.test.InstrumentationTestCase;
import com.google.mlkit.vision.common.InputImage;
import java.io.*;
import java.util.*;

public class OptionReceiptTest extends InstrumentationTestCase {
    private OcrWord w(String text,int x,int y){return new OcrWord(text,new Rect(x-25,y-10,x+25,y+10));}
    public void testHeaderSearchDoesNotStartFromMetadata(){
        var c=ColumnLayout.find(Arrays.asList(w("서류번호",70,10),w("발급",70,25),
            w("수량",300,40),w("금액",450,40),w("상품명",70,60),w("단가",200,60)));
        assertNotNull(c);assertEquals(Float.valueOf(200),c.price);
    }
    public void testPendingNameKeepsNumbersBeforeNextName(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("메뉴",70,60),w("9,000",200,85),w("1",300,85),w("8,500",450,85),w("소스",70,93),
            w("0",200,103),w("1",300,103),w("0",450,103),w("합계",70,140),w("8,500",450,140)),"합계 8,500");
        assertEquals(2,r.items.size());assertEquals("메뉴",r.items.get(0).name);
        assertEquals(9000L,r.items.get(0).unit);assertEquals("소스",r.items.get(1).name);
    }
    public void testSmallQuantityBoxStaysWithAmount(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("추가",70,60),w("1,878",450,67),new OcrWord("2",new Rect(297,62,303,72)),
            w("1,000",200,67),w("합계",70,100),w("1,878",450,100)),"합계 1,878");
        assertEquals(1,r.items.size());assertEquals(2,r.items.get(0).count);assertEquals(1000L,r.items.get(0).unit);
    }
    public void testTiltedHeaderNetAmountsAndSummaryDiscount(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("상품명",80,40),w("단가",300,48),w("수량",400,40),w("금액",520,30),
            w("식사",80,80),w("10,000",300,100),w("2",400,92),w("18,000",520,82),
            w("추가",80,125),w("1,000",300,125),w("1",400,117),w("900",520,107),
            w("합계",80,160),w("21,000",520,142),w("할인금액",80,180),w("2,100",520,162),
            w("할인내역",80,210),w("2,100",520,192)),"합계 18,900");
        assertNotNull(r);assertEquals(3,r.items.size());assertEquals("식사",r.items.get(0).name);
        assertEquals(2,r.items.get(0).count);assertEquals(10000L,r.items.get(0).unit);
        assertEquals(18000L,r.items.get(0).printedTotal);assertEquals(18900L,r.itemSum());
    }
    public void testInlineDiscountIsNotRepeatedByFooter(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("식사",70,50),w("10,000",200,50),w("1",300,50),w("10,000",450,50),
            w("할인",70,80),w("-1,000",450,80),w("합계",70,110),w("9,000",450,110),
            w("할인금액",70,140),w("1,000",450,140)),"합계 9,000");
        assertEquals(2,r.items.size());assertEquals(9000L,r.itemSum());
    }
    public void testQuantityOcrIsNeverRepairedFromTotal(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("식사",70,50),w("8,900",200,50),w("11",300,50),w("8,359",450,50),
            w("합계",70,90),w("8,359",450,90)),"합계 8,359");
        assertEquals(11,r.items.get(0).count);assertEquals(8359L,r.items.get(0).printedTotal);
        assertFalse(r.warnings.isEmpty());
    }
    public void testCaptureOptionCoordinates() throws Exception {
        var target=getInstrumentation().getTargetContext();
        File copy=File.createTempFile("option-receipt-",".jpg",target.getCacheDir());
        try {
            try(var in=getInstrumentation().getContext().getAssets().open("receipts/03.jpg");
                var out=new FileOutputStream(copy)){out.write(KoreanModel.bytes(in));}
            var page=InputImage.fromFilePath(target,Uri.fromFile(copy)).getBitmapInternal();
            try(var engine=new FullPaddle(target,true)) {
                var original=engine.read(page,1600);
                var split=PaddleRetry.read(page,original,engine.korean);
                var trimmed=PaddleTrim.read(page,split,engine.korean);
                var quantities=QuantityRecovery.read(page,trimmed.lines);
                var result=PaddleReceipt.parse(quantities).put("file","03.jpg");
                List<OcrWord> words=new ArrayList<>();List<Double> slopes=new ArrayList<>();
                for(var line:trimmed.lines){words.add(line.word);var a=line.points.get(0);var b=line.points.get(1);
                    if(b.x-a.x>80)slopes.add((double)(b.y-a.y)/(b.x-a.x));}
                Collections.sort(slopes);var layout=ColumnLayout.find(WordGeometry.flatten(words,slopes.isEmpty()?0:slopes.get(slopes.size()/2)));
                if(layout!=null)result.put("columnDebug",new org.json.JSONObject().put("price",layout.price)
                    .put("quantity",layout.quantity).put("amount",layout.amount).put("start",layout.start));
                File output=new File(target.getExternalFilesDir(null),"option-v6-"+System.currentTimeMillis()+".json");
                try(var out=new OutputStreamWriter(new FileOutputStream(output),"UTF-8")){out.write(result.toString(2));}
                System.out.println("OPTION_COORDINATES="+output.getName());
                assertTrue(result.getJSONArray("lines").length()>0);
            } finally {page.recycle();}
        } finally {copy.delete();}
    }
}
