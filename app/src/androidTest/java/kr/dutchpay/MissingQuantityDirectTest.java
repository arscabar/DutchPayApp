package kr.dutchpay;

import android.graphics.*;
import android.test.AndroidTestCase;

public class MissingQuantityDirectTest extends AndroidTestCase {
    public void testEmptyOrFlatCellHasNoNumericInk(){
        Bitmap b=Bitmap.createBitmap(100,60,Bitmap.Config.ARGB_8888);
        try{
            b.eraseColor(Color.WHITE);assertNull(MissingQuantityDirect.inkBox(b));
            Canvas canvas=new Canvas(b);Paint ink=new Paint();ink.setColor(Color.rgb(240,240,240));
            canvas.drawRect(40,10,48,52,ink);assertNull(MissingQuantityDirect.inkBox(b));
            b.eraseColor(Color.WHITE);ink.setColor(Color.BLACK);canvas.drawRect(5,28,95,31,ink);
            assertNull(MissingQuantityDirect.inkBox(b));
            b.eraseColor(Color.WHITE);canvas.drawRect(40,10,48,52,ink);
            assertNotNull(MissingQuantityDirect.inkBox(b));
        }finally{b.recycle();}
    }
    public void testTwoActualModelsMustAgreeAtHighConfidence(){
        var one=new KoreanModel.Reading("1",.998);
        assertTrue(NumericEnglish.agrees(one,new KoreanModel.Reading("1",.999)));
        assertFalse(NumericEnglish.agrees(one,new KoreanModel.Reading("2",.999)));
        assertFalse(NumericEnglish.agrees(one,new KoreanModel.Reading("1",.94)));
        assertFalse(NumericEnglish.agrees(one,new KoreanModel.Reading("I",.999)));
        assertFalse(NumericEnglish.agrees(one,new KoreanModel.Reading("",.999)));
    }
}
