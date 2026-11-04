package pfm.android;
import java.util.*;

/** Read-only workload from completed fixture facts, independent of current ownership and legacy forecasts. */
public final class MatchParticipation {
    private MatchParticipation(){}
    public static final class Summary {
        public int matches,covered,substitutions,starts;public final int[] distribution=new int[4];public final HashSet<String> starters=new HashSet<String>();
        public double average(){return covered==0?Double.NaN:substitutions/(double)covered;}
        public String text(int played){return "YOUR CLUB · DOMESTIC LEAGUE ONLY\nRecorded playing time: "+covered+" / "+Math.max(played,matches)+" matches\nExecuted substitutions: "+substitutions+(covered>0?" · "+String.format(Locale.US,"%.2f",average())+" per team match":" · average unavailable")+"\nMatches with 0 / 1 / 2 / 3 substitutions: "+distribution[0]+" / "+distribution[1]+" / "+distribution[2]+" / "+distribution[3]+"\nPlayers used in starting XI: "+starters.size()+" · starts: "+starts+(covered<Math.max(played,matches)?"\nEarlier matches without reliable playing time are excluded from the average.":"")+(covered>0&&covered<10?"\nSmall sample; individual matches can differ from a season average.":"");}
    }
    public static Summary summarize(Iterable<MatchJournal.Match> records,String competition,String club){Summary out=new Summary();HashSet<String> seen=new HashSet<String>();
        for(MatchJournal.Match m:records){if(!m.competition.equals(competition)||(!club.equals(m.home)&&!club.equals(m.away))||!seen.add(m.id))continue;out.matches++;
            if(m.effectsVersion<2&&!m.report.contains("\nLIVE EVENTS\n"))continue;
            int starts=0,subs=0;HashSet<String> ids=new HashSet<String>();for(MatchJournal.PlayerFact p:m.players)if(club.equals(p.club)&&p.minutes>0&&ids.add(p.uid)){if(p.stats[3]>0)starts++;else subs++;}
            if(starts!=11||subs>3)continue;out.covered++;out.starts+=starts;out.substitutions+=subs;out.distribution[subs]++;for(MatchJournal.PlayerFact p:m.players)if(club.equals(p.club)&&p.minutes>0&&p.stats[3]>0)out.starters.add(p.uid);
        }return out;
    }
    public static Map<String,int[]> completeTotals(java.util.List<MatchJournal.Match> records,String competition,int played){HashMap<String,int[]> out=new HashMap<String,int[]>();if(played<=0)return out;HashSet<String> clubs=new HashSet<String>();for(MatchJournal.Match m:records)if(m.competition.equals(competition)){clubs.add(m.home);clubs.add(m.away);}if(clubs.isEmpty())return out;
        for(String club:clubs){Summary s=summarize(records,competition,club);if(s.matches!=played||s.covered!=played)return new HashMap<String,int[]>();}
        HashSet<String> seen=new HashSet<String>();for(MatchJournal.Match m:records)if(m.competition.equals(competition)&&seen.add(m.id))for(MatchJournal.PlayerFact p:m.players)if(p.minutes>0){int[] value=out.get(p.uid);if(value==null){value=new int[4];out.put(p.uid,value);}value[0]++;value[1]+=p.stats[3]>0?1:0;if(p.stats[4]>0){value[2]+=p.stats[4];value[3]++;}}
        return out;
    }
}
