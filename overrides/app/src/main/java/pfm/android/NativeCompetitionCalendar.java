package pfm.android;
import android.content.Context;
import java.io.*;
import java.util.*;

/** Shared fixture definitions; viewing a competition does not advance this calendar. */
final class NativeCompetitionCalendar {
    static final int[] UCL_AT={4,8,11,14,17,19};
    static final int[][] UCL_PAIRS={{0,3,1,2},{2,0,3,1},{0,1,2,3},{1,0,3,2},{3,0,2,1},{0,2,1,3}};
    static int parallelRound(int played,int sourceTotal,String code){int target=code.equals("de")?34:38;return Math.min(target,(played*target+sourceTotal/2)/sourceTotal);}
    static int uclCompleted(int played,int total){int equivalent=(played*38+total/2)/total,n=0;for(int mark:UCL_AT)if(equivalent>=mark)n++;return n;}
    static ArrayList<ArrayList<String>> groups(Context c)throws IOException{
        ArrayList<ArrayList<String>> out=new ArrayList<ArrayList<String>>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open("europe_ucl_groups_2010.tsv"),"UTF-8"))){
            reader.readLine();String line;while((line=reader.readLine())!=null){String[] p=line.split("\t",-1);if(p.length!=2||p[0].length()!=1)throw new IOException("Bad UCL group row");int g=p[0].charAt(0)-'A';if(g<0||g>7)throw new IOException("Bad UCL group");while(out.size()<=g)out.add(new ArrayList<String>());out.get(g).add(p[1]);}
        }
        if(out.size()!=8)throw new IOException("Incomplete UCL groups");for(ArrayList<String> group:out)if(group.size()!=4)throw new IOException("Incomplete UCL group");return out;
    }
}
