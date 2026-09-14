/** Wider, deterministic substitution-time distribution for the 2010/11 three-sub era. */
public final class pfmSubTiming38 {
    private pfmSubTiming38() {}
    private static int hash(int x){x^=x>>>16;x*=0x045d9f3b;x^=x>>>16;x*=0x045d9f3b;x^=x>>>16;return x&0x7fffffff;}
    private static int mod(int x,int n){return n<=0?0:hash(x)%n;}
    /** Returns minutes played by the incoming substitute, not the entry minute. */
    public static int subMinutes(int playerId,int round){
        int seed=playerId*421+round*997+pfm2.getSeason()*53+38011;
        int roll=mod(seed,100),minute;
        if(roll<5)minute=15+mod(seed+1,21);          // rare early forced change: 15-35
        else if(roll<15)minute=45+mod(seed+2,8);    // half-time / early second half: 45-52
        else if(roll<78)minute=55+mod(seed+3,24);   // normal tactical window: 55-78
        else minute=79+mod(seed+4,10);              // late cameo: 79-88
        return 90-minute;
    }
}
