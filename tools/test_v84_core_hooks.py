#!/usr/bin/env python3
import os,shutil,subprocess,sys,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'patches'))
import v84_match_effects
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe');java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    exports=[f'--add-exports=java.base/jdk.internal.org.objectweb.asm{x}=ALL-UNNAMED' for x in ('','.tree','.tree.analysis')]
    with tempfile.TemporaryDirectory(prefix='pfm-v84-hook-',dir=ROOT/'artifacts') as tmp:
        jar=Path(tmp)/'core.jar';shutil.copyfile(sys.argv[1],jar);v84_match_effects.main(str(jar))
        subprocess.run([javac,*exports,'-d',tmp,str(ROOT/'tools/tests/v83/VerifyEffects84.java')],check=True)
        subprocess.run([java,*exports,'-cp',tmp,'VerifyEffects84',str(jar)],check=True)
if __name__=='__main__':main()
