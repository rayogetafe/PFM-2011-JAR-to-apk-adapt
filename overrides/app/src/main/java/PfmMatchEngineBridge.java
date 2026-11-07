import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Vector;
import pfm.android.MatchEngine;
import pfm.android.NativeWorldCenter;

/** Legacy presentation/ledger adapter. All goal decisions belong to MatchEngine. */
public final class PfmMatchEngineBridge {
    private PfmMatchEngineBridge() {}
    private static final HashMap<String,Field> fields=new HashMap<String,Field>();
    private static final Method playerAt=method(cp.class,"a",ci.class,dw.class,Integer.TYPE);
    private static Object live;
    private static Vector[] liveGoals;
    private static Game liveGame;
    private static int lastMinute,pendingMask,pendingMinute;
    private static MatchEngine.Profile liveProfile=MatchEngine.DEFAULT;
    private static boolean reported;
    private static final HashMap<Integer,Game> games=new HashMap<Integer,Game>();
    private static final class Game {int round,seed,season,identity,notifiedHome,notifiedAway;boolean detailed;dw home,away;MatchEngine.Lineup h,a;String[] physicalHome,physicalAway;java.util.HashSet<Integer> goals=new java.util.HashSet<Integer>();}
    private static Game current;
    private static boolean liveAwaiting;
    private static MatchEngine.Lineup lineup(dw club,int key)throws Exception{
        short[] ids=(short[])field(dw.class,"a",short[].class).get(club);int count=Math.min(25,Math.min(ub(club,"e"),ids.length));
        java.util.ArrayList<MatchEngine.Member> xi=new java.util.ArrayList<MatchEngine.Member>(),reserves=new java.util.ArrayList<MatchEngine.Member>();
        for(int i=0;i<count;i++){int id=ids[i]&65535;ci p=player(club,i);if(p==null||id==65535)continue;int[] health=PfmEffectsBridge84.condition(id);if(PfmEffectsBridge84.injury(id,health[1])>0||pfmDiscipline70.suspended(id))continue;
            MatchEngine.Member member=new MatchEngine.Member("legacy:"+id,ub(p,"b"),ub(p,"e"),ub(p,"c"),ub(p,"d"),ub(p,"h"),PfmEffectsBridge84.fatigue(id,health[0]));(i<11?xi:reserves).add(member);}
        MatchEngine.Lineup side=MatchEngine.lineup(key,xi,reserves);tactics(club,side);return side;
    }
    public static MatchEngine.Lineup previewUser(){try{dw user=pfmPlayerHistoryV2.userTeam();if(live!=null&&current!=null&&current.detailed&&current.h.lastMinute<90){if(current.home==user)return current.h;if(current.away==user)return current.a;}
        dw[] clubs=(dw[])field(cp.class,"a",dw[].class).get(null);int[][][] schedule=(int[][][])field(cp.class,"a",int[][][].class).get(null);int round=cp.pfmPlayedForTable();if(round<0||round>=schedule.length)return null;
        for(int[] pair:schedule[round]){dw h=clubs[pair[0]],a=clubs[pair[1]];if(h==user||a==user)return lineup(user,seed(h,a)^(a==user?0x5bd1e995:0));}return null;
    }catch(Exception error){return null;}}
    private static Game game(dw home,dw away,boolean fresh)throws Exception{int key=seed(home,away);Game g=games.get(key);
        if(g==null||fresh){if(current!=null&&(current.round!=currentRound()||current.season!=pfm2.getSeason()||current.identity!=NativeWorldCenter.identity()))games.clear();g=new Game();g.seed=key;g.round=currentRound();g.season=pfm2.getSeason();g.identity=NativeWorldCenter.identity();g.home=home;g.away=away;g.h=lineup(home,key);g.a=lineup(away,key^0x5bd1e995);g.physicalHome=physical(home);g.physicalAway=physical(away);games.put(key,g);}current=g;return g;}
    private static String[] physical(dw club)throws Exception{short[] ids=(short[])field(dw.class,"a",short[].class).get(club);String[] out=new String[Math.min(11,ids.length)];for(int i=0;i<out.length;i++)out[i]="legacy:"+(ids[i]&65535);return out;}
    private static String[] sync(dw club,MatchEngine.Lineup side,String[] previous,int minute)throws Exception{String[] now=physical(club);short[] roster=(short[])field(dw.class,"a",short[].class).get(club);
        for(int i=0;i<now.length;i++)if(!now[i].equals(previous[i])){boolean reorder=false;for(String uid:previous)if(uid.equals(now[i]))reorder=true;if(reorder)continue;
            boolean accepted=side.manual(previous[i],now[i],minute);MatchEngine.Member old=side.find(previous[i]);if(!accepted&&old!=null&&old.red==0)accepted=side.manualAt(i,now[i],minute);
            if(!accepted){int oldId=Integer.parseInt(previous[i].substring(7));for(int j=11;j<roster.length;j++)if((roster[j]&65535)==oldId){roster[j]=roster[i];roster[i]=(short)oldId;break;}now[i]=previous[i];}}
        return now;}
    public static MatchEngine.Lineup[] lineups(dw home,dw away,int round){for(Game g:games.values())if(g.home==home&&g.away==away&&g.round==round-1&&g.season==pfm2.getSeason()&&g.h.lastMinute==90)return new MatchEngine.Lineup[]{g.h,g.a};return new MatchEngine.Lineup[0];}
    public static MatchEngine.Lineup[] currentLineups(){return current==null?new MatchEngine.Lineup[0]:new MatchEngine.Lineup[]{current.h,current.a};}
    public static MatchEngine.Lineup completedLineup(dw club,int round){for(Game g:games.values())if(g.round==round-1&&g.season==pfm2.getSeason()&&g.identity==NativeWorldCenter.identity()&&g.h.lastMinute==90&&g.a.lastMinute==90){if(g.home==club)return g.h;if(g.away==club)return g.a;}return null;}
    public static java.util.Map<Integer,MatchEngine.Member> completedParticipation(){java.util.HashMap<Integer,MatchEngine.Member> out=new java.util.HashMap<Integer,MatchEngine.Member>();try{for(Game g:games.values())if(g.season==pfm2.getSeason()&&g.identity==NativeWorldCenter.identity()&&g.h.lastMinute==90&&g.a.lastMinute==90)for(dw club:new dw[]{g.home,g.away}){MatchEngine.Lineup side=club==g.home?g.h:g.a;short[] ids=(short[])field(dw.class,"a",short[].class).get(club);for(int i=0;i<Math.min(ub(club,"e"),ids.length);i++){int id=ids[i]&65535;out.put(id,side.find("legacy:"+id));}}return out;}catch(Exception error){throw new IllegalStateException(error);}}
    public static int completedRating(int id){try{Class<?> archive=Class.forName("pfmLeagueMatchArchive34");for(Game g:games.values())if(g.h.lastMinute==90&&g.a.lastMinute==90){String report=(String)archive.getMethod("report",Integer.TYPE,dw.class,dw.class).invoke(null,g.round+1,g.home,g.away);for(String line:report.split("\n")){String[] p=line.split("\t",-1);if(p.length>10&&p[0].equals("P")&&Integer.parseInt(p[10])==id)return Integer.parseInt(p[7]);}}}catch(Exception ignored){}return -1;}
    /** The text event log records executed substitutions, including goalkeeper and substitute-for-substitute changes. */
    public static void appendSubstitutions(StringBuffer out,dw club,int zeroRound){MatchEngine.Lineup side=completedLineup(club,zeroRound+1);boolean any=false;if(side!=null)for(MatchEngine.Incident e:side.incidents)if(e.type.equals("SUB")){out.append(e.minute).append("' ").append(name(club,e.uid)).append(" -> ").append(name(club,e.other)).append('\n');any=true;}if(!any)out.append("NONE\n");}
    public static MatchEngine.Member played(int id){for(MatchEngine.Lineup side:completedLineups()){MatchEngine.Member p=side.find("legacy:"+id);if(p!=null)return p;}return null;}
    public static String cardMinutes(int id,String type){StringBuilder out=new StringBuilder();for(MatchEngine.Lineup side:completedLineups())for(MatchEngine.Incident e:side.incidents)if(e.uid.equals("legacy:"+id)&&(type.equals("YC")?e.type.equals("YC"):e.type.equals("RC")||e.type.equals("2YC-RC"))){if(out.length()>0)out.append(',');out.append(e.minute);}return out.toString();}
    public static MatchEngine.Lineup[] completedLineups(){java.util.ArrayList<MatchEngine.Lineup> out=new java.util.ArrayList<MatchEngine.Lineup>();int newest=-1;for(Game g:games.values())newest=Math.max(newest,g.round);for(Game g:games.values())if(g.round==newest&&g.h.lastMinute==90){out.add(g.h);out.add(g.a);}return out.toArray(new MatchEngine.Lineup[0]);}
    public static String incidents(dw club){for(Game g:games.values())if(g.h.lastMinute==90&&(g.home==club||g.away==club)){MatchEngine.Lineup side=g.home==club?g.h:g.a;StringBuilder b=new StringBuilder();for(MatchEngine.Incident e:side.incidents)b.append(e.minute).append("' ").append(e.type).append(" · ").append(e.uid).append(e.other.length()==0?"":" → "+e.other).append('\n');return b.toString();}return "";}
    /** Scorer selection is restricted to the actual on-field state, including minute-of-exit. */
    public static ci scorer(dw club,int minute){try{Game g=current;if(g==null||(g.home!=club&&g.away!=club))throw new IllegalStateException("Scorer without active match");MatchEngine.Lineup side=club==g.home?g.h:g.a;String uid=side.scorer(minute).uid;int id=Integer.parseInt(uid.substring(7));short[] ids=(short[])field(dw.class,"a",short[].class).get(club);for(int i=0;i<ids.length;i++)if((ids[i]&65535)==id){ci p=player(club,i);if(g.goals.add(minute*2+(club==g.home?0:1))){side.find(uid).goals++;(club==g.home?g.a:g.h).concede();}if(liveAwaiting){liveAwaiting=false;finish(g,minute);}return p;}throw new IllegalStateException("Active scorer missing from roster");}catch(Exception e){throw new IllegalStateException(e);}}
    private static void tactics(dw club,MatchEngine.Lineup side)throws Exception{
        side.style=ub(club,"c");side.formation=ub(club,"d");side.tacticalBias=0;
        if(club==pfmPlayerHistoryV2.userTeam()){byte[] settings=(byte[])field(cp.class,"a",byte[].class).get(null);int[] b=(int[])field(bb.class,"b",int[].class).get(null),c=(int[])field(bb.class,"c",int[].class).get(null),d=(int[])field(bb.class,"d",int[].class).get(null);side.tacticalBias=(150-b[settings[0]]-c[settings[3]]-d[settings[4]])*0.001;}
    }
    private static int advance(Game g,int minute,int hg,int ag,MatchEngine.Profile profile)throws Exception{g.physicalHome=sync(g.home,g.h,g.physicalHome,minute);g.physicalAway=sync(g.away,g.a,g.physicalAway,minute);tactics(g.home,g.h);tactics(g.away,g.a);g.h.begin(minute);g.a.begin(minute);return MatchEngine.events(g.seed,minute,g.h.team(),g.a.team(),profile,hg,ag);}
    private static String name(dw club,String uid){try{int id=Integer.parseInt(uid.substring(7));short[] ids=(short[])field(dw.class,"a",short[].class).get(club);for(int i=0;i<ids.length;i++)if((ids[i]&65535)==id)return (String)Class.forName("pfmPlayerName60").getMethod("name",ci.class).invoke(null,player(club,i));}catch(Exception ignored){}return uid;}
    private static void notify(Game g,MatchEngine.Lineup side,dw club,int from){if(!g.detailed)return;for(int i=from;i<side.incidents.size();i++){MatchEngine.Incident e=side.incidents.get(i);PfmIncidentsBridge88.notice(e.minute+"' "+e.type+" · "+name(club,e.uid)+(e.other.length()>0?" → "+name(club,e.other):""));}}
    private static void finish(Game g,int minute){g.h.end(minute);g.a.end(minute);notify(g,g.h,g.home,g.notifiedHome);notify(g,g.a,g.away,g.notifiedAway);g.notifiedHome=g.h.incidents.size();g.notifiedAway=g.a.incidents.size();}

