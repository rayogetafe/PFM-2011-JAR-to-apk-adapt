package pfm.android;
import java.nio.file.*;
import java.util.*;

public final class MatchEngineTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static MatchEngine.Team team(int rating,int keeper,int fatigue){
        MatchEngine.Team t=new MatchEngine.Team();
        for(int i=0;i<11;i++){int pos=i==0?0:i<5?1:i<9?2:3;
            t.add(pos,i==0?keeper:rating,i==0?keeper:rating,i==0?keeper:rating,55,fatigue);}
        return t;
    }
    static int[] simulate(int seed,MatchEngine.Team h,MatchEngine.Team a,int from,int to){
        int[] goals=new int[2];for(int m=from;m<=to;m++)for(int side=0;side<2;side++)if(MatchEngine.goal(seed,m,side,h,a))goals[side]++;
        return goals;
    }
    static void synthetic(){
        MatchEngine.Team equal=team(75,75,0),strong=team(85,85,0),weak=team(65,65,0);
        double hg=0,ag=0,draw=0,big=0,zero=0,win=0;int n=50000;
        for(int seed=0;seed<n;seed++){
            int[] g=simulate(seed,equal,equal,1,90),first=simulate(seed,equal,equal,1,37),rest=simulate(seed,equal,equal,38,90);
            check(g[0]==first[0]+rest[0]&&g[1]==first[1]+rest[1],"skip/restart changed result");
            hg+=g[0];ag+=g[1];if(g[0]==g[1])draw++;if(Math.abs(g[0]-g[1])>=4)big++;if(g[0]+g[1]==0)zero++;
            int[] unequal=simulate(seed,strong,weak,1,90);if(unequal[0]>unequal[1])win++;
        }
        check((hg+ag)/n>2.5&&(hg+ag)/n<2.9,"goal balance");
        check(hg>ag&&draw/n>0.22&&draw/n<0.30,"home/draw balance");
        check(big/n<0.06&&zero/n>0.05,"score distribution");
        check(win/n>0.70&&win/n<0.95,"strength too weak or absolute");
        check(MatchEngine.expectedGoals(equal,team(75,40,0),true)>MatchEngine.expectedGoals(equal,equal,true),"keeper has no effect");
        check(MatchEngine.expectedGoals(team(75,75,90),equal,true)<MatchEngine.expectedGoals(equal,equal,true),"fatigue has no effect");
        MatchEngine.Team depleted=new MatchEngine.Team();for(int i=0;i<10;i++)depleted.add(i==0?0:i<5?1:i<8?2:3,75,75,75,55,0);
        check(MatchEngine.expectedGoals(depleted,equal,true)<MatchEngine.expectedGoals(equal,equal,true),"red card has no effect");
        MatchEngine.Team attacking=team(75,75,0);attacking.style=10;
        check(MatchEngine.expectedGoals(attacking,equal,true)>MatchEngine.expectedGoals(equal,equal,true),"tactics have no effect");
        // Decisions already made cannot be changed by a substitution at minute 60.
        int changed=0;for(int seed=0;seed<500;seed++)for(int m=61;m<=90;m++)
            if(MatchEngine.goal(seed,m,0,strong,equal)!=MatchEngine.goal(seed,m,0,weak,equal))changed++;
        check(changed>100,"late substitutions have no effect");
        System.out.printf(Locale.US,"Equal teams (%d matches): %.3f goals, %.1f%% draws, %.1f%% 0:0, %.1f%% margins 4+, home %.2f away %.2f; stronger wins %.1f%%%n",n,(hg+ag)/n,100*draw/n,100*zero/n,100*big/n,hg/n,ag/n,100*win/n);
    }
    static NativeWorldCenter.S selection(NativeWorldCenter.T t){
        NativeWorldCenter.S s=new NativeWorldCenter.S();int[] counts={1,4,4,2};
        ArrayList<NativeWorldCenter.P> ranked=new ArrayList<NativeWorldCenter.P>(t.ps);
        Collections.sort(ranked,(a,b)->(b.qua+b.spe+b.res)-(a.qua+a.spe+a.res));
        for(int p=0;p<4;p++)for(NativeWorldCenter.P x:ranked)if(x.pos==p&&counts[p]>0){s.xi.add(x);counts[p]--;}
        for(NativeWorldCenter.P x:ranked)if(s.xi.size()<11&&x.pos>0&&!s.xi.contains(x))s.xi.add(x);
        check(s.xi.size()==11,"incomplete positional XI");s.used.addAll(s.xi);
        for(NativeWorldCenter.P p:ranked)if(s.used.size()<14&&!s.used.contains(p)&&p.pos>0)s.used.add(p);
        return s;
    }
    static void load(String path,TreeMap<String,LinkedHashMap<Integer,NativeWorldCenter.T>> leagues)throws Exception{
        for(String line:Files.readAllLines(Paths.get(path))){String[] r=line.split("\t",-1);if(r[0].equals("type"))continue;
            LinkedHashMap<Integer,NativeWorldCenter.T> clubs=leagues.computeIfAbsent(r[1],k->new LinkedHashMap<>());int id=Integer.parseInt(r[2]);
            if(r[0].equals("T")){NativeWorldCenter.T t=new NativeWorldCenter.T();t.style=Integer.parseInt(r[14]);t.form=Integer.parseInt(r[15]);clubs.put(id,t);}
            else {NativeWorldCenter.P p=new NativeWorldCenter.P();p.id=Integer.parseInt(r[4]);p.pos=Integer.parseInt(r[6]);p.spe=Integer.parseInt(r[10]);p.res=Integer.parseInt(r[11]);p.qua=Integer.parseInt(r[12]);p.mor=Integer.parseInt(r[13]);clubs.get(id).ps.add(p);}}
    }
    static void seasons(String path)throws Exception{
        TreeMap<String,LinkedHashMap<Integer,NativeWorldCenter.T>> leagues=new TreeMap<>();load(path,leagues);
        for(String code:leagues.keySet()){
            ArrayList<NativeWorldCenter.T> clubs=new ArrayList<>(leagues.get(code).values());int games=0,goals=0,draws=0,big=0;
            for(int season=0;season<10;season++)for(int h=0;h<clubs.size();h++)for(int a=0;a<clubs.size();a++)if(h!=a){
                NativeWorldCenter.T home=clubs.get(h),away=clubs.get(a);
                int[] g=NativeMatchEngine.score(MatchEngine.mix(season*10007+h*1009+a*37+code.hashCode()),home,away,selection(home),selection(away));
                games++;goals+=g[0]+g[1];if(g[0]==g[1])draws++;if(Math.abs(g[0]-g[1])>=4)big++;}
            double avg=(double)goals/games;check(avg>2.35&&avg<3.2,code+" inflated/low scoring "+avg);
            check((double)draws/games>0.16&&(double)draws/games<0.33,code+" draw balance");
            System.out.printf(Locale.US,"%s: %d matches, %.3f goals/match, %.1f%% draws, %.1f%% margins 4+%n",code,games,avg,draws*100.0/games,big*100.0/games);
        }
    }
    static void ucl(String world,String europe,String manifest)throws Exception{
        TreeMap<String,LinkedHashMap<Integer,NativeWorldCenter.T>> leagues=new TreeMap<>();load(world,leagues);load(europe,leagues);
        ArrayList<NativeWorldCenter.T> clubs=new ArrayList<>();
        for(String line:Files.readAllLines(Paths.get(manifest))){String[] r=line.split("\t",-1);if(r[0].equals("group"))continue;
            String[] id=r[1].split(":");clubs.add(leagues.get(id[0]).get(Integer.parseInt(id[1])));}
        check(clubs.size()==32,"UCL participants");int games=0,goals=0,draws=0;
        for(int season=0;season<100;season++)for(int group=0;group<8;group++)for(int h=0;h<4;h++)for(int a=0;a<4;a++)if(h!=a){
            NativeWorldCenter.T home=clubs.get(group*4+h),away=clubs.get(group*4+a);
            int[] g=NativeMatchEngine.score(MatchEngine.mix(season*10007+group*1009+h*37+a),home,away,selection(home),selection(away));
            games++;goals+=g[0]+g[1];if(g[0]==g[1])draws++;
        }
        double avg=(double)goals/games;check(avg>2.3&&avg<3.2,"UCL scoring "+avg);
        System.out.printf(Locale.US,"UCL: %d group matches, %.3f goals/match, %.1f%% draws%n",games,avg,100.0*draws/games);
    }
    public static void main(String[] args)throws Exception{synthetic();if(args.length>0)seasons(args[0]);if(args.length>2)ucl(args[0],args[1],args[2]);}
}
