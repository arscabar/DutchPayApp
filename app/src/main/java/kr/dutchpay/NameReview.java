package kr.dutchpay;

import android.graphics.Bitmap;
import java.util.*;
import org.json.*;

final class NameReview {
    static JSONArray apply(android.content.Context context,Bitmap page,List<PaddleLine> lines,Receipt receipt,KoreanModel model){
        JSONArray diagnostics=new JSONArray();
        try(var patches=NamePatches.create(page,lines,receipt);var single=new NameSingle();var english=new EnglishReview(context)){
            List<String> others=NameSheet.read(patches.padded);
            for(int n=0;n<patches.items.size();n++){
                Item i=patches.items.get(n);String original=i.name;long start=System.currentTimeMillis();
                var base=model.readWithConfidence(patches.originals.get(n));
                var padded=model.readWithConfidence(patches.padded.get(n));String other=others.get(n);
                JSONArray centers=new JSONArray();
                var center=MenuCenter.read(patches.originals.get(n),patches.scores.get(n),base,original,model,centers);
                for(String s:new String[]{other,padded.text,base.text})if(NameDecision.candidate(original,s)
                    && i.nameCandidates.stream().noneMatch(v->NameDecision.key(v).equals(NameDecision.key(s))))i.nameCandidates.add(s);
                boolean accepted=NameDecision.accept(original,base,padded,other);
                if(center!=null){padded=center;accepted=true;i.nameCandidates.add(center.text);}
                JSONObject cross=new JSONObject();
                if(!accepted){
                    var narrow=MenuViews.center(patches.originals.get(n),model);
                    cross.put("center80",narrow.text).put("score80",narrow.confidence);
                    double score=patches.scores.get(n);
                    boolean a=NameCross.eligible(original,score,base,base,false),b=NameCross.eligible(original,score,base,narrow,true);
                    if(a || b){
                        String alone=single.read(patches.padded.get(n));cross.put("other",alone);
                        if(a && NameCross.agrees(original,base,alone)){padded=base;accepted=true;}
                        else if(b && NameCross.agrees(original,narrow,alone)){padded=narrow;accepted=true;}
                        if(NameDecision.candidate(original,alone) && !i.nameCandidates.contains(alone))i.nameCandidates.add(alone);
                    }
                    cross.put("accepted",accepted);
                }
                if(accepted && !i.nameCandidates.contains(padded.text))i.nameCandidates.add(padded.text);
                if(!i.nameCandidates.isEmpty())i.originalName=original;
                if(accepted){i.name=padded.text;i.warning+=(i.warning.isEmpty()?"":" / ")+"품목명을 재인식했습니다. 원본 확인 필요";}
                var latin=english.read(patches.originals.get(n),i.name,model);
                if(!latin.text.equals(i.name)){
                    i.originalName=original;i.name=latin.text;i.nameCandidates.add(latin.text);accepted=true;
                    i.warning+=(i.warning.isEmpty()?"":" / ")+"영문 영역을 재인식했습니다. 원본 확인 필요";
                }
                var region=RegionRetry.read(patches.originals.get(n),patches.padded.get(n),i.name,model,single);
                if(!region.text.equals(i.name)){
                    i.originalName=original;i.name=region.text;accepted=true;
                    if(!i.nameCandidates.contains(region.text))i.nameCandidates.add(region.text);
                    i.warning+=(i.warning.isEmpty()?"":" / ")+"한글 후보를 두 이미지 보정본으로 확인했습니다. 원본 확인 필요";
                }
                diagnostics.put(new JSONObject().put("old",original).put("base",base.text).put("padded",padded.text)
                    .put("other",other).put("baseScore",base.confidence).put("paddedScore",padded.confidence)
                    .put("centers",centers)
                    .put("cross",cross)
                    .put("region",region.diagnostics)
                    .put("english",latin.diagnostics).put("final",i.name)
                    .put("accepted",accepted).put("milliseconds",System.currentTimeMillis()-start));
            }
        }catch(Exception failure){receipt.warnings.add("품목명 보완 읽기를 완료하지 못했습니다. 기존 결과를 확인하세요");}
        receipt.nameDiagnostics=diagnostics.toString();receipt.validate();return diagnostics;
    }
}
