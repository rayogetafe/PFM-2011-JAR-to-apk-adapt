#!/usr/bin/env python3
"""Route quick matches and the live goal gate through the same minute engine."""
import os, struct, sys, tempfile, zipfile

def u2(d,p):return struct.unpack_from('>H',d,p)[0]
def u4(d,p):return struct.unpack_from('>I',d,p)[0]
def patch(data,kind):
    d=bytearray(data);count=u2(d,8);cp=[None]*count;p=10;i=1
    while i<count:
        tag=d[p];p+=1
        if tag==1:
            n=u2(d,p);p+=2;cp[i]=bytes(d[p:p+n]).decode('utf-8');p+=n
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):p+=2
        elif tag in (9,10,11,12,17,18):p+=4
        elif tag==15:p+=3
        else:raise ValueError(tag)
        i+=1
    end=p;extra=[]
    def utf(s):
        idx=count+len(extra);b=s.encode();extra.append(b'\x01'+struct.pack('>H',len(b))+b);return idx
    owner=utf('PfmMatchEngineBridge');cl=count+len(extra);extra.append(b'\x07'+struct.pack('>H',owner))
    name,desc={'bb':('fast','(Ldw;Ldw;[Ljava/util/Vector;I)V'),
        'ca':('liveGoal','(Ljava/lang/Object;)Z'),
        'pfmSubstitutions':('prepareEventAndRound','(I)I')}[kind]
    ni,di=utf(name),utf(desc);nt=count+len(extra);extra.append(b'\x0c'+struct.pack('>HH',ni,di))
    ref=count+len(extra);extra.append(b'\x0a'+struct.pack('>HH',cl,nt))
    if kind=='ca':
        ni,di=utf('clockTick'),utf('(Ljava/lang/Object;)I');nt=count+len(extra);extra.append(b'\x0c'+struct.pack('>HH',ni,di))
        clock_ref=count+len(extra);extra.append(b'\x0a'+struct.pack('>HH',cl,nt))
    p=end+6;n=u2(d,p);p+=2+n*2
    def members(p,is_method):
        n=u2(d,p);p+=2;edits=[]
        for _ in range(n):
            mn,md=cp[u2(d,p+2)],cp[u2(d,p+4)];ac=u2(d,p+6);p+=8
            for _ in range(ac):
                attr=cp[u2(d,p)];size=u4(d,p+2);payload=p+6
                target=is_method and attr=='Code' and (
                    kind=='bb' and mn=='a' and md==desc or
                    kind=='ca' and mn=='b' and md=='()V' or
                    kind=='pfmSubstitutions' and mn=='prepareEventAndRound' and md=='(I)I')
                if target:
                    if kind=='ca':
                        start=payload+8;length=u4(d,payload+4);code=bytearray(d[start:start+length])
                        assert length==1183 and code[559:563]==b'\x1c\x99\x02\x61', 'live gate layout changed'
                        assert code[253:264]==b'\x2a\x59\xb4\x00\x34\x04\x60\x5a\xb5\x00\x34', 'clock layout changed'
                        clock=b'\x2a\xb8'+struct.pack('>H',clock_ref)
                        code[253:264]=clock+b'\x00'*(11-len(clock))
                        # Keep physical out-of-bounds restarts; scheduled goals use the original goal branch.
                        body=b'\x2a\xb8'+struct.pack('>H',ref)+b'\x9a'+struct.pack('>h',596-563)
                        body+=b'\x1c\x99'+struct.pack('>h',1169-567)+b'\xa7'+struct.pack('>h',1156-570)
                        code[559:596]=body+b'\x00'*(37-len(body));d[start:start+length]=code
                    else:
                        code=(b'\x2a\x2b\x2c\x1d' if kind=='bb' else b'\x1a')+b'\xb8'+struct.pack('>H',ref)+(b'\xb1' if kind=='bb' else b'\xac')
                        payload_new=struct.pack('>HHI',4 if kind=='bb' else 1,4 if kind=='bb' else 1,len(code))+code+b'\x00\x00\x00\x00'
                        edits.append((p+2,p+6+size,struct.pack('>I',len(payload_new))+payload_new))
                p=payload+size
        return p,edits
    p,_=members(p,False);_,edits=members(p,True)
    if kind!='ca':assert len(edits)==1, 'expected one method replacement'
    for a,b,replacement in reversed(edits):d[a:b]=replacement
    return bytes(d[:8])+struct.pack('>H',count+len(extra))+bytes(d[10:end])+b''.join(extra)+bytes(d[end:])

def main(path):
    with zipfile.ZipFile(path) as z:entries=[(i,z.read(i.filename)) for i in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v81-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd)
    changed=[]
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,data in entries:
                kind=item.filename.removesuffix('.class')
                if kind in ('bb','ca','pfmSubstitutions'):data=patch(data,kind);changed.append(kind)
                out.writestr(item,data)
        assert len(changed)==3, changed
        os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v81: quick, live and exact scorer minutes use the unified match engine')
if __name__=='__main__':main(sys.argv[1])
