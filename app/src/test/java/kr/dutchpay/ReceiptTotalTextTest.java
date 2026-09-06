package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReceiptTotalTextTest {
    @Test public void paymentLabelsBeatDiscountAndMobileSections(){
        String raw="82,300\n합 계 금 액 5,000\n할 인 금 액\n일반할인 5,000\n"
            +"신용 카드 77,300\n신용승인정보[1]\n모바일정보\n결제금액:0";
        assertEquals(Long.valueOf(77300),ReceiptTotalText.read(raw));
        assertEquals(Long.valueOf(9500),ReceiptTotalText.read("승인금액9,500원\n합: 계 9.500원"));
        assertEquals(Long.valueOf(20000),ReceiptTotalText.read("부가세1,818\n계:20,000\n1.카드결제:20,000"));
        assertEquals(Long.valueOf(9700),ReceiptTotalText.read("총 구 매 액 4 9,700"));
        assertNull(ReceiptTotalText.read("결제금액 4 9,700"));
    }
    @Test public void splitApprovalsNeverReplaceWholeReceiptTotal(){
        String approvals="신용승인정보[1]\n승인금액40,000\n신용승인정보[2]\n승인금액1,000";
        assertEquals(Long.valueOf(41000),ReceiptTotalText.read("합계41,000\n"+approvals));
        assertNull(ReceiptTotalText.read(approvals));
        assertNull(ReceiptTotalText.read("신용승인정보[1]\n승인금액40,000\n신용승인정보[2]"));
        assertEquals(Long.valueOf(15000),ReceiptTotalText.read("합계15,000\n할인1,000\n카드10,000\n현금5,000"));
    }
    @Test public void noTotalsAreInventedOrTakenFromTaxAndIdentifiers(){
        assertNull(ReceiptTotalText.read("판매금액9,091\n부가세909\n승인번호10000"));
        assertNull(ReceiptTotalText.read("소계20,000\n계::20,000"));
        assertNull(ReceiptTotalText.read("결제금액20,000\n결제금액21,000"));
        assertEquals(Long.valueOf(50000),ReceiptTotalText.read("신용카드 매출전표\n[금액]50,000원[부가세]4,546"));
    }
    @Test public void cardMetadataSeparatesSettlementFromWholeReceipt(){
        String first="*** 신용 카드 ***\n카드번호 ****\n카드회사 테스트\n승인번호12345\n결제금액15,000\n";
        String second="*** 신용 카드 ***\n카드번호 ****\n카드회사 테스트\n승인번호12346\n결제금액3,700";
        assertEquals(Long.valueOf(18700),ReceiptTotalText.read(
            "총구매액 6 18,700\n할인금액-900\n결제금액18,700\n신용카드18,700\n"+first+second));
        assertNull(ReceiptTotalText.read(first+second));
        assertEquals(Long.valueOf(15000),ReceiptTotalText.read("신용승인정보[1]\n"+first));
    }
    @Test public void tenderedCashIsNotTheExpense(){
        String paid="받은금액10,000\n거스름돈1,000";
        assertEquals(Long.valueOf(9000),ReceiptTotalText.read("합계9,000\n"+paid));
        assertEquals(Long.valueOf(9000),ReceiptTotalText.read("합계9,000\n현금10,000\n"+paid));
        assertNull(ReceiptTotalText.read(paid));
        assertEquals(Long.valueOf(10000),ReceiptTotalText.read("받은금액10,000\n거스름돈0"));
        assertEquals(Long.valueOf(10400),ReceiptTotalText.read("합계금액12,700\n할인금액-2,300\n받은금액10,400"));
    }
    @Test public void discountHeaderDoesNotPromoteOnePayment(){
        String split="합계15,000\n신용카드10,000\n지역화폐5,000";
        assertEquals(Long.valueOf(15000),ReceiptTotalText.read("상품명 수량 할인 금액\n"+split));
        assertEquals(Long.valueOf(15000),ReceiptTotalText.read("할인금액-500\n"+split));
        assertEquals(Long.valueOf(15000),ReceiptTotalText.read("할인금액-500\n분할결제\n합계15,000\n신용카드10,000"));
    }
}
