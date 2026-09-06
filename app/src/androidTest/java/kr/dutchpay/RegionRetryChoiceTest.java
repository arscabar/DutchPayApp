package kr.dutchpay;

import android.test.InstrumentationTestCase;
import org.json.*;

public class RegionRetryChoiceTest extends InstrumentationTestCase {
    final JSONArray chars=new JSONArray(java.util.Arrays.asList("","가","나","다","1","라","-"," "));
    RegionRetryCtc line(String text,float old,float alternative) throws Exception {
        float[][] rows=new float[text.length()*2][chars.length()];
        for(int i=0;i<text.length();i++){
            int id=0;for(int j=1;j<chars.length();j++)if(chars.getString(j).equals(text.substring(i,i+1)))id=j;
            rows[i*2][id]=.999f;rows[i*2][0]=.001f;rows[i*2+1][0]=1;
            if(text.charAt(i)=='나'){
                rows[i*2][id]=old;rows[i*2][3]=alternative;rows[i*2][0]=1-old-alternative;
            }
        }
        return RegionRetryCtc.read(rows,chars);
    }
    String choose(String original,RegionRetryCtc center,String ml) throws Exception {
        return RegionRetryChoice.select(original,line(original,.88f,.10f),center,ml,new JSONArray());
    }
    public void testObservedGlyphAndNumericPreservation() throws Exception {
        assertEquals("가다1",choose("가나1",line("가나1",.60f,.35f),"가다1"));
        assertEquals("가나1",choose("가나1",line("가나1",.60f,.35f),"가다2"));
        assertEquals("가 다-1",choose("가 나-1",line("가 나-1",.60f,.35f),"가다-1"));
        assertEquals("가 나-1",choose("가 나-1",line("가 나-1",.60f,.35f),"가다+2"));
        assertEquals("가나1",choose("가나1",line("가나1",.60f,.35f),"가다11"));
    }
    public void testOriginalStrengtheningAndUnrelatedViewVeto() throws Exception {
        assertEquals("가나1",choose("가나1",line("가나1",.91f,.08f),"가다1"));
        assertEquals("가나1",choose("가나1",line("가라1",.6f,.35f),"가다1"));
        assertFalse(RegionRetryChoice.eligible("가나1",line("가나1",.96f,.03f),"가다1"));
        assertFalse(RegionRetryChoice.eligible("가나1",line("가나1",.90f,.06f),"가다1"));
        assertFalse(RegionRetryChoice.eligible("가나1",line("가나1",.60f,.15f),"가다1")); // blank .25 is second
        assertEquals("가가",line("가가",.9f,.05f).text);
    }
    public void testOnlyOneObservedHangulDifference(){
        assertTrue(RegionRetryChoice.singleDifference("[가]나다1","[가]라다1"));
        assertFalse(RegionRetryChoice.singleDifference("[가]나다1","[나]라다1"));
        assertFalse(RegionRetryChoice.singleDifference("가나1","가나다1"));
        assertFalse(RegionRetryChoice.singleDifference("가나-1","가나+1"));
        assertFalse(RegionRetryChoice.singleDifference("가나1","가나2"));
        assertFalse(RegionRetryChoice.singleDifference("가나1","가나1"));
    }
}
