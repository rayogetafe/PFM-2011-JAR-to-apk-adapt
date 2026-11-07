package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import java.io.*;
import java.util.*;

/** Calendar writer and read-only projections for every competition's fixture facts. */
public final class NativeMatchJournal {
    private NativeMatchJournal() {}
    private static int identity=Integer.MIN_VALUE,lastSeason=Integer.MIN_VALUE,lastRound=-1;
    private static final HashMap<Integer,MatchJournal> journals=new HashMap<Integer,MatchJournal>();
    private static Context context;
    private static volatile MatchEffects currentEffects;
    private static volatile int effectsSeason=Integer.MIN_VALUE,effectsIdentity=Integer.MIN_VALUE;
    private static final ThreadLocal<Boolean> restoring=new ThreadLocal<Boolean>();
    private static volatile Map<Integer,String> legacyIds=new HashMap<Integer,String>();
    private static final HashMap<String,int[]> legacyCondition=new HashMap<String,int[]>();
    private static String participationRepair="";
    private static void repairParticipation(MatchJournal j,String code,int played)throws Exception{if(legacyIds.isEmpty())return;String key=identity+":"+NativeWorldCenter.season()+":"+j.size()+":"+played+":"+legacyIds.hashCode();if(key.equals(participationRepair))return;
        Map<String,int[]> totals=MatchParticipation.completeTotals(j.all(),"league:"+code,played);if(totals.isEmpty())return;HashMap<Integer,int[]> ids=new HashMap<Integer,int[]>();for(Map.Entry<Integer,String> entry:legacyIds.entrySet())ids.put(entry.getKey(),totals.containsKey(entry.getValue())?totals.get(entry.getValue()):new int[4]);
        try{Class.forName("PfmParticipationBridge90").getMethod("reconcile",Map.class).invoke(null,ids);participationRepair=key;}catch(ClassNotFoundException headless){/* Pure native audit has no domestic core ledger. */}}
    public static String key(String competition,int round,String home,String away){return competition+"|"+round+"|"+home+"|"+away;}
    public static String squadWorkload(Context c){try{NativeWorldCenter.load(c);Class<?> bridge=Class.forName("PfmSeasonBridge");String club=String.valueOf(bridge.getMethod("clubId").invoke(null)),code=String.valueOf(bridge.getMethod("leagueCode").invoke(null));int played=((Integer)bridge.getMethod("playedMatches").invoke(null)).intValue();MatchJournal j=journal(c,NativeWorldCenter.season());repairParticipation(j,code,played);return MatchParticipation.summarize(j.all(),"league:"+code,club).text(played);}catch(Exception error){return "Recorded match workload is unavailable. No forecast is substituted for played matches.";}}
    private static MatchJournal journal(Context c,int season)throws IOException{
        int now=NativeWorldCenter.identity();if(identity!=now){journals.clear();identity=now;lastSeason=Integer.MIN_VALUE;lastRound=-1;currentEffects=null;legacyIds=new HashMap<Integer,String>();legacyCondition.clear();}
        MatchJournal j=journals.get(season);if(j==null){Iterator<Integer> years=journals.keySet().iterator();while(years.hasNext()){int y=years.next();if(y!=2010&&y!=NativeWorldCenter.season())years.remove();}j=new MatchJournal(new File(c.getFilesDir(),"match-journals"),now,season);journals.put(season,j);}return j;
    }
    static HashMap<String,NativeWorldCenter.T> teams(NativeCareerStore.Career career){
        HashMap<String,NativeWorldCenter.T> out=new HashMap<String,NativeWorldCenter.T>();
        for(NativeCareerStore.Club club:career.clubs){
            NativeWorldCenter.T team=new NativeWorldCenter.T();team.id=club.sourceId;team.name=club.name;team.style=55;team.form=2;
            ArrayList<NativeWorldCenter.T> source=NativeWorldCenter.leagues.get(club.league);
            if(source!=null&&club.sourceId>=0&&club.sourceId<source.size()){NativeWorldCenter.T t=source.get(club.sourceId);team.style=t.style;team.form=t.form;}
            out.put(club.id,team);
        }
        for(NativeCareerStore.Player player:career.players){
            NativeWorldCenter.T team=out.get(player.owner);if(team==null)continue;
            NativeWorldCenter.P p=new NativeWorldCenter.P();p.id=player.sourceId;p.uid=player.id;p.name=player.name;p.nat=player.nationality;
            p.pos=player.position;p.age=player.age;p.ovr=player.overall;p.spe=player.spe;p.res=player.res;p.qua=player.qua;p.mor=player.morale;team.ps.add(p);
        }
        for(NativeWorldCenter.T t:out.values()){int total=0;for(NativeWorldCenter.P p:t.ps)total+=p.ovr;t.ovr=t.ps.isEmpty()?60:total/t.ps.size();}
        return out;
    }
    private static int[] stats(NativeWorldCenter.P p){return new int[]{p.g,p.a,p.ap,p.st,p.rs,p.cs,p.ga,p.sv,p.fat};}
    private static void facts(MatchJournal.Match m,String club,NativeWorldCenter.T team,int gf,int ga,int seed,NativeWorldCenter.S selection,MatchEngine.Lineup side){
        for(NativeWorldCenter.P p:team.ps){MatchJournal.PlayerFact f=new MatchJournal.PlayerFact();f.uid=p.uid;f.club=club;MatchEngine.Member q=side.find(p.uid);f.stats[8]=p.fat;
            if(q!=null&&q.minutes>0){f.name=p.name;f.minutes=q.minutes;f.yellow=q.yellow;f.red=q.red;f.injury=q.injury;f.stats[0]=q.goals;f.stats[1]=q.assists;f.stats[2]=1;f.stats[3]=q.starter?1:0;f.stats[4]=Math.max(10,63+(gf-ga)*2+(p.ovr-team.ovr)/4+(MatchEngine.mix(seed^p.uid.hashCode())&0x7fffffff)%7);
                if(p.pos==0){f.stats[6]=q.conceded;f.stats[5]=q.minutes>=60&&q.conceded==0?1:0;f.stats[7]=Math.max(0,(2+(MatchEngine.mix(seed^p.id)&0x7fffffff)%5)*q.minutes/90-q.conceded/2);}
                p.g+=f.stats[0];p.a+=f.stats[1];p.ap++;p.st+=f.stats[3];p.rs+=f.stats[4];p.cs+=f.stats[5];p.ga+=f.stats[6];p.sv+=f.stats[7];}
            m.players.add(f);}
        StringBuilder log=new StringBuilder();for(MatchEngine.Incident event:side.incidents){MatchEngine.Member p=side.find(event.uid);NativeWorldCenter.P named=null,other=null;for(NativeWorldCenter.P q:team.ps){if(q.uid.equals(event.uid))named=q;if(q.uid.equals(event.other))other=q;}
            log.append(event.minute).append("' ").append(event.type).append(" · ").append(named==null?event.uid:named.name);if(other!=null)log.append(" → ").append(other.name);if(event.type.equals("INJ"))log.append(" (").append(p.injury).append(" rounds)");log.append('\n');}
        if(log.length()>0)m.report+="\n"+team.name+"\n"+log;
    }
    static NativeWorldCenter.S eligible(NativeWorldCenter.T team,String club,String competition,int seed,MatchEffects effects,HashMap<String,NativeWorldCenter.P> players){
        NativeWorldCenter.T fit=new NativeWorldCenter.T();
        for(NativeWorldCenter.P p:team.ps){int[] base=legacyCondition.get(p.uid);p.fat=Math.min(100,effects.fatigue(p.uid)+(base==null?0:base[0]));if(effects.available(p.uid,competition)&&(base==null||base[1]==0))fit.ps.add(p);}
        int keepers=0;for(NativeWorldCenter.P p:fit.ps)if(p.pos==0)keepers++;
        // Explicit temporary academy cover; never silently select injured players or rewrite career rosters.
        int cover=0,outfield=fit.ps.size()-keepers;while(outfield<10||keepers==0){NativeWorldCenter.P p=new NativeWorldCenter.P();p.pos=keepers==0?0:1+cover%3;p.uid="cover:"+NativeWorldCenter.season()+":"+club+":"+seed+":"+cover++;p.name=p.pos==0?"Academy goalkeeper":"Academy cover";p.nat="Academy";p.ovr=p.spe=p.res=p.qua=45;p.mor=55;p.id=p.uid.hashCode();fit.ps.add(p);team.ps.add(p);players.put(p.uid,p);if(p.pos==0)keepers++;else outfield++;}
        NativeWorldCenter.S selected=NativeWorldCenter.selection(fit,seed);
        for(NativeWorldCenter.P p:fit.ps)if(!selected.xi.contains(p))selected.bench.add(p);
        return selected;
    }
    static MatchJournal.Match capture(MatchJournal j,String competition,int round,String homeId,String awayId,int seed,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career){
        return capture(j,competition,round,homeId,awayId,seed,teams,players,source,effects,career,null,false);
    }
    static MatchJournal.Match capture(MatchJournal j,String competition,int round,String homeId,String awayId,int seed,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career,MatchJournal.Match firstLeg,boolean neutralFinal){
        String id=key(competition,round,homeId,awayId);MatchJournal.Match old=j.get(id);if(old!=null){effects.apply(old);return old;}
        NativeWorldCenter.T home=teams.get(homeId),away=teams.get(awayId);if(home==null||away==null||home.ps.size()<11||away.ps.size()<11)throw new IllegalStateException("Fixture squad unavailable: "+id);
        NativeWorldCenter.S hs=eligible(home,homeId,competition,seed,effects,players),as=eligible(away,awayId,competition,seed+17,effects,players);
        boolean domesticCup = competition.startsWith("cup:");
        MatchEngine.Profile profile=neutralFinal?MatchEngine.NEUTRAL_VENUE:MatchEngine.profile(competition);
        NativeMatchEngine.Result result=NativeMatchEngine.play(seed,home,away,hs,as,profile);
        int decision=0;if(firstLeg!=null||neutralFinal||domesticCup){decision=knockoutDecision(result,firstLeg);
            if(decision==0){NativeMatchEngine.extraTime(result,seed,profile);decision=knockoutDecision(result,firstLeg);}
            if(decision==0){NativeMatchEngine.penalties(result,seed^0x741e);decision=Integer.compare(result.homePenalties,result.awayPenalties);}}
        MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.competition=competition;m.round=round;m.home=homeId;m.away=awayId;m.homeGoals=result.homeGoals;m.awayGoals=result.awayGoals;m.seed=seed;m.source=source;m.duration=result.duration;m.homePenalties=result.homePenalties;m.awayPenalties=result.awayPenalties;if(decision!=0)m.winner=decision>0?homeId:awayId;
        m.report=home.name+"  "+m.homeGoals+" – "+m.awayGoals+"  "+away.name;
        if(neutralFinal)m.report+=domesticCup?"\nFinal · neutral venue":"\nFinal · neutral venue ("+(competition.startsWith("uel:")?"Dublin":"Wembley")+")";
        if(m.duration==120)m.report+="\nAfter extra time (120 minutes)";
        if(m.homePenalties+m.awayPenalties>0)m.report+="\nPenalties: "+m.homePenalties+" – "+m.awayPenalties+" (excluded from player goals)";
        if(firstLeg!=null)m.report+="\nAggregate: "+(firstLeg.awayGoals+m.homeGoals)+" – "+(firstLeg.homeGoals+m.awayGoals)+"; away goals: "+firstLeg.awayGoals+" – "+m.awayGoals;
        if(m.winner.length()>0)m.report+="\nQualified / winner: "+(decision>0?home.name:away.name);
        facts(m,homeId,home,m.homeGoals,m.awayGoals,seed,hs,result.home);facts(m,awayId,away,m.awayGoals,m.homeGoals,seed+17,as,result.away);
        if (source.equals("native-v84")) {
            m.clock = effects.clock;
            m.effectsVersion = 2;
            MatchEffects.stamp(m, players);
            if (!domesticCup) {
                long homePayroll = 0, awayPayroll = 0;
                for (NativeCareerStore.Player player : career.players) {
                    if (player.owner.equals(homeId)) homePayroll += player.contractWage;
                    if (player.owner.equals(awayId)) awayPayroll += player.contractWage;
                }
                int leagueRounds = competition.startsWith("league:") ?
                        NativeCompetitionCalendar.leagueRounds(competition.substring(7)) : 38;
                boolean european = EuropeanPlaces.european(competition);
                m.homeCredit = MatchEffects.credit(european, true, m.homeGoals, m.awayGoals, home.ovr, homePayroll, leagueRounds);
                m.awayCredit = MatchEffects.credit(european, false, m.awayGoals, m.homeGoals, away.ovr, awayPayroll, leagueRounds);
            }
        }
        if(neutralFinal&&!domesticCup&&m.effectsVersion>0)m.homeCredit=MatchEffects.credit(true,false,m.homeGoals,m.awayGoals,home.ovr,0,38);
        if(competition.startsWith("uel:")&&m.effectsVersion>0){m.homeCredit=m.homeCredit*2/3;m.awayCredit=m.awayCredit*2/3;}
        if(competition.startsWith("uclq:")&&m.effectsVersion>0){m.homeCredit=m.homeCredit/3;m.awayCredit=m.awayCredit/3;}
        if(m.effectsVersion>0)for(MatchJournal.PlayerFact f:m.players){int[] base=legacyCondition.get(f.uid);f.stats[8]=Math.min(100,effects.fatigue(f.uid)+f.fatigueDelta+(base==null?0:base[0]));}
        j.put(m);effects.apply(m);return m;
    }
    private static int knockoutDecision(NativeMatchEngine.Result result, MatchJournal.Match firstLeg) {
        if (firstLeg == null) return Integer.compare(result.homeGoals, result.awayGoals);
        return UclKnockout.decision(firstLeg.homeGoals, firstLeg.awayGoals, result.homeGoals, result.awayGoals);
    }
    private static void minutes(MatchJournal.Match m,String club,NativeWorldCenter.S s){HashMap<NativeWorldCenter.P,Integer> minutes=new HashMap<NativeWorldCenter.P,Integer>();for(NativeWorldCenter.P p:s.xi)minutes.put(p,90);ArrayList<NativeWorldCenter.P> xi=new ArrayList<NativeWorldCenter.P>(s.xi);int sub=0;for(NativeWorldCenter.P p:s.used)if(!s.xi.contains(p)&&p.pos!=0){int at=65+sub++*7;for(int i=xi.size()-1;i>=0;i--)if(xi.get(i).pos==p.pos){minutes.put(xi.get(i),at-1);minutes.put(p,91-at);xi.set(i,p);break;}}for(MatchJournal.PlayerFact f:m.players)if(f.club.equals(club))for(Map.Entry<NativeWorldCenter.P,Integer> e:minutes.entrySet())if(f.uid.equals(e.getKey().uid))f.minutes=e.getValue();}
    private static void importLegacy(Context c,MatchJournal j,int through,NativeCareerStore.Career career)throws Exception{
        @SuppressWarnings("unchecked") ArrayList<MatchJournal.Match> matches=(ArrayList<MatchJournal.Match>)Class.forName("PfmJournalBridge83").getMethod("matches",Integer.TYPE).invoke(null,through);
        HashMap<Integer,String> ids=new HashMap<Integer,String>();for(NativeCareerStore.Player p:career.players)if(p.id.startsWith(career.activeLeague+":"))ids.put(p.sourceId,p.id);
        for(Map.Entry<String,?> e:c.getSharedPreferences("pfm_native_legacy_ids_v58",0).getAll().entrySet())if(e.getValue() instanceof Integer)ids.put((Integer)e.getValue(),e.getKey());
        legacyIds=ids;
        legacyCondition.clear();for(Map.Entry<Integer,String> e:ids.entrySet())try{legacyCondition.put(e.getValue(),(int[])Class.forName("PfmEffectsBridge84").getMethod("condition",Integer.TYPE).invoke(null,e.getKey()));}catch(Exception ignored){}
        for(MatchJournal.Match m:matches){if(m.homeGoals==255||m.awayGoals==255)continue;for(MatchJournal.PlayerFact f:m.players){int id=Integer.parseInt(f.uid.substring(f.uid.lastIndexOf(':')+1));if(ids.containsKey(id))f.uid=ids.get(id);}j.put(m);}
    }
    private static void importUcl(Context c,MatchJournal j,ArrayList<ArrayList<String>> groups,HashMap<String,NativeWorldCenter.T> teams){
        for(int r=0;r<6;r++)for(int g=0;g<8;g++)for(int match=0;match<2;match++){
            String score=c.getSharedPreferences("pfm_ucl_groups_v79",0).getString(identity+"_2010_"+r+"_"+g+"_"+match,null);if(score==null)continue;
            String home=groups.get(g).get(NativeCompetitionCalendar.UCL_PAIRS[r][match*2]),away=groups.get(g).get(NativeCompetitionCalendar.UCL_PAIRS[r][match*2+1]);String id=key("ucl:2010",r+1,home,away);if(j.get(id)!=null)continue;
            String[] values=score.split(":");MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.competition="ucl:2010";m.round=r+1;m.home=home;m.away=away;m.homeGoals=Integer.parseInt(values[0]);m.awayGoals=Integer.parseInt(values[1]);m.source="ucl-score-import";
            m.report=teams.get(home).name+"  "+m.homeGoals+" – "+m.awayGoals+"  "+teams.get(away).name;j.put(m);
        }
    }
    private static MatchEffects effects(Context c, MatchJournal journal, int season, int completed) throws IOException {
        int total = NativeTransferCalendar.total();
        MatchEffects state = new MatchEffects(total);
        if (!journal.openingDefined() || !journal.europeanOpeningDefined()) {
            MatchEffects previousEffects = new MatchEffects(total);
            // Older journals with played fixtures keep their original starting conditions.
            if (journal.size() == 0 && completed <= 1 && season > 2010) {
                MatchJournal previous = new MatchJournal(new File(c.getFilesDir(), "match-journals"),
                        NativeWorldCenter.identity(), season - 1);
                previousEffects.openingBans(previous.openingBans());
                previousEffects.openingEuropeanBans(season - 1, previous.europeanOpeningBans());
                replayEffects(previousEffects, previous.all());
            }
            journal.defineOpeningBans(previousEffects.carryBans());
            journal.defineEuropeanOpeningBans(previousEffects.carryEuropeanBans(season - 1));
        }
        state.openingBans(journal.openingBans());
        state.openingEuropeanBans(season, journal.europeanOpeningBans());
        return state;
    }

