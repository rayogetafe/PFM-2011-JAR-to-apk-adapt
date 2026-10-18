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
    public static String key(String competition,int round,String home,String away){return competition+"|"+round+"|"+home+"|"+away;}
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
    private static void facts(MatchJournal.Match m,String club,NativeWorldCenter.T team,int gf,int ga,int seed,NativeWorldCenter.S selection){
        HashMap<String,int[]> before=new HashMap<String,int[]>();for(NativeWorldCenter.P p:team.ps)before.put(p.uid,stats(p));
        NativeWorldCenter.stats(team,gf,ga,seed,selection);
        for(NativeWorldCenter.P p:team.ps){MatchJournal.PlayerFact f=new MatchJournal.PlayerFact();f.uid=p.uid;f.club=club;f.name=selection.used.contains(p)?p.name:"";int[] old=before.get(p.uid);f.stats=stats(p);for(int i=0;i<8;i++)f.stats[i]-=old[i];m.players.add(f);}
    }
    private static NativeWorldCenter.S eligible(NativeWorldCenter.T team,String club,String competition,int seed,MatchEffects effects,HashMap<String,NativeWorldCenter.P> players){
        NativeWorldCenter.T fit=new NativeWorldCenter.T();
        for(NativeWorldCenter.P p:team.ps){int[] base=legacyCondition.get(p.uid);p.fat=Math.min(100,effects.fatigue(p.uid)+(base==null?0:base[0]));if(effects.available(p.uid,competition)&&(base==null||base[1]==0))fit.ps.add(p);}
        int keepers=0;for(NativeWorldCenter.P p:fit.ps)if(p.pos==0)keepers++;
        // Explicit temporary academy cover; never silently select injured players or rewrite career rosters.
        int cover=0,outfield=fit.ps.size()-keepers;while(outfield<10||keepers==0){NativeWorldCenter.P p=new NativeWorldCenter.P();p.pos=keepers==0?0:1+cover%3;p.uid="cover:"+NativeWorldCenter.season()+":"+club+":"+seed+":"+cover++;p.name=p.pos==0?"Academy goalkeeper":"Academy cover";p.nat="Academy";p.ovr=p.spe=p.res=p.qua=45;p.mor=55;p.id=p.uid.hashCode();fit.ps.add(p);team.ps.add(p);players.put(p.uid,p);if(p.pos==0)keepers++;else outfield++;}
        return NativeWorldCenter.selection(fit,seed);
    }
    static MatchJournal.Match capture(MatchJournal j,String competition,int round,String homeId,String awayId,int seed,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career){
        String id=key(competition,round,homeId,awayId);MatchJournal.Match old=j.get(id);if(old!=null){effects.apply(old);return old;}
        NativeWorldCenter.T home=teams.get(homeId),away=teams.get(awayId);if(home==null||away==null||home.ps.size()<11||away.ps.size()<11)throw new IllegalStateException("Fixture squad unavailable: "+id);
        NativeWorldCenter.S hs=eligible(home,homeId,competition,seed,effects,players),as=eligible(away,awayId,competition,seed+17,effects,players);
        int[] result=NativeMatchEngine.score(seed,home,away,hs,as);MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.competition=competition;m.round=round;m.home=homeId;m.away=awayId;m.homeGoals=result[0];m.awayGoals=result[1];m.seed=seed;m.source=source;
        m.report=home.name+"  "+m.homeGoals+" – "+m.awayGoals+"  "+away.name;
        facts(m,homeId,home,m.homeGoals,m.awayGoals,seed,hs);facts(m,awayId,away,m.awayGoals,m.homeGoals,seed+17,as);
        if(source.equals("native-v84")){m.clock=effects.clock;minutes(m,homeId,hs);minutes(m,awayId,as);MatchEffects.stamp(m,players);
            long hw=0,aw=0;for(NativeCareerStore.Player p:career.players){if(p.owner.equals(homeId))hw+=p.contractWage;if(p.owner.equals(awayId))aw+=p.contractWage;}
            m.homeCredit=MatchEffects.credit(competition.startsWith("ucl:"),true,m.homeGoals,m.awayGoals,home.ovr,hw,competition.equals("league:de")?34:38);m.awayCredit=MatchEffects.credit(competition.startsWith("ucl:"),false,m.awayGoals,m.homeGoals,away.ovr,aw,competition.equals("league:de")?34:38);}
        if(m.effectsVersion>0)for(MatchJournal.PlayerFact f:m.players){int[] base=legacyCondition.get(f.uid);f.stats[8]=Math.min(100,effects.fatigue(f.uid)+f.fatigueDelta+(base==null?0:base[0]));}
        j.put(m);effects.apply(m);return m;
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
    public static synchronized void advance(Context c,int completed)throws Exception{
        context=c.getApplicationContext();NativeWorldCenter.load(c);int season=NativeWorldCenter.season(),total=NativeTransferCalendar.total();completed=Math.min(total,Math.max(0,completed));
        MatchJournal j=journal(c,season);if(lastSeason==season&&completed<=lastRound)return;
        if(completed==0&&season==2010){lastSeason=season;lastRound=0;return;}
        boolean baseline=j.size()==0&&completed>1;String source=baseline?"reconstructed-v83":"native-v84";
        NativeCareerStore.Career career=NativeCareerStore.ensure(c);HashMap<String,NativeWorldCenter.T> teams=teams(career);HashMap<String,NativeWorldCenter.P> players=new HashMap<String,NativeWorldCenter.P>();for(NativeWorldCenter.T team:teams.values())for(NativeWorldCenter.P p:team.ps)players.put(p.uid,p);
        ArrayList<ArrayList<String>> groups=NativeCompetitionCalendar.groups(c);MatchJournal ucl=season==2010?j:journal(c,2010);importUcl(c,ucl,groups,teams);
        importLegacy(c,j,completed,career);
        MatchEffects effects=new MatchEffects();
        for(int step=1;step<=completed;step++){
            effects.rest(step);
            for(String code:NativeWorldCenter.C)if(!code.equals(career.activeLeague)){
                ArrayList<NativeWorldCenter.T> clubs=NativeWorldCenter.leagues.get(code);if(clubs==null)continue;
                int previous=NativeCompetitionCalendar.parallelRound(step-1,total,code),now=NativeCompetitionCalendar.parallelRound(step,total,code);
                for(NativeWorldCenter.M fixture:NativeWorldCenter.schedule(clubs.size()))if(fixture.r>previous&&fixture.r<=now){int seed=identity+season*10007+code.hashCode()*101+fixture.r*37+fixture.h*11+fixture.a;capture(j,"league:"+code,fixture.r,code+":"+fixture.h,code+":"+fixture.a,seed,teams,players,source,effects,career);}
            }
            if(season==2010){int previous=NativeCompetitionCalendar.uclCompleted(step-1,total),now=NativeCompetitionCalendar.uclCompleted(step,total);
                for(int r=previous;r<now;r++)for(int g=0;g<8;g++)for(int match=0;match<2;match++){int[] pairs=NativeCompetitionCalendar.UCL_PAIRS[r];capture(j,"ucl:2010",r+1,groups.get(g).get(pairs[match*2]),groups.get(g).get(pairs[match*2+1]),identity+r*104729+g*1009+match*37,teams,players,source,effects,career);}
            }
        }
        j.save();if(ucl!=j)ucl.save();currentEffects=effects;effectsSeason=season;effectsIdentity=identity;NativeCareerStore.applyMatchFinance(c,j.all(),season);lastSeason=season;lastRound=completed;
    }
    /** Non-mutating legacy overlay. Domestic suspensions/cards stay in the domestic ledger. */
    public static int[] legacyEffects(int id){
        if(Boolean.TRUE.equals(restoring.get()))return new int[5];
        if(effectsIdentity!=NativeWorldCenter.careerNonce||effectsSeason!=NativeWorldCenter.season()||currentEffects==null){
            restoring.set(Boolean.TRUE);try{Context c=AndroidRuntime.context();NativeWorldCenter.load(c);int season=NativeWorldCenter.season(),nonce=NativeWorldCenter.identity();MatchJournal j=new MatchJournal(new File(c.getFilesDir(),"match-journals"),nonce,season);MatchEffects state=new MatchEffects();
                ArrayList<MatchJournal.Match> records=j.all();Collections.sort(records,(a,b)->a.clock-b.clock);for(MatchJournal.Match m:records)if(m.effectsVersion>0){state.rest(m.clock);state.apply(m);}state.rest(NativeTransferCalendar.played());
                HashMap<Integer,String> ids=new HashMap<Integer,String>();if(!records.isEmpty()){NativeCareerStore.Career career=NativeCareerStore.ensure(c);for(NativeCareerStore.Player p:career.players)if(p.id.startsWith(career.activeLeague+":"))ids.put(p.sourceId,p.id);for(Map.Entry<String,?> e:c.getSharedPreferences("pfm_native_legacy_ids_v58",0).getAll().entrySet())if(e.getValue() instanceof Integer)ids.put((Integer)e.getValue(),e.getKey());}
                if(!records.isEmpty())NativeCareerStore.applyMatchFinance(c,records,season);if(currentEffects==null||effectsIdentity!=nonce||effectsSeason!=season||currentEffects.clock<=state.clock){legacyIds=ids;currentEffects=state;effectsSeason=season;effectsIdentity=nonce;}
            }catch(Exception e){throw new IllegalStateException("Saved match effects unavailable",e);}finally{restoring.remove();}
        }
        String uid=legacyIds.get(id);MatchEffects snapshot=currentEffects;if(uid==null||snapshot==null)return new int[5];MatchEffects.Discipline d=snapshot.readDiscipline("ucl:2010",uid);return new int[]{snapshot.fatigue(uid),snapshot.injury(uid),d.yellow,d.red,d.ban};
    }
    static String conditionText(String uid){if(currentEffects==null)return "";MatchEffects.Discipline d=currentEffects.discipline("ucl:2010",uid);String league="";for(Map.Entry<String,ArrayList<NativeWorldCenter.T>> entry:NativeWorldCenter.leagues.entrySet())for(NativeWorldCenter.T team:entry.getValue())for(NativeWorldCenter.P p:team.ps)if(uid.equals(p.uid))league=entry.getKey();MatchEffects.Discipline domestic=currentEffects.discipline("league:"+league,uid);return "\nFatigue: "+currentEffects.fatigue(uid)+"/100\nInjury: "+currentEffects.injury(uid)+" league rounds\nLeague: YC "+domestic.yellow+" RC "+domestic.red+"; ban "+domestic.ban+" matches\nUCL: YC "+d.yellow+" RC "+d.red+"; ban "+d.ban+" UCL matches";}
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
    static MatchJournal.Match match(Context c,String competition,int round,String home,String away,int season)throws IOException{return journal(c,season).get(key(competition,round,home,away));}
    static void showReport(Activity a,MatchJournal.Match m){
        if(m==null)return;String title=m.report;if(title.startsWith("H\t")){String[] h=title.split("\n")[0].split("\t");title=h[1]+"  "+m.homeGoals+" – "+m.awayGoals+"  "+h[2];}
        StringBuilder body=new StringBuilder();if(m.source.equals("reconstructed-v83"))body.append("Reconstructed at upgrade; original match details were not saved.\n\n");for(int side=0;side<2;side++){String club=side==0?m.home:m.away;body.append(side==0?"HOME\n":"\nAWAY\n");for(MatchJournal.PlayerFact p:m.players)if(p.club.equals(club)&&p.stats[2]>0)body.append(p.name).append(p.stats[3]>0?" · XI":" · SUB").append("  G ").append(p.stats[0]).append(" A ").append(p.stats[1]).append(m.effectsVersion>0?"  MIN "+p.minutes+" YC "+p.yellow+" RC "+p.red+(p.injury>0?" INJ "+p.injury+" rounds":""):"").append('\n');}
        if(m.effectsVersion>0)body.append("\nMatch finance (net, gameplay model): home £").append(m.homeCredit).append("; away £").append(m.awayCredit).append("\nCards/injuries are post-match consequences; bans are competition-specific.\n");if(m.players.isEmpty())body.append("This historical result has no recorded line-up.");new AlertDialog.Builder(a).setTitle(title).setMessage(body.toString()).setPositiveButton("Close",null).show();
    }
}
