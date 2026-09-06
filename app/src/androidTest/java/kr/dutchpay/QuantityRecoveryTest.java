package kr.dutchpay;

import android.graphics.*;
import android.test.InstrumentationTestCase;
import java.util.*;

public class QuantityRecoveryTest extends InstrumentationTestCase {
    private PaddleLine line(String s,int x,int top,int bottom) {
        return new PaddleLine(s,Arrays.asList(new PointF(x-2,top),new PointF(x+2,top),
            new PointF(x+2,bottom),new PointF(x-2,bottom)));
    }
    public void testRejectNoiseAndPreservePrices() {
        PaddleLine zero=line("0",501,221,236),price=line("1000",493,221,236),merged=line("11",522,222,255);
        List<PaddleLine> original=Arrays.asList(zero,price,merged);
        Rect strip=new Rect(490,200,558,736);
        List<PaddleLine> result=QuantityRecovery.merge(original,Arrays.asList(line("인",524,704,719),
            line("1",522,688,691),line("1",522,224,237),line("1",522,241,254)),strip,516,23);
        assertEquals(4,result.size());assertTrue(result.contains(zero));assertTrue(result.contains(price));
        assertFalse(result.contains(merged));assertEquals(3,original.size());
        assertEquals(2,result.stream().filter(l -> l.word.text.equals("1")).count());
        assertSame(original,QuantityRecovery.merge(original,Arrays.asList(line("0",522,224,237),
            line("10000",522,224,237),line("-1",522,224,237)),strip,516,23));
    }
}
