package pfm.android;

import android.content.Context;
import java.util.*;

/** Calendar-paced AI market with league, club and player-repeat safeguards. */
final class NativeAiMarket {
 private NativeAiMarket(){}
 private static int count(List<NativeCareerStore.Player> ps,int pos){int n=0;for(NativeCareerStore.Player p:ps)if(p.position==pos)n++;return n;}
 private static long payroll(List<NativeCareerStore.Player> ps){long n=0;for(NativeCareerStore.Player p:ps)n+=p.contractWage;return n;}
 private static int weakest(List<NativeCareerStore.Player> ps,int pos){int n=0,sum=0,min=100;for(NativeCareerStore.Player p:ps)if(p.position==pos){n++;sum+=p.overall;min=Math.min(min,p.overall);}return n==0?40:Math.min(sum/n,min+5);}
 private static int average(List<NativeCareerStore.Player> ps){int n=0;for(NativeCareerStore.Player p:ps)n+=p.overall;return ps.isEmpty()?60:n/ps.size();}
 private static int windowCap(){try{return ((Number)Class.forName("pfmTransferPolicy31").getMethod("windowCap").invoke(null)).intValue();}catch(Throwable t){return 5;}}
 private static int hash(String s,int seed){return (s.hashCode()^seed)&0x7fffffff;}

