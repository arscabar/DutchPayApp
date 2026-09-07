package kr.dutchpay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Item {
    public String name;
    public String originalName = "";
    public android.graphics.Bitmap crop;
    public final List<String> nameCandidates = new ArrayList<>();
    public long unit, printedTotal;
    public int count;
    public boolean amountBased, includedDiscount;
    public boolean quantityKnown = true;
    public String warning = "";
    public Item(String name, long unit, int count, long total) {
        this.name = name; this.unit = unit; this.count = count; printedTotal = total;
        if (unit * count != total) warning = "단가 × 수량과 인쇄 금액이 다릅니다";
    }
    public static Item line(String name, Long unit, Integer count, long total) {
        if(count!=null && (count==0 || count< -9999 || count>9999
            || count<0 && (total>0 || unit!=null && unit<0)))throw new IllegalArgumentException("Invalid quantity");
        Item i=new Item(name,unit==null?total:unit,count==null?0:count,total);
        if(unit==null){i.amountBased=true;i.warning="";}
        i.quantityKnown=count!=null;
        if(!i.quantityKnown)i.warning="수량 미인식: 인쇄 행 금액 적용, 원본 확인 필요";
        return i;
    }
    public long baseAmount() {
        return includedDiscount?0:amountBased || !quantityKnown?printedTotal:Math.multiplyExact(unit,count);
    }
    public long portion(String quantity,String percent,long editedValue) {
        if(includedDiscount)return 0;
        if(!quantityKnown) {
            if(quantity.trim().isEmpty())return scaled(amountBased?editedValue:printedTotal,"1",percent,1);
            if(amountBased)throw new IllegalArgumentException("Original quantity is unknown");
        }
        if(count<0){
            if(amountBased?editedValue>0:editedValue<0)throw new IllegalArgumentException("Invalid removal amount");
            if(!amountBased)editedValue=Math.negateExact(editedValue);
        }
        return scaled(editedValue,quantity,percent,amountBased?Math.abs(count):1);
    }
    public static long amount(long unit, String quantity, String percent) {
        return scaled(unit,quantity,percent,1);
    }
    private static long scaled(long unit,String quantity,String percent,int denominator) {
        BigDecimal q = new BigDecimal(quantity), p = new BigDecimal(percent);
        if (q.signum() < 0 || q.compareTo(new BigDecimal("9999")) > 0
                || p.signum() < 0 || p.compareTo(new BigDecimal("100")) > 0
                || unit < -1000000000L || unit > 1000000000L || denominator<1) throw new IllegalArgumentException();
        return BigDecimal.valueOf(unit).multiply(q).multiply(p)
                .divide(BigDecimal.valueOf(100L*denominator), 0, RoundingMode.HALF_UP).longValueExact();
    }
}
