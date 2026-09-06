package kr.dutchpay;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class DiscountParsingTest {
    @Test public void explicitNegativeIsDiscountNotTheRemainingPrice() {
        Receipt r=Parser.parse(Arrays.asList("상품명 단가 수량 금액",
            "스팸 순두부찌개 10,900 1 10,900", "L 할인 :(-1,000 ) 9,900",
            "스팸 순두부찌개 10,900 1 10,900", "ㄴ할인: ( -1,000 ) 9,900",
            "합계 19,800"));
        assertEquals(4,r.items.size()); assertEquals(19800,r.itemSum());
        assertEquals(-1000,r.items.get(1).unit); assertEquals(-1000,r.items.get(3).unit);
        assertTrue(r.warnings.isEmpty());
        for(String row:new String[]{"할인 -1,000", "할인 1,000",
                "할인 (− 1,000) 9,900", "할인 -1,000 -1,000 9,900"}) {
            Receipt single=Parser.parse(Arrays.asList("상품명 금액",row));
            assertEquals(-1000,single.itemSum());
            assertTrue(single.items.get(0).warning.isEmpty());
        }
        Receipt ambiguous=Parser.parse(Arrays.asList("상품명 금액", "할인 -1,000 -2,000 9,900"));
        assertEquals(1,ambiguous.items.size()); assertEquals(0,ambiguous.itemSum());
        assertTrue(ambiguous.items.get(0).warning.contains("원본 확인 후 입력"));
    }
}
