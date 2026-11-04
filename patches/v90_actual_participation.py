#!/usr/bin/env python3
"""Stop pre-incrementing snapshot starts; actual appearances and starts are committed together."""
import os,sys,tempfile,zipfile
from v88_in_match_events import replace
def main(path):
    with zipfile.ZipFile(path) as z:entries=[(i,z.read(i.filename)) for i in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v90-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);changed=0
    try:
        with zipfile.ZipFile(tmp,'w') as z:
            for item,data in entries:
                if item.filename=='pfmLineup83.class':data=replace(data,'addStartsFromSnapshot','()V','PfmParticipationBridge90','noSnapshotStarts','()V',b'',0,0);changed+=1
                if item.filename=='pfmMatchEvents.class':data=replace(data,'currentRound','()I','PfmMatchEngineBridge','currentRound','()I',b'',1,0);changed+=1
                z.writestr(item,data)
        assert changed==2;os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v90: appearances, starts and rating samples follow actual playing time')
if __name__=='__main__':main(sys.argv[1])
