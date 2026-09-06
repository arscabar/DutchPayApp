package kr.dutchpay;

import java.util.ArrayList;
import java.util.List;

public class Receipt {
    public final List<Item> items = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();
    private final List<String> validationWarnings = new ArrayList<>();
    public String raw = "";
    public String originalRaw = "", method = "";
    public String nameDiagnostics = "[]", numericDiagnostics = "[]";
    public Long total;
    boolean paymentOnly() {
        return items.isEmpty() && raw.replaceAll("\\s","").startsWith("신용카드전표");
    }
    public long itemSum() {
        long sum=0;
        for (Item i : items) sum=Math.addExact(sum,i.baseAmount());
        return sum;
    }
    void supplyTotal(Long fallback) {
        if (total == null) { total = fallback; validate(); }
    }
    public void validate() {
        warnings.removeAll(validationWarnings); validationWarnings.clear();
        if (items.isEmpty()) validationWarnings.add(paymentOnly()?
            "품목이 확인되지 않은 카드전표입니다. 결제 총액을 확인하세요":
            "품목을 찾지 못했습니다. 품목 없는 전표이거나 인식 실패입니다.");
        else if (total == null) validationWarnings.add("영수증 합계를 읽지 못했습니다. 원본 확인 필요");
        try {
            long sum=itemSum(), discounts=0;
            for (Item i : items) if (i.baseAmount()<0)
                discounts=Math.addExact(discounts,i.baseAmount());
            if (total != null && !items.isEmpty() && total != sum) {
                validationWarnings.add("추출 합계 " + sum + "원 / 영수증 합계 " + total + "원: 확인 필요");
                if (discounts<0 && Math.subtractExact(sum,discounts)==total)
                    validationWarnings.add("할인이 이미 품목 금액에 반영됐을 수 있습니다. 중복 차감 여부를 확인하세요");
            }
        } catch (ArithmeticException e) { validationWarnings.add("금액이 계산 범위를 넘었습니다. 원본 확인 필요"); }
        if (total!=null && total<0) validationWarnings.add("원본 합계가 음수입니다. 반품 여부를 확인하세요");
        for (Item i : items) {
            String issue=issue(i);
            if (!issue.isEmpty()) validationWarnings.add(i.name+": "+issue);
        }
        warnings.addAll(validationWarnings);
    }
    static String issue(Item i) {
        if (i.includedDiscount) return i.warning;
        if ((i.quantityKnown?i.count==0 || i.count< -9999 || i.count>9999:i.count!=0)
                || i.unit < -1000000000L || i.unit>1000000000L)
            return "단가 또는 수량이 허용 범위를 벗어났습니다";
        if(i.count<0 && (i.printedTotal>0 || !i.amountBased && i.unit<0))
            return "음수 수량과 단가·행금액의 부호를 확인하세요";
        if (!i.quantityKnown) return "수량 미인식: 인쇄 행 금액 적용, 원본 확인 필요"
            +(i.warning.isEmpty() || i.warning.startsWith("수량 미인식:")?"":" / "+i.warning);
        if (!i.warning.isEmpty()) return i.warning;
        if (!i.amountBased && i.unit*i.count!=i.printedTotal) return "단가 × 수량과 인쇄 금액이 다릅니다";
        return "";
    }
}
