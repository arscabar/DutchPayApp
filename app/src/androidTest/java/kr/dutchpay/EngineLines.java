package kr.dutchpay;

import android.graphics.*;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import org.json.*;
import java.util.concurrent.TimeUnit;

final class EngineLines {
    static JSONArray read(Bitmap page, Text text, TextRecognizer ml, KoreanModel paddle,
            boolean padded) throws Exception {
        JSONArray lines=new JSONArray();
        for(var block:text.getTextBlocks())for(var line:block.getLines()) {
            Rect r=line.getBoundingBox(); if(r==null)continue;
            JSONObject value=new JSONObject().put("ml",line.getText());
            if(padded) {
                // ponytail: global margin; curved baselines still need a better detector.
                int dx=(int)Math.ceil(r.height()*.5), dy=(int)Math.ceil(r.height()*.15);
                int l=Math.max(0,r.left-dx), t=Math.max(0,r.top-dy);
                Bitmap crop=Bitmap.createBitmap(page,l,t,Math.min(page.getWidth(),r.right+dx)-l,
                    Math.min(page.getHeight(),r.bottom+dy)-t);
                value.put("paddlePadded",paddle.read(crop)); crop.recycle();
            } else {
                Bitmap box=LineCrop.create(page,line,false), flat=LineCrop.create(page,line,true);
                float factor=Math.max(1,32f/Math.min(flat.getWidth(),flat.getHeight()));
                Bitmap input=Bitmap.createScaledBitmap(flat,(int)Math.ceil(flat.getWidth()*factor),
                    (int)Math.ceil(flat.getHeight()*factor),true);
                var retry=Tasks.await(ml.process(InputImage.fromBitmap(input,0)),120,TimeUnit.SECONDS);
                if(input!=flat)input.recycle();
                value.put("mlFlat",retry.getText()).put("paddleBox",paddle.read(box))
                    .put("paddleFlat",paddle.read(flat));
                box.recycle(); flat.recycle();
            }
            lines.put(value);
        }
        return lines;
    }
}
