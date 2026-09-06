package kr.dutchpay;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ReceiptValidationTest {
    @Test public void inheritedTotalRefreshesWarningsWithoutChangingOriginal() {
        Receipt original = Parser.parse(Arrays.asList("합계 10"));
        Receipt crop = Parser.parse(Arrays.asList("상품명 수량 금액", "음료 1 6,702"));
        assertTrue(crop.warnings.get(0).contains("합계를 읽지 못했습니다"));
        crop.warnings.add("단가 미인식: 추가 품목");
        crop.supplyTotal(original.total);
        String mismatch = "추출 합계 6702원 / 영수증 합계 10원: 확인 필요";
        assertEquals(Arrays.asList("단가 미인식: 추가 품목", mismatch), crop.warnings);
        crop.validate(); crop.supplyTotal(6702L);
        assertEquals(2, crop.warnings.size());
        assertEquals(Long.valueOf(10), crop.total);
        assertEquals(Long.valueOf(10), original.total);
        assertTrue(original.items.isEmpty());
        assertEquals(1, original.warnings.size());

        crop.total = 6702L; crop.validate();
        assertEquals(Arrays.asList("단가 미인식: 추가 품목"), crop.warnings);
        original.supplyTotal(null); original.validate();
        assertEquals(1, original.warnings.size());
    }
}
