package kr.dutchpay;

import java.util.*;

final class ReceiptChoice {
    static Receipt parse(List<OcrWord> words,String raw) {
        Receipt rows=Parser.parse(Arrays.asList(raw.split("\n")));
        ReceiptTotals.apply(rows,words,raw);
        Receipt columns=ColumnReceipt.parse(words,raw);
        if(columns!=null && !columns.items.isEmpty()) {
            ReceiptTotals.apply(columns,words,raw);
            columns.method="coordinates";return columns;
        }
        Receipt table=TableParser.parseWords(words,raw);
        if(table!=null && rows.total!=null && table.itemSum()==rows.total) {
            ReceiptTotals.apply(table,words,raw);table.method="legacy columns";return table;
        }
        rows.method="rows";return rows;
    }
    static boolean balanced(Receipt r) {
        try{return r.total!=null && !r.items.isEmpty() && r.total==r.itemSum();}
        catch(ArithmeticException ex){return false;}
    }
}
