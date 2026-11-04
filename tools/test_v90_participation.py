#!/usr/bin/env python3
import os,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe');java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    with tempfile.TemporaryDirectory(prefix='pfm-v90-',dir=ROOT/'artifacts') as tmp:
        sources=[ROOT/'overrides/app/src/main/java/pfm/android'/name for name in ('MatchEngine.java','MatchJournal.java','MatchParticipation.java')]+[ROOT/'tools/tests/v90/ParticipationHarness.java']
        subprocess.run([javac,'-encoding','UTF-8','-d',tmp,*map(str,sources)],check=True);subprocess.run([java,'-Xverify:all','-cp',tmp,'ParticipationHarness'],check=True)
if __name__=='__main__':main()
