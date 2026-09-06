package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class ColumnReceiptTest extends AndroidTestCase {
    private OcrWord w(String text,int x,int y){return new OcrWord(text,new Rect(x-25,y-10,x+25,y+10));}
    public void testSeparateCodeAndQuantity(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("상품명",70,20),w("수량",300,20),w("금액",450,20),
            w("000123 메뉴1+1",90,50),w("AL790003",70,80),w("2",300,80),w("7,200",450,80),
            w("합계",70,120),w("7,200",450,120)),"상품명 수량 금액\n합계 7,200");
        assertNotNull(r);assertEquals(1,r.items.size());assertEquals("메뉴1+1",r.items.get(0).name);
        assertEquals(2,r.items.get(0).count);assertEquals(7200L,r.items.get(0).printedTotal);
    }
    public void testIncludedDiscountAndZeroOption(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("음료",70,50),w("-2,400",450,50),w("8801234567890",70,80),w("2",300,80),w("2,400",450,80),
            w("기본 옵션",70,110),w("1",300,110),w("(0)",450,110),
            w("합계",70,150),w("2,400",450,150)),"상품명 수량 금액\n합계 2,400");
        assertNotNull(r);assertEquals(3,r.items.size());assertTrue(r.items.get(1).includedDiscount);
        assertEquals(0L,r.items.get(2).printedTotal);assertEquals(2400L,r.itemSum());
    }
    public void testNoColumnsFallsBack(){assertNull(ColumnReceipt.parse(Arrays.asList(w("메뉴 9000",70,50)),""));}
    public void testDiscountCannotHideMissingQuantity(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("식사",70,60),w("10,000",200,60),w("9,000",450,60),
            w("합계",70,100),w("10,000",450,100),w("할인",70,140),w("1,000",450,140)),"합계 10,000\n할인 1,000");
        assertNotNull(r);assertTrue(r.items.get(0).warning.contains("수량 미인식"));
        assertFalse(r.items.get(0).quantityKnown);assertEquals(0,r.items.get(0).count);
        assertFalse(r.items.get(0).warning.contains("할인 전"));assertEquals(9000L,r.itemSum());
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("추가 차감 보류")));
    }
    public void testSeparatorCannotBecomePendingName(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("단가",200,20),w("수량",300,20),w("금액",450,20),
            w("=======",70,50),w("뼈추가",70,80),w("13,000",200,80),w("1",300,80),w("13,000",450,80),
            w("합계",70,120),w("13,000",450,120)),"합계 13,000");
        assertEquals(1,r.items.size());assertEquals("뼈추가",r.items.get(0).name);assertEquals(13000L,r.itemSum());
    }
    public void testAmountNearNextNameUsesPendingName(){
        Receipt r=ColumnReceipt.parse(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("첫 메뉴",70,50),w("8801234567890",70,70),w("1,800",450,92),w("둘째 메뉴",70,95),
            w("8801234567891",70,120),w("1",300,120),w("1,500",450,120),
            w("합계",70,150),w("3,300",450,150)),"상품명 수량 금액\n합계 3,300");
        assertEquals(2,r.items.size());assertEquals("첫 메뉴",r.items.get(0).name);
        assertEquals(1800L,r.items.get(0).printedTotal);assertEquals("둘째 메뉴",r.items.get(1).name);
    }
}
