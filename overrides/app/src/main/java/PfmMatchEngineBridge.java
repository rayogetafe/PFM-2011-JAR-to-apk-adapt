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
    private static int liveSeed,lastMinute,pendingMask,pendingMinute;
    private static boolean reported;

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
    private static int seed(dw home,dw away)throws Exception{
        return MatchEngine.mix(NativeWorldCenter.identity()^pfm2.getSeason()*10007
            ^cp.pfmPlayedForTable()*104729^ub(home,"n")*1009^ub(away,"n")*65537);
    }
    private static MatchEngine.Team snapshot(dw club,int minute,boolean detailed)throws Exception{
        MatchEngine.Team out=new MatchEngine.Team();out.style=ub(club,"c");out.formation=ub(club,"d");
        if(club==pfmPlayerHistoryV2.userTeam()){
            byte[] settings=(byte[])field(cp.class,"a",byte[].class).get(null);
            int[] b=(int[])field(bb.class,"b",int[].class).get(null),c=(int[])field(bb.class,"c",int[].class).get(null),d=(int[])field(bb.class,"d",int[].class).get(null);
            out.tacticalBias=(150-b[settings[0]]-c[settings[3]]-d[settings[4]])*0.001;
        }
        int count=ub(club,"e"),round=cp.pfmPlayedForTable();
        short[] ids=(short[])field(dw.class,"a",short[].class).get(club);
        byte[] fatigue=(byte[])field(pfmCondition60.class,"fatigue",byte[].class).get(null);
        for(int i=0;i<Math.min(count,ids.length);i++){
            boolean active=i<11;
            if(!detailed){int minutes=pfmAvailable70.minutesForRosterIndex(club,i,round);
                active=i<11?minutes>0&&minute<=minutes:minutes>0&&minute>90-minutes;}
            if(!active)continue;ci p=player(club,i);if(p==null)continue;
            int id=ids[i]&65535,fat=fatigue!=null&&id<fatigue.length?fatigue[id]&255:0;
            out.add(ub(p,"b"),ub(p,"e"),ub(p,"c"),ub(p,"d"),ub(p,"h"),fat);
        }
        return out;
    }
    /** The scorer ledger receives the exact event minute in every match mode. */
    public static int prepareEventAndRound(int minute){
        try{field(pfmSubstitutions.class,"currentEventMinute",Integer.TYPE).setInt(null,minute);}
        catch(Exception e){throw new IllegalStateException(e);}
        return cp.pfmPlayedForTable();
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
    /** Called for each half, or for the remaining half when the user selects Skip. */
    public static void fast(dw home,dw away,Vector[] goals,int start){
        try{
            int key=seed(home,away),end=start<45?45:90,from=start+1;
            if(live!=null&&key==liveSeed){
                if(pendingMask!=0){for(int side=0;side<2;side++)if((pendingMask&(1<<side))!=0)
                    append(side==0?home:away,home,away,goals[side],pendingMinute);pendingMask=0;}
                from=Math.max(from,lastMinute+1);
            }
            for(int minute=from;minute<=end;minute++){
                MatchEngine.Team h=snapshot(home,minute,false),a=snapshot(away,minute,false);
                for(int side=0;side<2;side++)if(MatchEngine.goal(key,minute,side,h,a))
                    append(side==0?home:away,home,away,goals[side],minute);
                if(live!=null&&key==liveSeed)lastMinute=minute;
            }
        }catch(Exception e){throw new IllegalStateException("Unified fast match failed",e);}
    }
    /** Called by the live renderer before its existing scorer, scoreboard and kickoff code. */
    public static boolean liveGoal(Object screen){
        try{
            int phase=ub(screen,"c");if(phase!=1&&phase!=3)return false;
            int minute=Math.min(90,field(ca.class,"n",Integer.TYPE).getInt(screen)+1);
            dw home=(dw)field(ca.class,"a",dw.class).get(screen),away=(dw)field(ca.class,"b",dw.class).get(screen);
            if(live!=screen||(minute<=1&&lastMinute>1)){live=screen;liveSeed=seed(home,away);lastMinute=0;pendingMask=0;}
            if(field(ca.class,"t",Integer.TYPE).getInt(screen)>0)return false;
            if(pendingMask==0&&minute>lastMinute){
                MatchEngine.Team h=snapshot(home,minute,true),a=snapshot(away,minute,true);
                for(int m=lastMinute+1;m<=minute;m++){
                    for(int side=0;side<2;side++)if(MatchEngine.goal(liveSeed,m,side,h,a))pendingMask|=1<<side;
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
            return true;
        }catch(Exception e){if(!reported){reported=true;android.util.Log.e("PFM-MatchEngine","Live adapter failed",e);}return false;}
    }
}
