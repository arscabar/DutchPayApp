package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class QuantityRowTest extends AndroidTestCase {
    private OcrWord w(String text,int l,int t,int r,int b){return new OcrWord(text,new Rect(l,t,r,b));}
    public void testSkewedOrphanQuantityKeepsPreviousBarcodeItem(){
        OcrWord one=w("1",445,658,456,684);
        List<OcrWord> words=Arrays.asList(w("수량",380,544,472,596),w("금액",558,558,686,610),
            w("첫 품목",18,578,533,674),w("8801234567890",44,627,326,684),one,w("2,700",577,664,683,709),
            w("다음 품목",25,667,449,724),w("8801234567891",46,713,321,758),
            w("2",443,724,452,754),w("5,300",576,731,683,775));
        Receipt receipt=ColumnReceipt.parse(words,"");assertEquals(2,receipt.items.size());
        Item first=receipt.items.get(0),next=receipt.items.get(1);
        assertEquals("첫 품목",first.name);assertEquals(1,first.count);assertTrue(first.quantityKnown);
        assertEquals("다음 품목",next.name);assertEquals(2,next.count);assertEquals(5300L,next.printedTotal);
        assertEquals(2700L,first.printedTotal);assertEquals(8000L,receipt.itemSum());
        assertEquals(new Rect(445,658,456,684),one.box);assertEquals("1",one.text);
    }
    public void testAmbiguousOrPreviouslyLinkedCellsAreNotMoved(){
        OcrWord q=w("수량",280,-10,320,10),a=w("금액",480,10,520,30);
        for(List<OcrWord> words:Arrays.asList(
            Arrays.asList(w("1",296,82,304,94),w("700",480,75,520,125),w("900",480,100,520,150)),
            Arrays.asList(w("1",296,84,304,92),w("2",296,90,304,98),w("700",480,95,520,125)),
            Arrays.asList(w("1",296,90,304,120),w("700",480,90,520,120)),
            Arrays.asList(w("7500",200,82,240,94),w("700",480,95,520,125)))){
            ColumnLayout c=new ColumnLayout(ColumnRows.group(words));c.quantity=300;c.amount=500;c.start=0;
            assertSame(words,QuantityRows.align(words,c,q,a));
        }
    }
    public void testPreviousBarcodeCannotStealAnAlignedItemName(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",280,10,320,30),w("금액",480,10,520,30),
            w("금액 없는 앞 품목",40,60,190,80),w("8801234567890",40,90,190,110),
            w("현재 품목",40,130,190,150),w("2",290,130,310,150),w("3,200",480,130,520,150)),"");
        assertEquals(1,r.items.size());assertEquals("현재 품목",r.items.get(0).name);
        assertEquals(2,r.items.get(0).count);assertEquals(3200L,r.itemSum());
    }
}
