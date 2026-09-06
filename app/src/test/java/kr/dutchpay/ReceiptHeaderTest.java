package kr.dutchpay;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ReceiptHeaderTest {
    @Test public void tableHeadersAndOcrPosStartTheItemSection() {
        for(String header:new String[]{"상품\t단가 수량\t금액", "메뉴\t단가 수량\t금액",
                "28278\t2026-09-04(금) P0S-01", "POS-01"}) {
            Receipt r=Parser.parse(Arrays.asList(header,"국밥\t10,000\t2\t20,000","합계 20,000"));
            assertEquals(1,r.items.size()); assertEquals("국밥",r.items.get(0).name);
            assertEquals(2,r.items.get(0).count); assertEquals(20000,r.itemSum());
            assertTrue(r.warnings.isEmpty());
        }
        Receipt metadata=Parser.parse(Arrays.asList("판매시간 P0S100", "담당 1 1000"));
        assertTrue(metadata.items.isEmpty());
        for(String end:new String[]{"소\t계 20,000", "계:: 20,000"}) {
            Receipt bounded=Parser.parse(Arrays.asList("상품 단가 수량 금액",
                "계란국 10,000 2 20,000",end,"카드 승인 1 20,000"));
            assertEquals(1,bounded.items.size()); assertEquals(20000,bounded.itemSum());
            assertNull(bounded.total); // A subtotal is not a verified payment total.
            assertTrue(bounded.warnings.get(0).contains("합계를 읽지 못했습니다"));
        }
    }
}
