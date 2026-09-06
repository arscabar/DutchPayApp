package kr.dutchpay;

import android.content.Intent;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.*;

public class EditorTest extends InstrumentationTestCase {
    public void testEditActualOcr() throws Exception {
        android.content.Context context = getInstrumentation().getTargetContext();
        File file = new File(context.getExternalFilesDir(null),"batch.json");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n=in.read(buffer))!=-1) bytes.write(buffer,0,n);
        }
        JSONArray batch = new JSONArray(bytes.toString("UTF-8"));
        String rows="";
        for(int i=0;i<batch.length();i++) if(batch.getJSONObject(i).getString("file").equals("10.jpg"))
            rows=batch.getJSONObject(i).getString("rows");
        assertFalse(rows.isEmpty());
        Receipt receipt=Parser.parse(Arrays.asList(rows.split("\n")));
        Intent intent=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(intent);
        getInstrumentation().runOnMainSync(() -> {
            activity.status.setText("실제 OCR 결과 · 10번 영수증 편집 검증");
            ReceiptEditor editor=new ReceiptEditor(activity,activity.body,receipt,Uri.EMPTY);
            assertEquals(4,editor.editors.size());
            assertTrue(editor.note.contains("41000원"));
            editor.editors.get(0).percent.setText("50");
            assertTrue(editor.note.contains("36000원"));
            editor.editors.get(1).selected.setChecked(false);
            assertTrue(editor.note.contains("25000원"));
            editor.editors.get(0).percent.setText("101");
            assertFalse(editor.copy.isEnabled());
            editor.editors.get(0).percent.setText("50");
            assertTrue(editor.copy.isEnabled());
        });
        getInstrumentation().waitForIdleSync();
        android.os.SystemClock.sleep(400);
        getInstrumentation().runOnMainSync(() -> {
            android.view.View view=activity.getWindow().getDecorView();
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(
                view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
            view.draw(new android.graphics.Canvas(bitmap));
            try (OutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"editor.png"))) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
            } catch(IOException e) { throw new RuntimeException(e); }
            bitmap.recycle();
        });
    }
}
