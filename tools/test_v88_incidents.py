#!/usr/bin/env python3
"""Focused tests for the production shared playing-time incident state machine."""
import os,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'));(ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v88-',dir=ROOT/'artifacts') as tmp:
        sources=[ROOT/'overrides/app/src/main/java/pfm/android/MatchEngine.java',ROOT/'tools/tests/v88/IncidentHarness.java']
        subprocess.run([javac,'-encoding','UTF-8','-d',tmp,*map(str,sources)],check=True)
        subprocess.run([java,'-Xverify:all','-cp',tmp,'IncidentHarness'],check=True)
if __name__=='__main__':main()
