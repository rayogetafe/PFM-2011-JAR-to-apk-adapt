import java.lang.reflect.*;
import pfm.android.MatchEngine;
import pfm.android.MatchJournal;

/** Commit actual playing-time incidents to the existing domestic ledgers exactly once. */
public final class PfmIncidentsBridge88 {
    private PfmIncidentsBridge88(){}
    private static Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void call(Class<?> c,String name)throws Exception{Method m=c.getDeclaredMethod(name);m.setAccessible(true);m.invoke(null);}
    private static int id(MatchEngine.Member p){return Integer.parseInt(p.uid.substring(7));}
    private static byte[] array(Class<?> c,String name)throws Exception{return (byte[])field(c,name).get(null);}
    private static void add(byte[] data,int id,int amount){if(id>=0&&id<data.length)data[id]=(byte)Math.min(255,(data[id]&255)+amount);}
    public static void notice(final String text){try{
        Class<?> runtime=Class.forName("pfm.android.AndroidRuntime");final Object activity=runtime.getMethod("activity").invoke(null);
        activity.getClass().getMethod("runOnUiThread",Runnable.class).invoke(activity,new Runnable(){public void run(){try{Class<?> toast=Class.forName("android.widget.Toast");Object value=toast.getMethod("makeText",Class.forName("android.content.Context"),CharSequence.class,Integer.TYPE).invoke(null,activity,text,0);toast.getMethod("show").invoke(value);}catch(Exception ignored){}}});
    }catch(Exception ignored){/* A headless audit has no presentation surface. Match facts remain authoritative. */}}
    public static void enrich(MatchJournal.Match m,dw home,dw away){
        MatchEngine.Lineup[] sides=PfmMatchEngineBridge.lineups(home,away,m.round);if(sides.length==0)return;
        StringBuilder log=new StringBuilder("\nLIVE EVENTS\n");
        for(int s=0;s<2;s++){MatchEngine.Lineup side=sides[s];String club=s==0?m.home:m.away;
            for(MatchEngine.Member p:side.squad)if(p.minutes>0){MatchJournal.PlayerFact f=null;for(MatchJournal.PlayerFact q:m.players)if(q.club.equals(club)&&q.uid.endsWith(":"+id(p)))f=q;
                if(f==null){f=new MatchJournal.PlayerFact();f.uid="legacy:"+m.competition.substring(7)+":"+id(p);f.club=club;f.name=p.uid;m.players.add(f);}f.minutes=p.minutes;f.yellow=p.yellow;f.red=p.red;f.injury=p.injury;f.stats[2]=1;f.stats[3]=p.starter?1:0;}
            for(MatchEngine.Incident e:side.incidents){String name=e.uid,other=e.other;for(MatchJournal.PlayerFact p:m.players){if(p.club.equals(club)&&p.uid.endsWith(":"+e.uid.substring(7)))name=p.name;if(e.other.length()>0&&p.club.equals(club)&&p.uid.endsWith(":"+e.other.substring(7)))other=p.name;}
                log.append(e.minute).append("' ").append(e.type).append(" · ").append(name);if(other.length()>0)log.append(" → ").append(other);log.append('\n');}}
        m.report+=log; // Domestic effects already live in their original ledgers; do not replay them via the overlay.
    }
    public static void condition(){try{
        Class<?> c=pfmCondition60.class;call(c,"ensure");int round=pfmRoundAccess60.round();Field last=field(c,"lastRound");if(last.getInt(null)==round)return;
        byte[] fatigue=array(c,"fatigue"),injury=array(c,"injury");
        for(int i=0;i<fatigue.length;i++){fatigue[i]=(byte)Math.max(0,(fatigue[i]&255)-7);injury[i]=(byte)Math.max(0,(injury[i]&255)-1);}
        for(MatchEngine.Lineup side:PfmMatchEngineBridge.completedLineups())for(MatchEngine.Member p:side.squad)if(p.minutes>0){int id=id(p);if(id<0||id>=fatigue.length)continue;
            fatigue[id]=(byte)Math.min(100,(fatigue[id]&255)+Math.max(2,(p.minutes*(18-p.resistance/12)+89)/90));if(p.injury>0)injury[id]=(byte)Math.max(injury[id]&255,p.injury);}
        last.setInt(null,round);call(c,"save");
    }catch(Exception e){throw new IllegalStateException("In-match condition commit failed",e);}}
    public static void discipline(){try{
        Class<?> c=pfmDiscipline70.class;call(c,"ensure");int round=pfmRoundAccess60.round()+1;Field last=field(c,"lastRound");if(round<=last.getInt(null))return;
        byte[] yellow=array(c,"yellow"),red=array(c,"red"),ban=array(c,"suspension");for(int i=0;i<ban.length;i++)ban[i]=(byte)Math.max(0,(ban[i]&255)-1);
        for(MatchEngine.Lineup side:PfmMatchEngineBridge.completedLineups())for(MatchEngine.Member p:side.squad)if(p.minutes>0){int id=id(p);if(id<0||id>=ban.length)continue;int old=yellow[id]&255;add(yellow,id,p.yellow);add(red,id,p.red);int suspension=(p.red>0?(p.yellow==2?1:2):0)+(p.yellow<2&&p.yellow>0&&(old+p.yellow)/5>old/5?1:0);add(ban,id,suspension);}
        last.setInt(null,round);call(c,"save");
    }catch(Exception e){throw new IllegalStateException("In-match discipline commit failed",e);}}
}
