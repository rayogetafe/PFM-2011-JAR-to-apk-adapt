#!/usr/bin/env python3
"""Use the completed one-based live round instead of the lagging table count."""
import os,struct,sys,tempfile,zipfile

def cp_end(d):
 p=8;c=struct.unpack_from('>H',d,p)[0];p+=2;i=1
 while i<c:
  t=d[p];p+=1
  if t==1:n=struct.unpack_from('>H',d,p)[0];p+=2+n
  elif t in (3,4):p+=4
  elif t in (5,6):p+=8;i+=1
  elif t in (7,8,16,19,20):p+=2
  elif t in (9,10,11,12,17,18):p+=4
  elif t==15:p+=3
  else:raise RuntimeError('cp tag %d'%t)
  i+=1
 return c,p

def add_round_ref(d):
 c,end=cp_end(d);e=[]
 def u(s):b=s.encode();e.append(b'\x01'+struct.pack('>H',len(b))+b);return c+len(e)-1
 ou=u('pfmMatchPost35');oc=c+len(e);e.append(b'\x07'+struct.pack('>H',ou));nu=u('completedRoundOne');du=u('()I');nt=c+len(e);e.append(b'\x0c'+struct.pack('>HH',nu,du));ref=c+len(e);e.append(b'\x0a'+struct.pack('>HH',oc,nt))
 return d[:8]+struct.pack('>H',c+len(e))+d[10:end]+b''.join(e)+d[end:],ref

def patch(d):
 d,ref=add_round_ref(d);old=b'\xb8\x00\x8a'
 if d.count(old)!=1:raise RuntimeError('unexpected cp.pfmPlayedForTable call count %d'%d.count(old))
 return d.replace(old,b'\xb8'+struct.pack('>H',ref),1)

def main(path):
 with zipfile.ZipFile(path) as z:entries=[(x,z.read(x.filename)) for x in z.infolist()]
 fd,tmp=tempfile.mkstemp(prefix='pfm-v36-',suffix='.jar',dir=os.path.dirname(path) or '.');os.close(fd);seen=False
 try:
  with zipfile.ZipFile(tmp,'w') as out:
   for x,d in entries:
    if x.filename=='pfmDiscipline70.class':d=patch(d);seen=True
    out.writestr(x,d)
  if not seen:raise RuntimeError('pfmDiscipline70.class missing')
  os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 print('v36 timing: discipline and archive enrichment use the completed live round')

if __name__=='__main__':
 if len(sys.argv)!=2:raise SystemExit('usage: v36_same_round_discipline.py CORE_JAR')
 main(sys.argv[1])
