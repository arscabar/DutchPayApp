package kr.dutchpay;

import java.util.*;

final class NumericAlignment {
    // Match nearby observed boxes in reading order, allowing gaps in either column.
    static Map<OcrWord,OcrWord> match(List<OcrWord> anchors,List<OcrWord> cells,float shift){
        Map<OcrWord,OcrWord> result=new HashMap<>();
        if(anchors.size()>160 || cells.size()>160)return result;
        List<OcrWord> a=new ArrayList<>(anchors),b=new ArrayList<>(cells);
        a.sort(Comparator.comparingDouble(OcrWord::y));b.sort(Comparator.comparingDouble(OcrWord::y));
        double[][] scores=new double[a.size()+1][b.size()+1];byte[][] step=new byte[a.size()+1][b.size()+1];
        for(int i=1;i<=a.size();i++)for(int j=1;j<=b.size();j++){
            scores[i][j]=scores[i-1][j];step[i][j]=1;
            if(scores[i][j-1]>scores[i][j]){scores[i][j]=scores[i][j-1];step[i][j]=2;}
            OcrWord p=a.get(i-1),q=b.get(j-1);double tolerance=Math.max(p.box.height(),q.box.height())*.9;
            double distance=Math.abs(p.y()-q.y()-shift);
            double paired=scores[i-1][j-1]+1000-distance/Math.max(1,tolerance);
            if(distance<=tolerance && paired>scores[i][j]){scores[i][j]=paired;step[i][j]=3;}
        }
        int i=a.size(),j=b.size();
        while(i>0 && j>0){
            if(step[i][j]==3){result.put(b.get(--j),a.get(--i));}
            else if(step[i][j]==1)i--;else j--;
        }
        return result;
    }
}
