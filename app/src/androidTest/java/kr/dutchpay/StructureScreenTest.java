package kr.dutchpay;

import android.content.Intent;
import android.widget.*;
import java.io.File;

public class StructureScreenTest extends AppScreenTest {
    @Override public void testRealPaddleScreens() throws Exception {
        var app=getInstrumentation().getTargetContext();
        var assets=getInstrumentation().getContext().getAssets();
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(
            new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            select(activity,copy(assets.open("receipts/03.jpg")));
            getInstrumentation().runOnMainSync(() -> {
                assertTrue(text(activity.body).contains("선택 합계: 77300원"));
                assertEquals(20L,views(activity.body).stream().filter(v->v instanceof RowEditor).count());
                showSummary(activity);
            });
            screenshot("structure-option-screen.png");
            select(activity,copy(assets.open("receipts/04.jpg")));
            getInstrumentation().runOnMainSync(() -> {
                assertTrue(text(activity.body).contains("원본 인쇄 합계: 10000원"));
                assertEquals(0L,views(activity.body).stream().filter(v->v instanceof RowEditor).count());
                assertFalse(button(activity.body).isEnabled());
                Button payment=(Button)views(activity.body).stream().filter(v->v instanceof Button
                    && ((Button)v).getText().toString().equals("품목 없이 전표 총액 적용")).findFirst().orElse(null);
                assertNotNull(payment);assertTrue(payment.isEnabled());payment.performClick();
                assertFalse(payment.isEnabled());
                assertEquals(1L,views(activity.body).stream().filter(v->v instanceof RowEditor).count());
                RowEditor row=(RowEditor)views(activity.body).stream().filter(v->v instanceof RowEditor).findFirst().get();
                assertEquals("전표 결제금액 (품목 미확인)",row.name.getText().toString());
                assertTrue(text(activity.body).contains("선택 합계: 10000원"));
                assertFalse(row.reviewed.isChecked());assertFalse(button(activity.body).isEnabled());
                showSummary(activity);
            });
            screenshot("structure-card-screen.png");
        } finally {
            getInstrumentation().runOnMainSync(activity::finish);
            for(File file:copies)file.delete();
        }
    }
    private void showSummary(MainActivity a) {
        TextView summary=(TextView)views(a.body).stream().filter(v->v instanceof TextView
            && ((TextView)v).getText().toString().startsWith("선택 합계:")).findFirst().get();
        ((ScrollView)a.body.getParent()).scrollTo(0,Math.max(0,summary.getTop()-80));
    }
}
