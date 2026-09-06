package kr.dutchpay;

import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;

final class Scan {
    static Task<Receipt> read(Context context,Uri uri,TextRecognizer engine) throws java.io.IOException {
        return read(context,uri,engine,text -> {});
    }
    static Task<Receipt> read(Context context,Uri uri,TextRecognizer engine,
            java.util.function.Consumer<Text> inspect) throws java.io.IOException {
        InputImage image=InputImage.fromFilePath(context,uri);Bitmap page=image.getBitmapInternal();
        return engine.process(image).continueWithTask(first -> {
            if(!first.isSuccessful()){page.recycle();throw first.getException();}
            return parsed(page,first.getResult(),engine).continueWithTask(parsed -> {
                if(!parsed.isSuccessful()){page.recycle();throw parsed.getException();}
                Receipt original=parsed.getResult();Bitmap crop;
                try{crop=ReceiptCrop.create(page,first.getResult());}
                catch(RuntimeException failure){page.recycle();return Tasks.forResult(original);}
                if(page!=crop)page.recycle();
                if(crop==null)return Tasks.forResult(original);
                return engine.process(InputImage.fromBitmap(crop,0)).continueWithTask(second -> {
                    if(!second.isSuccessful()){crop.recycle();return Tasks.forResult(original);}
                    inspect.accept(second.getResult());
                    return parsed(crop,second.getResult(),engine).continueWith(last -> {
                        crop.recycle();if(!last.isSuccessful())return original;
                        Receipt improved=last.getResult();improved.supplyTotal(original.total);
                        if(ReceiptChoice.balanced(original) && !ReceiptChoice.balanced(improved)
                            || improved.items.isEmpty() && !original.items.isEmpty())return original;
                        improved.originalRaw=original.originalRaw;
                        improved.method="crop1600 + "+improved.method;return improved;
                    });
                });
            });
        });
    }
    private static Task<Receipt> parsed(Bitmap page,Text text,TextRecognizer engine) {
        return RegionalOcr.read(page,text,engine).continueWith(task -> {
            var words=WordGeometry.from(text,task.isSuccessful()?task.getResult():OcrWord.words(text));
            String raw=String.join("\n",ColumnRows.group(words).stream().map(ColumnRows::text).toArray(String[]::new));
            Receipt r=ReceiptChoice.parse(words,raw);r.originalRaw=String.join("\n",Layout.rows(text));return r;
        });
    }
}
