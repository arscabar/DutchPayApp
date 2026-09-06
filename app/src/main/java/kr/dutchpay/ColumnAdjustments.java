package kr.dutchpay;

import java.util.regex.*;

final class ColumnAdjustments {
    private static final String PRICE="([0-9]{1,9}|[0-9]{1,3}(?:,[0-9]{3}){1,2})원";
    private static boolean reduction(String text,long amount){
        Matcher m=Pattern.compile("[^0-9→]*[가-힣][^0-9→]*"+PRICE+"→"+PRICE).matcher(text);
        if(!m.matches())return false;
        long before=Long.parseLong(m.group(1).replace(",","")),after=Long.parseLong(m.group(2).replace(",",""));
        return before>after && before-after==Math.abs(amount);
    }
    static void apply(Receipt r,ColumnLayout c){
        Long sales=null,value=null;String label=null;boolean ambiguous=false;
        for(int k=c.start;k<c.rows.size();k++){
            ColumnRows row=c.rows.get(k);String name=row.name(c);
            String text=ColumnRows.compact(name);
            Long amount=c.value(row,c.amount),quantity=c.quantityValue(row);
            if(sales==null){
                if(text.matches("판매액|판매금액") && amount!=null && quantity==null
                    && amount>=0 && amount<=1000000000L)sales=amount;
                continue;
            }
            if(text.matches("합계|함계|총결제금액|총구매액|결제금액|받을금액")){
                if(label!=null && !ambiguous && amount!=null && sales-Math.abs(value)==amount){
                    Item discount=new Item(label,-Math.abs(value),1,value);
                    discount.warning="판매액·합계 사이의 인쇄 조정 금액을 차감했습니다. 원본 확인 필요";
                    r.items.add(discount);
                }else if(label!=null || ambiguous)
                    r.warnings.add("판매액과 합계 사이의 조정 금액·부호 확인 필요: 자동 차감 보류");
                return;
            }
            if(ColumnRows.footer(row.text()) && !text.matches(".*(할인|쿠폰).*"))break;
            if(amount==null)continue;
            boolean coupon=text.contains("→")?reduction(text,amount):text.matches(".*(할인|쿠폰).*|\\[[^\\]]{1,40}\\].+");
            if(quantity!=null || amount==0 || amount< -1000000000L || amount>1000000000L
                || !coupon || label!=null)ambiguous=true;
            else {label=name;value=amount;}
        }
        if(label!=null)r.warnings.add("정산 구간의 조정 금액은 인쇄 합계 확인 전까지 차감하지 않았습니다");
    }
}
