package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class UnknownQuantityTest {
    @Test public void observedUnitCannotBecomeOneItemAmount() {
        Item i=Item.line("식사",3900L,null,12601);
        assertFalse(i.quantityKnown);assertEquals(0,i.count);
        assertFalse(i.amountBased);assertEquals(3900,i.unit);
        assertEquals(12601,i.printedTotal);assertEquals(12601,i.baseAmount());
        assertEquals(12601,i.portion("","100",3900));
        assertEquals(6301,i.portion("","50",3900));
        assertEquals(7800,i.portion("2","100",3900));
        assertEquals(8000,i.portion("2","100",4000));
        assertEquals(12601,i.portion("","100",4000));
        assertEquals(3900,i.unit);assertEquals(0,i.count);
        Receipt r=new Receipt();r.total=12601L;r.items.add(i);r.validate();
        assertEquals(12601,r.itemSum());assertTrue(Receipt.issue(i).contains("수량 미인식"));
        assertFalse(r.warnings.stream().anyMatch(w -> w.contains("추출 합계")));
    }
    @Test public void unknownUnitAndQuantityPermitOnlyWholeLinePercent() {
        Item i=Item.line("품목",null,null,5067);
        assertTrue(i.amountBased);assertFalse(i.quantityKnown);
        assertEquals(5067,i.unit);assertEquals(5067,i.baseAmount());
        assertEquals(2534,i.portion("","50",5067));
        assertEquals(2000,i.portion("","50",4000));
        try{i.portion("1","100",5067);fail();}catch(IllegalArgumentException expected){}
        try{Item.line("잘못된 수량",100L,0,100);fail();}catch(IllegalArgumentException expected){}
        assertEquals(200,Item.line("정상 수량",100L,2,200).baseAmount());
    }
}
