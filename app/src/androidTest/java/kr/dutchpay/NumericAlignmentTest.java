package kr.dutchpay;

import android.graphics.*;
import android.test.AndroidTestCase;
import java.util.*;

public class NumericAlignmentTest extends AndroidTestCase {
    private OcrWord box(String text,int x,int y,int height){return new OcrWord(text,new Rect(x,y-height/2,x+8,y+height/2));}
    public void testOrderedMatchesDoNotStealFollowingQuantity(){
        List<OcrWord> prices=Arrays.asList(box("0",480,544,16),box("0",480,562,18),box("8900",460,583,24));
        List<OcrWord> qty=Arrays.asList(box("1",530,540,14),box("1",530,558,14),box("1",530,577,14));
        var match=NumericAlignment.match(prices,qty,14);
        assertEquals(3,match.size());for(int i=0;i<3;i++)assertSame(prices.get(i),match.get(qty.get(i)));
        OcrWord far=box("9",530,900,14);
        assertTrue(NumericAlignment.match(prices,Collections.singletonList(far),14).isEmpty());
    }
    public void testNarrowPassCannotCombineExistingQuantities(){
        var a=line("1",90,110);var b=line("1",110,130);List<PaddleLine> source=Arrays.asList(a,b);
        Rect strip=new Rect(490,80,550,200);
        assertSame(source,QuantityRecovery.fill(source,Collections.singletonList(line("11",95,125)),strip,520,20));
        var missing=line("2",150,170);
        var result=QuantityRecovery.fill(source,Collections.singletonList(missing),strip,520,20);
        assertEquals(3,result.size());assertTrue(result.contains(a));assertTrue(result.contains(b));assertTrue(result.contains(missing));
    }
    public void testStripRejectsInvalidOrOversizedRegion() throws Exception {
        Bitmap page=Bitmap.createBitmap(1000,1000,Bitmap.Config.ARGB_8888);
        try{
            for(Rect box:Arrays.asList(new Rect(),new Rect(-1,0,9,9),new Rect(0,0,1001,1),new Rect(0,0,1000,1000)))
                assertTrue(NumericStrip.read(page,box).isEmpty());
        }finally{page.recycle();}
    }
    public void testIndependentQuantityAgreement(){
        assertTrue(NumericEnglish.canCompare(""));assertTrue(NumericEnglish.canCompare("I"));
        assertFalse(NumericEnglish.canCompare("2"));assertFalse(NumericEnglish.canCompare("1 | 2"));
        var one=new KoreanModel.Reading("1",.99);var two=new KoreanModel.Reading("2",.99);
        assertTrue(NumericEnglish.candidate("2",one));assertFalse(NumericEnglish.candidate("1",one));
        assertFalse(NumericEnglish.candidate("",one));assertFalse(NumericEnglish.candidate("0",one));
        assertTrue(NumericEnglish.agrees(one,one));assertFalse(NumericEnglish.agrees(one,two));
        assertFalse(NumericEnglish.agrees(one,new KoreanModel.Reading("1",.94)));
        assertFalse(NumericEnglish.agrees(new KoreanModel.Reading("1",.94),one));
    }
    public void testQuantityRegionAmbiguityIsNotBlank(){
        var boxes=Arrays.asList(Collections.singletonList(new Rect(510,90,530,112)),
            Collections.singletonList(new Rect(510,108,530,130)));
        var shared=NumericQuantities.match(boxes,Collections.singletonList(line("1",100,120)));
        for(String text:shared)assertTrue(text.contains(" | "));
        var multiple=NumericQuantities.match(boxes,Arrays.asList(line("1",95,105),line("2",97,107)));
        assertEquals("1 | 2",multiple.get(0));assertEquals("",multiple.get(1));
        var overlap=Arrays.asList(Collections.singletonList(new Rect(510,90,530,130)),
            Collections.singletonList(new Rect(510,110,530,150)));
        var mixed=NumericQuantities.match(overlap,Arrays.asList(line("1",110,130),line("2",95,105)));
        for(String text:mixed)assertTrue(text.contains(" | shared"));
        Collections.reverse(overlap);
        for(String text:NumericQuantities.match(overlap,Arrays.asList(line("1",110,130),line("2",95,105))))
            assertTrue(text.contains(" | shared"));
    }
    private PaddleLine line(String text,int top,int bottom){
        return new PaddleLine(text,Arrays.asList(new PointF(517,top),new PointF(523,top),new PointF(523,bottom),new PointF(517,bottom)));
    }
}