    private static void replayEffects(MatchEffects state, List<MatchJournal.Match> matches) {
        Collections.sort(matches, (a, b) -> Integer.compare(a.clock, b.clock));
        for (MatchJournal.Match match : matches) {
            if (match.effectsVersion > 0) {
                state.rest(match.clock);
                state.apply(match);
            }
        }
    }
    public static synchronized void advance(Context c,int completed)throws Exception{
        context=c.getApplicationContext();NativeWorldCenter.load(c);int season=NativeWorldCenter.season(),total=NativeTransferCalendar.total();completed=Math.min(total,Math.max(0,completed));
        MatchJournal j=journal(c,season);if(lastSeason==season&&completed<=lastRound)return;
        if(completed==0&&season==2010){lastSeason=season;lastRound=0;return;}
        boolean baseline=j.size()==0&&completed>1;String source=baseline?"reconstructed-v83":"native-v84";
        if (j.metadata("cup-v95-cutoff") == null) {
            int cutoff = baseline ? completed : 0;
            for (MatchJournal.Match old : j.all()) cutoff = Math.max(cutoff, old.clock);
            if (cutoff == 0 && j.size() > 0) cutoff = completed;
            j.define("cup-v95-cutoff", String.valueOf(cutoff));
        }
        if(season==2010&&j.metadata("ucl-v91-cutoff")==null){int cutoff=baseline?completed:0;for(MatchJournal.Match old:j.all())cutoff=Math.max(cutoff,old.clock);j.define("ucl-v91-cutoff",String.valueOf(cutoff));}
        NativeCareerStore.Career career=NativeCareerStore.ensure(c);HashMap<String,NativeWorldCenter.T> teams=teams(career);HashMap<String,NativeWorldCenter.P> players=new HashMap<String,NativeWorldCenter.P>();for(NativeWorldCenter.T team:teams.values())for(NativeWorldCenter.P p:team.ps)players.put(p.uid,p);
        if(season>2010&&j.metadata("ucl-groups")==null&&j.metadata("uel-groups")==null){if(j.metadata("uclq-cutoff")==null){int cutoff=baseline?completed:0;for(MatchJournal.Match old:j.all())cutoff=Math.max(cutoff,old.clock);j.define("uclq-cutoff",String.valueOf(cutoff));}EuropeanQualification.prepare(j,journal(c,season-1),career,identity^season*10007);}
        ArrayList<ArrayList<String>> groups;
        if(season==2010){groups=NativeCompetitionCalendar.groups(c);importUcl(c,j,groups,teams);}
        else {if(j.metadata("ucl-v92-cutoff")==null){int cutoff=baseline?completed:0;for(MatchJournal.Match old:j.all())cutoff=Math.max(cutoff,old.clock);j.define("ucl-v92-cutoff",String.valueOf(cutoff));}groups=EuropeanQualification.planned(j)?UclSeason.decode(j.metadata("ucl-groups")):UclSeason.prepare(j,journal(c,season-1),career,identity^season*10007);}
        if(j.metadata("uel-v93-cutoff")==null){int cutoff=baseline?completed:0;for(MatchJournal.Match old:j.all())cutoff=Math.max(cutoff,old.clock);j.define("uel-v93-cutoff",String.valueOf(cutoff));}
        ArrayList<ArrayList<String>> europaGroups=EuropeanQualification.planned(j)?UclSeason.decode(j.metadata("uel-groups")):EuropaSeason.prepare(j,journal(c,season-1),career,groups,identity^season*10007^0x3e17);
        String competition=UclSeason.competition(season);
        HashMap<String,String> associations=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)associations.put(club.id,EuropeanPlaces.association(club.league));
        MatchEffects effects=effects(c,j,season,completed);
        importLegacy(c,j,completed,career);
        Map<String, ArrayList<NativeWorldCenter.M>> parallel = new LinkedHashMap<>();
        for (String code : NativeWorldCenter.C) {
            ArrayList<NativeWorldCenter.T> clubs = NativeWorldCenter.leagues.get(code);
            if (clubs != null && !code.equals(career.activeLeague)) parallel.put(code, NativeWorldCenter.schedule(clubs.size()));
        }
        if (NativeChampionship.active(career, season)) {
            parallel.put(NativeChampionship.CODE, NativeWorldCenter.schedule(NativeChampionship.CLUBS));
        }
        for(int step=1;step<=completed;step++){
            effects.rest(step);
            for(String code:parallel.keySet()){
                int previous=NativeCompetitionCalendar.parallelRound(step-1,total,code),now=NativeCompetitionCalendar.parallelRound(step,total,code);
                for(NativeWorldCenter.M fixture:parallel.get(code))if(fixture.r>previous&&fixture.r<=now){int seed=identity+season*10007+code.hashCode()*101+fixture.r*37+fixture.h*11+fixture.a;capture(j,"league:"+code,fixture.r,code+":"+fixture.h,code+":"+fixture.a,seed,teams,players,source,effects,career);}
            }
            EuropeanQualification.advance(j,identity,step,total,teams,players,source,effects,career);
            if(EuropeanQualification.planned(j)){groups=UclSeason.decode(j.metadata("ucl-groups"));europaGroups=UclSeason.decode(j.metadata("uel-groups"));}
            {int previous=NativeCompetitionCalendar.uclCompleted(step-1,total),now=NativeCompetitionCalendar.uclCompleted(step,total);
                for(int r=previous;r<now;r++)for(int g=0;g<8;g++)for(int match=0;match<2;match++){int[] pairs=NativeCompetitionCalendar.UCL_PAIRS[r];capture(j,competition,r+1,groups.get(g).get(pairs[match*2]),groups.get(g).get(pairs[match*2+1]),identity+r*104729+g*1009+match*37+(season-2010)*1000003,teams,players,season>2010&&step<=Integer.parseInt(j.metadata("ucl-v92-cutoff"))?"reconstructed-v92":source,effects,career);}
                knockouts(j,groups,associations,identity,step,total,teams,players,source,effects,career);
                EuropaSeason.advance(j,europaGroups,groups,associations,identity,step,total,teams,players,source,effects,career);
            }
            DomesticCup.advance(j, identity, step, total, teams, players, source, effects, career);
        }
        j.save();
        if(completed==total){MatchJournal next=journal(c,season+1);if(next.metadata("ucl-groups")!=null||next.metadata("uel-groups")!=null){ArrayList<ArrayList<String>> nextUcl=UclSeason.prepare(next,j,career,identity^(season+1)*10007);EuropaSeason.prepare(next,j,career,nextUcl,identity^(season+1)*10007^0x3e17);}else EuropeanQualification.prepare(next,j,career,identity^(season+1)*10007);next.save();}
        repairParticipation(j,career.activeLeague,completed);currentEffects=effects;effectsSeason=season;effectsIdentity=identity;NativeCareerStore.applyMatchFinance(c,j.all(),season);lastSeason=season;lastRound=completed;
    }
    static void knockouts(MatchJournal j,ArrayList<ArrayList<String>> groups,HashMap<String,String> associations,int nonce,int step,int total,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career){
                String competition=UclSeason.competition(j.season()),cutoffKey=j.season()==2010?"ucl-v91-cutoff":"ucl-v92-cutoff";int drawSeed=nonce^0x201011^(j.season()-2010)*1000003;
                int before=UclKnockout.completed(step-1,total),after=UclKnockout.completed(step,total);
                for(int k=before;k<after;k++){int stage=Math.min(3,k/2),round=k+7;ArrayList<String[]> ties=UclKnockout.draw(j,groups,associations,drawSeed,stage,competition);if(ties.isEmpty())throw new IllegalStateException("UCL previous stage incomplete");
                    for(int t=0;t<ties.size();t++){String[] tie=ties.get(t);boolean second=k<6&&k%2==1;String home=second?tie[1]:tie[0],away=second?tie[0]:tie[1];MatchJournal.Match first=second?j.get(UclKnockout.key(competition,round-1,away,home)):null;
                        String koSource=j.metadata(cutoffKey)!=null&&step<=Integer.parseInt(j.metadata(cutoffKey))?"reconstructed-v91":source;
                        capture(j,competition,round,home,away,nonce^round*104729^t*1009^(j.season()-2010)*1000003,teams,players,koSource,effects,career,first,k==6);}}
                if(NativeCompetitionCalendar.uclCompleted(step,total)==6)for(int stage=0;stage<4;stage++)UclKnockout.draw(j,groups,associations,drawSeed,stage,competition);
    }
    /** Non-mutating legacy overlay. Domestic suspensions/cards stay in the domestic ledger. */
    public static int[] legacyEffects(int id){
        if(Boolean.TRUE.equals(restoring.get()))return new int[5];
        if(effectsIdentity!=NativeWorldCenter.careerNonce||effectsSeason!=NativeWorldCenter.season()||currentEffects==null){
            restoring.set(Boolean.TRUE);try{Context c=AndroidRuntime.context();NativeWorldCenter.load(c);int season=NativeWorldCenter.season(),nonce=NativeWorldCenter.identity();MatchJournal j=new MatchJournal(new File(c.getFilesDir(),"match-journals"),nonce,season);MatchEffects state=effects(c,j,season,NativeTransferCalendar.played());
                ArrayList<MatchJournal.Match> records=j.all();replayEffects(state,records);state.rest(NativeTransferCalendar.played());
                HashMap<Integer,String> ids=new HashMap<Integer,String>();if(!records.isEmpty()||!j.europeanOpeningBans().isEmpty()){NativeCareerStore.Career career=NativeCareerStore.ensure(c);for(NativeCareerStore.Player p:career.players)if(p.id.startsWith(career.activeLeague+":"))ids.put(p.sourceId,p.id);for(Map.Entry<String,?> e:c.getSharedPreferences("pfm_native_legacy_ids_v58",0).getAll().entrySet())if(e.getValue() instanceof Integer)ids.put((Integer)e.getValue(),e.getKey());}
                if(!records.isEmpty())NativeCareerStore.applyMatchFinance(c,records,season);if(currentEffects==null||effectsIdentity!=nonce||effectsSeason!=season||currentEffects.clock<=state.clock){legacyIds=ids;currentEffects=state;effectsSeason=season;effectsIdentity=nonce;}
            }catch(Exception e){throw new IllegalStateException("Saved match effects unavailable",e);}finally{restoring.remove();}
        }
        String uid=legacyIds.get(id);MatchEffects snapshot=currentEffects;if(uid==null||snapshot==null)return new int[5];MatchEffects.Discipline d=snapshot.readDiscipline(UclSeason.competition(NativeWorldCenter.season()),uid);return new int[]{snapshot.fatigue(uid),snapshot.injury(uid),d.yellow,d.red,d.ban};
    }
    static String conditionText(String uid){if(currentEffects==null)return "";MatchEffects.Discipline d=currentEffects.discipline(UclSeason.competition(NativeWorldCenter.season()),uid);String league="";for(Map.Entry<String,ArrayList<NativeWorldCenter.T>> entry:NativeWorldCenter.leagues.entrySet())for(NativeWorldCenter.T team:entry.getValue())for(NativeWorldCenter.P p:team.ps)if(uid.equals(p.uid))league=entry.getKey();MatchEffects.Discipline domestic=currentEffects.discipline("league:"+league,uid);return "\nFatigue: "+currentEffects.fatigue(uid)+"/100\nInjury: "+currentEffects.injury(uid)+" league rounds\nLeague: YC "+domestic.yellow+" RC "+domestic.red+"; ban "+domestic.ban+" matches\nEurope: YC "+d.yellow+" RC "+d.red+"; ban "+d.ban+" European matches";}
    private static void add(NativeWorldCenter.T home,NativeWorldCenter.T away,int hg,int ag){home.p++;away.p++;home.gf+=hg;home.ga+=ag;away.gf+=ag;away.ga+=hg;if(hg>ag){home.w++;away.l++;home.pts+=3;}else if(hg<ag){away.w++;home.l++;away.pts+=3;}else{home.d++;away.d++;home.pts++;away.pts++;}}
    static ArrayList<NativeWorldCenter.M> project(String code,int upto,int season)throws Exception{
        Context c=context==null?AndroidRuntime.context():context;if(season==NativeWorldCenter.season())advance(c,NativeTransferCalendar.played());
        MatchJournal journal=journal(c,season);ArrayList<NativeWorldCenter.T> teams=NativeWorldCenter.leagues.get(code);HashMap<String,NativeWorldCenter.P> players=new HashMap<String,NativeWorldCenter.P>();
        for(NativeWorldCenter.T team:teams){team.p=team.w=team.d=team.l=team.gf=team.ga=team.pts=0;for(NativeWorldCenter.P p:team.ps){p.g=p.a=p.ap=p.st=p.rs=p.cs=p.ga=p.sv=p.fat=0;players.put(p.uid,p);}}
        ArrayList<NativeWorldCenter.M> fixtures=NativeWorldCenter.schedule(teams.size());for(NativeWorldCenter.M fixture:fixtures){fixture.hg=fixture.ag=-1;if(fixture.r>upto)continue;MatchJournal.Match m=journal.get(key("league:"+code,fixture.r,code+":"+fixture.h,code+":"+fixture.a));if(m==null)continue;
            fixture.hg=m.homeGoals;fixture.ag=m.awayGoals;add(teams.get(fixture.h),teams.get(fixture.a),fixture.hg,fixture.ag);
            for(MatchJournal.PlayerFact f:m.players){NativeWorldCenter.P p=players.get(f.uid);if(p==null)continue;int[] s=f.stats;p.g+=s[0];p.a+=s[1];p.ap+=s[2];p.st+=s[3];p.rs+=s[4];p.cs+=s[5];p.ga+=s[6];p.sv+=s[7];if(s[8]>=0)p.fat=s[8];}
        }
        return fixtures;
    }
    static ArrayList<MatchJournal.Match> ucl(Context c)throws IOException{return journal(c,2010).competition("ucl:2010");}
    static ArrayList<String[]> uclDraw(Context c,int stage)throws IOException{return UclKnockout.decode(journal(c,2010).metadata("ucl-draw-"+stage));}
    static Set<String> uclCandidates(Context c,int year)throws IOException{MatchJournal j=journal(c,year);return EuropeanQualification.planned(j)?EuropeanQualification.candidates(j):EuropaSeason.entrants(uclGroups(c,year));}
    static Set<String> europaCandidates(Context c,int year)throws IOException{MatchJournal j=journal(c,year);return EuropeanQualification.planned(j)?new HashSet<String>(EuropeanQualification.ids(j.metadata("uelq-base"))):EuropaSeason.entrants(europaGroups(c,year));}
    static String europaCupEntrant(Context c, int year, String league) throws IOException {
        return journal(c, year).metadata("uel-cup-" + league);
    }
    static MatchJournal domesticCupJournal(Context c, int year) throws IOException {
        return journal(c, year);
    }
    static ArrayList<String[]> qualifyingPairs(Context c,int year)throws IOException{return EuropeanQualification.pairs(journal(c,year));}
    static Map<String, Integer> qualifyingSeeding(Context c, int year) throws IOException {
        MatchJournal draw = journal(c, year);
        if (draw.metadata("europe-seeding-points") == null) return null;
        Map<String, Integer> points = EuropeanQualification.frozenSeedingPoints(draw);
        ArrayList<String> clubs = new ArrayList<>(points.keySet());
        Collections.sort(clubs, EuropeanQualification.seedingOrder(points, EuropeanQualification.frozenStrengths(draw)));
        Map<String, Integer> ranking = new LinkedHashMap<>();
        for (String club : clubs) ranking.put(club, points.get(club));
        return ranking;
    }
    static ArrayList<String> directUcl(Context c,int year)throws IOException{return EuropeanQualification.ids(journal(c,year).metadata("uclq-direct"));}
    static ArrayList<MatchJournal.Match> qualifyingMatches(Context c,int year)throws IOException{return journal(c,year).competition(EuropeanQualification.competition(year));}
    static ArrayList<MatchJournal.Match> europa(Context c,int year)throws IOException{return journal(c,year).competition(EuropaSeason.competition(year));}
    static ArrayList<ArrayList<String>> europaGroups(Context c,int year)throws IOException{return UclSeason.decode(journal(c,year).metadata("uel-groups"));}
    static ArrayList<String[]> europaDraw(Context c,int year,int stage)throws IOException{return UclKnockout.decode(journal(c,year).metadata("uel-draw-"+stage));}
    static String europaQualification(Context c,int year)throws IOException{return journal(c,year).metadata("uel-qualification");}
    static ArrayList<MatchJournal.Match> ucl(Context c,int year)throws IOException{return journal(c,year).competition(UclSeason.competition(year));}
    static ArrayList<String[]> uclDraw(Context c,int year,int stage)throws IOException{return UclKnockout.decode(journal(c,year).metadata("ucl-draw-"+stage));}
    static ArrayList<ArrayList<String>> uclGroups(Context c,int year)throws IOException{return year==2010?NativeCompetitionCalendar.groups(c):UclSeason.decode(journal(c,year).metadata("ucl-groups"));}
    static String uclQualification(Context c,int year)throws IOException{return year==2010?"Historical 2010/11 group participants.":journal(c,year).metadata("ucl-qualification");}
    static ArrayList<UclSeason.Standing> qualifyingTable(Context c,String code)throws Exception{return UclSeason.standings(journal(c,NativeWorldCenter.season()),code,NativeCareerStore.ensure(c).clubs);}

    static MatchJournal.Match match(Context c,String competition,int round,String home,String away,int season)throws IOException{return journal(c,season).get(key(competition,round,home,away));}
    static void showReport(Activity a,MatchJournal.Match m){
        if(m==null)return;String title=m.report.split("\n",2)[0];if(title.startsWith("H\t")){String[] h=title.split("\t");title=h[1]+"  "+m.homeGoals+" – "+m.awayGoals+"  "+h[2];}
        StringBuilder body=new StringBuilder();if(m.source.startsWith("reconstructed-"))body.append("Reconstructed at upgrade; no retrospective fatigue, suspensions or finance applied.\n\n");
        for(int side=0;side<2;side++){String club=side==0?m.home:m.away;body.append(side==0?"HOME\n":"\nAWAY\n");
            for(MatchJournal.PlayerFact p:m.players)if(p.club.equals(club)&&p.stats[2]>0)body.append(p.name).append(p.stats[3]>0?" · XI":" · SUB").append("  G ").append(p.stats[0]).append(" A ").append(p.stats[1]).append(p.minutes>0?"  MIN "+p.minutes+" YC "+p.yellow+" RC "+p.red+(p.injury>0?" INJ "+p.injury+" rounds":""):"").append('\n');}
        if(m.effectsVersion>=2||m.competition.startsWith("uclq:")||m.competition.startsWith("cup:")||m.round>=7&&EuropeanPlaces.european(m.competition))body.append("\nEVENTS\n").append(m.report.substring(m.report.indexOf('\n')>=0?m.report.indexOf('\n')+1:m.report.length()));
        else if(m.report.contains("\nLIVE EVENTS\n"))body.append(m.report.substring(m.report.indexOf("\nLIVE EVENTS\n")));
        if(m.effectsVersion>0){body.append("\nMatch finance (net, gameplay model): home £").append(m.homeCredit).append("; away £").append(m.awayCredit);if(m.effectsVersion==1)body.append("\nCards/injuries were post-match consequences in this older report.");body.append("\nBans are competition-specific.\n");}
        if(m.players.isEmpty())body.append("This historical result has no recorded line-up.");new AlertDialog.Builder(a).setTitle(title).setMessage(body.toString()).setPositiveButton("Close",null).show();
    }
}
