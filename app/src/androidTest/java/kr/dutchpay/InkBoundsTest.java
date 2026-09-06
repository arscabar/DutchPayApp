package kr.dutchpay;

import android.graphics.*;
import android.test.InstrumentationTestCase;

public class InkBoundsTest extends InstrumentationTestCase {
    public void testTrimPreservesInkAndInput() {
        Bitmap image=Bitmap.createBitmap(100,60,Bitmap.Config.ARGB_8888);
        image.eraseColor(Color.WHITE);
        assertEquals(new Rect(0,0,100,60),InkBounds.trim(image));
        Canvas canvas=new Canvas(image); Paint ink=new Paint();
        ink.setColor(Color.DKGRAY); canvas.drawRect(20,15,80,45,ink);
        assertEquals(new Rect(18,13,82,47),InkBounds.trim(image));
        image.setPixel(0,30,Color.DKGRAY); // Preserve a detached dot or minus.
        assertEquals(new Rect(0,13,82,47),InkBounds.trim(image));
        assertEquals(Color.WHITE,image.getPixel(10,10));
        assertEquals(Color.DKGRAY,image.getPixel(0,30));
        image.eraseColor(Color.BLACK);
        assertEquals(new Rect(0,0,100,60),InkBounds.trim(image)); image.recycle();
    }
}
