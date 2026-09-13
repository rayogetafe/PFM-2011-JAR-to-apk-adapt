#!/usr/bin/env python3
"""Route the legacy in-season market call through the unified transfer policy."""
import os, struct, sys, tempfile, zipfile

def cp_end(data):
    p=8; count=struct.unpack_from('>H',data,p)[0]; p+=2; i=1
    while i<count:
        tag=data[p]; p+=1
        if tag==1:n=struct.unpack_from('>H',data,p)[0];p+=2+n
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):p+=2
        elif tag in (9,10,11,12,17,18):p+=4
        elif tag==15:p+=3
        else:raise RuntimeError('unsupported cp tag %d'%tag)
        i+=1
    return count,p

def add_ref(data):
    count,end=cp_end(data);entries=[]
    def utf(s):
        b=s.encode();entries.append(b'\x01'+struct.pack('>H',len(b))+b);return count+len(entries)-1
    owner_u=utf('pfmTransferPolicy31');owner_c=count+len(entries);entries.append(b'\x07'+struct.pack('>H',owner_u))
    name_u=utf('runStockMarketTick');desc_u=utf('()V');nt=count+len(entries);entries.append(b'\x0c'+struct.pack('>HH',name_u,desc_u))
    ref=count+len(entries);entries.append(b'\x0a'+struct.pack('>HH',owner_c,nt))
    return data[:8]+struct.pack('>H',count+len(entries))+data[10:end]+b''.join(entries)+data[end:],ref

def patch(data):
    data,ref=add_ref(data)
    # ca.a(Object,int), state 0 / action 0: the one legacy bb.a() call which
    # drives the round 17-21 winter market and used to bypass v32 entirely.
    old=b'\xb8\x01\x8e'
    if data.count(old)!=1:raise RuntimeError('unexpected ca -> bb.a market call count %d'%data.count(old))
    return data.replace(old,b'\xb8'+struct.pack('>H',ref),1)

def main(path):
    with zipfile.ZipFile(path) as z:entries=[(x,z.read(x.filename)) for x in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v33-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=False
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,content in entries:
                if item.filename=='ca.class':content=patch(content);seen=True
                out.writestr(item,content)
        if not seen:raise RuntimeError('ca.class missing')
        os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v33 market: winter and summer calls share the Season/Career policy gateway')

if __name__=='__main__':
    if len(sys.argv)!=2:raise SystemExit('usage: v33_market_gateway.py CORE_JAR')
    main(sys.argv[1])
