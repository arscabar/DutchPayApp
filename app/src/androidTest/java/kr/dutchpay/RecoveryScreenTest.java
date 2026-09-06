package kr.dutchpay;

import android.content.Intent;
import android.graphics.Rect;
import android.widget.ScrollView;
import java.io.*;

public class RecoveryScreenTest extends AppScreenTest {
    @Override public void testRealPaddleScreens() throws Exception {
        var app=getInstrumentation().getTargetContext();
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(new Intent(app,MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            select(activity,copy(new FileInputStream(new File(app.getExternalFilesDir(null),
                "scans/Scan_20260806_190801.jpg"))));
            getInstrumentation().runOnMainSync(()->{
                assertTrue(text(activity.body).contains("선택 합계: 9400원"));
                RowEditor removed=row(activity,true);assertEquals(-1,removed.source.count);
                assertEquals("1",removed.quantity.getText().toString());assertEquals(-2200L,removed.amount());
                assertTrue(removed.note().contains("원본 수량 -1개"));show(activity,removed);
            });
            screenshot("recovery-removal-screen.png");
            getInstrumentation().runOnMainSync(()->{
                RowEditor coupon=row(activity,false);
                assertTrue(coupon.note().contains("원본 인쇄 행 금액 2200원 / 적용 계산 -2200원"));
                assertFalse(coupon.confirmed());show(activity,coupon);
            });
            screenshot("recovery-coupon-screen.png");
            select(activity,copy(new FileInputStream(new File(app.getExternalFilesDir(null),
                "scans/Scan_20260710_124943.jpg"))));
            getInstrumentation().runOnMainSync(()->{
                RowEditor first=(RowEditor)views(activity.body).stream().filter(v->v instanceof RowEditor).findFirst().get();
                assertTrue(first.source.quantityKnown);assertEquals(2,first.source.count);
                assertEquals("CJ)맥스봉고소한치즈후랑",first.source.name);
                assertEquals(2400L,first.amount());
                assertTrue(text(activity.body).contains("선택 합계: 9500원"));show(activity,first);
            });
            screenshot("recovery-quantity-screen.png");
        }finally{getInstrumentation().runOnMainSync(activity::finish);for(File file:copies)file.delete();}
    }
    private RowEditor row(MainActivity activity,boolean removed){
        return (RowEditor)views(activity.body).stream().filter(v->v instanceof RowEditor &&
            (removed?((RowEditor)v).source.count<0:((RowEditor)v).source.unit<0 && ((RowEditor)v).source.printedTotal>0)).findFirst().get();
    }
    private void show(MainActivity activity,RowEditor row){
        Rect rect=new Rect();row.getDrawingRect(rect);activity.body.offsetDescendantRectToMyCoords(row,rect);
        ((ScrollView)activity.body.getParent()).scrollTo(0,rect.top);
    }
}
