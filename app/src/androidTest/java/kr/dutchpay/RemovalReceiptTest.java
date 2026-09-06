package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class RemovalReceiptTest extends AndroidTestCase {
    private OcrWord w(String t,int x,int y){return new OcrWord(t,new Rect(x-20,y-10,x+20,y+10));}
    public void testSignedRemovalIsNotIncludedDiscount(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("급액",450,20),
            w("세트",70,50),w("1",300,50),w("9,000",450,50),
            w("기존 음료",70,80),w("-1",300,80),w("-1,200",450,80),
            w("교환 음료",70,110),w("1",300,110),w("1,400",450,110),
            w("합계",70,150),w("9,200",450,150)),"합계 9,200");
        assertNotNull(r);assertEquals(3,r.items.size());Item removed=r.items.get(1);
        assertEquals("기존 음료",removed.name);assertEquals(-1,removed.count);
        assertEquals(-1200L,removed.printedTotal);assertFalse(removed.includedDiscount);
        assertEquals(9200L,r.itemSum());
    }
    public void testUnsignedInlineDiscountKeepsExistingMeaning(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("음료",70,50),w("-1,200",450,50),w("8801234567890",70,80),
            w("2",300,80),w("1,200",450,80)),"");
        assertEquals(2,r.items.size());assertTrue(r.items.get(1).includedDiscount);
        assertEquals(1200L,r.itemSum());assertEquals(2,r.items.get(0).count);
    }
    public void testQuantityAndPriceSignsCannotCreatePositiveReturn(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("메뉴",70,50),w("1",300,50),w("5,000",450,50),
            w("모순 행",70,80),w("-1",300,80),w("1,200",450,80)),"");
        assertEquals(1,r.items.size());assertTrue(r.warnings.stream().anyMatch(s->s.contains("음수 수량")));
    }
}
