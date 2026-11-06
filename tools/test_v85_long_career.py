#!/usr/bin/env python3
"""Multi-season integration audit: real journal, match/effect/finance rules, AI and transfer calendar."""
import os,shutil,subprocess,sys,tempfile
from pathlib import Path
from test_v83_match_journal import world_fixture
ROOT=Path(__file__).resolve().parents[1];JAVA=ROOT/'overrides/app/src/main/java/pfm/android'

def extract(source,signature):
    start=source.index(signature);at=source.index('{',start);end=at+1;depth=1
    while depth:
        depth+=(source[end]=='{')-(source[end]=='}');end+=1
    return source[start:end]

def main(harness='pfm.android.LongCareerHarness',extra_sources=()):
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe');java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    (ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v85-',dir=ROOT/'artifacts') as tmp:
        temp=Path(tmp);sources=[]
        def generated(name,body):
            p=temp/name;p.write_text(body,encoding='utf-8');sources.append(p)
        generated('NativeWorldCenter.java',world_fixture((JAVA/'NativeWorldCenter.java').read_text(encoding='utf-8')))
        fixture=(ROOT/'tools/tests/v83/pfm/android/JournalHarness.java').read_text(encoding='utf-8').split('\nclass NativeCareerStore')[0]
        generated('JournalHarness.java',fixture)
        career=(JAVA/'NativeCareerStore.java').read_text(encoding='utf-8')
        store=(ROOT/'tools/tests/v85/NativeCareerStore.fixture').read_text(encoding='utf-8')
        store=store.replace('/* FINANCE */',extract(career,'    static synchronized void applyMatchFinance(')).replace('/* ADVANCE */',extract(career,'    private static void advance('))
        generated('NativeCareerStore.java',store)
        sources += list((ROOT/'tools/tests/v83/android').rglob('*.java'))+[ROOT/'tools/tests/v83/pfm/android/AndroidRuntime.java',ROOT/'tools/tests/v83/pfm/android/EffectsHarness.java',ROOT/'tools/tests/v83/PfmJournalBridge83.java']
        sources += list((ROOT/'tools/tests/v85').rglob('*.java'))
        sources += list(extra_sources)
        sources += [JAVA/name for name in ('UclSeason.java','UclKnockout.java','MatchJournal.java','MatchParticipation.java','MatchEffects.java','NativeMatchJournal.java','NativeCompetitionCalendar.java','NativeMatchEngine.java','MatchEngine.java','NativeAiMarket.java','NativeTransferMarketModel.java','NativeTransferCalendar.java')]
        subprocess.run([javac,'-encoding','UTF-8','-d',str(temp),*map(str,sources)],check=True)
        output=Path(sys.argv[2]) if len(sys.argv)>2 else ROOT/'artifacts/v85-long-career-baseline.txt'
        years=sys.argv[3] if len(sys.argv)>3 else '5';seeds=sys.argv[4] if len(sys.argv)>4 else '2'
        run=subprocess.Popen([java,'-Xmx768m','-Xverify:all','-cp',str(temp),harness,sys.argv[1],str(temp/'data'),years,seeds],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8')
        lines=[]
        for line in run.stdout:print(line,end='',flush=True);lines.append(line)
        code=run.wait();output.parent.mkdir(parents=True,exist_ok=True);output.write_text(''.join(lines),encoding='utf-8')
        if code:raise subprocess.CalledProcessError(code,run.args)
if __name__=='__main__':main()
