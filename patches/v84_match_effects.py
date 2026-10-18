#!/usr/bin/env python3
"""Route legacy eligibility through a read-only overlay; keep original domestic ledgers intact."""
import os,struct,sys,tempfile,zipfile

def patch(data):
    d=bytearray(data);count=struct.unpack_from('>H',d,8)[0];cp=[None]*count;refs=[];p=10;i=1
    while i<count:
        at=p;tag=d[p];p+=1
        if tag==1:
            n=struct.unpack_from('>H',d,p)[0];p+=2;cp[i]=bytes(d[p:p+n]).decode('utf-8');p+=n
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):cp[i]=struct.unpack_from('>H',d,p)[0];p+=2
        elif tag in (9,10,11,12,17,18):
            cp[i]=struct.unpack_from('>HH',d,p)
            if tag==10:refs.append((at,cp[i]))
            p+=4
        elif tag==15:p+=3
        else:raise ValueError(tag)
        i+=1
    targets=[]
    for at,(owner,nt) in refs:
        name,desc=cp[nt]
        if cp[cp[owner]]=='pfmAvailable70' and (cp[name],cp[desc]) in {('starts','(I)I'),('minutesForRosterIndex','(Ldw;II)I'),('benchImpact','(Ldw;)I')}:
            targets.append(at)
    if not targets:return data,0
    for at in targets:struct.pack_into('>H',d,at+1,count+1)
    name=b'PfmEffectsBridge84';extra=b'\x01'+struct.pack('>H',len(name))+name+b'\x07'+struct.pack('>H',count)
    return bytes(d[:8])+struct.pack('>H',count+2)+bytes(d[10:p])+extra+bytes(d[p:]),len(targets)

def main(path):
    with zipfile.ZipFile(path) as z:entries=[(i,z.read(i.filename)) for i in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v84-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);changed=0
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,data in entries:
                if item.filename.endswith('.class'):
                    data,n=patch(data);changed+=n
                out.writestr(item,data)
        assert changed>0,'No availability references found'
        os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v84: %d eligibility references use the European overlay; domestic ledgers unchanged'%changed)
if __name__=='__main__':main(sys.argv[1])
