package kr.dutchpay;

import android.test.AndroidTestCase;

public class NameReviewTest extends AndroidTestCase {
    public void testCandidateRequiresIndependentEvidenceAndPreservesDigits(){
        var base=new KoreanModel.Reading("우윤식빵",.85);var better=new KoreanModel.Reading("우유식빵",.99);
        assertTrue(NameDecision.accept("우윤식빵",base,better,"우유식빵"));
        assertTrue(NameDecision.accept("우윤식빵",base,better,"-우유식빵"));
        assertFalse(NameDecision.accept("우윤식빵",base,new KoreanModel.Reading("우유식빵]",.99),"우유식빵"));
        assertTrue(NameDecision.sameText("▶ 식품 옵션","식품옵션"));
        assertFalse(NameDecision.sameText("식품(소)","식품소"));
        assertTrue(NameDecision.sameText("-식품","식품"));
        assertFalse(NameDecision.sameText("--식품","식품"));
        assertFalse(NameDecision.sameText("-500식품","500식품"));
        assertFalse(NameDecision.sameText("식품-옵션","식품옵션"));
        assertFalse(NameDecision.sameText("A1] New)메뉴","A1 New)메뉴"));
        assertFalse(NameDecision.accept("우윤식빵",base,better,"우윤식빵"));
        assertFalse(NameDecision.accept("우윤식빵",new KoreanModel.Reading("우윤식빵",.98),better,"우유식빵"));
        assertFalse(NameDecision.accept("삶은계란 추가",new KoreanModel.Reading("삶은계란 추가",.891),
            new KoreanModel.Reading("삶은계란 추기",.905),"삶은계란 추기"));
        assertFalse(NameDecision.sameNumbers("음료350ml","음료550ml"));
        assertFalse(NameDecision.sameNumbers("프러틴1+1팩","프로틴11팩"));
        assertFalse(NameDecision.sameNumbers("음로2.0L","음료20L"));
        assertFalse(NameDecision.sameNumbers("항목-500","항목500"));
        assertFalse(NameDecision.sameNumbers("항목−500","항목500"));
        assertFalse(NameDecision.sameNumbers("행사2~3개","행사23개"));
        assertTrue(NameDecision.candidate("A11New새우마요","AllNew새우마요"));
        assertFalse(NameDecision.candidate("우유","우유 초콜릿 대용량"));
    }
}
