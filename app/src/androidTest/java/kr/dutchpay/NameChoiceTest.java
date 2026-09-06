package kr.dutchpay;

import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import android.widget.ListView;

public class NameChoiceTest extends InstrumentationTestCase {
    public void testCandidateSelectionPreservesAmountsAndRequiresReview() {
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(
            getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            getInstrumentation().runOnMainSync(() -> {
                Receipt r=new Receipt();r.total=24000L;
                Item item=new Item("보리차",8000,3,24000);
                item.originalName="보리자";
                item.nameCandidates.add("보리차");item.nameCandidates.add("보리차");
                item.warning="품목명 후보 확인 필요";r.items.add(item);
                ReceiptEditor editor=new ReceiptEditor(activity,activity.body,r,Uri.EMPTY);
                RowEditor row=editor.editors.get(0);
                assertEquals("처음 인식: 보리자",row.nameChoices.original.getText().toString());
                assertEquals(0,row.nameChoices.original.getVisibility());
                row.unit.setText("6500");row.quantity.setText("2");row.percent.setText("75");
                editor.reviewed.setChecked(true);row.reviewed.setChecked(true);
                assertTrue(editor.copy.isEnabled());assertEquals(9750L,row.amount());
                choose(row,0);
                assertEquals("보리자",row.name.getText().toString());
                assertEquals(8,row.nameChoices.original.getVisibility());
                assertFalse(row.reviewed.isChecked());assertFalse(editor.copy.isEnabled());
                assertEquals("6500",row.unit.getText().toString());
                assertEquals("2",row.quantity.getText().toString());
                assertEquals("75",row.percent.getText().toString());
                assertTrue(row.selected.isChecked());assertEquals(9750L,row.amount());
                assertTrue(editor.note.contains("선택 합계: 9750원"));
                assertEquals(8000L,item.unit);assertEquals(3,item.count);assertEquals(24000L,item.printedTotal);
                row.reviewed.setChecked(true);choose(row,1);
                assertEquals("보리차",row.name.getText().toString());
                assertEquals(0,row.nameChoices.original.getVisibility());
                assertFalse(row.reviewed.isChecked());assertEquals(9750L,row.amount());
                row.nameChoices.button.performClick();
                var dialog=row.nameChoices.dialog;assertTrue(dialog.isShowing());
                row.removeView(row.nameChoices);
                assertFalse(dialog.isShowing());assertNull(row.nameChoices.dialog);
            });
        } finally { getInstrumentation().runOnMainSync(activity::finish); }
    }
    private void choose(RowEditor row,int index) {
        row.nameChoices.button.performClick();
        ListView list=row.nameChoices.dialog.getListView();
        assertEquals(2,list.getAdapter().getCount());
        list.performItemClick(null,index,list.getAdapter().getItemId(index));
        assertFalse(row.nameChoices.dialog.isShowing());
    }
}
