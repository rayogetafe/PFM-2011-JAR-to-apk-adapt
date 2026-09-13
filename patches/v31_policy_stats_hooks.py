#!/usr/bin/env python3
"""Add constant-pool method refs and redirect two fixed no-arg invokestatic hooks."""
import os, struct, sys, tempfile, zipfile

def cp_end(data):
    p=8; count=struct.unpack_from('>H',data,p)[0]; p+=2; i=1
    while i<count:
        tag=data[p]; p+=1
        if tag==1: n=struct.unpack_from('>H',data,p)[0]; p+=2+n
        elif tag in (3,4): p+=4
        elif tag in (5,6): p+=8; i+=1
        elif tag in (7,8,16,19,20): p+=2
        elif tag in (9,10,11,12,17,18): p+=4
        elif tag==15: p+=3
        else: raise RuntimeError('unsupported cp tag %d'%tag)
        i+=1
    return count,p

def add_methodref(data, owner, name, desc):
    count,end=cp_end(data); entries=[]
    def utf(s):
        b=s.encode('utf-8'); entries.append(bytes([1])+struct.pack('>H',len(b))+b); return count+len(entries)-1
    owner_u=utf(owner); owner_c=count+len(entries); entries.append(bytes([7])+struct.pack('>H',owner_u))
    name_u=utf(name); desc_u=utf(desc); nt=count+len(entries); entries.append(bytes([12])+struct.pack('>HH',name_u,desc_u))
    ref=count+len(entries); entries.append(bytes([10])+struct.pack('>HH',owner_c,nt))
    out=bytearray(); out+=data[:8]; out+=struct.pack('>H',count+len(entries)); out+=data[10:end]; out+=b''.join(entries); out+=data[end:]
    return bytes(out),ref

def patch_market(data):
    data,ref=add_methodref(data,'pfmTransferPolicy31','rollForMarket','()I')
    pattern=b'\x10\x64\xb8'
    hits=[i for i in range(len(data)-7) if data[i:i+3]==pattern and data[i+5:i+7]==b'\x10\x1e']
    if len(hits)!=1: raise RuntimeError('unexpected pfmMarketTick random gate count %d'%len(hits))
    i=hits[0]; return data[:i]+b'\xb8'+struct.pack('>H',ref)+b'\x00\x00'+data[i+5:]

def patch_stats(data):
    data,ref=add_methodref(data,'pfmStatsHook31','onMatchdayCompleted','()V')
    marker=b'pfmPlayerStats'
    if marker not in data: raise RuntimeError('pfmLineup83 marker missing')
    # restoreAndRecord contains a single invokestatic to the stock no-arg stats method.
    # Resolve the old Methodref index from the constant pool by using javap-stable index 94.
    old=b'\xb8\x00\x5e'
    if data.count(old)!=1: raise RuntimeError('unexpected pfmLineup83 stats call count')
    return data.replace(old,b'\xb8'+struct.pack('>H',ref),1)

def main(path):
    with zipfile.ZipFile(path,'r') as z: entries=[(x,z.read(x.filename)) for x in z.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v31-',suffix='.jar',dir=os.path.dirname(path) or '.'); os.close(fd); seen=set()
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,content in entries:
                if item.filename=='pfmMarketTick.class': content=patch_market(content); seen.add(item.filename)
                if item.filename=='pfmLineup83.class': content=patch_stats(content); seen.add(item.filename)
                out.writestr(item,content)
        if seen!={'pfmMarketTick.class','pfmLineup83.class'}: raise RuntimeError('hook classes missing')
        os.replace(tmp,path)
    finally:
        if os.path.exists(tmp): os.unlink(tmp)
    print('v31 hooks: career transfer frequency policy + goalkeeper match ledger')

if __name__=='__main__':
    if len(sys.argv)!=2: raise SystemExit('usage: v31_policy_stats_hooks.py CORE_JAR')
    main(sys.argv[1])
