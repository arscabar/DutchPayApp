package kr.dutchpay;

import android.graphics.Rect;
import java.util.*;
import java.util.regex.*;

final class ColumnRows {
    final List<OcrWord> words=new ArrayList<>();
    float y(){return words.get(0).y();}
    int height(){return words.get(0).box.height();}
    String text(){return String.join(" ",words.stream().map(w->w.text).toArray(String[]::new));}
    static List<ColumnRows> group(List<OcrWord> input){
        List<OcrWord> sorted=new ArrayList<>(input);
        sorted.sort(Comparator.comparingDouble(OcrWord::y));
        List<ColumnRows> rows=new ArrayList<>();
        for(OcrWord w:sorted){
            ColumnRows row=rows.isEmpty()?null:rows.get(rows.size()-1);
            OcrWord anchor=row==null?null:row.words.get(0);
            if(row!=null && quantity(w)!=null)for(OcrWord member:row.words)
                if(quantity(member)!=null){anchor=member;break;}
            if(row==null || Math.abs(anchor.y()-w.y())>Math.min(anchor.box.height(),w.box.height())*.45){
                row=new ColumnRows(); rows.add(row);
            }
            row.words.add(w);
        }
        for(ColumnRows r:rows)r.words.sort(Comparator.comparingDouble(OcrWord::x));
        return rows;
    }
    static Long number(OcrWord w){
        String s=w.text.replaceAll("[()₩#￦원\\s]","");
        return new OcrWord(s,w.box).number();
    }
    static Long quantity(OcrWord w){
        String s=w.text.trim();
        if(s.matches("[-−]?[0-9]{1,4}\\s*개"))
            return Long.parseLong(s.replace("개","").replaceAll("\\s","").replace('−','-'));
        return number(w);
    }
    static String compact(String s){return s.replaceAll("\\s","");}
    static List<OcrWord> align(List<OcrWord> words,ColumnLayout c,float top,float p,float q,float a){
        List<OcrWord> out=new ArrayList<>(),anchors=new ArrayList<>();float end=Float.MAX_VALUE;
        for(ColumnRows row:group(words))if(row.y()>top && footer(row.text())){end=row.y();break;}
        for(OcrWord w:words)if(w.y()>top && w.y()<end && number(w)!=null && c.column(w.x())==0)anchors.add(w);
        List<OcrWord> quantities=new ArrayList<>();
        for(OcrWord w:words)if(w.y()>top && w.y()<end && quantity(w)!=null && c.column(w.x())==1)quantities.add(w);
        Map<OcrWord,OcrWord> paired=NumericAlignment.match(anchors,quantities,p-q);
        for(OcrWord w:words){
            Rect box=new Rect(w.box);
            if(w.y()>top && (number(w)!=null || c.column(w.x())==1 && quantity(w)!=null)){
                int column=c.column(w.x());float shift=column==2?p-a:column==1?p-q:0;
                if(Math.abs(shift)>w.box.height()*.3){
                    float y=w.y()+shift;OcrWord near=column==1?paired.get(w):null;
                    if(near==null && w.y()<end)for(OcrWord anchor:anchors)
                        if(Math.abs(anchor.y()-y)<Math.max(anchor.box.height(),w.box.height())*.65
                            && (near==null || Math.abs(anchor.y()-y)<Math.abs(near.y()-y)))near=anchor;
                    box.offset(0,Math.round((near==null?y:near.y())-w.y()));
                }
            }
            out.add(new OcrWord(w.text,box));
        }
        return out;
    }
    static boolean footer(String s){
        return compact(s).matches(".*(합계|함계|소계|총액|총주문|총구매|과세|부가세|공급가|신용|카드|결제|받을금액).*" )
            || compact(s).matches("(판매액|판매금액)[:：]?[0-9,.원₩￦−-]*")
            || compact(s).matches("계[:：]+.*");
    }
    static Set<Long> discounts(List<OcrWord> words,ColumnLayout c){
        Set<Long> values=new HashSet<>();boolean ended=false;
        for(ColumnRows row:group(words)){
            String s=compact(row.text());
            if(ended && s.matches(".*(신용|카드|승인|회원정보|회원번호|회원명|적립내역|모바일정보).*"))break;
            if(footer(s))ended=true;
            if(!ended || !s.contains("할인"))continue;
            Long value=c.value(row,c.amount);OcrWord near=null;
            if(value==null)for(OcrWord w:words)if(number(w)!=null && c.column(w.x())==2
                && Math.abs(w.y()-row.y())<Math.max(row.height(),w.box.height())*.9
                && (near==null || Math.abs(w.y()-row.y())<Math.abs(near.y()-row.y())))near=w;
            if(value==null && near!=null)value=number(near);
            if(value!=null && value!=0)values.add(Math.abs(value));
        }
        return values;
    }
    String name(float right){return name(right,null);}
    static boolean barcode(String text){return compact(text).matches("\\*?(?:[0-9]{8}|[0-9]{12,14})");}
    boolean barcode(ColumnLayout c){
        return words.stream().anyMatch(w->w.x()<c.nameRight() && barcode(w.text));
    }
    boolean nameAfterAmount(ColumnLayout c){
        OcrWord amount=null;
        for(OcrWord w:words)if(c.column(w.x())==2 && number(w)!=null){
            if(amount!=null)return false;amount=w;
        }
        if(amount==null)return false;
        for(OcrWord w:words)if(w.x()<c.nameRight() && number(w)==null && quantity(w)==null && !barcode(w.text)
            && w.y()<=amount.y())return false;
        return true;
    }
    String name(ColumnLayout layout){return name(layout.nameRight(),layout);}
    private String name(float right,ColumnLayout layout){
        List<String> parts=new ArrayList<>();
        for(OcrWord w:words)if(w.x()<right && number(w)==null){
            if(layout!=null && layout.column(w.x())==1 && quantity(w)!=null)continue;
            String s=w.text.trim();
            if(s.matches("[\\s\\p{P}\\p{S}]+"))continue;
            if(barcode(s) || s.matches("[A-Za-z]*[0-9]{5,}"))continue;
            s=s.replaceFirst("^0[0-9]+\\s+","");
            if(!s.isEmpty())parts.add(s);
        }
        return String.join(" ",parts);
    }
    static List<OcrWord> headers(ColumnRows row){
        List<OcrWord> out=new ArrayList<>();
        Pattern labels=Pattern.compile("[단딘]\\s*가|수\\s*[량랑람당]|매\\s*수|[금급]\\s*액");
        for(OcrWord w:row.words){
            Matcher m=labels.matcher(w.text);
            while(m.find()){
                int x=w.box.left+w.box.width()*m.start()/w.text.length();
                int end=w.box.left+w.box.width()*m.end()/w.text.length();
                String label=compact(m.group()).replace("딘가","단가").replace("급액","금액").replace("매수","수량");
                out.add(new OcrWord(label,new Rect(x,w.box.top,end,w.box.bottom)));
            }
        }
        return out;
    }
}
