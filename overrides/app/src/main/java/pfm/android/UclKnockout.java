package pfm.android;

import java.util.*;

/** Pure 2010/11 tournament rules. Frozen draws and results live in the match journal. */
final class UclKnockout {
    static final String COMP="ucl:2010";
    static final int[] AT={23,26,29,31,33,35,38};
    static final String[] STAGES={"ROUND OF 16","QUARTER-FINALS","SEMI-FINALS","FINAL"};
    static final class Row {String id;int played,points,gf,ga;Row(String id){this.id=id;}}
    static ArrayList<Row> table(List<String> ids,List<MatchJournal.Match> matches){
        HashMap<String,Row> rows=new HashMap<String,Row>();for(String id:ids)rows.put(id,new Row(id));
        for(MatchJournal.Match m:matches)if(m.round<=6&&rows.containsKey(m.home)&&rows.containsKey(m.away)){Row h=rows.get(m.home),a=rows.get(m.away);h.played++;a.played++;h.gf+=m.homeGoals;h.ga+=m.awayGoals;a.gf+=m.awayGoals;a.ga+=m.homeGoals;h.points+=m.homeGoals>m.awayGoals?3:m.homeGoals==m.awayGoals?1:0;a.points+=m.awayGoals>m.homeGoals?3:m.homeGoals==m.awayGoals?1:0;}
        ArrayList<Row> sorted=new ArrayList<Row>(rows.values());Collections.sort(sorted,(a,b)->b.points-a.points);
        for(int first=0;first<sorted.size();){int end=first+1;while(end<sorted.size()&&sorted.get(end).points==sorted.get(first).points)end++;
            final HashMap<String,int[]> mini=new HashMap<String,int[]>();for(int i=first;i<end;i++)mini.put(sorted.get(i).id,new int[3]);
            for(MatchJournal.Match m:matches)if(m.round<=6&&mini.containsKey(m.home)&&mini.containsKey(m.away)){int[] h=mini.get(m.home),a=mini.get(m.away);h[0]+=m.homeGoals>m.awayGoals?3:m.homeGoals==m.awayGoals?1:0;a[0]+=m.awayGoals>m.homeGoals?3:m.homeGoals==m.awayGoals?1:0;h[1]+=m.homeGoals-m.awayGoals;a[1]+=m.awayGoals-m.homeGoals;a[2]+=m.awayGoals;}
            Collections.sort(sorted.subList(first,end),(a,b)->{int[] x=mini.get(a.id),y=mini.get(b.id);for(int i=0;i<3;i++)if(x[i]!=y[i])return y[i]-x[i];int d=(b.gf-b.ga)-(a.gf-a.ga);if(d!=0)return d;if(a.gf!=b.gf)return b.gf-a.gf;return a.id.compareTo(b.id);});first=end;
        }return sorted;
    }
    static String key(int round,String home,String away){return COMP+"|"+round+"|"+home+"|"+away;}
    /** Result is relative to the second-leg home side, including extra-time away goals. */
    static int decision(int firstHome,int firstAway,int secondHome,int secondAway){int home=firstAway+secondHome,away=firstHome+secondAway;if(home!=away)return home>away?1:-1;if(firstAway!=secondAway)return firstAway>secondAway?1:-1;return 0;}
    static ArrayList<String[]> draw(MatchJournal j,List<? extends List<String>> groups,Map<String,String> associations,int seed,int stage){
        String stored=j.metadata("ucl-draw-"+stage);if(stored!=null)return decode(stored);
        ArrayList<String[]> pairs=new ArrayList<String[]>();ArrayList<MatchJournal.Match> matches=j.competition(COMP);
        if(stage==0){ArrayList<String> winners=new ArrayList<String>(),seconds=new ArrayList<String>();HashMap<String,Integer> group=new HashMap<String,Integer>();
            for(int i=0;i<groups.size();i++){ArrayList<Row> rows=table(groups.get(i),matches);for(Row row:rows)if(row.played!=6)return pairs;winners.add(rows.get(0).id);seconds.add(rows.get(1).id);for(String id:groups.get(i))group.put(id,i);}
            Collections.shuffle(winners,new Random(seed));Collections.shuffle(seconds,new Random(seed^0x51ed270b));
            if(!pair(winners,seconds,group,associations,0,new HashSet<String>(),pairs))throw new IllegalStateException("No valid UCL last-16 draw");
        }else{
            ArrayList<String[]> previous=draw(j,groups,associations,seed,stage-1);if(previous.isEmpty())return pairs;
            ArrayList<String> qualified=new ArrayList<String>();int round=stage*2+6;
            for(String[] tie:previous){MatchJournal.Match m=j.get(key(round,tie[1],tie[0]));if(m==null||m.winner.length()==0)return pairs;qualified.add(m.winner);}
            Collections.shuffle(qualified,new Random(seed^stage*104729));for(int i=0;i<qualified.size();i+=2)pairs.add(new String[]{qualified.get(i),qualified.get(i+1)});
        }
        StringBuilder encoded=new StringBuilder();for(String[] pair:pairs)encoded.append(pair[0]).append(',').append(pair[1]).append(';');j.define("ucl-draw-"+stage,encoded.toString());return pairs;
    }
    private static boolean pair(List<String> winners,List<String> seconds,Map<String,Integer> groups,Map<String,String> associations,int at,Set<String> used,ArrayList<String[]> out){
        if(at==winners.size())return true;String winner=winners.get(at);
        for(String second:seconds)if(!used.contains(second)&&!groups.get(winner).equals(groups.get(second))&&!Objects.equals(associations.get(winner),associations.get(second))){used.add(second);out.add(new String[]{second,winner});if(pair(winners,seconds,groups,associations,at+1,used,out))return true;out.remove(out.size()-1);used.remove(second);}return false;
    }
    static ArrayList<String[]> decode(String value){ArrayList<String[]> pairs=new ArrayList<String[]>();if(value!=null)for(String tie:value.split(";"))if(tie.length()>0)pairs.add(tie.split(","));return pairs;}
    static int completed(int played,int total){int equivalent=(played*38+total/2)/total,n=0;for(int mark:AT)if(equivalent>=mark)n++;return n;}
}
