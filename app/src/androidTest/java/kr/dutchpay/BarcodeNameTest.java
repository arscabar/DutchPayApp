package kr.dutchpay;

import android.graphics.Rect;
import android.test.AndroidTestCase;
import java.util.*;

public class BarcodeNameTest extends AndroidTestCase {
    private OcrWord w(String text,int x,int y){return new OcrWord(text,new Rect(x-20,y-10,x+20,y+10));}
    public void testQuantityRecoveryCannotReplaceItemNameWithTaxMarkedBarcode(){
        for(String code:new String[]{"12345678","*12345678","1234567890123","*1234567890123"}){
            OcrWord barcode=w(code,80,100);
            List<OcrWord> words=new ArrayList<>(Arrays.asList(w("수량",300,20),w("금액",450,20),
                w("종이 봉투 보증금 200원",80,60),barcode,w("200",450,100)));
            Item before=ColumnReceipt.parse(words,"").items.get(0);
            words.add(w("2",300,100));Item after=ColumnReceipt.parse(words,"").items.get(0);
            assertEquals("종이 봉투 보증금 200원",after.name);assertEquals(before.name,after.name);
            assertEquals(before.unit,after.unit);assertEquals(before.printedTotal,after.printedTotal);
            assertEquals(before.baseAmount(),after.baseAmount());assertEquals(2,after.count);
            assertTrue(after.quantityKnown);assertEquals(code,barcode.text);
        }
    }
    public void testNamesWithStarsNumbersOrUnitsAreNotBarcodes(){
        for(String name:new String[]{"*상품1234567890123","상품2입","보증금200원","*1234","비타민B12"}){
            assertFalse(ColumnRows.barcode(name));ColumnRows row=new ColumnRows();row.words.add(w(name,80,60));
            assertEquals(name,row.name(200));
        }
    }
}
