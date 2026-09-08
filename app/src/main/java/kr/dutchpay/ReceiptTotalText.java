package kr.dutchpay;

import java.util.*;
import java.util.regex.*;

final class ReceiptTotalText {
    static final String LABEL="실결제금액|총결제금액|결제금액|받을금액|받은금액|청구금액|합계금액|합계|함계|총구매액|총금액|승인금액|카드결제금액|신용카드|카드결제|현금결제|현금|상품권|지역화폐|계좌이체|간편결제|카드|금액|매출합계|매출합계카드|계";
    static final Pattern MONEY=Pattern.compile("^("+LABEL+")(-?[0-9][0-9,.]*)(?:원)?(?:\\|([0-9][0-9,.]*원?))?");
    static final class Candidate {
        final long value; final int rank;
        Candidate(long value,int rank){this.value=value;this.rank=rank;}
    }
    static String clean(String s){return Numbers.clean(s).replaceAll("(?<=\\d)\\s+(?=-?\\d)","|")
        .replaceAll("[\\s\\[\\](){}:*#₩￦]","");}
    static List<Candidate> candidates(String raw){
        List<Candidate> out=new ArrayList<>(), approvals=new ArrayList<>(), payments=new ArrayList<>(),received=new ArrayList<>();
        Set<Long> discounts=new HashSet<>();Set<String> methods=new HashSet<>();Long change=null;
        String[] rows=raw.split("\n");boolean card=false,approval=false,loyalty=false,numberSeen=false,split=false;
        int blocks=0,tax=-100;
        for(int i=0;i<rows.length;i++){
            String s=clean(rows[i]);
            if(s.matches(".*(상품명|메뉴명).*")){approval=false;numberSeen=false;}
            if(s.contains("카드전표") || s.contains("카드매출전표"))card=true;
            if(s.contains("신용승인") || s.contains("신용숭인") || s.contains("카드매출전표")){
                blocks++;approval=true;card=true;numberSeen=false;
            }else if(s.startsWith("카드번")){
                if(!approval || numberSeen)blocks++;
                approval=true;card=true;numberSeen=true;
            }else if(s.startsWith("카드회사") || s.startsWith("승인번호")){
                if(!approval)blocks++;approval=true;card=true;
            }
            if(s.matches(".*(모바일정보|포인트정보|적립정보).*"))loyalty=true;
            if(s.contains("부가세"))tax=i;
            if(s.matches(".*(분할결제|복합결제).*"))split=true;
            Long d=tail(s,".*할인(?:금액)?"),cash=tail(s,"거스름돈");
            if(!approval && d!=null && d!=0 && !s.matches(".*(상품명|수량|단가).*"))discounts.add(Math.abs(d));
            if(!approval && cash!=null && cash>=0)change=cash;
            if(loyalty)continue;
            s=s.replaceFirst("^[0-9]+[.]카드결제","카드결제");
            Matcher m=MONEY.matcher(s);if(!m.find())continue;
            String amount=m.group(2);
            if(m.group(3)!=null){
                if(!m.group(1).matches("합계금액|합계|함계|총구매액|총금액") || !amount.matches("[0-9]{1,4}")
                    || !m.group(3).matches(".*[,원].*"))continue;
                amount=m.group(3).replace("원","");
            }
            if(s.substring(m.end()).startsWith("|"))continue;
            long value;try{value=Long.parseLong(amount.replaceAll("[,.]",""));}
            catch(NumberFormatException e){continue;}
            if(value<0 || value>1000000000L)continue;
            String label=m.group(1);
            if(label.equals("금액") || label.equals("승인금액")){
                if(card || label.equals("승인금액"))approvals.add(new Candidate(value,1));
            }else if(label.matches("(신용카드|카드|카드결제|카드결제금액|현금결제|현금|상품권|지역화폐|계좌이체|간편결제)")){
                (approval?approvals:payments).add(new Candidate(value,3));
                if(!approval)methods.add(label.contains("카드")?"카드":label.replace("결제",""));
            }else if(label.equals("받은금액")){
                (approval?approvals:received).add(new Candidate(value,1));
            }else if(label.equals("계")){
                if(i-tax<=4)out.add(new Candidate(value,2));
            }else if(approval)approvals.add(new Candidate(value,1));
            else out.add(new Candidate(value,label.matches(".*(결제|받을|받은|청구).*")?5:4));
        }
        Long payment=choose(payments);
        boolean single=payment!=null && methods.size()==1 && blocks<=1 && !split;
        for(Candidate c:received){
            if(change!=null && change>0)continue;
            boolean supported=Long.valueOf(0).equals(change) || single && c.value==payment;
            for(Candidate gross:out)if(gross.rank==4 && discounts.contains(gross.value-c.value))supported=true;
            if(supported)out.add(new Candidate(c.value,5));
        }
        for(Candidate c:payments)out.add(new Candidate(c.value,!discounts.isEmpty() && single?5:3));
        if(blocks<=1)for(Candidate c:approvals)out.add(new Candidate(c.value,1));
        return out;
    }
    static Long tail(String s,String label){
        Matcher m=Pattern.compile("^"+label+"(-?[0-9][0-9,]*)(?:원)?$").matcher(s);
        if(!m.matches())return null;
        try{long n=Long.parseLong(m.group(1).replace(",",""));return n>=-1000000000L && n<=1000000000L?n:null;}
        catch(NumberFormatException e){return null;}
    }
    static Long choose(List<Candidate> values){
        int rank=0;Set<Long> best=new HashSet<>();
        for(Candidate c:values){if(c.rank>rank){rank=c.rank;best.clear();}if(c.rank==rank)best.add(c.value);}
        return best.size()==1?best.iterator().next():null;
    }
    static Long read(String raw){return choose(candidates(raw));}
}
