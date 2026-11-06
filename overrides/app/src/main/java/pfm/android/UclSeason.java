package pfm.android;

import java.util.*;

/** Persisted qualification and group draw for recurring, fictional career seasons. */
final class UclSeason {
    static double value(Map<String,Double> values,String id){Double value=values.get(id);return value==null?0:value;}
    static String competition(int year){return "ucl:"+year;}
    // Fixed career-era allocation, shared with the qualification screen; qualifying rounds are abstracted.
    static int slots(String code){return EuropeanPlaces.champions(code);}
    static final class Standing {String id;int played,points,gf,ga;Standing(String id){this.id=id;}}
    static ArrayList<Standing> standings(MatchJournal journal,String code,List<NativeCareerStore.Club> clubs){
        HashMap<String,Standing> rows=new HashMap<String,Standing>();for(NativeCareerStore.Club club:clubs)if(club.league.equals(code))rows.put(club.id,new Standing(club.id));
        for(MatchJournal.Match m:journal.competition("league:"+code)){Standing h=rows.get(m.home),a=rows.get(m.away);if(h==null||a==null)continue;h.played++;a.played++;h.gf+=m.homeGoals;h.ga+=m.awayGoals;a.gf+=m.awayGoals;a.ga+=m.homeGoals;h.points+=m.homeGoals>m.awayGoals?3:m.homeGoals==m.awayGoals?1:0;a.points+=m.awayGoals>m.homeGoals?3:m.homeGoals==m.awayGoals?1:0;}
        ArrayList<Standing> out=new ArrayList<Standing>(rows.values());Collections.sort(out,(a,b)->{if(a.points!=b.points)return b.points-a.points;int d=(b.gf-b.ga)-(a.gf-a.ga);if(d!=0)return d;if(a.gf!=b.gf)return b.gf-a.gf;return a.id.compareTo(b.id);});return out;
    }
    static HashMap<String,Double> strengths(NativeCareerStore.Career career){
        HashMap<String,ArrayList<Integer>> squads=new HashMap<String,ArrayList<Integer>>();for(NativeCareerStore.Player p:career.players){ArrayList<Integer> ratings=squads.get(p.owner);if(ratings==null){ratings=new ArrayList<Integer>();squads.put(p.owner,ratings);}ratings.add(p.qua);}
        HashMap<String,Double> out=new HashMap<String,Double>();for(Map.Entry<String,ArrayList<Integer>> entry:squads.entrySet()){Collections.sort(entry.getValue(),Collections.reverseOrder());double sum=0;int n=Math.min(11,entry.getValue().size());for(int i=0;i<n;i++)sum+=entry.getValue().get(i);out.put(entry.getKey(),n==0?0:sum/n);}return out;
    }
    static String champion(MatchJournal previous){for(MatchJournal.Match m:previous.competition(competition(previous.season())))if(m.round==13&&m.winner.length()>0)return m.winner;return "";}
    static ArrayList<ArrayList<String>> prepare(MatchJournal journal,MatchJournal previous,NativeCareerStore.Career career,int seed){
        String frozen=journal.metadata("ucl-groups");if(frozen!=null)return decode(frozen);
        HashMap<String,Double> strengths=strengths(career);HashMap<String,String> associations=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)associations.put(club.id,club.league);
        String champion=champion(previous);ArrayList<String> entrants=new ArrayList<String>();boolean estimated=false;
        for(String code:new String[]{"en","es","it","de","fr"}){
            ArrayList<Standing> table=standings(previous,code,career.clubs);int total=code.equals("de")?34:38;boolean complete=table.size()==(code.equals("de")?18:20);for(Standing row:table)if(row.played!=total)complete=false;
            if(!complete){estimated=true;Collections.sort(table,(a,b)->{int d=Double.compare(value(strengths,b.id),value(strengths,a.id));return d!=0?d:a.id.compareTo(b.id);});}
            int places=slots(code);for(int i=0;i<places;i++)entrants.add(table.get(i).id);
            if(code.equals(associations.get(champion))&&!entrants.contains(champion)){entrants.remove(entrants.size()-1);entrants.add(champion);}
        }
        // Other domestic leagues are not yet simulated: external places are explicitly estimated.
        HashMap<String,Double> performance=new HashMap<String,Double>();for(MatchJournal.Match m:previous.competition(competition(previous.season()))){performance.put(m.home,value(performance,m.home)+(m.homeGoals>m.awayGoals?3:m.homeGoals==m.awayGoals?1:0));performance.put(m.away,value(performance,m.away)+(m.awayGoals>m.homeGoals?3:m.homeGoals==m.awayGoals?1:0));}
        ArrayList<String> external=new ArrayList<String>();for(NativeCareerStore.Club club:career.clubs)if(club.league.startsWith("eu_")&&club.activeFrom<=journal.season())external.add(club.id);
        Collections.sort(external,(a,b)->{double x=value(strengths,a)+value(performance,a)*.3,y=value(strengths,b)+value(performance,b)*.3;int d=Double.compare(y,x);return d!=0?d:a.compareTo(b);});
        if(external.contains(champion)&&!entrants.contains(champion))entrants.add(champion);
        HashSet<String> represented=new HashSet<String>();for(String id:entrants)represented.add(associations.get(id));
        for(String id:external)if(entrants.size()<32&&represented.add(associations.get(id)))entrants.add(id);
        for(String id:external)if(entrants.size()<32&&!entrants.contains(id))entrants.add(id);
        if(entrants.size()!=32||new HashSet<String>(entrants).size()!=32)throw new IllegalStateException("UCL requires 32 distinct entrants");
        Collections.sort(entrants,(a,b)->{if(a.equals(champion))return b.equals(champion)?0:-1;if(b.equals(champion))return 1;int d=Double.compare(value(strengths,b),value(strengths,a));return d!=0?d:a.compareTo(b);});
        ArrayList<ArrayList<String>> groups=new ArrayList<ArrayList<String>>();for(int g=0;g<8;g++)groups.add(new ArrayList<String>());
        if(!assign(entrants,associations,groups,0,new Random(seed)))throw new IllegalStateException("UCL group draw has no valid allocation");
        journal.define("ucl-groups",encode(groups));journal.define("ucl-qualification",estimated?"Estimated top-five entrants: previous final tables unavailable. External places estimated from squad strength and previous UCL results.":"Top-five entrants from previous final tables. External places estimated from squad strength and previous UCL results.");
        return groups;
    }
    static boolean assign(List<String> entrants,Map<String,String> associations,ArrayList<ArrayList<String>> groups,int at,Random random){
        if(at==entrants.size())return true;String id=entrants.get(at),association=associations.get(id);int pot=at/groups.size();ArrayList<Integer> order=new ArrayList<Integer>();for(int i=0;i<groups.size();i++)order.add(i);Collections.shuffle(order,random);
        for(int g:order){ArrayList<String> group=groups.get(g);if(group.size()!=pot)continue;boolean clash=false;for(String old:group)if(association.equals(associations.get(old)))clash=true;if(clash)continue;group.add(id);if(assign(entrants,associations,groups,at+1,random))return true;group.remove(group.size()-1);}return false;
    }
    static String encode(List<? extends List<String>> groups){StringBuilder out=new StringBuilder();for(List<String> group:groups){for(int i=0;i<group.size();i++){if(i>0)out.append(',');out.append(group.get(i));}out.append(';');}return out.toString();}
    static ArrayList<ArrayList<String>> decode(String value){ArrayList<ArrayList<String>> groups=new ArrayList<ArrayList<String>>();if(value!=null)for(String group:value.split(";"))if(group.length()>0)groups.add(new ArrayList<String>(Arrays.asList(group.split(","))));return groups;}
}
