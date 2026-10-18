#!/usr/bin/env python3
"""Run the production AI market on real world data with minimal persistence fixtures."""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'));prod=ROOT/'overrides/app/src/main/java/pfm/android'
    (ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v82-',dir=ROOT/'artifacts') as temp:
        sources=[*list((ROOT/'tools/tests/v82').rglob('*.java')),prod/'NativeAiMarket.java',prod/'MatchEngine.java']
        if (prod/'NativeTransferMarketModel.java').exists():sources.append(prod/'NativeTransferMarketModel.java')
        subprocess.run([javac,'-encoding','UTF-8','-d',temp,*map(str,sources)],check=True)
        subprocess.run([java,'-cp',temp,'pfm.android.MarketHarness',*sys.argv[1:]],cwd=ROOT,check=True)
if __name__=='__main__':main()
