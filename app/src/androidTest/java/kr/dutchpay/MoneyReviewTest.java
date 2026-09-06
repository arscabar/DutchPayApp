package kr.dutchpay;

import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;

public class MoneyReviewTest extends InstrumentationTestCase {
    public void testPaymentOnlyRequiresExplicitSelectionAndReview() {
        MainActivity activity=open();
        getInstrumentation().runOnMainSync(() -> {
            Receipt r=new Receipt();r.raw="신용카드전표-고객용";r.total=39000L;
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
            assertTrue(r.paymentOnly());assertFalse(editor.copy.isEnabled());
            android.widget.Button payment=null;
            for(int i=0;i<activity.body.getChildCount();i++){
                var view=activity.body.getChildAt(i);
                if(view instanceof android.widget.Button && ((android.widget.Button)view).getText()
                    .toString().equals("품목 없이 전표 총액 적용"))payment=(android.widget.Button)view;
            }
            assertNotNull(payment);payment.performClick();assertFalse(payment.isEnabled());
            assertEquals(1,editor.editors.size());assertFalse(editor.copy.isEnabled());
            editor.reviewed.setChecked(true);editor.editors.get(0).reviewed.setChecked(true);
            assertTrue(editor.copy.isEnabled());assertTrue(editor.note.contains("선택 합계: 39000원"));
            assertTrue(editor.note.contains("품목 미확인"));activity.finish();
        });
    }
    public void testReviewAndSelection() {
        MainActivity activity=open();
        getInstrumentation().runOnMainSync(() -> {
            Receipt r=new Receipt(); r.total=10000L;
            Item item=new Item("음료",10000,1,10000); item.warning="수량 미인식"; r.items.add(item);
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
            RowEditor row=editor.editors.get(0);
            assertFalse(editor.copy.isEnabled()); editor.reviewed.setChecked(true);
            assertFalse(editor.copy.isEnabled()); row.reviewed.setChecked(true);
            assertTrue(editor.copy.isEnabled()); row.unit.setText("9000");
            assertFalse(editor.copy.isEnabled()); row.reviewed.setChecked(true);
            assertTrue(editor.copy.isEnabled());
            assertTrue(editor.note.contains("원본 인쇄 합계: 10000원"));
            assertTrue(editor.note.contains("선택 합계: 9000원"));
            row.percent.setText("101"); assertFalse(editor.copy.isEnabled());
            row.percent.setText("50"); assertTrue(editor.copy.isEnabled());
            row.selected.setChecked(false); assertFalse(editor.copy.isEnabled());
            activity.finish();
        });
    }
    public void testIncludedDiscountAndUnevenDivision() {
        MainActivity activity=open();
        getInstrumentation().runOnMainSync(() -> {
            Receipt r=new Receipt(); r.total=5067L; r.items.add(Item.line("품목",null,2,5067));
            Item discount=new Item("할인",-1000,1,-1000); discount.includedDiscount=true; r.items.add(discount);
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
            RowEditor row=editor.editors.get(0), included=editor.editors.get(1);
            assertEquals(5067,row.amount()); row.quantity.setText("1"); assertEquals(2534,row.amount());
            assertTrue(row.note().contains("원수량 2")); assertFalse(included.selected.isEnabled());
            assertFalse(included.unit.isEnabled()); assertEquals(0,included.amount());
            assertTrue(editor.note.contains("추가 차감 없음")); activity.finish();
        });
    }
    private MainActivity open() {
        return (MainActivity)getInstrumentation().startActivitySync(new Intent(
            getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
}
