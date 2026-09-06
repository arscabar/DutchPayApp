package kr.dutchpay;

import java.util.*;

final class ColumnLayout {
    final List<ColumnRows> rows;
    int start; float quantity,amount; Float price;
    ColumnLayout(List<ColumnRows> rows){this.rows=rows;}
    static ColumnLayout find(List<OcrWord> words){
        ColumnLayout c=new ColumnLayout(ColumnRows.group(words));
        int pos=-1;
        for(int i=0;i<c.rows.size();i++){
            ColumnRows row=c.rows.get(i); OcrWord q=null,a=null,p=null;int last=i;
            if(ColumnRows.compact(row.text()).matches(".*P[O0]S[-:]?0?1(?!\\d).*"))pos=i;
            if(ColumnRows.headers(row).isEmpty())continue;
            for(int j=i;j<Math.min(i+3,c.rows.size());j++){
                ColumnRows near=c.rows.get(j);
                if(near.y()-row.y()>row.height()*2)break;
                for(OcrWord w:ColumnRows.headers(near)){
                    if(w.text.equals("단가"))p=w;
                    if(w.text.startsWith("수"))q=w;
                    if(w.text.equals("금액"))a=w;
                    last=j;
                }
            }
            if(q!=null && a!=null && a.x()>q.x()+row.height()){
                c.quantity=q.x();c.amount=a.x();c.price=p==null?null:p.x();c.start=last+1;
                if(p!=null && p.x()<q.x()){
                    float bottom=Math.max(p.y(),Math.max(q.y(),a.y()))+Math.min(p.box.height(),q.box.height())*.4f;
                    c.rows.clear();c.rows.addAll(ColumnRows.group(ColumnRows.align(words,c,bottom,p.y(),q.y(),a.y())));
                    c.start=0;while(c.start<c.rows.size() && c.rows.get(c.start).y()<=bottom)c.start++;
                }else if(p==null){
                    float bottom=Math.max(q.y(),a.y())+Math.min(q.box.height(),a.box.height())*.4f;
                    List<OcrWord> aligned=QuantityRows.align(words,c,q,a);
                    if(aligned!=words){
                        c.rows.clear();c.rows.addAll(ColumnRows.group(aligned));
                        c.start=0;while(c.start<c.rows.size() && c.rows.get(c.start).y()<=bottom)c.start++;
                    }
                }
                return c;
            }
        }
        if(pos<0)return null;
        // ponytail: repeated quantity/amount columns only; unsupported layouts fall back.
        List<float[]> pairs=new ArrayList<>();
        for(int i=pos+1;i<c.rows.size();i++){
            ColumnRows row=c.rows.get(i);if(ColumnRows.footer(row.text()))break;
            for(OcrWord q:row.words){
                Long n=ColumnRows.number(q);if(n==null || n<1 || n>999)continue;
                for(OcrWord a:row.words){
                    Long v=ColumnRows.number(a);
                    if(a.x()>q.x()+row.height() && v!=null && Math.abs(v)>=100)
                        pairs.add(new float[]{q.x(),a.x(),row.height()});
                }
            }
        }
        for(float[] p:pairs){
            long count=pairs.stream().filter(v->Math.abs(v[0]-p[0])<p[2]
                && Math.abs(v[1]-p[1])<p[2]).count();
            if(count>=2){c.quantity=p[0];c.amount=p[1];c.start=pos+1;return c;}
        }
        return null;
    }
    Long value(ColumnRows row,float x){
        return value(row,x,false);
    }
    Long quantityValue(ColumnRows row){return value(row,quantity,true);}
    private Long value(ColumnRows row,float x,boolean quantityOnly){
        OcrWord best=null;
        for(OcrWord w:row.words)if((quantityOnly?ColumnRows.quantity(w):ColumnRows.number(w))!=null
            && column(w.x())==column(x)
            && (best==null || Math.abs(w.x()-x)<Math.abs(best.x()-x)))best=w;
        return best==null?null:quantityOnly?ColumnRows.quantity(best):ColumnRows.number(best);
    }
    int column(float x){
        if(x>(quantity+amount)/2)return 2;
        float left=price==null?quantity-(amount-quantity)*.6f:(price+quantity)/2;
        if(x>left)return 1;
        return price!=null && x>price-(quantity-price)?0:-1;
    }
    Long unlabelledUnit(ColumnRows row,long quantity,long amount){
        if(price!=null || quantity==0)return null;
        Long found=null;
        for(OcrWord w:row.words){
            Long value=ColumnRows.number(w);
            if(w.box.right>=this.quantity || value==null || value<0 || value>Math.abs(amount))continue;
            // Validate a printed number; never manufacture a unit price by division.
            if(value<=Long.MAX_VALUE/Math.abs(quantity) && value*quantity==amount){
                if(found!=null)return null;found=value;
            }
        }
        return found;
    }
    float nameRight(){return price==null?quantity-20:(price+quantity)/2;}
}
