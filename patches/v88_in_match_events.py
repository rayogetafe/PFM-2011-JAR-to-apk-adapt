#!/usr/bin/env python3
"""Replace obsolete post-match lotteries; scorer selection uses the actual on-field XI."""
import os,struct,sys,tempfile,zipfile
def replace(data,target,descriptor,owner,method,call_descriptor,arguments,stack,locals_count):
    d=bytearray(data);count=struct.unpack_from('>H',d,8)[0];cp=[None]*count;p=10;i=1
    while i<count:
        tag=d[p];p+=1
        if tag==1:
            n=struct.unpack_from('>H',d,p)[0];p+=2;cp[i]=bytes(d[p:p+n]).decode();p+=n
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):p+=2
        elif tag in (9,10,11,12,17,18):p+=4
        elif tag==15:p+=3
        else:raise ValueError(tag)
        i+=1
    end=p;extra=[]
    def utf(s):
        b=s.encode();idx=count+len(extra);extra.append(b'\x01'+struct.pack('>H',len(b))+b);return idx
    ou=utf(owner);cl=count+len(extra);extra.append(b'\x07'+struct.pack('>H',ou));name,desc=utf(method),utf(call_descriptor)
    nt=count+len(extra);extra.append(b'\x0c'+struct.pack('>HH',name,desc));ref=count+len(extra);extra.append(b'\x0a'+struct.pack('>HH',cl,nt))
    p=end+6;n=struct.unpack_from('>H',d,p)[0];p+=2+2*n
    changed=0
    for is_method in (False,True):
        n=struct.unpack_from('>H',d,p)[0];p+=2
        for _ in range(n):
            mn,md=cp[struct.unpack_from('>H',d,p+2)[0]],cp[struct.unpack_from('>H',d,p+4)[0]];ac=struct.unpack_from('>H',d,p+6)[0];p+=8
            for _ in range(ac):
                attr=cp[struct.unpack_from('>H',d,p)[0]];size=struct.unpack_from('>I',d,p+2)[0]
                if is_method and mn==target and md==descriptor and attr=='Code':
                    code=arguments+b'\xb8'+struct.pack('>H',ref)+(b'\xb1' if descriptor.endswith('V') else b'\xac' if descriptor.endswith('I') else b'\xb0')
                    payload=struct.pack('>HHI',stack,locals_count,len(code))+code+b'\x00\x00\x00\x00'
                    d[p+2:p+6+size]=struct.pack('>I',len(payload))+payload;size=len(payload);changed+=1
                p+=6+size
    assert changed==1,(target,changed)
    return bytes(d[:8])+struct.pack('>H',count+len(extra))+bytes(d[10:end])+b''.join(extra)+bytes(d[end:])
def main(path):
    targets={'pfmSubstitutions.class':('pfmScorer','(Ldw;I)Lci;','PfmMatchEngineBridge','scorer','(Ldw;I)Lci;',b'\x2a\x1b',2,2),
             'pfmCondition60.class':('onMatchdayCompleted','()V','PfmIncidentsBridge88','condition','()V',b'',0,0),
             'pfmDiscipline70.class':('onMatchdayCompleted','()V','PfmIncidentsBridge88','discipline','()V',b'',0,0)}
    with zipfile.ZipFile(path) as z:entries=[(i,z.read(i.filename)) for i in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v88-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=set()
    try:
        with zipfile.ZipFile(tmp,'w') as z:
            for item,data in entries:
                if item.filename in targets:data=replace(data,*targets[item.filename]);seen.add(item.filename)
                z.writestr(item,data)
        assert seen==set(targets);os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v88: actual on-field scorers and once-only in-match condition/discipline commits')
if __name__=='__main__':main(sys.argv[1])
