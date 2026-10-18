package pfm.android;

import android.content.Context;
import java.io.*;
import java.util.*;

/** Recruitment weights and conservative spending limits; no writes to career finances. */
final class NativeTransferMarketModel {
    static final class Profile {String country;int finance;}
    private final HashMap<String,Profile> profiles=new HashMap<String,Profile>();
    private final HashMap<String,Double> domestic=new HashMap<String,Double>();
    private final HashMap<String,LinkedHashMap<String,Double>> affinity=new HashMap<String,LinkedHashMap<String,Double>>();
    private final HashMap<String,Double> geography=new HashMap<String,Double>();
    private final ArrayList<NativeCareerStore.Club> destinations;
    NativeTransferMarketModel(Context context,ArrayList<NativeCareerStore.Club> destinations)throws IOException{
        this.destinations=destinations;
        try(BufferedReader r=reader(context,"europe_clubs_v72.tsv")){r.readLine();String s;while((s=r.readLine())!=null){String[] q=s.split("\t",-1);Profile p=new Profile();p.country=q[1];p.finance=Integer.parseInt(q[8]);profiles.put(q[2],p);}}
        try(BufferedReader r=reader(context,"transfer_domestic_share_v72.csv")){r.readLine();String s;while((s=r.readLine())!=null){String[] q=s.split(",");domestic.put(q[0],Double.parseDouble(q[1])/100.0);}}
        try(BufferedReader r=reader(context,"transfer_international_affinity_v72.csv")){r.readLine();String s;while((s=r.readLine())!=null){String[] q=s.split(",");LinkedHashMap<String,Double> row=affinity.get(q[0]);if(row==null){row=new LinkedHashMap<String,Double>();affinity.put(q[0],row);}row.put(q[1],Double.parseDouble(q[2])/100.0);}}
    }
    private static BufferedReader reader(Context c,String asset)throws IOException{return new BufferedReader(new InputStreamReader(c.getAssets().open(asset),"UTF-8"));}
    private Profile profile(NativeCareerStore.Club c){return c.league.startsWith("eu_")?profiles.get(c.name):null;}
    String country(NativeCareerStore.Club c){
        Profile p=profile(c);if(p!=null)return p.country;
        if(c.league.equals("en"))return "England";if(c.league.equals("es"))return "Spain";if(c.league.equals("it"))return "Italy";if(c.league.equals("de"))return "Germany";if(c.league.equals("fr"))return "France";
        throw new IllegalStateException("Missing transfer profile: "+c.name);
    }
    private static String region(String country){
        if(Arrays.asList("Denmark","Norway","Sweden","Finland").contains(country))return "Scandinavia";
        if(Arrays.asList("Croatia","Serbia","Slovenia","Bulgaria","Greece").contains(country))return "Balkans";
        if(Arrays.asList("Russia","Ukraine","Belarus","Moldova","Romania","Poland","Czech Republic","Slovakia","Hungary","Azerbaijan").contains(country))return "Eastern Europe";
        return country;
    }
    private static boolean matches(String group,String country){
        for(String part:group.split(" / "))if(part.equals(country)||part.equals(region(country))||part.equals("Other Scandinavia")&&region(country).equals("Scandinavia"))return true;
        return false;
    }
    private LinkedHashMap<String,Double> row(String country){
        LinkedHashMap<String,Double> row=affinity.get(country);if(row==null)row=affinity.get(region(country));
        // The supplied draft has no dedicated origin rows for these countries.
        if(row==null)row=affinity.get(country.equals("Scotland")||country.equals("Ireland")?"England":"Eastern Europe");
        return row;
    }
    private double domesticShare(String country){
        Double d=domestic.get(country);if(d==null)d=domestic.get(region(country));
        if(d==null&&region(country).equals("Eastern Europe"))d=domestic.get("Russia / Eastern Europe");
        return d==null?0.65:d;
    }
    private String bucket(String source,String destination){
        if(source.equals(destination))return "DOMESTIC";
        for(String group:row(source).keySet())if(!group.equals("Other Europe")&&!group.equals("Outside Europe")&&matches(group,destination))return group;
        return "Other Europe";
    }
    double geography(NativeCareerStore.Club seller,NativeCareerStore.Club buyer){
        String source=country(seller),target=country(buyer),key=source+":"+target;
        Double cached=geography.get(key);if(cached!=null)return cached;
        String bucket=bucket(source,target);int count=0;
        for(NativeCareerStore.Club c:destinations)if(bucket(source,country(c)).equals(bucket))count++;
        double share=domesticShare(source),weight;
        if(bucket.equals("DOMESTIC"))weight=share;
        else {Double conditional=row(source).get(bucket);weight=(1-share)*(conditional==null?0.05:conditional);}
        weight/=Math.max(1,count);geography.put(key,weight);return weight;
    }
    double activity(NativeCareerStore.Club club,long payroll){
        Profile p=profile(club);
        if(p==null)return Math.max(0.7,Math.min(4,Math.sqrt(payroll/2500000.0)));
        double[] base={0,0.16,0.32,0.60,1.25,2.0};
        return base[Math.max(1,Math.min(5,p.finance))]*Math.max(0.5,Math.min(2,Math.sqrt(payroll/2000000.0)));
    }
    int feeLimit(NativeCareerStore.Club club){
        Profile p=profile(club);if(p==null)return Integer.MAX_VALUE;
        int[] limit={0,600000,1500000,4000000,12000000,25000000};return limit[Math.max(1,Math.min(5,p.finance))];
    }
    int wageLimit(NativeCareerStore.Club club){
        Profile p=profile(club);if(p==null)return Integer.MAX_VALUE;
        int[] limit={0,120000,300000,700000,1800000,4000000};return limit[Math.max(1,Math.min(5,p.finance))];
    }
}
