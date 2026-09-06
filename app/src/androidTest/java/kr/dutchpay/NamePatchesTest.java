package kr.dutchpay;

import android.graphics.*;
import android.test.AndroidTestCase;
import java.util.*;

public class NamePatchesTest extends AndroidTestCase {
    private Bitmap page;
    @Override protected void setUp() throws Exception {
        super.setUp();page=Bitmap.createBitmap(400,240,Bitmap.Config.ARGB_8888);page.eraseColor(Color.WHITE);
    }
    @Override protected void tearDown() throws Exception {page.recycle();super.tearDown();}
    public void testCancellationCannotSupplyPurchasedNameCrop(){
        List<PaddleLine> lines=new ArrayList<>();
        row(lines,"통다리1조각",20,"-1","-3,600");
        row(lines,"통다리1조각",80,"4","14,400");
        row(lines,"우유",140,"1","2,000");
        Receipt r=new Receipt();Item cancelled=new Item("통다리1조각",-3600,1,-3600);
        cancelled.includedDiscount=true;Item bought=new Item("통다리1조각",3600,4,14400);
        Item milk=new Item("우유",2000,1,2000);r.items.addAll(Arrays.asList(cancelled,bought,milk));
        try(var patches=NamePatches.create(page,lines,r)){
            assertEquals(1,patches.items.size());assertSame(milk,patches.items.get(0));
            assertEquals(1,patches.originals.size());assertEquals(1,patches.padded.size());
        }
        assertEquals(3600L,bought.unit);assertEquals(4,bought.count);assertEquals(14400L,bought.printedTotal);
        assertEquals(-3600L,cancelled.printedTotal);assertTrue(cancelled.includedDiscount);
    }
    public void testRepeatedOptionsWithDifferentAmountsAreSkipped(){
        List<PaddleLine> lines=new ArrayList<>();
        row(lines,"삶은계란 추가",20,"2","1,878");row(lines,"삶은계란 추가",80,"1","939");
        Receipt r=new Receipt();r.items.add(new Item("삶은계란 추가",1000,2,1878));
        r.items.add(new Item("삶은계란 추가",1000,1,939));
        try(var patches=NamePatches.create(page,lines,r)){assertTrue(patches.items.isEmpty());}
    }
    public void testUniqueFragmentGroupCannotBeReused(){
        List<PaddleLine> lines=Arrays.asList(line("생고기",10,20,90),line("김치찌개",100,20,210));
        Receipt r=new Receipt();Item first=new Item("생고기 김치찌개",9000,1,9000);
        r.items.add(first);r.items.add(new Item(first.name,9000,1,9000));
        try(var patches=NamePatches.create(page,lines,r)){
            assertEquals(1,patches.items.size());assertSame(first,patches.items.get(0));
        }
    }
    private void row(List<PaddleLine> lines,String name,int y,String quantity,String amount){
        lines.add(line(name,10,y,200));lines.add(line(quantity,220,y,245));lines.add(line(amount,270,y,360));
    }
    private PaddleLine line(String text,int x,int y,int right){
        return new PaddleLine(text,Arrays.asList(new PointF(x,y),new PointF(right,y),
            new PointF(right,y+24),new PointF(x,y+24)));
    }
}
