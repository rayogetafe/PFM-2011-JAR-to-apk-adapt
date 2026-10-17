#!/usr/bin/env python3
"""Execute the actual Java goal model and native adapter, including multi-season balance."""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'overrides/app/src/main/java'
def main():
    javac=shutil.which('javac')
    if not javac:
        javac=str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    (ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v81-',dir=ROOT/'artifacts') as out:
        sources=[JAVA/'pfm/android/MatchEngine.java',JAVA/'pfm/android/NativeMatchEngine.java',
            ROOT/'tools/tests/v81/pfm/android/NativeWorldCenter.java',ROOT/'tools/tests/v81/pfm/android/MatchEngineTest.java']
        subprocess.run([javac,'-encoding','UTF-8','-d',out,*map(str,sources)],check=True)
        args=sys.argv[1:]
        if len(args)==1:args += [str(JAVA.parent/'assets/europe_world_v78.tsv'),str(JAVA.parent/'assets/europe_ucl_groups_2010.tsv')]
        subprocess.run([java,'-Xverify:all','-cp',out,'pfm.android.MatchEngineTest',*args],check=True)
if __name__=='__main__':main()
