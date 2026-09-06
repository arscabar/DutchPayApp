package kr.dutchpay;
import org.junit.Test;
import static org.junit.Assert.*;

public class LineAmountTest {
    @Test public void exactLineAmountIsRoundedOnlyAtTheEnd() {
        Item i=Item.line("음료",null,2,5067);
        assertTrue(i.amountBased); assertEquals(5067,i.baseAmount());
        assertEquals(5067,i.portion("2","100",5067));
        assertEquals(2534,i.portion("1","100",5067));
        assertEquals(1267,i.portion("1","50",5067));
        Item discount=new Item("반영된 할인",-2400,1,-2400);
        discount.includedDiscount=true;
        assertEquals(0,discount.baseAmount()); assertEquals(0,discount.portion("1","100",-2400));
        assertEquals(9000,Item.line("국밥",9000L,2,18000).portion("1","100",9000));
        try{i.portion("-1","100",5067);fail();}catch(IllegalArgumentException expected){}
    }
}
