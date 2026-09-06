package kr.dutchpay;

import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class UnknownQuantityScreenTest extends InstrumentationTestCase {
    public void testWholeLineAndUserQuantityAreDistinguished() {
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(
            getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try { getInstrumentation().runOnMainSync(() -> {
            Receipt r=new Receipt();r.total=12601L;
            Item item=Item.line("식사",3900L,null,12601);r.items.add(item);
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
            RowEditor row=editor.editors.get(0);
            assertEquals("",row.quantity.getText().toString());assertFalse(row.unit.isEnabled());
            assertEquals("3900",row.unit.getText().toString());assertEquals(12601L,row.amount());
            assertTrue(row.note().contains("행 금액 전체 적용, 원수량 미인식"));
            assertFalse(editor.copy.isEnabled());editor.reviewed.setChecked(true);
            row.reviewed.setChecked(true);assertTrue(editor.copy.isEnabled());
            row.quantity.setText("2");assertEquals(7800L,row.amount());assertTrue(row.unit.isEnabled());
            assertFalse(editor.copy.isEnabled());assertTrue(row.note().contains("직접 입력한 적용 수량"));
            row.unit.setText("4000");assertEquals(8000L,row.amount());row.reviewed.setChecked(true);
            row.quantity.setText("");assertFalse(row.unit.isEnabled());assertFalse(editor.copy.isEnabled());
            assertEquals("4000",row.unit.getText().toString());assertEquals(12601L,row.amount());
            assertTrue(row.note().contains("인쇄 단가 3900원"));
            row.percent.setText("50");assertEquals(6301L,row.amount());
            assertEquals(3900L,item.unit);assertEquals(0,item.count);assertFalse(item.quantityKnown);
        }); } finally { getInstrumentation().runOnMainSync(activity::finish); }
    }
    public void testUnknownUnitCannotDivideByInventedQuantity() {
        getInstrumentation().runOnMainSync(() -> {
            RowEditor row=new RowEditor(getInstrumentation().getTargetContext(),
                Item.line("품목",null,null,5067),() -> {});
            assertFalse(row.quantity.isEnabled());assertTrue(row.unit.isEnabled());
            assertEquals("",row.quantity.getText().toString());assertEquals(5067L,row.amount());
            row.unit.setText("4000");row.percent.setText("50");assertEquals(2000L,row.amount());
            assertTrue(row.note().contains("원수량 미인식"));
        });
    }
}
