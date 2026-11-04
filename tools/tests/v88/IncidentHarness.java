import pfm.android.MatchEngine;
import java.util.*;

public class IncidentHarness {
    static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    static MatchEngine.Lineup lineup(int seed){MatchEngine.Lineup s=new MatchEngine.Lineup(seed);for(int i=0;i<18;i++)s.add(new MatchEngine.Member("p"+i,i==0||i==11?0:i<5||i==12||i==13?1:i<9||i<16?2:3,75,75,75,55,10),i<11);s.plan(65,"p12");s.plan(72,"p14");s.plan(79,"p16");return s;}
    static void play(MatchEngine.Lineup s,int from,int to){for(int m=from;m<=to;m++){s.begin(m);MatchEngine.Member scorer=s.scorer(m);check(s.active(scorer.uid),"inactive scorer");s.end(m);}}
    static String facts(MatchEngine.Lineup s){StringBuilder b=new StringBuilder();for(MatchEngine.Member p:s.squad)b.append(p.uid+":"+p.minutes+":"+p.yellow+":"+p.red+":"+p.injury+":"+p.entryMinute+":"+p.exitMinute+";");for(MatchEngine.Incident e:s.incidents)b.append(e.minute+":"+e.type+":"+e.uid+":"+e.other+";");return b.toString();}
    public static void main(String[] args){
        MatchEngine.Lineup s=lineup(123);s.scripted(20,"p1","RC");s.scripted(30,"p0","INJ");s.scripted(10,"p2","YC");s.scripted(40,"p2","YC");
        play(s,1,40);check(s.find("p1").red==1&&s.find("p1").minutes==20,"straight red did not stop playing time");check(s.find("p2").yellow==2&&s.find("p2").red==1&&s.find("p2").minutes==40,"second yellow did not dismiss");
        check(s.find("p0").minutes==30&&s.find("p0").injury>0&&s.find("p11").entryMinute==31,"injured goalkeeper was not replaced next minute");
        double after=MatchEngine.expectedGoals(s.team(),lineup(999).team(),true);double before=MatchEngine.expectedGoals(lineup(123).team(),lineup(999).team(),true);check(after<before,"dismissals did not reduce future scoring intensity");
        s.manual("p1","p13",41);check(!s.active("p13"),"manual substitution replaced a red card");play(s,41,90);
        check(s.find("p1").minutes==20&&s.find("p2").minutes==40&&s.find("p0").minutes==30&&s.substitutions<=3,"removed players re-entered or substitution limit exceeded");
        MatchEngine.Lineup exhausted=lineup(811);exhausted.scripted(10,"p1","INJ");exhausted.scripted(20,"p2","INJ");exhausted.scripted(30,"p3","INJ");exhausted.scripted(40,"p0","INJ");play(exhausted,1,90);
        check(exhausted.substitutions==3&&exhausted.find("p0").minutes==40&&exhausted.find("p11").minutes==0,"fourth replacement allowed after exhaustion");
        MatchEngine.Lineup late=lineup(55);late.scripted(90,"p0","INJ");play(late,1,90);check(late.find("p0").minutes==90&&late.find("p11").minutes==0,"90th-minute injury invented reserve appearance");
        for(int seed=0;seed<1000;seed++){MatchEngine.Lineup a=lineup(seed),b=lineup(seed);play(a,1,90);play(b,1,37);String prefix=facts(b);play(b,38,90);check(facts(a).equals(facts(b)),"chunking changed incidents");check(prefix.length()>0,"empty prefix");for(MatchEngine.Member p:a.squad){check(p.minutes>=0&&p.minutes<=90&&p.yellow<=2&&p.red<=1,"invalid participant facts");if(p.exitMinute>0)check(p.minutes<=p.exitMinute-p.entryMinute+1,"minutes after exit");}}
        boolean rejected=false;try{late.end(90);}catch(IllegalStateException expected){rejected=true;}check(rejected,"repeated incident minute accepted");
        System.out.println("PASS: straight/second-yellow dismissals, weaker future attack, goalkeeper injury/replacement, exhausted bench, minute 90, no inactive scorers, 1000 chunked timelines and duplicate-minute guard");
    }
}
