package kr.dutchpay;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import java.util.concurrent.*;

final class PaddleScan {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r ->
        new Thread(r,"receipt-paddle"));
    static Task<Receipt> read(Context context,Uri uri) {
        if(uri==null)return Tasks.forException(new IllegalArgumentException("Image URI is missing"));
        Context app=context.getApplicationContext();
        return Tasks.call(WORKER,() -> {
            Bitmap page=null;
            try {
                page=InputImage.fromFilePath(app,uri).getBitmapInternal();
                if(page==null)throw new java.io.IOException("Image bitmap is unavailable");
                try(FullPaddle engine=new FullPaddle(app,true)) {
                    var original=engine.read(page,1600);
                    try(var direction=PageOrientation.read(page,original,engine)) {
                    var updated=PaddleRetry.read(direction.page,direction.lines,engine.korean);
                    var trimmed=PaddleTrim.read(direction.page,updated,engine.korean);
                    var quantities=QuantityRecovery.read(direction.page,trimmed.lines);
                    var diagnostics=new org.json.JSONArray();
                    var numbers=NumericRecovery.read(direction.page,quantities,engine.korean,diagnostics);
                    var complete=MissingQuantity.read(direction.page,numbers,engine.korean,diagnostics);
                    Receipt r=PaddleReceipt.receipt(complete);
                    NameReview.apply(app,direction.page,complete,r,engine.korean);
                    r.numericDiagnostics=diagnostics.toString();
                    r.originalRaw=String.join("\n",PaddleLine.rows(original));
                    if(direction.degrees!=0)r.warnings.add("영수증 방향을 "+direction.degrees+"도 회전하여 재인식했습니다");
                    if(numbers!=quantities)r.warnings.add("분리한 숫자 영역을 두 인식기로 다시 읽었습니다. 원본 확인 필요");
                    if(complete!=numbers)r.warnings.add("검출에서 빠진 수량을 두 인식기로 다시 읽었습니다. 원본 확인 필요");
                    if(quantities!=trimmed.lines)r.warnings.add("수량 열을 확대 재인식했습니다. 원본 확인 필요");
                    for(var diagnostic:trimmed.diagnostics){
                        String old=diagnostic.optString("old"),next=diagnostic.optString("new");
                        if(diagnostic.optBoolean("accepted") && !old.equals(next))
                            r.warnings.add("부분 재인식: "+old+" → "+next+" (원본 확인 필요)");
                    }
                    String backendDesc=engine.korean.backend!=null?" ("+engine.korean.backend.description+")":"";
                    r.method="Paddle 한국어 OCR"+backendDesc+" · "+r.method;
                    return r;
                    }
                }
            } catch(LinkageError failure) {
                throw new IllegalStateException("Paddle native engine is unavailable",failure);
            } finally {if(page!=null)page.recycle();}
        });
    }
}
