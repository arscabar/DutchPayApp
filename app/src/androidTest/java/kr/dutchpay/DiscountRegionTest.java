package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class DiscountRegionTest extends AndroidTestCase {
    private OcrWord w(String s,int x,int y){return new OcrWord(s,new Rect(x-25,y-10,x+25,y+10));}
    private List<OcrWord> base(){return new ArrayList<>(Arrays.asList(
        w("단가",200,20),w("수량",300,20),w("금액",450,20),
        w("식사",70,50),w("10,000",200,50),w("1",300,50),w("10,000",450,50)));}
    private void row(List<OcrWord> words,String label,String value,int y){words.add(w(label,70,y));words.add(w(value,450,y));}
    public void testDiscountAfterTaxUsesPrintedAmount(){
        var words=base();row(words,"합계","10,000",90);row(words,"과세물품가액","8,182",120);
        row(words,"부가세","818",150);row(words,"할인금액","-1,000",180);
        row(words,"받을금액","9,000",210);
        Receipt r=ColumnReceipt.parse(words,"합계 10,000\n받을금액 9,000");
        assertEquals(2,r.items.size());assertEquals(-1000L,r.items.get(1).baseAmount());
        assertEquals(9000L,r.itemSum());assertEquals(1,r.items.get(0).count);
    }
    public void testTaxSeparatedSummaryDoesNotRepeatInlineDiscount(){
        var words=base();row(words,"할인","-1,000",75);row(words,"합계","9,000",110);
        row(words,"과세물품가액","8,182",140);row(words,"부가세","818",170);
        row(words,"할인금액","1,000",200);row(words,"할인내역","1,000",230);
        Receipt r=ColumnReceipt.parse(words,"합계 9,000");
        assertEquals(2,r.items.size());assertEquals(9000L,r.itemSum());
    }
    public void testPaymentAndMembershipDetailsAreOutsideDiscountRegion(){
        for(String boundary:Arrays.asList("신용카드","카드거래명세표","승인일시","회원정보","모바일정보")){
            var words=base();row(words,"합계","10,000",90);row(words,"부가세","909",120);
            words.add(w(boundary,70,150));row(words,"할인금액","7,000",180);
            Receipt r=ColumnReceipt.parse(words,"합계 10,000");
            assertEquals(boundary,1,r.items.size());assertEquals(boundary,10000L,r.itemSum());
        }
    }
    public void testConflictingPrintedDiscountsRemainUnapplied(){
        var words=base();row(words,"합계","10,000",90);row(words,"부가세","909",120);
        row(words,"할인금액","1,000",150);row(words,"할인내역","2,000",180);
        Receipt r=ColumnReceipt.parse(words,"합계 10,000");
        assertEquals(1,r.items.size());assertEquals(10000L,r.itemSum());assertFalse(r.warnings.isEmpty());
    }
}
