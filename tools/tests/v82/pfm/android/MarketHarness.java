package pfm.android;
import android.content.Context;
import java.nio.file.*;
import java.util.*;

/** Executes production AI transactions against the real initial world assets. */
public class MarketHarness {
    static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static void load(String file,NativeCareerStore.Career c)throws Exception{
        HashMap<String,NativeCareerStore.Club> clubs=new HashMap<>();for(NativeCareerStore.Club x:c.clubs)clubs.put(x.id,x);
        for(String line:Files.readAllLines(Paths.get(file))){String[] r=line.split("\t",-1);if(r[0].equals("type"))continue;String id=r[1]+":"+r[2];
            if(r[0].equals("T")){NativeCareerStore.Club x=new NativeCareerStore.Club();x.id=id;x.league=r[1];x.name=r[3];x.activeFrom=r[1].startsWith("eu_")?Integer.parseInt(r[7]):2010;clubs.put(id,x);c.clubs.add(x);}
            else{NativeCareerStore.Player p=new NativeCareerStore.Player();p.id=r[1].startsWith("eu_")?r[18]:id+":"+r[4];p.owner=id;p.name=r[5];p.position=Integer.parseInt(r[6]);p.age=Integer.parseInt(r[7]);p.overall=Integer.parseInt(r[8]);p.value=Integer.parseInt(r[16]);p.contractWage=Integer.parseInt(r[17]);p.contractUntil=2014;c.players.add(p);}}
    }
    static NativeCareerStore.Career world(String path)throws Exception{
        NativeCareerStore.Career c=new NativeCareerStore.Career();c.season=2010;c.activeLeague="en";c.userClub="Chelsea";
        load(path,c);load("overrides/app/src/main/assets/europe_world_v78.tsv",c);
        for(NativeCareerStore.Club club:c.clubs){long wages=0,values=0;for(NativeCareerStore.Player p:c.players)if(p.owner.equals(club.id)){wages+=p.contractWage;values+=p.value;}
            club.balance=(int)Math.min(500000000L,Math.max(2000000L,values/4));club.wageBudget=(int)Math.max(1000000L,wages*115/100);}
        return c;
    }
    static long money(NativeCareerStore.Career c){long n=0;for(NativeCareerStore.Club club:c.clubs)n+=club.balance;return n;}
    static void cap(int n)throws Exception{Class.forName("pfmTransferPolicy31").getField("cap").setInt(null,n);}
    static void calendar(Context context,String path)throws Exception{
        NativeCareerStore.current=world(path);NativeCareerStore.Career c=NativeCareerStore.current;
        NativeTransferCalendar.currentPhase=0;NativeTransferCalendar.currentTick=20;cap(0);
        check(NativeAiMarket.run(context)==0&&c.deals.isEmpty(),"off setting created deals");
        cap(25);NativeAiMarket.run(context);int summer=c.deals.size();check(summer>0&&summer<25,"partial summer setup");
        NativeTransferCalendar.currentPhase=1;
        for(int tick=40;tick<=42;tick++){NativeTransferCalendar.currentTick=tick;NativeAiMarket.run(context);check(c.deals.size()-summer<=25,"winter exceeded cap after partial summer");}
        int before=c.deals.size();cap(8);NativeTransferCalendar.currentTick=43;NativeAiMarket.run(context);check(c.deals.size()==before,"lowering cap created transfers");
        cap(25);NativeTransferCalendar.currentTick=44;NativeAiMarket.run(context);check(c.deals.size()-summer<=25,"winter exceeded cap");
        check(c.aiCountWindow==20101&&c.aiCount==c.deals.size()-summer,"window counter");
        for(int amount:new int[]{2,5,12,50,80}){cap(amount);NativeCareerStore.current=world(path);NativeTransferCalendar.currentPhase=0;NativeTransferCalendar.currentTick=23;int moved=NativeAiMarket.run(context);check(moved>0&&moved<=amount,"frequency "+amount);}
        cap(120);System.out.println("Calendar/frequency: OFF, partial summer, winter cap, repeated ticks and lowered cap verified");
    }
    public static void main(String[] args)throws Exception{
        Context context=new Context("overrides/app/src/main/assets");int runs=args.length>1?Integer.parseInt(args[1]):12;
        int all=0,externalBuy=0,externalSell=0,domestic=0,keepers=0;Set<String> buyers=new HashSet<>();
        for(int seed=1;seed<=runs;seed++){
            NativeWorldCenter.nonce=MatchEngine.mix(seed*10007);NativeCareerStore.current=world(args[0]);NativeCareerStore.Career c=NativeCareerStore.current;long before=money(c);
            NativeTransferCalendar.currentTick=23;NativeTransferCalendar.currentPhase=0;
            int moved=NativeAiMarket.run(context);check(moved>0,"market frozen");check(NativeAiMarket.run(context)==0,"same tick repeated");check(money(c)==before,"money not conserved");
            HashMap<String,NativeCareerStore.Club> map=new HashMap<>();for(NativeCareerStore.Club club:c.clubs)map.put(club.id,club);
            HashMap<String,Integer> sizes=new HashMap<>();HashSet<String> ids=new HashSet<>();for(NativeCareerStore.Player p:c.players){check(ids.add(p.id),"duplicate player");sizes.put(p.owner,sizes.getOrDefault(p.owner,0)+1);}
            for(NativeCareerStore.Club club:c.clubs){int floor=club.league.startsWith("eu_")?13:18;check(sizes.getOrDefault(club.id,0)>=floor,"undersized seller "+club.name);check(club.balance>=0,"negative balance");}
            ArrayList<NativeCareerStore.Club> eligible=new ArrayList<>();for(NativeCareerStore.Club club:c.clubs)if(!club.league.equals(c.activeLeague))eligible.add(club);
            NativeTransferMarketModel model=new NativeTransferMarketModel(context,eligible);
            for(NativeCareerStore.Deal d:c.deals){NativeCareerStore.Club b=map.get(d.to),s=map.get(d.from);check(d.fee<=model.feeLimit(b)&&d.wage<=model.wageLimit(b),"buyer profile exceeded");all++;if(b.league.startsWith("eu_"))externalBuy++;if(s.league.startsWith("eu_"))externalSell++;if(b.league.equals(s.league))domestic++;buyers.add(b.id);for(NativeCareerStore.Player p:c.players)if(p.id.equals(d.player)&&p.position==0)keepers++;}
        }
        check(all<=runs*120,"window limit");check(externalBuy*1.0/all<0.65&&externalBuy*1.0/all>0.20,"destination bias");check(domestic*1.0/all>0.40,"domestic geography ignored");check(keepers*1.0/all<0.20,"keeper bias");check(buyers.size()>100,"fixed buyers dominate");
        System.out.printf(Locale.US,"%d windows, %d transfers; outside-top5 buyers %.1f%% sellers %.1f%%; domestic %.1f%%; goalkeepers %.1f%%; %d unique buyers%n",runs,all,100.0*externalBuy/all,100.0*externalSell/all,100.0*domestic/all,100.0*keepers/all,buyers.size());
        calendar(context,args[0]);
    }
}
class NativeCareerStore {
    static class Club {String id,name,league;int balance,wageBudget,activeFrom=2010;}
    static class Player {String id,name,owner;int position,age,overall,value,contractWage,contractUntil;}
    static class Deal {String player,from,to;int fee,wage,years,season;boolean ai;}
    static class Career {int season,aiWindow=-1,aiCountWindow=-1,aiCount;String activeLeague,userClub;ArrayList<Club> clubs=new ArrayList<>();ArrayList<Player> players=new ArrayList<>();ArrayList<Deal> deals=new ArrayList<>();}
    static Career current;
    static Career mutable(Context c){return current;}static boolean aiReady(Context c){return true;}
    static Club userClub(Career c){for(Club x:c.clubs)if(x.name.equals(c.userClub)&&x.league.equals(c.activeLeague))return x;return null;}
    static String commitAi(Context c,Career career){for(Club club:career.clubs){long wages=0;for(Player p:career.players)if(p.owner.equals(club.id))wages+=p.contractWage;if(wages>club.wageBudget)return "wage budget exceeded";}return "OK";}static void postMail(Career c,Deal d){}
}
class NativeWorldCenter {
    static int nonce;static int identity(){return nonce;}
    static ArrayList<String> allCodes(){ArrayList<String> codes=new ArrayList<>();for(NativeCareerStore.Club c:NativeCareerStore.current.clubs)if(!codes.contains(c.league))codes.add(c.league);Collections.sort(codes);return codes;}
}
class NativeGlobalMarket {static int quoted(int n){return Math.max(100000,((n+50000)/100000)*100000);}}
class NativeTransferCalendar {
    static int currentTick=23,currentPhase=0;
    static int phase(){return currentPhase;}static int windowId(int season){return season*10+currentPhase;}
    static int tick(Context c,boolean advance){return currentTick;}static int effectiveSeason(int s){return s;}
    static int target(int cap,int tick){return tick>=40?(cap*(tick-39)+4)/5:tick==23?cap:tick<20?(cap*Math.min(5,tick)+9)/10:(cap*new int[]{60,75,90,100}[tick-20]+99)/100;}
}
