package pfm.android;
import android.content.Context;
import java.io.*;
import java.util.*;

/** Full initial-roster seasons with actual rotation, fatigue, injuries, bans, substitutes and UCL load. */
public class ScoringHarness {
 static final String[] C={"en","es","it","de","fr"};
 static final int[] MATCHES={380,380,380,306,380},HOME={617,622,544,504,510},AWAY={446,420,411,390,380};
 static class Stats {int n,hg,ag,draw,zero,four;double neutralH,neutralA,expectedH,expectedA;}
 public static void main(String[] args)throws Exception{
  int samples=Integer.parseInt(args[2]),start=Integer.parseInt(args[3]);Stats[] stats=new Stats[5];for(int i=0;i<5;i++)stats[i]=new Stats();
  File root=new File(args[1]);root.mkdirs();Context context=new Context("overrides/app/src/main/assets",root);AndroidRuntime.current=context;
  ArrayList<ArrayList<String>> groups=NativeCompetitionCalendar.groups(context);
  for(int sample=start;sample<start+samples;sample++){
   JournalHarness.world(args[0],MatchEngine.mix(sample*10007+865001));NativeCareerStore.Career career=NativeCareerStore.current;
   HashMap<String,NativeWorldCenter.T> teams=NativeMatchJournal.teams(career);HashMap<String,NativeWorldCenter.P> players=new HashMap<>();for(NativeWorldCenter.T team:teams.values())for(NativeWorldCenter.P p:team.ps)players.put(p.uid,p);
   MatchJournal journal=new MatchJournal(root,NativeWorldCenter.nonce,2010);MatchEffects effects=new MatchEffects(38);
   for(int step=1;step<=38;step++){
    effects.rest(step);
    for(int i=0;i<5;i++){String code=C[i];int previous=NativeCompetitionCalendar.parallelRound(step-1,38,code),now=NativeCompetitionCalendar.parallelRound(step,38,code);
     for(NativeWorldCenter.M fixture:NativeWorldCenter.schedule(NativeWorldCenter.leagues.get(code).size()))if(fixture.r>previous&&fixture.r<=now){
      String hid=code+":"+fixture.h,aid=code+":"+fixture.a;int seed=NativeWorldCenter.nonce+2010*10007+code.hashCode()*101+fixture.r*37+fixture.h*11+fixture.a;
      NativeWorldCenter.T h=teams.get(hid),a=teams.get(aid);NativeWorldCenter.S hs=NativeMatchJournal.eligible(h,hid,"league:"+code,seed,effects,players),as=NativeMatchJournal.eligible(a,aid,"league:"+code,seed+17,effects,players);
      double[] neutral=NativeMatchEngine.expected(h,a,hs,as,MatchEngine.DEFAULT),expected=NativeMatchEngine.expected(h,a,hs,as,MatchEngine.profile(code));
      MatchJournal.Match m=NativeMatchJournal.capture(journal,"league:"+code,fixture.r,hid,aid,seed,teams,players,"native-v84",effects,career);
      Stats s=stats[i];s.n++;s.hg+=m.homeGoals;s.ag+=m.awayGoals;if(m.homeGoals==m.awayGoals)s.draw++;if(m.homeGoals+m.awayGoals==0)s.zero++;if(m.homeGoals+m.awayGoals>=4)s.four++;s.neutralH+=neutral[0];s.neutralA+=neutral[1];s.expectedH+=expected[0];s.expectedA+=expected[1];
     }
    }
    int previous=NativeCompetitionCalendar.uclCompleted(step-1,38),now=NativeCompetitionCalendar.uclCompleted(step,38);
    for(int r=previous;r<now;r++)for(int g=0;g<8;g++)for(int match=0;match<2;match++){int[] pairs=NativeCompetitionCalendar.UCL_PAIRS[r];NativeMatchJournal.capture(journal,"ucl:2010",r+1,groups.get(g).get(pairs[match*2]),groups.get(g).get(pairs[match*2+1]),NativeWorldCenter.nonce+r*104729+g*1009+match*37,teams,players,"native-v84",effects,career);}
   }
   if((sample-start+1)%10==0)System.out.println("Progress: "+(sample-start+1)+" full seasons");
  }
  System.out.println("league,matches,actualGPM,expectedGPM,targetGPM,homeGPM,awayGPM,drawPct,zeroPct,fourPlusPct,suggestedHomeScale,suggestedAwayScale");
  for(int i=0;i<5;i++){Stats s=stats[i];double target=(HOME[i]+AWAY[i])/(double)MATCHES[i];
   System.out.printf(Locale.US,"%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f,%.6f,%.6f%n",C[i],s.n,(s.hg+s.ag)/(double)s.n,(s.expectedH+s.expectedA)/s.n,target,s.hg/(double)s.n,s.ag/(double)s.n,100.0*s.draw/s.n,100.0*s.zero/s.n,100.0*s.four/s.n,HOME[i]/(double)MATCHES[i]/(s.neutralH/s.n),AWAY[i]/(double)MATCHES[i]/(s.neutralA/s.n));
   if(start>=1000){JournalHarness.check(Math.abs((s.hg+s.ag)/(double)s.n-target)<0.065,"league mean outside historical tolerance "+C[i]);JournalHarness.check(Math.abs(s.hg/(double)s.n-HOME[i]/(double)MATCHES[i])<0.055&&Math.abs(s.ag/(double)s.n-AWAY[i]/(double)MATCHES[i])<0.055,"home/away balance "+C[i]);JournalHarness.check(Math.abs((s.expectedH+s.expectedA)/s.n-target)<0.02,"expected rate drift "+C[i]);}
  }
  JournalHarness.check(MatchEngine.profile("ucl:2010")==MatchEngine.DEFAULT,"domestic profile leaked into UCL");
  JournalHarness.check(MatchEngine.profile("league:fr")==MatchEngine.profile("fr"),"competition key alias changed calibration");
  if(start>=1000)System.out.println("PASS: held-out league scoring, expected intensity, home/away totals and UCL context isolation; "+samples+" seasons");
 }
}
