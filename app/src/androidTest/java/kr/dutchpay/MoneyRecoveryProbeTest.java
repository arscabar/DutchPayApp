package kr.dutchpay;

import android.test.InstrumentationTestCase;
import android.net.Uri;
import com.google.android.gms.tasks.Tasks;
import java.io.*;
import java.util.concurrent.TimeUnit;
import org.json.*;

public class MoneyRecoveryProbeTest extends InstrumentationTestCase {
    public void testActualSignedOptionsCouponsAndPrintedUnits() throws Exception {
        var target=getInstrumentation().getTargetContext();JSONArray results=new JSONArray();boolean correct=true;
        String[] files={"02.jpg","Scan_20260514_210346.jpg","Scan_20260806_190801.jpg","Scan_20260825_121832.jpg",
            "Scan_20260515_190736.jpg","Scan_20260721_190550.jpg","Scan_20260728_193528.jpg"};
        long[] totals={20000,51000,9400,39700,9400,10000,10000},coupons={0,0,2200,5000,2500,3200,4000};
        int[] rows={1,2,4,12,4,4,4};
        for(int n=0;n<files.length;n++){
            String name=files[n];File copy=File.createTempFile("money-recovery-",".jpg",target.getCacheDir());
            try{
                try(InputStream in=name.startsWith("Scan_")?new FileInputStream(new File(target.getExternalFilesDir(null),"scans/"+name)):
                    getInstrumentation().getContext().getAssets().open("receipts/"+name);OutputStream out=new FileOutputStream(copy)){
                    out.write(KoreanModel.bytes(in));
                }
                Receipt r=Tasks.await(PaddleScan.read(target,Uri.fromFile(copy)),120,TimeUnit.SECONDS);
                JSONArray items=new JSONArray();for(Item i:r.items)items.put(new JSONObject().put("name",i.name)
                    .put("unit",i.unit).put("count",i.count).put("quantityKnown",i.quantityKnown).put("printedTotal",i.printedTotal)
                    .put("baseTotal",i.baseAmount()).put("includedDiscount",i.includedDiscount).put("amountBased",i.amountBased).put("warning",i.warning));
                results.put(new JSONObject().put("file",name).put("items",items).put("rows",r.raw).put("total",r.total)
                    .put("sum",r.itemSum()).put("warnings",new JSONArray(r.warnings)));
                correct&=r.items.size()==rows[n] && r.total!=null && r.total==totals[n] && r.itemSum()==totals[n];
                if(n<2 && !r.items.isEmpty())correct&=r.items.get(0).unit==10000 && r.items.get(0).count==(n==0?2:4);
                if(n==0 && r.items.size()==1)correct&=r.items.get(0).name.replaceAll("\\s","").equals("소고기국밥");
                if(n==1 && r.items.size()==2)correct&=r.items.get(0).name.replaceAll("\\s","").equals("들기름막국수")
                    && r.items.get(1).name.replaceAll("\\s","").equals("육전(물)막국수");
                if(n>=2){
                    correct&=r.items.stream().filter(i->i.count<0 && !i.includedDiscount).count()==(n==3?3:1);
                    final long coupon=coupons[n];
                    correct&=r.items.stream().filter(i->i.printedTotal==coupon && i.baseAmount()==-coupon && i.count==1).count()==1;
                }
            }finally{copy.delete();}
            try(var out=new OutputStreamWriter(new FileOutputStream(new File(target.getExternalFilesDir(null),"recovery-money-probe.json")),"UTF-8")){
                out.write(results.toString(2));
            }
        }
        assertTrue("Actual printed quantities, units, removals and coupon totals",correct);
    }
}
