package kr.dutchpay;

import org.json.*;
import java.util.*;

final class RegionRetryTrace {
    static JSONArray tokens(float[][] values,JSONArray chars) throws Exception {
        JSONArray out=new JSONArray();int previous=-1;
        for(int t=0;t<values.length;t++){
            float[] row=values[t];int best=0;
            for(int j=1;j<row.length;j++)if(row[j]>row[best])best=j;
            if(best>0 && best!=previous){
                int[] indices=new int[Math.min(5,row.length)];Arrays.fill(indices,-1);
                for(int j=0;j<row.length;j++)for(int k=0;k<indices.length;k++){
                    if(indices[k]<0 || Float.compare(row[j],row[indices[k]])>0){
                        System.arraycopy(indices,k,indices,k+1,indices.length-k-1);indices[k]=j;break;
                    }
                }
                JSONArray top=new JSONArray();
                for(int index:indices)top.put(new JSONObject().put("text",chars.getString(index)).put("score",row[index]));
                out.put(new JSONObject().put("text",chars.getString(best)).put("first",t).put("last",t)
                    .put("score",row[best]).put("alternatives",top));
            }else if(best>0 && out.length()>0)out.getJSONObject(out.length()-1).put("last",t);
            previous=best;
        }
        return out;
    }
}
