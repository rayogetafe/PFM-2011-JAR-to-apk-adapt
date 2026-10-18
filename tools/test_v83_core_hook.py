#!/usr/bin/env python3
"""Check the hook on a copied real core JAR without changing the input."""
import os,shutil,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'patches'))
import v83_match_journal
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'));exports=[f'--add-exports=java.base/jdk.internal.org.objectweb.asm{x}=ALL-UNNAMED' for x in ('','.tree','.tree.analysis')]
    (ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v83-hook-',dir=ROOT/'artifacts') as temp:
        jar=Path(temp)/'core.jar';shutil.copyfile(sys.argv[1],jar);v83_match_journal.main(str(jar))
        subprocess.run([javac,*exports,'-d',temp,str(ROOT/'tools/tests/v83/VerifyJournalHook.java')],check=True)
        subprocess.run([java,*exports,'-cp',temp,'VerifyJournalHook',str(jar)],check=True)
if __name__=='__main__':main()
