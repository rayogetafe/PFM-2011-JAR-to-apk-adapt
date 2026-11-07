package pfm.android;

import java.util.*;

/** Europa's tournament orchestration; football, lineups and tie resolution use the common engine. */
final class EuropaSeason {
    static final int[] AT={21,22,25,26,29,30,33,35,37};
    static final String[] STAGES={"ROUND OF 32","ROUND OF 16","QUARTER-FINALS","SEMI-FINALS","FINAL"};
    static String competition(int year){return "uel:"+year;}
    static HashSet<String> entrants(List<? extends List<String>> groups){HashSet<String> out=new HashSet<String>();for(List<String> group:groups)out.addAll(group);return out;}
    static String champion(MatchJournal j){for(MatchJournal.Match m:j.competition(competition(j.season())))if(m.round==15)return m.winner;return "";}
    static ArrayList<String> leaguePlaces(String code,MatchJournal previous,NativeCareerStore.Career career,Set<String> ucl,Map<String,Double> strengths,String holder){
        ArrayList<UclSeason.Standing> table=UclSeason.standings(previous,code,career.clubs);boolean complete=true;for(UclSeason.Standing row:table)if(row.played!=(code.equals("de")?34:38))complete=false;
        if(!complete)Collections.sort(table,(a,b)->{int d=Double.compare(UclSeason.value(strengths,b.id),UclSeason.value(strengths,a.id));return d!=0?d:a.id.compareTo(b.id);});
        ArrayList<String> selected = new ArrayList<>();
        if (!ucl.contains(holder)) {
            for (NativeCareerStore.Club club : career.clubs) {
                if (club.id.equals(holder) && EuropeanPlaces.association(club.league).equals(code)) selected.add(holder);
            }
        }
        String cupEntrant = DomesticCup.europaEntrant(previous, code, ucl);
        if (!cupEntrant.isEmpty() && !selected.contains(cupEntrant)) selected.add(cupEntrant);
        for (UclSeason.Standing row : table) {
            if (selected.size() == EuropeanPlaces.europa(code)) break;
            if (!ucl.contains(row.id) && !selected.contains(row.id)) selected.add(row.id);
        }
        return selected;
    }
    static ArrayList<ArrayList<String>> prepare(MatchJournal j,MatchJournal previous,NativeCareerStore.Career career,ArrayList<ArrayList<String>> uclGroups,int seed){
        String frozen=j.metadata("uel-groups");if(frozen!=null)return UclSeason.decode(frozen);
        Set<String> ucl=entrants(uclGroups);HashMap<String,Double> strengths=UclSeason.strengths(career);HashMap<String,String> associations=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)associations.put(club.id,EuropeanPlaces.association(club.league));
        String holder = champion(previous);
        ArrayList<String> selected = new ArrayList<>();
        for (String code : NativeWorldCenter.C) {
            selected.addAll(leaguePlaces(code, previous, career, ucl, strengths, holder));
            j.define("uel-cup-" + code, DomesticCup.europaEntrant(previous, code, ucl));
        }
        ArrayList<String> external=new ArrayList<String>();for(NativeCareerStore.Club club:career.clubs)if(club.league.startsWith("eu_")&&club.activeFrom<=j.season()&&!ucl.contains(club.id))external.add(club.id);
        Collections.sort(external,(a,b)->{int d=Double.compare(UclSeason.value(strengths,b),UclSeason.value(strengths,a));return d!=0?d:a.compareTo(b);});
        if(external.contains(holder))selected.add(holder);
        HashMap<String,Integer> counts=new HashMap<String,Integer>();for(String id:selected){String a=associations.get(id);counts.put(a,counts.containsKey(a)?counts.get(a)+1:1);}
        for(int cap=1;selected.size()<48&&cap<=4;cap++)for(String id:external){String a=associations.get(id);int count=counts.containsKey(a)?counts.get(a):0;if(selected.size()<48&&!selected.contains(id)&&count<cap){selected.add(id);counts.put(a,count+1);}}
        if(selected.size()!=48||new HashSet<String>(selected).size()!=48)throw new IllegalStateException("Europa requires 48 distinct entrants");
        return freeze(j, selected, career, EuropeanQualification.holderFirst(holder,
                EuropeanQualification.seedingOrder(Collections.emptyMap(), strengths)), seed);
    }
    static ArrayList<ArrayList<String>> freeze(MatchJournal j,ArrayList<String> selected,NativeCareerStore.Career career,Comparator<String> order,int seed){
        if(j.metadata("uel-groups")!=null)return UclSeason.decode(j.metadata("uel-groups"));
        if(selected.size()!=48||new HashSet<String>(selected).size()!=48)throw new IllegalStateException("Europa requires 48 distinct entrants");
        HashMap<String,String> associations=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)associations.put(club.id,EuropeanPlaces.association(club.league));
        Collections.sort(selected, order);
        ArrayList<ArrayList<String>> groups=new ArrayList<ArrayList<String>>();for(int g=0;g<12;g++)groups.add(new ArrayList<String>());if(!UclSeason.assign(selected,associations,groups,0,new Random(seed)))throw new IllegalStateException("Europa group draw failed");
        j.define("uel-groups",UclSeason.encode(groups));j.define("uel-qualification",j.season()==2010?"Estimated inaugural 2010/11 entrants; historical group draw is not reproduced.":"Three league-table places per top-five league, excluding UCL entrants. External places estimated; cup qualifying rounds abstracted.");return groups;
    }
    static ArrayList<String[]> draw(MatchJournal j,ArrayList<ArrayList<String>> groups,ArrayList<ArrayList<String>> uclGroups,Map<String,String> associations,int seed,int stage){
        String stored=j.metadata("uel-draw-"+stage);if(stored!=null)return UclKnockout.decode(stored);ArrayList<String[]> pairs=new ArrayList<String[]>();String competition=competition(j.season());
        if(stage==0){ArrayList<String> winners=new ArrayList<String>(),seconds=new ArrayList<String>();HashMap<String,Integer> groupOf=new HashMap<String,Integer>();
            for(int g=0;g<groups.size();g++){ArrayList<UclKnockout.Row> rows=UclKnockout.table(groups.get(g),j.competition(competition));for(UclKnockout.Row row:rows)if(row.played!=6)return pairs;winners.add(rows.get(0).id);seconds.add(rows.get(1).id);for(String id:groups.get(g))groupOf.put(id,g);}
            ArrayList<UclKnockout.Row> thirds=new ArrayList<UclKnockout.Row>();for(int g=0;g<uclGroups.size();g++){ArrayList<UclKnockout.Row> rows=UclKnockout.table(uclGroups.get(g),j.competition(UclSeason.competition(j.season())));for(UclKnockout.Row row:rows)if(row.played!=6)return pairs;thirds.add(rows.get(2));groupOf.put(rows.get(2).id,12+g);}
            Collections.sort(thirds,(a,b)->{if(a.points!=b.points)return b.points-a.points;int d=(b.gf-b.ga)-(a.gf-a.ga);if(d!=0)return d;if(a.gf!=b.gf)return b.gf-a.gf;return a.id.compareTo(b.id);});
            StringBuilder transferred=new StringBuilder();for(int i=0;i<8;i++){String id=thirds.get(i).id;(i<4?winners:seconds).add(id);transferred.append(id).append(';');}j.define("uel-from-ucl",transferred.toString());
            Collections.shuffle(winners,new Random(seed));Collections.shuffle(seconds,new Random(seed^0x15ae));if(!UclKnockout.pair(winners,seconds,groupOf,associations,0,new HashSet<String>(),pairs))throw new IllegalStateException("Europa last-32 draw failed");
        }else{ArrayList<String[]> previous=draw(j,groups,uclGroups,associations,seed,stage-1);if(previous.isEmpty())return pairs;ArrayList<String> qualified=new ArrayList<String>();int round=stage*2+6;for(String[] tie:previous){MatchJournal.Match m=j.get(UclKnockout.key(competition,round,tie[1],tie[0]));if(m==null||m.winner.length()==0)return pairs;qualified.add(m.winner);}Collections.shuffle(qualified,new Random(seed^stage*104729));for(int i=0;i<qualified.size();i+=2)pairs.add(new String[]{qualified.get(i),qualified.get(i+1)});}
        StringBuilder value=new StringBuilder();for(String[] pair:pairs)value.append(pair[0]).append(',').append(pair[1]).append(';');j.define("uel-draw-"+stage,value.toString());return pairs;
    }

    static int completed(int step,int total){int equivalent=(step*38+total/2)/total,n=0;for(int at:AT)if(equivalent>=at)n++;return n;}
    static void advance(MatchJournal j,ArrayList<ArrayList<String>> groups,ArrayList<ArrayList<String>> uclGroups,HashMap<String,String> associations,int nonce,int step,int total,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career){
        String competition=competition(j.season()),savedCutoff=j.metadata("uel-v93-cutoff");int cutoff=savedCutoff==null?0:Integer.parseInt(savedCutoff);String applied=step<=cutoff?"reconstructed-v93":source;int seed=nonce^j.season()*1000003^0x3e17;
        int before=NativeCompetitionCalendar.uclCompleted(step-1,total),after=NativeCompetitionCalendar.uclCompleted(step,total);for(int r=before;r<after;r++)for(int g=0;g<12;g++)for(int match=0;match<2;match++){int[] pair=NativeCompetitionCalendar.UCL_PAIRS[r];NativeMatchJournal.capture(j,competition,r+1,groups.get(g).get(pair[match*2]),groups.get(g).get(pair[match*2+1]),seed^r*104729^g*1009^match*37,teams,players,applied,effects,career);}
        before=completed(step-1,total);after=completed(step,total);for(int k=before;k<after;k++){int stage=Math.min(4,k/2),round=k+7;ArrayList<String[]> pairs=draw(j,groups,uclGroups,associations,seed,stage);if(pairs.isEmpty())throw new IllegalStateException("Europa previous stage incomplete");for(int t=0;t<pairs.size();t++){String[] pair=pairs.get(t);boolean second=k<8&&k%2==1;String home=second?pair[1]:pair[0],away=second?pair[0]:pair[1];MatchJournal.Match first=second?j.get(UclKnockout.key(competition,round-1,away,home)):null;NativeMatchJournal.capture(j,competition,round,home,away,seed^round*104729^t*1009,teams,players,applied,effects,career,first,k==8);}}
        if(NativeCompetitionCalendar.uclCompleted(step,total)==6)for(int stage=0;stage<5;stage++)draw(j,groups,uclGroups,associations,seed,stage);
    }
}
