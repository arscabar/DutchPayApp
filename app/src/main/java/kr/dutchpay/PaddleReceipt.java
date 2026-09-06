package kr.dutchpay;

import java.util.*;
import org.json.*;

final class PaddleReceipt {
    static Receipt receipt(List<PaddleLine> lines) {
        List<String> rows=PaddleLine.rows(lines);
        List<OcrWord> words=new ArrayList<>();for(var l:lines)words.add(l.word);
        List<Double> slopes=new ArrayList<>();
        for(var l:lines){var a=l.points.get(0);var b=l.points.get(1);
            if(b.x-a.x>80)slopes.add((double)(b.y-a.y)/(b.x-a.x));}
        Collections.sort(slopes);
        words=WordGeometry.flatten(words,slopes.isEmpty()?0:slopes.get(slopes.size()/2));
        return ReceiptChoice.parse(words,String.join("\n",rows));
    }
    static JSONObject parse(List<PaddleLine> lines) throws Exception {
        Receipt r=receipt(lines);
        JSONArray items=new JSONArray(), raw=new JSONArray();
        for(var l:lines)raw.put(l.json());
        for(Item i:r.items)items.put(new JSONObject().put("name",i.name).put("unit",i.unit)
            .put("count",i.count).put("quantityKnown",i.quantityKnown).put("printedTotal",i.printedTotal).put("warning",i.warning)
            .put("amountBased",i.amountBased).put("includedDiscount",i.includedDiscount).put("baseTotal",i.baseAmount()));
        return new JSONObject().put("lines",raw).put("rows",r.raw).put("items",items)
            .put("total",r.total).put("warnings",new JSONArray(r.warnings))
            .put("method",r.method);
    }
}
