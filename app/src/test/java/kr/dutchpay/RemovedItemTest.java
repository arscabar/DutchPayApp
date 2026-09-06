package kr.dutchpay;

import org.junit.Test;
import static org.junit.Assert.*;

public class RemovedItemTest {
    @Test public void negativePrintedQuantityKeepsSignAndExactAmount(){
        Item i=Item.line("기존 음료",null,-2,-4101);
        assertEquals(-2,i.count);assertEquals(-4101,i.printedTotal);
        assertEquals(-4101,i.baseAmount());assertFalse(i.includedDiscount);
        assertEquals(-4101,i.portion("2","100",-4101));
        assertEquals(-2051,i.portion("1","100",-4101));
        assertEquals(-1025,i.portion("1","50",-4101));
        assertEquals(-2,i.count);assertEquals(-4101,i.unit);
        try{i.portion("1","100",4101);fail();}catch(IllegalArgumentException expected){}
        try{i.portion("-1","100",-4101);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void knownPositivePriceReturnAndSeparateDiscountRemainDistinct(){
        Item returned=Item.line("반품",3000L,-2,-6000);
        assertEquals(3000,returned.unit);assertEquals(-6000,returned.baseAmount());
        assertEquals(-1500,returned.portion("1","50",3000));
        assertEquals("",Receipt.issue(returned));
        Item discount=new Item("별도 할인",-700,1,-700);
        assertEquals(-700,discount.portion("1","100",-700));
        discount.includedDiscount=true;assertEquals(0,discount.baseAmount());
        try{Item.line("중복 음수",-3000L,-2,-6000);fail();}catch(IllegalArgumentException expected){}
        try{Item.line("모순 부호",null,-2,6000);fail();}catch(IllegalArgumentException expected){}
    }
}
