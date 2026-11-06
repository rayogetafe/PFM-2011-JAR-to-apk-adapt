package pfm.android;

import java.util.*;

/** Frozen decisive UCL qualifying round. Earlier external rounds remain an estimated selection. */
final class EuropeanQualification {
    static String competition(int year){return "uclq:"+year;}
    static ArrayList<String> ids(String value){return value==null||value.length()==0?new ArrayList<String>():new ArrayList<String>(Arrays.asList(value.split(";")));}
    static String encode(Collection<String> ids){StringBuilder out=new StringBuilder();for(String id:ids)out.append(id).append(';');return out.toString();}
    static ArrayList<String[]> pairs(MatchJournal j){return UclKnockout.decode(j.metadata("uclq-pairs"));}
    static boolean planned(MatchJournal j){return j.metadata("uclq-pairs")!=null;}
    static Set<String> candidates(MatchJournal j){HashSet<String> ids=new HashSet<String>(ids(j.metadata("uclq-direct")));for(String[] pair:pairs(j)){ids.add(pair[0]);ids.add(pair[1]);}return ids;}
    private static Comparator<String> ranked(final Map<String,Double> strengths){return (a,b)->{int d=Double.compare(UclSeason.value(strengths,b),UclSeason.value(strengths,a));return d!=0?d:a.compareTo(b);};}
    private static ArrayList<UclSeason.Standing> table(MatchJournal previous,NativeCareerStore.Career career,String code,Map<String,Double> strengths){ArrayList<UclSeason.Standing> table=UclSeason.standings(previous,code,career.clubs);boolean full=true;for(UclSeason.Standing row:table)if(row.played!=(code.equals("de")?34:38))full=false;if(!full)Collections.sort(table,(a,b)->ranked(strengths).compare(a.id,b.id));return table;}
    static void prepare(MatchJournal next,MatchJournal previous,NativeCareerStore.Career career,int seed){
        if(planned(next)||next.metadata("ucl-groups")!=null)return;
        HashMap<String,Double> strengths=UclSeason.strengths(career);HashMap<String,String> country=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)country.put(club.id,club.league);
        String holder=UclSeason.champion(previous);ArrayList<String> direct=new ArrayList<String>(),leaguePath=new ArrayList<String>();boolean estimated=false;
        for(String code:new String[]{"en","es","it","de","fr"}){ArrayList<UclSeason.Standing> order=table(previous,career,code,strengths);for(UclSeason.Standing row:order)if(row.played!=(code.equals("de")?34:38))estimated=true;ArrayList<String> eligible=new ArrayList<String>();for(int i=0;i<EuropeanPlaces.champions(code);i++)eligible.add(order.get(i).id);if(code.equals(country.get(holder))){if(!eligible.contains(holder))eligible.set(eligible.size()-1,holder);eligible.remove(holder);eligible.add(0,holder);}for(int i=0;i<eligible.size()-1;i++)direct.add(eligible.get(i));leaguePath.add(eligible.get(eligible.size()-1));}
        ArrayList<String> external=new ArrayList<String>();for(NativeCareerStore.Club club:career.clubs)if(club.league.startsWith("eu_")&&club.activeFrom<=next.season())external.add(club.id);Collections.sort(external,ranked(strengths));
        HashSet<String> directCountries=new HashSet<String>();int externalDirect=0;if(external.contains(holder)){direct.add(holder);directCountries.add(country.get(holder));externalDirect++;}
        for(String id:external)if(externalDirect<9&&directCountries.add(country.get(id))){direct.add(id);externalDirect++;}
        HashSet<String> runners=new HashSet<String>();for(String id:external)if(leaguePath.size()<10&&directCountries.contains(country.get(id))&&!direct.contains(id)&&runners.add(country.get(id)))leaguePath.add(id);
        ArrayList<String> championPath=new ArrayList<String>();HashSet<String> champions=new HashSet<String>();for(String id:external)if(championPath.size()<10&&!directCountries.contains(country.get(id))&&champions.add(country.get(id)))championPath.add(id);
        if(direct.size()!=22||leaguePath.size()!=10||championPath.size()!=10)throw new IllegalStateException("Incomplete qualifying paths");
        ArrayList<String[]> pairs=new ArrayList<String[]>();qualifyingDraw(leaguePath,country,strengths,seed,pairs);qualifyingDraw(championPath,country,strengths,seed^0x7941,pairs);
        HashSet<String> blocked=new HashSet<String>(direct);blocked.addAll(leaguePath);blocked.addAll(championPath);if(blocked.size()!=42)throw new IllegalStateException("Duplicate UCL qualification entrant");
        String elHolder=EuropaSeason.champion(previous);ArrayList<String> base=new ArrayList<String>();for(String code:new String[]{"en","es","it","de","fr"})base.addAll(EuropaSeason.leaguePlaces(code,previous,career,blocked,strengths,elHolder));
        if(external.contains(elHolder)&&!blocked.contains(elHolder))base.add(elHolder);
        HashMap<String,Integer> counts=new HashMap<String,Integer>();for(String id:base){String c=country.get(id);counts.put(c,counts.containsKey(c)?counts.get(c)+1:1);}
        for(int cap=1;base.size()<38&&cap<=4;cap++)for(String id:external){String c=country.get(id);int count=counts.containsKey(c)?counts.get(c):0;if(base.size()<38&&!blocked.contains(id)&&!base.contains(id)&&count<cap){base.add(id);counts.put(c,count+1);}}
        if(base.size()!=38)throw new IllegalStateException("Europa base requires 38 entrants");
        StringBuilder draw=new StringBuilder();for(String[] pair:pairs)draw.append(pair[0]).append(',').append(pair[1]).append(';');next.define("uclq-direct",encode(direct));next.define("uclq-pairs",draw.toString());next.define("uelq-base",encode(base));next.define("uclq-holder",holder);next.define("uelq-holder",elHolder);
        StringBuilder frozenStrength=new StringBuilder();for(String id:new TreeSet<String>(strengths.keySet()))frozenStrength.append(id).append(',').append(strengths.get(id)).append(';');next.define("uclq-strengths",frozenStrength.toString());
        next.define("ucl-qualification",(estimated?"Estimated top-five entries: final tables unavailable. ":"Top-five entries from final tables. ")+"22 direct group places + 10 two-leg playoff winners. External paths estimated; earlier rounds abstracted.");next.define("uel-qualification","38 fixed entrants + 10 UCL playoff losers. Cup and earlier external qualifying rounds abstracted.");
    }
    private static void qualifyingDraw(ArrayList<String> clubs,Map<String,String> country,Map<String,Double> strengths,int seed,ArrayList<String[]> out){Collections.sort(clubs,ranked(strengths));ArrayList<String> seeded=new ArrayList<String>(clubs.subList(0,5)),unseeded=new ArrayList<String>(clubs.subList(5,10));Collections.shuffle(seeded,new Random(seed));Collections.shuffle(unseeded,new Random(seed^0x71af));HashMap<String,Integer> distinct=new HashMap<String,Integer>();for(int i=0;i<clubs.size();i++)distinct.put(clubs.get(i),i);if(!UclKnockout.pair(seeded,unseeded,distinct,country,0,new HashSet<String>(),out))throw new IllegalStateException("No qualifying draw");}
    static void advance(MatchJournal j,int nonce,int step,int total,HashMap<String,NativeWorldCenter.T> teams,HashMap<String,NativeWorldCenter.P> players,String source,MatchEffects effects,NativeCareerStore.Career career){
        if(!planned(j))return;int before=(step-1)*38/total,now=step*38/total;for(int leg=0;leg<2;leg++)if(before<leg+1&&now>=leg+1){int n=0;for(String[] pair:pairs(j)){String home=leg==0?pair[0]:pair[1],away=leg==0?pair[1]:pair[0];MatchJournal.Match first=leg==1?j.get(UclKnockout.key(competition(j.season()),1,away,home)):null;String cutoff=j.metadata("uclq-cutoff");String applied=cutoff!=null&&step<=Integer.parseInt(cutoff)?"reconstructed-v94":source;NativeMatchJournal.capture(j,competition(j.season()),leg+1,home,away,nonce^j.season()*1000003^leg*104729^n++*1009,teams,players,applied,effects,career,first,false);}if(leg==1)finish(j,nonce,career);}
    }
    private static void finish(MatchJournal j,int nonce,NativeCareerStore.Career career){
        if(j.metadata("ucl-groups")!=null&&j.metadata("uel-groups")!=null)return;
        ArrayList<String> qualified=ids(j.metadata("uclq-direct")),losers=new ArrayList<String>();for(String[] pair:pairs(j)){MatchJournal.Match m=j.get(UclKnockout.key(competition(j.season()),2,pair[1],pair[0]));if(m==null||m.winner.length()==0)throw new IllegalStateException("Qualifying tie unresolved");qualified.add(m.winner);losers.add(m.winner.equals(pair[0])?pair[1]:pair[0]);}
        HashMap<String,Double> strengths=new HashMap<String,Double>();for(String entry:j.metadata("uclq-strengths").split(";")){String[] values=entry.split(",");strengths.put(values[0],Double.parseDouble(values[1]));}HashMap<String,String> country=new HashMap<String,String>();for(NativeCareerStore.Club club:career.clubs)country.put(club.id,club.league);
        String holder=j.metadata("uclq-holder");Collections.sort(qualified,(a,b)->{if(a.equals(holder))return b.equals(holder)?0:-1;if(b.equals(holder))return 1;return ranked(strengths).compare(a,b);});ArrayList<ArrayList<String>> groups=new ArrayList<ArrayList<String>>();for(int g=0;g<8;g++)groups.add(new ArrayList<String>());if(!UclSeason.assign(qualified,country,groups,0,new Random(nonce^j.season()*10007)))throw new IllegalStateException("Qualified UCL group draw failed");j.define("ucl-groups",UclSeason.encode(groups));
        ArrayList<String> europa=ids(j.metadata("uelq-base"));europa.addAll(losers);EuropaSeason.freeze(j,europa,career,strengths,j.metadata("uelq-holder"),nonce^j.season()*10007^0x3e17);j.define("uel-from-uclq",encode(losers));
    }
}
