package kr.dutchpay;

import android.content.Context;
import android.graphics.Bitmap;
import ai.onnxruntime.*;
import org.json.JSONArray;
import java.nio.FloatBuffer;
import java.util.Collections;

final class KoreanModel implements AutoCloseable {
    static final class Reading {
        final String text; final double confidence;
        Reading(String text,double confidence){this.text=text;this.confidence=confidence;}
    }
    final OrtEnvironment env=OrtEnvironment.getEnvironment();
    final OrtSession session;
    final JSONArray chars;
    final Context context;
    KoreanModel(Context context) throws Exception {
        this(context,"models/korean.onnx","models/characters.json");
    }
    KoreanModel(Context context,String modelPath,String charsPath) throws Exception {
        this.context=context.getApplicationContext();
        try(var model=context.getAssets().open(modelPath);
            var dict=context.getAssets().open(charsPath);
            var options=new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2);
            chars=new JSONArray(new String(bytes(dict),java.nio.charset.StandardCharsets.UTF_8));
            session=env.createSession(bytes(model),options);
        }
    }
    String read(Bitmap source) throws Exception { return readWithConfidence(source).text; }
    Reading readWithConfidence(Bitmap source) throws Exception {
        return decode(infer(source),chars);
    }
    float[][] infer(Bitmap source) throws Exception {
        int w=Math.max(1,(int)Math.ceil(source.getWidth()*48.0/source.getHeight()));
        int width=Math.max(320,w), plane=48*width;
        Bitmap scaled=Bitmap.createScaledBitmap(source,w,48,true);
        int[] pixels=new int[w*48]; scaled.getPixels(pixels,0,w,0,0,w,48);
        if(scaled!=source)scaled.recycle();
        float[] input=new float[plane*3];
        for(int y=0;y<48;y++)for(int x=0;x<w;x++)for(int c=0;c<3;c++)
            input[c*plane+y*width+x]=((pixels[y*w+x]>>(8*c))&255)/127.5f-1;
        try(var tensor=OnnxTensor.createTensor(env,FloatBuffer.wrap(input),new long[]{1,3,48,width});
            var result=session.run(Collections.singletonMap("x",tensor))) {
            return ((float[][][])result.get(0).getValue())[0];
        }
    }
    static Reading decode(float[][] values,JSONArray chars) throws Exception {
        StringBuilder text=new StringBuilder(); int previous=-1,n=0; double score=0;
        for(float[] row:values) {
            if(row.length!=chars.length())throw new IllegalStateException("Character table mismatch");
            int best=0; for(int j=1;j<row.length;j++)if(row[j]>row[best])best=j;
            if(best>0 && best!=previous){text.append(chars.getString(best));score+=row[best];n++;}
            previous=best;
        }
        // CTC score diagnostic, not a calibrated probability of correctness.
        return new Reading(text.toString(),n==0?0:score/n);
    }
    public void close() throws Exception { session.close(); }
    static byte[] bytes(java.io.InputStream in) throws Exception {
        var out=new java.io.ByteArrayOutputStream(); byte[] b=new byte[8192]; int n;
        while((n=in.read(b))!=-1)out.write(b,0,n); return out.toByteArray();
    }
}
