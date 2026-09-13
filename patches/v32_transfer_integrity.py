#!/usr/bin/env python3
"""Gate stock transfer-list processing and wrap big transfers with roster reconciliation."""
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

def add_ref(data,name):
    count,end=cp_end(data);entries=[]
    def utf(s):
        b=s.encode();entries.append(b'\x01'+struct.pack('>H',len(b))+b);return count+len(entries)-1
    ou=utf('pfmTransferPolicy31');oc=count+len(entries);entries.append(b'\x07'+struct.pack('>H',ou));nu=utf(name);du=utf('()V');nt=count+len(entries);entries.append(b'\x0c'+struct.pack('>HH',nu,du));ref=count+len(entries);entries.append(b'\x0a'+struct.pack('>HH',oc,nt));
    return data[:8]+struct.pack('>H',count+len(entries))+data[10:end]+b''.join(entries)+data[end:],ref

def patch(data):
    data,stock=add_ref(data,'runStockMarketTick');data,poach=add_ref(data,'runPoach')
    old_stock=b'\xb8\x00\x1c';old_poach=b'\xb8\x00\x2a'
    if data.count(old_stock)!=1 or data.count(old_poach)!=1:raise RuntimeError('unexpected market call sites')
    return data.replace(old_stock,b'\xb8'+struct.pack('>H',stock),1).replace(old_poach,b'\xb8'+struct.pack('>H',poach),1)

def main(path):
    with zipfile.ZipFile(path) as z:entries=[(x,z.read(x.filename)) for x in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v32-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=False
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,content in entries:
                if item.filename=='pfmMarketTick.class':content=patch(content);seen=True
                out.writestr(item,content)
        if not seen:raise RuntimeError('pfmMarketTick.class missing')
        os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v32 transfers: full policy gate + post-transaction unique roster ownership')

if __name__=='__main__':
    if len(sys.argv)!=2:raise SystemExit('usage: v32_transfer_integrity.py CORE_JAR')
    main(sys.argv[1])
