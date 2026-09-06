package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class PrintedUnitTest extends AndroidTestCase {
    private OcrWord w(String t,int x,int y){return new OcrWord(t,new Rect(x-20,y-10,x+20,y+10));}
    public void testSpacedAndMisreadUnitHeaders(){
        for(String label:new String[]{"단 가","딘가"}){
            Receipt r=ColumnReceipt.parse(Arrays.asList(w(label,200,20),w("수 량",300,20),w("금 액",450,20),
                w("메뉴",70,60),w("9,000",200,60),w("2",300,60),w("18,000",450,60)),"");
            assertNotNull(r);assertEquals(1,r.items.size());Item i=r.items.get(0);
            assertFalse(i.amountBased);assertEquals(9000L,i.unit);assertEquals(18000L,i.baseAmount());
        }
    }
    public void testPrintedUnitUnderNameWithoutUnitHeader(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),w("메뉴",70,50),
            w("10,000",70,80),w("2",300,80),w("0",370,80),w("20,000",450,80)),"");
        Item i=r.items.get(0);assertEquals(10000L,i.unit);assertFalse(i.amountBased);
        assertEquals(2,i.count);assertTrue(i.warning.contains("인쇄 가격"));
    }
    public void testNoPrintedUnitIsNeverDerived(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),w("메뉴",70,50),
            w("8801234567890",70,80),w("2",300,80),w("20,000",450,80)),"");
        assertTrue(r.items.get(0).amountBased);
    }
    public void testObservedUnitInsideBroadQuantityBand(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",200,20),w("금액",445,20),w("메뉴",100,50),
            w("7,500",110,80),w("2",200,80),w("0",315,80),w("15,000",445,80)),"");
        assertFalse(r.items.get(0).amountBased);assertEquals(7500L,r.items.get(0).unit);
        assertEquals(2,r.items.get(0).count);assertEquals(15000L,r.itemSum());
    }
    public void testQuantitySuffixIsOnlyReadInQuantityColumn(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),w("메뉴2입",70,50),
            w("2,500",70,80),w("4개",270,80),w("10,000",450,80)),"");
        assertEquals("메뉴2입",r.items.get(0).name);assertEquals(4,r.items.get(0).count);
        assertEquals(2500L,r.items.get(0).unit);
        assertNull(ColumnRows.number(w("4개",70,10)));
        assertNull(ColumnRows.quantity(w("2입",300,10)));
        assertNull(ColumnRows.quantity(w("상품2개",300,10)));
    }
}
