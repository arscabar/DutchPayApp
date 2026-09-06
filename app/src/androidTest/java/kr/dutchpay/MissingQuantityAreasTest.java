package kr.dutchpay;

import android.test.AndroidTestCase;
import android.graphics.*;
import java.util.*;

public class MissingQuantityAreasTest extends AndroidTestCase {
    private PaddleLine w(String text,int x,int y){return new PaddleLine(text,Arrays.asList(
        new PointF(x-10,y-10),new PointF(x+10,y-10),new PointF(x+10,y+10),new PointF(x-10,y+10)));}
    public void testOnlyMissingPositiveItemCells(){
        Bitmap page=Bitmap.createBitmap(500,300,Bitmap.Config.ARGB_8888);page.eraseColor(Color.WHITE);
        List<PaddleLine> lines=new ArrayList<>(Arrays.asList(w("수량",300,20),w("금액",450,20),
            w("상품",80,60),w("3000",450,60),w("할인",80,100),w("-300",450,100),w("합계",80,160),w("2700",450,160)));
        try{
            var missing=MissingQuantityAreas.find(page,lines);assertEquals(1,missing.size());
            assertTrue(missing.get(0).quantity.top<=50);assertTrue(missing.get(0).quantity.bottom>=70);
            lines.add(w("2",300,60));assertTrue(MissingQuantityAreas.find(page,lines).isEmpty());
            lines.remove(lines.size()-1);lines.removeIf(l->l.word.text.equals("합계"));
            assertTrue(MissingQuantityAreas.find(page,lines).isEmpty());
        }finally{page.recycle();}
    }
    public void testFooterBoxCannotClipThePrintedItemRow(){
        Bitmap page=Bitmap.createBitmap(500,300,Bitmap.Config.ARGB_8888);page.eraseColor(Color.WHITE);
        var amount=new PaddleLine("5000",Arrays.asList(new PointF(440,60),new PointF(460,60),new PointF(460,110),new PointF(440,110)));
        try{
            var areas=MissingQuantityAreas.find(page,Arrays.asList(w("수량",300,20),w("금액",450,20),
                w("상품",80,80),amount,w("합계",80,115),w("5000",450,115)));
            assertEquals(1,areas.size());assertTrue(areas.get(0).quantity.bottom>105);
            assertTrue(areas.get(0).context.bottom>=amount.word.box.bottom);
        }finally{page.recycle();}
    }
    public void testCenterBandAndReplacedFragment(){
        var digit=w("1",451,671);var neighbor=w("각",425,699);
        var selected=MissingQuantityMatch.centered(Arrays.asList(neighbor,digit),new Rect(410,639,476,708));
        assertEquals(Collections.singletonList(digit),selected);
        assertTrue(MissingQuantityMatch.replaced(w("'2",300,80),w("2",300,80)));
        assertFalse(MissingQuantityMatch.replaced(w("2000",300,80),w("2",300,80)));
        assertFalse(MissingQuantityMatch.replaced(w("상품",80,80),w("2",300,80)));
        var wide=Arrays.asList(new PointF(10,10),new PointF(120,10),new PointF(120,61),new PointF(10,61));
        var found=new PaddleLine("2",Arrays.asList(new PointF(88,17),new PointF(106,17),new PointF(106,56),new PointF(88,56)));
        assertTrue(MissingQuantityMatch.replaced(new PaddleLine("'2",wide),found));
        assertFalse(MissingQuantityMatch.replaced(new PaddleLine("상품2",wide),found));
        assertFalse(MissingQuantityMatch.replaced(new PaddleLine("'3",wide),found));
    }
}
