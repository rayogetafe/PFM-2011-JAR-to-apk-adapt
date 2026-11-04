#!/usr/bin/env python3
import os,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe');java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    with tempfile.TemporaryDirectory(prefix='pfm-v87-math-',dir=ROOT/'artifacts') as temp:
        subprocess.run([javac,'-encoding','UTF-8','-d',temp,str(ROOT/'overrides/app/src/main/java/pfm/android/MatchEngine.java'),str(ROOT/'tools/tests/v87/DistributionTests.java')],check=True)
        subprocess.run([java,'-Xmx512m','-Xverify:all','-cp',temp,'pfm.android.DistributionTests'],check=True)
if __name__=='__main__':main()
