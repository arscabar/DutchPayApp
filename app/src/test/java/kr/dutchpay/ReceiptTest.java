package kr.dutchpay;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ReceiptTest {
    @Test public void calculationAndParsing() {
        assertEquals(9000, Item.amount(9000,"2","50"));
        assertEquals(-500, Item.amount(-1000,"1","50"));
        assertEquals(2, Item.amount(3,"1","50"));
        assertEquals(-2, Item.amount(-3,"1","50"));
        for (String p : new String[]{"-1","101","NaN",""}) {
            try { Item.amount(1000,"1",p); fail("invalid percent accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        Receipt r = Parser.parse(Arrays.asList("상품명 단가 수량 금액",
            "김치찌개 9,000 2 18,000", "할인 -1,000", "합계 17,000", "부가세 1,545"));
        assertEquals(2,r.items.size()); assertEquals(17000,r.itemSum());
        assertTrue(r.warnings.isEmpty());
        Receipt slip = Parser.parse(Arrays.asList("신용카드전표", "판매 금액 9,091", "합계 10,000"));
        assertTrue(slip.items.isEmpty()); assertFalse(slip.warnings.isEmpty());
        Receipt two = Parser.parse(Arrays.asList("상품명 수량 할인 금액", "소고기국밥",
            "10,000 2 0 20,000", "합계 20,000"));
        assertEquals(20000,two.itemSum()); assertEquals("소고기국밥",two.items.get(0).name);
        Receipt dots = Parser.parse(Arrays.asList("상품명 단가 수량 금액",
            "김치찌개 9. 000 2 18. 000", "합계 18.000"));
        assertEquals(18000,dots.itemSum()); assertTrue(dots.warnings.isEmpty());
        Receipt decorated = Parser.parse(Arrays.asList("대기번호 -036- 번",
            "왕돈까스 - 1- 9. 500", "결재구분 카드", "합계 9,500"));
        assertEquals(9500,decorated.itemSum()); assertEquals("왕돈까스",decorated.items.get(0).name);
        Receipt discount = Parser.parse(Arrays.asList("상품명 수량 금액",
            "상품A -5,200", "8801047161608 2 5,200", "과세물품가액 4,727",
            "할인 -5,200", "합계 5,200"));
        assertEquals(2,discount.items.size()); assertEquals(5200,discount.itemSum());
        Receipt missing = Parser.parse(Arrays.asList("상품명 수량 금액",
            "음료 3,900", "할인 -1,000", "합계 2,900"));
        assertEquals(2900,missing.itemSum()); assertFalse(missing.items.get(0).warning.isEmpty());
    }
}
