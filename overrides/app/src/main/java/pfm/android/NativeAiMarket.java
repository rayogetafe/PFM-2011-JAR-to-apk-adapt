package pfm.android;

import android.content.Context;
import java.util.*;

/** Deterministic, once-per-window AI-to-AI transfer batch. */
final class NativeAiMarket {
    private NativeAiMarket() {}
    private static int count(List<NativeCareerStore.Player> ps,int pos){int n=0;for(NativeCareerStore.Player p:ps)if(p.position==pos)n++;return n;}
    private static long payroll(List<NativeCareerStore.Player> ps){long n=0;for(NativeCareerStore.Player p:ps)n+=p.contractWage;return n;}
    private static int weakest(List<NativeCareerStore.Player> ps,int pos){int n=0,sum=0,min=100;for(NativeCareerStore.Player p:ps)if(p.position==pos){n++;sum+=p.overall;min=Math.min(min,p.overall);}return n==0?40:Math.min(sum/n,min+5);}

    static synchronized int run(Context context)throws Exception{
        NativeCareerStore.Career c=NativeCareerStore.mutable(context);
        if(!NativeCareerStore.aiReady(context))return 0;
        int window=NativeTransferCalendar.windowId(c.season);
        if(window<0)return 0;
        int legacyWindow=c.season*2+NativeTransferCalendar.phase();
        if(c.aiWindow==window||c.aiWindow==legacyWindow)return 0;
        NativeCareerStore.Club user=NativeCareerStore.userClub(c);
        final int seed=window^NativeWorldCenter.identity();
        HashMap<String,NativeCareerStore.Club> clubs=new HashMap<String,NativeCareerStore.Club>();
        HashMap<String,ArrayList<NativeCareerStore.Player>> squads=new HashMap<String,ArrayList<NativeCareerStore.Player>>();
        for(NativeCareerStore.Club club:c.clubs){clubs.put(club.id,club);squads.put(club.id,new ArrayList<NativeCareerStore.Player>());}
        for(NativeCareerStore.Player p:c.players)squads.get(p.owner).add(p);
        ArrayList<NativeCareerStore.Club> buyers=new ArrayList<NativeCareerStore.Club>();
        for(NativeCareerStore.Club club:c.clubs)if((user==null||!user.id.equals(club.id))&&!club.league.equals(c.activeLeague))buyers.add(club);
        Collections.sort(buyers,(a,b)->Integer.compare((a.id.hashCode()^seed)&0x7fffffff,(b.id.hashCode()^seed)&0x7fffffff));
        int moved=0;
        for(NativeCareerStore.Club buyer:buyers){
            if(moved>=8)break;
            ArrayList<NativeCareerStore.Player> buyerSquad=squads.get(buyer.id);if(buyerSquad.size()>=30)continue;
            int need=0,needScore=999;
            for(int pos=0;pos<4;pos++){int score=weakest(buyerSquad,pos)+(count(buyerSquad,pos)==0?-30:0);if(score<needScore){needScore=score;need=pos;}}
            NativeCareerStore.Player best=null;NativeCareerStore.Club seller=null;int bestScore=Integer.MIN_VALUE,bestFee=0;
            for(NativeCareerStore.Player p:c.players){
                if(p.position!=need||p.owner.equals(buyer.id)||(user!=null&&p.owner.equals(user.id)))continue;
                NativeCareerStore.Club owner=clubs.get(p.owner);ArrayList<NativeCareerStore.Player> sellerSquad=squads.get(p.owner);
                if(owner==null||owner.league.equals(c.activeLeague)||owner.league.equals(buyer.league)||sellerSquad.size()<=18)continue;
                int min=need==0?2:need==1?5:need==2?5:4;if(count(sellerSquad,need)<=min)continue;
                int stronger=0;for(NativeCareerStore.Player q:sellerSquad)if(q.position==need&&q.overall>p.overall)stronger++;
                if(stronger<2||p.overall<needScore+2)continue;
                int pct=100+(((p.id+":"+buyer.id+":"+window).hashCode()&0x7fffffff)%16);
                int fee=NativeGlobalMarket.quoted((int)Math.max(100000L,(long)p.value*pct/100L));
                if(fee>buyer.balance||payroll(buyerSquad)+p.contractWage>buyer.wageBudget)continue;
                int score=(p.overall-needScore)*1000-fee/100000+((p.id+buyer.id).hashCode()&255);
                if(score>bestScore){best=p;seller=owner;bestScore=score;bestFee=fee;}
            }
            if(best==null)continue;
            buyer.balance-=bestFee;seller.balance=(int)Math.min(Integer.MAX_VALUE,(long)seller.balance+bestFee);
            String from=best.owner;squads.get(from).remove(best);buyerSquad.add(best);best.owner=buyer.id;best.contractUntil=c.season+3;
            NativeCareerStore.Deal d=new NativeCareerStore.Deal();d.player=best.id;d.from=from;d.to=buyer.id;d.fee=bestFee;d.wage=best.contractWage;d.years=3;d.season=c.season;d.ai=true;c.deals.add(d);moved++;
        }
        c.aiWindow=window;
        String audit=NativeCareerStore.commitAi(context,c);if(!"OK".equals(audit))throw new IllegalStateException(audit);
        return moved;
    }
}
