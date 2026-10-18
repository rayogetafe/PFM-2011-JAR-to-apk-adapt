#!/usr/bin/env python3
"""Production legacy overlay and extracted production finance transaction, including crash boundaries."""
import os,re,shutil,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'overrides/app/src/main/java'

def method(source,name):
    start=source.index('    static synchronized void '+name);at=source.index('{',start);end=at+1;depth=1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]

def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'));(ROOT/'artifacts').mkdir(exist_ok=True)
    career=(JAVA/'pfm/android/NativeCareerStore.java').read_text(encoding='utf-8')
    for receipt in ('root.put("matchReceipts",receipts)','root.put("matchBudgetPending",c.matchBudgetPending)','root.optJSONArray("matchReceipts")','root.optBoolean("matchBudgetPending",false)'):
        assert receipt in career,receipt
    with tempfile.TemporaryDirectory(prefix='pfm-v84-',dir=ROOT/'artifacts') as tmp:
        temp=Path(tmp);files={
        'NativeMatchJournal.java':'''package pfm.android;
public class NativeMatchJournal {public static int[] effects=new int[5];public static int[] legacyEffects(int id){return effects.clone();}}''',
        'CoreEffectsHarness.java':'''import pfm.android.NativeMatchJournal;
public class CoreEffectsHarness {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void main(String[] args)throws Exception{
  pfmCondition60.fatigue[0]=20;NativeMatchJournal.effects=new int[]{12,1,3,1,2};
  check(PfmEffectsBridge84.fatigue(0,20)==32&&PfmEffectsBridge84.injury(0,0)==1,"legacy effect view");
  check(PfmEffectsBridge84.starts(0)>=5000&&PfmEffectsBridge84.minutesForRosterIndex(pfmPlayerHistoryV2.user,0,4)==0,"injured player remained eligible");
  check(pfmCondition60.fatigue[0]==20&&pfmCondition60.injury[0]==0,"overlay mutated original ledger");
  NativeMatchJournal.effects=new int[]{12,0,3,1,2};check(PfmEffectsBridge84.minutesForRosterIndex(pfmPlayerHistoryV2.user,0,4)==90,"UCL ban leaked into domestic eligibility");
  check(PfmEffectsBridge84.starts(0)>4&&PfmEffectsBridge84.starts(0)<5000,"extra fatigue not considered in rotation");
  check(PfmEffectsBridge84.fatigue(0,95)==100&&PfmEffectsBridge84.injury(0,3)==3,"condition bounds");
  PfmEffectsBridge84.setBudget(123);check(pfmPlayerHistoryV2.user.g==123,"absolute budget publication");
  System.out.println("Legacy overlay: injury exclusion, extra fatigue, domestic/UCL ban isolation, no domestic ledger writes verified");
 }
}
class dw {int g=1000000;}
class pfmCondition60 {static byte[] fatigue={0},injury={0};private static void ensure(){}}
class pfmAvailable70 {public static int starts(int id){return 4;}public static int minutesForRosterIndex(dw t,int i,int r){return 90;}public static int benchImpact(dw t){return 0;}}
class pfmRosterId60 {public static int idAt(dw t,int i){return i;}}
class pfmPlayerHistoryV2 {static dw user=new dw();public static dw userTeam(){return user;}}
''',
        'FinanceStore.java':'''import java.io.*;import java.util.*;import pfm.android.MatchJournal;
class FinanceStore {
 static class Club implements Serializable {String id;int balance;Club(String id,int balance){this.id=id;this.balance=balance;}}
 static class Career implements Serializable {ArrayList<Club> clubs=new ArrayList<>();HashSet<String> matchReceipts=new HashSet<>();boolean matchBudgetPending;}
 static File file;static int writes,failWrite;
 static Career mutable(Context c)throws Exception{try(ObjectInputStream in=new ObjectInputStream(new FileInputStream(file))){return (Career)in.readObject();}}
 static Club user(Career c){return c.clubs.get(0);}
 static int legacyBudget(){return pfmPlayerHistoryV2.user.g;}
 static void writeAtomic(Context c,Career state)throws Exception{if(++writes==failWrite)throw new IOException("injected transaction interruption");File tmp=new File(file+".tmp");try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(tmp))){out.writeObject(state);}java.nio.file.Files.move(tmp.toPath(),file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
'''+method(career,'applyMatchFinance')+'\n}\nclass Context {}',
        'FinanceHarness.java':'''import java.io.*;import java.util.*;import pfm.android.MatchJournal;
public class FinanceHarness {
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static MatchJournal.Match fixture(String id,int amount){MatchJournal.Match m=new MatchJournal.Match();m.id=id;m.effectsVersion=1;m.home="user";m.away="other";m.homeCredit=amount;return m;}
 public static void main(String[] args)throws Exception{
  FinanceStore.file=new File(args[0]);Context context=new Context();FinanceStore.Career initial=new FinanceStore.Career();initial.clubs.add(new FinanceStore.Club("user",1000000));initial.clubs.add(new FinanceStore.Club("other",1000000));FinanceStore.writeAtomic(context,initial);
  ArrayList<MatchJournal.Match> matches=new ArrayList<>();matches.add(fixture("ucl:1",450000));FinanceStore.applyMatchFinance(context,matches,2010);
  check(pfmPlayerHistoryV2.user.g==1450000&&FinanceStore.mutable(context).matchReceipts.size()==1,"first credit");
  int writes=FinanceStore.writes;FinanceStore.applyMatchFinance(context,matches,2010);check(FinanceStore.writes==writes&&pfmPlayerHistoryV2.user.g==1450000,"duplicate credit/write");
  pfmPlayerHistoryV2.user.g+=75000;FinanceStore.applyMatchFinance(context,matches,2010);check(pfmPlayerHistoryV2.user.g==1525000,"reopening overwrote domestic income");
  matches.add(fixture("ucl:2",100000));FinanceStore.failWrite=FinanceStore.writes+2;boolean failed=false;try{FinanceStore.applyMatchFinance(context,matches,2010);}catch(Exception expected){failed=true;}
  check(failed&&FinanceStore.mutable(context).matchBudgetPending&&pfmPlayerHistoryV2.user.g==1625000,"crash boundary not reached");
  FinanceStore.failWrite=0;FinanceStore.applyMatchFinance(context,matches,2010);check(!FinanceStore.mutable(context).matchBudgetPending&&pfmPlayerHistoryV2.user.g==1625000,"crash retry double credited");
  MatchJournal.Match old=fixture("old-v83",999999);old.effectsVersion=0;matches.add(old);FinanceStore.applyMatchFinance(context,matches,2010);check(pfmPlayerHistoryV2.user.g==1625000,"retroactive finance on old result");
  matches.add(fixture("ucl:3",300000));FinanceStore.failWrite=FinanceStore.writes+1;try{FinanceStore.applyMatchFinance(context,matches,2010);}catch(IOException expected){}check(pfmPlayerHistoryV2.user.g==1625000,"failed commit published money");
  FinanceStore.failWrite=0;FinanceStore.applyMatchFinance(context,matches,2010);check(pfmPlayerHistoryV2.user.g==1925000,"saved journal retry lost credit");
  System.out.println("Production finance transaction: receipts + balance atomic, restart retry before/after legacy publication, no double credit, domestic income preserved");
 }
}'''}
        paths=[]
        for name,body in files.items():
            p=temp/name;p.write_text(body,encoding='utf-8');paths.append(p)
        subprocess.run([javac,'-encoding','UTF-8','-d',str(temp),*map(str,paths),str(JAVA/'PfmEffectsBridge84.java'),str(JAVA/'pfm/android/MatchJournal.java')],check=True)
        subprocess.run([java,'-Xverify:all','-cp',str(temp),'CoreEffectsHarness'],check=True)
        subprocess.run([java,'-Xverify:all','-cp',str(temp),'FinanceHarness',str(temp/'finance.bin')],check=True)
if __name__=='__main__':main()