    private static Method method(Class<?> c,String name,Class<?> result,Class<?>... args){
        for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getReturnType()==result
            &&java.util.Arrays.equals(m.getParameterTypes(),args)){m.setAccessible(true);return m;}
        throw new IllegalStateException("Missing method "+c.getName()+"."+name);
    }
    private static Field field(Class<?> c,String name,Class<?> type)throws Exception{
        String key=c.getName()+":"+name+":"+type.getName();Field result=fields.get(key);
        if(result==null){for(Class<?> owner=c;owner!=null;owner=owner.getSuperclass())for(Field f:owner.getDeclaredFields())
            if(f.getName().equals(name)&&f.getType()==type){f.setAccessible(true);fields.put(key,f);return f;}
            throw new NoSuchFieldException(key);}
        return result;
    }
    private static int ub(Object o,String name)throws Exception{return field(o.getClass(),name,Byte.TYPE).getByte(o)&255;}
    private static ci player(dw t,int i)throws Exception{return (ci)playerAt.invoke(null,t,i);}
    public static int currentRound(){return pfmRoundAccess60.round();}
    private static int seed(dw home,dw away)throws Exception{
        return MatchEngine.mix(NativeWorldCenter.identity()^pfm2.getSeason()*10007
            ^currentRound()*104729^ub(home,"n")*1009^ub(away,"n")*65537);
    }
    private static MatchEngine.Profile profile(){try{return MatchEngine.profile(String.valueOf(Class.forName("PfmSeasonBridge").getMethod("leagueCode").invoke(null)));}catch(Exception error){throw new IllegalStateException("Competition scoring profile unavailable",error);}}
    /** The scorer ledger receives the exact event minute in every match mode. */
    public static int prepareEventAndRound(int minute){
        try{field(pfmSubstitutions.class,"currentEventMinute",Integer.TYPE).setInt(null,minute);}
        catch(Exception e){throw new IllegalStateException(e);}
        return currentRound();
    }
    /** Celebration time does not consume football minutes or discard scheduled events. */
    public static int clockTick(Object screen){
        try{Field m=field(ca.class,"m",Integer.TYPE);int tick=m.getInt(screen);
            if(field(ca.class,"t",Integer.TYPE).getInt(screen)==0)m.setInt(screen,++tick);
            return tick;
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void append(dw team,dw home,dw away,Vector goals,int minute)throws Exception{
        pfmSubstitutions.appendLiveMinute(new StringBuffer(),minute);
        dw user=pfmPlayerHistoryV2.userTeam();
        ci scorer=(home==user||away==user)?pfmMatchEvents.liveScorer(team,minute):pfmSubstitutions.pfmScorer(team,minute);
        if(scorer==null)throw new IllegalStateException("No eligible scorer at "+minute);
        goals.addElement(scorer);goals.addElement(Integer.toString(minute));
    }
    private static boolean isLiveMatch(dw home,dw away,Vector[] goals){
        return liveGame!=null&&liveGoals==goals&&liveGame.home==home&&liveGame.away==away
            &&liveGame.round==currentRound()&&liveGame.season==pfm2.getSeason()
            &&liveGame.identity==NativeWorldCenter.identity();
    }
    /** Called for each half, or for the remaining half when the user selects Skip. */
    public static void fast(dw home,dw away,Vector[] goals,int start){
        try{
            int end=start<45?45:90,from=start+1;
            boolean resume=isLiveMatch(home,away,goals);
            boolean fresh=start==0&&(current==null||current.h.lastMinute>0||current==liveGame);
            Game game=resume?liveGame:game(home,away,fresh);
            current=game;
            game.detailed=false;
            MatchEngine.Profile profile=resume?liveProfile:profile();
            if(resume){
                if(pendingMask!=0){for(int side=0;side<2;side++)if((pendingMask&(1<<side))!=0)
                    append(side==0?home:away,home,away,goals[side],pendingMinute);if(game.h.lastMinute<pendingMinute)finish(game,pendingMinute);pendingMask=0;}
                // The celebration can show minute zero after the first goal has already been booked.
                from=lastMinute+1;
            }
            for(int minute=from;minute<=end;minute++){
                int events=advance(game,minute,goals[0].size()/2,goals[1].size()/2,profile);
                for(int side=0;side<2;side++)if((events&(1<<side))!=0)
                    append(side==0?home:away,home,away,goals[side],minute);
                finish(game,minute);
                if(resume)lastMinute=minute;
            }
        }catch(Exception e){throw new IllegalStateException("Unified fast match failed",e);}
    }
    /** Called by the live renderer before its existing scorer, scoreboard and kickoff code. */
    public static boolean liveGoal(Object screen){
        try{
            int phase=ub(screen,"c");if(phase!=1&&phase!=3)return false;
            int minute=Math.min(90,field(ca.class,"n",Integer.TYPE).getInt(screen)+1);
            dw home=(dw)field(ca.class,"a",dw.class).get(screen),away=(dw)field(ca.class,"b",dw.class).get(screen);
            Vector[] goals=(Vector[])field(ca.class,"a",Vector[].class).get(screen);
            if(live!=screen||!isLiveMatch(home,away,goals)){
                liveGame=game(home,away,true);live=screen;liveGoals=goals;liveProfile=profile();
                lastMinute=0;pendingMask=0;liveAwaiting=false;
            }
            if(field(ca.class,"t",Integer.TYPE).getInt(screen)>0)return false;
            current=liveGame;
            current.detailed=true;
            if(pendingMask==0&&minute>lastMinute){
                for(int m=lastMinute+1;m<=minute;m++){
                    // Scorers must still be active in this minute; retire incidents at the next minute boundary.
                    if(current.h.lastMinute<m-1)finish(current,m-1);
                    pendingMask=advance(current,m,goals[0].size()/2,goals[1].size()/2,liveProfile);
                    if(pendingMask==0)finish(current,m);
                    lastMinute=m;if(pendingMask!=0){pendingMinute=m;break;}
                }
            }
            if(pendingMask==0)return false;
            int side=(pendingMask&1)!=0?0:1;
            af scene=(af)field(ca.class,"a",af.class).get(screen);
            bt attacking=(bt)field(ca.class,side==0?"a":"b",bt.class).get(screen);
            Field actor=field(af.class,"b",bm.class);
            if(actor.get(scene)==null){actor.set(scene,method(bt.class,"a",bm.class).invoke(attacking));if(actor.get(scene)==null)return false;}
            Object ball=field(aw.class,"a",cb.class).get(scene);
            bt h=(bt)field(ca.class,"a",bt.class).get(screen);
            int direction=field(bt.class,"m",Integer.TYPE).getInt(h);
            field(cb.class,"p",Integer.TYPE).setInt(ball,side==0?-direction*1000:direction*1000);
            field(cb.class,"q",Integer.TYPE).setInt(ball,0);
            field(ca.class,"n",Integer.TYPE).setInt(screen,pendingMinute);
            pendingMask&=~(1<<side);
            liveAwaiting=true;
            return true;
        }catch(Exception e){if(!reported){reported=true;android.util.Log.e("PFM-MatchEngine","Live adapter failed",e);}return false;}
    }
}
