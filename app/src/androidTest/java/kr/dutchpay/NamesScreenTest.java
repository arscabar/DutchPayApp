package kr.dutchpay;

import android.content.Intent;
import android.widget.ScrollView;
import java.io.File;

public class NamesScreenTest extends AppScreenTest {
    @Override public void testRealPaddleScreens() throws Exception {
        var app=getInstrumentation().getTargetContext();MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(
            new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            select(activity,copy(getInstrumentation().getContext().getAssets().open("receipts/03.jpg")));
            getInstrumentation().runOnMainSync(() -> {
                assertTrue(text(activity.body).contains("선택 합계: 77300원"));
                RowEditor row=(RowEditor)views(activity.body).stream().filter(v->v instanceof RowEditor
                    && ((RowEditor)v).source.originalName.equals("우윤식빵")).findFirst().orElse(null);
                assertNotNull("Name region did not produce candidates",row);
                assertTrue(row.nameChoices.button.isEnabled());
                android.graphics.Rect position=new android.graphics.Rect();row.getDrawingRect(position);
                activity.body.offsetDescendantRectToMyCoords(row,position);
                ((ScrollView)activity.body.getParent()).scrollTo(0,position.top);
            });
            screenshot("names-candidate-screen.png");
            getInstrumentation().runOnMainSync(() -> {
                RowEditor row=(RowEditor)views(activity.body).stream().filter(v->v instanceof RowEditor
                    && ((RowEditor)v).source.originalName.equals("우윤식빵")).findFirst().get();
                row.nameChoices.button.performClick();assertTrue(row.nameChoices.dialog.isShowing());
            });
            screenshot("names-choice-screen.png");
        }finally{
            getInstrumentation().runOnMainSync(activity::finish);
            for(File f:copies)f.delete();
        }
    }
}
