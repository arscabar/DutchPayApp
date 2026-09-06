package kr.dutchpay;

import android.graphics.*;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import java.util.*;

final class RegionalOcr {
    static Task<List<OcrWord>> read(Bitmap page,Text first,TextRecognizer engine) {
        List<Text.Line> lines=new ArrayList<>(); List<Integer> heights=new ArrayList<>();
        for(Text.TextBlock block:first.getTextBlocks()) for(Text.Line line:block.getLines()) {
            Rect box=line.getBoundingBox();
            if(box==null || box.isEmpty() || box.height()>box.width()*1.4) continue;
            lines.add(line); heights.add(box.height());
        }
        Task<List<OcrWord>> task=Tasks.forResult(OcrWord.words(first));
        if(heights.isEmpty()) return task;
        Collections.sort(heights);
        double median=(heights.get((heights.size()-1)/2)+heights.get(heights.size()/2))/2.0;
        int[] tried={0};
        for(Text.Line line:lines) if(line.getBoundingBox().height()>=median*1.6)
            task=task.continueWithTask(previous -> {
                List<OcrWord> words=previous.getResult();
                if(tried[0]>=3) return Tasks.forResult(words);
                TextPatch candidate=null; List<Rect> bands;
                try {
                    candidate=TextPatch.create(page,line.getCornerPoints());
                    if(candidate==null) return Tasks.forResult(words);
                    bands=LineBands.split(candidate.image);
                } catch(RuntimeException failure) {
                    if(candidate!=null) candidate.close(); return Tasks.forResult(words);
                }
                TextPatch patch=candidate;
                if(bands.size()<2) { patch.close(); return Tasks.forResult(words); }
                tried[0]++;
                Task<List<OcrWord>> retry=Tasks.forResult(new ArrayList<>());
                for(Rect band:bands) retry=retry.continueWithTask(prior -> {
                    if(!prior.isSuccessful() || prior.getResult()==null) return Tasks.forResult(null);
                    return band(patch,band,engine,prior.getResult());
                });
                return retry.continueWith(result -> {
                    try {
                        List<OcrWord> added=result.isSuccessful()?result.getResult():null;
                        if(added!=null && added.stream().mapToInt(w -> count(w.text)).sum()>=count(line.getText())) {
                            words.removeIf(patch::contains); words.addAll(added);
                            words.sort(Comparator.comparingDouble(OcrWord::y));
                        }
                        return words;
                    } finally { patch.close(); }
                });
            });
        return task;
    }

    private static Task<List<OcrWord>> band(TextPatch patch,Rect band,TextRecognizer engine,List<OcrWord> words) {
        Bitmap crop=Bitmap.createBitmap(patch.image,band.left,band.top,band.width(),band.height());
        try {
            return engine.process(InputImage.fromBitmap(crop,0)).continueWith(task -> {
                crop.recycle();
                if(!task.isSuccessful() || count(task.getResult().getText())==0) return null;
                List<OcrWord> found=OcrWord.words(task.getResult());
                if(found.isEmpty()) return null;
                for(OcrWord word:found) {
                    Rect box=patch.map(word.box,band.top);
                    if(box==null) return null;
                    words.add(new OcrWord(word.text,box));
                }
                return words;
            });
        } catch(RuntimeException failure) { crop.recycle(); return Tasks.forResult(null); }
    }

    private static int count(String text) { return text.replaceAll("\\s","").length(); }
}