 static synchronized int run(Context context)throws Exception{return run(context,false);}
 static synchronized int run(Context context,boolean calendarTick)throws Exception{
  NativeCareerStore.Career c=NativeCareerStore.mutable(context);if(!NativeCareerStore.aiReady(context))return 0;
  int cap=windowCap(),phase=NativeTransferCalendar.phase();if(cap<=0||phase<0)return 0;
  int window=NativeTransferCalendar.windowId(c.season),tick=NativeTransferCalendar.tick(context,calendarTick);if(window<0||tick<0)return 0;
  int token=window*100+tick;if(c.aiWindow==token)return 0;
  int dealSeason=NativeTransferCalendar.effectiveSeason(c.season),already=0;
  for(NativeCareerStore.Deal d:c.deals)if(d.ai&&d.season==dealSeason)already++;
  if(phase==1)already=Math.max(0,already-cap);
  int target=NativeTransferCalendar.target(cap,tick),limit=Math.max(0,target-already);
  NativeCareerStore.Club user=NativeCareerStore.userClub(c);final int seed=window*1009+tick*97+NativeWorldCenter.identity();
  HashMap<String,NativeCareerStore.Club> clubs=new HashMap<String,NativeCareerStore.Club>();HashMap<String,ArrayList<NativeCareerStore.Player>> squads=new HashMap<String,ArrayList<NativeCareerStore.Player>>();
  HashMap<String,Integer> incoming=new HashMap<String,Integer>(),outgoing=new HashMap<String,Integer>(),leagueMoves=new HashMap<String,Integer>();HashSet<String> recent=new HashSet<String>();
  for(NativeCareerStore.Club club:c.clubs){clubs.put(club.id,club);squads.put(club.id,new ArrayList<NativeCareerStore.Player>());}for(NativeCareerStore.Player p:c.players)squads.get(p.owner).add(p);
  for(NativeCareerStore.Deal d:c.deals){if(d.ai&&d.season>=dealSeason-1)recent.add(d.player);if(d.ai&&d.season==dealSeason){incoming.put(d.to,1+(incoming.containsKey(d.to)?incoming.get(d.to):0));outgoing.put(d.from,1+(outgoing.containsKey(d.from)?outgoing.get(d.from):0));NativeCareerStore.Club b=clubs.get(d.to);if(b!=null)leagueMoves.put(b.league,1+(leagueMoves.containsKey(b.league)?leagueMoves.get(b.league):0));}}
  ArrayList<NativeCareerStore.Club> buyers=new ArrayList<NativeCareerStore.Club>();for(NativeCareerStore.Club club:c.clubs)if((user==null||!user.id.equals(club.id))&&!club.league.equals(c.activeLeague))buyers.add(club);
  Collections.sort(buyers,(a,b)->{int la=leagueMoves.containsKey(a.league)?leagueMoves.get(a.league):0,lb=leagueMoves.containsKey(b.league)?leagueMoves.get(b.league):0;if(la!=lb)return la-lb;int ia=incoming.containsKey(a.id)?incoming.get(a.id):0,ib=incoming.containsKey(b.id)?incoming.get(b.id):0;if(ia!=ib)return ia-ib;return Integer.compare(hash(a.id,seed),hash(b.id,seed));});
  int moved=0,firstDeal=c.deals.size(),clubLimit=cap>=80?3:cap>=25?2:1;
  for(int pass=0;pass<3&&moved<limit;pass++)for(NativeCareerStore.Club buyer:buyers){if(moved>=limit)break;int minLeague=Integer.MAX_VALUE;for(String code:NativeWorldCenter.C)if(!code.equals(c.activeLeague))minLeague=Math.min(minLeague,leagueMoves.containsKey(code)?leagueMoves.get(code):0);int buyerLeague=leagueMoves.containsKey(buyer.league)?leagueMoves.get(buyer.league):0;if(buyerLeague>minLeague+1)continue;int bought=incoming.containsKey(buyer.id)?incoming.get(buyer.id):0;if(bought>=clubLimit||bought>pass)continue;ArrayList<NativeCareerStore.Player> buyerSquad=squads.get(buyer.id);if(buyerSquad.size()>=30)continue;
   int need=0,needScore=999;for(int pos=0;pos<4;pos++){int score=weakest(buyerSquad,pos)+(count(buyerSquad,pos)==0?-30:0);if(score<needScore){needScore=score;need=pos;}}
   int buyerLevel=average(buyerSquad);NativeCareerStore.Player best=null;NativeCareerStore.Club seller=null;int bestScore=Integer.MIN_VALUE,bestFee=0,bestWage=0,bestYears=0;
   for(NativeCareerStore.Player p:c.players){if(p.position!=need||p.owner.equals(buyer.id)||recent.contains(p.id)||(user!=null&&p.owner.equals(user.id)))continue;NativeCareerStore.Club owner=clubs.get(p.owner);ArrayList<NativeCareerStore.Player> sellerSquad=squads.get(p.owner);if(owner==null||owner.league.equals(c.activeLeague)||sellerSquad.size()<=18)continue;
    int sold=outgoing.containsKey(owner.id)?outgoing.get(owner.id):0;if(sold>=clubLimit)continue;int min=need==0?2:need==1?5:need==2?5:4;if(count(sellerSquad,need)<=min)continue;int stronger=0;for(NativeCareerStore.Player q:sellerSquad)if(q.position==need&&q.overall>p.overall)stronger++;
    int improvement=p.overall-needScore;if(stronger<(p.age>=29?1:2)||improvement<(p.age>=30?1:2)||p.overall>buyerLevel+10)continue;int pct=(p.age<=23?110:p.age>=30?85:98)+(hash(p.id+buyer.id,seed)%(p.age<=23?21:16));int fee=NativeGlobalMarket.quoted((int)Math.max(100000L,(long)p.value*pct/100L));int wage=(int)Math.max(50000L,((long)p.contractWage*(100+(hash(p.id+":"+buyer.id,seed)&15))/100L+5000L)/10000L*10000L),years=p.age<=23?4:p.age>=30?2:3;if(fee>buyer.balance||payroll(buyerSquad)+wage>buyer.wageBudget)continue;
    int ideal=Math.min(buyerLevel+5,needScore+5),fit=Math.abs(p.overall-ideal),ageFit=p.age<=24?160:p.age<=29?80:-(p.age-29)*30,score=improvement*450-fit*300+ageFit-fee/250000+(owner.league.equals(buyer.league)?100:0)+(hash(p.id+buyer.id,seed)%900);if(score>bestScore){best=p;seller=owner;bestScore=score;bestFee=fee;bestWage=wage;bestYears=years;}}
   if(best==null)continue;buyer.balance-=bestFee;seller.balance=(int)Math.min(Integer.MAX_VALUE,(long)seller.balance+bestFee);String from=best.owner;squads.get(from).remove(best);buyerSquad.add(best);best.owner=buyer.id;best.contractWage=bestWage;best.contractUntil=dealSeason+bestYears;
   NativeCareerStore.Deal d=new NativeCareerStore.Deal();d.player=best.id;d.from=from;d.to=buyer.id;d.fee=bestFee;d.wage=bestWage;d.years=bestYears;d.season=dealSeason;d.ai=true;c.deals.add(d);recent.add(best.id);incoming.put(buyer.id,bought+1);outgoing.put(seller.id,1+(outgoing.containsKey(seller.id)?outgoing.get(seller.id):0));leagueMoves.put(buyer.league,1+(leagueMoves.containsKey(buyer.league)?leagueMoves.get(buyer.league):0));moved++;}
  c.aiWindow=token;String audit=NativeCareerStore.commitAi(context,c);if(!"OK".equals(audit))throw new IllegalStateException(audit);for(int i=firstDeal;i<c.deals.size();i++)NativeCareerStore.postMail(c,c.deals.get(i));return moved;
 }
}
