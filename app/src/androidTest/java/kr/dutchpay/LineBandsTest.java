package kr.dutchpay;

import android.graphics.*;
import android.test.InstrumentationTestCase;
import java.util.List;

public class LineBandsTest extends InstrumentationTestCase {
    public void testTallNumbersAndLabels() {
        Bitmap page=Bitmap.createBitmap(150,250,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(page); Paint ink=new Paint(); page.eraseColor(Color.WHITE);
        for(int i=0;i<9;i++)canvas.drawRect(15,15+i*24,16,29+i*24,ink);
        Point[] corners={new Point(5,10),new Point(29,10),new Point(29,230),new Point(5,230)};
        assertNull(TextPatch.create(page,corners));
        try(TextPatch patch=TextPatch.create(page,corners,true)) {
            assertNotNull(patch);assertEquals(1,LineBands.split(patch.image).size());
            List<Rect> bands=LineBands.split(patch.image,23);
            assertEquals(9,bands.size());assertEquals(220,bands.get(8).bottom);
            Rect mapped=patch.map(new Rect(10,0,13,bands.get(1).height()),bands.get(1).top);
            assertEquals(15,mapped.left);assertEquals(10+bands.get(1).top,mapped.top);
        }
        page.eraseColor(Color.WHITE);
        for(int i=0;i<9;i++) {
            canvas.drawRect(10,10+i*24,130,16+i*24,ink);
            canvas.drawRect(10,19+i*24,130,27+i*24,ink);
        }
        assertEquals(9,LineBands.split(page,23).size());
        assertFalse(page.isRecycled());page.recycle();
    }

    public void testPatchCoordinates() {
        Bitmap page=Bitmap.createBitmap(200,150,Bitmap.Config.ARGB_8888);
        page.eraseColor(Color.WHITE);
        TextPatch patch=TextPatch.create(page,new Point[]{new Point(20,30),
            new Point(140,30),new Point(140,90),new Point(20,90)});
        assertNotNull(patch);
        assertEquals(new Rect(30,65,50,75),patch.map(new Rect(10,5,30,15),30));
        float[] mappedPoint=patch.mapPoints(new float[]{10,5});
        assertEquals(30f,mappedPoint[0],.01f); assertEquals(35f,mappedPoint[1],.01f);
        assertTrue(patch.contains(new OcrWord("inside",new Rect(30,40,60,60))));
        assertFalse(patch.contains(new OcrWord("outside",new Rect(160,100,180,120))));
        patch.close(); assertFalse(page.isRecycled());
        patch=TextPatch.create(page,new Point[]{new Point(20,30),new Point(140,40),
            new Point(150,100),new Point(30,90)});
        assertNotNull(patch);
        Rect mapped=patch.map(new Rect(0,0,patch.image.getWidth(),patch.image.getHeight()),0);
        assertTrue(Math.abs(mapped.left-20)<=1 && Math.abs(mapped.top-30)<=1
            && Math.abs(mapped.right-150)<=1 && Math.abs(mapped.bottom-100)<=1);
        patch.close(); page.recycle();
    }

    public void testConservativeSplitting() {
        Bitmap image=Bitmap.createBitmap(200,100,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image); Paint ink=new Paint();
        ink.setColor(Color.BLACK); canvas.drawColor(Color.WHITE);
        assertEquals(1,LineBands.split(image).size());
        // Disconnected components mimic a Hangul syllable, not separate lines.
        canvas.drawRect(12,10,188,24,ink); canvas.drawRect(12,28,188,44,ink);
        assertEquals(1,LineBands.split(image).size());
        canvas.drawRect(12,60,188,74,ink); canvas.drawRect(12,78,188,94,ink);
        int[] before=new int[20000]; image.getPixels(before,0,200,0,0,200,100);
        List<Rect> result=LineBands.split(image);
        assertEquals(2,result.size());
        assertEquals(new Rect(0,0,200,52),result.get(0));
        assertEquals(new Rect(0,52,200,100),result.get(1));
        int[] after=new int[20000]; image.getPixels(after,0,200,0,0,200,100);
        assertTrue(java.util.Arrays.equals(before,after));
        canvas.drawColor(Color.WHITE); canvas.drawRect(12,10,188,44,ink);
        canvas.drawRect(12,70,188,72,ink); // A rule is not another text line.
        assertEquals(1,LineBands.split(image).size());
        canvas.drawColor(Color.BLACK);
        assertEquals(1,LineBands.split(image).size()); image.recycle();
    }
}
