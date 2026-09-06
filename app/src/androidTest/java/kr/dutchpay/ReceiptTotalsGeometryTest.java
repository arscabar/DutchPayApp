package kr.dutchpay;

import android.graphics.Rect;
import android.test.InstrumentationTestCase;
import java.util.*;

public class ReceiptTotalsGeometryTest extends InstrumentationTestCase {
    public void testVerticalTotalLabelIsRecoveredWithReviewWarning(){
        List<OcrWord> words=new ArrayList<>();
        for(int i=0;i<4;i++)words.add(new OcrWord(new String[]{"판부봉합","매가사계","금세료금","액액액액"}[i],
            new Rect(i*30,100,i*30+20,220)));
        String[] numbers={"9,091","909","0","10,000"};
        for(int i=0;i<4;i++)words.add(word(numbers[i],100+i*30));
        Receipt r=new Receipt();ReceiptTotals.apply(r,words,"신용카드전표-고객용\n판부봉합 매가사계 금세료금 액액액액");
        assertEquals(Long.valueOf(10000),r.total);
        assertTrue(r.warnings.stream().anyMatch(w->w.contains("좌표로 연결")));
    }
    public void testTaxStructureRequiresRepeatedPrintedTotal(){
        String raw="신용카드전표-고객용\n신용승인정보[1]";
        List<OcrWord> words=new ArrayList<>(Arrays.asList(word("9,091",100),word("909",130),word("10,000",160)));
        assertNull(CardTotalRecovery.read(words,raw));
        words.addAll(Arrays.asList(word("9,091",300),word("909",330),word("0",360),word("10,000",390)));
        Receipt r=new Receipt();ReceiptTotals.apply(r,words,raw);
        assertEquals(Long.valueOf(10000),r.total);
        assertTrue(r.warnings.stream().anyMatch(w->w.contains("금액열 구조")));
        assertNull(CardTotalRecovery.read(words,raw+"\n신용승인정보[2]"));
        words.removeIf(w->w.text.equals("10,000"));assertNull(CardTotalRecovery.read(words,raw));
    }
    public void testSameRankConflictsRemainUnconfirmed(){
        Receipt r=new Receipt();ReceiptTotals.apply(r,Collections.emptyList(),"결제금액20,000\n결제금액21,000");
        assertNull(r.total);assertTrue(r.warnings.stream().anyMatch(w->w.contains("서로 다른")));
    }
    private OcrWord word(String text,int y){return new OcrWord(text,new Rect(200,y,270,y+20));}
}
