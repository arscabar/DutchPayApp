package kr.dutchpay;

final class NameCross {
    static boolean eligible(String old,double original,KoreanModel.Reading base,KoreanModel.Reading candidate,boolean cropped){
        if(candidate.confidence<.9 || !NameDecision.candidate(old,candidate.text)
            || !NameDecision.sameNumbers(old,candidate.text))return false;
        boolean protectedName=Double.isFinite(original) && original>=.97
            || NameDecision.key(base.text).equals(NameDecision.key(old)) && base.confidence>=.97;
        return !protectedName || cropped && insertion(old,candidate.text);
    }
    static boolean insertion(String old,String next){
        String a=old.replaceAll("\\s",""),b=next.replaceAll("\\s","");int i=0,added=0;
        for(char c:b.toCharArray()){
            if(i<a.length() && c==a.charAt(i))i++;
            else if(c>='가' && c<='힣')added++;
            else return false;
        }
        return i==a.length() && added>=1 && added<=2;
    }
    static boolean agrees(String old,KoreanModel.Reading candidate,String other){
        return NameDecision.candidate(old,other) && NameDecision.sameNumbers(old,other)
            && NameDecision.sameText(candidate.text,other);
    }
}
