package kr.dutchpay;

import java.util.List;

final class ItemParser {
    final Receipt receipt;
    String pending = "";
    boolean pendingBullet = false;
    Long pendingTotal, discount;
    ItemParser(Receipt receipt) { this.receipt = receipt; }
    void flush() {
        if (!pending.isEmpty() && pendingTotal != null) {
            Item i = new Item(pending,pendingTotal,1,pendingTotal);
            i.warning = "수량을 읽지 못해 1로 표시했습니다"; receipt.items.add(i);
        }
        pending = ""; pendingBullet = false; pendingTotal = null; discount = null;
    }
    void accept(String s, List<Long> ns) {
        String trimmed = s.trim();
        boolean isBullet = trimmed.startsWith("▶") || trimmed.startsWith("►");
        boolean isSubOption = trimmed.startsWith("└") || trimmed.startsWith("ㄴ");
        if (pendingBullet && pendingTotal == null && !isBullet) {
            // Sub-option line (e.g. └비프 스테이크 ... 9,900 or └레몬갈릭소스 0) or single-char line wrap (e.g. "가")
            // before the main ▶ item's "24,800 x 1 24,800" summary line
            if (isSubOption && !trimmed.contains("할인") && ns.stream().noneMatch(n -> n < 0)) {
                return;
            }
            if (ns.isEmpty() && trimmed.length() <= 2) {
                return;
            }
        }
        String name = Numbers.name(s).replaceFirst("^[▶►]+\\s*", "").trim();
        boolean named = name.matches(".*[가-힣a-zA-Z].*");
        if (named) flush(); else name = pending;
        if (name.isEmpty()) return;
        if (ns.isEmpty()) { pending = name; pendingBullet = isBullet; return; }
        long total=ns.get(ns.size()-1), unit=total; int count=1;
        if (name.contains("할인")) {
            long[] negatives=ns.stream().mapToLong(n->n).filter(n->n<0).distinct().toArray();
            long value=negatives.length==1?negatives[0]:negatives.length==0?-Math.abs(total):0;
            Item i=new Item(name,value,1,value);
            if(negatives.length>1)i.warning="서로 다른 할인액이 여러 개입니다. 0원으로 보류했으니 원본 확인 후 입력하세요";
            receipt.items.add(i); return;
        }
        if (named && ns.size()==1) {
            pending=name; pendingBullet=isBullet;
            if (total < 0) {
                discount=total;
                receipt.items.add(new Item("할인 · " + name,total,1,total));
            } else pendingTotal=total;
            return;
        }
        String warning="";
        if (ns.size()>=3 && ns.get(0)>=1000) {
            unit=ns.get(0); count=count(ns.get(1));
        } else if (ns.size()>=2 && ns.get(0)>0 && ns.get(0)<=999) {
            count=count(ns.get(0));
            long before=total-(discount==null?0:discount);
            unit=before/count;
            if (before%count!=0) warning="단가 나눗셈이 맞지 않습니다";
            if (discount!=null) warning="할인 전 단가를 역산했습니다. 원본 확인 필요";
            if (ns.size()>2) warning="수량 열에 추가 숫자가 있습니다. 확인 필요";
        } else if (ns.size()>=2) warning="단가·수량 구분 확인 필요";
        Item i=new Item(name,unit,count,total);
        if (!warning.isEmpty()) i.warning=warning;
        receipt.items.add(i); pending=""; pendingBullet=false; pendingTotal=null; discount=null;
    }
    private int count(long n) { return n>0 && n<=999 ? (int)n : 1; }
}
