package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.graphics.Rect;
import org.json.*;
import java.util.*;
import java.io.*;

public class TableTest extends InstrumentationTestCase {
    public void testSplitThousands(){
        OcrWord a=new OcrWord("10,",new Rect(0,0,30,20));
        assertTrue(a.join(new OcrWord("000",new Rect(32,1,60,21))));
        assertEquals(Long.valueOf(10000),a.number());
        OcrWord b=new OcrWord("10,",new Rect(0,0,30,20));
        assertFalse(b.join(new OcrWord("000",new Rect(32,30,60,50))));
    }
    public void testStaggeredColumns() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=getInstrumentation().getContext().getAssets().open("geometry.json")){
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);
        }
        JSONArray data=new JSONArray(bytes.toString("UTF-8")); List<OcrWord> words=new ArrayList<>();
        for(int i=0;i<data.length();i++){
            JSONObject j=data.getJSONObject(i);int x=j.getInt("x"),y=j.getInt("y"),w=j.getInt("w"),h=j.getInt("h");
            OcrWord next=new OcrWord(j.getString("text"),new Rect(x-w/2,y-h/2,x+w/2,y+h/2));
            if(words.stream().noneMatch(p -> p.text.equals(next.text)&&Math.abs(p.x()-next.x())<w*.6&&Math.abs(p.y()-next.y())<h*.5))words.add(next);
        }
        words.sort(Comparator.comparingDouble(OcrWord::y));
        Receipt r=TableParser.parseWords(words,""); assertNotNull(r);
        assertEquals(77300,r.itemSum()); assertEquals(Long.valueOf(77300),r.total);
        assertEquals(8900,r.items.get(0).unit);
        assertTrue(r.items.get(0).name.startsWith("매콤닭다리살"));
        assertEquals(0,r.items.get(1).unit);
        assertEquals(1000,r.items.get(2).unit); assertEquals(2,r.items.get(2).count);
        assertTrue(r.items.get(2).name.contains("삶은계란"));
        assertEquals(9000,r.items.get(3).unit);
        assertTrue(r.items.get(3).name.startsWith("훈제오리 단백질"));
    }
}
