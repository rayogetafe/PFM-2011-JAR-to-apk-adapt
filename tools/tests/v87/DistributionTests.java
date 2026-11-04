package pfm.android;
import java.util.*;

/** Mathematical invariants, event-chain endpoint check, restart and causality (no Android fixtures). */
public final class DistributionTests {
 static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
 static MatchEngine.Team team(int rating){MatchEngine.Team t=new MatchEngine.Team();for(int i=0;i<11;i++)t.add(i==0?0:i<5?1:i<9?2:3,rating,rating,rating,55,0);return t;}
 static final int[] X={0,0,1,1,1,2,2,2,3,3},Y={0,1,0,1,2,1,2,3,2,3};
 static void conservation(){int cases=0;
  for(double h:new double[]{.08,.4,1.4,3,5.5})for(double a:new double[]{.08,.8,1.2,3,5.5})for(double z:new double[]{-.95,0,.95})for(double o:new double[]{-.95,0,.95})for(double t:new double[]{-.95,0,.95}){
   double[] base=MatchEngine.distribution(h,a,0,0,0),changed=MatchEngine.distribution(h,a,z,o,t);double mass=0,meanH=0,meanA=0;double[] rows=new double[4],cols=new double[4];
   for(int i=0;i<10;i++){check(Double.isFinite(changed[i])&&changed[i]>=-1e-14,"negative probability");double delta=changed[i]-base[i];mass+=delta;meanH+=X[i]*delta;meanA+=Y[i]*delta;rows[X[i]]+=delta;cols[Y[i]]+=delta;}
   check(Math.abs(mass)<1e-12&&Math.abs(meanH)<1e-12&&Math.abs(meanA)<1e-12,"cycle altered mass or marginal means");for(int k=0;k<4;k++)check(Math.abs(rows[k])<1e-12&&Math.abs(cols[k])<1e-12,"goal marginal altered");check(changed[10]>=0&&changed[10]<=1,"invalid draw probability");cases++;
  }
  System.out.println("Joint model: "+cases+" intensity/coefficient combinations preserve probability mass and both complete goal marginals");
 }
 static void chain(){int n=30000;
  for(String code:new String[]{"en","es","de","fr"}){
   MatchEngine.Profile profile=MatchEngine.profile(code);MatchEngine.Team home=team(75),away=team(75);double h=MatchEngine.expectedGoals(home,away,true,profile),a=MatchEngine.expectedGoals(away,home,false,profile);double[] expected=MatchEngine.distribution(h,a,profile);int hg=0,ag=0,draw=0;int[] low=new int[10];
   for(int seed=0;seed<n;seed++){int x=0,y=0;for(int minute=1;minute<=90;minute++){int event=MatchEngine.events(MatchEngine.mix(seed*10007+87013),minute,home,away,profile,x,y);check(event==0||event==1||event==2,"invalid event mask");if(event==1)x++;if(event==2)y++;}hg+=x;ag+=y;if(x==y)draw++;for(int i=0;i<10;i++)if(x==X[i]&&y==Y[i])low[i]++;}
   check(Math.abs(hg/(double)n-h)<.04&&Math.abs(ag/(double)n-a)<.04,"event chain changed goal means "+code);check(Math.abs(draw/(double)n-expected[10])<.013,"event chain endpoint mismatch "+code);for(int i=0;i<10;i++)check(Math.abs(low[i]/(double)n-expected[i])<.010,"low-score endpoint mismatch "+code+" "+X[i]+":"+Y[i]);
   System.out.printf(Locale.US,"%s event chain: %d matches; goals %.3f / analytic %.3f; draws %.2f%% / analytic %.2f%%%n",code,n,(hg+ag)/(double)n,h+a,100.0*draw/n,100*expected[10]);
  }
 }
 static void replay(){int changed=0;MatchEngine.Profile p=MatchEngine.profile("fr");
  for(int seed=0;seed<1000;seed++){int h=0,a=0,checkpointH=0,checkpointA=0;int[] events=new int[91];MatchEngine.Team home=team(75),away=team(75);
   for(int minute=1;minute<=90;minute++){events[minute]=MatchEngine.events(seed,minute,home,away,p,h,a);if(events[minute]==1)h++;if(events[minute]==2)a++;if(minute==37){checkpointH=h;checkpointA=a;}}
   int x=checkpointH,y=checkpointA;for(int minute=38;minute<=90;minute++){int event=MatchEngine.events(seed,minute,team(75),team(75),p,x,y);check(event==events[minute],"restart/cache changed event prefix");if(event==1)x++;if(event==2)y++;}check(x==h&&y==a,"restart final score");
   int strongH=0,strongA=0,weakH=0,weakA=0;for(int minute=1;minute<=90;minute++){int strong=MatchEngine.events(seed,minute,minute<=60?home:team(90),away,p,strongH,strongA),weak=MatchEngine.events(seed,minute,minute<=60?home:team(60),away,p,weakH,weakA);if(minute<=60)check(strong==events[minute]&&weak==events[minute],"future tactic rewrote earlier event");else if(strong!=weak)changed++;if(strong==1)strongH++;if(strong==2)strongA++;if(weak==1)weakH++;if(weak==2)weakA++;}
  }
  check(changed>150,"future team strength had no effect");
  MatchEngine.Team h=team(75),a=team(75);for(int seed=0;seed<1000;seed++)for(int minute=1;minute<=90;minute++){double roll=(MatchEngine.mix(seed^minute*0x9e3779b9)&0xffffffffL)/4294967296.0,ph=MatchEngine.expectedGoals(h,a,true)/90,pa=MatchEngine.expectedGoals(a,h,false)/90;int original=roll<ph?1:roll<ph+pa?2:0;check(MatchEngine.events(seed,minute,h,a,MatchEngine.DEFAULT,2,1)==original,"default/UCL context changed");}
  boolean required=false;try{MatchEngine.goal(1,1,0,h,a,p);}catch(IllegalArgumentException expected){required=true;}check(required,"missing score prefix silently accepted");
  System.out.println("Replay: cold reconstruction, cached/fresh teams, tactical causality, explicit score prefix and unchanged default/UCL model verified");
 }
 public static void main(String[] args){conservation();chain();replay();}
}
