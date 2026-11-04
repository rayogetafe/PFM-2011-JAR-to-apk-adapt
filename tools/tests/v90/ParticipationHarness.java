import java.util.*;
import pfm.android.*;
public class ParticipationHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static MatchJournal.Match match(String id,int homeSubs,int awaySubs){MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.competition="league:en";m.home="en:0";m.away="en:1";m.effectsVersion=2;m.report="Report";
        for(String club:new String[]{m.home,m.away})for(int i=0;i<11+(club.equals(m.home)?homeSubs:awaySubs);i++){MatchJournal.PlayerFact p=new MatchJournal.PlayerFact();p.club=club;p.uid=club+":"+i;p.minutes=i<11?60:30;p.stats[2]=1;p.stats[3]=i<11?1:0;p.stats[4]=65;m.players.add(p);}return m;
    }
    static MatchEngine.Lineup side(int seed){ArrayList<MatchEngine.Member> xi=new ArrayList<>(),reserves=new ArrayList<>();for(int i=0;i<25;i++){int position=i==0||i==11||i==12?0:i<5||i<18&&i>=13?1:i<9||i<22&&i>=18?2:3;MatchEngine.Member p=new MatchEngine.Member("p"+i,position,75,75,75,55,10);(i<11?xi:reserves).add(p);}return MatchEngine.lineup(seed,xi,reserves);}
    static void play(MatchEngine.Lineup s){for(int m=1;m<=90;m++){s.begin(m);s.end(m);}}
    public static void main(String[] args)throws Exception{
        MatchJournal.Match first=match("one",2,3),second=match("two",3,1),europe=match("ucl",3,3);europe.competition="ucl:2010";
        MatchJournal.PlayerFact phantom=new MatchJournal.PlayerFact();phantom.uid="fake";phantom.club="en:0";phantom.stats[2]=3;first.players.add(phantom);
        ArrayList<MatchJournal.Match> records=new ArrayList<>(Arrays.asList(first,second,europe,first));MatchParticipation.Summary result=MatchParticipation.summarize(records,"league:en","en:0");
        check(result.covered==2&&result.substitutions==5&&result.average()==2.5&&result.distribution[2]==1&&result.distribution[3]==1,"2+3 must be 2.50; not legacy counters, opponent, UCL or duplicate reports");
        Map<String,int[]> totals=MatchParticipation.completeTotals(records,"league:en",2);check(totals.get("en:0:0")[0]==2&&totals.get("en:0:0")[1]==2&&!totals.containsKey("fake"),"complete journal totals");
        MatchJournal.Match unknown=match("old",3,3);unknown.effectsVersion=0;records.add(unknown);result=MatchParticipation.summarize(records,"league:en","en:0");check(result.covered==2&&result.matches==3&&result.average()==2.5,"unknown report treated as played plan");check(MatchParticipation.completeTotals(records,"league:en",3).isEmpty(),"partial history repaired counters");check(Double.isNaN(MatchParticipation.summarize(Collections.<MatchJournal.Match>emptyList(),"league:en","en:0").average()),"no matches fabricated zero average");
        MatchEngine.Lineup collision=side(77);java.lang.reflect.Field f=MatchEngine.Lineup.class.getDeclaredField("planned");f.setAccessible(true);((Map<?,?>)f.get(collision)).clear();collision.plan(65,"p13");collision.plan(65,"p14");collision.plan(79,"p22");play(collision);check(collision.substitutions==3,"two requests at same minute overwritten");check(collision.find("p13").minutes>0&&collision.find("p14").minutes>0,"same-minute plan replaced a player before he played");
        int[] counts=new int[4];int changes=0;for(int seed=0;seed<5000;seed++){MatchEngine.Lineup s=side(seed);check(s.squad.size()==18,"not seven reserves");int[] cover=new int[4];for(MatchEngine.Member p:s.squad)if(!p.starter)cover[p.position]++;check(Arrays.equals(cover,new int[]{1,2,2,2}),"bench lost positions after first 18 roster slots");play(s);check(s.substitutions<=3,"four substitutions");counts[s.substitutions]++;changes+=s.substitutions;}
        double mean=changes/5000.0;check(mean>2.55&&mean<2.72&&counts[1]<400&&counts[3]>2750,"bad substitution distribution "+mean);
        System.out.printf(java.util.Locale.US,"PASS: 2+3=2.50, scope/coverage/migration, coincident-minute plans, balanced bench; 5000 team matches mean %.4f, distribution %s%n",mean,Arrays.toString(counts));
    }
}
