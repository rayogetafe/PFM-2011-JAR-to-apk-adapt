#!/usr/bin/env python3
"""Exercise real journal/capture/projection code and real pure WorldCenter methods on JVM."""
import os, re, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'overrides/app/src/main/java/pfm/android'
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'));(ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v83-',dir=ROOT/'artifacts') as temp:
        temp=Path(temp);world=(JAVA/'NativeWorldCenter.java').read_text(encoding='utf-8')
        methods=[]
        for name in ('hash','schedule','pick','selectScore','select','selection','stats'):
            match=re.search(r' static [\w<>\[\], ]+ \b'+name+r'\(',world);assert match,name
            start=match.start();at=world.index('{',start);depth=1;end=at+1
            while depth:
                if world[end]=='{':depth+=1
                elif world[end]=='}':depth-=1
                end+=1
            methods.append(world[start:end])
        types=re.search(r' static class P\{.*? static class M\{.*? static HashMap',world,re.S).group().removesuffix(' static HashMap')
        fixture='package pfm.android;\nimport java.util.*;import android.content.Context;\npublic class NativeWorldCenter {\n'+types+'''
static final String[] C={"en","es","it","de","fr"};
static HashMap<String,ArrayList<T>> leagues=new HashMap<>();static int nonce,careerNonce,year=2010,played;
static void load(Context c){careerNonce=nonce;}static int identity(){return nonce;}static int season(){return year;}
'''+''.join(methods)+'\n}'
        stub=temp/'NativeWorldCenter.java';stub.write_text(fixture,encoding='utf-8')
        sources=[*[p for p in (ROOT/'tools/tests/v83').rglob('*.java') if not p.name.startswith('Verify')],stub,*[JAVA/name for name in ('MatchJournal.java','MatchEffects.java','NativeMatchJournal.java','NativeCompetitionCalendar.java','NativeMatchEngine.java','MatchEngine.java')]]
        subprocess.run([javac,'-encoding','UTF-8','-d',str(temp),*map(str,sources)],check=True)
        data=temp/'data';data.mkdir()
        subprocess.run([java,'-Xverify:all','-cp',str(temp),'pfm.android.JournalHarness',sys.argv[1],str(data)],cwd=ROOT,check=True)
        if len(sys.argv)>2:
            bridge=temp/'bridge';bridge.mkdir();stubs=[]
            for name,body in {
                'PfmSeasonBridge':'public static String leagueCode(){return "en";}',
                'pfmMatchPost35':'public static int completedRoundOne(){return 1;}',
                'pfmLeagueMatchArchive34':'public static String report(int round,dw home,dw away){return "";}'
            }.items():
                path=bridge/(name+'.java');path.write_text('public class '+name+'{'+body+'}',encoding='utf-8');stubs.append(path)
            subprocess.run([javac,'-encoding','UTF-8','-cp',os.pathsep.join([sys.argv[2],str(temp)]),'-d',str(bridge),*map(str,stubs),str(ROOT/'tools/tests/v81/android/util/Log.java'),str(JAVA.parent.parent/'PfmJournalBridge83.java'),str(JAVA.parent.parent/'PfmEffectsBridge84.java')],check=True)
if __name__=='__main__':main()
