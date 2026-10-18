#!/usr/bin/env python3
"""Wrap the complete legacy post-match ledger, preserving every original instruction."""
import os,struct,sys,tempfile,zipfile

def patch(data):
    d=bytearray(data);count=struct.unpack_from('>H',d,8)[0];cp=[None]*count;offsets={};p=10;i=1
    while i<count:
        offsets[i]=p;tag=d[p];p+=1
        if tag==1:
            n=struct.unpack_from('>H',d,p)[0];p+=2;cp[i]=(tag,bytes(d[p:p+n]).decode('utf-8'));p+=n
        elif tag in (3,4):p+=4
        elif tag in (5,6):p+=8;i+=1
        elif tag in (7,8,16,19,20):cp[i]=(tag,struct.unpack_from('>H',d,p)[0]);p+=2
        elif tag in (9,10,11,12,17,18):cp[i]=(tag,*struct.unpack_from('>HH',d,p));p+=4
        elif tag==15:p+=3
        else:raise ValueError(tag)
        i+=1
    target=[]
    for i,e in enumerate(cp):
        if e and e[0]==10:
            owner=cp[cp[e[1]][1]][1];nat=cp[e[2]]
            if owner=='pfmAvailable70' and cp[nat[1]][1]=='sortTableAndRecord' and cp[nat[2]][1]=='()V':target.append(i)
    assert len(target)==1,'legacy ledger hook changed'
    name=b'PfmJournalBridge83';appended=b'\x01'+struct.pack('>H',len(name))+name+b'\x07'+struct.pack('>H',count)
    struct.pack_into('>H',d,offsets[target[0]]+1,count+1)
    return bytes(d[:8])+struct.pack('>H',count+2)+bytes(d[10:p])+appended+bytes(d[p:])

def main(path):
    with zipfile.ZipFile(path) as source:entries=[(i,source.read(i.filename)) for i in source.infolist()]
    fd,tmp=tempfile.mkstemp(prefix='pfm-v83-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=False
    try:
        with zipfile.ZipFile(tmp,'w') as out:
            for item,data in entries:
                if item.filename=='pfmMatchEvents.class':data=patch(data);seen=True
                out.writestr(item,data)
        assert seen,'pfmMatchEvents missing';os.replace(tmp,path)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)
    print('v83: journal advances after the complete legacy post-match ledger')
if __name__=='__main__':main(sys.argv[1])
