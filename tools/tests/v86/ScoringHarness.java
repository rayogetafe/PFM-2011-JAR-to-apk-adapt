package pfm.android;
import android.content.Context;
import java.io.*;
import java.util.*;

/** Full initial-roster seasons with actual rotation, fatigue, injuries, bans, substitutes and UCL load. */
public class ScoringHarness {
 static final String[] C={"en","es","it","de","fr"};
 static final int[] MATCHES={380,380,380,306,380},HOME={617,622,544,504,510},AWAY={446,420,411,390,380};
 static final int[] DRAWS={111,79,97,63,130},ZERO={25,27,34,14,44},ONE={52,35,43,34,63};
 static class Stats {int n,hg,ag,draw,zero,four,one,two,three;int[] subs=new int[4];double neutralH,neutralA,expectedH,expectedA;ArrayList<double[]> intensities=new ArrayList<>();}
 static double measure(Stats stats,double zero,double one,double two,int index){double sum=0;for(double[] pair:stats.intensities)sum+=MatchEngine.distribution(pair[0],pair[1],zero,one,two)[index];return sum/stats.n;}
 static double fit(Stats stats,double zero,double one,double target,int index){double lo=-0.95,hi=0.95;for(int k=0;k<24;k++){double mid=(lo+hi)/2,value=index==0?measure(stats,mid,0,0,0):index==3?measure(stats,zero,mid,0,3):measure(stats,zero,one,mid,10);if(value<target)lo=mid;else hi=mid;}return (lo+hi)/2;}
 public static void main(String[] args)throws Exception{
  int samples=Integer.parseInt(args[2]),start=Integer.parseInt(args[3]);Stats[] stats=new Stats[5];for(int i=0;i<5;i++)stats[i]=new Stats();
  File root=new File(args[1]);root.mkdirs();Context context=new Context("overrides/app/src/main/assets",root);AndroidRuntime.current=context;
  ArrayList<ArrayList<String>> groups=NativeCompetitionCalendar.groups(context);
  for(int sample=start;sample<start+samples;sample++){
   JournalHarness.world(args[0],MatchEngine.mix(sample*10007+865001));NativeCareerStore.Career career=NativeCareerStore.current;
   HashMap<String,NativeWorldCenter.T> teams=NativeMatchJournal.teams(career);HashMap<String,NativeWorldCenter.P> players=new HashMap<>();for(NativeWorldCenter.T team:teams.values())for(NativeWorldCenter.P p:team.ps)players.put(p.uid,p);
   MatchJournal journal=new MatchJournal(root,NativeWorldCenter.nonce,2010);MatchEffects effects=new MatchEffects(38);ArrayList<ArrayList<String>> europaGroups=EuropaSeason.prepare(journal,new MatchJournal(root,NativeWorldCenter.nonce,2009),career,groups,NativeWorldCenter.nonce^2010*10007^0x3e17);
   for(int step=1;step<=38;step++){
    effects.rest(step);
    for(int i=0;i<5;i++){String code=C[i];int previous=NativeCompetitionCalendar.parallelRound(step-1,38,code),now=NativeCompetitionCalendar.parallelRound(step,38,code);
     for(NativeWorldCenter.M fixture:NativeWorldCenter.schedule(NativeWorldCenter.leagues.get(code).size()))if(fixture.r>previous&&fixture.r<=now){
      String hid=code+":"+fixture.h,aid=code+":"+fixture.a;int seed=NativeWorldCenter.nonce+2010*10007+code.hashCode()*101+fixture.r*37+fixture.h*11+fixture.a;
      NativeWorldCenter.T h=teams.get(hid),a=teams.get(aid);NativeWorldCenter.S hs=NativeMatchJournal.eligible(h,hid,"league:"+code,seed,effects,players),as=NativeMatchJournal.eligible(a,aid,"league:"+code,seed+17,effects,players);
      double[] neutral=NativeMatchEngine.expected(h,a,hs,as,MatchEngine.DEFAULT,seed),expected=NativeMatchEngine.expected(h,a,hs,as,MatchEngine.profile(code),seed);
      MatchJournal.Match m=NativeMatchJournal.capture(journal,"league:"+code,fixture.r,hid,aid,seed,teams,players,"native-v84",effects,career);
      Stats s=stats[i];for(String club:new String[]{hid,aid}){int changes=0;for(MatchJournal.PlayerFact fact:m.players)if(fact.club.equals(club)&&fact.minutes>0&&fact.stats[3]==0)changes++;JournalHarness.check(changes<=3,"four substitutions");s.subs[changes]++;}s.n++;s.hg+=m.homeGoals;s.ag+=m.awayGoals;if(m.homeGoals==m.awayGoals)s.draw++;if(m.homeGoals+m.awayGoals==0)s.zero++;if(m.homeGoals+m.awayGoals>=4)s.four++;if(m.homeGoals==1&&m.awayGoals==1)s.one++;if(m.homeGoals==2&&m.awayGoals==2)s.two++;if(m.homeGoals==3&&m.awayGoals==3)s.three++;s.neutralH+=neutral[0];s.neutralA+=neutral[1];s.expectedH+=expected[0];s.expectedA+=expected[1];s.intensities.add(expected);
     }
    }
    int previous=NativeCompetitionCalendar.uclCompleted(step-1,38),now=NativeCompetitionCalendar.uclCompleted(step,38);
    for(int r=previous;r<now;r++)for(int g=0;g<8;g++)for(int match=0;match<2;match++){int[] pairs=NativeCompetitionCalendar.UCL_PAIRS[r];NativeMatchJournal.capture(journal,"ucl:2010",r+1,groups.get(g).get(pairs[match*2]),groups.get(g).get(pairs[match*2+1]),NativeWorldCenter.nonce+r*104729+g*1009+match*37,teams,players,"native-v84",effects,career);}
    HashMap<String,String> associations=new HashMap<>();for(NativeCareerStore.Club club:career.clubs)associations.put(club.id,club.league);NativeMatchJournal.knockouts(journal,groups,associations,NativeWorldCenter.nonce,step,38,teams,players,"native-v84",effects,career);EuropaSeason.advance(journal,europaGroups,groups,associations,NativeWorldCenter.nonce,step,38,teams,players,"native-v84",effects,career);
   }
   if((sample-start+1)%10==0)System.out.println("Progress: "+(sample-start+1)+" full seasons");
  }
  System.out.println("league,matches,actualGPM,expectedGPM,targetGPM,homeGPM,awayGPM,drawPct,zeroPct,fourPlusPct,suggestedHomeScale,suggestedAwayScale,oneOnePct,twoTwoPct,threeThreePct");
  for(int i=0;i<5;i++){Stats s=stats[i];double target=(HOME[i]+AWAY[i])/(double)MATCHES[i];
   System.out.printf(Locale.US,"%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.2f,%.2f,%.6f,%.6f,%.2f,%.2f,%.2f%n",C[i],s.n,(s.hg+s.ag)/(double)s.n,(s.expectedH+s.expectedA)/s.n,target,s.hg/(double)s.n,s.ag/(double)s.n,100.0*s.draw/s.n,100.0*s.zero/s.n,100.0*s.four/s.n,HOME[i]/(double)MATCHES[i]/(s.neutralH/s.n),AWAY[i]/(double)MATCHES[i]/(s.neutralA/s.n),100.0*s.one/s.n,100.0*s.two/s.n,100.0*s.three/s.n);
   double subMean=(s.subs[1]+2*s.subs[2]+3*s.subs[3])/(2.0*s.n);System.out.printf(Locale.US,"SUBS,%s,%d,%.4f,zero %.2f%%,one %.2f%%,two %.2f%%,three %.2f%%%n",C[i],2*s.n,subMean,50.0*s.subs[0]/s.n,50.0*s.subs[1]/s.n,50.0*s.subs[2]/s.n,50.0*s.subs[3]/s.n);if(start>=2000){JournalHarness.check(subMean>2.55&&subMean<2.72,"substitution mean "+C[i]);JournalHarness.check(s.subs[1]/(2.0*s.n)<0.08&&s.subs[3]/(2.0*s.n)>0.55,"too many one/two-sub games "+C[i]);}
   if(start<1000){double zero=fit(s,0,0,ZERO[i]/(double)MATCHES[i],0),one=fit(s,zero,0,ONE[i]/(double)MATCHES[i],3),two=fit(s,zero,one,DRAWS[i]/(double)MATCHES[i],10);System.out.printf(Locale.US,"FIT,%s,%.6f,%.6f,%.6f,draw %.4f,zero %.4f,one %.4f%n",C[i],zero,one,two,measure(s,zero,one,two,10),measure(s,zero,one,two,0),measure(s,zero,one,two,3));}
   if(start>=1000){JournalHarness.check(Math.abs((s.hg+s.ag)/(double)s.n-target)<0.065,"league mean outside historical tolerance "+C[i]);JournalHarness.check(Math.abs(s.hg/(double)s.n-HOME[i]/(double)MATCHES[i])<0.055&&Math.abs(s.ag/(double)s.n-AWAY[i]/(double)MATCHES[i])<0.055,"home/away balance "+C[i]);JournalHarness.check(Math.abs((s.expectedH+s.expectedA)/s.n-target)<0.02,"expected rate drift "+C[i]);}
   if(start>=2000){JournalHarness.check(Math.abs(s.draw/(double)s.n-DRAWS[i]/(double)MATCHES[i])<0.015,"draw rate "+C[i]);JournalHarness.check(Math.abs(s.zero/(double)s.n-ZERO[i]/(double)MATCHES[i])<0.010,"zero rate "+C[i]);JournalHarness.check(Math.abs(s.one/(double)s.n-ONE[i]/(double)MATCHES[i])<0.012,"one-one rate "+C[i]);}
  }
  JournalHarness.check(MatchEngine.profile("ucl:2010")==MatchEngine.DEFAULT,"domestic profile leaked into UCL");
  JournalHarness.check(MatchEngine.profile("league:fr")==MatchEngine.profile("fr"),"competition key alias changed calibration");
  if(start>=1000)System.out.println("PASS: held-out league scoring, home/away totals"+(start>=2000?", draws, 0:0, 1:1":"")+" and UCL context isolation; "+samples+" seasons");
 }
}
