#!/usr/bin/env python3
"""Patch a copy and verify real bytecode, leaving the input JAR untouched."""
import os, shutil, subprocess, sys, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'patches'))
import v81_unified_match_engine
def main():
    javac=shutil.which('javac') or str(Path(os.environ.get('ProgramFiles','C:/Program Files'))/'BellSoft/LibericaJDK-17/bin/javac.exe')
    java=str(Path(javac).with_name('java.exe' if os.name=='nt' else 'java'))
    exports=[f'--add-exports=java.base/jdk.internal.org.objectweb.asm{x}=ALL-UNNAMED' for x in ('','.tree','.tree.analysis')]
    (ROOT/'artifacts').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='pfm-v81-core-',dir=ROOT/'artifacts') as temp:
        jar=Path(temp)/'core.jar';shutil.copyfile(sys.argv[1],jar);v81_unified_match_engine.main(str(jar))
        sources=[ROOT/'overrides/app/src/main/java/PfmMatchEngineBridge.java',ROOT/'overrides/app/src/main/java/pfm/android/MatchEngine.java',
            ROOT/'tools/tests/v81/pfm/android/NativeWorldCenter.java',ROOT/'tools/tests/v81/android/util/Log.java',ROOT/'tools/tests/v81/PfmEffectsBridge84.java']
        subprocess.run([javac,'-encoding','UTF-8','-cp',str(jar),'-d',temp,*map(str,sources)],check=True)
        subprocess.run([javac,*exports,'-d',temp,str(ROOT/'tools/tests/v81/VerifyCore.java')],check=True)
        subprocess.run([javac,'-cp',temp,'-d',temp,str(ROOT/'tools/tests/v81/BridgeHarness.java')],check=True)
        subprocess.run([java,*exports,'-cp',temp,'VerifyCore',str(jar),temp],check=True)
        subprocess.run([java,'-Xverify:all','-cp',temp,'BridgeHarness'],check=True)
if __name__=='__main__':main()
