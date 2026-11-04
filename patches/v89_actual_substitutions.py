#!/usr/bin/env python3
"""Use executed substitutions in the legacy persisted event log instead of planned exposure."""
import os,sys,tempfile,zipfile
from v88_in_match_events import replace
def main(path):
    with zipfile.ZipFile(path) as z:entries=[(i,z.read(i.filename)) for i in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v89-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);changed=0
    try:
        with zipfile.ZipFile(tmp,'w') as z:
            for item,data in entries:
                if item.filename=='pfmMatchEvents.class':
                    data=replace(data,'appendSubstitutions','(Ljava/lang/StringBuffer;Ldw;I)V','PfmMatchEngineBridge','appendSubstitutions','(Ljava/lang/StringBuffer;Ldw;I)V',b'\x2a\x2b\x1c',3,3);changed+=1
                z.writestr(item,data)
        assert changed==1;os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v89: persisted substitution log uses executed match events, not planned minutes')
if __name__=='__main__':main(sys.argv[1])
