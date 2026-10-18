package pfm.android;
import java.util.*;
import java.io.*;

/** Deterministic, targeted rule tests, separate from season averages. */
final class EffectsHarness {
    static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    static MatchJournal.Match fixture(String id,String competition,int clock,String uid,int yellow,int red,int injury){
        MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.competition=competition;m.home="a";m.away="b";m.round=clock;m.clock=clock;m.effectsVersion=1;
        MatchJournal.PlayerFact p=new MatchJournal.PlayerFact();p.uid=uid;p.club="a";p.stats[2]=1;p.minutes=90;p.fatigueDelta=12;p.yellow=yellow;p.red=red;p.injury=injury;m.players.add(p);return m;
    }
    static void run(File directory)throws Exception{
        MatchEffects s=new MatchEffects();s.rest(4);MatchJournal.Match first=fixture("one","ucl:2010",4,"uid",1,0,1);s.apply(first);s.apply(first);
        check(s.fatigue("uid")==12&&s.injury("uid")==1,"duplicate fatigue/injury");
        s.rest(5);check(!s.available("uid","ucl:2010"),"one-round injury did not exclude next match");s.rest(6);check(s.available("uid","ucl:2010"),"injury did not recover");check(s.fatigue("uid")==0,"weekly recovery");
        for(int r=1;r<=3;r++)s.apply(fixture("yellow"+r,"ucl:2010",6,"cards",1,0,0));
        check(!s.available("cards","ucl:2010")&&s.available("cards","league:en"),"competition ban leaked into domestic selection");
        MatchJournal.Match domestic=fixture("domestic","league:en",7,"cards",0,0,0);domestic.players.get(0).stats[2]=0;s.apply(domestic);check(s.discipline("ucl:2010","cards").ban==1,"league match served European ban");
        MatchJournal.Match suspended=fixture("serve","ucl:2010",8,"cards",0,0,0);suspended.players.get(0).stats[2]=0;suspended.players.get(0).fatigueDelta=0;s.apply(suspended);check(s.available("cards","ucl:2010"),"European ban not served");s.apply(suspended);check(s.discipline("ucl:2010","cards").ban==0,"duplicate serve altered ban");
        s.apply(fixture("red","ucl:2010",8,"red",0,1,0));check(s.discipline("ucl:2010","red").ban==2,"straight red ban");
        MatchJournal.Match old=fixture("old","ucl:2010",8,"old",2,1,4);old.effectsVersion=0;s.apply(old);check(s.fatigue("old")==0&&s.injury("old")==0&&s.available("old","ucl:2010"),"retroactive effects on old archive");
        MatchJournal j=new MatchJournal(new File(directory,"effect-codec"),84,2010);first.homeCredit=450000;first.awayCredit=0;j.put(first);j.save();first.players.get(0).yellow=99;
        MatchJournal.Match restored=new MatchJournal(new File(directory,"effect-codec"),84,2010).get("one");check(restored.effectsVersion==1&&restored.clock==4&&restored.homeCredit==450000&&restored.players.get(0).yellow==1&&restored.players.get(0).injury==1,"effect codec/deep copy");
        MatchEffects replay=new MatchEffects();replay.rest(restored.clock);replay.apply(restored);check(replay.fatigue("uid")==12&&replay.injury("uid")==1,"restart replay");
        // Genuine old binary layout: version 1 had neither effect fields nor finance metadata.
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream v1=new DataOutputStream(bytes);v1.writeInt(0x50464d53);v1.writeInt(1);v1.writeInt(83);v1.writeInt(2010);v1.writeInt(1);
        for(String text:new String[]{"v1","ucl:2010","a","b","native-v83","Old result"})v1.writeUTF(text);for(int value:new int[]{1,2,1,123,0})v1.writeInt(value);v1.close();byte[] data=bytes.toByteArray();java.util.zip.CRC32 crc=new java.util.zip.CRC32();crc.update(data);
        File oldDirectory=new File(directory,"v1-codec");oldDirectory.mkdirs();try(DataOutputStream out=new DataOutputStream(new FileOutputStream(new File(oldDirectory,"matches-83-2010.bin")))){out.writeInt(data.length);out.write(data);out.writeLong(crc.getValue());}
        MatchJournal migrated=new MatchJournal(oldDirectory,83,2010);check(migrated.get("v1").homeGoals==2&&migrated.get("v1").effectsVersion==0,"v1 journal compatibility");migrated.put(restored);migrated.save();check(new MatchJournal(oldDirectory,83,2010).size()==2,"v1 to v2 save lost old scores");
        int yc=0,rc=0,inj=0;HashMap<String,NativeWorldCenter.P> players=new HashMap<>();NativeWorldCenter.P p=new NativeWorldCenter.P();p.uid="rate";p.pos=1;p.res=75;p.fat=15;players.put(p.uid,p);
        for(int seed=0;seed<100000;seed++){MatchJournal.Match m=fixture("rate","ucl:2010",1,p.uid,0,0,0);m.seed=seed;MatchEffects.stamp(m,players);MatchJournal.PlayerFact f=m.players.get(0);yc+=f.yellow;rc+=f.red;inj+=f.injury>0?1:0;check(f.fatigueDelta==12,"resistance/minutes fatigue");}
        check(yc>19000&&yc<24000&&rc>800&&rc<1400&&inj>400&&inj<900,"post-match event frequency out of intended range");
        HashMap<String,NativeWorldCenter.T> clubs=new HashMap<>();HashMap<String,NativeWorldCenter.P> depleted=new HashMap<>();MatchEffects missing=new MatchEffects();missing.rest(1);
        for(String club:new String[]{"a","b"}){NativeWorldCenter.T team=new NativeWorldCenter.T();team.name=club;team.ovr=70;for(int i=0;i<13;i++){NativeWorldCenter.P member=new NativeWorldCenter.P();member.uid=club+i;member.name=member.uid;member.nat="Test";member.pos=i<3?0:1;member.id=i;member.ovr=member.spe=member.res=member.qua=70;member.mor=55;team.ps.add(member);depleted.put(member.uid,member);if(club.equals("a")&&(i<3||i<6))missing.health(member.uid).injuryUntil=2;}clubs.put(club,team);}
        MatchJournal.Match cover=NativeMatchJournal.capture(new MatchJournal(new File(directory,"cover"),99,2010),"ucl:2010",1,"a","b",99,clubs,depleted,"native-v84",missing,new NativeCareerStore.Career());int homeStarters=0,awayStarters=0,homeKeepers=0;
        for(MatchJournal.PlayerFact member:cover.players){if(member.club.equals("a")){homeStarters+=member.stats[3];if(member.stats[3]>0&&depleted.get(member.uid).pos==0)homeKeepers++;if(member.uid.matches("a[0-5]"))check(member.stats[2]==0,"injured real player used instead of cover");}else awayStarters+=member.stats[3];}
        check(homeStarters==11&&awayStarters==11&&homeKeepers==1,"depleted squad must field XI with explicit emergency goalkeeper");
        System.out.printf(Locale.US,"Effects: duplicate/replay, recovery, injury duration, competition bans and v83 no-retroactivity verified; YC %.1f%% RC %.1f%% injuries %.2f%%%n",yc/1000.0,rc/1000.0,inj/1000.0);
    }
}
