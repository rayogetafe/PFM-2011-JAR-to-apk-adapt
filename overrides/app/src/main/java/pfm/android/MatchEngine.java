package pfm.android;

/** The sole goal model. Independent of Android and presentation; competition supplies immutable calibration. */
public final class MatchEngine {
    private MatchEngine() {}
    /** Immutable competition calibration, never an alternative simulation algorithm. */
    public static final class Profile {public final String code;public final double historicalRate;final double homeScale,awayScale;private Profile(String code,double home,double away){this(code,home,away,0,1);}private Profile(String code,double home,double away,int goals,int games){this.code=code;homeScale=home;awayScale=away;historicalRate=goals/(double)games;}}
    public static final Profile DEFAULT=new Profile("neutral",1,1);
    // 2010/11 home/away totals; fitted to expected event intensity, not to individual RNG outcomes.
    // Source totals and calibration/held-out verification live in tools/reference and test_v86_scoring.py.
    private static final Profile EN=new Profile("en",1.076150,0.978114,1063,380),ES=new Profile("es",1.039024,0.880435,1042,380),IT=new Profile("it",0.898447,0.852025,955,380),DE=new Profile("de",1.137937,1.108136,894,306),FR=new Profile("fr",0.831068,0.779695,890,380);
    public static Profile profile(String competition){String code=competition==null?"":competition;if(code.startsWith("league:"))code=code.substring(7);if(code.equals("en"))return EN;if(code.equals("es"))return ES;if(code.equals("it"))return IT;if(code.equals("de"))return DE;if(code.equals("fr"))return FR;return DEFAULT;}
    public static final class Team {
        private double attack, defence, midfield, keeper;
        private double aw, dw, mw, kw;
        private int count;
        public int style=55, formation=2;
        public double tacticalBias;
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
        if(minute<1||minute>90)throw new IllegalArgumentException("minute outside 1..90");
        double h=expectedGoals(home,away,true,profile)/90.0,a=expectedGoals(away,home,false,profile)/90.0;
        double roll=(mix(seed^minute*0x9e3779b9)&0xffffffffL)/4294967296.0;
        return side==0?roll<h:roll>=h&&roll<h+a;
    }
    public static int mix(int x){x^=x>>>16;x*=0x7feb352d;x^=x>>>15;x*=0x846ca68b;return x^(x>>>16);}
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
