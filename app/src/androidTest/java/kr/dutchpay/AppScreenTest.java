package kr.dutchpay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.test.InstrumentationTestCase;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class AppScreenTest extends InstrumentationTestCase {
    final List<File> copies=new ArrayList<>();
    public void testRealPaddleScreens() throws Exception {
        var app=getInstrumentation().getTargetContext();
        MainActivity activity=(MainActivity)getInstrumentation().startActivitySync(
            new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            select(activity,copy(getInstrumentation().getContext().getAssets().open("receipts/01.jpg")));
            getInstrumentation().runOnMainSync(() -> {
                assertTrue(text(activity.body).contains("선택 합계: 58000원"));
                assertTrue(views(activity.body).stream().filter(v->v instanceof RowEditor).count()>=3);
            });
            screenshot("steps-app-screen.png");
            select(activity,copy(new FileInputStream(new File(app.getExternalFilesDir(null),
                "scans/Scan_20260901_210817.jpg"))));
            getInstrumentation().runOnMainSync(() -> {
                assertTrue(text(activity.body).contains("확인 필요"));
                assertFalse(button(activity.body).isEnabled());
            });
            screenshot("steps-review-screen.png");
        } finally {
            getInstrumentation().runOnMainSync(activity::finish);
            for(File file:copies)file.delete();
        }
    }
    File copy(InputStream in) throws Exception {
        File file=File.createTempFile("steps-receipt-",".jpg",getInstrumentation().getTargetContext().getCacheDir());
        copies.add(file);
        try(in;var out=new FileOutputStream(file)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
        return file;
    }
    void select(MainActivity a,File file) {
        getInstrumentation().runOnMainSync(() -> a.onActivityResult(1,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(file))));
        long deadline=SystemClock.elapsedRealtime()+120000;boolean[] done={false};
        while(!done[0] && SystemClock.elapsedRealtime()<deadline){
            getInstrumentation().runOnMainSync(() -> {
                String status=a.status.getText().toString();
                assertFalse(status,status.contains("실패") || status.contains("열 수 없습니다"));
                done[0]=button(a.body)!=null;
                if(done[0])assertTrue(status,status.startsWith("Paddle 한국어 OCR"));
            });
            if(!done[0])SystemClock.sleep(250);
        }
        assertTrue("Paddle 화면 완료 시간 초과",done[0]);getInstrumentation().waitForIdleSync();
    }
    List<View> views(View v){
        List<View> out=new ArrayList<>();out.add(v);
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.addAll(views(((ViewGroup)v).getChildAt(i)));
        return out;
    }
    String text(View v){return views(v).stream().filter(x->x instanceof TextView && x.getVisibility()==View.VISIBLE)
        .map(x->((TextView)x).getText().toString()).reduce("",(a,b)->a+"\n"+b);}
    Button button(View v){return (Button)views(v).stream().filter(x->x instanceof Button
        && ((Button)x).getText().toString().equals("비고 미리보기·복사")).findFirst().orElse(null);}
    void screenshot(String name) throws Exception {
        SystemClock.sleep(300);Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        try(var out=new FileOutputStream(new File(getInstrumentation().getTargetContext().getExternalFilesDir(null),name))){
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));
        }finally{image.recycle();}
    }
}
