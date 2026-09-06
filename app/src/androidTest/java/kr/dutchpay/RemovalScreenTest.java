package kr.dutchpay;

import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class RemovalScreenTest extends InstrumentationTestCase {
    public void testSignedSourceUsesPositiveAppliedQuantityAndNegativeAmount(){
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(
            getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{getInstrumentation().runOnMainSync(()->{
            Receipt r=new Receipt();r.total=4900L;r.items.add(new Item("세트",9000,1,9000));
            Item removed=Item.line("기존 음료",null,-2,-4100);removed.warning="원본 음수 수량 확인";r.items.add(removed);
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
            RowEditor row=editor.editors.get(1);
            assertEquals("2",row.quantity.getText().toString());assertEquals("-4100",row.unit.getText().toString());
            assertEquals(-4100L,row.amount());assertTrue(row.note().contains("원본 수량 -2개"));
            assertFalse(editor.copy.isEnabled());editor.reviewed.setChecked(true);row.reviewed.setChecked(true);
            assertTrue(editor.copy.isEnabled());row.quantity.setText("1");assertFalse(editor.copy.isEnabled());
            assertEquals(-2050L,row.amount());assertEquals(-2,removed.count);
            row.selected.setChecked(false);assertEquals(0L,row.amount());
            assertTrue(editor.note.contains("선택 합계: 9000원"));
            RowEditor returned=new RowEditor(activity,Item.line("반품",3000L,-2,-6000),()->{});
            returned.quantity.setText("1");assertEquals(-3000L,returned.amount());
            assertTrue(returned.note().contains("편집 단가 3000원"));
            Item coupon=new Item("행사 쿠폰",-800,1,800);coupon.warning="정산 구간 차감 확인";
            RowEditor adjustment=new RowEditor(activity,coupon,()->{});
            assertEquals("정산 구간 차감 확인",Receipt.issue(coupon));assertFalse(adjustment.confirmed());
            assertEquals(-800L,adjustment.amount());
            assertTrue(adjustment.note().contains("원본 인쇄 행 금액 800원 / 적용 계산 -800원"));
        });}finally{getInstrumentation().runOnMainSync(activity::finish);}
    }
}
