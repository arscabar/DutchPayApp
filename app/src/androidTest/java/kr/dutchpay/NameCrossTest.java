package kr.dutchpay;

import android.test.InstrumentationTestCase;

public class NameCrossTest extends InstrumentationTestCase {
    public void testDifferentEnginesMustAgree(){
        var base=new KoreanModel.Reading("수박쥬스",.88);
        var retry=new KoreanModel.Reading("수박주스",.93);
        assertTrue(NameCross.eligible(base.text,.89,base,retry,true));
        assertTrue(NameCross.agrees(base.text,retry,"수박 주스"));
        assertFalse(NameCross.agrees(base.text,retry,"수박 우유"));
        assertFalse(NameCross.agrees("Al1 New)메뉴",new KoreanModel.Reading("A1] New)메뉴",.95),"A1 New)메뉴"));
        assertTrue(NameCross.agrees("식풍옵션",new KoreanModel.Reading("→식품 옵션",.95),"식품옵션"));
        assertFalse(NameCross.eligible("주스300",.5,base,new KoreanModel.Reading("주스350",.99),true));
    }
    public void testHighConfidenceProtection(){
        var base=new KoreanModel.Reading("삼겹살",.99);
        assertFalse(NameCross.eligible(base.text,.99,base,new KoreanModel.Reading("삼겹살맛",.95),false));
        assertFalse(NameCross.eligible(base.text,.99,base,new KoreanModel.Reading("삼겹살300",.99),true));
        assertFalse(NameCross.eligible(base.text,.99,base,new KoreanModel.Reading("삼겹전",.99),true));
    }
    public void testOnlyObservedHangulInsertion(){
        var base=new KoreanModel.Reading("우유300",.995);
        assertTrue(NameCross.eligible(base.text,.995,base,new KoreanModel.Reading("우유팩300",.92),true));
        assertFalse(NameCross.insertion("우유300","우유팩350"));
        assertFalse(NameCross.insertion("우유300","우유BOX300"));
        assertFalse(NameCross.insertion("우유300","우유맛있는팩300"));
    }
}
