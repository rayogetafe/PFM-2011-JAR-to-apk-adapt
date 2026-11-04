package pfm.android;
import android.content.Context;
import java.io.*;
import java.util.*;
import java.nio.file.*;

public class LongCareerHarness {
 public static int legacyBudget=10000000;
 static HashMap<Integer,Integer> windows=new HashMap<>();
 public static int played(){return NativeWorldCenter.played;}
 public static int total(){return NativeCareerStore.current.activeLeague.equals("de")?34:38;}
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static long money(){long sum=0;for(NativeCareerStore.Club c:NativeCareerStore.current.clubs)sum+=c.balance;return sum;}
 static void economics(String path)throws Exception{
  NativeCareerStore.Career c=NativeCareerStore.current;
  for(String file:new String[]{path,"overrides/app/src/main/assets/europe_world_v78.tsv"})for(String line:Files.readAllLines(Paths.get(file))){String[] p=line.split("\t",-1);if(!p[0].equals("P"))continue;String id=p[1].startsWith("eu_")?p[18]:p[1]+":"+p[2]+":"+p[4];for(NativeCareerStore.Player x:c.players)if(x.id.equals(id)){x.value=Integer.parseInt(p[16]);x.contractWage=Integer.parseInt(p[17]);x.contractUntil=2014;}}
  for(NativeCareerStore.Club club:c.clubs){long wages=0,values=0;for(NativeCareerStore.Player p:c.players)if(p.owner.equals(club.id)){wages+=p.contractWage;values+=p.value;}club.balance=(int)Math.min(500000000L,Math.max(2000000L,values/4));club.wageBudget=(int)Math.max(1000000L,wages*115/100);}
 }
 static void market(Context c)throws Exception{long before=money();int deals=NativeCareerStore.current.deals.size(),window=NativeTransferCalendar.windowId(NativeCareerStore.current.season);NativeAiMarket.run(c,true);check(before==money(),"AI money conservation");int after=NativeCareerStore.current.deals.size();NativeAiMarket.run(c,false);check(after==NativeCareerStore.current.deals.size(),"repeat tick moved player twice");int count=windows.getOrDefault(window,0)+after-deals;windows.put(window,count);check(count<=120,"window cap "+window+" "+count);}
 public static void main(String[] args)throws Exception{
  int years=Integer.parseInt(args[2]),seeds=Integer.parseInt(args[3]),totalGames=0;System.out.println("active,seed,season,matches,gpm,injuries,yellow,red,meanFatigue,peakFatigue,coverStarts,aiDeals,outsideBuyPct,top5MoneyM,externalMoneyM,zeroTop5,usedMedian,maxStarts");
  for(String active:new String[]{"en","de"})for(int seed=1;seed<=seeds;seed++){
   File root=new File(args[1],active+"-"+seed);root.mkdirs();Context context=new Context("overrides/app/src/main/assets",root);AndroidRuntime.current=context;
   windows.clear();legacyBudget=10000000;JournalHarness.world(args[0],MatchEngine.mix(seed*10007+active.hashCode()));NativeCareerStore.current.activeLeague=active;NativeCareerStore.current.userClub=NativeWorldCenter.leagues.get(active).get(0).name;economics(args[0]);
   HashMap<Integer,Integer> firstCounts=new HashMap<>();HashMap<String,Integer> expectedBans=new HashMap<>();TreeMap<String,long[]> careerRates=new TreeMap<>();
   for(int year=2010;year<2010+years;year++){
    NativeWorldCenter.year=year;NativeWorldCenter.played=0;NativeCareerStore.ensure(context);int dealsBefore=NativeCareerStore.current.deals.size();
    NativeMatchJournal.advance(context,0);check(JournalHarness.open(context,NativeWorldCenter.nonce,year).openingBans().equals(expectedBans),"season opening dropped/added outstanding bans");market(context);
    for(int round=1;round<=total();round++){NativeWorldCenter.played=round;NativeMatchJournal.advance(context,round);market(context);if(round==9||round==23){int count=JournalHarness.open(context,NativeWorldCenter.nonce,year).size();JournalHarness.restart();NativeMatchJournal.advance(context,round);check(JournalHarness.open(context,NativeWorldCenter.nonce,year).size()==count,"restart duplicate fixture");}}
    for(int i=0;i<5;i++)market(context);
    MatchJournal j=JournalHarness.open(context,NativeWorldCenter.nonce,year);firstCounts.put(year,j.size());int games=0,goals=0,injuries=0,yellow=0,red=0,peak=0,cover=0;long fatigue=0,appearances=0;HashMap<String,Integer> starts=new HashMap<>();HashMap<String,HashSet<String>> used=new HashMap<>();
    MatchEffects effects=new MatchEffects(total());effects.openingBans(j.openingBans());TreeMap<String,int[]> rates=new TreeMap<>();
    for(MatchJournal.Match m:j.all())if(m.effectsVersion>0){effects.rest(m.clock);games++;goals+=m.homeGoals+m.awayGoals;int hs=0,as=0;
     for(MatchJournal.PlayerFact p:m.players){injuries+=p.injury>0?1:0;yellow+=p.yellow;red+=p.red;if(p.stats[2]>0){check(effects.available(p.uid,m.competition),"unavailable participant "+p.uid);fatigue+=p.stats[8];appearances++;peak=Math.max(peak,p.stats[8]);used.computeIfAbsent(p.club,k->new HashSet<>()).add(p.uid);}if(p.stats[3]>0){starts.put(p.uid,starts.getOrDefault(p.uid,0)+1);if(p.club.equals(m.home))hs++;else as++;if(p.uid.startsWith("cover:"))cover++;}}
     check(hs==11&&as==11,"not XI");effects.apply(m);if(m.competition.startsWith("league:")){String code=m.competition.substring(7);int[] rate=rates.computeIfAbsent(code,k->new int[3]);rate[0]++;rate[1]+=m.homeGoals;rate[2]+=m.awayGoals;}
    }
    int expected=year==2010?1922:1826;check(j.size()==expected,"calendar count "+j.size());
    expectedBans=effects.carryBans();ArrayList<Integer> use=new ArrayList<>();for(HashSet<String> ids:used.values())use.add(ids.size());Collections.sort(use);int max=0;for(int n:starts.values())max=Math.max(max,n);
    int nDeals=NativeCareerStore.current.deals.size()-dealsBefore,external=0,zero=0;long top=0,other=0;HashMap<String,NativeCareerStore.Club> clubs=new HashMap<>();for(NativeCareerStore.Club club:NativeCareerStore.current.clubs){clubs.put(club.id,club);check(club.balance>=0,"negative finance");if(club.league.startsWith("eu_"))other+=club.balance;else{top+=club.balance;if(club.balance==0)zero++;}}
    for(int i=dealsBefore;i<NativeCareerStore.current.deals.size();i++)if(clubs.get(NativeCareerStore.current.deals.get(i).to).league.startsWith("eu_"))external++;
    HashSet<String> ids=new HashSet<>();HashMap<String,Integer> sizes=new HashMap<>();HashMap<String,Long> wages=new HashMap<>();for(NativeCareerStore.Player p:NativeCareerStore.current.players){check(ids.add(p.id),"duplicate player");sizes.put(p.owner,sizes.getOrDefault(p.owner,0)+1);wages.put(p.owner,wages.getOrDefault(p.owner,0L)+p.contractWage);}for(NativeCareerStore.Club club:clubs.values()){check(sizes.getOrDefault(club.id,0)>=(club.league.startsWith("eu_")?13:18)&&sizes.get(club.id)<=30,"squad limits");check(wages.getOrDefault(club.id,0L)<=club.wageBudget,"wage budget exceeded");}
    check(goals/(double)games>2&&goals/(double)games<4,"scoring escaped broad balance bounds");check(fatigue/(double)Math.max(1,appearances)<60,"sustained fatigue overload");check(cover<games/100&&zero<15,"mass roster/finance collapse");check(nDeals>20,"market frozen in long career");check(external/(double)nDeals>0.20&&external/(double)nDeals<0.70,"long-term destination bias");totalGames+=games;
    System.out.printf(Locale.US,"%s,%d,%d,%d,%.3f,%d,%d,%d,%.2f,%d,%d,%d,%.1f,%.2f,%.2f,%d,%d,%d%n",active,seed,year,games,goals/(double)games,injuries,yellow,red,fatigue/(double)Math.max(1,appearances),peak,cover,nDeals,100.0*external/Math.max(1,nDeals),top/1e6,other/1e6,zero,use.get(use.size()/2),max);System.out.flush();
    for(Map.Entry<String,int[]> entry:rates.entrySet()){int[] rate=entry.getValue();long[] accumulated=careerRates.computeIfAbsent(entry.getKey(),k->new long[2]);accumulated[0]+=rate[0];accumulated[1]+=rate[1]+rate[2];System.out.printf(Locale.US,"LEAGUE,%s,%d,%d,%s,%d,%.4f,%.4f,%.4f%n",active,seed,year,entry.getKey(),rate[0],(rate[1]+rate[2])/(double)rate[0],rate[1]/(double)rate[0],rate[2]/(double)rate[0]);}
    if(year==2010+years-1&&years>=3)for(Map.Entry<String,long[]> entry:careerRates.entrySet()){double target=MatchEngine.profile(entry.getKey()).historicalRate;if(target>0)check(Math.abs(entry.getValue()[1]/(double)entry.getValue()[0]-target)<0.18,"multi-season league goal drift "+entry.getKey());}
    if(year>2010)check(JournalHarness.open(context,NativeWorldCenter.nonce,year-1).size()==firstCounts.get(year-1),"history rewritten");
   }
  }
  System.out.println("PASS: "+(2*seeds)+" careers, "+(2*seeds*years)+" seasons, "+totalGames+" native matches; rollover bans, recovery, restart, finance, ownership and window limits verified");
 }
}
