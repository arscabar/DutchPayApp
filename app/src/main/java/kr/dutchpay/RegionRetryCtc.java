package kr.dutchpay;

import java.util.*;
import org.json.JSONArray;

/** First-emission CTC evidence, with exact probabilities retained for comparisons. */
final class RegionRetryCtc {
    static final class Glyph {
        final String text,alternative;final int best,second;final float[] values;
        Glyph(float[] row,int a,int b,JSONArray chars) throws Exception {
            values=row;best=a;second=b;text=chars.getString(a);alternative=chars.getString(b);
        }
    }
    final String text;final List<Glyph> glyphs;
    RegionRetryCtc(String text,List<Glyph> glyphs){this.text=text;this.glyphs=glyphs;}
    static RegionRetryCtc read(float[][] values,JSONArray chars) throws Exception {
        StringBuilder text=new StringBuilder();List<Glyph> glyphs=new ArrayList<>();int previous=-1;
        for(float[] row:values){
            if(row.length!=chars.length())throw new IllegalStateException("Character table mismatch");
            int best=0;for(int j=1;j<row.length;j++)if(row[j]>row[best])best=j;
            if(best>0 && best!=previous){
                String token=chars.getString(best);
                if(token.length()!=1)throw new IllegalStateException("Unsupported CTC token length");
                if(!token.matches("\\s")){
                    int second=0;for(int j=1;j<row.length;j++)if(j!=best && row[j]>row[second])second=j;
                    text.append(token);glyphs.add(new Glyph(row,best,second,chars));
                }
            }
            previous=best;
        }
        String compact=text.toString(),clean=NameDecision.evidenceText(compact);
        int skip=compact.length()-clean.length();
        if(skip<0 || !compact.substring(skip).equals(clean))throw new IllegalStateException("Unsupported CTC normalization");
        return new RegionRetryCtc(clean,new ArrayList<>(glyphs.subList(skip,glyphs.size())));
    }
}
