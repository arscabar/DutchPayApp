package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReceiptMoneyTest {
    @Test public void matchingTotalDoesNotHideIndividualErrors() {
        Receipt r=new Receipt(); r.total=20000L;
        r.items.add(new Item("A",9000,1,10000)); r.items.add(new Item("B",11000,1,10000));
        r.validate(); assertEquals(20000,r.itemSum()); assertEquals(2,r.warnings.size());
        assertTrue(r.warnings.get(0).contains("단가 × 수량"));
        r.validate(); assertEquals(2,r.warnings.size());
    }
    @Test public void duplicateDiscountIsFlaggedWithoutChangingMoney() {
        Receipt r=new Receipt(); r.total=10000L;
        r.items.add(new Item("이미 할인된 품목",10000,1,10000));
        Item discount=new Item("할인",-1000,1,-1000); r.items.add(discount);
        r.validate(); assertEquals(9000,r.itemSum());
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("중복 차감")));
        discount.includedDiscount=true; r.validate();
        assertEquals(10000,r.itemSum()); assertTrue(r.warnings.isEmpty());
        discount.warning="할인 반영 여부 확인";r.validate();
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("반영 여부")));
    }
    @Test public void lineAmountAndMissingCountKeepTheirMeaning() {
        Receipt r=new Receipt(); r.total=5067L;
        Item item=Item.line("품목",null,2,5067); r.items.add(item); r.validate();
        assertEquals(5067,r.itemSum());
        item.warning="수량 미인식"; r.validate();
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("수량 미인식")));
        item.count=0; r.validate(); assertTrue(Receipt.issue(item).contains("허용 범위"));
    }
    @Test public void arithmeticOverflowCannotBecomeAValidTotal() {
        Receipt r=new Receipt(); r.items.add(new Item("잘못된 숫자",Long.MAX_VALUE,2,0));
        r.total=0L; r.validate();
        assertTrue(r.warnings.stream().anyMatch(w -> w.contains("계산 범위")));
        try { r.itemSum(); fail("overflow accepted"); } catch (ArithmeticException expected) {}
    }
}
