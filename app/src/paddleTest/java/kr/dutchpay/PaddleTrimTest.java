package kr.dutchpay;
import android.test.InstrumentationTestCase;

public class PaddleTrimTest extends InstrumentationTestCase {
    public void testCtcRepeatsAndBlank() throws Exception {
        var decoded=KoreanModel.decode(new float[][]{{.1f,.8f,.1f},{.1f,.8f,.1f},
            {.9f,.05f,.05f},{.05f,.9f,.05f},{.1f,.2f,.7f}},new org.json.JSONArray("[\"\",\"한\",\"두\"]"));
        assertEquals("한한두",decoded.text);assertEquals(.8,decoded.confidence,.0001);
    }
    public void testNoRewardForDroppingCharacters() {
        var base=new KoreanModel.Reading("가나다",.80);
        assertTrue(PaddleTrim.accepts(base,new KoreanModel.Reading("가너다",.90)));
        assertFalse(PaddleTrim.accepts(base,new KoreanModel.Reading("가나",.99)));
        assertFalse(PaddleTrim.accepts(base,new KoreanModel.Reading("가너다",.81)));
        assertFalse(PaddleTrim.accepts(new KoreanModel.Reading("가나다",.96),new KoreanModel.Reading("가너다",.99)));
    }
    public void testNumbersAndSignsMustStayExact() {
        var base=new KoreanModel.Reading("초고바75m -2,040",.80);
        assertTrue(PaddleTrim.accepts(base,new KoreanModel.Reading("초코바75m -2,040",.90)));
        for(String changed:new String[]{"초코바750m -2,040","초코바75m -2,400",
                "초코바75m 2,040원","초코바75m −2,040","초코바75m -2040원"})
            assertFalse(changed,PaddleTrim.accepts(base,new KoreanModel.Reading(changed,.90)));
        assertFalse(PaddleTrim.accepts(new KoreanModel.Reading("초고바75m 2,040",.80),
            new KoreanModel.Reading("초코바75m -2,040",.90)));
    }
}
