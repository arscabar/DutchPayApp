package kr.dutchpay;

import java.text.Normalizer;

final class NameDecision {
    static String key(String text){return Normalizer.normalize(text,Normalizer.Form.NFC).replaceAll("[^\\p{L}\\p{N}]","");}
    // Only layout whitespace and a leading receipt option marker are ignorable evidence.
    static String evidenceText(String text){return Normalizer.normalize(text,Normalizer.Form.NFC)
        .replaceAll("\\s","").replaceFirst("^(?:[▶►→]+|-(?=[가-힣]))","");}
    static boolean sameText(String a,String b){return evidenceText(a).equals(evidenceText(b));}
    static boolean candidate(String original,String next){
        String a=key(original),b=key(next);
        return b.matches(".*[가-힣].*") && b.length()>=Math.max(2,a.length()*.7)
            && b.length()<=a.length()*1.3+2 && !a.equals(b);
    }
    static boolean sameNumbers(String a,String b){return a.replaceAll("[^0-9+.,/%×~−-]","").equals(b.replaceAll("[^0-9+.,/%×~−-]",""));}
    static boolean accept(String original,KoreanModel.Reading base,KoreanModel.Reading padded,String other){
        // Scores are one model's CTC diagnostics; other engine scores are never compared.
        return candidate(original,padded.text) && sameNumbers(original,padded.text) && key(base.text).equals(key(original))
            && sameText(padded.text,other) && base.confidence<.95
            && padded.confidence>=.97 && padded.confidence>=base.confidence+.05;
    }
}
