package kr.dutchpay;

import java.util.*;

public final class Parser {
    public static Receipt parse(List<String> rows) {
        Receipt r = new Receipt(); r.raw = String.join("\n", rows);
        r.total=ReceiptTotalText.read(r.raw);
        boolean active = false; ItemParser items = new ItemParser(r);
        for (String source : rows) {
            String s = Numbers.clean(source);
            s = s.replaceFirst("^\\s*\\d{1,6}\\s+(?=[가-힣A-Za-z])", "");
            String compact = s.replaceAll("\\s", "");
            List<Long> ns = Numbers.values(s);
            if (compact.matches(".*(합계|총액|총주문금액|총구매액|결[제재]금액|실결제금액|신용카드|카드결제|현금결제|받은금액).*")) {
                items.flush(); active = false; continue;
            }
            if (compact.matches("(소계|계)[:：]*[-−]?[0-9,]*원?")) {
                items.flush(); active=false; continue;
            }
            if (compact.matches(".*(상품명|삼품명|품명|티켓명|P[O0]S[-:]?0?[1-9](?:[-:]\\d+|(?!\\d))|대기번호).*")
                || compact.matches("(상품|메뉴|티켓|품목).*(수[량랑람당]|매수).*금액")
                || compact.matches("^(매장식사|포장주문)$")) {
                items.flush(); active = true; continue;
            }
            if (compact.startsWith("▶") || compact.startsWith("►")) {
                active = true;
            }
            if (compact.matches(".*(부가세|과세|공급가|VAT|결[제재]구분|신용승인|카드종류|카드번호|받을금액).*")) {
                items.flush(); active = false; continue;
            }
            if (!active || compact.matches("[-=_*]+") || compact.isEmpty()) continue;
            // ponytail: bounded receipt heuristic; uncertain layouts remain editable.
            items.accept(s,ns);
        }
        items.flush(); r.validate(); return r;
    }
}
