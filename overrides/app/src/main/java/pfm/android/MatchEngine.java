package pfm.android;

/** The sole goal model. Independent of Android and presentation; competition supplies immutable calibration. */
public final class MatchEngine {
    private MatchEngine() {}
    /** Immutable competition calibration, never an alternative simulation algorithm. */
    public static final class Profile {public final String code;public final double historicalRate,historicalDrawRate;final double homeScale,awayScale,zeroCycle,oneCycle,twoCycle;private Profile(String code,double home,double away){this(code,home,away,0,1,0,0,0,0);}private Profile(String code,double home,double away,int goals,int games,int draws,double zero,double one,double two){this.code=code;homeScale=home;awayScale=away;historicalRate=goals/(double)games;historicalDrawRate=draws/(double)games;zeroCycle=zero;oneCycle=one;twoCycle=two;}}
    public static final Profile DEFAULT=new Profile("neutral",1,1);
    // 2010/11 home/away totals; fitted to expected event intensity, not to individual RNG outcomes.
    // Source totals and calibration/held-out verification live in tools/reference and test_v86_scoring.py.
    private static final Profile EN=new Profile("en",1.076150,0.978114,1063,380,111,0.090918,0.397663,0.314263),ES=new Profile("es",1.039024,0.880435,1042,380,79,0.105164,-0.524888,0.551434),IT=new Profile("it",0.898447,0.852025,955,380,97,0.125208,-0.316774,0.704416),DE=new Profile("de",1.137937,1.108136,894,306,63,-0.126113,0.170077,-0.950000),FR=new Profile("fr",0.831068,0.779695,890,380,130,0.243903,0.409416,-0.519981);
    public static Profile profile(String competition){String code=competition==null?"":competition;if(code.startsWith("league:"))code=code.substring(7);if(code.equals("en"))return EN;if(code.equals("es"))return ES;if(code.equals("it"))return IT;if(code.equals("de"))return DE;if(code.equals("fr"))return FR;return DEFAULT;}
    public static final class Team {
        private double attack, defence, midfield, keeper;
        private double aw, dw, mw, kw;
        private int count;
        public int style=55, formation=2;
        public double tacticalBias;
        private Correction correction;
        public void add(int position,int quality,int speed,int resistance,int morale,int fatigue){
            double rating=(quality*0.55+speed*0.25+resistance*0.20)
                +(clamp(morale,0,100)-55)*0.035-clamp(fatigue,0,100)*0.035;
            rating=clamp(rating,1,99);
            double a=position==3?1.0:position==2?0.55:position==1?0.12:0;
            double d=position==1?1.0:position==2?0.45:position==3?0.10:0;
            double m=position==2?1.0:position==1?0.25:position==3?0.25:0;
            attack+=a*rating;aw+=a;defence+=d*rating;dw+=d;midfield+=m*rating;mw+=m;
            if(position==0){keeper+=rating;kw++;}count++;
        }
        private double average(double sum,double weight){return weight>0?sum/weight:35;}
        private double attack(){return average(attack,aw);}
        private double defence(){return average(defence,dw)*0.78+(kw>0?keeper/kw:25)*0.22;}
        private double midfield(){return average(midfield,mw);}
        private double intent(){
            int[] attackShare={55,65,50,55,70,60,50,65,60};
            return clamp((55-style)*0.004+(attackShare[Math.max(0,Math.min(8,formation))]-50)*0.004+tacticalBias,-0.25,0.25);
        }
    }
    public static double expectedGoals(Team own,Team opponent,boolean home){
        return expectedGoals(own,opponent,home,DEFAULT);
    }
    public static double expectedGoals(Team own,Team opponent,boolean home,Profile profile){
        double difference=(own.attack()-opponent.defence())*0.030
            +(own.midfield()-opponent.midfield())*0.010;
        double intent=own.intent()+opponent.intent()*0.65;
        double manpower=Math.log(Math.max(1,own.count)/11.0)*1.4
            -Math.log(Math.max(1,opponent.count)/11.0)*0.65;
        return clamp((home?1.40*profile.homeScale:1.12*profile.awayScale)*Math.exp(difference+intent+manpower),0.08,5.5);
    }
    /** One categorical goal trial per minute; mode never changes RNG. */
    public static boolean goal(int seed,int minute,int side,Team home,Team away){
        return goal(seed,minute,side,home,away,DEFAULT);
    }
    public static boolean goal(int seed,int minute,int side,Team home,Team away,Profile profile){
        if(profile.zeroCycle!=0||profile.oneCycle!=0||profile.twoCycle!=0)throw new IllegalArgumentException("score prefix required for correlated profile");
        return goal(seed,minute,side,home,away,profile,0,0);
    }
    public static boolean goal(int seed,int minute,int side,Team home,Team away,Profile profile,int homeGoals,int awayGoals){
        return (events(seed,minute,home,away,profile,homeGoals,awayGoals)&(1<<side))!=0;
    }
    /** Pure score-aware transition. The caller supplies the already committed prefix; no hidden RNG/session state. */
    public static int events(int seed,int minute,Team home,Team away,Profile profile,int homeGoals,int awayGoals){
        if(minute<1||minute>90)throw new IllegalArgumentException("minute outside 1..90");
        if(homeGoals<0||awayGoals<0)throw new IllegalArgumentException("negative score prefix");
        double h=expectedGoals(home,away,true,profile)/90.0,a=expectedGoals(away,home,false,profile)/90.0;
        if((profile.zeroCycle!=0||profile.oneCycle!=0||profile.twoCycle!=0)&&homeGoals<=3&&awayGoals<=3){
            Correction c=home.correction;if(c==null||c.h!=h||c.a!=a||c.profile!=profile){c=new Correction(h,a,profile);home.correction=c;}
            int remaining=90-minute;double q=1-h-a,power=Math.pow(q,remaining);double[] falling=new double[7];falling[0]=1;for(int i=1;i<7;i++)falling[i]=falling[i-1]*(remaining-i+1);
            double no=q*c.future(homeGoals,awayGoals,remaining,power,falling),yesH=h*c.future(homeGoals+1,awayGoals,remaining,power,falling),yesA=a*c.future(homeGoals,awayGoals+1,remaining,power,falling),total=no+yesH+yesA;
            if(!(total>0)||Double.isInfinite(total))throw new IllegalStateException("Invalid goal transition mass");
            h=yesH/total;a=yesA/total;
        }
        double roll=(mix(seed^minute*0x9e3779b9)&0xffffffffL)/4294967296.0;
        return roll<h?1:roll<h+a?2:0;
    }
    /** Sparse 2x2 probability cycles preserve both goal-count marginals for fixed team intensities. */
    private static final class Correction {
        final double h,a,rx,ry;final Profile profile;final double[] adjustment=new double[10];
        static final int[] X={0,0,1,1,1,2,2,2,3,3},Y={0,1,0,1,2,1,2,3,2,3};
        Correction(double h,double a,Profile p){this.h=h;this.a=a;profile=p;double q=1-h-a;rx=h/q;ry=a/q;double[] probabilities=new double[10];for(int i=0;i<10;i++)probabilities[i]=base(h,a,X[i],Y[i]);
            double zero=p.zeroCycle*(p.zeroCycle>=0?Math.min(probabilities[1],probabilities[2]):Math.min(probabilities[0],probabilities[3]));
            double one=p.oneCycle*(p.oneCycle>=0?Math.min(probabilities[4],probabilities[5]):Math.min(probabilities[3]+zero,probabilities[6]));
            double two=p.twoCycle*(p.twoCycle>=0?Math.min(probabilities[7],probabilities[8]):Math.min(probabilities[6]+one,probabilities[9]));
            double[] delta={zero,-zero,-zero,zero+one,-one,-one,one+two,-two,-two,two};for(int i=0;i<10;i++)adjustment[i]=delta[i]/probabilities[i];
        }
        double future(int hg,int ag,int remaining,double power,double[] falling){if(hg>3||ag>3)return 1;double value=1;for(int i=0;i<10;i++){int x=X[i]-hg,y=Y[i]-ag,k=x+y;if(x<0||y<0||k>remaining)continue;double probability=power*falling[k]*(x==0?1:x==1?rx:x==2?rx*rx:rx*rx*rx)*(y==0?1:y==1?ry:y==2?ry*ry:ry*ry*ry)/((x==3?6:x==2?2:1)*(y==3?6:y==2?2:1));value+=adjustment[i]*probability;}return Math.max(1e-12,value);}
    }
    private static double base(double h,double a,int x,int y){double q=1-h-a,value=Math.pow(q,90),rx=h/q,ry=a/q;for(int k=0;k<x+y;k++)value*=90-k;for(int i=1;i<=x;i++)value*=rx/i;for(int i=1;i<=y;i++)value*=ry/i;return value;}
    /** Exact fixed-intensity endpoint probabilities, for calibration and conservation tests only. */
    public static double[] distribution(double homeGoals,double awayGoals,Profile profile){return distribution(homeGoals,awayGoals,profile.zeroCycle,profile.oneCycle,profile.twoCycle);}
    public static double[] distribution(double homeGoals,double awayGoals,double zeroCycle,double oneCycle,double twoCycle){
        if(!(homeGoals>0)||!(awayGoals>0)||homeGoals+awayGoals>=90)throw new IllegalArgumentException("invalid goal intensities");
        if(Math.abs(zeroCycle)>0.95||Math.abs(oneCycle)>0.95||Math.abs(twoCycle)>0.95)throw new IllegalArgumentException("cycle coefficient outside safe bounds");
        Profile p=new Profile("audit",1,1,0,1,0,zeroCycle,oneCycle,twoCycle);double h=homeGoals/90.0,a=awayGoals/90.0;Correction c=new Correction(h,a,p);double[] result=new double[11];
        for(int i=0;i<10;i++)result[i]=base(h,a,Correction.X[i],Correction.Y[i])*(1+c.adjustment[i]);
        double diagonal=base(h,a,0,0),draw=diagonal,q=1-h-a;for(int k=1;k<=45;k++){diagonal*=(92-2*k)*(91-2*k)*(h*a/(q*q))/(k*(double)k);draw+=diagonal;}
        result[10]=draw+base(h,a,0,0)*c.adjustment[0]+base(h,a,1,1)*c.adjustment[3]+base(h,a,2,2)*c.adjustment[6]+base(h,a,3,3)*c.adjustment[9];return result;
    }
    public static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;return x^(x>>>16);}
    /** Match-local participants. Identity and minute, never presentation mode, key incident RNG. */
    public static final class Member {
        public final String uid;public final int position,quality,speed,resistance,morale,fatigue;
        public int minutes,yellow,red,injury,entryMinute,exitMinute,goals,assists,conceded;public boolean starter;
        public Member(String uid,int pos,int qua,int spe,int res,int mor,int fat){this.uid=uid;position=pos;quality=qua;speed=spe;resistance=res;morale=mor;fatigue=fat;}
    }
    public static final class Incident {
        public final int minute;public final String type,uid,other;
        Incident(int minute,String type,String uid,String other){this.minute=minute;this.type=type;this.uid=uid;this.other=other;}
    }
    /** The same balanced seven-player bench and automatic policy are used by every adapter. */
    public static Lineup lineup(int seed,java.util.List<Member> starters,java.util.List<Member> reserves){
        Lineup out=new Lineup(seed);for(Member p:starters)out.add(p,true);
        java.util.ArrayList<Member> ranked=new java.util.ArrayList<Member>(reserves);
        java.util.Collections.sort(ranked,new java.util.Comparator<Member>(){public int compare(Member a,Member b){int c=Double.compare(b.quality-b.fatigue*0.2,a.quality-a.fatigue*0.2);return c!=0?c:a.uid.compareTo(b.uid);}});
        int[] want={1,2,2,2};for(int pos=0;pos<4;pos++)for(Member p:ranked)if(want[pos]>0&&p.position==pos&&out.find(p.uid)==null){out.add(p,false);want[pos]--;}
        for(Member p:ranked)if(out.squad.size()<starters.size()+7&&out.find(p.uid)==null)out.add(p,false);
        out.autoPlan();return out;
    }
    /** Shared on-field state for native, quick, live and Skip. Events occur after that minute's play. */
    public static final class Lineup {
        public final java.util.ArrayList<Member> squad=new java.util.ArrayList<Member>();
        public final java.util.ArrayList<Incident> incidents=new java.util.ArrayList<Incident>();
        private final Member[] slots=new Member[11];private final java.util.HashSet<String> entered=new java.util.HashSet<String>();
        private final java.util.TreeMap<Integer,java.util.ArrayList<String>> planned=new java.util.TreeMap<Integer,java.util.ArrayList<String>>();
        private final java.util.HashMap<Integer,String> forced=new java.util.HashMap<Integer,String>();
        public int substitutions,lastMinute;private final int seed;public int style=55,formation=2;public double tacticalBias;
        public Lineup(int seed){this.seed=seed;}
        public void add(Member p,boolean starter){if(find(p.uid)!=null)throw new IllegalArgumentException("Duplicate match participant");squad.add(p);if(starter){int slot=entered.size();if(slot>=11)throw new IllegalArgumentException("More than XI");slots[slot]=p;entered.add(p.uid);p.starter=true;p.entryMinute=1;}}
        public Member find(String uid){for(Member p:squad)if(p.uid.equals(uid))return p;return null;}
        public void plan(int minute,String uid){if(minute<1||minute>90||find(uid)==null)throw new IllegalArgumentException("Invalid substitution plan");java.util.ArrayList<String> at=planned.get(minute);if(at==null){at=new java.util.ArrayList<String>();planned.put(minute,at);}if(!at.contains(uid))at.add(uid);}
        private void autoPlan(){int draw=(mix(seed^0x73a72f91)&0x7fffffff)%1000,target=draw<5?0:draw<45?1:draw<325?2:3;java.util.ArrayList<Member> options=new java.util.ArrayList<Member>();for(Member p:squad)if(!p.starter&&p.position>0)options.add(p);
            java.util.Collections.sort(options,new java.util.Comparator<Member>(){public int compare(Member a,Member b){int x=mix(seed^a.uid.hashCode()^137)&0x7fffffff,y=mix(seed^b.uid.hashCode()^137)&0x7fffffff;return x!=y?Integer.compare(x,y):a.uid.compareTo(b.uid);}});
            for(int i=0;i<target&&i<options.size();i++){int minute=55+i*10+(mix(seed^i*719^0x2b91)&0x7fffffff)%9;plan(minute,options.get(i).uid);}
        }
        public java.util.ArrayList<Incident> planPreview(){java.util.ArrayList<Incident> out=new java.util.ArrayList<Incident>();for(java.util.Map.Entry<Integer,java.util.ArrayList<String>> entry:planned.entrySet())for(String uid:entry.getValue())out.add(new Incident(entry.getKey(),"PLAN","",uid));return out;}
        private boolean active(Member p){for(Member q:slots)if(q==p)return true;return false;}
        public boolean active(String uid){Member p=find(uid);return p!=null&&active(p)&&p.red==0&&p.injury==0;}
        private Member reserve(int position){for(Member p:squad)if(p.position==position&&!entered.contains(p.uid)&&p.red==0&&p.injury==0)return p;return null;}
        private int tacticalSlot(Member in,int minute){int best=-1;double value=-Double.MAX_VALUE;for(int i=0;i<slots.length;i++){Member p=slots[i];if(p==null||p.position!=in.position||p.red>0||p.injury>0||(!p.starter&&p.entryMinute==minute))continue;double score=p.minutes+p.fatigue*0.5+(100-p.resistance)*0.1-p.quality*0.05;if(score>value){value=score;best=i;}}return best;}
        private void substitute(int slot,Member in,int minute){Member out=slots[slot];if(in==null||substitutions>=3||entered.contains(in.uid)||out==null||out.red>0)return;if(out.exitMinute==0)out.exitMinute=minute-1;in.entryMinute=minute;slots[slot]=in;entered.add(in.uid);substitutions++;incidents.add(new Incident(minute,"SUB",out.uid,in.uid));}
        /** A renderer/user substitution is accepted once and cannot replace a sent-off player. */
        public boolean manual(String out,String in,int minute){Member p=find(out),q=find(in);if(p==null||q==null)return false;for(int i=0;i<11;i++)if(slots[i]==p){int old=substitutions;substitute(i,q,minute);return substitutions>old;}return false;}
        public boolean manualAt(int slot,String in,int minute){if(slot<0||slot>=11)return false;int old=substitutions;substitute(slot,find(in),minute);return substitutions>old;}
        public void begin(int minute){if(minute!=lastMinute+1)throw new IllegalStateException("Non-sequential football minute");
            for(int i=0;i<11;i++){Member p=slots[i];if(p!=null&&p.injury>0&&p.red==0){Member q=reserve(p.position);if(q==null&&p.position!=0){for(Member r:squad)if(r.position>0&&!entered.contains(r.uid)){q=r;break;}}substitute(i,q,minute);}}
            java.util.ArrayList<String> at=planned.get(minute);if(at!=null)for(String uid:at){Member in=find(uid);if(in!=null&&!entered.contains(in.uid)){int slot=tacticalSlot(in,minute);if(slot>=0)substitute(slot,in,minute);}}
        }
        public Team team(){Team t=new Team();t.style=style;t.formation=formation;t.tacticalBias=tacticalBias;for(Member p:slots)if(p!=null&&p.red==0&&p.injury==0)t.add(p.position,p.quality,p.speed,p.resistance,p.morale,p.fatigue);return t;}
        private double random(Member p,int minute,int salt){return (mix(seed^p.uid.hashCode()^minute*0x632be5ab^salt)&0xffffffffL)/4294967296.0;}
        /** Explicit scripted events are used only by focused tests; production uses the same transition below. */
        public void scripted(int minute,String uid,String type){if(!type.equals("YC")&&!type.equals("RC")&&!type.equals("INJ"))throw new IllegalArgumentException("Invalid incident");forced.put(minute,uid+"\t"+type);}
        public void end(int minute){if(minute!=lastMinute+1)throw new IllegalStateException("Duplicate incident minute");String script=forced.get(minute);
            for(Member p:slots)if(p!=null&&p.red==0&&p.injury==0){p.minutes++;String type="";
                if(script!=null&&script.startsWith(p.uid+"\t"))type=script.substring(p.uid.length()+1);
                else {double y=p.position==0?0.00034:p.position==1?0.00262:p.position==2?0.00194:0.00117;
                    if(random(p,minute,281)<0.0000334)type="RC";
                    else if(random(p,minute,271)<(p.yellow==0?y:0.00046))type="YC";
                    else if(random(p,minute,283)<(6+Math.max(0,p.fatigue-35)/3)/90000.0)type="INJ";}
                if(type.equals("YC")){p.yellow++;if(p.yellow==2){p.red=1;p.exitMinute=minute;}}
                if(type.equals("RC")){p.red=1;p.exitMinute=minute;}
                if(type.equals("INJ")){p.injury=1+(mix(seed^p.uid.hashCode()^minute^293)&0x7fffffff)%4;p.exitMinute=minute;}
                if(type.length()>0){incidents.add(new Incident(minute,type,p.uid,""));if(type.equals("YC")&&p.red>0)incidents.add(new Incident(minute,"2YC-RC",p.uid,""));}
            }lastMinute=minute;
        }
        public Member scorer(int minute){int total=0;for(Member p:slots)if(p!=null&&p.red==0&&p.injury==0)total+=weight(p);if(total==0)throw new IllegalStateException("No eligible scorer");int draw=(mix(seed^minute*313^0x51ed270b)&0x7fffffff)%total;for(Member p:slots)if(p!=null&&p.red==0&&p.injury==0){draw-=weight(p);if(draw<0)return p;}throw new AssertionError();}
        private int weight(Member p){return Math.max(1,(p.position==3?12:p.position==2?5:p.position==1?2:0)*Math.max(1,p.quality));}
        public void goal(int minute){Member scorer=scorer(minute);scorer.goals++;if((mix(seed^minute*719)&0x7fffffff)%100<72){for(int i=0;i<slots.length;i++){Member p=slots[(i+minute)%slots.length];if(p!=null&&p!=scorer&&active(p.uid)&&p.position>0){p.assists++;break;}}}}
        public void concede(){for(Member p:slots)if(p!=null&&p.position==0&&active(p.uid)){p.conceded++;break;}}
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
