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
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
