package kr.dutchpay;

import java.util.*;

final class ColumnReceipt {
    static Receipt parse(List<OcrWord> words,String raw){
        ColumnLayout c=ColumnLayout.find(words);if(c==null)return null;
        Receipt r=new Receipt();r.raw=raw;
        r.total=Parser.parse(Arrays.asList(raw.split("\\n"))).total;
        String pending=null; Long discount=null; boolean stopped=false,pendingCode=false;
        Set<Long> summary=c.price==null?Collections.emptySet():ColumnRows.discounts(words,c);
        for(int k=c.start;k<c.rows.size();k++){
            ColumnRows row=c.rows.get(k);String text=ColumnRows.compact(row.text());
            Long amount=c.value(row,c.amount),qty=c.quantityValue(row),unit=c.price==null?null:c.value(row,c.price);
            if(text.matches(".*(합계|함계|총구매액|총결제금액|받을금액).*" ) && amount!=null){
                if(r.total==null)r.total=amount;
            }
            if(ColumnRows.footer(text)){
                if(pending!=null)r.warnings.add("금액 미인식: "+pending);
                stopped=true;pending=null;pendingCode=false;
            }
            if(stopped)continue;
            String name=row.name(c);
            if(qty!=null && qty<0 && amount!=null){
                if(qty< -9999 || amount>0 || unit!=null && unit<0 || name.isEmpty())
                    r.warnings.add("음수 수량·금액 행 확인: "+row.text());
                else {
                    Item removed=Item.line(name,unit,qty.intValue(),amount);
                    removed.warning="원본 음수 수량: 구성품 제거 또는 반품 여부 확인";
                    r.items.add(removed);
                }
                if(pending!=null)r.warnings.add("연결되지 않은 앞 품목 확인: "+pending);
                pending=null;discount=null;pendingCode=false;continue;
            }
            boolean explicit=text.matches(".*(할인|멤버십).*" );
            List<Long> ns=Numbers.values(row.text());
            List<Long> negatives=new ArrayList<>();for(long n:ns)if(n<0)negatives.add(n);
            if(explicit && qty==null && negatives.size()>1){r.warnings.add("할인 금액 후보가 여러 개입니다. 원본 확인 필요");continue;}
            Long negative=negatives.isEmpty()?null:negatives.get(0);
            if(explicit && qty==null && negative!=null){
                r.items.add(new Item(name.isEmpty()?"할인":name,negative,1,negative));continue;
            }
            if(amount!=null && amount<0 && qty==null && !name.isEmpty() && c.price==null){
                pending=name;discount=amount;pendingCode=false;continue;
            }
            if(amount==null){
                if(!name.isEmpty()){pending=name;pendingCode=false;}
                if(pending!=null && c.price==null && row.barcode(c))pendingCode=true;
                continue;
            }
            if(qty==null && pending==null && unit==null){
                if(!name.isEmpty())r.warnings.add("수량 없는 금액 행 확인: "+name+" / "+amount+"원");
                continue;
            }
            String next=null;
            boolean recoveredUnit=false;
            if(c.price==null && pending!=null && name.isEmpty() && qty!=null){
                unit=c.unlabelledUnit(row,qty,amount);recoveredUnit=unit!=null;
            }
            if(pending!=null && !name.isEmpty() && (qty==null || c.price!=null || pendingCode && row.nameAfterAmount(c))){
                next=name;name=pending;
            }else if(name.isEmpty())name=pending;
            if(name==null || name.isEmpty())continue;
            if(qty!=null && (qty<1 || qty>9999)){
                r.warnings.add("수량 범위 확인: "+name);pending=null;discount=null;pendingCode=false;continue;
            }
            Integer count=qty==null?null:qty.intValue();
            Item item=Item.line(name,unit,count,amount);
            if(recoveredUnit)item.warning="이름 아래의 인쇄 가격을 단가로 연결했습니다. 원본 확인 필요";
            r.items.add(item);
            if(discount!=null){
                Item d=new Item("할인 · "+name,discount,1,discount);d.includedDiscount=true;
                d.warning="행 금액에 반영된 할인으로 분류했습니다. 원본 확인 필요";r.items.add(d);
            }
            pending=next;discount=null;pendingCode=false;
        }
        if(r.items.isEmpty())return null;
        ColumnAdjustments.apply(r,c);
        if(summary.size()==1){
            long value=summary.iterator().next(),existing=0,gross=0,net=0;
            for(Item i:r.items){
                if(i.baseAmount()<0){if(i.count>0)existing-=i.baseAmount();}
                else {gross+=i.baseAmount();net+=i.printedTotal;}
            }
            if(existing==0 && r.items.stream().anyMatch(i -> !i.quantityKnown)){
                r.warnings.add("전체 할인 "+value+"원: 수량 미인식 행에 이미 반영됐는지 확인 필요, 추가 차감 보류");
            }else if(existing==0){
                r.items.add(new Item("전체 할인",-value,1,-value));
                if(gross-net==value)for(Item i:r.items)if(i.baseAmount()>0 && i.baseAmount()!=i.printedTotal)
                    i.warning+=(i.warning.isEmpty()?"":" / ")+"단가 × 수량은 할인 전 금액, 인쇄 행 금액은 할인 후 금액입니다";
            }else if(existing!=value)r.warnings.add("개별 할인과 전체 할인 요약이 다릅니다. 중복 차감하지 않았습니다");
        }else if(summary.size()>1)r.warnings.add("전체 할인 금액 후보가 여러 개입니다. 원본 확인 필요");
        r.method="좌표 열 연결";r.validate();return r;
    }
}
