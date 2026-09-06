package kr.dutchpay;

import android.graphics.*;
import com.google.mlkit.vision.text.Text;
import java.util.*;

final class ReceiptCrop {
    static Bitmap create(Bitmap image, Text text) {
        List<Text.Line> lines=new ArrayList<>();
        for(Text.TextBlock b:text.getTextBlocks()) lines.addAll(b.getLines());
        int top=image.getHeight(), bottom=image.getHeight(), left=image.getWidth(), right=0, pad=15;
        for(Text.Line l:lines) {
            Rect r=l.getBoundingBox(); if(r==null) continue;
            left=Math.min(left,r.left); right=Math.max(right,r.right);
            String s=l.getText().replaceAll("\\s","");
            if(s.matches(".*(상품명|삼품명|품명|POS[-:]?0?1|대기번호).*")) {
                top=Math.min(top,r.top); pad=Math.max(pad,r.height());
            }
        }
        if(top==image.getHeight()) return null;
        for(Text.Line l:lines) {
            Rect r=l.getBoundingBox(); if(r==null || r.top<=top+pad*2) continue;
            String s=l.getText().replaceAll("\\s","");
            if(s.matches(".*(과세|결[제재]구분|신용승인).*")) bottom=Math.min(bottom,r.top+pad);
        }
        top=Math.max(0,top-pad); bottom=Math.min(image.getHeight(),bottom);
        left=Math.max(0,left-pad); right=Math.min(image.getWidth(),right+pad);
        if(right<=left || bottom<=top) return null;
        Bitmap crop=Bitmap.createBitmap(image,left,top,right-left,bottom-top);
        // ponytail: fixed working width; page dewarping if curved receipts remain unreliable.
        int width=1600, height=Math.max(1,Math.round(crop.getHeight()*1600f/crop.getWidth()));
        Bitmap scaled=Bitmap.createScaledBitmap(crop,width,height,true);
        if(crop!=image && crop!=scaled) crop.recycle();
        return scaled;
    }
}
