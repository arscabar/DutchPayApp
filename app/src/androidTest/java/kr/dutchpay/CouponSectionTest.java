package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class CouponSectionTest extends AndroidTestCase {
    private OcrWord w(String t,int x,int y){return new OcrWord(t,new Rect(x-20,y-10,x+20,y+10));}
    private Receipt parse(String label,long total,boolean subtotal,boolean extra,boolean quantity){
        List<OcrWord> words=new ArrayList<>(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("식사",70,50),w("1",300,50),w("8,000",450,50)));
        if(subtotal)words.addAll(Arrays.asList(w("판매액",70,80),w("8,000",450,80)));
        words.addAll(Arrays.asList(w(label,70,110),w("1,200",450,110)));
        if(quantity)words.add(w("1",300,110));
        if(extra)words.addAll(Arrays.asList(w("쿠폰",70,140),w("100",450,140)));
        words.addAll(Arrays.asList(w("합계",70,170),w(""+total,450,170),
            w("신용카드",70,200),w(""+total,450,200),w("할인 상세",70,230),w("1,200",450,230)));
        return ColumnReceipt.parse(words,"합계 "+total);
    }
    public void testBracketPromotionUsesPrintedSettlementEvidence(){
        Receipt r=parse("[행사] 회원 혜택",6800,true,false,false);
        assertEquals(2,r.items.size());Item i=r.items.get(1);
        assertEquals(1200L,i.printedTotal);assertEquals(-1200L,i.baseAmount());
        assertEquals(6800L,r.itemSum());assertTrue(i.warning.contains("원본 확인"));
    }
    public void testExplicitCouponIsNotRepeatedInPaymentDetails(){
        Receipt r=parse("쿠폰 할인",6800,true,false,false);
        assertEquals(2,r.items.size());assertEquals(6800L,r.itemSum());
    }
    public void testPriceArrowRequiresExactlyTwoCurrencyPricesAndMatchingDifference(){
        Receipt r=parse("식사8,000원→6,800원",6800,true,false,false);
        assertEquals(2,r.items.size());assertEquals(-1200L,r.items.get(1).baseAmount());
        for(String label:new String[]{"식사8,000원→6,700원","식사6,800원→8,000원",
            "식사8,000→6,800","식사2개8,000원→6,800원","식사8,000원→6,800원100원",
            "식사2026-05-15→2026-05-16","식사8,000원6,800원","식사8,00원→6,800원"}){
            r=parse(label,6800,true,false,false);
            assertEquals(label,1,r.items.size());assertEquals(8000L,r.itemSum());
        }
    }
    public void testAmountIsNeverInventedToBalanceReceipt(){
        for(Receipt r:new Receipt[]{parse("쿠폰 할인",6700,true,false,false),
            parse("쿠폰 할인",6800,false,false,false),parse("미확인 금액",6800,true,false,false),
            parse("쿠폰 할인",6700,true,true,false),parse("쿠폰 할인",6800,true,false,true)}){
            assertEquals(1,r.items.size());assertEquals(8000L,r.itemSum());assertFalse(r.warnings.isEmpty());
        }
    }
}
